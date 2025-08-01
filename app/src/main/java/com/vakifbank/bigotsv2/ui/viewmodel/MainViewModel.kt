package com.vakifbank.bigotsv2.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakifbank.bigotsv2.data.repository.CryptoRepository
import com.vakifbank.bigotsv2.domain.model.ArbitrageOpportunity
import com.vakifbank.bigotsv2.domain.model.CoinData
import com.vakifbank.bigotsv2.service.ServiceManager
import com.vakifbank.bigotsv2.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: CryptoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadGlobalThreshold()
        observeData()
        fetchInitialData()
    }

    private fun loadGlobalThreshold() {
        viewModelScope.launch {
            val savedThreshold = repository.getGlobalThreshold()
            _uiState.value = _uiState.value.copy(globalThreshold = savedThreshold)
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                repository.coinDataList,
                repository.arbitrageOpportunities,
                repository.usdTryRate,
                repository.usdTryRateBtcTurk,
                ServiceManager.isServiceRunning
            ) { coins, opportunities, usdTryRate, usdTryRateBtcTurk, isServiceRunning ->
                val currentGlobalThreshold = _uiState.value.globalThreshold
                MainUiState(
                    coinList = coins,
                    arbitrageOpportunities = opportunities,
                    usdTryRate = usdTryRate,
                    usdTryRateBtcTurk = usdTryRateBtcTurk,
                    isServiceRunning = isServiceRunning,
                    btcPrice = getBtcPrice(coins, usdTryRate),
                    btcPriceUsd = getBtcPriceUsd(coins, usdTryRate),
                    globalThreshold = currentGlobalThreshold,
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

    // Service management
    fun toggleService(context: Context) {
        viewModelScope.launch {
            if (_uiState.value.isServiceRunning) {
                ServiceManager.stopService(context)
            } else {
                ServiceManager.startService(context)
            }
        }
    }

    // Global settings
    fun setAllThresholds(threshold: Double) {
        viewModelScope.launch {
            repository.updateAllThresholds(threshold)
            _uiState.value = _uiState.value.copy(globalThreshold = threshold)
        }
    }

    fun updateAllSoundLevels(level: Int) {
        viewModelScope.launch {
            repository.updateAllSoundLevels(level)
        }
    }

    fun updateRefreshRate(rate: Float) {
        viewModelScope.launch {
            repository.updateRefreshRate(rate)
        }
    }

    // UI helper methods
    fun getFormattedUsdTryRate(): String {
        return "₺${String.format("%.3f", _uiState.value.usdTryRate)}"
    }

    fun getFormattedUsdTryRateBtcTurk(): String {
        return "₺${String.format("%.3f", _uiState.value.usdTryRateBtcTurk)}"
    }

    fun getServiceStatusText(): String {
        return if (_uiState.value.isServiceRunning) "Çalışıyor" else "Durduruldu"
    }

    fun getStatusIndicatorBackground(): Int {
        return if (_uiState.value.isServiceRunning) {
            com.vakifbank.bigotsv2.R.drawable.circle_green
        } else {
            com.vakifbank.bigotsv2.R.drawable.circle_red
        }
    }

    // Data refresh
    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            repository.fetchAllData()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    // Individual coin settings (called from fragments through their ViewModels)
    fun updateThreshold(coinSymbol: String, threshold: Double, isForBtcTurk: Boolean = false) {
        viewModelScope.launch {
            repository.updateCoinThreshold(coinSymbol, threshold, isForBtcTurk)
        }
    }
}

data class MainUiState(
    val coinList: List<CoinData> = emptyList(),
    val arbitrageOpportunities: List<ArbitrageOpportunity> = emptyList(),
    val usdTryRate: Double = 0.0,
    val usdTryRateBtcTurk: Double = 0.0,
    val isServiceRunning: Boolean = false,
    val btcPrice: String = "$0.00",
    val btcPriceUsd: Double = 0.0,
    val globalThreshold: Double = Constants.Numeric.DEFAULT_ALERT_THRESHOLD,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)