package com.example.flowfund_part2.data.repository

import android.util.Log
import com.example.flowfund_part2.data.remote.FlowFundApi
import com.example.flowfund_part2.data.remote.SsoVerifyRequest
import com.example.flowfund_part2.data.remote.TokenStore

/**
 * Exchanges the Firebase ID token (obtained by Google Sign-In in LoginActivity) for the API's own
 * access/refresh tokens and stores them. After this, every API call is authenticated by the interceptor.
 */
class AuthRepository(
    private val api: FlowFundApi,
    private val tokens: TokenStore
) {
    val isSignedIn: Boolean get() = tokens.accessToken != null

    suspend fun exchangeFirebaseToken(firebaseIdToken: String): SyncResult = try {
        val auth = api.verifySso(SsoVerifyRequest(firebaseIdToken))
        tokens.accessToken = auth.accessToken
        tokens.refreshToken = auth.refreshToken
        Log.i(TAG, "API session started for user ${auth.user.id}")
        SyncResult.Success
    } catch (e: Exception) {
        Log.w(TAG, "SSO exchange failed: ${e.message}")
        e.toFailure()
    }

    /**
     * Same exchange as [exchangeFirebaseToken], but returns a plain value for the login screen:
     * null on success, otherwise a user-facing error message.
     */
    suspend fun signInWithFirebaseToken(firebaseIdToken: String): String? =
        (exchangeFirebaseToken(firebaseIdToken) as? SyncResult.Failure)?.message

    fun signOut() = tokens.clear()

    private companion object { const val TAG = "AuthRepository" }
}

