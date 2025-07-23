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
import androidx.core.app.NotificationCompat
import com.vakifbank.bigotsv2.CryptoArbitrageApplication
import com.vakifbank.bigotsv2.R
import com.vakifbank.bigotsv2.data.model.ArbitrageOpportunity
import com.vakifbank.bigotsv2.data.model.Exchange
import com.vakifbank.bigotsv2.data.repository.CryptoRepository
import com.vakifbank.bigotsv2.ui.activity.MainActivity
import com.vakifbank.bigotsv2.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ArbitrageService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val UPDATE_INTERVAL = 2000L

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
    private val repository = CryptoRepository.getInstance()
    private var updateJob: Job? = null
    private var notificationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundService()
        startDataCollection()
        startNotificationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
        notificationJob?.cancel()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CryptoArbitrageApplication.NOTIFICATION_CHANNEL_ID,
                CryptoArbitrageApplication.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Arbitraj fırsatları bildirimleri"
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val notification = createNotification("Arbitraj servisi başlatılıyor...")

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
                    delay(UPDATE_INTERVAL)
                } catch (e: Exception) {
                    delay(5000)
                }
            }
        }
    }

    private fun startNotificationUpdates() {
        notificationJob = serviceScope.launch {
            repository.arbitrageOpportunities.collect { opportunities ->
                updateNotification(opportunities)
            }
        }
    }

    private fun createNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CryptoArbitrageApplication.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Bigots - Arbitraj Takibi")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(opportunities: List<ArbitrageOpportunity>) {
        val content = if (opportunities.isEmpty()) {
            "Arbitraj fırsatı bekleniyor..."
        } else {
            val top3 = opportunities.take(3)
            buildString {
                top3.forEachIndexed { index, opportunity ->
                    val exchangeName = when (opportunity.exchange) {
                        Exchange.PARIBU -> Constants.ExchangeNames.PARIBU
                        Exchange.BTCTURK -> Constants.ExchangeNames.BTCTURK
                        else -> "Unknown"
                    }

                    val sign = if (opportunity.isPositive!!) "+" else ""
                    append("${index + 1}. ${opportunity.coin?.symbol} ")
                    append("($exchangeName): $sign%.2f%%".format(opportunity.difference))

                    if (index < top3.size - 1) append(" | ")
                }
            }
        }

        val notification = createNotification(content)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }
}