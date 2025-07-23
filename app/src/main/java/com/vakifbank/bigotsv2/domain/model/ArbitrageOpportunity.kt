package com.vakifbank.bigotsv2.domain.model

data class ArbitrageOpportunity(
    val coin: CoinData?,
    val exchange: Exchange?,
    val difference: Double?,
    val isPositive: Boolean?
)