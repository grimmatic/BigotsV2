package com.vakifbank.bigotsv2.data.repository

import android.util.Log
import com.vakifbank.bigotsv2.data.Config
import com.vakifbank.bigotsv2.data.api.ApiClient
import com.vakifbank.bigotsv2.data.model.ArbitrageOpportunity
import com.vakifbank.bigotsv2.data.model.BinanceTickerResponse
import com.vakifbank.bigotsv2.data.model.BtcTurkTicker
import com.vakifbank.bigotsv2.data.model.CoinData
import com.vakifbank.bigotsv2.data.model.Exchange
import com.vakifbank.bigotsv2.data.model.ParibuTicker
import com.vakifbank.bigotsv2.data.model.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CryptoRepository {

    private val _coinDataList = MutableStateFlow<List<CoinData>>(emptyList())
    val coinDataList: Flow<List<CoinData>> = _coinDataList.asStateFlow()

    private val _arbitrageOpportunities = MutableStateFlow<List<ArbitrageOpportunity>>(emptyList())
    val arbitrageOpportunities: Flow<List<ArbitrageOpportunity>> =
        _arbitrageOpportunities.asStateFlow()

    private val _usdTryRate = MutableStateFlow(0.0)
    val usdTryRate: Flow<Double> = _usdTryRate.asStateFlow()

    private val supportedCoins = Config.supportedCoins
    private var lastFetchTime = 0L
    private val cacheDuration = 10000 // 10 seconds

    suspend fun fetchAllData() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFetchTime < cacheDuration) {
            Log.d("CryptoRepository", "Using cached data")
            return
        }
        lastFetchTime = currentTime
        try {
            Log.d("CryptoRepository", "Starting data fetch...")

            coroutineScope {
                val paribuDeferred = async { fetchParibuData() }
                val binanceDeferred = async { fetchBinanceData() }
                val btcturkDeferred = async { fetchBtcTurkData() }

                val paribuResult = paribuDeferred.await()
                val binanceResult = binanceDeferred.await()
                val btcturkResult = btcturkDeferred.await()

                val paribuData = if (paribuResult is Result.Success) paribuResult.data else emptyMap()
                val binanceData = if (binanceResult is Result.Success) binanceResult.data else emptyMap()
                val btcturkData = if (btcturkResult is Result.Success) btcturkResult.data else emptyMap()


                Log.d("CryptoRepository", "Paribu data size: ${paribuData.size}")
                Log.d("CryptoRepository", "Binance data size: ${binanceData.size}")
                Log.d("CryptoRepository", "BtcTurk data size: ${btcturkData.size}")

                paribuData["USDT_TL"]?.let { usdtTicker ->
                    _usdTryRate.value = usdtTicker.lowestAsk
                    Log.d("CryptoRepository", "USD/TRY Rate: ${usdtTicker.lowestAsk}")
                }

                val updatedCoins = calculateArbitrage(paribuData, binanceData, btcturkData)
                Log.d("CryptoRepository", "Updated coins count: ${updatedCoins.size}")

                _coinDataList.value = updatedCoins

                val opportunities = findArbitrageOpportunities(updatedCoins)
                Log.d("CryptoRepository", "Arbitrage opportunities: ${opportunities.size}")

                _arbitrageOpportunities.value = opportunities
            }
        } catch (e: Exception) {
            Log.e("CryptoRepository", "Error fetching data", e)
        }
    }

    private suspend fun fetchParibuData(): Result<Map<String, ParibuTicker>> {
        return try {
            Log.d("CryptoRepository", "Fetching Paribu data...")
            val response = ApiClient.paribuApi.getTickers()
            if (response.isSuccessful) {
                val data = response.body() ?: emptyMap()
                Log.d("CryptoRepository", "Paribu success: ${data.size} tickers")
                Result.Success(data)
            } else {
                Log.e("CryptoRepository", "Paribu API error: ${response.code()}")
                Result.Error(Exception("Paribu API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CryptoRepository", "Paribu fetch error", e)
            Result.Error(e)
        }
    }

    private suspend fun fetchBinanceData(): Result<Map<String, BinanceTickerResponse>> {
        return try {
            Log.d("CryptoRepository", "Fetching Binance data...")
            val response = ApiClient.binanceApi.getBookTickers()
            if (response.isSuccessful) {
                val data = response.body()?.associateBy { it.symbol } ?: emptyMap()
                Log.d("CryptoRepository", "Binance success: ${data.size} tickers")
                Result.Success(data)
            } else {
                Log.e("CryptoRepository", "Binance API error: ${response.code()}")
                Result.Error(Exception("Binance API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CryptoRepository", "Binance fetch error", e)
            Result.Error(e)
        }
    }

    private suspend fun fetchBtcTurkData(): Result<Map<String, BtcTurkTicker>> {
        return try {
            Log.d("CryptoRepository", "Fetching BtcTurk data...")
            val response = ApiClient.btcturkApi.getTickers()
            if (response.isSuccessful) {
                val data = response.body()?.data?.associateBy { it.pair } ?: emptyMap()
                Log.d("CryptoRepository", "BtcTurk success: ${data.size} tickers")
                Result.Success(data)
            } else {
                Log.e("CryptoRepository", "BtcTurk API error: ${response.code()}")
                Result.Error(Exception("BtcTurk API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CryptoRepository", "BtcTurk fetch error", e)
            Result.Error(e)
        }
    }

    private fun calculateArbitrage(
        paribuData: Map<String, ParibuTicker>,
        binanceData: Map<String, BinanceTickerResponse>,
        btcturkData: Map<String, BtcTurkTicker>
    ): List<CoinData> {
        val usdTryRate = _usdTryRate.value
        Log.d("CryptoRepository", "Calculating arbitrage with USD/TRY rate: $usdTryRate")

        return supportedCoins.mapNotNull { coinSymbol ->
            try {
                val baseSymbol = coinSymbol.replace("_TL", "").replace("_TRY", "")

                val paribuPrice = getParibuPrice(paribuData, coinSymbol)
                val binanceTlPrice = getBinanceTlPrice(binanceData, baseSymbol, usdTryRate)
                val btcturkPrice = getBtcTurkPrice(btcturkData, coinSymbol)

                val paribuDifference = calculateDifference(paribuPrice, binanceTlPrice)
                val btcturkDifference = calculateDifference(btcturkPrice, binanceTlPrice)

                val coin = CoinData(
                    symbol = baseSymbol,
                    name = baseSymbol,
                    paribuPrice = paribuPrice,
                    btcturkPrice = btcturkPrice,
                    binancePrice = binanceTlPrice,
                    paribuDifference = paribuDifference,
                    btcturkDifference = btcturkDifference
                )

                Log.d("CryptoRepository", "Coin: ${coin.symbol}, Paribu: ${coin.paribuPrice}, Binance: ${coin.binancePrice}, BTCTurk: ${coin.btcturkPrice}")
                coin
            } catch (e: Exception) {
                Log.e("CryptoRepository", "Error calculating arbitrage for $coinSymbol", e)
                null
            }
        }
    }

    private fun getParibuPrice(paribuData: Map<String, ParibuTicker>, coinSymbol: String): Double {
        return paribuData[coinSymbol]?.highestBid ?: 0.0
    }

    private fun getBinanceTlPrice(binanceData: Map<String, BinanceTickerResponse>, baseSymbol: String, usdTryRate: Double): Double {
        val binanceUsdPrice = binanceData["${baseSymbol}USDT"]?.askPrice?.toDoubleOrNull() ?: 0.0
        return if (usdTryRate > 0) binanceUsdPrice * usdTryRate else 0.0
    }

    private fun getBtcTurkPrice(btcturkData: Map<String, BtcTurkTicker>, coinSymbol: String): Double {
        val btcturkSymbol = coinSymbol.replace("_TL", "_TRY").replace("_", "")
        return btcturkData[btcturkSymbol]?.bid ?: 0.0
    }

    private fun calculateDifference(price1: Double, price2: Double): Double {
        return if (price1 > 0 && price2 > 0) {
            ((price1 - price2) * 100.0) / price1
        } else 0.0
    }

    private fun findArbitrageOpportunities(coins: List<CoinData>): List<ArbitrageOpportunity> {
        val opportunities = mutableListOf<ArbitrageOpportunity>()

        coins.forEach { coin ->
            if (kotlin.math.abs(coin.paribuDifference) > coin.alertThreshold) {
                opportunities.add(
                    ArbitrageOpportunity(
                        coin = coin,
                        exchange = Exchange.PARIBU,
                        difference = coin.paribuDifference,
                        isPositive = coin.paribuDifference > 0
                    )
                )
            }

            if (kotlin.math.abs(coin.btcturkDifference) > coin.alertThreshold) {
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

        return opportunities.sortedByDescending { kotlin.math.abs(it.difference) }
    }

    fun updateThreshold(coinSymbol: String, threshold: Double) {
        val currentList = _coinDataList.value.toMutableList()
        val coinIndex = currentList.indexOfFirst { it.symbol == coinSymbol }
        if (coinIndex != -1) {
            val updatedCoin = currentList[coinIndex].copy(alertThreshold = threshold)
            currentList[coinIndex] = updatedCoin
            _coinDataList.value = currentList
        }
    }

    fun updateAllThresholds(threshold: Double) {
        val currentList = _coinDataList.value.toMutableList()
        val updatedList = currentList.map { it.copy(alertThreshold = threshold) }
        _coinDataList.value = updatedList
    }
}