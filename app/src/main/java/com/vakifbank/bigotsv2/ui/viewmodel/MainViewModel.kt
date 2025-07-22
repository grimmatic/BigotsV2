package com.vakifbank.bigotsv2.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vakifbank.bigotsv2.data.model.ArbitrageOpportunity
import com.vakifbank.bigotsv2.data.model.CoinData
import com.vakifbank.bigotsv2.data.repository.CryptoRepository
import com.vakifbank.bigotsv2.service.ServiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: CryptoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        observeData()
        refreshData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                repository.coinDataList,
                repository.arbitrageOpportunities,
                repository.usdTryRate,
                ServiceManager.isServiceRunning
            ) { coins, opportunities, usdTryRate, isServiceRunning ->
                _uiState.value.copy(
                    coinList = coins,
                    arbitrageOpportunities = opportunities,
                    usdTryRate = usdTryRate,
                    isServiceRunning = isServiceRunning,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.fetchAllData()
        }
    }

    fun toggleService(context: android.content.Context) {
        viewModelScope.launch {
            if (_uiState.value.isServiceRunning) {
                ServiceManager.stopService(context)
            } else {
                ServiceManager.startService(context)
            }
        }
    }

    fun updateThreshold(coinSymbol: String, threshold: Double) {
        viewModelScope.launch {
        }
    }

    fun filterCoins(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun sortCoins(sortType: SortType) {
        _uiState.value = _uiState.value.copy(sortType = sortType)
    }
}

data class MainUiState(
    val coinList: List<CoinData> = emptyList(),
    val arbitrageOpportunities: List<ArbitrageOpportunity> = emptyList(),
    val usdTryRate: Double = 0.0,
    val isServiceRunning: Boolean = false,
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val sortType: SortType = SortType.BY_DIFFERENCE,
    val filterType: FilterType = FilterType.ALL
)

enum class SortType {
    BY_DIFFERENCE, BY_NAME, BY_PRICE, BY_VOLUME
}

enum class FilterType {
    ALL, ALERTS_ONLY, POSITIVE_ONLY, NEGATIVE_ONLY
}

class MainViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(CryptoRepository.getInstance()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}