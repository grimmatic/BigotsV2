package com.vakifbank.bigotsv2.data.model

data class ArbitrageOpportunity(
    val coin: CoinData,
    val exchange: Exchange,
    val difference: Double,
    val isPositive: Boolean
)