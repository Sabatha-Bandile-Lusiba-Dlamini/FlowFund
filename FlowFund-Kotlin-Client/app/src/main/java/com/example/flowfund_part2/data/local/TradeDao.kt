
package com.example.flowfund_part2.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing trade records.
 *
 * Room generates the implementation of this interface
 * during compilation (Android Developers, 2026a).
 */
@Dao
interface TradeDao {

    /** Visible trades, newest first. Flow re-emits whenever the table changes (Android Developers, 2026b). */
    @Query("SELECT * FROM trades WHERE syncState != 'DELETED' ORDER BY tradeDate DESC")
    fun getAllTrades(): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trades WHERE id = :tradeId")
    suspend fun getTradeById(tradeId: Long): TradeEntity?

    @Query("SELECT * FROM trades WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: Long): TradeEntity?

    @Query("SELECT * FROM trades WHERE clientGeneratedId = :clientId LIMIT 1")
    suspend fun getByClientId(clientId: String): TradeEntity?

    /** Rows the API has not seen yet (new or edited): PENDING, plus DELETED rows that need a remote delete. */
    @Query("SELECT * FROM trades WHERE syncState != 'SYNCED'")
    suspend fun getUnsynced(): List<TradeEntity>

    @Query("SELECT * FROM trades WHERE syncState = 'SYNCED' AND remoteId IS NOT NULL")
    suspend fun getSynced(): List<TradeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: TradeEntity): Long

    @Update
    suspend fun updateTrade(trade: TradeEntity)

    @Delete
    suspend fun deleteTrade(trade: TradeEntity)

    @Query("DELETE FROM trades")
    suspend fun deleteAllTrades()
}


/*
REFERENCES

Android Developers (2026a) 'Access data using Room DAOs'.
Available at: https://developer.android.com/training/data-storage/room/accessing-data
(Accessed: 3 November 2026).

Android Developers (2026b) 'Write asynchronous DAO queries'.
Available at: https://developer.android.com/training/data-storage/room/async-queries
(Accessed: 3 November 2026).
*/
