package com.vakifbank.bigotsv2.utils

object Constants {

    object Numeric {
        const val DEFAULT_PRICE = 0.0
        const val DEFAULT_DIFFERENCE = 0.0
        const val DEFAULT_ALERT_THRESHOLD = 2.5
        const val DEFAULT_SOUND_LEVEL = 15
        const val PERCENTAGE_MULTIPLIER = 100.0
        const val EMPTY = ""
    }

    object ExchangeNames {
        const val PARIBU = "Paribu"
        const val BTCTURK = "BTCTurk"
        const val BINANCE = "Binance"
    }

    object ApiSymbols {
        const val USDT_TL = "USDT_TL"
        const val USDT_TRY = "USDTTRY"
    }

    object Defaults {
        const val IS_POSITIVE = false
    }

    object SharedPreferences {
        const val COIN_SETTINGS = "coin_settings"
        const val APP_SETTINGS = "app_settings"
        const val GLOBAL_THRESHOLD = "global_threshold"
        const val REFRESH_RATE = "refresh_rate"
    }

    object SharedPreferencesSuffixes {
        const val THRESHOLD = "_threshold"
        const val THRESHOLD_BTC = "_threshold_btc"
        const val SOUND_LEVEL = "_sound_level"
        const val SOUND_LEVEL_BTC = "_sound_level_btc"

    }

    object Strings {
        const val UNDERSCORE = "_"
    }
}
