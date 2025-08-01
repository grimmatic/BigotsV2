package com.vakifbank.bigotsv2.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CoinData(
    val symbol: String?,
    val name: String?,
    val paribuPrice: Double? = 0.0,
    val btcturkPrice: Double? = 0.0,
    val binancePrice: Double? = 0.0,
    val binancePriceUsd: Double? = 0.0,
    val binancePriceBtcTurk: Double? = 0.0,
    val paribuDifference: Double? = 0.0,
    val btcturkDifference: Double? = 0.0,
    val alertThreshold: Double? = 2.5,
    val soundLevel: Int? = 15,
) : Parcelable