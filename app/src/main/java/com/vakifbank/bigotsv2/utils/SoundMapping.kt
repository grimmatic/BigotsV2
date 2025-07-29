package com.vakifbank.bigotsv2.utils

import com.vakifbank.bigotsv2.R

object SoundMapping {
    private val soundMap = mapOf(
        "DOT" to R.raw.dot,
        "AVAX" to R.raw.avax,
        "TRX" to R.raw.tron,
        "EOS" to R.raw.eos,
        "BTTC" to R.raw.btt,
        "XRP" to R.raw.ripple,
        "XLM" to R.raw.sitellar,
        "ONT" to R.raw.ont,
        "ATOM" to R.raw.atom,
        "HOT" to R.raw.hot,
        "NEO" to R.raw.neo,
        "BAT" to R.raw.bat,
        "CHZ" to R.raw.chz,
        "UNI" to R.raw.uni,
        "BAL" to R.raw.bal,
        "AAVE" to R.raw.aave,
        "LINK" to R.raw.link,
        "MKR" to R.raw.mkr,
        "W" to R.raw.w,
        "RAY" to R.raw.ray,
        "LRC" to R.raw.lrc,
        "BAND" to R.raw.band,
        "ALGO" to R.raw.algo,
        "GRT" to R.raw.grt,
        "ENJ" to R.raw.enj,
        "THETA" to R.raw.theta,
        "MATIC" to R.raw.matic,
        "OXT" to R.raw.oxt,
        "CRV" to R.raw.crv,
        "OGN" to R.raw.ogn,
        "MANA" to R.raw.mana,
        "MIOTA" to R.raw.iota,
        "SOL" to R.raw.sol,
        "APE" to R.raw.ape,
        "VET" to R.raw.vet,
        "ANKR" to R.raw.ankr,
        "SHIB" to R.raw.shib,
        "LPT" to R.raw.lpt,
        "INJ" to R.raw.inj,
        "ICP" to R.raw.icp,
        "FTM" to R.raw.ftm,
        "AXS" to R.raw.axs,
        "ENS" to R.raw.ens,
        "SAND" to R.raw.sand,
        "AUDIO" to R.raw.audio,
        "BTC" to R.raw.btc,
        "ETH" to R.raw.eth
    )

    fun getSoundResource(symbol: String): Int {
        return soundMap[symbol.uppercase()] ?: R.raw.default_sound
    }
}