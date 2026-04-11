package com.kakao.taxi.liveupdate.networkspeed

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import com.kakao.taxi.liveupdate.ConverterPrefs

class NetworkSpeedForegroundService : Service() {
    private lateinit var prefs: ConverterPrefs
    private lateinit var notificationBuilder: NetworkSpeedNotificationBuilder
    private lateinit var notificationManager: NotificationManagerCompat
    private var workerThread: HandlerThread? = null
    private var workerHandler: Handler? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var monitoringStarted = false
    private var lastTotalRxBytes = 0L
    private var lastTotalTxBytes = 0L
    private var lastSampleAtMs = 0L
    private var latestSample = NetworkSpeedSample()

    private val monitorRunnable = object : Runnable {
        override fun run() {
            val handler = workerHandler ?: return
            if (!NetworkSpeedController.shouldRun(applicationContext, prefs)) {
                mainHandler.post { stopSelf() }
                return
            }

            val startedAtMs = SystemClock.elapsedRealtime()
            val trafficData = NetworkSpeedDataSource.getTrafficData()
            val nowMs = SystemClock.elapsedRealtime()

            if (lastSampleAtMs != 0L) {
                val deltaMs = nowMs - lastSampleAtMs
                val downloadDelta = trafficData.rxBytes - lastTotalRxBytes
                val uploadDelta = trafficData.txBytes - lastTotalTxBytes
                if (deltaMs > 0L) {
                    latestSample = NetworkSpeedSample(
                        downloadBytesPerSecond = ((downloadDelta * 1000L) / deltaMs)
                            .coerceAtLeast(0L),
                        uploadBytesPerSecond = ((uploadDelta * 1000L) / deltaMs)
                            .coerceAtLeast(0L)
                    )
                    publishCurrentNotification()
                }
            }

            lastTotalRxBytes = trafficData.rxBytes
            lastTotalTxBytes = trafficData.txBytes
            lastSampleAtMs = nowMs

            val elapsedMs = SystemClock.elapsedRealtime() - startedAtMs
            handler.postDelayed(this, (POLL_INTERVAL_MS - elapsedMs).coerceAtLeast(0L))
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = ConverterPrefs(applicationContext)
        notificationBuilder = NetworkSpeedNotificationBuilder(applicationContext)
        notificationManager = NotificationManagerCompat.from(applicationContext)
        notificationBuilder.ensureChannel()
        workerThread = HandlerThread("LiveBridgeNetworkSpeed").also { thread ->
            thread.start()
            workerHandler = Handler(thread.looper)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!NetworkSpeedController.shouldRun(applicationContext, prefs)) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundCompat(buildCurrentNotification())
        startMonitoringIfNeeded()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        workerHandler?.removeCallbacksAndMessages(null)
        workerThread?.quitSafely()
        workerHandler = null
        workerThread = null
        notificationManager.cancel(NOTIFICATION_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startMonitoringIfNeeded() {
        if (monitoringStarted) {
            return
        }
        monitoringStarted = true
        workerHandler?.post(monitorRunnable)
    }

    private fun publishCurrentNotification() {
        notificationManager.notify(
            NOTIFICATION_ID,
            buildCurrentNotification()
        )
    }

    private fun buildCurrentNotification(): Notification {
        return notificationBuilder.build(
            sample = latestSample,
            minPromotedBytesPerSecond = prefs.getNetworkSpeedMinThresholdBytesPerSecond()
        )
    }

    companion object {
        private const val POLL_INTERVAL_MS = 1500L
        const val NOTIFICATION_ID = 45100
    }
}
