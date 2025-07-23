package com.vakifbank.bigotsv2.di

import com.vakifbank.bigotsv2.data.repository.CryptoRepository
import com.vakifbank.bigotsv2.data.service.BinanceApiService
import com.vakifbank.bigotsv2.data.service.BtcTurkApiService
import com.vakifbank.bigotsv2.data.service.ParibuApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideCryptoRepository(
        paribuApi: ParibuApiService,
        binanceApi: BinanceApiService,
        btcturkApi: BtcTurkApiService
    ): CryptoRepository {
        return CryptoRepository.getInstance(paribuApi, binanceApi, btcturkApi)
    }
}