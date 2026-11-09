package com.example.flowfund_part2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.flowfund_part2.data.local.JournalEntryEntity
import com.example.flowfund_part2.ui.theme.FLOWFUNDPART2Theme
import com.example.flowfund_part2.viewmodel.JournalViewModel
import com.example.flowfund_part2.viewmodel.JournalViewModelFactory
import java.text.DateFormat
import java.util.Date
import kotlin.getValue

/**
 * Journal screen: list of entries with Loading / Empty / Error states, plus an add/edit dialog.
 * Data comes only from Room via [JournalViewModel]; the API sync happens behind it.
 */
class JournalActivity : ComponentActivity() {

    private val viewModel: JournalViewModel by viewModels {
        JournalViewModelFactory(AppContainer.get(this).journalRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FLOWFUNDPART2Theme {
                val state by viewModel.uiState.collectAsState()
                val syncMessage by viewModel.syncMessage.collectAsState()
                val needsSignIn by viewModel.needsSignIn.collectAsState()

                // Session expired: go back to sign-in instead of failing silently.
                LaunchedEffect(needsSignIn) {
                    if (needsSignIn) {
                        AppContainer.get(this@JournalActivity).authRepository.signOut()
                        startActivity(Intent(this@JournalActivity, LoginActivity::class.java))
                        finish()
                    }
                }

                JournalScreen(
                    state = state,
                    syncMessage = syncMessage,
                    onSave = viewModel::save,
                    onDelete = viewModel::delete,
                    onRetry = viewModel::refresh
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    state: JournalUiState,
    syncMessage: String?,
    onSave: (JournalEntryEntity) -> Unit,
    onDelete: (JournalEntryEntity) -> Unit,
    onRetry: () -> Unit
) {
    // null = dialog closed; an entity (id 0 for new) = dialog open.
    var editing by remember { mutableStateOf<JournalEntryEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = JournalEntryEntity(title = "", content = "")
            }) { Text("+") }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Journal", style = MaterialTheme.typography.headlineMedium)

            if (syncMessage != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(syncMessage, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetry) { Text("Retry") }
                    }
                }
            }

            when (state) {
                JournalUiState.Loading -> Text("Loading your journal...")
                JournalUiState.Empty -> Text("No journal entries yet. Tap + to write your first one.")
                is JournalUiState.Error -> Column {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetry) { Text("Try again") }
                }
                is JournalUiState.Success -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.entries, key = { it.id }) { entry ->
                        Card(Modifier.fillMaxWidth().clickable { editing = entry }) {
                            Column(Modifier.padding(12.dp)) {
                                Text(entry.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    DateFormat.getDateInstance().format(Date(entry.entryDate)) +
                                        if (entry.mood.isNotBlank()) "  -  ${entry.mood}" else "",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(entry.content, maxLines = 3, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { current ->
        var title by remember(current) { mutableStateOf(current.title) }
        var content by remember(current) { mutableStateOf(current.content) }
        var mood by remember(current) { mutableStateOf(current.mood) }
        val valid = title.isNotBlank() && content.isNotBlank()

        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (current.id == 0L) "New entry" else "Edit entry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.Start) {
                    OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(mood, { mood = it }, label = { Text("Mood (optional)") }, singleLine = true)
                    OutlinedTextField(content, { content = it }, label = { Text("What happened?") }, minLines = 3)
                    if (!valid) Text("Title and text are required.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(enabled = valid, onClick = {
                    onSave(current.copy(title = title.trim(), content = content.trim(), mood = mood.trim()))
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = {
                Row2(current, onDelete = { onDelete(it); editing = null }, onCancel = { editing = null })
            }
        )
    }
}

@Composable
private fun Row2(entry: JournalEntryEntity, onDelete: (JournalEntryEntity) -> Unit, onCancel: () -> Unit) {
    androidx.compose.foundation.layout.Row {
        if (entry.id != 0L) TextButton(onClick = { onDelete(entry) }) { Text("Delete") }
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}
