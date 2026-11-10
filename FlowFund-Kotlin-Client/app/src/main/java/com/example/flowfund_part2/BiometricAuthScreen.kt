
package com.example.flowfund_part2

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

@Composable
fun BiometricAuthScreen(
    onAuthenticated: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var message by remember {
        mutableStateOf(
            "Use your fingerprint or supported face recognition to unlock FlowFund."
        )
    }

    var biometricAvailable by remember {
        mutableStateOf(false)
    }

    var isAuthenticating by remember {
        mutableStateOf(false)
    }

    // Check whether strong biometric authentication is available.
    LaunchedEffect(activity) {
        if (activity == null) {
            message = "Unable to open biometric authentication."
            biometricAvailable = false
        } else {
            val result = BiometricManager.from(activity).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            )

            biometricAvailable =
                result == BiometricManager.BIOMETRIC_SUCCESS

            if (!biometricAvailable) {
                message = when (result) {
                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                        "This device does not support biometric authentication."

                    BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                        "Biometric hardware is temporarily unavailable."

                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                        "No suitable biometrics are enrolled. Register a fingerprint or supported face recognition in your phone settings."

                    BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                        "A security update is required before biometrics can be used."

                    BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED ->
                        "This device does not support the required authentication method."

                    else ->
                        "Biometric authentication is currently unavailable."
                }
            } else {
                message = "Your device is ready for biometric authentication."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("FlowFund Secure Unlock")

        Spacer(modifier = Modifier.height(16.dp))

        Text(message)

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            enabled = biometricAvailable && !isAuthenticating &&
                    activity != null,
            onClick = {
                val currentActivity = activity

                if (currentActivity == null) {
                    message = "Unable to open biometric authentication."
                    return@Button
                }

                // Check availability again before opening the prompt.
                val result = BiometricManager.from(currentActivity)
                    .canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG
                    )

                if (result != BiometricManager.BIOMETRIC_SUCCESS) {
                    biometricAvailable = false
                    message = "Biometric authentication is no longer available. Check your phone's security settings."
                    return@Button
                }

                isAuthenticating = true
                message = "Waiting for authentication..."

                val executor =
                    ContextCompat.getMainExecutor(currentActivity)

                val prompt = BiometricPrompt(
                    currentActivity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {

                        override fun onAuthenticationSucceeded(
                            result: BiometricPrompt.AuthenticationResult
                        ) {
                            super.onAuthenticationSucceeded(result)

                            isAuthenticating = false
                            message = "Authentication successful!"

                            // Only successful authentication unlocks the app.
                            onAuthenticated()
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()

                            message =
                                "Biometric not recognised. Try again."
                        }

                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence
                        ) {
                            super.onAuthenticationError(
                                errorCode,
                                errString
                            )

                            isAuthenticating = false

                            message = when (errorCode) {
                                BiometricPrompt.ERROR_USER_CANCELED,
                                BiometricPrompt.ERROR_NEGATIVE_BUTTON ->
                                    "Authentication cancelled. Authenticate to unlock FlowFund."

                                BiometricPrompt.ERROR_LOCKOUT ->
                                    "Too many attempts. Follow your phone's instructions before trying again."

                                BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                                    "Biometric authentication is locked. Follow your phone's security instructions."

                                else -> errString.toString()
                            }

                            // Cancellation and errors do not unlock the app.
                        }
                    }
                )

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock FlowFund")
                    .setSubtitle("Verify your identity")
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG
                    )
                    .setNegativeButtonText("Cancel")
                    .build()

                prompt.authenticate(promptInfo)
            }
        ) {
            Text(
                if (isAuthenticating) {
                    "Authenticating..."
                } else {
                    "Authenticate"
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onBack,
            enabled = !isAuthenticating
        ) {
            Text("Exit FlowFund")
        }
    }
}
