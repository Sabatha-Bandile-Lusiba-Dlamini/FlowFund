
package com.example.flowfund_part2

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Displays the main FlowFund dashboard.
 *
 * Provides navigation to notifications, stokvel group settings,
 * and the upcoming payout schedule.
 *
 * The Activity structure follows Android development guidelines
 * (Google, 2026a).
 */
class DashboardActivity : AppCompatActivity() {

    /**
     * Initializes the dashboard and configures its navigation controls.
     *
     * The onCreate() method is used to initialize an Activity
     * and load its user interface (Google, 2026a).
     *
     * @param savedInstanceState Previously saved activity state, if available.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Opens the missed payments screen when notifications are selected.
        // Click listeners handle user interaction (Google, 2026b).
        findViewById<android.view.View>(R.id.btnNotifications)
            .setOnClickListener {
                openScreen(MissedPaymentsActivity::class.java)
            }

        // Opens group settings when the user selects "View all groups".
        findViewById<TextView>(R.id.txtViewAllGroups)
            .setOnClickListener {
                openScreen(GroupSettingsActivity::class.java)
            }

        // Opens settings for the Family Stokvel group.
        findViewById<LinearLayout>(R.id.cardFamilyStokvel)
            .setOnClickListener {
                openGroupSettings("Family Stokvel")
            }

        // Opens settings for the Friends Fund group.
        findViewById<LinearLayout>(R.id.cardFriendsFund)
            .setOnClickListener {
                openGroupSettings("Friends Fund")
            }


        // Opens the API-synced trades and journal screens.
        findViewById<android.view.View>(R.id.btnTrades)
            .setOnClickListener { openScreen(TradesActivity::class.java) }

        findViewById<android.view.View>(R.id.btnJournal)
            .setOnClickListener { openScreen(JournalActivity::class.java) }

        // Opens the payout schedule from the upcoming contribution card.
        findViewById<LinearLayout>(R.id.cardUpcomingContribution)
            .setOnClickListener {
                openScreen(PayoutScheduleActivity::class.java)
            }
    }

    /**
     * Opens the specified activity.
     *
     * Explicit Intents are used to navigate between activities
     * within the application (Google, 2026c).
     *
     * @param destination The activity class to open.
     */
    private fun openScreen(destination: Class<*>) {
        startActivity(Intent(this, destination))
    }

    /**
     * Opens group settings for the selected stokvel group.
     *
     * An Intent extra passes the selected group name
     * to the destination Activity (Google, 2026c).
     *
     * @param groupName The name of the group whose settings should be opened.
     */
    private fun openGroupSettings(groupName: String) {
        val intent = Intent(this, GroupSettingsActivity::class.java)

        // Passes the selected group name to the destination activity.
        intent.putExtra("group_name", groupName)

        startActivity(intent)
    }
}

/*
REFERENCES

Google (2026a) 'Activity lifecycle', Android Developers.
Available at: https://developer.android.com/guide/components/activities/activity-lifecycle
(Accessed: 3 November 2026).

Google (2026b) 'Input events overview', Android Developers.
Available at: https://developer.android.com/develop/ui/views/touch-and-input/input-events
(Accessed: 3 November 2026).

Google (2026c) 'Intents and intent filters', Android Developers.
Available at: https://developer.android.com/guide/components/intents-filters
(Accessed: 3 November 2026).
*/
