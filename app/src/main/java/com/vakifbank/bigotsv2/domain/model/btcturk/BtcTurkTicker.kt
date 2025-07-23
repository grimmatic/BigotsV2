package com.vakifbank.bigotsv2.domain.model.btcturk

data class BtcTurkTicker(
    val pair: String?,
    val bid: Double?,
    val ask: Double?,
    val last: Double?
)