
package com.example.flowfund_part2.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium
            )

            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
        }

        HorizontalDivider()

        if (uiState.isLoading) {
            CircularProgressIndicator()
            Text("Loading your preferences...")
        } else {
            Text(
                "Language",
                style = MaterialTheme.typography.titleMedium
            )

            SettingsDropdown(
                label = "Preferred language",
                selected = settings.preferredLanguage,
                options = listOf(
                    "en" to "English",
                    "zu" to "isiZulu",
                    "af" to "Afrikaans"
                ),
                onSelected = {
                    viewModel.updateSettings(
                        settings.copy(preferredLanguage = it)
                    )
                }
            )

            Text(
                "Currency",
                style = MaterialTheme.typography.titleMedium
            )

            SettingsDropdown(
                label = "Preferred currency",
                selected = settings.preferredCurrency,
                options = listOf(
                    "ZAR" to "ZAR - South African Rand",
                    "USD" to "USD - US Dollar",
                    "EUR" to "EUR - Euro"
                ),
                onSelected = {
                    viewModel.updateSettings(
                        settings.copy(preferredCurrency = it)
                    )
                }
            )

            HorizontalDivider()

            SettingsSwitch(
                title = "Notifications",
                checked = settings.notificationsEnabled,
                onCheckedChange = {
                    viewModel.updateSettings(
                        settings.copy(notificationsEnabled = it)
                    )
                }
            )

            SettingsSwitch(
                title = "Biometric unlock",
                checked = settings.biometricEnabled,
                onCheckedChange = {
                    viewModel.updateSettings(
                        settings.copy(biometricEnabled = it)
                    )
                }
            )

            Text(
                text = if (settings.biometricEnabled) {
                    "Biometric unlock is enabled. Save your settings. " +
                            "The app will request biometric authentication " +
                            "when you reopen FlowFund."
                } else {
                    "Biometric unlock is disabled. FlowFund will open " +
                            "without requesting biometric authentication."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            Text(
                "Remind me before a due date",
                style = MaterialTheme.typography.titleMedium
            )

            SettingsDropdown(
                label = "Reminder timing",
                selected = settings.reminderHoursBefore.toString(),
                options = listOf(
                    "1" to "1 hour before",
                    "6" to "6 hours before",
                    "12" to "12 hours before",
                    "24" to "24 hours before",
                    "48" to "48 hours before"
                ),
                onSelected = {
                    viewModel.updateSettings(
                        settings.copy(
                            reminderHoursBefore = it.toInt()
                        )
                    )
                }
            )

            uiState.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }

            uiState.savedMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = { viewModel.saveSettings() },
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (uiState.isSaving) {
                        "Saving..."
                    } else {
                        "Save Settings"
                    }
                )
            }

            OutlinedButton(
                onClick = { viewModel.loadSettings() },
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reload saved settings")
            }
        }

        if (!uiState.isLoading && uiState.error != null) {
            Button(
                onClick = { viewModel.loadSettings() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Retry")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title)

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsDropdown(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedLabel = options
        .firstOrNull { it.first == selected }
        ?.second ?: selected

    Column {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("$label: $selectedLabel")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.second) },
                    onClick = {
                        onSelected(option.first)
                        expanded = false
                    }
                )
            }
        }
    }
}
