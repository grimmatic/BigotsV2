package com.vakifbank.bigotsv2.data.api

import com.vakifbank.bigotsv2.data.model.ParibuTicker
import retrofit2.Response
import retrofit2.http.GET

interface ParibuApiService {
    @GET("ticker")
    suspend fun getTickers(): Response<Map<String, ParibuTicker>>
}
