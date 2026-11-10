package com.example.flowfund_part2.data.remote

import com.example.flowfund_part2.BuildConfig
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the single Retrofit/OkHttp stack. The base URL comes from BuildConfig.API_BASE_URL (set per
 * build type in app/build.gradle.kts), so no URL is hard-coded in code (Square, 2026).
 */
object ApiClient {

    fun create(tokens: TokenStore, baseUrl: String = BuildConfig.API_BASE_URL): FlowFundApi {
        // A SEPARATE client with no authenticator performs the refresh, so a failing refresh cannot recurse.
        val plainApi = retrofit(baseUrl, OkHttpClient.Builder().timeouts().build())

        val authenticator = TokenAuthenticator(tokens) { refreshToken ->
            val call = plainApi.create(FlowFundApi::class.java)
            kotlinx.coroutines.runBlocking { call.refresh(RefreshRequest(refreshToken)) }
        }

        val client = OkHttpClient.Builder()
            .timeouts()
            .addInterceptor(AuthInterceptor(tokens))
            .authenticator(authenticator)
            .apply {
                if (BuildConfig.DEBUG) {
                    // Headers only: never log bodies, they contain tokens and personal data.
                    addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                }
            }
            .build()

        return retrofit(baseUrl, client).create(FlowFundApi::class.java)
    }

    // Serverless Azure SQL can take ~60 s to wake from auto-pause, hence the generous read timeout.
    private fun OkHttpClient.Builder.timeouts() = apply {
        connectTimeout(30, TimeUnit.SECONDS)
        readTimeout(90, TimeUnit.SECONDS)
        writeTimeout(30, TimeUnit.SECONDS)
    }

    private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
}
