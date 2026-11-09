package com.example.flowfund_part2.data.repository

import android.util.Log
import com.example.flowfund_part2.data.local.JournalEntryDao
import com.example.flowfund_part2.data.local.JournalEntryEntity
import com.example.flowfund_part2.data.local.SyncState
import com.example.flowfund_part2.data.remote.FlowFundApi
import com.example.flowfund_part2.data.remote.JournalRequest
import com.example.flowfund_part2.data.remote.JournalResponse
import kotlinx.coroutines.flow.Flow

/** Offline-first repository for journal entries; identical rules to [TradeRepository]. */
class JournalRepository(
    private val journalDao: JournalEntryDao,
    private val api: FlowFundApi
) {
    fun getAllEntries(): Flow<List<JournalEntryEntity>> = journalDao.getAllEntries()

    suspend fun getEntryById(id: Long): JournalEntryEntity? = journalDao.getEntryById(id)

    suspend fun insertEntry(entry: JournalEntryEntity): Long =
        journalDao.insertEntry(entry.copy(syncState = SyncState.PENDING))

    suspend fun updateEntry(entry: JournalEntryEntity) =
        journalDao.updateEntry(entry.copy(syncState = SyncState.PENDING))

    suspend fun deleteEntry(entry: JournalEntryEntity) {
        if (entry.remoteId == null) journalDao.deleteEntry(entry)
        else journalDao.updateEntry(entry.copy(syncState = SyncState.DELETED))
    }

    suspend fun sync(): SyncResult {
        val pushed = syncPending()
        if (pushed is SyncResult.Failure) return pushed
        return refresh()
    }

    suspend fun syncPending(): SyncResult {
        var failure: SyncResult.Failure? = null
        for (row in journalDao.getUnsynced()) {
            try {
                when {
                    row.syncState == SyncState.DELETED -> {
                        val r = api.deleteJournal(row.remoteId!!)
                        if (r.isSuccessful || r.code() == 404) journalDao.deleteEntry(row)
                        else throw retrofit2.HttpException(r)
                    }
                    row.remoteId == null -> {
                        val created = api.createJournal(row.toRequest())
                        journalDao.updateEntry(row.copy(remoteId = created.id, syncState = SyncState.SYNCED))
                    }
                    else -> {
                        api.updateJournal(row.remoteId, row.toRequest())
                        journalDao.updateEntry(row.copy(syncState = SyncState.SYNCED))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Sync of journal entry ${row.id} failed: ${e.message}")
                val f = e.toFailure()
                if (f.isAuth) return f
                failure = failure ?: f
            }
        }
        return failure ?: SyncResult.Success
    }

    suspend fun refresh(): SyncResult = try {
        val remote = api.getJournal()
        val remoteIds = remote.map { it.id }.toSet()
        for (dto in remote) {
            val local = journalDao.getByRemoteId(dto.id) ?: journalDao.getByClientId(dto.clientGeneratedId)
            if (local == null || local.syncState == SyncState.SYNCED) {
                journalDao.insertEntry(dto.toEntity(localId = local?.id ?: 0))
            }
        }
        journalDao.getSynced().filter { it.remoteId !in remoteIds }.forEach { journalDao.deleteEntry(it) }
        Log.i(TAG, "Refreshed ${remote.size} journal entries from the API")
        SyncResult.Success
    } catch (e: Exception) {
        Log.w(TAG, "Journal refresh failed: ${e.message}")
        e.toFailure()
    }

    private companion object { const val TAG = "JournalRepository" }
}

internal fun JournalEntryEntity.toRequest() = JournalRequest(title, content, mood, entryDate, clientGeneratedId)

internal fun JournalResponse.toEntity(localId: Long = 0) = JournalEntryEntity(
    id = localId, title = title, content = content, mood = mood, entryDate = entryDate,
    remoteId = id, clientGeneratedId = clientGeneratedId, syncState = SyncState.SYNCED
)
