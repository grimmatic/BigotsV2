package com.vakifbank.bigotsv2.data.api

import com.vakifbank.bigotsv2.data.model.BinanceTickerResponse
import retrofit2.Response
import retrofit2.http.GET

interface BinanceApiService {
    @GET("api/v3/ticker/bookTicker")
    suspend fun getBookTickers(): Response<List<BinanceTickerResponse>>
}