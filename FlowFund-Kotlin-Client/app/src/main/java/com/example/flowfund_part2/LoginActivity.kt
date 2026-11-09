
package com.example.flowfund_part2

import android.content.Intent
import com.example.flowfund_part2.data.repository.SyncResult
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

/**
 * Handles Google sign-in and Firebase authentication for FlowFund.
 *
 * Displays the login screen, manages the Google sign-in process,
 * and directs successfully authenticated users to the secure unlock screen.
 *
 * Google and Firebase authentication concepts are based on the
 * official Firebase documentation (Google, 2026a).
 */

/**
 * Handles Google sign-in and Firebase authentication for FlowFund.
 *
 * Displays the login screen, manages the Google sign-in process,
 * and directs successfully authenticated users to the secure unlock screen.
 *
 * Google and Firebase authentication concepts are based on the
 * official Firebase documentation (Google, 2026a).
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInButton: LinearLayout
    private lateinit var progressLogin: ProgressBar

    companion object {
        private const val TAG = "FlowFundLogin"
    }

    /**
     * Receives the result of the Google sign-in activity.
     *
     * Retrieves the Google account ID token and passes it to Firebase
     * authentication. Displays an error if sign-in fails or is cancelled.
     */
    private val googleSignInLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == RESULT_OK) {

                val task = GoogleSignIn.getSignedInAccountFromIntent(
                    result.data
                )

                try {
                    val account = task.getResult(ApiException::class.java)
                    val idToken = account.idToken

                    if (idToken != null) {
                        Log.i(TAG, "Google sign-in returned an ID token")
                        signInToFirebase(idToken)
                    } else {
                        Log.e(TAG, "Google sign-in did not return an ID token")
                        showError("Google did not provide an ID token.")
                    }

                } catch (e: ApiException) {
                    Log.e(
                        TAG,
                        "Google sign-in failed with status code ${e.statusCode}",
                        e
                    )
                    showError("Google sign-in failed: ${e.statusCode}")
                }

            } else {
                Log.w(TAG, "Google sign-in was cancelled or did not complete")
                showError("Google sign-in was cancelled.")
            }
        }

    /**
     * Initialises the login screen and configures its user interface.
     *
     * Initialises Firebase Authentication, connects the XML views,
     * and sets a click listener on the Google sign-in button.
     *
     * @param savedInstanceState Previously saved activity state, if available.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Log.d(TAG, "Login screen created")

        auth = FirebaseAuth.getInstance()

        googleSignInButton = findViewById(R.id.googleSignInButton)
        progressLogin = findViewById(R.id.progressLogin)

        googleSignInButton.setOnClickListener {
            Log.d(TAG, "Google sign-in button clicked")
            startGoogleSignIn()
        }
    }

    /**
     * Configures and launches the Google sign-in activity.
     *
     * Requests the user's email and an ID token, then displays the
     * progress indicator while the sign-in process is underway.
     */
    private fun startGoogleSignIn() {
        Log.i(TAG, "Starting Google sign-in")

        val googleSignInOptions = GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(
            this,
            googleSignInOptions
        )

        progressLogin.visibility = View.VISIBLE
        googleSignInButton.isEnabled = false

        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    /**
     * Authenticates the user with Firebase using Google's ID token.
     *
     * Displays a welcome message and opens the secure unlock screen
     * when authentication succeeds. Otherwise, displays an error.
     *
     * @param idToken The Google ID token used to create Firebase credentials.
     */
    private fun signInToFirebase(idToken: String) {
        Log.d(TAG, "Attempting Firebase authentication")

        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->

                progressLogin.visibility = View.GONE
                googleSignInButton.isEnabled = true

                if (task.isSuccessful) {
                    Log.i(TAG, "Firebase authentication successful")

                    Toast.makeText(
                        this,
                        "Welcome to FlowFund!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Exchange the Firebase ID token for the FlowFund API's own tokens, then continue.
                    exchangeForApiSession()

                } else {
                    Log.e(
                        TAG,
                        "Firebase authentication failed",
                        task.exception
                    )

                    showError(
                        task.exception?.localizedMessage
                            ?: "Firebase sign-in failed."
                    )
                }
            }
    }

    /**
     * Displays an error message and restores the login controls.
     *
     * @param message The error message displayed to the user.
     */
    private fun showError(message: String) {
        Log.w(TAG, "Displaying login error")

        progressLogin.visibility = View.GONE
        googleSignInButton.isEnabled = true

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * Gets a fresh Firebase ID token, trades it at POST /auth/sso/verify for the API's access/refresh
     * tokens (stored encrypted), and only then opens the unlock screen. If the API is unreachable the
     * user stays on this screen with a clear message instead of entering an app that cannot sync.
     */
    private fun exchangeForApiSession() {
        val user = auth.currentUser ?: return
        progressLogin.visibility = View.VISIBLE
        googleSignInButton.isEnabled = false

        user.getIdToken(true).addOnCompleteListener(this) { tokenTask ->
            val idToken = tokenTask.result?.token
            if (!tokenTask.isSuccessful || idToken == null) {
                Log.e(TAG, "Could not get Firebase ID token", tokenTask.exception)
                showApiError("Could not read your sign-in. Please try again.")
                return@addOnCompleteListener
            }
            lifecycleScope.launch {
                val error = AppContainer.get(this@LoginActivity).authRepository.signInWithFirebaseToken(idToken)
                if (error == null) {
                    startActivity(Intent(this@LoginActivity, UnlockActivity::class.java))
                    finish()
                } else {
                    showApiError(error)
                }
            }
        }
    }

    private fun showApiError(message: String) {
        progressLogin.visibility = View.GONE
        googleSignInButton.isEnabled = true
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

/*
REFERENCES

Google (2026a) 'Authenticate with Google on Android',
Firebase Authentication. Available at:
https://firebase.google.com/docs/auth/android/google-signin
(Accessed: 3 November 2026).

Google (2026b) 'Get a result from an activity',
Android Developers. Available at:
https://developer.android.com/training/basics/intents/result
(Accessed: 3 November 2026).

Google (2026c) 'Activity lifecycle',
Android Developers. Available at:
https://developer.android.com/guide/components/activities/activity-lifecycle
(Accessed: 3 November 2026).
*/
