package com.vakifbank.bigotsv2.data.repository

import android.util.Log
import com.vakifbank.bigotsv2.domain.model.ArbitrageOpportunity
import com.vakifbank.bigotsv2.domain.model.CoinData
import com.vakifbank.bigotsv2.domain.model.Exchange
import com.vakifbank.bigotsv2.domain.model.SupportedCoins
import com.vakifbank.bigotsv2.domain.model.binance.BinanceTickerResponse
import com.vakifbank.bigotsv2.domain.model.btcturk.BtcTurkTicker
import com.vakifbank.bigotsv2.domain.model.paribu.ParibuTicker
import com.vakifbank.bigotsv2.data.service.ApiClient
import com.vakifbank.bigotsv2.data.service.BinanceApiService
import com.vakifbank.bigotsv2.data.service.BtcTurkApiService
import com.vakifbank.bigotsv2.data.service.ParibuApiService
import com.vakifbank.bigotsv2.utils.Constants
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class CryptoRepository @Inject constructor(
    private val paribuApi: ParibuApiService,
    private val binanceApi: BinanceApiService,
    private val btcturkApi: BtcTurkApiService
) {

    companion object {
        @Volatile
        private var INSTANCE: CryptoRepository? = null

        fun getInstance(
            paribuApi: ParibuApiService,
            binanceApi: BinanceApiService,
            btcturkApi: BtcTurkApiService
        ): CryptoRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CryptoRepository(paribuApi, binanceApi, btcturkApi).also {
                    INSTANCE = it
                }
            }
        }
    }

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

    private suspend fun fetchParibuData(): Map<String, ParibuTicker> {
        return try {
            val response = paribuApi.getTickers()
            if (response.isSuccessful) {
                val data = response.body() ?: emptyMap()
                data[Constants.ApiSymbols.USDT_TL]?.let { usdtTicker -> }
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
            val response =binanceApi.getBookTickers()
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
                    alertThreshold = Constants.Numeric.DEFAULT_ALERT_THRESHOLD,
                    soundLevel = Constants.Numeric.DEFAULT_SOUND_LEVEL,
                    isAlertActive = Constants.Defaults.ALERT_ACTIVE
                )
            } catch (e: Exception) {
                null
            }
        }
    }


    private fun calculateDifferencePercentage(localPrice: Double, binancePrice: Double): Double {
        return if (localPrice > Constants.Numeric.DEFAULT_PRICE && binancePrice > Constants.Numeric.DEFAULT_PRICE) {
            ((localPrice - binancePrice) * Constants.Numeric.PERCENTAGE_MULTIPLIER) / localPrice
        } else {
            Constants.Numeric.DEFAULT_DIFFERENCE
        }
    }

    private fun findArbitrageOpportunities(coins: List<CoinData>): List<ArbitrageOpportunity> {
        val opportunities = mutableListOf<ArbitrageOpportunity>()

        coins.forEach { coin ->
            coin.paribuDifference?.let { difference ->
                if (kotlin.math.abs(difference) > coin.alertThreshold!!) {
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
                if (kotlin.math.abs(difference) > coin.alertThreshold!!) {
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

        return opportunities.sortedByDescending { kotlin.math.abs(it.difference!!) }
    }

    fun getSupportedCoins(): List<SupportedCoins> = SupportedCoins.values().toList()
    fun getSupportedCoinsForParibu(): List<String> = SupportedCoins.getAllParibuSymbols()
    fun getSupportedCoinsForBtcturk(): List<String> = SupportedCoins.getAllBtcturkSymbols()
    fun getSupportedCoinsForBinance(): List<String> = SupportedCoins.getAllBinanceSymbols()
}