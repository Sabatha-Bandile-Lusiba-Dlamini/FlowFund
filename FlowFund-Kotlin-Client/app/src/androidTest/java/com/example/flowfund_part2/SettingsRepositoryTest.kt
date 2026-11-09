
package com.example.flowfund_part2

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.flowfund_part2.data.preferences.SettingsRepository
import com.example.flowfund_part2.data.preferences.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Tests saving, retrieving, and validating FlowFund user preferences.
 *
 * Instrumentation testing uses the Android application context
 * to test application-related functionality (Android Developers, 2026a).
 */
class SettingsRepositoryTest {

    // Retrieves the application context required by the repository.
    // ApplicationProvider supplies the application context during testing
    // (Android Developers, 2026a).
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val repository: SettingsRepository
        get() = SettingsRepository(context)

    /**
     * Checks that saved settings can be read back with the same values.
     *
     * DataStore is used to store and retrieve application preferences
     * (Android Developers, 2026b).
     */
    @Test
    fun saveSettingsAndReadThemBack() = runBlocking {

        val expected = UserSettings(
            notificationsEnabled = false,
            biometricEnabled = true,
            preferredLanguage = "zu",
            preferredCurrency = "ZAR",
            reminderHoursBefore = 12
        )

        // Saves the user's preferences to the repository.
        repository.saveSettings(expected)

        // Collects the first emitted value from the settings Flow.
        // Kotlin Flow supports asynchronous data streams
        // (Kotlin, 2026a).
        val actual = repository.settings.first()

        // Verifies that the retrieved preferences match the saved values.
        // JUnit assertions are used to verify expected test results
        // (JUnit, 2026).
        assertEquals(expected.notificationsEnabled, actual.notificationsEnabled)
        assertEquals(expected.biometricEnabled, actual.biometricEnabled)
        assertEquals(expected.preferredLanguage, actual.preferredLanguage)
        assertEquals(expected.preferredCurrency, actual.preferredCurrency)
        assertEquals(expected.reminderHoursBefore, actual.reminderHoursBefore)
    }

    /**
     * Checks that an unsupported language is rejected.
     *
     * Exception testing verifies that invalid input is handled correctly
     * (JUnit, 2026).
     */
    @Test
    fun rejectsUnsupportedLanguage() = runBlocking {

        val settings = UserSettings(
            notificationsEnabled = true,
            biometricEnabled = false,
            preferredLanguage = "xx",
            preferredCurrency = "ZAR",
            reminderHoursBefore = 24
        )

        // Verifies that saving an unsupported language throws an exception.
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.saveSettings(settings)
            }
        }

        // Checks that the correct validation message is returned.
        assertEquals(
            "Please select a supported language.",
            exception.message
        )
    }

    /**
     * Checks that an unsupported currency is rejected.
     *
     * JUnit assertThrows verifies the expected exception type
     * (JUnit, 2026).
     */
    @Test
    fun rejectsUnsupportedCurrency() {

        val settings = UserSettings(
            notificationsEnabled = true,
            biometricEnabled = false,
            preferredLanguage = "en",
            preferredCurrency = "GBP",
            reminderHoursBefore = 24
        )

        // Attempts to save an unsupported currency and checks the exception.
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.saveSettings(settings)
            }
        }

        assertEquals(
            "Please select a supported currency.",
            exception.message
        )
    }

    /**
     * Checks that an unsupported reminder interval is rejected.
     *
     * Input validation ensures that users can only save supported
     * reminder intervals.
     */
    @Test
    fun rejectsUnsupportedReminderTime() {

        val settings = UserSettings(
            notificationsEnabled = true,
            biometricEnabled = false,
            preferredLanguage = "en",
            preferredCurrency = "ZAR",
            reminderHoursBefore = 7
        )

        // Verifies that an invalid reminder interval causes an exception.
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.saveSettings(settings)
            }
        }

        assertEquals(
            "Please select a supported reminder time.",
            exception.message
        )
    }
}

/*
REFERENCES

Android Developers (2026a) 'ApplicationProvider',
AndroidX Test API Reference. Available at:
https://developer.android.com/reference/androidx/test/core/app/ApplicationProvider
(Accessed: 3 November 2026).

Android Developers (2026b) 'Save data in a local database using DataStore',
Android Developers. Available at:
https://developer.android.com/topic/libraries/architecture/datastore
(Accessed: 3 November 2026).

JUnit (2026) 'Exception testing',
JUnit 4 Documentation. Available at:
https://github.com/junit-team/junit4/wiki/Exception-testing
(Accessed: 3 November 2026).

Kotlin (2026a) 'Asynchronous Flow',
Kotlin Coroutines Documentation. Available at:
https://kotlinlang.org/docs/flow.html
(Accessed: 3 November 2026).

Kotlin (2026b) 'runBlocking',
kotlinx.coroutines API Reference. Available at:
https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/run-blocking.html
(Accessed: 3 November 2026).
*/
