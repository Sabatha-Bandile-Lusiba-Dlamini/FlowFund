package com.example.flowfund_part2.data.remote

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Where the API's own access/refresh tokens live between launches. An interface so tests can fake it. */
interface TokenStore {
    var accessToken: String?
    var refreshToken: String?
    fun clear()
}

/**
 * Keeps the tokens in EncryptedSharedPreferences, encrypted with a key held in the Android Keystore
 * (Android Developers, 2026), instead of plain SharedPreferences where a rooted device or a backup could read them.
 */
class EncryptedTokenStore(context: Context) : TokenStore {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        "flowfund_secure_tokens",
        MasterKey.Builder(context.applicationContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override var accessToken: String?
        get() = prefs.getString(KEY_ACCESS, null)
        set(value) = prefs.edit().putString(KEY_ACCESS, value).apply()

    override var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH, null)
        set(value) = prefs.edit().putString(KEY_REFRESH, value).apply()

    override fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
    }
}

/** In-memory store used by unit tests. */
class InMemoryTokenStore : TokenStore {
    override var accessToken: String? = null
    override var refreshToken: String? = null
    override fun clear() { accessToken = null; refreshToken = null }
}

/*
REFERENCES

Android Developers (2026) 'Work with data more securely: EncryptedSharedPreferences'.
Available at: https://developer.android.com/privacy-and-security/cryptography
(Accessed: 4 November 2026).
*/
