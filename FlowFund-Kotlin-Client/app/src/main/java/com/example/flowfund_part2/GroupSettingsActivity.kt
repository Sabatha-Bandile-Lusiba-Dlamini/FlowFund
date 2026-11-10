
package com.example.flowfund_part2

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

/**
 * Manages the settings for an individual FlowFund stokvel group.
 *
 * Allows users to edit group details, contribution amounts, payment
 * frequency, currency, cycle start date, and member permissions.
 * Settings are stored locally using SharedPreferences.
 */
class GroupSettingsActivity : AppCompatActivity() {

    private lateinit var groupName: EditText
    private lateinit var contributionAmount: EditText
    private lateinit var frequency: Spinner
    private lateinit var currency: Spinner
    private lateinit var cycleStartDate: EditText

    private lateinit var memberContributions: Switch
    private lateinit var memberMeetings: Switch
    private lateinit var memberInvite: Switch

    private lateinit var preferences: android.content.SharedPreferences

    private var selectedGroup = "Default Group"

    /**
     * Initializes the group settings screen and configures its controls.
     *
     * Retrieves the selected group name, connects the XML views,
     * loads saved settings, and registers button click listeners.
     *
     * @param savedInstanceState Previously saved activity state, if available.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_settings)

        // Retrieve the group selected on the dashboard.
        selectedGroup = intent.getStringExtra("group_name")
            ?: "Default Group"

        // Open the local preference file used to store group settings.
        preferences = getSharedPreferences(
            "FlowFundGroupSettings",
            MODE_PRIVATE
        )

        // Connect the XML fields to their corresponding variables.
        groupName = findViewById(R.id.edtGroupName)
        contributionAmount =
            findViewById(R.id.edtContributionAmount)
        frequency = findViewById(R.id.spinnerFrequency)
        currency = findViewById(R.id.spinnerCurrency)
        cycleStartDate = findViewById(R.id.edtCycleStartDate)

        memberContributions =
            findViewById(R.id.switchMemberContributions)
        memberMeetings =
            findViewById(R.id.switchMemberMeetings)
        memberInvite = findViewById(R.id.switchMemberInvite)

        // Configure the available contribution frequencies.
        setupSpinner(
            frequency,
            listOf("Weekly", "Monthly", "Quarterly")
        )

        // Configure the supported currencies.
        setupSpinner(
            currency,
            listOf("ZAR", "USD", "EUR")
        )

        // Populate the screen with previously saved group settings.
        loadSettings()

        // Return to the previous screen.
        findViewById<ImageButton>(R.id.btnBack)
            .setOnClickListener {
                finish()
            }

        // Open the date picker when the cycle start date is selected.
        cycleStartDate.setOnClickListener {
            showDatePicker()
        }

        // Explain the group's contribution rules.
        findViewById<LinearLayout>(
            R.id.layoutContributionRules
        ).setOnClickListener {
            showInfo(
                "Contribution Rules",
                "Set and communicate how much members contribute, " +
                        "when payments are due, and how contributions " +
                        "are recorded."
            )
        }

        // Explain how the group handles late contributions.
        findViewById<LinearLayout>(
            R.id.layoutLatePaymentRules
        ).setOnClickListener {
            showInfo(
                "Late Payment Rules",
                "Agree on payment deadlines and any consequences " +
                        "for late contributions with your group members."
            )
        }

        // Validate and save the updated settings.
        findViewById<Button>(
            R.id.btnSaveGroupSettings
        ).setOnClickListener {
            saveSettings()
        }

        // Ask for confirmation before attempting to leave the group.
        findViewById<Button>(
            R.id.btnLeaveGroup
        ).setOnClickListener {
            confirmLeaveGroup()
        }
    }

    /**
     * Populates a spinner with the supplied options.
     *
     * @param spinner The dropdown control to configure.
     * @param options The values displayed in the dropdown.
     */
    private fun setupSpinner(
        spinner: Spinner,
        options: List<String>
    ) {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            options
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinner.adapter = adapter
    }

    /**
     * Loads the selected group's saved settings from SharedPreferences.
     *
     * Uses default values when a setting has not previously been saved.
     */
    private fun loadSettings() {
        val key = selectedGroup

        groupName.setText(
            preferences.getString(
                "${key}_name",
                selectedGroup.takeUnless {
                    it == "Default Group"
                } ?: ""
            )
        )

        contributionAmount.setText(
            preferences.getString("${key}_amount", "")
        )

        selectSpinnerValue(
            frequency,
            preferences.getString("${key}_frequency", "Monthly")
                ?: "Monthly"
        )

        selectSpinnerValue(
            currency,
            preferences.getString("${key}_currency", "ZAR")
                ?: "ZAR"
        )

        cycleStartDate.setText(
            preferences.getString("${key}_date", "")
        )

        // Restore the member permissions, using the existing defaults.
        memberContributions.isChecked =
            preferences.getBoolean("${key}_view_contributions", true)

        memberMeetings.isChecked =
            preferences.getBoolean("${key}_view_meetings", true)

        memberInvite.isChecked =
            preferences.getBoolean("${key}_invite_members", false)
    }

    /**
     * Selects the matching option in a spinner.
     *
     * Leaves the current selection unchanged if the value is not found.
     *
     * @param spinner The dropdown control to update.
     * @param value The option that should be selected.
     */
    private fun selectSpinnerValue(
        spinner: Spinner,
        value: String
    ) {
        val adapter = spinner.adapter

        for (index in 0 until adapter.count) {
            if (adapter.getItem(index).toString() == value) {
                spinner.setSelection(index)
                return
            }
        }
    }

    /**
     * Displays a date picker for the group's contribution cycle start date.
     *
     * Formats the selected date as day/month/year.
     */
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, year, month, day ->
                cycleStartDate.setText(
                    String.format(
                        java.util.Locale.getDefault(),
                        "%02d/%02d/%04d",
                        day,
                        month + 1,
                        year
                    )
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /**
     * Validates and saves the current group settings.
     *
     * Requires a group name, a positive numeric contribution amount,
     * and a selected cycle start date before saving the settings locally.
     * Displays a message when validation fails or saving completes.
     */
    private fun saveSettings() {
        val name = groupName.text.toString().trim()
        val amountText =
            contributionAmount.text.toString().trim()

        // A group name is required.
        if (name.isEmpty()) {
            groupName.error = "Enter a group name"
            groupName.requestFocus()
            return
        }

        // Ensure the contribution amount is numeric and greater than zero.
        val amount = amountText.toDoubleOrNull()

        if (amount == null || amount <= 0) {
            contributionAmount.error =
                "Enter a valid contribution amount"
            contributionAmount.requestFocus()
            return
        }

        // A contribution cycle start date is required.
        if (cycleStartDate.text.toString().isBlank()) {
            cycleStartDate.error = "Select a start date"
            Toast.makeText(
                this,
                "Please select the cycle start date.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val key = selectedGroup

        // Save group details and member permissions locally.
        preferences.edit()
            .putString("${key}_name", name)
            .putString("${key}_amount", amountText)
            .putString(
                "${key}_frequency",
                frequency.selectedItem.toString()
            )
            .putString(
                "${key}_currency",
                currency.selectedItem.toString()
            )
            .putString(
                "${key}_date",
                cycleStartDate.text.toString()
            )
            .putBoolean(
                "${key}_view_contributions",
                memberContributions.isChecked
            )
            .putBoolean(
                "${key}_view_meetings",
                memberMeetings.isChecked
            )
            .putBoolean(
                "${key}_invite_members",
                memberInvite.isChecked
            )
            .apply()

        Toast.makeText(
            this,
            "Group settings saved successfully.",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Displays an informational dialog.
     *
     * @param title The dialog title.
     * @param message The information displayed to the user.
     */
    private fun showInfo(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    /**
     * Requests confirmation before the user leaves the selected group.
     *
     * The current implementation displays a notice because actual group
     * membership removal has not yet been connected to membership data.
     */
    private fun confirmLeaveGroup() {
        AlertDialog.Builder(this)
            .setTitle("Leave Group?")
            .setMessage(
                "Are you sure you want to leave $selectedGroup?"
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Leave") { _, _ ->
                Toast.makeText(
                    this,
                    "Leave Group needs to be connected to " +
                            "your membership data.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .show()
    }
}
