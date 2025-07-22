package com.vakifbank.bigotsv2.data.api

import com.vakifbank.bigotsv2.data.model.BtcTurkResponse
import retrofit2.Response
import retrofit2.http.GET

interface BtcTurkApiService {
    @GET("api/v2/ticker")
    suspend fun getTickers(): Response<BtcTurkResponse>
}