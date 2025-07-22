package com.vakifbank.bigotsv2.data.repository

import android.util.Log
import com.vakifbank.bigotsv2.data.api.ApiClient
import com.vakifbank.bigotsv2.data.model.ArbitrageOpportunity
import com.vakifbank.bigotsv2.data.model.BinanceTickerResponse
import com.vakifbank.bigotsv2.data.model.BtcTurkTicker
import com.vakifbank.bigotsv2.data.model.CoinData
import com.vakifbank.bigotsv2.data.model.Exchange
import com.vakifbank.bigotsv2.data.model.ParibuTicker
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

    private val supportedCoins = listOf(
        "DOT_TL", "AVAX_TL", "TRX_TL", "EOS_TL", "BTTC_TL", "XRP_TL", "XLM_TL",
        "ONT_TL", "ATOM_TL", "HOT_TL", "NEO_TL", "BAT_TL", "CHZ_TL", "UNI_TL",
        "BAL_TL", "AAVE_TL", "LINK_TL", "MKR_TL", "W_TL", "RAY_TL", "LRC_TL",
        "BAND_TL", "ALGO_TL", "GRT_TL", "ENJ_TL", "THETA_TL", "MATIC_TL",
        "OXT_TL", "CRV_TL", "OGN_TL", "MANA_TL", "MIOTA_TL", "SOL_TL",
        "APE_TL", "VET_TL", "ANKR_TL", "SHIB_TL", "LPT_TL", "INJ_TL",
        "ICP_TL", "FTM_TL", "AXS_TL", "ENS_TL", "SAND_TL", "AUDIO_TL",
        "BTC_TL", "ETH_TL"
    )

    suspend fun fetchAllData() {
        try {
            Log.d("CryptoRepository", "Starting data fetch...")

            coroutineScope {
                val paribuDeferred = async { fetchParibuData() }
                val binanceDeferred = async { fetchBinanceData() }
                val btcturkDeferred = async { fetchBtcTurkData() }

                val paribuData = paribuDeferred.await()
                val binanceData = binanceDeferred.await()
                val btcturkData = btcturkDeferred.await()

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

    private suspend fun fetchParibuData(): Map<String, ParibuTicker> {
        return try {
            Log.d("CryptoRepository", "Fetching Paribu data...")
            val response = ApiClient.paribuApi.getTickers()
            if (response.isSuccessful) {
                val data = response.body() ?: emptyMap()
                Log.d("CryptoRepository", "Paribu success: ${data.size} tickers")
                data
            } else {
                Log.e("CryptoRepository", "Paribu API error: ${response.code()}")
                emptyMap()
            }
        } catch (e: Exception) {
            Log.e("CryptoRepository", "Paribu fetch error", e)
            emptyMap()
        }
    }

    private suspend fun fetchBinanceData(): Map<String, BinanceTickerResponse> {
        return try {
            Log.d("CryptoRepository", "Fetching Binance data...")
            val response = ApiClient.binanceApi.getBookTickers()
            if (response.isSuccessful) {
                val data = response.body()?.associateBy { it.symbol } ?: emptyMap()
                Log.d("CryptoRepository", "Binance success: ${data.size} tickers")
                data
            } else {
                Log.e("CryptoRepository", "Binance API error: ${response.code()}")
                emptyMap()
            }
        } catch (e: Exception) {
            Log.e("CryptoRepository", "Binance fetch error", e)
            emptyMap()
        }
    }

    private suspend fun fetchBtcTurkData(): Map<String, BtcTurkTicker> {
        return try {
            Log.d("CryptoRepository", "Fetching BtcTurk data...")
            val response = ApiClient.btcturkApi.getTickers()
            if (response.isSuccessful) {
                val data = response.body()?.data?.associateBy { it.pair } ?: emptyMap()
                Log.d("CryptoRepository", "BtcTurk success: ${data.size} tickers")
                data
            } else {
                Log.e("CryptoRepository", "BtcTurk API error: ${response.code()}")
                emptyMap()
            }
        } catch (e: Exception) {
            Log.e("CryptoRepository", "BtcTurk fetch error", e)
            emptyMap()
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

                // Paribu fiyatı
                val paribuPrice = paribuData[coinSymbol]?.highestBid ?: 0.0

                // Binance fiyatı (USD cinsinden)
                val binanceUsdPrice =
                    binanceData["${baseSymbol}USDT"]?.askPrice?.toDoubleOrNull() ?: 0.0
                val binanceTlPrice = if (usdTryRate > 0) binanceUsdPrice * usdTryRate else 0.0

                // BtcTurk fiyatı
                val btcturkSymbol = coinSymbol.replace("_TL", "_TRY").replace("_", "")
                val btcturkPrice = btcturkData[btcturkSymbol]?.bid ?: 0.0

                // Arbitraj hesaplamaları
                val paribuDifference = if (paribuPrice > 0 && binanceTlPrice > 0) {
                    ((paribuPrice - binanceTlPrice) * 100.0) / paribuPrice
                } else 0.0

                val btcturkDifference = if (btcturkPrice > 0 && binanceTlPrice > 0) {
                    ((btcturkPrice - binanceTlPrice) * 100.0) / btcturkPrice
                } else 0.0

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
}