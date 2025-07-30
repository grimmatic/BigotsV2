package com.vakifbank.bigotsv2.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakifbank.bigotsv2.domain.model.ArbitrageOpportunity
import com.vakifbank.bigotsv2.domain.model.CoinData
import com.vakifbank.bigotsv2.domain.model.Exchange
import com.vakifbank.bigotsv2.data.repository.CryptoRepository
import com.vakifbank.bigotsv2.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class ParibuViewModel @Inject constructor(
    private val repository: CryptoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParibuUiState())
    val uiState: StateFlow<ParibuUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                repository.coinDataList,
                repository.arbitrageOpportunities
            ) { coins, opportunities ->
                val paribuCoins = filterParibuCoins(coins)
                val paribuOpportunities = opportunities.filter { it.exchange == Exchange.PARIBU }

                ParibuUiState(
                    coinList = paribuCoins,
                    arbitrageOpportunities = paribuOpportunities,
                    alertCount = paribuOpportunities.size,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun filterParibuCoins(coins: List<CoinData>): List<CoinData> {
        return coins.filter { coin ->
            coin.paribuPrice!! > 0 && kotlin.math.abs(coin.paribuDifference!!) >= 0.1
        }.sortedByDescending {
            it.paribuDifference?.let { x -> kotlin.math.abs(x) }
        }
    }

    fun showCoinDetailDialog(coin: CoinData) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedCoin = coin)
        }
    }

    fun hideCoinDetailDialog() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedCoin = null)
        }
    }

    fun showCoinOptionsMenu(coin: CoinData) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedCoin = coin,
                showOptionsMenu = true
            )
        }
    }

    fun hideOptionsMenu() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showOptionsMenu = false)
        }
    }

    fun getActiveAlertText(): String {
        return "${_uiState.value.alertCount} aktif alarm"
    }

    fun getExchangeName(): String {
        return Constants.ExchangeNames.PARIBU
    }

    fun getExchangeIcon(): Int {
        return com.vakifbank.bigotsv2.R.drawable.paribu
    }

    fun getCoinPrice(coin: CoinData): String {
        return "₺${String.format("%.2f", coin.paribuPrice)}"
    }

    fun getCoinDifference(coin: CoinData): String {
        val difference = coin.paribuDifference ?: 0.0
        val sign = if (difference > 0) "+" else ""
        return "$sign${String.format("%.2f", difference)}%"
    }

    fun getCoinDifferenceColor(coin: CoinData): Int {
        val maxDifference = kotlin.math.abs(coin.paribuDifference ?: 0.0)
        val alertThreshold = coin.alertThreshold ?: Constants.Numeric.DEFAULT_ALERT_THRESHOLD
        val isPositive = (coin.paribuDifference ?: 0.0) > 0

        return when {
            maxDifference > alertThreshold -> {
                if (isPositive) com.vakifbank.bigotsv2.R.color.success_color
                else com.vakifbank.bigotsv2.R.color.error_color
            }
            else -> com.vakifbank.bigotsv2.R.color.text_secondary
        }
    }

    fun shouldShowAlertIndicator(coin: CoinData): Boolean {
        val maxDifference = kotlin.math.abs(coin.paribuDifference ?: 0.0)
        val alertThreshold = coin.alertThreshold ?: Constants.Numeric.DEFAULT_ALERT_THRESHOLD
        return maxDifference > alertThreshold
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            repository.fetchAllData()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun updateCoinAlert(coin: CoinData, isActive: Boolean) {
        viewModelScope.launch {
            coin.symbol?.let { repository.updateCoinAlertStatus(it, isActive, false) }
        }
    }

    fun updateCoinThreshold(coin: CoinData, threshold: Double) {
        viewModelScope.launch {
            coin.symbol?.let { repository.updateCoinThreshold(it, threshold, false) }
        }
    }

    fun updateCoinSoundLevel(coin: CoinData, soundLevel: Int) {
        viewModelScope.launch {
            coin.symbol?.let { repository.updateCoinSoundLevel(it, soundLevel, false) }
        }
    }
}

data class ParibuUiState(
    val coinList: List<CoinData> = emptyList(),
    val arbitrageOpportunities: List<ArbitrageOpportunity> = emptyList(),
    val alertCount: Int = 0,
    val selectedCoin: CoinData? = null,
    val showOptionsMenu: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)