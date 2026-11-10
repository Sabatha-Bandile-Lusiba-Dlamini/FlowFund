package com.example.flowfund_part2.utils

import com.example.flowfund_part2.data.local.TradeEntity

/** Result of checking the text typed into the trade form. */
sealed interface TradeInputResult {
    data class Valid(val trade: TradeEntity) : TradeInputResult
    data class Invalid(val message: String) : TradeInputResult
}

/**
 * Turns the raw form fields into a [TradeEntity], or explains what is wrong. Kept as a plain function
 * (no Android classes) so it is unit tested directly. Limits mirror the API's validation, so the user is
 * told about a bad value here instead of getting a 400 later.
 */
object TradeInput {

    fun parse(
        base: TradeEntity,
        title: String,
        asset: String,
        tradeType: String,
        entryPrice: String,
        exitPrice: String,
        quantity: String,
        notes: String
    ): TradeInputResult {
        val t = title.trim()
        val a = asset.trim()
        val type = tradeType.trim().uppercase()
        val entry = entryPrice.trim().toDoubleOrNull()
        val exit = exitPrice.trim().toDoubleOrNull()
        val qty = quantity.trim().toDoubleOrNull()

        return when {
            t.isEmpty() || t.length > 100 -> TradeInputResult.Invalid("Title must be 1-100 characters.")
            a.isEmpty() || a.length > 30 -> TradeInputResult.Invalid("Asset must be 1-30 characters.")
            type != "BUY" && type != "SELL" -> TradeInputResult.Invalid("Choose Buy or Sell.")
            entry == null || entry < 0 -> TradeInputResult.Invalid("Entry price must be a number, 0 or more.")
            exit == null || exit < 0 -> TradeInputResult.Invalid("Exit price must be a number, 0 or more.")
            qty == null || qty <= 0 -> TradeInputResult.Invalid("Quantity must be greater than zero.")
            notes.length > 2000 -> TradeInputResult.Invalid("Notes must be at most 2000 characters.")
            else -> TradeInputResult.Valid(
                base.copy(
                    title = t, asset = a, tradeType = type,
                    entryPrice = entry, exitPrice = exit, quantity = qty, notes = notes.trim()
                )
            )
        }
    }
}
