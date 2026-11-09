package com.example.flowfund_part2

import com.example.flowfund_part2.data.local.SyncState
import com.example.flowfund_part2.data.local.TradeEntity
import com.example.flowfund_part2.data.remote.TradeResponse
import com.example.flowfund_part2.data.repository.SyncResult
import com.example.flowfund_part2.data.repository.TradeRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

/** Runs the real TradeRepository against a fake DAO and fake API (no emulator needed). */
class TradeRepositoryTest {

    private val dao = FakeTradeDao()
    private val api = FakeApi()
    private val repo = TradeRepository(dao, api)

    private fun trade(title: String = "BTC") =
        TradeEntity(title = title, asset = "BTC", tradeType = "BUY", entryPrice = 1.0, exitPrice = 2.0, quantity = 1.0, tradeDate = 1000L)

    private fun remote(id: Long, client: String) =
        TradeResponse(id, "R$id", "ETH", "BUY", 1.0, 2.0, 1.0, 2000L, "", client, 1.0)

    @Test fun emptyDatabaseAndEmptyServer_givesEmptyList_notSampleData() = runBlocking {
        assertEquals(SyncResult.Success, repo.sync())
        assertTrue(repo.getAllTrades().first().isEmpty())
    }

    @Test fun insertedTrade_isPendingUntilSynced_thenSynced() = runBlocking {
        repo.insertTrade(trade())
        assertEquals(SyncState.PENDING, dao.rows.value.single().syncState)

        assertEquals(SyncResult.Success, repo.syncPending())
        val row = dao.rows.value.single()
        assertEquals(SyncState.SYNCED, row.syncState)
        assertEquals(100L, row.remoteId)
        assertEquals(row.clientGeneratedId, api.created.single().clientGeneratedId)
    }

    @Test fun offline_keepsTradeLocally_andReportsConnectionMessage() = runBlocking {
        api.failWith = IOException("offline")
        repo.insertTrade(trade())
        val result = repo.syncPending()
        assertTrue(result is SyncResult.Failure)
        assertTrue((result as SyncResult.Failure).message.contains("No connection"))
        assertEquals(SyncState.PENDING, dao.rows.value.single().syncState)
    }

    @Test fun refresh_importsServerTrades() = runBlocking {
        api.remote = listOf(remote(5, "c-5"))
        repo.refresh()
        val row = dao.rows.value.single()
        assertEquals(5L, row.remoteId)
        assertEquals(SyncState.SYNCED, row.syncState)
    }

    @Test fun refresh_doesNotOverwriteUnsyncedLocalEdit() = runBlocking {
        val id = repo.insertTrade(trade("Local edit").copy(remoteId = 5, clientGeneratedId = "c-5"))
        api.remote = listOf(remote(5, "c-5"))
        repo.refresh()
        assertEquals("Local edit", dao.getTradeById(id)!!.title)
    }

    @Test fun refresh_removesSyncedTradesDeletedOnServer() = runBlocking {
        api.remote = listOf(remote(5, "c-5"))
        repo.refresh()
        api.remote = emptyList()
        repo.refresh()
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test fun deleteOfNeverSyncedTrade_removesItWithoutCallingApi() = runBlocking {
        val id = repo.insertTrade(trade())
        repo.deleteTrade(dao.getTradeById(id)!!)
        assertTrue(dao.rows.value.isEmpty())
        assertTrue(api.deleted.isEmpty())
    }

    @Test fun deleteOfSyncedTrade_isHiddenThenRemovedAfterApiConfirms() = runBlocking {
        api.remote = listOf(remote(5, "c-5"))
        repo.refresh()
        repo.deleteTrade(dao.rows.value.single())

        assertTrue(repo.getAllTrades().first().isEmpty())     // hidden immediately
        assertEquals(1, dao.rows.value.size)                  // still stored until confirmed

        repo.syncPending()
        assertEquals(listOf(5L), api.deleted)
        assertTrue(dao.rows.value.isEmpty())
    }
}
