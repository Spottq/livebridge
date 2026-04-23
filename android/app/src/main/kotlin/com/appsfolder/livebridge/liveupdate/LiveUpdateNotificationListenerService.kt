package com.kakao.taxi.liveupdate

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlin.math.min

class LiveUpdateNotificationListenerService : NotificationListenerService() {
    private val prefs by lazy { ConverterPrefs(applicationContext) }
    private val flashlightController by lazy { FlashlightController(applicationContext) }
    private val mainHandler = Handler(Looper.getMainLooper())
    private val selfDismissLock = Any()
    private val selfDismissedSourceKeys = mutableSetOf<String>()
    private val selfDismissedFlashlightSourceKeys = mutableSetOf<String>()
    private var trackedFlashlightSourceKey: String? = null
    private var isTorchCallbackRegistered = false
    private var rebindAttempts = 0
    private var rebindScheduled = false
    private var snapshotSyncScheduled = false

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            val flashlightCameraId = flashlightController.getCapability().cameraId ?: return
            if (cameraId != flashlightCameraId) {
                return
            }
            Log.d(TAG, "Listener torch mode changed: cameraId=$cameraId enabled=$enabled")
            handleObservedTorchState(enabled)
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            val flashlightCameraId = flashlightController.getCapability().cameraId ?: return
            if (cameraId != flashlightCameraId) {
                return
            }
            Log.d(TAG, "Listener torch unavailable: cameraId=$cameraId")
            handleObservedTorchUnavailable()
        }
    }

    private val snapshotSyncRunnable = object : Runnable {
        override fun run() {
            snapshotSyncScheduled = false
            if (isUnsupportedDevice()) {
                FlashlightSourceState.clear()
                FlashlightForegroundService.stop(applicationContext)
                LiveUpdateNotifier.cancelAllMirrored(applicationContext)
                return
            }

            val snapshots = try {
                activeNotifications?.toList().orEmpty()
            } catch (error: Throwable) {
                Log.w(TAG, "Snapshot sync failed while reading active notifications", error)
                scheduleRebind("snapshot_sync_failed")
                scheduleSnapshotSync()
                return
            }

            syncFlashlightMirror(snapshots)

            if (!prefs.getConverterEnabled()) {
                LiveUpdateNotifier.cancelAllMirrored(applicationContext)
                scheduleSnapshotSync()
                return
            }

            for (sbn in snapshots) {
                if (sbn.packageName == packageName || isFlashlightSourceNotification(sbn)) {
                    continue
                }
                try {
                    processIncomingNotification(sbn)
                } catch (error: Throwable) {
                    Log.e(TAG, "Snapshot sync processing failed: ${sbn.key}", error)
                }
            }

            scheduleSnapshotSync()
        }
    }

    override fun onCreate() {
        super.onCreate()
        activeInstance = this

        if (isUnsupportedDevice()) {
            FlashlightSourceState.clear()
            FlashlightForegroundService.stop(applicationContext)
            LiveUpdateNotifier.cancelAllMirrored(applicationContext)
            return
        }
        if (!prefs.getConverterEnabled()) {
            LiveUpdateNotifier.cancelAllMirrored(applicationContext)
        }

        syncTorchMonitoring()
        LiveUpdateNotifier.ensureChannel(applicationContext)
        scheduleSnapshotSync()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        rebindAttempts = 0
        rebindScheduled = false

        if (isUnsupportedDevice()) {
            FlashlightSourceState.clear()
            FlashlightForegroundService.stop(applicationContext)
            LiveUpdateNotifier.cancelAllMirrored(applicationContext)
            return
        }

        syncTorchMonitoring()
        val snapshots = try {
            activeNotifications?.toList().orEmpty()
        } catch (error: Throwable) {
            Log.w(TAG, "Unable to read active notifications on connect", error)
            emptyList()
        }

        syncFlashlightMirror(snapshots)

        if (!prefs.getConverterEnabled()) {
            LiveUpdateNotifier.cancelAllMirrored(applicationContext)
            scheduleSnapshotSync()
            return
        }

        if (snapshots.isEmpty()) {
            scheduleSnapshotSync()
            return
        }

        for (sbn in snapshots) {
            if (sbn.packageName == packageName || isFlashlightSourceNotification(sbn)) {
                continue
            }
            try {
                processIncomingNotification(sbn)
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to restore active notification: ${sbn.key}", error)
            }
        }
        scheduleSnapshotSync()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (isUnsupportedDevice()) {
            return
        }
        scheduleRebind("listener_disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (isUnsupportedDevice()) {
            return
        }
        if (sbn.packageName == packageName) {
            return
        }
        if (isFlashlightSourceNotification(sbn)) {
            syncFlashlightMirror(listOf(sbn))
            return
        }
        if (!prefs.getConverterEnabled()) {
            LiveUpdateNotifier.cancelMirrored(applicationContext, sbn)
            return
        }

        try {
            processIncomingNotification(sbn)
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to process posted notification: ${sbn.key}", error)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        if (isUnsupportedDevice()) {
            return
        }
        if (sbn.packageName == packageName) {
            LiveUpdateNotifier.handleMirroredRemoved(applicationContext, sbn)
            return
        }
        if (consumeSelfDismissedSourceKey(sbn.key)) {
            return
        }
        if (isFlashlightSourceNotification(sbn)) {
            forgetTrackedFlashlightSourceKey(sbn.key)
            if (consumeSelfDismissedFlashlightSourceKey(sbn.key)) {
                return
            }
            FlashlightSourceState.clear()
            FlashlightForegroundService.stop(applicationContext)
            return
        }
        try {
            LiveUpdateNotifier.cancelMirrored(applicationContext, sbn)
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to process removed notification: ${sbn.key}", error)
        }
    }

    private fun isUnsupportedDevice(): Boolean {
        return DeviceBlocker.isBlockedDevice()
    }

    override fun onDestroy() {
        unregisterTorchCallbackIfNeeded()
        mainHandler.removeCallbacksAndMessages(null)
        rebindScheduled = false
        snapshotSyncScheduled = false
        if (activeInstance === this) {
            activeInstance = null
        }
        super.onDestroy()
    }

    private fun processIncomingNotification(sbn: StatusBarNotification) {
        val result = LiveUpdateNotifier.maybeMirror(applicationContext, prefs, sbn)
        if (result.mirrored) {
            ConversionLogStore.upsertMirroredNotification(
                context = applicationContext,
                prefs = prefs,
                sbn = sbn,
                title = extractLogTitle(sbn),
                text = extractLogText(sbn.notification)
            )
        }
        maybeDismissOriginalSource(sbn, result)
    }

    private fun extractLogTitle(sbn: StatusBarNotification): String {
        val extras = sbn.notification.extras
        return extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()?.trim()
                ?.takeIf { it.isNotEmpty() }
            ?: runCatching {
                val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
                packageManager.getApplicationLabel(appInfo)?.toString()?.trim()
            }.getOrNull().takeUnless { it.isNullOrBlank() }
            ?: sbn.packageName
    }

    private fun extractLogText(notification: Notification): String {
        val extras = notification.extras
        return extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
                ?.takeIf { it.isNotEmpty() }
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()
                ?.takeIf { it.isNotEmpty() }
            ?: extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()?.trim()
                ?.takeIf { it.isNotEmpty() }
            ?: extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
                ?.joinToString("\n")
                .orEmpty()
    }

    private fun syncTorchMonitoring() {
        if (prefs.getSmartFlashlightEnabled()) {
            ensureTorchCallbackRegistered()
        } else {
            unregisterTorchCallbackIfNeeded()
        }
    }

    private fun ensureTorchCallbackRegistered() {
        if (isTorchCallbackRegistered) {
            return
        }
        runCatching { flashlightController.registerTorchCallback(torchCallback) }
            .onSuccess {
                isTorchCallbackRegistered = true
                Log.d(TAG, "Listener torch callback registered")
            }
            .onFailure { error ->
                Log.w(TAG, "Failed to register listener torch callback", error)
            }
    }

    private fun refreshTorchCallbackRegistration() {
        unregisterTorchCallbackIfNeeded()
        ensureTorchCallbackRegistered()
    }

    private fun unregisterTorchCallbackIfNeeded() {
        if (!isTorchCallbackRegistered) {
            return
        }
        runCatching { flashlightController.unregisterTorchCallback(torchCallback) }
            .onFailure { error ->
                Log.w(TAG, "Failed to unregister listener torch callback", error)
            }
        isTorchCallbackRegistered = false
    }

    private fun handleObservedTorchState(enabled: Boolean) {
        if (!prefs.getSmartFlashlightEnabled()) {
            if (!enabled) {
                resetFlashlightSourceDismissState()
                FlashlightSourceState.clear()
                FlashlightForegroundService.stop(applicationContext)
            }
            return
        }

        if (enabled) {
            FlashlightForegroundService.sync(applicationContext)
            return
        }

        clearTrackedFlashlightSourceKey()
        FlashlightSourceState.clear()
        FlashlightForegroundService.stop(applicationContext)
    }

    private fun handleObservedTorchUnavailable() {
        resetFlashlightSourceDismissState()
        FlashlightSourceState.clear()
        FlashlightForegroundService.stop(applicationContext)
    }

    private fun syncFlashlightMirror(snapshots: Collection<StatusBarNotification>) {
        syncTorchMonitoring()
        if (!prefs.getSmartFlashlightEnabled()) {
            resetFlashlightSourceDismissState()
            FlashlightSourceState.clear()
            FlashlightForegroundService.stop(applicationContext)
            return
        }

        val sourceNotification = snapshots.firstOrNull(::isFlashlightSourceNotification)
        if (sourceNotification != null) {
            rememberTrackedFlashlightSourceKey(sourceNotification.key)
            FlashlightSourceState.updateFrom(
                context = applicationContext,
                packageName = sourceNotification.packageName,
                notification = sourceNotification.notification
            )
            FlashlightForegroundService.sync(applicationContext)
        } else {
            clearTrackedFlashlightSourceKey()
            if (FlashlightForegroundService.hasActiveNotification()) {
                return
            }
            FlashlightSourceState.clear()
            FlashlightForegroundService.stop(applicationContext)
        }
    }

    private fun isFlashlightSourceNotification(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName != FLASHLIGHT_SOURCE_PACKAGE) {
            return false
        }
        return sbn.notification.channelId == FLASHLIGHT_SOURCE_CHANNEL_ID || sbn.tag == FLASHLIGHT_SOURCE_TAG
    }

    fun requestImmediateFlashlightSnapshotSync() {
        mainHandler.removeCallbacks(snapshotSyncRunnable)
        if (prefs.getSmartFlashlightEnabled()) {
            refreshTorchCallbackRegistration()
        } else {
            resetFlashlightSourceDismissState()
            unregisterTorchCallbackIfNeeded()
        }
        snapshotSyncScheduled = true
        mainHandler.post(snapshotSyncRunnable)
    }

    private fun requestTrackedFlashlightSourceDismissal() {
        if (!prefs.getSmartFlashlightEnabled()) {
            resetFlashlightSourceDismissState()
            Log.v(TAG, "Skip flashlight source dismiss request: smart flashlight disabled")
            return
        }
        mainHandler.post {
            dismissTrackedFlashlightSourceNotification()
        }
    }

    private fun dismissTrackedFlashlightSourceNotification() {
        if (!prefs.getSmartFlashlightEnabled() || !FlashlightForegroundService.hasActiveNotification()) {
            resetFlashlightSourceDismissState()
            Log.v(TAG, "Skip flashlight source dismiss: feature disabled or LiveBridge notification hidden")
            return
        }
        val sourceKey = synchronized(selfDismissLock) {
            trackedFlashlightSourceKey?.also(selfDismissedFlashlightSourceKeys::add)
        }
        if (sourceKey == null) {
            Log.v(TAG, "Skip flashlight source dismiss: no tracked key")
            return
        }

        val cancelDirectRequested = runCatching {
            cancelNotification(sourceKey)
        }.onSuccess {
            Log.i(TAG, "Requested flashlight source cancel via cancelNotification: $sourceKey")
        }.onFailure { error ->
            Log.w(TAG, "cancelNotification failed for flashlight source: $sourceKey", error)
        }.isSuccess

        val cancelBatchRequested = runCatching {
            cancelNotifications(arrayOf(sourceKey))
        }.onSuccess {
            Log.i(TAG, "Requested flashlight source cancel via cancelNotifications: $sourceKey")
        }.onFailure { error ->
            Log.w(TAG, "cancelNotifications failed for flashlight source: $sourceKey", error)
        }.isSuccess

        val snoozeRequested = runCatching {
            snoozeNotification(sourceKey, FLASHLIGHT_SOURCE_SNOOZE_MS)
        }.onSuccess {
            Log.i(TAG, "Requested flashlight source snooze fallback: $sourceKey")
        }.onFailure { error ->
            Log.w(TAG, "snoozeNotification failed for flashlight source: $sourceKey", error)
        }.isSuccess

        if (!cancelDirectRequested && !cancelBatchRequested && !snoozeRequested) {
            forgetSelfDismissedFlashlightSourceKey(sourceKey)
            Log.w(TAG, "Unable to dismiss or snooze SystemUI flashlight notification: $sourceKey")
            return
        }

        mainHandler.postDelayed(
            { verifyFlashlightSourceDismissal(sourceKey) },
            FLASHLIGHT_SOURCE_VERIFY_DELAY_MS
        )
    }

    private fun verifyFlashlightSourceDismissal(sourceKey: String) {
        if (!prefs.getSmartFlashlightEnabled() || !FlashlightForegroundService.hasActiveNotification()) {
            Log.v(TAG, "Skip flashlight source dismissal verify: feature disabled or LiveBridge notification hidden")
            return
        }
        val stillPresent = runCatching {
            activeNotifications?.any { it.key == sourceKey } == true
        }.getOrElse { error ->
            Log.w(TAG, "Failed to verify flashlight source dismissal: $sourceKey", error)
            false
        }
        if (!stillPresent) {
            Log.i(TAG, "Flashlight source no longer active after dismissal: $sourceKey")
            return
        }

        val snoozeRequested = runCatching {
            snoozeNotification(sourceKey, FLASHLIGHT_SOURCE_SNOOZE_MS)
        }.onSuccess {
            Log.i(TAG, "Retried flashlight source snooze after failed dismissal: $sourceKey")
        }.onFailure { error ->
            Log.w(TAG, "Retry snooze failed for flashlight source: $sourceKey", error)
        }.isSuccess

        if (!snoozeRequested) {
            Log.w(TAG, "Flashlight source is still active after all dismissal attempts: $sourceKey")
        }
    }

    private fun maybeDismissOriginalSource(
        sbn: StatusBarNotification,
        result: LiveUpdateNotifier.MirrorResult
    ) {
        if (!result.mirrored) {
            return
        }
        if (!sbn.isClearable) {
            return
        }
        val appPresentationRemoveOriginal = AppPresentationOverridesLoader
            .get(prefs)
            .resolve(sbn.packageName.lowercase())
            .removeOriginalMessage
        val legacyDedup =
            prefs.getNotificationDedupEnabled() &&
                prefs.isNotificationDedupPackageAllowed(sbn.packageName) &&
                when (prefs.getNotificationDedupMode()) {
                    "otp_only" -> result.dedupKind == LiveUpdateNotifier.MirrorDedupKind.OTP
                    else -> {
                        result.dedupKind == LiveUpdateNotifier.MirrorDedupKind.OTP ||
                            result.dedupKind == LiveUpdateNotifier.MirrorDedupKind.STATUS
                    }
                }
        val upstreamDismiss = when (result.dedupKind) {
            LiveUpdateNotifier.MirrorDedupKind.OTP -> {
                prefs.getOtpRemoveOriginalMessageEnabled() &&
                    prefs.isOtpPackageAllowed(sbn.packageName)
            }
            LiveUpdateNotifier.MirrorDedupKind.STATUS -> {
                prefs.getSmartRemoveOriginalMessageEnabled() &&
                    prefs.isSmartPackageAllowed(sbn.packageName)
            }
            else -> false
        }
        val shouldDismiss =
            appPresentationRemoveOriginal ||
                legacyDedup ||
                upstreamDismiss
        if (!shouldDismiss) {
            return
        }

        rememberSelfDismissedSourceKey(sbn.key)
        try {
            cancelNotification(sbn.key)
        } catch (error: Throwable) {
            forgetSelfDismissedSourceKey(sbn.key)
            Log.e(TAG, "Failed to auto-dismiss original notification: ${sbn.key}", error)
        }
    }

    private fun rememberSelfDismissedSourceKey(sbnKey: String) {
        synchronized(selfDismissLock) {
            selfDismissedSourceKeys.add(sbnKey)
        }
    }

    private fun forgetSelfDismissedSourceKey(sbnKey: String) {
        synchronized(selfDismissLock) {
            selfDismissedSourceKeys.remove(sbnKey)
        }
    }

    private fun consumeSelfDismissedSourceKey(sbnKey: String): Boolean {
        return synchronized(selfDismissLock) {
            selfDismissedSourceKeys.remove(sbnKey)
        }
    }

    private fun rememberTrackedFlashlightSourceKey(sbnKey: String) {
        synchronized(selfDismissLock) {
            selfDismissedFlashlightSourceKeys.remove(sbnKey)
            trackedFlashlightSourceKey = sbnKey
        }
    }

    private fun forgetTrackedFlashlightSourceKey(sbnKey: String) {
        synchronized(selfDismissLock) {
            if (trackedFlashlightSourceKey == sbnKey) {
                trackedFlashlightSourceKey = null
            }
        }
    }

    private fun clearTrackedFlashlightSourceKey() {
        synchronized(selfDismissLock) {
            trackedFlashlightSourceKey = null
        }
    }

    private fun resetFlashlightSourceDismissState() {
        synchronized(selfDismissLock) {
            trackedFlashlightSourceKey = null
            selfDismissedFlashlightSourceKeys.clear()
        }
    }

    private fun forgetSelfDismissedFlashlightSourceKey(sbnKey: String) {
        synchronized(selfDismissLock) {
            selfDismissedFlashlightSourceKeys.remove(sbnKey)
        }
    }

    private fun consumeSelfDismissedFlashlightSourceKey(sbnKey: String): Boolean {
        return synchronized(selfDismissLock) {
            selfDismissedFlashlightSourceKeys.remove(sbnKey)
        }
    }
    private fun scheduleRebind(reason: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return
        }
        if (rebindScheduled) {
            return
        }

        val delayMs = min(MAX_REBIND_DELAY_MS, INITIAL_REBIND_DELAY_MS shl rebindAttempts)
        rebindScheduled = true
        mainHandler.postDelayed({
            rebindScheduled = false
            val requested = requestRebindIfEnabled(applicationContext, reason)
            if (!requested) {
                return@postDelayed
            }
            rebindAttempts = min(rebindAttempts + 1, MAX_REBIND_ATTEMPTS)
        }, delayMs)
    }

    private fun scheduleSnapshotSync() {
        if (snapshotSyncScheduled) {
            return
        }
        snapshotSyncScheduled = true
        mainHandler.postDelayed(snapshotSyncRunnable, SNAPSHOT_SYNC_INTERVAL_MS)
    }

    companion object {
        private const val TAG = "LiveUpdateListener"
        private const val INITIAL_REBIND_DELAY_MS = 1_000L
        private const val MAX_REBIND_DELAY_MS = 30_000L
        private const val MAX_REBIND_ATTEMPTS = 6
        private const val SNAPSHOT_SYNC_INTERVAL_MS = 4_000L
        private const val FLASHLIGHT_SOURCE_SNOOZE_MS = 1_500L
        private const val FLASHLIGHT_SOURCE_VERIFY_DELAY_MS = 300L
        private const val FLASHLIGHT_SOURCE_PACKAGE = "com.android.systemui"
        private const val FLASHLIGHT_SOURCE_CHANNEL_ID = "FLASHLIGHT_ONGOING"
        private const val FLASHLIGHT_SOURCE_TAG = "Flashlight"

        @Volatile
        private var activeInstance: LiveUpdateNotificationListenerService? = null

        fun requestFlashlightSnapshotSync() {
            activeInstance?.requestImmediateFlashlightSnapshotSync()
        }

        fun requestFlashlightSourceDismissal() {
            val listener = activeInstance
            if (listener == null) {
                Log.w(TAG, "Skip flashlight source dismiss: listener is not active")
                return
            }
            listener.requestTrackedFlashlightSourceDismissal()
        }

        private fun requestRebindIfEnabled(context: Context, reason: String): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                return false
            }
            if (!isListenerEnabled(context)) {
                Log.w(TAG, "Skip rebind ($reason): listener disabled")
                return false
            }

            return try {
                requestRebind(ComponentName(context, LiveUpdateNotificationListenerService::class.java))
                Log.i(TAG, "Requested listener rebind ($reason)")
                true
            } catch (error: Throwable) {
                Log.e(TAG, "Failed listener rebind ($reason)", error)
                false
            }
        }

        private fun isListenerEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val service = ComponentName(context, LiveUpdateNotificationListenerService::class.java)
            return enabled.split(":")
                .mapNotNull(ComponentName::unflattenFromString)
                .any { it == service }
        }
    }
}
