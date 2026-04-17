package com.kakao.taxi.liveupdate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.camera2.CameraManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class FlashlightForegroundService : Service() {
    private val prefs by lazy { ConverterPrefs(applicationContext) }
    private val controller by lazy { FlashlightController(applicationContext) }
    private val notificationBuilder by lazy {
        FlashlightNotificationBuilder(applicationContext)
    }
    private val notificationManager by lazy {
        NotificationManagerCompat.from(applicationContext)
    }

    private var capability = FlashlightCapability(available = false)
    private var isForegroundActive = false
    private var isTorchCallbackRegistered = false

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            val activeCameraId = capability.cameraId ?: return
            if (cameraId != activeCameraId) {
                return
            }
            Log.d(TAG, "onTorchModeChanged cameraId=$cameraId enabled=$enabled")
            if (!prefs.getSmartFlashlightEnabled()) {
                stopSelfSafely(clearPreference = false)
                return
            }
            if (!enabled) {
                stopSelfSafely(clearPreference = false)
                return
            }
            refreshNotification()
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            val activeCameraId = capability.cameraId ?: return
            if (cameraId != activeCameraId) {
                return
            }
            Log.d(TAG, "onTorchModeUnavailable cameraId=$cameraId")
            stopSelfSafely(clearPreference = false)
        }

        override fun onTorchStrengthLevelChanged(cameraId: String, newStrengthLevel: Int) {
            val activeCameraId = capability.cameraId ?: return
            if (cameraId != activeCameraId) {
                return
            }
            Log.d(TAG, "onTorchStrengthLevelChanged cameraId=$cameraId level=$newStrengthLevel")
            if (!prefs.getSmartFlashlightEnabled()) {
                stopSelfSafely(clearPreference = false)
                return
            }
            capability = controller.getCapability()
            if (capability.supportsFiveLevels) {
                prefs.setSmartFlashlightLevel(
                    controller.resolveLevelIndexForStrength(capability, newStrengthLevel)
                )
            }
            refreshNotification()
        }
    }

    override fun onCreate() {
        super.onCreate()
        capability = controller.getCapability()
        ensureChannel(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(
            TAG,
            "onStartCommand action=${intent?.action} " +
                "level=${intent?.getIntExtra(EXTRA_LEVEL_INDEX, -1)}"
        )
        when (intent?.action) {
            ACTION_SET_LEVEL -> {
                prefs.setSmartFlashlightLevel(
                    intent.getIntExtra(EXTRA_LEVEL_INDEX, prefs.getSmartFlashlightLevel())
                )
            }
            ACTION_DISABLE -> {
                runCatching {
                    controller.apply(
                        enabled = false,
                        requestedLevelIndex = prefs.getSmartFlashlightLevel()
                    )
                }.onFailure { error ->
                    Log.e(TAG, "Failed to disable flashlight from notification", error)
                }
                stopSelfSafely(clearPreference = false)
                return START_NOT_STICKY
            }
        }

        if (!prefs.getSmartFlashlightEnabled()) {
            stopSelfSafely(clearPreference = false)
            return START_NOT_STICKY
        }

        val desiredLevel = prefs.getSmartFlashlightLevel()
        capability = if (intent?.action == ACTION_SET_LEVEL) {
            runCatching {
                controller.apply(
                    enabled = true,
                    requestedLevelIndex = desiredLevel
                )
            }.onFailure { error ->
                Log.e(TAG, "Failed to adjust flashlight level", error)
                stopSelfSafely(clearPreference = false)
            }.getOrNull() ?: return START_NOT_STICKY
        } else {
            controller.getCapability()
        }

        Log.d(
            TAG,
            "Flashlight mirror sync available=${capability.available} " +
                "supportsFiveLevels=${capability.supportsFiveLevels} " +
                "level=$desiredLevel action=${intent?.action}"
        )
        if (!capability.available) {
            stopSelfSafely(clearPreference = false)
            return START_NOT_STICKY
        }

        ensureTorchCallbackRegistered()
        refreshNotification()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterTorchCallbackIfNeeded()
        hideNotification()
        super.onDestroy()
    }

    private fun refreshNotification() {
        val notification = runCatching {
            notificationBuilder.build(
                prefs = prefs,
                capability = capability
            )
        }.onFailure { error ->
            Log.e(TAG, "Failed to refresh flashlight notification", error)
            stopSelfSafely(clearPreference = false)
        }.getOrNull() ?: return

        runCatching {
            if (isForegroundActive) {
                notificationManager.notify(FlashlightNotificationBuilder.NOTIFICATION_ID, notification)
            } else {
                startForegroundCompat(notification)
                isForegroundActive = true
            }
        }.onFailure { error ->
            Log.e(TAG, "Failed to post flashlight notification", error)
            stopSelfSafely(clearPreference = false)
        }.onSuccess {
            setActiveNotificationVisible(true)
            LiveUpdateNotificationListenerService.requestFlashlightSourceDismissal()
        }
    }

    private fun hideNotification() {
        setActiveNotificationVisible(false)
        if (isForegroundActive) {
            runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
            isForegroundActive = false
        }
        notificationManager.cancel(FlashlightNotificationBuilder.NOTIFICATION_ID)
    }

    private fun stopSelfSafely(clearPreference: Boolean) {
        if (clearPreference) {
            prefs.setSmartFlashlightEnabled(false)
        }
        hideNotification()
        stopSelf()
    }

    private fun startForegroundCompat(notification: Notification) {
        startForeground(
            FlashlightNotificationBuilder.NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
    }

    private fun ensureTorchCallbackRegistered() {
        if (isTorchCallbackRegistered) {
            return
        }
        runCatching { controller.registerTorchCallback(torchCallback) }
            .onSuccess {
                isTorchCallbackRegistered = true
                Log.d(TAG, "Torch callback registered")
            }
            .onFailure { error ->
                Log.w(TAG, "Failed to register torch callback", error)
            }
    }

    private fun unregisterTorchCallbackIfNeeded() {
        if (!isTorchCallbackRegistered) {
            return
        }
        runCatching { controller.unregisterTorchCallback(torchCallback) }
            .onFailure { error ->
                Log.w(TAG, "Failed to unregister torch callback", error)
            }
        isTorchCallbackRegistered = false
    }

    companion object {
        const val ACTION_SYNC = "com.kakao.taxi.liveupdate.FLASHLIGHT_SYNC"
        const val ACTION_SET_LEVEL = "com.kakao.taxi.liveupdate.FLASHLIGHT_SET_LEVEL"
        const val ACTION_DISABLE = "com.kakao.taxi.liveupdate.FLASHLIGHT_DISABLE"
        const val EXTRA_LEVEL_INDEX = "level_index"

        private const val TAG = "FlashlightService"
        private const val CHANNEL_NAME_EN = "Flashlight"
        private const val CHANNEL_NAME_RU = "\u0424\u043e\u043d\u0430\u0440\u0438\u043a"
        private const val CHANNEL_DESCRIPTION_EN =
            "Shows a flashlight control notification and mirrors it into the Now Bar"
        private const val CHANNEL_DESCRIPTION_RU =
            "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0435\u0442 \u0443\u0432\u0435\u0434\u043e\u043c\u043b\u0435\u043d\u0438\u0435 \u0444\u043e\u043d\u0430\u0440\u0438\u043a\u0430 \u0438 \u0432\u044b\u0432\u043e\u0434\u0438\u0442 \u0435\u0433\u043e \u0432 Now Bar"

        @Volatile
        private var activeNotificationVisible = false

        fun hasActiveNotification(): Boolean = activeNotificationVisible

        fun sync(context: Context) {
            val prefs = ConverterPrefs(context)
            if (!prefs.getSmartFlashlightEnabled()) {
                stop(context)
                return
            }
            ContextCompat.startForegroundService(
                context,
                Intent(context, FlashlightForegroundService::class.java).apply {
                    action = ACTION_SYNC
                }
            )
        }

        fun stop(context: Context) {
            setActiveNotificationVisible(false)
            context.stopService(Intent(context, FlashlightForegroundService::class.java))
            NotificationManagerCompat.from(context)
                .cancel(FlashlightNotificationBuilder.NOTIFICATION_ID)
        }

        private fun setActiveNotificationVisible(value: Boolean) {
            activeNotificationVisible = value
        }

        private fun ensureChannel(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = manager.getNotificationChannel(FlashlightNotificationBuilder.CHANNEL_ID)
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
                    FlashlightNotificationBuilder.CHANNEL_ID,
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
