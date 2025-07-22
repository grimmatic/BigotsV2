package com.vakifbank.bigotsv2.data.repository

import android.util.Log
import com.vakifbank.bigotsv2.data.model.ArbitrageOpportunity
import com.vakifbank.bigotsv2.data.model.CoinData
import com.vakifbank.bigotsv2.data.model.Exchange
import com.vakifbank.bigotsv2.data.model.SupportedCoins
import com.vakifbank.bigotsv2.data.model.binance.BinanceTickerResponse
import com.vakifbank.bigotsv2.data.model.btcturk.BtcTurkTicker
import com.vakifbank.bigotsv2.data.model.paribu.ParibuTicker
import com.vakifbank.bigotsv2.data.service.ApiClient
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

    private val _usdTryRate = MutableStateFlow(0.0)
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


                paribuData["USDT_TL"]?.let { usdtTicker ->
                    _usdTryRate.value = usdtTicker.lowestAsk!!
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
            val response = ApiClient.binanceApi.getBookTickers()
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
            val response = ApiClient.btcturkApi.getTickers()
            if (response.isSuccessful) {
                val data = response.body()?.data?.associateBy { it.pair } ?: emptyMap()
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
        val usdTryRate = _usdTryRate.value

        return SupportedCoins.values().mapNotNull { coin ->
            try {
                // Paribu fiyatı
                val paribuPrice = paribuData[coin.paribuSymbol]?.highestBid ?: 0.0

                // Binance fiyatı (USD cinsinden)
                val binanceUsdPrice =
                    binanceData[coin.binanceSymbol]?.askPrice?.toDoubleOrNull() ?: 0.0
                val binanceTlPrice = if (usdTryRate > 0) binanceUsdPrice * usdTryRate else 0.0

                // BtcTurk fiyatı
                val btcturkPrice = btcturkData[coin.btcturkSymbol]?.bid ?: 0.0

                // Arbitraj hesaplamaları
                val paribuDifference = if (paribuPrice > 0 && binanceTlPrice > 0) {
                    ((paribuPrice - binanceTlPrice) * 100.0) / paribuPrice
                } else 0.0

                val btcturkDifference = if (btcturkPrice > 0 && binanceTlPrice > 0) {
                    ((btcturkPrice - binanceTlPrice) * 100.0) / btcturkPrice
                } else 0.0

                val coinData = CoinData(
                    symbol = coin.symbol,
                    name = coin.displayName,
                    paribuPrice = paribuPrice,
                    btcturkPrice = btcturkPrice,
                    binancePrice = binanceTlPrice,
                    paribuDifference = paribuDifference,
                    btcturkDifference = btcturkDifference
                )

                coinData
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun findArbitrageOpportunities(coins: List<CoinData>): List<ArbitrageOpportunity> {
        val opportunities = mutableListOf<ArbitrageOpportunity>()

        coins.forEach { coin ->
            if (kotlin.math.abs(coin.paribuDifference!!) > coin.alertThreshold!!) {
                opportunities.add(
                    ArbitrageOpportunity(
                        coin = coin,
                        exchange = Exchange.PARIBU,
                        difference = coin.paribuDifference,
                        isPositive = coin.paribuDifference > 0
                    )
                )
            }

            if (kotlin.math.abs(coin.btcturkDifference!!) > coin.alertThreshold) {
                opportunities.add(
                    ArbitrageOpportunity(
                        coin = coin,
                        exchange = Exchange.BTCTURK,
                        difference = coin.btcturkDifference,
                        isPositive = coin.btcturkDifference > 0
                    )
                )
            }
        }

        return opportunities.sortedByDescending { kotlin.math.abs(it.difference!!) }
    }

    fun getSupportedCoins(): List<SupportedCoins> = SupportedCoins.values().toList()

    fun getSupportedCoinsForParibu(): List<String> = SupportedCoins.getAllParibuSymbols()
    fun getSupportedCoinsForBtcturk(): List<String> = SupportedCoins.getAllBtcturkSymbols()
    fun getSupportedCoinsForBinance(): List<String> = SupportedCoins.getAllBinanceSymbols()
}