
package com.example.flowfund_part2

data class TradeAnalytics(
    val totalClosedTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val totalProfitLoss: Double,
    val winRate: Double
)

fun calculateAnalytics(
    closedTradeProfitLoss: List<Double>
): TradeAnalytics {
    val wins = closedTradeProfitLoss.count { it > 0.0 }
    val losses = closedTradeProfitLoss.count { it < 0.0 }
    val total = closedTradeProfitLoss.size

    return TradeAnalytics(
        totalClosedTrades = total,
        winningTrades = wins,
        losingTrades = losses,
        totalProfitLoss = closedTradeProfitLoss.sum(),
        winRate = if (total == 0) {
            0.0
        } else {
            wins.toDouble() / total * 100.0
        }
    )
}
