
package com.example.flowfund_part2.data.preferences

/**
 * Represents the user's saved FlowFund preferences.
 *
 * These settings control notifications, biometric unlocking,
 * the preferred app language, currency, and contribution reminders.
 *
 * @property notificationsEnabled Whether notifications are enabled.
 * @property biometricEnabled Whether biometric unlocking is enabled.
 * @property preferredLanguage The selected language: English, isiZulu, or Afrikaans.
 * @property preferredCurrency The selected currency: ZAR, USD, or EUR.
 * @property reminderHoursBefore The number of hours before a contribution
 * is due when a reminder should be sent.
 */
data class UserSettings(
    val notificationsEnabled: Boolean = true,
    val biometricEnabled: Boolean = false,
    val preferredLanguage: String = "en",
    val preferredCurrency: String = "ZAR",
    val reminderHoursBefore: Int = 24
)
