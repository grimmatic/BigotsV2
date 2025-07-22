package com.vakifbank.bigotsv2.data.model

enum class SupportedCoins(
    val symbol: String,
    val displayName: String,
    val paribuSymbol: String,
    val btcturkSymbol: String,
    val binanceSymbol: String
) {
    DOT("DOT", "Polkadot", "DOT_TL", "DOTTRY", "DOTUSDT"),
    AVAX("AVAX", "Avalanche", "AVAX_TL", "AVAXTRY", "AVAXUSDT"),
    TRX("TRX", "TRON", "TRX_TL", "TRXTRY", "TRXUSDT"),
    EOS("EOS", "EOS", "EOS_TL", "EOSTRY", "EOSUSDT"),
    BTTC("BTTC", "BitTorrent", "BTTC_TL", "BTTCTRY", "BTTCUSDT"),
    XRP("XRP", "Ripple", "XRP_TL", "XRPTRY", "XRPUSDT"),
    XLM("XLM", "Stellar", "XLM_TL", "XLMTRY", "XLMUSDT"),
    ONT("ONT", "Ontology", "ONT_TL", "ONTTRY", "ONTUSDT"),
    ATOM("ATOM", "Cosmos", "ATOM_TL", "ATOMTRY", "ATOMUSDT"),
    HOT("HOT", "Holo", "HOT_TL", "HOTTRY", "HOTUSDT"),
    NEO("NEO", "Neo", "NEO_TL", "NEOTRY", "NEOUSDT"),
    BAT("BAT", "Basic Attention Token", "BAT_TL", "BATTRY", "BATUSDT"),
    CHZ("CHZ", "Chiliz", "CHZ_TL", "CHZTRY", "CHZUSDT"),
    UNI("UNI", "Uniswap", "UNI_TL", "UNITRY", "UNIUSDT"),
    BAL("BAL", "Balancer", "BAL_TL", "BALTRY", "BALUSDT"),
    AAVE("AAVE", "Aave", "AAVE_TL", "AAVETRY", "AAVEUSDT"),
    LINK("LINK", "Chainlink", "LINK_TL", "LINKTRY", "LINKUSDT"),
    MKR("MKR", "Maker", "MKR_TL", "MKRTRY", "MKRUSDT"),
    W("W", "Wormhole", "W_TL", "WTRY", "WUSDT"),
    RAY("RAY", "Raydium", "RAY_TL", "RAYTRY", "RAYUSDT"),
    LRC("LRC", "Loopring", "LRC_TL", "LRCTRY", "LRCUSDT"),
    BAND("BAND", "Band Protocol", "BAND_TL", "BANDTRY", "BANDUSDT"),
    ALGO("ALGO", "Algorand", "ALGO_TL", "ALGOTRY", "ALGOUSDT"),
    GRT("GRT", "The Graph", "GRT_TL", "GRTTRY", "GRTUSDT"),
    ENJ("ENJ", "Enjin Coin", "ENJ_TL", "ENJTRY", "ENJUSDT"),
    THETA("THETA", "Theta", "THETA_TL", "THETATRY", "THETAUSDT"),
    MATIC("MATIC", "Polygon", "MATIC_TL", "MATICTRY", "MATICUSDT"),
    OXT("OXT", "Orchid", "OXT_TL", "OXTTRY", "OXTUSDT"),
    CRV("CRV", "Curve", "CRV_TL", "CRVTRY", "CRVUSDT"),
    OGN("OGN", "Origin Protocol", "OGN_TL", "OGNTRY", "OGNUSDT"),
    MANA("MANA", "Decentraland", "MANA_TL", "MANATRY", "MANAUSDT"),
    MIOTA("MIOTA", "IOTA", "MIOTA_TL", "MIOTATRY", "IOTAUSDT"),
    SOL("SOL", "Solana", "SOL_TL", "SOLTRY", "SOLUSDT"),
    APE("APE", "ApeCoin", "APE_TL", "APETRY", "APEUSDT"),
    VET("VET", "VeChain", "VET_TL", "VETTRY", "VETUSDT"),
    ANKR("ANKR", "Ankr", "ANKR_TL", "ANKRTRY", "ANKRUSDT"),
    SHIB("SHIB", "Shiba Inu", "SHIB_TL", "SHIBTRY", "SHIBUSDT"),
    LPT("LPT", "Livepeer", "LPT_TL", "LPTTRY", "LPTUSDT"),
    INJ("INJ", "Injective", "INJ_TL", "INJTRY", "INJUSDT"),
    ICP("ICP", "Internet Computer", "ICP_TL", "ICPTRY", "ICPUSDT"),
    FTM("FTM", "Fantom", "FTM_TL", "FTMTRY", "FTMUSDT"),
    AXS("AXS", "Axie Infinity", "AXS_TL", "AXSTRY", "AXSUSDT"),
    ENS("ENS", "Ethereum Name Service", "ENS_TL", "ENSTRY", "ENSUSDT"),
    SAND("SAND", "The Sandbox", "SAND_TL", "SANDTRY", "SANDUSDT"),
    AUDIO("AUDIO", "Audius", "AUDIO_TL", "AUDIOTRY", "AUDIOUSDT"),
    BTC("BTC", "Bitcoin", "BTC_TL", "BTCTRY", "BTCUSDT"),
    ETH("ETH", "Ethereum", "ETH_TL", "ETHTRY", "ETHUSDT");

    companion object {
        fun getAllParibuSymbols(): List<String> = values().map { it.paribuSymbol }

        fun getAllBtcturkSymbols(): List<String> = values().map { it.btcturkSymbol }

        fun getAllBinanceSymbols(): List<String> = values().map { it.binanceSymbol }

        fun getBySymbol(symbol: String): SupportedCoins? = values().find { it.symbol == symbol }

        fun getByParibuSymbol(paribuSymbol: String): SupportedCoins? =
            values().find { it.paribuSymbol == paribuSymbol }

        fun getByBtcturkSymbol(btcturkSymbol: String): SupportedCoins? =
            values().find { it.btcturkSymbol == btcturkSymbol }

        fun getByBinanceSymbol(binanceSymbol: String): SupportedCoins? =
            values().find { it.binanceSymbol == binanceSymbol }
    }
}