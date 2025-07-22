package com.vakifbank.bigotsv2.data.model

data class CoinData(
    val symbol: String,
    val name: String,
    val paribuPrice: Double = 0.0,
    val btcturkPrice: Double = 0.0,
    val binancePrice: Double = 0.0,
    val paribuDifference: Double = 0.0,
    val btcturkDifference: Double = 0.0,
    var alertThreshold: Double = 2.5,
    val soundLevel: Int = 15,
    val isAlertActive: Boolean = false
)