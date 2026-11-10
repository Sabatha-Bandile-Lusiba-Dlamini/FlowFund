package com.example.flowfund_part2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowfund_part2.TradesUiState
import com.example.flowfund_part2.data.local.TradeEntity
import com.example.flowfund_part2.data.repository.SyncResult
import com.example.flowfund_part2.data.repository.TradeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Drives the trades list. It shows exactly what Room holds: Loading, Empty (no trades), Success, or
 * Error. There is no sample-data fallback: if the API is unreachable and Room is empty, the user sees
 * the empty state plus [syncMessage], never invented trades.
 */
class TradesViewModel(private val repository: TradeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<TradesUiState>(TradesUiState.Loading)
    val uiState: StateFlow<TradesUiState> = _uiState.asStateFlow()

    /** Latest sync problem for a banner/snackbar; null when the last sync worked. */
    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    /** Becomes true when the API says the session expired, so the UI can send the user to sign in. */
    private val _needsSignIn = MutableStateFlow(false)
    val needsSignIn: StateFlow<Boolean> = _needsSignIn.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllTrades()
                .catch { _uiState.value = TradesUiState.Error("Unable to load trades. Please try again.") }
                .collect { trades ->
                    _uiState.value = if (trades.isEmpty()) TradesUiState.Empty else TradesUiState.Success(trades)
                }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { report(repository.sync()) }
    }

    fun addTrade(trade: TradeEntity) = launchAndSync { repository.insertTrade(trade) }
    fun updateTrade(trade: TradeEntity) = launchAndSync { repository.updateTrade(trade) }
    fun deleteTrade(trade: TradeEntity) = launchAndSync { repository.deleteTrade(trade) }

    private fun launchAndSync(change: suspend () -> Unit) {
        viewModelScope.launch {
            change()
            report(repository.syncPending())
        }
    }

    private fun report(result: SyncResult) {
        when (result) {
            SyncResult.Success -> _syncMessage.value = null
            is SyncResult.Failure -> {
                _syncMessage.value = result.message
                if (result.isAuth) _needsSignIn.value = true
            }
        }
    }
}
