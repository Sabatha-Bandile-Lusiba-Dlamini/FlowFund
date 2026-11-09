
package com.example.flowfund_part2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.flowfund_part2.data.preferences.SettingsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


/**
 * Provides secure access to FlowFund using the user's biometric preference.
 *
 * Checks Firebase authentication, loads security settings, and displays
 * a biometric prompt when biometric unlocking is enabled.
 *
 * Biometric authentication follows Android security guidelines
 * (Android Developers, 2026a).
 */
class UnlockActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "FlowFundUnlock"
    }

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var message: TextView
    private lateinit var unlockButton: Button

    private var biometricEnabled = false
    private var authenticationInProgress = false

    /**
     * Initializes the unlock screen and checks the user's authentication status.
     *
     * The Activity lifecycle is used to initialize application components
     * and prepare the user interface (Android Developers, 2026b).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "Unlock screen created")

        firebaseAuth = FirebaseAuth.getInstance()
        settingsRepository = SettingsRepository(applicationContext)

        // Checks whether a Firebase user is currently authenticated.
        // Firebase Authentication provides access to the current user.
        if (firebaseAuth.currentUser == null) {
            Log.w(TAG, "No authenticated user; redirecting to login")
            openLogin()
            return
        }

        Log.d(TAG, "Authenticated user detected")

        showUnlockScreen()
        loadBiometricPreference()
    }

    /**
     * Builds the secure unlock screen and its action buttons.
     *
     * Programmatically creates the user interface using Android views.
     */
    private fun showUnlockScreen() {

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
        }

        val title = TextView(this).apply {
            text = "FlowFund Secure Unlock"
            textSize = 24f
            gravity = Gravity.CENTER
        }

        val message = TextView(this).apply {
            text = "Loading your security settings..."
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
        }

        this.message = message

        unlockButton = Button(this).apply {
            text = "Authenticate"
            isEnabled = false

            // Handles the user's request to authenticate.
            setOnClickListener {
                Log.d(TAG, "Authenticate button clicked")

                if (biometricEnabled) {
                    showBiometricPrompt()
                } else {
                    Log.d(TAG, "Biometric unlocking is disabled")
                }
            }
        }

        val signOutButton = Button(this).apply {
            text = "Sign out"

            // Signs the user out of Firebase Authentication.
            setOnClickListener {
                Log.i(TAG, "User selected sign out")
                firebaseAuth.signOut()
                openLogin()
            }
        }

        layout.addView(title)
        layout.addView(message)
        layout.addView(unlockButton)
        layout.addView(signOutButton)

        setContentView(layout)
    }

    /**
     * Reads the saved biometric preference from DataStore.
     *
     * DataStore uses Kotlin Flow to retrieve stored preferences
     * asynchronously (Android Developers, 2026c).
     *
     * lifecycleScope manages the coroutine according to the Activity
     * lifecycle (Android Developers, 2026d).
     *
     * Navigates to the dashboard when biometric unlocking is disabled.
     * Displays an error message if the preference cannot be loaded.
     */
    private fun loadBiometricPreference() {

        lifecycleScope.launch {
            try {
                // Retrieves the first available settings value.
                val settings = settingsRepository.settings.first()

                biometricEnabled = settings.biometricEnabled

                Log.d(TAG, "Biometric preference loaded")

                if (!biometricEnabled) {
                    Log.i(
                        TAG,
                        "Biometric unlocking is disabled; opening dashboard"
                    )
                    openHome()
                } else {
                    message.text =
                        "Use your fingerprint or supported biometric to unlock FlowFund."

                    unlockButton.isEnabled = true

                    Log.i(TAG, "Biometric unlocking is enabled")
                }

            } catch (exception: Exception) {
                Log.e(TAG, "Unable to load security settings", exception)

                message.text =
                    "Unable to load your security settings. Please try again."

                unlockButton.isEnabled = false
            }
        }
    }

    /**
     * Checks biometric availability and displays the system authentication prompt.
     *
     * BiometricManager checks whether the device supports the required
     * biometric authentication strength (Android Developers, 2026a).
     *
     * BiometricPrompt displays the system authentication interface
     * and handles authentication callbacks (Android Developers, 2026a).
     */
    private fun showBiometricPrompt() {

        if (authenticationInProgress) {
            Log.d(TAG, "Authentication is already in progress")
            return
        }

        val biometricManager = BiometricManager.from(this)

        // Checks whether strong biometric authentication is available.
        when (
            biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            )
        ) {

            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d(TAG, "Strong biometric authentication is available")
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.w(TAG, "No supported biometric is enrolled")
                message.text =
                    "No supported biometric is enrolled. Enable one in your phone settings."
                return
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.w(TAG, "Device does not support strong biometrics")
                message.text =
                    "This device does not support strong biometrics."
                return
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.w(TAG, "Biometric hardware is temporarily unavailable")
                message.text =
                    "Biometric hardware is temporarily unavailable."
                return
            }

            else -> {
                Log.w(TAG, "Biometric authentication is unavailable")
                message.text =
                    "Biometric authentication is unavailable."
                return
            }
        }

        authenticationInProgress = true
        unlockButton.isEnabled = false

        // Creates the biometric authentication prompt.
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {

                /**
                 * Called when biometric authentication succeeds.
                 */
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)

                    Log.i(TAG, "Biometric authentication succeeded")

                    authenticationInProgress = false
                    openHome()
                }

                /**
                 * Called when biometric recognition fails.
                 *
                 * The system prompt remains open for another attempt.
                 */
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()

                    Log.w(
                        TAG,
                        "Biometric recognition failed; another attempt is possible"
                    )

                    message.text =
                        "Biometric not recognized. Try again."
                }

                /**
                 * Called when biometric authentication encounters
                 * an error or the prompt is cancelled.
                 */
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)

                    Log.w(
                        TAG,
                        "Biometric prompt ended with error code $errorCode"
                    )

                    authenticationInProgress = false
                    message.text = errString.toString()
                    unlockButton.isEnabled = true
                }
            }
        )

        // Configures the title and permitted authentication method.
        // BIOMETRIC_STRONG requires a strong biometric authenticator.
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock FlowFund")
            .setSubtitle("Authenticate to access your account")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            )
            .build()

        Log.d(TAG, "Displaying biometric authentication prompt")

        // Displays the system biometric authentication dialog.
        prompt.authenticate(promptInfo)
    }

    /**
     * Opens the dashboard after the required authentication flow succeeds.
     *
     * Uses an explicit Intent to navigate to DashboardActivity.
     */
    private fun openHome() {
        Log.i(TAG, "Opening FlowFund dashboard")

        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    /**
     * Returns the user to the login screen.
     *
     * Uses an explicit Intent to open LoginActivity.
     */
    private fun openLogin() {
        Log.d(TAG, "Opening login screen")

        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}

/*
REFERENCES

Android Developers (2026a) 'Show a biometric authentication dialog',
Android Developers. Available at:
https://developer.android.com/identity/sign-in/biometric-auth
(Accessed: 3 November 2026).

Android Developers (2026b) 'The activity lifecycle',
Android Developers. Available at:
https://developer.android.com/guide/components/activities/activity-lifecycle
(Accessed: 3 November 2026).

Android Developers (2026c) 'DataStore',
Android Developers. Available at:
https://developer.android.com/topic/libraries/architecture/datastore
(Accessed: 3 November 2026).

Android Developers (2026d) 'Use Kotlin coroutines with lifecycle-aware components',
Android Developers. Available at:
https://developer.android.com/topic/libraries/architecture/views/coroutines-views
(Accessed: 3 November 2026).
*/
