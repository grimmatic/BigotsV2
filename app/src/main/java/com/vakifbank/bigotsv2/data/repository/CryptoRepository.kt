package com.vakifbank.bigotsv2.data.repository

import android.content.Context
import android.util.Log
import com.vakifbank.bigotsv2.domain.model.ArbitrageOpportunity
import com.vakifbank.bigotsv2.domain.model.CoinData
import com.vakifbank.bigotsv2.domain.model.Exchange
import com.vakifbank.bigotsv2.domain.model.SupportedCoins
import com.vakifbank.bigotsv2.domain.model.binance.BinanceTickerResponse
import com.vakifbank.bigotsv2.domain.model.btcturk.BtcTurkTicker
import com.vakifbank.bigotsv2.domain.model.paribu.ParibuTicker
import com.vakifbank.bigotsv2.data.service.BinanceApiService
import com.vakifbank.bigotsv2.data.service.BtcTurkApiService
import com.vakifbank.bigotsv2.data.service.ParibuApiService
import com.vakifbank.bigotsv2.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class CryptoRepository @Inject constructor(
    private val paribuApi: ParibuApiService,
    private val binanceApi: BinanceApiService,
    private val btcturkApi: BtcTurkApiService,
    @ApplicationContext private val context: Context
) {

    private val _coinDataList = MutableStateFlow<List<CoinData>>(emptyList())
    val coinDataList: Flow<List<CoinData>> = _coinDataList.asStateFlow()

    private val _arbitrageOpportunities = MutableStateFlow<List<ArbitrageOpportunity>>(emptyList())
    val arbitrageOpportunities: Flow<List<ArbitrageOpportunity>> =
        _arbitrageOpportunities.asStateFlow()

    private val _usdTryRate = MutableStateFlow(Constants.Numeric.DEFAULT_PRICE)
    val usdTryRate: Flow<Double> = _usdTryRate.asStateFlow()

    private val _usdTryRateBtcTurk = MutableStateFlow(Constants.Numeric.DEFAULT_PRICE)
    val usdTryRateBtcTurk: Flow<Double> = _usdTryRateBtcTurk.asStateFlow()

    suspend fun fetchAllData() {
        try {
            coroutineScope {
                val paribuDeferred = async { fetchParibuData() }
                val binanceDeferred = async { fetchBinanceData() }
                val btcturkDeferred = async { fetchBtcTurkData() }

                val paribuData = paribuDeferred.await()
                val binanceData = binanceDeferred.await()
                val btcturkData = btcturkDeferred.await()

                paribuData[Constants.ApiSymbols.USDT_TL]?.let { usdtTicker ->
                    val usdtRate = usdtTicker.lowestAsk ?: Constants.Numeric.DEFAULT_PRICE
                    _usdTryRate.value = usdtRate
                }

                btcturkData["USDTTRY"]?.let { usdtTicker ->
                    val usdtRate = usdtTicker.ask ?: Constants.Numeric.DEFAULT_PRICE
                    _usdTryRateBtcTurk.value = usdtRate
                }

                val updatedCoins = calculateArbitrage(paribuData, binanceData, btcturkData)
                _coinDataList.value = updatedCoins
                val opportunities = findArbitrageOpportunities(updatedCoins)

                _arbitrageOpportunities.value = opportunities
            }
        } catch (e: Exception) {
            Log.e("CryptoRepository", "Error fetching data", e)
        }
    }

    fun updateCoinThreshold(coinSymbol: String, newThreshold: Double, isForBtcTurk: Boolean = false) {
        val prefs = context.getSharedPreferences("coin_settings", Context.MODE_PRIVATE)
        val key = if (isForBtcTurk) "${coinSymbol}_threshold_btc" else "${coinSymbol}_threshold"
        prefs.edit().putFloat(key, newThreshold.toFloat()).apply()

        val updatedList = _coinDataList.value.map { coin ->
            if (coin.symbol == coinSymbol) {
                coin.copy(alertThreshold = newThreshold)
            } else coin
        }
        _coinDataList.value = updatedList

        val opportunities = findArbitrageOpportunities(updatedList)
        _arbitrageOpportunities.value = opportunities
    }

    fun saveGlobalThreshold(threshold: Double) {
        val prefs = context.getSharedPreferences("coin_settings", Context.MODE_PRIVATE)
        prefs.edit().putFloat("global_threshold", threshold.toFloat()).apply()
    }

    fun getGlobalThreshold(): Double {
        val prefs = context.getSharedPreferences("coin_settings", Context.MODE_PRIVATE)
        return prefs.getFloat("global_threshold", Constants.Numeric.DEFAULT_ALERT_THRESHOLD.toFloat()).toDouble()
    }

    fun updateAllThresholds(newThreshold: Double) {
        val prefs = context.getSharedPreferences("coin_settings", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putFloat("global_threshold", newThreshold.toFloat())


        SupportedCoins.values().forEach { coin ->
            editor.putFloat("${coin.symbol}_threshold", newThreshold.toFloat())
            editor.putFloat("${coin.symbol}_threshold_btc", newThreshold.toFloat())
        }
        editor.apply()

        val updatedList = _coinDataList.value.map { coin ->
            coin.copy(alertThreshold = newThreshold)
        }
        _coinDataList.value = updatedList

        val opportunities = findArbitrageOpportunities(updatedList)
        _arbitrageOpportunities.value = opportunities
    }

    fun updateCoinSoundLevel(coinSymbol: String, soundLevel: Int, isForBtcTurk: Boolean = false) {
        val prefs = context.getSharedPreferences("coin_settings", Context.MODE_PRIVATE)
        val key = if (isForBtcTurk) "${coinSymbol}_sound_level_btc" else "${coinSymbol}_sound_level"
        prefs.edit().putInt(key, soundLevel).apply()

        val updatedList = _coinDataList.value.map { coin ->
            if (coin.symbol == coinSymbol) {
                coin.copy(soundLevel = soundLevel)
            } else coin
        }
        _coinDataList.value = updatedList
    }

    fun updateCoinAlertStatus(coinSymbol: String, isActive: Boolean, isForBtcTurk: Boolean = false) {
        val prefs = context.getSharedPreferences("coin_settings", Context.MODE_PRIVATE)
        val key = if (isForBtcTurk) "${coinSymbol}_alert_active_btc" else "${coinSymbol}_alert_active"
        prefs.edit().putBoolean(key, isActive).apply()

        val updatedList = _coinDataList.value.map { coin ->
            if (coin.symbol == coinSymbol) {
                coin.copy(isAlertActive = isActive)
            } else coin
        }
        _coinDataList.value = updatedList

        val opportunities = findArbitrageOpportunities(updatedList)
        _arbitrageOpportunities.value = opportunities
    }

    private suspend fun fetchParibuData(): Map<String, ParibuTicker> {
        return try {
            val response = paribuApi.getTickers()
            if (response.isSuccessful) {
                val data = response.body() ?: emptyMap()
                data
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private suspend fun fetchBinanceData(): Map<String?, BinanceTickerResponse> {
        return try {
            val response = binanceApi.getBookTickers()
            if (response.isSuccessful) {
                val data = response.body()?.associateBy { it.symbol } ?: emptyMap()
                data
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private suspend fun fetchBtcTurkData(): Map<String?, BtcTurkTicker> {
        return try {
            val response = btcturkApi.getTickers()
            if (response.isSuccessful) {
                val btcturkResponse = response.body()
                val data = btcturkResponse?.data?.associateBy {
                    it.pair?.replace("_", "")
                } ?: emptyMap()
                data
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun calculateArbitrage(
        paribuData: Map<String, ParibuTicker>,
        binanceData: Map<String?, BinanceTickerResponse>,
        btcturkData: Map<String?, BtcTurkTicker>
    ): List<CoinData> {
        val paribuUsdtRate = _usdTryRate.value
        val btcturkUsdtRate = _usdTryRateBtcTurk.value
        val prefs = context.getSharedPreferences("coin_settings", Context.MODE_PRIVATE)

        return SupportedCoins.values().mapNotNull { coin ->
            try {
                val paribuPrice = paribuData[coin.paribuSymbol]?.highestBid
                    ?: Constants.Numeric.DEFAULT_PRICE

                val binanceUsdPrice = binanceData[coin.binanceSymbol]?.askPrice?.toDoubleOrNull()
                    ?: Constants.Numeric.DEFAULT_PRICE

                val binanceTlPriceParibu = if (paribuUsdtRate > Constants.Numeric.DEFAULT_PRICE) {
                    binanceUsdPrice * paribuUsdtRate
                } else {
                    Constants.Numeric.DEFAULT_PRICE
                }

                val btcturkPrice = btcturkData[coin.btcturkSymbol.replace("_", "")]?.bid
                    ?: Constants.Numeric.DEFAULT_PRICE

                val binanceTlPriceBtcTurk = if (btcturkUsdtRate > Constants.Numeric.DEFAULT_PRICE) {
                    binanceUsdPrice * btcturkUsdtRate
                } else {
                    Constants.Numeric.DEFAULT_PRICE
                }

                val paribuDifference = if (paribuPrice > Constants.Numeric.DEFAULT_PRICE) {
                    ((paribuPrice - binanceTlPriceParibu) * Constants.Numeric.PERCENTAGE_MULTIPLIER) / paribuPrice
                } else {
                    Constants.Numeric.DEFAULT_DIFFERENCE
                }

                val btcturkDifference = if (btcturkPrice > Constants.Numeric.DEFAULT_PRICE) {
                    ((btcturkPrice - binanceTlPriceBtcTurk) * Constants.Numeric.PERCENTAGE_MULTIPLIER) / btcturkPrice
                } else {
                    Constants.Numeric.DEFAULT_DIFFERENCE
                }

                val savedThresholdParibu = prefs.getFloat("${coin.symbol}_threshold", Constants.Numeric.DEFAULT_ALERT_THRESHOLD.toFloat()).toDouble()
                val savedThresholdBtc = prefs.getFloat("${coin.symbol}_threshold_btc", Constants.Numeric.DEFAULT_ALERT_THRESHOLD.toFloat()).toDouble()

                val savedSoundLevel = prefs.getInt("${coin.symbol}_sound_level", Constants.Numeric.DEFAULT_SOUND_LEVEL)
                val savedSoundLevelBtc = prefs.getInt("${coin.symbol}_sound_level_btc", Constants.Numeric.DEFAULT_SOUND_LEVEL)

                val savedAlertActive = prefs.getBoolean("${coin.symbol}_alert_active", true)
                val savedAlertActiveBtc = prefs.getBoolean("${coin.symbol}_alert_active_btc", true)

                val currentCoin = _coinDataList.value.find { it.symbol == coin.symbol }
                val currentThreshold = currentCoin?.alertThreshold ?: savedThresholdParibu

                CoinData(
                    symbol = coin.symbol,
                    name = coin.displayName,
                    paribuPrice = paribuPrice,
                    btcturkPrice = btcturkPrice,
                    binancePrice = binanceTlPriceParibu,
                    binancePriceBtcTurk = binanceTlPriceBtcTurk,
                    binancePriceUsd = binanceUsdPrice,
                    paribuDifference = paribuDifference,
                    btcturkDifference = btcturkDifference,
                    alertThreshold = currentThreshold,
                    soundLevel = currentCoin?.soundLevel ?: savedSoundLevel,
                    isAlertActive = currentCoin?.isAlertActive ?: savedAlertActive
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun findArbitrageOpportunities(coins: List<CoinData>): List<ArbitrageOpportunity> {
        val opportunities = mutableListOf<ArbitrageOpportunity>()
        val prefs = context.getSharedPreferences("coin_settings", Context.MODE_PRIVATE)

        coins.forEach { coin ->
            coin.symbol?.let { symbol ->
                coin.paribuDifference?.let { difference ->
                    val threshold = prefs.getFloat("${symbol}_threshold", coin.alertThreshold?.toFloat() ?: Constants.Numeric.DEFAULT_ALERT_THRESHOLD.toFloat()).toDouble()
                    val isAlertActive = prefs.getBoolean("${symbol}_alert_active", coin.isAlertActive ?: Constants.Defaults.ALERT_ACTIVE)

                    if (isAlertActive && kotlin.math.abs(difference) > threshold) {
                        opportunities.add(
                            ArbitrageOpportunity(
                                coin = coin,
                                exchange = Exchange.PARIBU,
                                difference = difference,
                                isPositive = difference > Constants.Numeric.DEFAULT_DIFFERENCE
                            )
                        )
                    }
                }

                coin.btcturkDifference?.let { difference ->
                    val threshold = prefs.getFloat("${symbol}_threshold_btc", coin.alertThreshold?.toFloat() ?: Constants.Numeric.DEFAULT_ALERT_THRESHOLD.toFloat()).toDouble()
                    val isAlertActive = prefs.getBoolean("${symbol}_alert_active_btc", coin.isAlertActive ?: Constants.Defaults.ALERT_ACTIVE)

                    if (isAlertActive && kotlin.math.abs(difference) > threshold) {
                        opportunities.add(
                            ArbitrageOpportunity(
                                coin = coin,
                                exchange = Exchange.BTCTURK,
                                difference = difference,
                                isPositive = difference > Constants.Numeric.DEFAULT_DIFFERENCE
                            )
                        )
                    }
                }
            }
        }

        return opportunities.sortedByDescending { kotlin.math.abs(it.difference ?: 0.0) }
    }

    fun getSupportedCoins(): List<SupportedCoins> = SupportedCoins.values().toList()
    fun getSupportedCoinsForParibu(): List<String> = SupportedCoins.getAllParibuSymbols()
    fun getSupportedCoinsForBtcturk(): List<String> = SupportedCoins.getAllBtcturkSymbols()
    fun getSupportedCoinsForBinance(): List<String> = SupportedCoins.getAllBinanceSymbols()

    fun updateAllSoundLevels(level: Int) {
        val prefs = context.getSharedPreferences("coin_settings", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        SupportedCoins.values().forEach { coin ->
            editor.putInt("${coin.symbol}_sound_level", level)
            editor.putInt("${coin.symbol}_sound_level_btc", level)
        }
        editor.apply()

        val updatedList = _coinDataList.value.map { coin ->
            coin.copy(soundLevel = level)
        }
        _coinDataList.value = updatedList
    }

    fun updateRefreshRate(rate: Float) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putFloat("refresh_rate", rate).apply()
    }
}