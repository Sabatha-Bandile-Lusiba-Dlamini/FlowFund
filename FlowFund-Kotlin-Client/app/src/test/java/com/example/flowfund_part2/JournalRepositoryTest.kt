package com.example.flowfund_part2

import com.example.flowfund_part2.data.local.JournalEntryEntity
import com.example.flowfund_part2.data.local.SyncState
import com.example.flowfund_part2.data.remote.JournalResponse
import com.example.flowfund_part2.data.repository.JournalRepository
import com.example.flowfund_part2.data.repository.SyncResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

/** Runs the real JournalRepository against a fake DAO and fake API. */
class JournalRepositoryTest {

    private val dao = FakeJournalDao()
    private val api = FakeApi()
    private val repo = JournalRepository(dao, api)

    private fun entry(title: String = "Day 1") = JournalEntryEntity(title = title, content = "Notes", entryDate = 1000L)
    private fun remote(id: Long, client: String) = JournalResponse(id, "R$id", "Text", "Calm", 2000L, client)

    @Test fun emptyEverywhere_isEmptyList_notSampleData() = runBlocking {
        assertEquals(SyncResult.Success, repo.sync())
        assertTrue(repo.getAllEntries().first().isEmpty())
    }

    @Test fun newEntry_isPending_thenSyncedWithRemoteIdAndClientId() = runBlocking {
        repo.insertEntry(entry())
        assertEquals(SyncState.PENDING, dao.rows.value.single().syncState)

        assertEquals(SyncResult.Success, repo.syncPending())
        val row = dao.rows.value.single()
        assertEquals(SyncState.SYNCED, row.syncState)
        assertEquals(100L, row.remoteId)
        assertEquals(row.clientGeneratedId, api.journalCreated.single().clientGeneratedId)
    }

    @Test fun offline_keepsEntryLocally_withConnectionMessage() = runBlocking {
        api.failWith = IOException("offline")
        repo.insertEntry(entry())
        val result = repo.syncPending() as SyncResult.Failure
        assertTrue(result.message.contains("No connection"))
        assertFalse(result.isAuth)
        assertEquals(SyncState.PENDING, dao.rows.value.single().syncState)
    }

    @Test fun refresh_importsServerEntries() = runBlocking {
        api.remoteJournal = listOf(remote(7, "c-7"))
        repo.refresh()
        val row = dao.rows.value.single()
        assertEquals(7L, row.remoteId)
        assertEquals(SyncState.SYNCED, row.syncState)
    }

    @Test fun refresh_keepsUnsyncedLocalEdit() = runBlocking {
        val id = repo.insertEntry(entry("My edit").copy(remoteId = 7, clientGeneratedId = "c-7"))
        api.remoteJournal = listOf(remote(7, "c-7"))
        repo.refresh()
        assertEquals("My edit", dao.getEntryById(id)!!.title)
    }

    @Test fun refresh_removesSyncedEntriesDeletedOnServer() = runBlocking {
        api.remoteJournal = listOf(remote(7, "c-7"))
        repo.refresh()
        api.remoteJournal = emptyList()
        repo.refresh()
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test fun editingSyncedEntry_marksPending_thenUpdatesRemote() = runBlocking {
        api.remoteJournal = listOf(remote(7, "c-7"))
        repo.refresh()
        repo.updateEntry(dao.rows.value.single().copy(title = "Changed"))
        assertEquals(SyncState.PENDING, dao.rows.value.single().syncState)
        repo.syncPending()
        assertEquals(SyncState.SYNCED, dao.rows.value.single().syncState)
        assertEquals("Changed", dao.rows.value.single().title)
    }

    @Test fun deleteNeverSyncedEntry_skipsApi() = runBlocking {
        val id = repo.insertEntry(entry())
        repo.deleteEntry(dao.getEntryById(id)!!)
        assertTrue(dao.rows.value.isEmpty())
        assertTrue(api.journalDeleted.isEmpty())
    }

    @Test fun deleteSyncedEntry_hiddenThenRemovedAfterApiConfirms() = runBlocking {
        api.remoteJournal = listOf(remote(7, "c-7"))
        repo.refresh()
        repo.deleteEntry(dao.rows.value.single())
        assertTrue(repo.getAllEntries().first().isEmpty())
        assertEquals(1, dao.rows.value.size)
        repo.syncPending()
        assertEquals(listOf(7L), api.journalDeleted)
        assertTrue(dao.rows.value.isEmpty())
    }
}
