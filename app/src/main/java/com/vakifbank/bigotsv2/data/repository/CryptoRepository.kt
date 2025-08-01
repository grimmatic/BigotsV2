package com.vakifbank.bigotsv2.data.repository

import android.content.Context
import androidx.core.content.edit
import com.vakifbank.bigotsv2.data.service.BinanceApiService
import com.vakifbank.bigotsv2.data.service.BtcTurkApiService
import com.vakifbank.bigotsv2.data.service.ParibuApiService
import com.vakifbank.bigotsv2.domain.model.ArbitrageOpportunity
import com.vakifbank.bigotsv2.domain.model.CoinData
import com.vakifbank.bigotsv2.domain.model.Exchange
import com.vakifbank.bigotsv2.domain.model.SupportedCoins
import com.vakifbank.bigotsv2.domain.model.binance.BinanceTickerResponse
import com.vakifbank.bigotsv2.domain.model.btcturk.BtcTurkTicker
import com.vakifbank.bigotsv2.domain.model.paribu.ParibuTicker
import com.vakifbank.bigotsv2.utils.Constants.ApiSymbols
import com.vakifbank.bigotsv2.utils.Constants.Numeric
import com.vakifbank.bigotsv2.utils.Constants.SharedPreferences
import com.vakifbank.bigotsv2.utils.Constants.SharedPreferencesSuffixes
import com.vakifbank.bigotsv2.utils.Constants.Strings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

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

    private val _usdTryRate = MutableStateFlow(Numeric.DEFAULT_PRICE)
    val usdTryRate: Flow<Double> = _usdTryRate.asStateFlow()

    private val _usdTryRateBtcTurk = MutableStateFlow(Numeric.DEFAULT_PRICE)
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

                paribuData[ApiSymbols.USDT_TL]?.let { usdtTicker ->
                    val usdtRate = usdtTicker.lowestAsk ?: Numeric.DEFAULT_PRICE
                    _usdTryRate.value = usdtRate
                }

                btcturkData[ApiSymbols.USDT_TRY]?.let { usdtTicker ->
                    val usdtRate = usdtTicker.ask ?: Numeric.DEFAULT_PRICE
                    _usdTryRateBtcTurk.value = usdtRate
                }

                val updatedCoins = calculateArbitrage(paribuData, binanceData, btcturkData)
                _coinDataList.value = updatedCoins
                val opportunities = findArbitrageOpportunities(updatedCoins)

                _arbitrageOpportunities.value = opportunities
            }
        } catch (e: Exception) {
        }
    }

    fun updateCoinThreshold(
        coinSymbol: String,
        newThreshold: Double,
        isForBtcTurk: Boolean = false
    ) {
        val prefs =
            context.getSharedPreferences(SharedPreferences.COIN_SETTINGS, Context.MODE_PRIVATE)
        val key =
            if (isForBtcTurk) "${coinSymbol}${SharedPreferencesSuffixes.THRESHOLD_BTC}" else "${coinSymbol}${SharedPreferencesSuffixes.THRESHOLD}"
        prefs.edit { putFloat(key, newThreshold.toFloat()) }

        val updatedList = _coinDataList.value.map { coin ->
            if (coin.symbol == coinSymbol) {
                coin.copy(alertThreshold = newThreshold)
            } else coin
        }
        _coinDataList.value = updatedList

        val opportunities = findArbitrageOpportunities(updatedList)
        _arbitrageOpportunities.value = opportunities
    }

    fun getGlobalThreshold(): Double {
        val prefs =
            context.getSharedPreferences(SharedPreferences.COIN_SETTINGS, Context.MODE_PRIVATE)
        return prefs.getFloat(
            SharedPreferences.GLOBAL_THRESHOLD,
            Numeric.DEFAULT_ALERT_THRESHOLD.toFloat()
        ).toDouble()
    }

    fun updateAllThresholds(newThreshold: Double) {
        val prefs =
            context.getSharedPreferences(SharedPreferences.COIN_SETTINGS, Context.MODE_PRIVATE)
        prefs.edit {
            putFloat(SharedPreferences.GLOBAL_THRESHOLD, newThreshold.toFloat())

            SupportedCoins.entries.forEach { coin ->
                putFloat(
                    "${coin.symbol}${SharedPreferencesSuffixes.THRESHOLD}",
                    newThreshold.toFloat()
                )
                putFloat(
                    "${coin.symbol}${SharedPreferencesSuffixes.THRESHOLD_BTC}",
                    newThreshold.toFloat()
                )
            }
        }

        val updatedList = _coinDataList.value.map { coin ->
            coin.copy(alertThreshold = newThreshold)
        }
        _coinDataList.value = updatedList

        val opportunities = findArbitrageOpportunities(updatedList)
        _arbitrageOpportunities.value = opportunities
    }

    fun updateCoinSoundLevel(coinSymbol: String, soundLevel: Int, isForBtcTurk: Boolean = false) {
        val prefs =
            context.getSharedPreferences(SharedPreferences.COIN_SETTINGS, Context.MODE_PRIVATE)
        val key =
            if (isForBtcTurk) "${coinSymbol}${SharedPreferencesSuffixes.SOUND_LEVEL_BTC}" else "${coinSymbol}${SharedPreferencesSuffixes.SOUND_LEVEL}"
        prefs.edit { putInt(key, soundLevel) }

        val updatedList = _coinDataList.value.map { coin ->
            if (coin.symbol == coinSymbol) {
                coin.copy(soundLevel = soundLevel)
            } else coin
        }
        _coinDataList.value = updatedList
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
                    it.pair?.replace(Strings.UNDERSCORE, Numeric.EMPTY)
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
        val prefs =
            context.getSharedPreferences(SharedPreferences.COIN_SETTINGS, Context.MODE_PRIVATE)

        return SupportedCoins.entries.mapNotNull { coin ->
            try {

                val paribuPrice = paribuData[coin.paribuSymbol]?.highestBid
                    ?: Numeric.DEFAULT_PRICE

                val binanceUsdPrice = binanceData[coin.binanceSymbol]?.askPrice?.toDoubleOrNull()
                    ?: Numeric.DEFAULT_PRICE

                val binanceTlPriceParibu = paribuUsdtRate.takeIf { it > Numeric.DEFAULT_PRICE }
                    ?.let { rate -> binanceUsdPrice * rate }
                    ?: Numeric.DEFAULT_PRICE

                val btcturkPrice = btcturkData[coin.btcturkSymbol.replace(
                    oldValue = Strings.UNDERSCORE,
                    newValue = Numeric.EMPTY
                )]?.bid
                    ?: Numeric.DEFAULT_PRICE

                val binanceTlPriceBtcTurk = btcturkUsdtRate.takeIf { it > Numeric.DEFAULT_PRICE }
                    ?.let { rate -> binanceUsdPrice * rate }
                    ?: Numeric.DEFAULT_PRICE

                val isParibuSupported = paribuData.containsKey(coin.paribuSymbol)
                val isBtcTurkSupported = btcturkData.containsKey(coin.btcturkSymbol.replace(
                    oldValue = Strings.UNDERSCORE,
                    newValue = Numeric.EMPTY
                ))

                if (isParibuSupported && (! (paribuPrice > 0.0 && binanceTlPriceParibu > 0.0))) {
                    return@mapNotNull null
                }
                if (isBtcTurkSupported && (! (btcturkPrice > 0.0 && binanceTlPriceBtcTurk > 0.0))) {
                    return@mapNotNull null
                }

                val paribuDifference =
                    paribuPrice.takeIf { it > Numeric.DEFAULT_PRICE }?.let {
                        ((paribuPrice - binanceTlPriceParibu) * Numeric.PERCENTAGE_MULTIPLIER) / paribuPrice
                    } ?: run {
                        Numeric.DEFAULT_DIFFERENCE
                    }

                val btcturkDifference = btcturkPrice.takeIf { it > Numeric.DEFAULT_PRICE }
                    ?.let { price ->
                        ((price - binanceTlPriceBtcTurk) * Numeric.PERCENTAGE_MULTIPLIER) / price
                    } ?: Numeric.DEFAULT_DIFFERENCE

                val savedThresholdParibu = prefs.getFloat(
                    "${coin.symbol}${SharedPreferencesSuffixes.THRESHOLD}",
                    Numeric.DEFAULT_ALERT_THRESHOLD.toFloat()
                ).toDouble()

                val savedSoundLevel = prefs.getInt(
                    "${coin.symbol}${SharedPreferencesSuffixes.SOUND_LEVEL}",
                    Numeric.DEFAULT_SOUND_LEVEL
                )

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
                    soundLevel = currentCoin?.soundLevel ?: savedSoundLevel
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun findArbitrageOpportunities(coins: List<CoinData>): List<ArbitrageOpportunity> {
        val opportunities = mutableListOf<ArbitrageOpportunity>()
        val prefs =
            context.getSharedPreferences(SharedPreferences.COIN_SETTINGS, Context.MODE_PRIVATE)

        coins.forEach { coin ->
            coin.symbol?.let { symbol ->
                coin.paribuDifference?.let { difference ->
                    val threshold = prefs.getFloat(
                        "${symbol}${SharedPreferencesSuffixes.THRESHOLD}",
                        coin.alertThreshold?.toFloat()
                            ?: Numeric.DEFAULT_ALERT_THRESHOLD.toFloat()
                    ).toDouble()

                    if (kotlin.math.abs(difference) > threshold) {
                        opportunities.add(
                            ArbitrageOpportunity(
                                coin = coin,
                                exchange = Exchange.PARIBU,
                                difference = difference,
                                isPositive = difference > Numeric.DEFAULT_DIFFERENCE
                            )
                        )
                    }
                }

                coin.btcturkDifference?.let { difference ->
                    val threshold = prefs.getFloat(
                        "${symbol}${SharedPreferencesSuffixes.THRESHOLD_BTC}",
                        coin.alertThreshold?.toFloat()
                            ?: Numeric.DEFAULT_ALERT_THRESHOLD.toFloat()
                    ).toDouble()

                    if (kotlin.math.abs(difference) > threshold) {
                        opportunities.add(
                            ArbitrageOpportunity(
                                coin = coin,
                                exchange = Exchange.BTCTURK,
                                difference = difference,
                                isPositive = difference > Numeric.DEFAULT_DIFFERENCE
                            )
                        )
                    }
                }
            }
        }

        return opportunities.sortedByDescending { kotlin.math.abs(it.difference ?: 0.0) }
    }

    fun updateAllSoundLevels(level: Int) {
        val prefs =
            context.getSharedPreferences(SharedPreferences.COIN_SETTINGS, Context.MODE_PRIVATE)
        prefs.edit {
            SupportedCoins.entries.forEach { coin ->
                putInt("${coin.symbol}${SharedPreferencesSuffixes.SOUND_LEVEL}", level)
                putInt("${coin.symbol}${SharedPreferencesSuffixes.SOUND_LEVEL_BTC}", level)
            }
        }

        val updatedList = _coinDataList.value.map { coin ->
            coin.copy(soundLevel = level)
        }
        _coinDataList.value = updatedList
    }

    fun updateRefreshRate(rate: Float) {
        val prefs =
            context.getSharedPreferences(SharedPreferences.APP_SETTINGS, Context.MODE_PRIVATE)
        prefs.edit { putFloat(SharedPreferences.REFRESH_RATE, rate) }
    }
}