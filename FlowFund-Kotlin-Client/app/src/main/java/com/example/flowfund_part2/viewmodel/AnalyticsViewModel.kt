
package com.example.flowfund_part2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowfund_part2.data.local.TradeDao
import com.example.flowfund_part2.utils.TradeAnalytics
import com.example.flowfund_part2.utils.calculateAnalyticsFromTrades
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

sealed interface AnalyticsUiState {
    data object Loading : AnalyticsUiState
    data object Empty : AnalyticsUiState
    data class Success(val analytics: TradeAnalytics) : AnalyticsUiState
    data class Error(val message: String) : AnalyticsUiState
}

class AnalyticsViewModel(
    private val tradeDao: TradeDao
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)

    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadAnalytics()
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            tradeDao.getAllTrades()
                .onStart {
                    _uiState.value = AnalyticsUiState.Loading
                }
                .catch {
                    _uiState.value = AnalyticsUiState.Error(
                        "Unable to load trades. Please try again."
                    )
                }
                .collect { trades ->
                    if (trades.isEmpty()) {
                        _uiState.value = AnalyticsUiState.Empty
                    } else {
                        _uiState.value = AnalyticsUiState.Success(
                            calculateAnalyticsFromTrades(trades)
                        )
                    }
                }
        }
    }
}
