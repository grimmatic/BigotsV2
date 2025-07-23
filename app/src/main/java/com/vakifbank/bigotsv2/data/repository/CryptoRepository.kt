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
import com.vakifbank.bigotsv2.utils.Constants
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CryptoRepository private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: CryptoRepository? = null

        fun getInstance(): CryptoRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CryptoRepository().also { INSTANCE = it }
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
                    _usdTryRate.value = usdtTicker.lowestAsk ?: Constants.Numeric.DEFAULT_PRICE
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
            val response = ApiClient.paribuApi.getTickers()
            if (response.isSuccessful) {
                response.body() ?: emptyMap()
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private suspend fun fetchBinanceData(): Map<String?, BinanceTickerResponse> {
        return try {
            val response = ApiClient.binanceApi.getBookTickers()
            if (response.isSuccessful) {
                response.body()?.associateBy { it.symbol } ?: emptyMap()
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private suspend fun fetchBtcTurkData(): Map<String?, BtcTurkTicker> {
        return try {
            val response = ApiClient.btcturkApi.getTickers()
            if (response.isSuccessful) {
                response.body()?.data?.associateBy { it.pair } ?: emptyMap()
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
        val usdTryRate = _usdTryRate.value

        return SupportedCoins.values().mapNotNull { coin ->
            try {
                val paribuPrice = paribuData[coin.paribuSymbol]?.highestBid
                    ?: Constants.Numeric.DEFAULT_PRICE

                val binanceUsdPrice = binanceData[coin.binanceSymbol]?.askPrice?.toDoubleOrNull()
                    ?: Constants.Numeric.DEFAULT_PRICE
                val binanceTlPrice = if (usdTryRate > Constants.Numeric.DEFAULT_PRICE) {
                    binanceUsdPrice * usdTryRate
                } else {
                    Constants.Numeric.DEFAULT_PRICE
                }

                val btcturkPrice = btcturkData[coin.btcturkSymbol]?.bid
                    ?: Constants.Numeric.DEFAULT_PRICE

                val paribuDifference = calculateDifferencePercentage(paribuPrice, binanceTlPrice)
                val btcturkDifference = calculateDifferencePercentage(btcturkPrice, binanceTlPrice)

                CoinData(
                    symbol = coin.symbol,
                    name = coin.displayName,
                    paribuPrice = paribuPrice,
                    btcturkPrice = btcturkPrice,
                    binancePrice = binanceTlPrice,
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