package com.vakifbank.bigotsv2.data.service

import com.vakifbank.bigotsv2.domain.model.binance.BinanceTickerResponse
import com.vakifbank.bigotsv2.domain.model.btcturk.BtcTurkResponse
import com.vakifbank.bigotsv2.domain.model.paribu.ParibuTicker
import retrofit2.Response
import retrofit2.http.GET

interface CryptoApiService<T> {
    suspend fun getTickers(): Response<T>
}

interface BinanceApiService {
    @GET("api/v3/ticker/bookTicker")
    suspend fun getBookTickers(): Response<List<BinanceTickerResponse>>
}

interface BtcTurkApiService {
    @GET("api/v2/ticker")
    suspend fun getTickers(): Response<BtcTurkResponse>
}

interface ParibuApiService {
    @GET("ticker")
    suspend fun getTickers(): Response<Map<String, ParibuTicker>>
}