package com.example.flowfund_part2.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Main Room database for FlowFund: trades and journal entries (Android Developers, 2026a).
 *
 * Version 2 adds the sync columns (remoteId, clientGeneratedId, syncState). [MIGRATION_1_2] keeps every
 * trade and entry users already have; Room is never allowed to wipe local data.
 */
@Database(
    entities = [TradeEntity::class, JournalEntryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tradeDao(): TradeDao
    abstract fun journalEntryDao(): JournalEntryDao

    companion object {

        /**
         * Adds the sync columns. Existing rows were created before the API existed, so they are marked
         * PENDING and will be uploaded by the next sync. Each gets its own random clientGeneratedId
         * (UUID-shaped, built in SQL) so the server can de-duplicate retries
         * (Android Developers, 2026b).
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                for (table in listOf("trades", "journal_entries")) {
                    connection.execSQL("ALTER TABLE $table ADD COLUMN remoteId INTEGER")
                    connection.execSQL("ALTER TABLE $table ADD COLUMN clientGeneratedId TEXT NOT NULL DEFAULT ''")
                    connection.execSQL("ALTER TABLE $table ADD COLUMN syncState TEXT NOT NULL DEFAULT 'PENDING'")
                    connection.execSQL(
                        "UPDATE $table SET clientGeneratedId = " +
                                "lower(hex(randomblob(4))) || '-' || lower(hex(randomblob(2))) || '-4' || " +
                                "substr(lower(hex(randomblob(2))),2) || '-a' || " +
                                "substr(lower(hex(randomblob(2))),2) || '-' || lower(hex(randomblob(6)))"
                    )
                }
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flowfund_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

/*
REFERENCES

Android Developers (2026a) 'Save data in a local database using Room'.
Available at: https://developer.android.com/training/data-storage/room
(Accessed: 4 November 2026).

Android Developers (2026b) 'Migrate your Room database'.
Available at: https://developer.android.com/training/data-storage/room/migrating-db-versions
(Accessed: 4 November 2026).
*/
