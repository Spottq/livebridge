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
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class NetworkSpeedForegroundService : Service() {
    private val prefs by lazy { ConverterPrefs(applicationContext) }
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
    private var latestDailyUsage = NetworkDailyUsage.ZERO
    private var dailyUsageTracker: NetworkDailyUsageTracker? = null
    private var wasDailyUsageEnabled = false
    private var initialized = false
    private var isForegroundActive = false
    @Volatile private var screenOff = false
    private var screenReceiverRegistered = false
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            screenOff = intent?.action == Intent.ACTION_SCREEN_OFF || !isInteractive()
            workerHandler?.post { refreshServiceState() }
        }
    }

    private val sampler = object : Runnable {
        override fun run() {
            val snapshot = speedMonitor.readSnapshot()
            val nowElapsedMs = SystemClock.elapsedRealtime()
            val previousSnapshot = lastSnapshot
            val previousElapsedMs = lastSampleAtElapsedMs
            refreshDailyUsage(snapshot)

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
            refreshServiceState()

            workerHandler?.postDelayed(this, SAMPLE_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val initResult = runCatching {
            ensureChannel(applicationContext)
            speedMonitor.start()
            workerThread = HandlerThread("LiveBridgeNetworkSpeed").apply { start() }
            workerHandler = Handler(workerThread!!.looper)
        }

        initResult.onSuccess {
            initialized = true
        }.onFailure { error ->
            Log.e(TAG, "Failed to initialize network speed service", error)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!initialized) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (DeviceBlocker.isBlockedDevice()) {
            stopSelfSafely()
            return START_NOT_STICKY
        }
        if (!prefs.getNetworkSpeedEnabled()) {
            stopSelfSafely()
            return START_NOT_STICKY
        }
        syncScreenReceiver()

        val startResult = runCatching {
            startSamplingIfNeeded()
            refreshServiceState()
        }

        if (startResult.isFailure) {
            Log.e(
                TAG,
                "Failed to start network speed foreground service",
                startResult.exceptionOrNull()
            )
            stopSelfSafely()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (screenReceiverRegistered) {
            runCatching { unregisterReceiver(screenReceiver) }
            screenReceiverRegistered = false
        }
        workerHandler?.removeCallbacksAndMessages(null)
        workerThread?.quitSafely()
        workerThread = null
        workerHandler = null
        speedMonitor.stop()
        dailyUsageTracker?.flush()
        dailyUsageTracker = null
        hideNotification()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        return notificationBuilder.build(
            prefs = prefs,
            sample = latestSample,
            dailyUsage = latestDailyUsage,
            allowPromotion = NetworkSpeedVisibilityPolicy.allowPromotion(
                hideWhenLocked = prefs.getNetworkSpeedHideWhenLocked(),
                screenOff = screenOff,
                keyguardLocked = isKeyguardLocked()
            )
        )
    }

    private fun syncScreenReceiver() {
        val needed = prefs.getNetworkSpeedHideWhenLocked()
        if (needed != screenReceiverRegistered) {
            if (needed) {
                ContextCompat.registerReceiver(
                    this,
                    screenReceiver,
                    IntentFilter().apply {
                        addAction(Intent.ACTION_SCREEN_OFF)
                        addAction(Intent.ACTION_SCREEN_ON)
                        addAction(Intent.ACTION_USER_PRESENT)
                    },
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            } else {
                runCatching { unregisterReceiver(screenReceiver) }
            }
            screenReceiverRegistered = needed
        }
        screenOff = !isInteractive()
    }

    private fun isInteractive(): Boolean {
        return (getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive
    }

    private fun isKeyguardLocked(): Boolean {
        return (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked
    }

    private fun refreshDailyUsage(snapshot: NetworkTrafficSnapshot) {
        val enabled = prefs.getNetworkSpeedDailyUsageEnabled()
        if (!enabled) {
            if (wasDailyUsageEnabled) {
                dailyUsageTracker?.flush()
                dailyUsageTracker = null
            }
            latestDailyUsage = NetworkDailyUsage.ZERO
            wasDailyUsageEnabled = false
            return
        }

        val tracker = dailyUsageTracker ?: NetworkDailyUsageTracker(applicationContext).also {
            dailyUsageTracker = it
        }
        latestDailyUsage =
            if (wasDailyUsageEnabled) {
                tracker.record(snapshot)
            } else {
                tracker.start(snapshot)
            }
        wasDailyUsageEnabled = true
    }

    private fun refreshServiceState() {
        if (DeviceBlocker.isBlockedDevice()) {
            stopSelfSafely()
            return
        }
        if (!prefs.getNetworkSpeedEnabled()) {
            stopSelfSafely()
            return
        }

        val notification = runCatching { buildNotification() }
            .onFailure { error ->
                Log.e(TAG, "Failed to refresh network speed notification", error)
                stopSelfSafely()
            }
            .getOrNull() ?: return

        runCatching {
            if (isForegroundActive) {
                notificationManager.notify(
                    NetworkSpeedNotificationBuilder.NOTIFICATION_ID,
                    notification
                )
            } else {
                startForegroundCompat(notification)
                isForegroundActive = true
            }
        }.onFailure { error ->
            Log.e(TAG, "Failed to post network speed notification", error)
            stopSelfSafely()
        }
    }

    private fun startSamplingIfNeeded() {
        val handler = workerHandler ?: return
        handler.removeCallbacks(sampler)
        handler.post(sampler)
    }

    private fun hideNotification() {
        if (isForegroundActive) {
            runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
            isForegroundActive = false
        }
        notificationManager.cancel(NetworkSpeedNotificationBuilder.NOTIFICATION_ID)
    }

    private fun stopSelfSafely() {
        hideNotification()
        stopSelf()
    }

    private fun startForegroundCompat(notification: Notification) {
        startForeground(
            NetworkSpeedNotificationBuilder.NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
    }

    companion object {
        private const val TAG = "NetworkSpeedService"
        private const val ACTION_REFRESH = "com.kakao.taxi.liveupdate.NETWORK_SPEED_REFRESH"
        private const val SAMPLE_INTERVAL_MS = 1000L
        private val LEGACY_CHANNEL_IDS = listOf(
            "livebridge_network_speed",
            "livebridge_network_speed_priority",
            "livebridge_network_speed_meter"
        )

        fun sync(context: Context) {
            val prefs = ConverterPrefs(context)
            if (!prefs.getNetworkSpeedEnabled() || DeviceBlocker.isBlockedDevice()) {
                stop(context)
                return
            }

            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, NetworkSpeedForegroundService::class.java).apply {
                        action = ACTION_REFRESH
                    }
                )
            } catch (error: IllegalStateException) {
                Log.w(TAG, "System deferred network monitor start", error)
            } catch (error: SecurityException) {
                Log.w(TAG, "System denied network monitor start", error)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, NetworkSpeedForegroundService::class.java))
            NotificationManagerCompat.from(context)
                .cancel(NetworkSpeedNotificationBuilder.NOTIFICATION_ID)
        }

        private fun ensureChannel(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = manager.getNotificationChannel(NetworkSpeedNotificationBuilder.CHANNEL_ID)
            val notificationText = NetworkSpeedNotificationLocalizer.resolve(
                context,
                ConverterPrefs(context)
            )
            val channelName = notificationText.title
            val channelDescription = notificationText.channelDescription

            deleteLegacyChannels(manager)

            if (existing != null) {
                existing.name = channelName
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
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = channelDescription
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                }
            )
        }

        private fun deleteLegacyChannels(manager: NotificationManager) {
            LEGACY_CHANNEL_IDS.forEach { channelId ->
                if (channelId != NetworkSpeedNotificationBuilder.CHANNEL_ID) {
                    manager.deleteNotificationChannel(channelId)
                }
            }
        }
    }
}
