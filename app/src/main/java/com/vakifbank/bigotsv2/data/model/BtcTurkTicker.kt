package com.vakifbank.bigotsv2.data.model

data class BtcTurkTicker(
    val pair: String,
    val bid: Double,
    val ask: Double,
    val last: Double
)