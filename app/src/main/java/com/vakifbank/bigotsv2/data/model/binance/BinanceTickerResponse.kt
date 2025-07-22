package com.vakifbank.bigotsv2.data.model.binance

data class BinanceTickerResponse(
    val symbol: String?,
    val bidPrice: String?,
    val askPrice: String?
)