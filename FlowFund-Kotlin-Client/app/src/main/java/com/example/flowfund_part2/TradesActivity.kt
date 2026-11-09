package com.example.flowfund_part2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.flowfund_part2.data.local.TradeEntity
import com.example.flowfund_part2.ui.theme.FLOWFUNDPART2Theme
import com.example.flowfund_part2.utils.TradeInput
import com.example.flowfund_part2.utils.TradeInputResult
import com.example.flowfund_part2.utils.calculateTradeProfitLoss
import com.example.flowfund_part2.viewmodel.TradesViewModel
import com.example.flowfund_part2.viewmodel.TradesViewModelFactory
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Trades screen. It reads only from Room through [TradesViewModel] (Loading / Empty / Success / Error) and
 * shows the API sync problem in a banner. No sample trades exist anywhere in this flow.
 */
class TradesActivity : ComponentActivity() {

    private val viewModel: TradesViewModel by viewModels {
        TradesViewModelFactory(AppContainer.get(this).tradeRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FLOWFUNDPART2Theme {
                val state by viewModel.uiState.collectAsState()
                val syncMessage by viewModel.syncMessage.collectAsState()
                val needsSignIn by viewModel.needsSignIn.collectAsState()

                LaunchedEffect(needsSignIn) {
                    if (needsSignIn) {
                        AppContainer.get(this@TradesActivity).authRepository.signOut()
                        startActivity(Intent(this@TradesActivity, LoginActivity::class.java))
                        finish()
                    }
                }

                TradesScreen(
                    state = state,
                    syncMessage = syncMessage,
                    onSave = { trade -> if (trade.id == 0L) viewModel.addTrade(trade) else viewModel.updateTrade(trade) },
                    onDelete = viewModel::deleteTrade,
                    onRetry = viewModel::refresh
                )
            }
        }
    }
}

@Composable
fun TradesScreen(
    state: TradesUiState,
    syncMessage: String?,
    onSave: (TradeEntity) -> Unit,
    onDelete: (TradeEntity) -> Unit,
    onRetry: () -> Unit
) {
    var editing by remember { mutableStateOf<TradeEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = TradeEntity(
                    title = "", asset = "", tradeType = "BUY", entryPrice = 0.0, exitPrice = 0.0,
                    quantity = 0.0, tradeDate = System.currentTimeMillis()
                )
            }) { Text("+") }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Trades", style = MaterialTheme.typography.headlineMedium)

            if (syncMessage != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(syncMessage, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetry) { Text("Retry") }
                    }
                }
            }

            when (state) {
                TradesUiState.Loading -> Text("Loading your trades...")
                TradesUiState.Empty -> Text("No trades yet. Tap + to record your first trade.")
                is TradesUiState.Error -> Column {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetry) { Text("Try again") }
                }
                is TradesUiState.Success -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.trades, key = { it.id }) { trade ->
                        val pl = calculateTradeProfitLoss(trade)
                        Card(Modifier.fillMaxWidth().clickable { editing = trade }) {
                            Column(Modifier.padding(12.dp)) {
                                Text("${trade.title}  (${trade.tradeType} ${trade.asset})", style = MaterialTheme.typography.titleMedium)
                                Text(DateFormat.getDateInstance().format(Date(trade.tradeDate)), style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "P/L: " + String.format(Locale.US, "%+.2f", pl),
                                    color = if (pl >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { current ->
        var title by remember(current) { mutableStateOf(current.title) }
        var asset by remember(current) { mutableStateOf(current.asset) }
        var type by remember(current) { mutableStateOf(current.tradeType) }
        var entry by remember(current) { mutableStateOf(if (current.id == 0L) "" else current.entryPrice.toString()) }
        var exit by remember(current) { mutableStateOf(if (current.id == 0L) "" else current.exitPrice.toString()) }
        var qty by remember(current) { mutableStateOf(if (current.id == 0L) "" else current.quantity.toString()) }
        var notes by remember(current) { mutableStateOf(current.notes) }
        var error by remember(current) { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (current.id == 0L) "New trade" else "Edit trade") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(asset, { asset = it }, label = { Text("Asset (e.g. BTC)") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = type == "BUY", onClick = { type = "BUY" }, label = { Text("Buy") })
                        FilterChip(selected = type == "SELL", onClick = { type = "SELL" }, label = { Text("Sell") })
                    }
                    OutlinedTextField(entry, { entry = it }, label = { Text("Entry price") }, singleLine = true)
                    OutlinedTextField(exit, { exit = it }, label = { Text("Exit price") }, singleLine = true)
                    OutlinedTextField(qty, { qty = it }, label = { Text("Quantity") }, singleLine = true)
                    OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") })
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when (val r = TradeInput.parse(current, title, asset, type, entry, exit, qty, notes)) {
                        is TradeInputResult.Invalid -> error = r.message
                        is TradeInputResult.Valid -> { onSave(r.trade); editing = null }
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    if (current.id != 0L) TextButton(onClick = { onDelete(current); editing = null }) { Text("Delete") }
                    TextButton(onClick = { editing = null }) { Text("Cancel") }
                }
            }
        )
    }
}
