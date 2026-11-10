package com.example.flowfund_part2

import com.example.flowfund_part2.data.local.JournalEntryEntity

/** What the journal screen can show. There is no sample-entry fallback: empty means empty. */
sealed interface JournalUiState {
    data object Loading : JournalUiState
    data object Empty : JournalUiState
    data class Success(val entries: List<JournalEntryEntity>) : JournalUiState
    data class Error(val message: String) : JournalUiState
}
