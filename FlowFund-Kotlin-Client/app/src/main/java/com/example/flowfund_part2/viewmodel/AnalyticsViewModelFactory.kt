
package com.example.flowfund_part2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.flowfund_part2.data.local.TradeDao

class AnalyticsViewModelFactory(
    private val tradeDao: TradeDao
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(
                AnalyticsViewModel::class.java
            )
        ) {
            @Suppress("UNCHECKED_CAST")
            return AnalyticsViewModel(tradeDao) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}

