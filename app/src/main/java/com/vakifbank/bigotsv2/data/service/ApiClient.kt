package com.vakifbank.bigotsv2.data.service

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val PARIBU_BASE_URL = "https://www.paribu.com/"
    private const val BINANCE_BASE_URL = "https://www.binance.com/"
    private const val BTCTURK_BASE_URL = "https://api.btcturk.com/"

    private fun createOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val paribuApi: ParibuApiService by lazy {
        createRetrofit(PARIBU_BASE_URL).create(ParibuApiService::class.java)
    }

    val binanceApi: BinanceApiService by lazy {
        createRetrofit(BINANCE_BASE_URL).create(BinanceApiService::class.java)
    }

    val btcturkApi: BtcTurkApiService by lazy {
        createRetrofit(BTCTURK_BASE_URL).create(BtcTurkApiService::class.java)
    }
}