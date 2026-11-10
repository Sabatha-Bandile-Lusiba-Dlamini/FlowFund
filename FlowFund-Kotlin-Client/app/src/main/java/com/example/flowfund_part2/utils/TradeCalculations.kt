
package com.example.flowfund_part2.utils

import com.example.flowfund_part2.data.local.TradeEntity

/**
 * Stores analytics results for closed trades.
 *
 * Kotlin data classes are used to store related information
 * in a structured format (Kotlin, 2026a).
 */
data class TradeAnalytics(
    val totalClosedTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val totalProfitLoss: Double,
    val winRate: Double
)

/**
 * Calculates the total profit or loss.
 *
 * The sum() collection operation adds all values in a collection
 * to produce the total result (Kotlin, 2026b).
 *
 * @param profitLossValues List of individual profit and loss values.
 * @return Total profit or loss.
 */
fun calculateTotalProfitLoss(
    profitLossValues: List<Double>
): Double {
    return profitLossValues.sum()
}

/**
 * Calculates analytics from profit/loss values.
 *
 * Collection filtering and counting operations are used to identify
 * winning and losing trades (Kotlin, 2026b).
 *
 * The win rate is calculated as the percentage of winning trades
 * out of all closed trades.
 *
 * @param closedTradeProfitLoss List of closed trade profit/loss values.
 * @return A TradeAnalytics object containing the calculated results.
 */
fun calculateAnalytics(
    closedTradeProfitLoss: List<Double>
): TradeAnalytics {

    // Counts trades with a positive profit.
    val wins = closedTradeProfitLoss.count { it > 0.0 }

    // Counts trades with a negative profit.
    val losses = closedTradeProfitLoss.count { it < 0.0 }

    // Determines the total number of closed trades.
    val total = closedTradeProfitLoss.size

    // Creates an analytics object containing the calculated results.
    return TradeAnalytics(
        totalClosedTrades = total,
        winningTrades = wins,
        losingTrades = losses,
        totalProfitLoss = closedTradeProfitLoss.sum(),

        // Calculates the win rate while preventing division by zero.
        winRate = if (total == 0) {
            0.0
        } else {
            wins.toDouble() / total * 100.0
        }
    )
}

/**
 * Calculates profit/loss for an individual trade.
 *
 * Uses the difference between entry and exit prices multiplied
 * by the trade quantity to calculate the result.
 *
 * BUY and SELL trades use different price calculations
 * according to their trading direction (Hayes, 2026).
 *
 * @param trade The saved trade record.
 * @return The calculated profit or loss.
 */
fun calculateTradeProfitLoss(trade: TradeEntity): Double {

    // Identifies the trade type and calculates the financial result.
    return when (trade.tradeType.trim().uppercase()) {

        // Calculates profit/loss for a BUY trade.
        "BUY" -> (trade.exitPrice - trade.entryPrice) * trade.quantity

        // Calculates profit/loss for a SELL trade.
        "SELL" -> (trade.entryPrice - trade.exitPrice) * trade.quantity

        // Returns zero for an unsupported trade type.
        else -> 0.0
    }
}

/**
 * Calculates analytics from saved trade records.
 *
 * The map() operation transforms each trade record into
 * its corresponding profit/loss value (Kotlin, 2026b).
 *
 * @param trades List of saved trade records.
 * @return Analytics calculated from the saved trades.
 */
fun calculateAnalyticsFromTrades(
    trades: List<TradeEntity>
): TradeAnalytics {

    // Converts each saved trade into a profit/loss value.
    val profitLossValues = trades.map {
        calculateTradeProfitLoss(it)
    }

    // Calculates the final analytics using the profit/loss values.
    return calculateAnalytics(profitLossValues)
}

/*
REFERENCES

Hayes, A. (2026) 'Profit and Loss (P&L): Definition, Formula, and Examples',
Investopedia. Available at:
https://www.investopedia.com/terms/p/plstatement.asp
(Accessed: 3 November 2026).

Kotlin (2026a) 'Data classes',
Kotlin Documentation. Available at:
https://kotlinlang.org/docs/data-classes.html
(Accessed: 3 November 2026).

Kotlin (2026b) 'Collection operations',
Kotlin Documentation. Available at:
https://kotlinlang.org/docs/collection-operations.html
(Accessed: 3 November 2026).
*/
