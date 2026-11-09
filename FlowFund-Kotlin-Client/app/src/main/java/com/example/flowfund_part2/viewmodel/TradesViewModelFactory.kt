package com.example.flowfund_part2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.flowfund_part2.data.repository.TradeRepository

class TradesViewModelFactory(private val repository: TradeRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TradesViewModel::class.java)) { "Unknown ViewModel: $modelClass" }
        return TradesViewModel(repository) as T
    }
}
