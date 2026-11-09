
package com.example.flowfund_part2.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a trade record stored in the FlowFund database.
 *
 * The Entity annotation tells Room to create a database table
 * named "trades" for this data class (Android Developers, 2026a).
 */
@Entity(tableName = "trades")
data class TradeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val asset: String,
    val tradeType: String,
    val entryPrice: Double,
    val exitPrice: Double,
    val quantity: Double,
    val tradeDate: Long,
    val notes: String = "",

    /** The API's id for this trade; null until the API has accepted it. */
    val remoteId: Long? = null,

    /** Idempotency key sent on create, so a retried upload can never duplicate the trade on the server. */
    @ColumnInfo(defaultValue = "''")
    val clientGeneratedId: String = UUID.randomUUID().toString(),

    /** One of [SyncState]. */
    @ColumnInfo(defaultValue = "'PENDING'")
    val syncState: String = SyncState.PENDING
)

/*
REFERENCES

Android Developers (2026a) 'Define data using Room entities'.
Available at: https://developer.android.com/training/data-storage/room/defining-data
(Accessed: 3 November 2026).

Android Developers (2026b) 'PrimaryKey'.
Available at: https://developer.android.com/reference/androidx/room/PrimaryKey
(Accessed: 3 November 2026).

Kotlin (2026) 'Data classes'.
Available at: https://kotlinlang.org/docs/data-classes.html
(Accessed: 3 November 2026).
*/
