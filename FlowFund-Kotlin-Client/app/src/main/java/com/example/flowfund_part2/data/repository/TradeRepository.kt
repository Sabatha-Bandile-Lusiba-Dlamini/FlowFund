package com.example.flowfund_part2.data.repository

import android.util.Log
import com.example.flowfund_part2.data.local.SyncState
import com.example.flowfund_part2.data.local.TradeDao
import com.example.flowfund_part2.data.local.TradeEntity
import com.example.flowfund_part2.data.remote.FlowFundApi
import com.example.flowfund_part2.data.remote.TradeRequest
import com.example.flowfund_part2.data.remote.TradeResponse
import kotlinx.coroutines.flow.Flow

/**
 * Single entry point for trade data. Room is the source of truth: the UI only ever reads Room, writes go
 * to Room first (so the app works offline) and are then pushed to the API by [syncPending]. [refresh]
 * pulls the server copy into Room. No sample data exists anywhere: an empty Room is an empty list.
 *
 * Pattern: offline-first repository (Android Developers, 2026).
 */
class TradeRepository(
    private val tradeDao: TradeDao,
    private val api: FlowFundApi
) {
    /** Visible trades, newest first. */
    fun getAllTrades(): Flow<List<TradeEntity>> = tradeDao.getAllTrades()

    suspend fun getTradeById(id: Long): TradeEntity? = tradeDao.getTradeById(id)

    /** Saves locally as PENDING; call [syncPending] afterwards to upload. */
    suspend fun insertTrade(trade: TradeEntity): Long =
        tradeDao.insertTrade(trade.copy(syncState = SyncState.PENDING))

    suspend fun updateTrade(trade: TradeEntity) =
        tradeDao.updateTrade(trade.copy(syncState = SyncState.PENDING))

    /**
     * Rows the API never saw are simply removed. Rows the API knows are hidden (DELETED) until
     * [syncPending] confirms the remote delete.
     */
    suspend fun deleteTrade(trade: TradeEntity) {
        if (trade.remoteId == null) tradeDao.deleteTrade(trade)
        else tradeDao.updateTrade(trade.copy(syncState = SyncState.DELETED))
    }

    /** Uploads local changes, then downloads the server copy. Stops at the first auth failure. */
    suspend fun sync(): SyncResult {
        val pushed = syncPending()
        if (pushed is SyncResult.Failure) return pushed
        return refresh()
    }

    /** Pushes every PENDING/DELETED row to the API. Per-row failures leave that row pending for next time. */
    suspend fun syncPending(): SyncResult {
        var failure: SyncResult.Failure? = null
        for (row in tradeDao.getUnsynced()) {
            try {
                when {
                    row.syncState == SyncState.DELETED -> pushDelete(row)
                    row.remoteId == null -> pushCreate(row)
                    else -> pushUpdate(row)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Sync of trade ${row.id} failed: ${e.message}")
                val f = e.toFailure()
                if (f.isAuth) return f
                failure = failure ?: f
            }
        }
        return failure ?: SyncResult.Success
    }

    /**
     * Replaces the SYNCED part of Room with the server list. Rows with unsynced local edits are never
     * overwritten, and synced rows missing from the server (deleted elsewhere) are removed.
     */
    suspend fun refresh(): SyncResult = try {
        val remote = api.getTrades()
        val remoteIds = remote.map { it.id }.toSet()

        for (dto in remote) {
            val local = tradeDao.getByRemoteId(dto.id) ?: tradeDao.getByClientId(dto.clientGeneratedId)
            if (local == null || local.syncState == SyncState.SYNCED) {
                tradeDao.insertTrade(dto.toEntity(localId = local?.id ?: 0))
            }
        }
        tradeDao.getSynced().filter { it.remoteId !in remoteIds }.forEach { tradeDao.deleteTrade(it) }
        Log.i(TAG, "Refreshed ${remote.size} trades from the API")
        SyncResult.Success
    } catch (e: Exception) {
        Log.w(TAG, "Trade refresh failed: ${e.message}")
        e.toFailure()
    }

    private suspend fun pushCreate(row: TradeEntity) {
        val created = api.createTrade(row.toRequest())
        tradeDao.updateTrade(row.copy(remoteId = created.id, syncState = SyncState.SYNCED))
    }

    private suspend fun pushUpdate(row: TradeEntity) {
        api.updateTrade(row.remoteId!!, row.toRequest())
        tradeDao.updateTrade(row.copy(syncState = SyncState.SYNCED))
    }

    private suspend fun pushDelete(row: TradeEntity) {
        val response = api.deleteTrade(row.remoteId!!)
        // 404 means it is already gone on the server, which is the outcome we wanted.
        if (response.isSuccessful || response.code() == 404) tradeDao.deleteTrade(row)
        else throw retrofit2.HttpException(response)
    }

    private companion object { const val TAG = "TradeRepository" }
}

internal fun TradeEntity.toRequest() = TradeRequest(
    title, asset, tradeType, entryPrice, exitPrice, quantity, tradeDate, notes, clientGeneratedId
)

internal fun TradeResponse.toEntity(localId: Long = 0) = TradeEntity(
    id = localId, title = title, asset = asset, tradeType = tradeType, entryPrice = entryPrice,
    exitPrice = exitPrice, quantity = quantity, tradeDate = tradeDate, notes = notes,
    remoteId = id, clientGeneratedId = clientGeneratedId, syncState = SyncState.SYNCED
)

/*
REFERENCES

Android Developers (2026) 'Build an offline-first app'.
Available at: https://developer.android.com/topic/architecture/data-layer/offline-first
(Accessed: 4 November 2026).
*/
