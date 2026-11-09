package com.example.flowfund_part2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.flowfund_part2.JournalUiState
import com.example.flowfund_part2.data.local.JournalEntryEntity
import com.example.flowfund_part2.data.repository.JournalRepository
import com.example.flowfund_part2.data.repository.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Drives the journal screen from Room; every change is saved locally first, then synced to the API. */
class JournalViewModel(private val repository: JournalRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<JournalUiState>(JournalUiState.Loading)
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _needsSignIn = MutableStateFlow(false)
    val needsSignIn: StateFlow<Boolean> = _needsSignIn.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllEntries()
                .catch { _uiState.value = JournalUiState.Error("Unable to load journal entries. Please try again.") }
                .collect { list ->
                    _uiState.value = if (list.isEmpty()) JournalUiState.Empty else JournalUiState.Success(list)
                }
        }
        refresh()
    }

    fun refresh() { viewModelScope.launch { report(repository.sync()) } }

    fun save(entry: JournalEntryEntity) = launchAndSync {
        if (entry.id == 0L) repository.insertEntry(entry) else repository.updateEntry(entry)
    }

    fun delete(entry: JournalEntryEntity) = launchAndSync { repository.deleteEntry(entry) }

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

class JournalViewModelFactory(private val repository: JournalRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(JournalViewModel::class.java)) { "Unknown ViewModel: $modelClass" }
        return JournalViewModel(repository) as T
    }
}
