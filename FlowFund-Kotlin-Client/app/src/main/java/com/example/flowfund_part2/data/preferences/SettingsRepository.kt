
package com.example.flowfund_part2.data.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by
preferencesDataStore(name = "user_settings")

/**
 * Reads and saves FlowFund user preferences using Jetpack DataStore.
 *
 * Stores notification settings, biometric preferences, language,
 * currency, and contribution reminder timing.
 *
 * @param context The context used to access DataStore.
 */
class SettingsRepository(private val context: Context) {

    private companion object {
        const val TAG = "SettingsRepository"

        val SUPPORTED_LANGUAGES = listOf("en", "zu", "af")
        val SUPPORTED_CURRENCIES = listOf("ZAR", "USD", "EUR")
        val SUPPORTED_REMINDER_HOURS = listOf(1, 6, 12, 24, 48)
    }

    /**
     * Contains the keys used to store individual preferences.
     */
    private object Keys {
        val NOTIFICATIONS_ENABLED =
            booleanPreferencesKey("notifications_enabled")
        val BIOMETRIC_ENABLED =
            booleanPreferencesKey("biometric_enabled")
        val LANGUAGE =
            stringPreferencesKey("preferred_language")
        val CURRENCY =
            stringPreferencesKey("preferred_currency")
        val REMINDER_HOURS =
            intPreferencesKey("reminder_hours_before")
    }

    /**
     * Emits the current settings whenever DataStore preferences change.
     *
     * An IOException results in default preferences being emitted.
     * Unexpected exceptions are logged and rethrown.
     */
    val settings: Flow<UserSettings> =
        context.settingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Log.w(
                        TAG,
                        "Could not read settings; using default preferences",
                        exception
                    )
                    emit(emptyPreferences())
                } else {
                    Log.e(
                        TAG,
                        "Unexpected error while reading settings",
                        exception
                    )
                    throw exception
                }
            }
            .map { preferences ->
                UserSettings(
                    notificationsEnabled =
                        preferences[Keys.NOTIFICATIONS_ENABLED] ?: true,
                    biometricEnabled =
                        preferences[Keys.BIOMETRIC_ENABLED] ?: false,
                    preferredLanguage =
                        preferences[Keys.LANGUAGE] ?: "en",
                    preferredCurrency =
                        preferences[Keys.CURRENCY] ?: "ZAR",
                    reminderHoursBefore =
                        preferences[Keys.REMINDER_HOURS] ?: 24
                )
            }

    /**
     * Validates and saves user preferences to DataStore.
     *
     * @param settings The preferences to validate and persist.
     * @throws IllegalArgumentException if a preference value is unsupported.
     */
    suspend fun saveSettings(settings: UserSettings) {

        // Reject unsupported language codes.
        require(settings.preferredLanguage in SUPPORTED_LANGUAGES) {
            "Please select a supported language."
        }

        // Reject unsupported currency codes.
        require(settings.preferredCurrency in SUPPORTED_CURRENCIES) {
            "Please select a supported currency."
        }

        // Allow only the reminder intervals offered by the app.
        require(settings.reminderHoursBefore in SUPPORTED_REMINDER_HOURS) {
            "Please select a supported reminder time."
        }

        try {
            // Save all settings together in one DataStore transaction.
            context.settingsDataStore.edit { preferences ->
                preferences[Keys.NOTIFICATIONS_ENABLED] =
                    settings.notificationsEnabled
                preferences[Keys.BIOMETRIC_ENABLED] =
                    settings.biometricEnabled
                preferences[Keys.LANGUAGE] =
                    settings.preferredLanguage
                preferences[Keys.CURRENCY] =
                    settings.preferredCurrency
                preferences[Keys.REMINDER_HOURS] =
                    settings.reminderHoursBefore
            }

            Log.i(TAG, "User settings saved successfully")
        } catch (exception: Exception) {
            // Log the failure and allow the calling code to handle it.
            Log.e(TAG, "Failed to save user settings", exception)
            throw exception
        }
    }
}
