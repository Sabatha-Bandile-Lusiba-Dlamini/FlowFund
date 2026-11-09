package com.example.flowfund_part2

import com.example.flowfund_part2.data.local.TradeEntity
import com.example.flowfund_part2.utils.TradeInput
import com.example.flowfund_part2.utils.TradeInputResult
import org.junit.Assert.*
import org.junit.Test

class TradeInputTest {

    private val base = TradeEntity(title = "", asset = "", tradeType = "BUY", entryPrice = 0.0, exitPrice = 0.0, quantity = 0.0, tradeDate = 5L)

    private fun parse(
        title: String = "Swing", asset: String = "btc", type: String = "buy",
        entry: String = "100", exit: String = "120", qty: String = "2", notes: String = ""
    ) = TradeInput.parse(base, title, asset, type, entry, exit, qty, notes)

    @Test fun validInput_buildsTrimmedUppercaseTrade_keepingBaseFields() {
        val r = parse(title = "  Swing  ", type = "sell") as TradeInputResult.Valid
        assertEquals("Swing", r.trade.title)
        assertEquals("SELL", r.trade.tradeType)
        assertEquals(100.0, r.trade.entryPrice, 0.0)
        assertEquals(5L, r.trade.tradeDate)            // date and sync fields come from base
        assertEquals(base.clientGeneratedId, r.trade.clientGeneratedId)
    }

    @Test fun blankTitle_isRejected() = assertTrue(parse(title = " ") is TradeInputResult.Invalid)
    @Test fun badType_isRejected() = assertTrue(parse(type = "hold") is TradeInputResult.Invalid)
    @Test fun nonNumericPrice_isRejected() = assertTrue(parse(entry = "abc") is TradeInputResult.Invalid)
    @Test fun negativePrice_isRejected() = assertTrue(parse(exit = "-1") is TradeInputResult.Invalid)
    @Test fun zeroQuantity_isRejected() = assertTrue(parse(qty = "0") is TradeInputResult.Invalid)
    @Test fun overlongNotes_areRejected() = assertTrue(parse(notes = "x".repeat(2001)) is TradeInputResult.Invalid)
}
