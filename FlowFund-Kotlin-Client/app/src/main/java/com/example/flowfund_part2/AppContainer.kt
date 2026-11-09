package com.example.flowfund_part2

import android.content.Context
import com.example.flowfund_part2.data.local.AppDatabase
import com.example.flowfund_part2.data.remote.ApiClient
import com.example.flowfund_part2.data.remote.EncryptedTokenStore
import com.example.flowfund_part2.data.repository.AuthRepository
import com.example.flowfund_part2.data.repository.JournalRepository
import com.example.flowfund_part2.data.repository.TradeRepository

/*
 Hand written dependency container: one database, one token store, one API client, shared by every screen.
  Use AppContainer.get(context) from any Activity. (No DI framework is needed at this size.)
 */
class AppContainer private constructor(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    val tokenStore = EncryptedTokenStore(context)
    val api = ApiClient.create(tokenStore)

    val authRepository = AuthRepository(api, tokenStore)
    val tradeRepository = TradeRepository(db.tradeDao(), api)
    val journalRepository = JournalRepository(db.journalEntryDao(), api)

    companion object {
        @Volatile private var instance: AppContainer? = null
        fun get(context: Context): AppContainer =
            instance ?: synchronized(this) { instance ?: AppContainer(context.applicationContext).also { instance = it } }
    }
}
