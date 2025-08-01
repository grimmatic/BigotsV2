package com.vakifbank.bigotsv2.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vakifbank.bigotsv2.CryptoArbitrageApplication
import com.vakifbank.bigotsv2.R
import com.vakifbank.bigotsv2.data.repository.CryptoRepository
import com.vakifbank.bigotsv2.domain.model.ArbitrageOpportunity
import com.vakifbank.bigotsv2.domain.model.Exchange
import com.vakifbank.bigotsv2.ui.activity.MainActivity
import com.vakifbank.bigotsv2.utils.Constants
import com.vakifbank.bigotsv2.utils.MediaPlayerManager
import com.vakifbank.bigotsv2.utils.SoundMapping
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ArbitrageService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val UPDATE_INTERVAL = 2000L
        private const val SOUND_PLAY_INTERVAL = 3000L

        fun startService(context: Context) {
            val intent = Intent(context, ArbitrageService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ArbitrageService::class.java)
            context.stopService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @Inject
    lateinit var repository: CryptoRepository
    private var updateJob: Job? = null
    private var soundPlayJob: Job? = null

    private lateinit var mediaPlayerManager: MediaPlayerManager
    private val currentlyPlayingOpportunities = mutableMapOf<String, ArbitrageOpportunity>()
    private val previousPlayingOpportunities = mutableMapOf<String, ArbitrageOpportunity>()

    private var serviceStartTime: Long = 0L

    override fun onCreate() {
        super.onCreate()
        serviceStartTime = System.currentTimeMillis()
        mediaPlayerManager = MediaPlayerManager.getInstance(this)
        startForegroundService()
        startDataCollection()
        startContinuousSoundPlayback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
        soundPlayJob?.cancel()
        serviceScope.cancel()
        mediaPlayerManager.releaseAll()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.cancel(NOTIFICATION_ID)
    }

    private fun startForegroundService() {
        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startDataCollection() {
        updateJob = serviceScope.launch {
            while (isActive) {
                try {
                    repository.fetchAllData()
                    val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
                    val refreshRate = prefs.getFloat("refresh_rate", 2.0f)
                    delay((refreshRate * 1000).toLong())
                } catch (e: Exception) {
                    delay(5000)
                }
            }
        }
    }

    private fun startContinuousSoundPlayback() {
        soundPlayJob = serviceScope.launch {
            while (isActive) {
                playActiveOpportunitySounds()
                delay(SOUND_PLAY_INTERVAL)
            }
        }
    }

    private fun updateCurrentOpportunities(opportunities: List<ArbitrageOpportunity>) {
        val prefs = getSharedPreferences("coin_settings", MODE_PRIVATE)
        previousPlayingOpportunities.clear()
        previousPlayingOpportunities.putAll(currentlyPlayingOpportunities)
        currentlyPlayingOpportunities.clear()

        opportunities.forEach { opportunity ->
            opportunity.coin?.let { coin ->
                val symbol = coin.symbol ?: return@let
                val exchange = opportunity.exchange?.name?.lowercase() ?: ""
                val opportunityId = "${symbol}_$exchange"

                val threshold = if (opportunity.exchange == Exchange.BTCTURK) {
                    prefs.getFloat(
                        "${symbol}_threshold_btc",
                        coin.alertThreshold?.toFloat()
                            ?: Constants.Numeric.DEFAULT_ALERT_THRESHOLD.toFloat()
                    ).toDouble()
                } else {
                    prefs.getFloat(
                        "${symbol}_threshold",
                        coin.alertThreshold?.toFloat()
                            ?: Constants.Numeric.DEFAULT_ALERT_THRESHOLD.toFloat()
                    ).toDouble()
                }

                val difference = kotlin.math.abs(opportunity.difference ?: 0.0)

                if (difference > threshold) {
                    currentlyPlayingOpportunities[opportunityId] = opportunity
                }
            }
        }

        stopInactiveSounds()
    }

    private fun stopInactiveSounds() {
        previousPlayingOpportunities.keys.forEach { opportunityId ->
            if (!currentlyPlayingOpportunities.containsKey(opportunityId)) {
                val opportunity = previousPlayingOpportunities[opportunityId]
                opportunity?.coin?.symbol?.let { symbol ->
                    val soundResource = SoundMapping.getSoundResource(symbol)
                    mediaPlayerManager.stopSound(soundResource)
                }
            }
        }
    }

    private fun playActiveOpportunitySounds() {
        if (currentlyPlayingOpportunities.isEmpty()) {
            mediaPlayerManager.stopAllSounds()
            return
        }

        val prefs = getSharedPreferences("coin_settings", MODE_PRIVATE)

        currentlyPlayingOpportunities.values.forEach { opportunity ->
            opportunity.coin?.let { coin ->
                val symbol = coin.symbol ?: return@let
                val exchange = opportunity.exchange

                val soundLevel = if (exchange == Exchange.BTCTURK) {
                    prefs.getInt("${symbol}_sound_level_btc", coin.soundLevel ?: 15)
                } else {
                    prefs.getInt("${symbol}_sound_level", coin.soundLevel ?: 15)
                }

                if (soundLevel > 0) {
                    val soundResource = SoundMapping.getSoundResource(symbol)
                    val volumeLevel = soundLevel / 15.0f
                    mediaPlayerManager.playSound(soundResource, volumeLevel)
                } else {
                    val soundResource = SoundMapping.getSoundResource(symbol)
                    mediaPlayerManager.stopSound(soundResource)
                }
            }
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CryptoArbitrageApplication.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Bigots - Arbitraj Takibi")
            .setContentText("Arbitraj servisi başlatıldı")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(false)
            .setWhen(serviceStartTime)
            .build()
    }
}