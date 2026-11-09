package com.example.flowfund_part2.data.remote

import android.util.Log
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/** Adds "Authorization: Bearer <access token>" to every request that has a stored token. */
class AuthInterceptor(private val tokens: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokens.accessToken ?: return chain.proceed(chain.request())
        return chain.proceed(chain.request().newBuilder().header("Authorization", "Bearer $token").build())
    }
}

/**
 * Called by OkHttp when the API answers 401 (the 15-minute access token expired). It exchanges the stored
 * refresh token for a new pair and retries the request once. If the refresh itself is rejected the
 * tokens are cleared, which makes the app require a fresh sign-in (OkHttp, 2026).
 *
 * [refreshCall] performs the blocking HTTP refresh; it is passed in so this class has no Retrofit dependency
 * and can be unit tested.
 */
class TokenAuthenticator(
    private val tokens: TokenStore,
    private val refreshCall: (refreshToken: String) -> AuthResponse?
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Give up after one retry so a permanently bad token cannot loop forever.
        if (response.priorResponse != null) return null

        synchronized(lock) {
            val sent = response.request.header("Authorization")?.removePrefix("Bearer ")
            val current = tokens.accessToken

            // Another request already refreshed while this one waited: just retry with the new token.
            if (current != null && current != sent) return response.request.withToken(current)

            val refresh = tokens.refreshToken ?: return null
            val result = try {
                refreshCall(refresh)
            } catch (e: Exception) {
                Log.w(TAG, "Token refresh failed: ${e.message}")
                null
            }
            if (result == null) {
                Log.w(TAG, "Refresh token rejected; signing the user out")
                tokens.clear()
                return null
            }
            tokens.accessToken = result.accessToken
            tokens.refreshToken = result.refreshToken
            return response.request.withToken(result.accessToken)
        }
    }

    private fun Request.withToken(token: String) = newBuilder().header("Authorization", "Bearer $token").build()

    private companion object { const val TAG = "TokenAuthenticator" }
}

/*
REFERENCES

OkHttp (2026) 'Authenticator'.
Available at: https://square.github.io/okhttp/5.x/okhttp/okhttp3/-authenticator/
(Accessed: 4 November 2026).
*/
