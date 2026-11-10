package com.example.flowfund_part2

import com.example.flowfund_part2.data.remote.*
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.*
import org.junit.Test

class TokenAuthenticatorTest {

    private fun unauthorised(token: String): Response =
        Response.Builder()
            .request(Request.Builder().url("http://localhost/api/v1/trades").header("Authorization", "Bearer $token").build())
            .protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized").build()

    private val user = ApiUser(1, "T", "t@t.com")

    @Test fun refreshesTokenAndRetriesWithNewOne() {
        val store = InMemoryTokenStore().apply { accessToken = "old"; refreshToken = "r1" }
        val auth = TokenAuthenticator(store) { AuthResponse("new", "r2", user) }

        val retry = auth.authenticate(null, unauthorised("old"))

        assertEquals("Bearer new", retry!!.header("Authorization"))
        assertEquals("r2", store.refreshToken)
    }

    @Test fun rejectedRefreshClearsTokens() {
        val store = InMemoryTokenStore().apply { accessToken = "old"; refreshToken = "r1" }
        val auth = TokenAuthenticator(store) { null }

        assertNull(auth.authenticate(null, unauthorised("old")))
        assertNull(store.accessToken)
        assertNull(store.refreshToken)
    }

    @Test fun usesTokenAnotherRequestAlreadyRefreshed() {
        val store = InMemoryTokenStore().apply { accessToken = "fresh"; refreshToken = "r2" }
        var calls = 0
        val auth = TokenAuthenticator(store) { calls++; null }

        val retry = auth.authenticate(null, unauthorised("stale"))

        assertEquals("Bearer fresh", retry!!.header("Authorization"))
        assertEquals(0, calls)
    }
}
