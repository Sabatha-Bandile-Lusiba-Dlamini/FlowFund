package com.example.flowfund_part2.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Retrofit description of the deployed FlowFund API (Square, 2026). Every path is relative to the
 * base URL ending in /api/v1/ (see BuildConfig.API_BASE_URL). Functions that can fail in a way the
 * caller must inspect (create, delete) return [Response] so the HTTP status is visible.
 */
interface FlowFundApi {

    @POST("auth/sso/verify")
    suspend fun verifySso(@Body body: SsoVerifyRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): AuthResponse

    @GET("trades")
    suspend fun getTrades(): List<TradeResponse>

    @POST("trades")
    suspend fun createTrade(@Body body: TradeRequest): TradeResponse

    @PUT("trades/{id}")
    suspend fun updateTrade(@Path("id") id: Long, @Body body: TradeRequest): TradeResponse

    @DELETE("trades/{id}")
    suspend fun deleteTrade(@Path("id") id: Long): Response<Unit>

    @GET("trades/statistics")
    suspend fun getTradeStatistics(): TradeStatisticsResponse

    @GET("journal")
    suspend fun getJournal(): List<JournalResponse>

    @POST("journal")
    suspend fun createJournal(@Body body: JournalRequest): JournalResponse

    @PUT("journal/{id}")
    suspend fun updateJournal(@Path("id") id: Long, @Body body: JournalRequest): JournalResponse

    @DELETE("journal/{id}")
    suspend fun deleteJournal(@Path("id") id: Long): Response<Unit>
}

/*
REFERENCES

Square (2026) 'Retrofit: a type-safe HTTP client for Android and Java'.
Available at: https://square.github.io/retrofit/
(Accessed: 4 November 2026).
*/
