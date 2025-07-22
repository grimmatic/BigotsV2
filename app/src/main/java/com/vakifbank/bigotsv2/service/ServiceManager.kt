package com.vakifbank.bigotsv2.service

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ServiceManager {
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning = _isServiceRunning.asStateFlow()

    fun startService(context: Context) {
        ArbitrageService.startService(context)
        _isServiceRunning.value = true
    }

    fun stopService(context: Context) {
        ArbitrageService.stopService(context)
        _isServiceRunning.value = false
    }
}