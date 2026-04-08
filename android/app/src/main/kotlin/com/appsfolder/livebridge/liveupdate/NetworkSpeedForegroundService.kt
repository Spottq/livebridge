package com.kakao.taxi.liveupdate

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class NetworkSpeedForegroundService : Service() {
    private val prefs by lazy { ConverterPrefs(applicationContext) }
    private val keyguardManager by lazy {
        getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    }
    private val notificationBuilder by lazy {
        NetworkSpeedNotificationBuilder(applicationContext)
    }
    private val notificationManager by lazy {
        NotificationManagerCompat.from(applicationContext)
    }
    private val speedMonitor by lazy { NetworkSpeedMonitor(applicationContext) }

    private var workerThread: HandlerThread? = null
    private var workerHandler: Handler? = null
    private var lastSnapshot: NetworkTrafficSnapshot? = null
    private var lastSampleAtElapsedMs: Long = 0L
    private var latestSample = NetworkSpeedSample.ZERO
    private var receiverRegistered = false

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshNotification()
        }
    }

    private val sampler = object : Runnable {
        override fun run() {
            val snapshot = speedMonitor.readSnapshot()
            val nowElapsedMs = SystemClock.elapsedRealtime()
            val previousSnapshot = lastSnapshot
            val previousElapsedMs = lastSampleAtElapsedMs

            latestSample =
                if (previousSnapshot == null || previousElapsedMs <= 0L) {
                    NetworkSpeedSample.ZERO
                } else {
                    val elapsedMs = (nowElapsedMs - previousElapsedMs).coerceAtLeast(1L)
                    val downloadSpeed =
                        ((snapshot.rxBytes - previousSnapshot.rxBytes) * 1000L / elapsedMs)
                            .coerceAtLeast(0L)
                    val uploadSpeed =
                        ((snapshot.txBytes - previousSnapshot.txBytes) * 1000L / elapsedMs)
                            .coerceAtLeast(0L)
                    NetworkSpeedSample(
                        downloadBytesPerSecond = downloadSpeed,
                        uploadBytesPerSecond = uploadSpeed
                    )
                }

            lastSnapshot = snapshot
            lastSampleAtElapsedMs = nowElapsedMs
            refreshNotification()

            workerHandler?.postDelayed(this, SAMPLE_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureChannel(applicationContext)
        speedMonitor.start()
        workerThread = HandlerThread("LiveBridgeNetworkSpeed").apply { start() }
        workerHandler = Handler(workerThread!!.looper)
        registerScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!prefs.getNetworkSpeedEnabled()) {
            stopSelfSafely()
            return START_NOT_STICKY
        }

        startForegroundCompat(buildNotification())
        startSamplingIfNeeded()

        if (intent?.action == ACTION_REFRESH) {
            refreshNotification()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterScreenReceiver()
        workerHandler?.removeCallbacksAndMessages(null)
        workerThread?.quitSafely()
        workerThread = null
        workerHandler = null
        speedMonitor.stop()
        notificationManager.cancel(NetworkSpeedNotificationBuilder.NOTIFICATION_ID)
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        return notificationBuilder.build(
            prefs = prefs,
            sample = latestSample,
            showLiveSurface = shouldShowLiveSurface()
        )
    }

    private fun refreshNotification() {
        if (!prefs.getNetworkSpeedEnabled()) {
            stopSelfSafely()
            return
        }
        notificationManager.notify(
            NetworkSpeedNotificationBuilder.NOTIFICATION_ID,
            buildNotification()
        )
    }

    private fun startSamplingIfNeeded() {
        val handler = workerHandler ?: return
        handler.removeCallbacks(sampler)
        handler.post(sampler)
    }

    private fun shouldShowLiveSurface(): Boolean {
        if (!prefs.getNetworkSpeedLockscreenOnly()) {
            return true
        }
        return keyguardManager.isDeviceLocked
    }

    private fun stopSelfSafely() {
        notificationManager.cancel(NetworkSpeedNotificationBuilder.NOTIFICATION_ID)
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    private fun startForegroundCompat(notification: Notification) {
        startForeground(
            NetworkSpeedNotificationBuilder.NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun registerScreenReceiver() {
        if (receiverRegistered) {
            return
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenStateReceiver, filter)
        }
        receiverRegistered = true
    }

    private fun unregisterScreenReceiver() {
        if (!receiverRegistered) {
            return
        }
        runCatching { unregisterReceiver(screenStateReceiver) }
        receiverRegistered = false
    }

    companion object {
        private const val ACTION_REFRESH = "com.kakao.taxi.liveupdate.NETWORK_SPEED_REFRESH"
        private const val SAMPLE_INTERVAL_MS = 1500L
        private const val CHANNEL_NAME_EN = "Network Speed"
        private const val CHANNEL_NAME_RU = "Скорость интернета"
        private const val CHANNEL_DESCRIPTION_EN =
            "Shows current network speed in the notification and Now Bar"
        private const val CHANNEL_DESCRIPTION_RU =
            "Показывает текущую скорость сети в уведомлении и Now Bar"

        fun sync(context: Context) {
            val prefs = ConverterPrefs(context)
            if (!prefs.getNetworkSpeedEnabled()) {
                stop(context)
                return
            }

            ContextCompat.startForegroundService(
                context,
                Intent(context, NetworkSpeedForegroundService::class.java).apply {
                    action = ACTION_REFRESH
                }
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, NetworkSpeedForegroundService::class.java))
            NotificationManagerCompat.from(context)
                .cancel(NetworkSpeedNotificationBuilder.NOTIFICATION_ID)
        }

        private fun ensureChannel(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = manager.getNotificationChannel(NetworkSpeedNotificationBuilder.CHANNEL_ID)
            val channelName = if (isRussianLocale(context)) CHANNEL_NAME_RU else CHANNEL_NAME_EN
            val channelDescription = if (isRussianLocale(context)) {
                CHANNEL_DESCRIPTION_RU
            } else {
                CHANNEL_DESCRIPTION_EN
            }

            if (existing != null) {
                existing.description = channelDescription
                existing.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                existing.setSound(null, null)
                existing.enableVibration(false)
                manager.createNotificationChannel(existing)
                return
            }

            manager.createNotificationChannel(
                NotificationChannel(
                    NetworkSpeedNotificationBuilder.CHANNEL_ID,
                    channelName,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = channelDescription
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                }
            )
        }

        private fun isRussianLocale(context: Context): Boolean {
            val locale = context.resources.configuration.locales.get(0)
            return locale?.language?.startsWith("ru", ignoreCase = true) == true
        }
    }
}
