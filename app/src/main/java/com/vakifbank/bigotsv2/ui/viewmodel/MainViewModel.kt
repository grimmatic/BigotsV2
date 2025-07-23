package com.vakifbank.bigotsv2.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vakifbank.bigotsv2.data.model.ArbitrageOpportunity
import com.vakifbank.bigotsv2.data.model.CoinData
import com.vakifbank.bigotsv2.data.repository.CryptoRepository
import com.vakifbank.bigotsv2.service.ServiceManager
import com.vakifbank.bigotsv2.utils.Constants
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
        fetchInitialData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                repository.coinDataList,
                repository.arbitrageOpportunities,
                repository.usdTryRate,
                ServiceManager.isServiceRunning
            ) { coins, opportunities, usdTryRate, isServiceRunning ->
                MainUiState(
                    coinList = coins,
                    arbitrageOpportunities = opportunities,
                    usdTryRate = usdTryRate,
                    isServiceRunning = isServiceRunning,
                    btcPrice = getBtcPrice(coins, usdTryRate),
                    btcPriceUsd = getBtcPriceUsd(coins, usdTryRate),
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun fetchInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.fetchAllData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun getBtcPrice(coins: List<CoinData>, usdTryRate: Double): String {
        val btcCoin = coins.find { it.symbol == "BTC" }
        return btcCoin?.let {
            if (usdTryRate > 0) {
                "$${String.format("%.2f", it.binancePrice?.div(usdTryRate))}"
            } else "$0.00"
        } ?: "$0.00"
    }

    private fun getBtcPriceUsd(coins: List<CoinData>, usdTryRate: Double): Double {
        val btcCoin = coins.find { it.symbol == "BTC" }
        return btcCoin?.let {
            if (usdTryRate > 0) {
                it.binancePrice?.div(usdTryRate) ?: 0.0
            } else 0.0
        } ?: 0.0
    }

    fun toggleService(context: Context) {
        viewModelScope.launch {
            if (_uiState.value.isServiceRunning) {
                ServiceManager.stopService(context)
            } else {
                ServiceManager.startService(context)
            }
        }
    }

    fun setAllThresholds(threshold: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(globalThreshold = threshold)
        }
    }

    fun getFormattedUsdTryRate(): String {
        return "₺${String.format("%.2f", _uiState.value.usdTryRate)}"
    }

    fun getServiceStatusText(): String {
        return if (_uiState.value.isServiceRunning) "Çalışıyor" else "Durduruldu"
    }

    fun getServiceStatusIcon(): Int {
        return if (_uiState.value.isServiceRunning) {
            com.vakifbank.bigotsv2.R.drawable.ic_stop
        } else {
            com.vakifbank.bigotsv2.R.drawable.ic_play_arrow
        }
    }

    fun getStatusIndicatorBackground(): Int {
        return if (_uiState.value.isServiceRunning) {
            com.vakifbank.bigotsv2.R.drawable.circle_green
        } else {
            com.vakifbank.bigotsv2.R.drawable.circle_red
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            repository.fetchAllData()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
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
    val btcPrice: String = "$0.00",
    val btcPriceUsd: Double = 0.0,
    val globalThreshold: Double = Constants.Numeric.DEFAULT_ALERT_THRESHOLD,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
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