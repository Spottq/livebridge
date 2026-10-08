package com.kakao.taxi.liveupdate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.kakao.taxi.MainActivity
import com.kakao.taxi.R

class KeepAliveForegroundService : Service() {
    private val prefs by lazy { ConverterPrefs(applicationContext) }
    private val handler = Handler(Looper.getMainLooper())
    private val listenerRecovery = object : Runnable {
        override fun run() {
            if (!shouldRun(applicationContext, prefs)) {
                stopSelf()
                return
            }
            if (!LiveUpdateNotificationListenerService.isConnected()) {
                LiveUpdateNotificationListenerService.requestRebindIfEnabled(
                    applicationContext,
                    "keep_alive_recovery"
                )
            }
            handler.postDelayed(this, RECOVERY_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DeviceBlocker.isBlockedDevice()) {
            stopSelf()
            return START_NOT_STICKY
        }
        // START_STICKY can restart this service after the user disabled it.
        if (!shouldRun(applicationContext, prefs)) {
            stopSelf()
            return START_NOT_STICKY
        }
        ensureChannel(this)
        val notification = buildNotification()
        try {
            startForegroundCompat(notification)
        } catch (error: IllegalStateException) {
            Log.w(TAG, "System refused foreground promotion", error)
            stopSelf()
            return START_NOT_STICKY
        } catch (error: SecurityException) {
            Log.w(TAG, "System denied foreground promotion", error)
            stopSelf()
            return START_NOT_STICKY
        }
        handler.removeCallbacks(listenerRecovery)
        handler.post(listenerRecovery)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_liveupdate)
            .setContentTitle(NativeAppStrings.text(this, "background mode", "Фоновый режим", "Modo en segundo plano", "Hintergrundmodus"))
            .setContentText(NativeAppStrings.text(this, "Keep this notification here for LiveBridge stability", "Сохраните это уведомление для стабильной работы LiveBridge", "Mantén esta notificación para que LiveBridge funcione de forma estable", "Behalte diese Benachrichtigung für einen stabilen LiveBridge-Betrieb"))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "LiveBridgeKeepAlive"
        private const val RECOVERY_INTERVAL_MS = 30_000L
        private const val CHANNEL_ID = "livebridge_keep_alive"
        private const val NOTIFICATION_ID = 41130

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, KeepAliveForegroundService::class.java)
                )
            } catch (error: IllegalStateException) {
                Log.w(TAG, "System deferred background-mode start", error)
            } catch (error: SecurityException) {
                Log.w(TAG, "System denied background-mode start", error)
            }
        }

        fun sync(context: Context, prefs: ConverterPrefs = ConverterPrefs(context)) {
            if (shouldRun(context, prefs)) {
                start(context)
            } else {
                stop(context)
            }
        }

        private fun shouldRun(context: Context, prefs: ConverterPrefs): Boolean {
            return prefs.getConverterEnabled() &&
                prefs.getKeepAliveForegroundEnabled() &&
                LiveUpdateNotificationListenerService.isListenerEnabled(context)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KeepAliveForegroundService::class.java))
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                return
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val name = NativeAppStrings.text(context, "Background Mode", "Фоновый режим", "Modo en segundo plano", "Hintergrundmodus")
            val description = NativeAppStrings.text(context, "Keep this notification here to use LiveBridge", "Сохраните это уведомление для работы LiveBridge", "Mantén esta notificación para usar LiveBridge", "Behalte diese Benachrichtigung, um LiveBridge zu nutzen")
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing != null && existing.name == name && existing.description == description) return
            val channel = existing ?: NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW).apply {
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            channel.name = name
            channel.description = description
            manager.createNotificationChannel(channel)
        }
    }
}
