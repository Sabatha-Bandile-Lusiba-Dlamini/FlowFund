package com.example.flowfund_part2.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalEntryDao {

    @Query("SELECT * FROM journal_entries WHERE syncState != 'DELETED' ORDER BY entryDate DESC")
    fun getAllEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :entryId")
    suspend fun getEntryById(entryId: Long): JournalEntryEntity?

    @Query("SELECT * FROM journal_entries WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: Long): JournalEntryEntity?

    @Query("SELECT * FROM journal_entries WHERE clientGeneratedId = :clientId LIMIT 1")
    suspend fun getByClientId(clientId: String): JournalEntryEntity?

    @Query("SELECT * FROM journal_entries WHERE syncState != 'SYNCED'")
    suspend fun getUnsynced(): List<JournalEntryEntity>

    @Query("SELECT * FROM journal_entries WHERE syncState = 'SYNCED' AND remoteId IS NOT NULL")
    suspend fun getSynced(): List<JournalEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: JournalEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: JournalEntryEntity)

    @Query("DELETE FROM journal_entries")
    suspend fun deleteAllEntries()
}
