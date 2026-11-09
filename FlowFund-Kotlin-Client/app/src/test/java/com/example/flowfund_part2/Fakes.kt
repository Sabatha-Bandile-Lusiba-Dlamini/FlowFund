package com.example.flowfund_part2

import com.example.flowfund_part2.data.local.JournalEntryDao
import com.example.flowfund_part2.data.local.JournalEntryEntity
import com.example.flowfund_part2.data.local.SyncState
import com.example.flowfund_part2.data.local.TradeDao
import com.example.flowfund_part2.data.local.TradeEntity
import com.example.flowfund_part2.data.remote.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import retrofit2.Response

/** In-memory TradeDao: the real repository code runs against it, only Room is replaced. */
class FakeTradeDao : TradeDao {
    val rows = MutableStateFlow<List<TradeEntity>>(emptyList())
    private var nextId = 1L

    override fun getAllTrades(): Flow<List<TradeEntity>> =
        rows.map { l -> l.filter { it.syncState != SyncState.DELETED }.sortedByDescending { it.tradeDate } }
    override suspend fun getTradeById(tradeId: Long) = rows.value.firstOrNull { it.id == tradeId }
    override suspend fun getByRemoteId(remoteId: Long) = rows.value.firstOrNull { it.remoteId == remoteId }
    override suspend fun getByClientId(clientId: String) = rows.value.firstOrNull { it.clientGeneratedId == clientId }
    override suspend fun getUnsynced() = rows.value.filter { it.syncState != SyncState.SYNCED }
    override suspend fun getSynced() = rows.value.filter { it.syncState == SyncState.SYNCED && it.remoteId != null }
    override suspend fun insertTrade(trade: TradeEntity): Long {
        val id = if (trade.id == 0L) nextId++ else trade.id
        rows.value = rows.value.filter { it.id != id } + trade.copy(id = id)
        return id
    }
    override suspend fun updateTrade(trade: TradeEntity) { rows.value = rows.value.map { if (it.id == trade.id) trade else it } }
    override suspend fun deleteTrade(trade: TradeEntity) { rows.value = rows.value.filter { it.id != trade.id } }
    override suspend fun deleteAllTrades() { rows.value = emptyList() }
}

/** In-memory JournalEntryDao, same idea as FakeTradeDao. */
class FakeJournalDao : JournalEntryDao {
    val rows = MutableStateFlow<List<JournalEntryEntity>>(emptyList())
    private var nextId = 1L

    override fun getAllEntries(): Flow<List<JournalEntryEntity>> =
        rows.map { l -> l.filter { it.syncState != SyncState.DELETED }.sortedByDescending { it.entryDate } }
    override suspend fun getEntryById(entryId: Long) = rows.value.firstOrNull { it.id == entryId }
    override suspend fun getByRemoteId(remoteId: Long) = rows.value.firstOrNull { it.remoteId == remoteId }
    override suspend fun getByClientId(clientId: String) = rows.value.firstOrNull { it.clientGeneratedId == clientId }
    override suspend fun getUnsynced() = rows.value.filter { it.syncState != SyncState.SYNCED }
    override suspend fun getSynced() = rows.value.filter { it.syncState == SyncState.SYNCED && it.remoteId != null }
    override suspend fun insertEntry(entry: JournalEntryEntity): Long {
        val id = if (entry.id == 0L) nextId++ else entry.id
        rows.value = rows.value.filter { it.id != id } + entry.copy(id = id)
        return id
    }
    override suspend fun updateEntry(entry: JournalEntryEntity) { rows.value = rows.value.map { if (it.id == entry.id) entry else it } }
    override suspend fun deleteEntry(entry: JournalEntryEntity) { rows.value = rows.value.filter { it.id != entry.id } }
    override suspend fun deleteAllEntries() { rows.value = emptyList() }
}

/** Scriptable API: set [remote] for GET, [failWith] to simulate an outage. Records every call. */
class FakeApi : FlowFundApi {
    var remote: List<TradeResponse> = emptyList()
    var failWith: Exception? = null
    val created = mutableListOf<TradeRequest>()
    val deleted = mutableListOf<Long>()
    private var nextRemoteId = 100L

    private fun check() { failWith?.let { throw it } }

    override suspend fun getTrades(): List<TradeResponse> { check(); return remote }
    override suspend fun createTrade(body: TradeRequest): TradeResponse {
        check(); created += body
        return TradeResponse(nextRemoteId++, body.title, body.asset, body.tradeType, body.entryPrice, body.exitPrice,
            body.quantity, body.tradeDate, body.notes, body.clientGeneratedId, 0.0)
    }
    override suspend fun updateTrade(id: Long, body: TradeRequest): TradeResponse {
        check(); return TradeResponse(id, body.title, body.asset, body.tradeType, body.entryPrice, body.exitPrice,
            body.quantity, body.tradeDate, body.notes, body.clientGeneratedId, 0.0)
    }
    override suspend fun deleteTrade(id: Long): Response<Unit> { check(); deleted += id; return Response.success(Unit) }

    override suspend fun verifySso(body: SsoVerifyRequest) = AuthResponse("access-1", "refresh-1", ApiUser(1, "T", "t@t.com")).also { check() }
    override suspend fun refresh(body: RefreshRequest) = AuthResponse("access-2", "refresh-2", ApiUser(1, "T", "t@t.com"))
    override suspend fun getTradeStatistics(): TradeStatisticsResponse = TODO("not used in these tests")
    var remoteJournal: List<JournalResponse> = emptyList()
    val journalCreated = mutableListOf<JournalRequest>()
    val journalDeleted = mutableListOf<Long>()

    override suspend fun getJournal(): List<JournalResponse> { check(); return remoteJournal }
    override suspend fun createJournal(body: JournalRequest): JournalResponse {
        check(); journalCreated += body
        return JournalResponse(nextRemoteId++, body.title, body.content, body.mood, body.entryDate, body.clientGeneratedId)
    }
    override suspend fun updateJournal(id: Long, body: JournalRequest): JournalResponse {
        check(); return JournalResponse(id, body.title, body.content, body.mood, body.entryDate, body.clientGeneratedId)
    }
    override suspend fun deleteJournal(id: Long): Response<Unit> { check(); journalDeleted += id; return Response.success(Unit) }
}
