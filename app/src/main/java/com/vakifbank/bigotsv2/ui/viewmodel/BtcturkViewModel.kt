package com.vakifbank.bigotsv2.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakifbank.bigotsv2.data.repository.CryptoRepository
import com.vakifbank.bigotsv2.domain.model.ArbitrageOpportunity
import com.vakifbank.bigotsv2.domain.model.CoinData
import com.vakifbank.bigotsv2.domain.model.Exchange
import com.vakifbank.bigotsv2.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class BtcturkViewModel @Inject constructor(
    private val repository: CryptoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BtcturkUiState())
    val uiState: StateFlow<BtcturkUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentFilterType = MutableStateFlow(FilterType.ALL)
    val currentFilterType: StateFlow<FilterType> = _currentFilterType.asStateFlow()

    private val _currentSortType = MutableStateFlow(SortType.DIFFERENCE_DESC)
    val currentSortType: StateFlow<SortType> = _currentSortType.asStateFlow()

    private val _isSearchExpanded = MutableStateFlow(false)
    val isSearchExpanded: StateFlow<Boolean> = _isSearchExpanded.asStateFlow()

    private var allCoins: List<CoinData> = emptyList()

    init {
        observeData()
        observeFiltersAndSearch()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                repository.coinDataList,
                repository.arbitrageOpportunities
            ) { coins, opportunities ->
                allCoins = filterBtcturkCoins(coins)
                val btcturkOpportunities = opportunities.filter { it.exchange == Exchange.BTCTURK }

                _uiState.value = _uiState.value.copy(
                    arbitrageOpportunities = btcturkOpportunities,
                    alertCount = btcturkOpportunities.size,
                    isLoading = false
                )

                applyFiltersAndSort()
            }.collect { }
        }
    }

    private fun observeFiltersAndSearch() {
        viewModelScope.launch {
            combine(
                _searchQuery,
                _currentFilterType,
                _currentSortType
            ) { _, _, _ ->
                applyFiltersAndSort()
            }.collect { }
        }
    }

    private fun filterBtcturkCoins(coins: List<CoinData>): List<CoinData> {
        return coins.filter { coin ->
            coin.btcturkPrice!! > 0 && coin.binancePrice!! > 0
        }.sortedByDescending {
            it.btcturkDifference?.let { x -> abs(x) }
        }
    }

    private fun applyFiltersAndSort() {
        val searchQuery = _searchQuery.value.lowercase().trim()

        var filtered = if (searchQuery.isEmpty()) {
            allCoins
        } else {
            allCoins.filter { coin ->
                coin.symbol?.lowercase()?.contains(searchQuery) == true ||
                        coin.name?.lowercase()?.contains(searchQuery) == true
            }
        }

        filtered = when (_currentFilterType.value) {
            FilterType.ALL -> filtered
            FilterType.ALERTS_ONLY -> filtered.filter { coin ->
                val difference = abs(coin.btcturkDifference ?: 0.0)
                val threshold = coin.alertThreshold ?: 2.5
                difference > threshold
            }

            FilterType.POSITIVE_ONLY -> filtered.filter { coin ->
                (coin.btcturkDifference ?: 0.0) > 0
            }

            FilterType.NEGATIVE_ONLY -> filtered.filter { coin ->
                (coin.btcturkDifference ?: 0.0) < 0
            }
        }

        val sortedCoins = when (_currentSortType.value) {
            SortType.DIFFERENCE_DESC -> filtered.sortedByDescending {
                abs(
                    it.btcturkDifference ?: 0.0
                )
            }

            SortType.DIFFERENCE_ASC -> filtered.sortedBy { abs(it.btcturkDifference ?: 0.0) }
            SortType.NAME_ASC -> filtered.sortedBy { it.symbol }
            SortType.NAME_DESC -> filtered.sortedByDescending { it.symbol }
            SortType.PRICE_DESC -> filtered.sortedByDescending { it.btcturkPrice ?: 0.0 }
            SortType.PRICE_ASC -> filtered.sortedBy { it.btcturkPrice ?: 0.0 }
        }
        val alertCount = calculateAlertCountFromDisplayedCoins(sortedCoins)

        _uiState.value = _uiState.value.copy(
            coinList = sortedCoins,
            filteredCoinCount = sortedCoins.size,
            totalCoinCount = allCoins.size,
            hasActiveFilters = hasActiveFilters(),
            alertCount = alertCount
        )
    }

    private fun calculateAlertCountFromDisplayedCoins(displayedCoins: List<CoinData>): Int {
        return displayedCoins.count { coin ->
            val difference = abs(coin.btcturkDifference ?: 0.0)
            val threshold = coin.alertThreshold ?: 2.5
            difference > threshold
        }
    }

    private fun hasActiveFilters(): Boolean {
        return _searchQuery.value.isNotEmpty() ||
                _currentFilterType.value != FilterType.ALL
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearchExpansion() {
        _isSearchExpanded.value = !_isSearchExpanded.value
        if (!_isSearchExpanded.value) {
            clearAllFilters()
        }
    }

    fun updateFilterType(filterType: FilterType) {
        _currentFilterType.value = filterType
    }

    fun updateSortType(sortType: SortType) {
        _currentSortType.value = sortType
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _currentFilterType.value = FilterType.ALL
        _currentSortType.value = SortType.DIFFERENCE_DESC
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

    fun updateCoinThreshold(coin: CoinData, threshold: Double) {
        viewModelScope.launch {
            coin.symbol?.let { repository.updateCoinThreshold(it, threshold, true) }
        }
    }

    fun updateCoinSoundLevel(coin: CoinData, soundLevel: Int) {
        viewModelScope.launch {
            coin.symbol?.let { repository.updateCoinSoundLevel(it, soundLevel, true) }
        }
    }

    fun getExchangeName(): String {
        return Constants.ExchangeNames.BTCTURK
    }

    fun getExchangeIcon(): Int {
        return com.vakifbank.bigotsv2.R.drawable.btcturk
    }

    fun getFilterButtonText(): String {
        return when (_currentFilterType.value) {
            FilterType.ALL -> "Tümü"
            FilterType.ALERTS_ONLY -> "Alarmlar"
            FilterType.POSITIVE_ONLY -> "Pozitif Fark"
            FilterType.NEGATIVE_ONLY -> "Negatif Fark"
        }
    }

    fun getSortButtonText(): String {
        return when (_currentSortType.value) {
            SortType.DIFFERENCE_DESC -> "Fark % ↓"
            SortType.DIFFERENCE_ASC -> "Fark % ↑"
            SortType.NAME_ASC -> "İsim ↓"
            SortType.NAME_DESC -> "İsim ↑"
            SortType.PRICE_DESC -> "Fiyat ↓"
            SortType.PRICE_ASC -> "Fiyat ↑"
        }
    }
}

data class BtcturkUiState(
    val coinList: List<CoinData> = emptyList(),
    val arbitrageOpportunities: List<ArbitrageOpportunity> = emptyList(),
    val alertCount: Int = 0,
    val selectedCoin: CoinData? = null,
    val showOptionsMenu: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val filteredCoinCount: Int = 0,
    val totalCoinCount: Int = 0,
    val hasActiveFilters: Boolean = false
)