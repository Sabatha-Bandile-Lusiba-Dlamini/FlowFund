package com.example.flowfund_part2.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val mood: String = "",
    val entryDate: Long = System.currentTimeMillis(),

    val remoteId: Long? = null,

    @ColumnInfo(defaultValue = "''")
    val clientGeneratedId: String = UUID.randomUUID().toString(),

    @ColumnInfo(defaultValue = "'PENDING'")
    val syncState: String = SyncState.PENDING
)
