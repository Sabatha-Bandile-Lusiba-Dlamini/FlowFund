
package com.example.flowfund_part2

import com.example.flowfund_part2.data.local.TradeEntity

sealed interface TradesUiState {
    data object Loading : TradesUiState

    data object Empty : TradesUiState

    data class Success(
        val trades: List<TradeEntity>
    ) : TradesUiState

    data class Error(
        val message: String
    ) : TradesUiState
}
