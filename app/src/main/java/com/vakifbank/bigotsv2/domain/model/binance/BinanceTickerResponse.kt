package com.vakifbank.bigotsv2.domain.model.binance

data class BinanceTickerResponse(
    val symbol: String?,
    val bidPrice: String?,
    val askPrice: String?
)