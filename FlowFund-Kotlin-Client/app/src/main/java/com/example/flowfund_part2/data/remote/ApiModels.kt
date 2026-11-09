package com.example.flowfund_part2.data.remote

/**
 * JSON bodies exchanged with the FlowFund API. Property names match the API's camelCase JSON exactly.
 * Dates are Unix epoch MILLISECONDS, the same value Room already stores, so no conversion is needed.
 */
data class SsoVerifyRequest(val idToken: String)

data class RefreshRequest(val refreshToken: String)

data class ApiUser(val id: Int, val fullName: String, val email: String)

data class AuthResponse(val accessToken: String, val refreshToken: String, val user: ApiUser)

data class TradeRequest(
    val title: String,
    val asset: String,
    val tradeType: String,
    val entryPrice: Double,
    val exitPrice: Double,
    val quantity: Double,
    val tradeDate: Long,
    val notes: String,
    val clientGeneratedId: String
)

data class TradeResponse(
    val id: Long,
    val title: String,
    val asset: String,
    val tradeType: String,
    val entryPrice: Double,
    val exitPrice: Double,
    val quantity: Double,
    val tradeDate: Long,
    val notes: String,
    val clientGeneratedId: String,
    val profitLoss: Double
)

data class JournalRequest(
    val title: String,
    val content: String,
    val mood: String,
    val entryDate: Long,
    val clientGeneratedId: String
)

data class JournalResponse(
    val id: Long,
    val title: String,
    val content: String,
    val mood: String,
    val entryDate: Long,
    val clientGeneratedId: String
)

data class AssetStatistics(val asset: String, val trades: Int, val profitLoss: Double)

data class TradeStatisticsResponse(
    val totalClosedTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val totalProfitLoss: Double,
    val winRate: Double,
    val byAsset: List<AssetStatistics>
)
