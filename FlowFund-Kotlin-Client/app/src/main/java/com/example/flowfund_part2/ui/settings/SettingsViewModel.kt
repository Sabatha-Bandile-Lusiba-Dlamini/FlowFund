
package com.example.flowfund_part2.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowfund_part2.data.preferences.SettingsRepository
import com.example.flowfund_part2.data.preferences.UserSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val savedMessage: String? = null
)

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var loadingJob: Job? = null

    init {
        loadSettings()
    }

    fun loadSettings() {
        loadingJob?.cancel()

        loadingJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            repository.settings
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message
                                ?: "Could not load settings."
                        )
                    }
                }
                .collect { settings ->
                    _uiState.update {
                        it.copy(
                            settings = settings,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    fun updateSettings(settings: UserSettings) {
        _uiState.update {
            it.copy(
                settings = settings,
                error = null,
                savedMessage = null
            )
        }
    }

    fun saveSettings() {
        val settingsToSave = _uiState.value.settings

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    error = null,
                    savedMessage = null
                )
            }

            try {
                repository.saveSettings(settingsToSave)

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        savedMessage = "Settings saved successfully."
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = exception.message
                            ?: "Could not save settings."
                    )
                }
            }
        }
    }
}
