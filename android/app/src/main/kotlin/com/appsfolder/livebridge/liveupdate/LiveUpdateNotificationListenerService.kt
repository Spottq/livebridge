package com.kakao.taxi.liveupdate

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlin.math.min

class LiveUpdateNotificationListenerService : NotificationListenerService() {
    private val prefs by lazy { ConverterPrefs(applicationContext) }
    private val flashlightController by lazy { FlashlightController(applicationContext) }
    private val mainHandler = Handler(Looper.getMainLooper())
    private val selfDismissLock = Any()
    private val selfDismissedSourcesByKey = mutableMapOf<String, ProtectedSourceDismissal>()
    private val protectedMirrorDismissalsById = mutableMapOf<Int, ProtectedSourceDismissal>()
    private val selfDismissedFlashlightSourceKeys = mutableSetOf<String>()
    private var trackedFlashlightSourceKey: String? = null
    private var isTorchCallbackRegistered = false
    private var rebindAttempts = 0
    private var rebindScheduled = false
    private var snapshotSyncScheduled = false
    private var lockscreenStateReceiverRegistered = false
    private var chargingInfoReceiverRegistered = false

    private val lockscreenPrivacyRefreshRunnable = Runnable {
        requestImmediateSnapshotSync()
    }

    private val lockscreenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF,
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_USER_PRESENT -> handleLockscreenStateChanged(intent.action)
            }
        }
    }

    private val chargingInfoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handleChargingInfoBatteryChanged(intent)
        }
    }

    private data class ProtectedSourceDismissal(
        val sourceKey: String,
        val sourcePackageName: String,
        val sourceId: Int,
        val sourceTag: String?,
        val sourceGroupKey: String?,
        val sourceSbn: StatusBarNotification,
        val mirrorNotificationId: Int,
        val mirrorKey: String?,
        val expiresAtMs: Long,
        var sourceRemovalConsumed: Boolean = false,
        var mirrorRepostAttempts: Int = 0
    ) {
        fun matchesSource(sbn: StatusBarNotification): Boolean {
            if (sourceKey == sbn.key) {
                return true
            }
            if (sourcePackageName == sbn.packageName &&
                sourceId == sbn.id &&
                sourceTag == sbn.tag
            ) {
                return true
            }
            return sourcePackageName == sbn.packageName &&
                !sourceGroupKey.isNullOrBlank() &&
                sourceGroupKey == sbn.groupKey &&
                sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
        }
    }

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
                NetworkSpeedForegroundService.stop(applicationContext)
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
            syncChargingInfoMonitoring(snapshots)

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

            refreshNotificationCapsule(snapshots)
            scheduleSnapshotSync()
        }
    }

    override fun onCreate() {
        super.onCreate()
        activeInstance = this

        if (isUnsupportedDevice()) {
            FlashlightSourceState.clear()
            FlashlightForegroundService.stop(applicationContext)
            NetworkSpeedForegroundService.stop(applicationContext)
            LiveUpdateNotifier.cancelAllMirrored(applicationContext)
            return
        }
        if (!prefs.getConverterEnabled()) {
            LiveUpdateNotifier.cancelAllMirrored(applicationContext)
        }

        registerLockscreenStateReceiver()
        syncTorchMonitoring()
        syncChargingInfoMonitoring()
        LiveUpdateNotifier.ensureChannel(applicationContext)
        syncNetworkSpeedService()
        scheduleSnapshotSync()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        rebindAttempts = 0
        rebindScheduled = false

        if (isUnsupportedDevice()) {
            FlashlightSourceState.clear()
            FlashlightForegroundService.stop(applicationContext)
            NetworkSpeedForegroundService.stop(applicationContext)
            LiveUpdateNotifier.cancelAllMirrored(applicationContext)
            return
        }

        syncNetworkSpeedService()
        syncTorchMonitoring()
        val snapshots = try {
            activeNotifications?.toList().orEmpty()
        } catch (error: Throwable) {
            Log.w(TAG, "Unable to read active notifications on connect", error)
            emptyList()
        }

        syncChargingInfoMonitoring(snapshots)
        syncFlashlightMirror(snapshots)

        if (!prefs.getConverterEnabled()) {
            LiveUpdateNotifier.cancelAllMirrored(applicationContext)
            scheduleSnapshotSync()
            return
        }

        if (snapshots.isEmpty()) {
            refreshNotificationCapsule(snapshots)
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
        refreshNotificationCapsule(snapshots)
        scheduleSnapshotSync()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (isUnsupportedDevice()) {
            return
        }
        LiveUpdateNotifier.cancelNotificationCapsule(applicationContext)
        syncNetworkSpeedService()
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
            refreshChargingInfoFromActiveNotifications()
            refreshNotificationCapsuleFromActiveNotifications()
            return
        }
        refreshChargingInfoFromActiveNotifications(sbn)
        if (!prefs.getConverterEnabled()) {
            LiveUpdateNotifier.cancelMirrored(applicationContext, sbn)
            refreshNotificationCapsuleFromActiveNotifications()
            return
        }

        try {
            processIncomingNotification(sbn)
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to process posted notification: ${sbn.key}", error)
        }
        refreshNotificationCapsuleFromActiveNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        handleNotificationRemoved(sbn, reason = null)
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
        reason: Int
    ) {
        handleNotificationRemoved(sbn, reason)
    }

    private fun handleNotificationRemoved(sbn: StatusBarNotification?, reason: Int?) {
        sbn ?: return
        if (isUnsupportedDevice()) {
            return
        }
        if (sbn.packageName == packageName) {
            if (handleProtectedMirrorRemoval(sbn, reason)) {
                return
            }
            LiveUpdateNotifier.handleMirroredRemoved(applicationContext, sbn)
            return
        }
        refreshChargingInfoFromActiveNotifications()
        if (consumeSelfDismissedSource(sbn)) {
            refreshNotificationCapsuleFromActiveNotifications()
            return
        }
        if (isFlashlightSourceNotification(sbn)) {
            forgetTrackedFlashlightSourceKey(sbn.key)
            if (consumeSelfDismissedFlashlightSourceKey(sbn.key)) {
                refreshNotificationCapsuleFromActiveNotifications()
                return
            }
            FlashlightSourceState.clear()
            FlashlightForegroundService.stop(applicationContext)
            refreshNotificationCapsuleFromActiveNotifications()
            return
        }
        try {
            LiveUpdateNotifier.cancelMirrored(applicationContext, sbn)
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to process removed notification: ${sbn.key}", error)
        }
        refreshNotificationCapsuleFromActiveNotifications()
    }

    private fun isUnsupportedDevice(): Boolean {
        return DeviceBlocker.isBlockedDevice()
    }

    private fun syncNetworkSpeedService() {
        if (prefs.getNetworkSpeedEnabled() && !DeviceBlocker.isBlockedDevice()) {
            NetworkSpeedForegroundService.sync(applicationContext)
        } else {
            NetworkSpeedForegroundService.stop(applicationContext)
        }
    }

    private fun refreshNotificationCapsule(snapshots: Collection<StatusBarNotification>) {
        LiveUpdateNotifier.refreshNotificationCapsule(
            context = applicationContext,
            prefs = prefs,
            snapshots = snapshots
        )
    }

    private fun refreshNotificationCapsuleFromActiveNotifications() {
        val snapshots = try {
            activeNotifications?.toList().orEmpty()
        } catch (error: Throwable) {
            Log.w(TAG, "Unable to refresh notification capsule from active notifications", error)
            return
        }
        refreshNotificationCapsule(snapshots)
    }

    private fun clearNotificationsForPackage(packageName: String) {
        val normalizedPackageName = packageName.trim().lowercase()
        if (normalizedPackageName.isBlank()) {
            return
        }
        val snapshots = try {
            activeNotifications?.toList().orEmpty()
        } catch (error: Throwable) {
            Log.w(TAG, "Unable to clear package notifications", error)
            return
        }
        val sourceKeys = snapshots
            .filter { sbn -> sbn.packageName.lowercase() == normalizedPackageName }
            .filter { sbn -> sbn.isClearable }
            .map { sbn -> sbn.key }
            .distinct()

        if (sourceKeys.isEmpty()) {
            refreshNotificationCapsule(snapshots)
            return
        }

        sourceKeys.forEach { sourceKey ->
            runCatching {
                cancelNotification(sourceKey)
            }.onFailure { error ->
                Log.w(TAG, "cancelNotification failed for capsule clear: $sourceKey", error)
            }
        }
        runCatching {
            cancelNotifications(sourceKeys.toTypedArray())
        }.onFailure { error ->
            Log.w(TAG, "cancelNotifications failed for capsule clear", error)
        }
        mainHandler.postDelayed(
            { refreshNotificationCapsuleFromActiveNotifications() },
            CAPSULE_CLEAR_REFRESH_DELAY_MS
        )
    }

    private fun clearNotificationsForKeys(notificationKeys: Collection<String>) {
        val requestedKeys = notificationKeys
            .map { key -> key.trim() }
            .filter { key -> key.isNotBlank() }
            .toSet()
        if (requestedKeys.isEmpty()) {
            return
        }
        val snapshots = try {
            activeNotifications?.toList().orEmpty()
        } catch (error: Throwable) {
            Log.w(TAG, "Unable to clear notification capsule keys", error)
            return
        }
        val sourceKeys = snapshots
            .filter { sbn -> sbn.key in requestedKeys }
            .filter { sbn -> sbn.isClearable }
            .map { sbn -> sbn.key }
            .distinct()

        if (sourceKeys.isEmpty()) {
            refreshNotificationCapsule(snapshots)
            return
        }

        sourceKeys.forEach { sourceKey ->
            runCatching {
                cancelNotification(sourceKey)
            }.onFailure { error ->
                Log.w(TAG, "cancelNotification failed for capsule clear: $sourceKey", error)
            }
        }
        runCatching {
            cancelNotifications(sourceKeys.toTypedArray())
        }.onFailure { error ->
            Log.w(TAG, "cancelNotifications failed for capsule clear", error)
        }
        mainHandler.postDelayed(
            { refreshNotificationCapsuleFromActiveNotifications() },
            CAPSULE_CLEAR_REFRESH_DELAY_MS
        )
    }

    override fun onDestroy() {
        unregisterLockscreenStateReceiver()
        unregisterTorchCallbackIfNeeded()
        unregisterChargingInfoReceiverIfNeeded()
        mainHandler.removeCallbacksAndMessages(null)
        rebindScheduled = false
        snapshotSyncScheduled = false
        if (activeInstance === this) {
            activeInstance = null
        }
        LiveUpdateNotifier.cancelNotificationCapsule(applicationContext)
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

    private fun syncChargingInfoMonitoring(
        snapshots: Collection<StatusBarNotification>? = null
    ) {
        if (
            prefs.getSmartChargingInfoEnabled() &&
            prefs.getConverterEnabled() &&
            !isUnsupportedDevice()
        ) {
            ensureChargingInfoReceiverRegistered()
            LiveUpdateNotifier.refreshChargingInfo(
                context = applicationContext,
                prefs = prefs,
                activeNotifications = snapshots ?: activeNotificationSnapshotsForChargingInfo()
            )
        } else {
            unregisterChargingInfoReceiverIfNeeded()
            LiveUpdateNotifier.cancelChargingInfo(applicationContext)
        }
    }

    private fun refreshChargingInfoFromActiveNotifications(
        removedNotification: StatusBarNotification? = null
    ) {
        if (!prefs.getSmartChargingInfoEnabled()) {
            return
        }
        LiveUpdateNotifier.refreshChargingInfo(
            context = applicationContext,
            prefs = prefs,
            activeNotifications = activeNotificationSnapshotsForChargingInfo(removedNotification)
        )
    }

    private fun activeNotificationSnapshotsForChargingInfo(
        extraNotification: StatusBarNotification? = null
    ): List<StatusBarNotification> {
        val snapshots = try {
            activeNotifications?.toList().orEmpty()
        } catch (error: Throwable) {
            Log.w(TAG, "Unable to read active notifications for charging info", error)
            emptyList()
        }
        extraNotification ?: return snapshots
        return (snapshots + extraNotification).distinctBy { sbn -> sbn.key }
    }

    private fun registerLockscreenStateReceiver() {
        if (lockscreenStateReceiverRegistered) {
            return
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(
                    lockscreenStateReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(lockscreenStateReceiver, filter)
            }
        }.onSuccess {
            lockscreenStateReceiverRegistered = true
        }.onFailure { error ->
            Log.w(TAG, "Failed to register lockscreen state receiver", error)
        }
    }

    private fun unregisterLockscreenStateReceiver() {
        if (!lockscreenStateReceiverRegistered) {
            return
        }
        runCatching { unregisterReceiver(lockscreenStateReceiver) }
            .onFailure { error ->
                Log.w(TAG, "Failed to unregister lockscreen state receiver", error)
            }
        lockscreenStateReceiverRegistered = false
    }

    private fun ensureChargingInfoReceiverRegistered() {
        if (chargingInfoReceiverRegistered) {
            return
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(
                    chargingInfoReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(chargingInfoReceiver, filter)
            }
        }.onSuccess {
            chargingInfoReceiverRegistered = true
        }.onFailure { error ->
            Log.w(TAG, "Failed to register charging info receiver", error)
        }
    }

    private fun unregisterChargingInfoReceiverIfNeeded() {
        if (!chargingInfoReceiverRegistered) {
            return
        }
        runCatching { unregisterReceiver(chargingInfoReceiver) }
            .onFailure { error ->
                Log.w(TAG, "Failed to unregister charging info receiver", error)
            }
        chargingInfoReceiverRegistered = false
    }

    private fun handleChargingInfoBatteryChanged(intent: Intent?) {
        val batteryIntent = intent?.takeIf { it.action == Intent.ACTION_BATTERY_CHANGED }
        LiveUpdateNotifier.refreshChargingInfo(
            context = applicationContext,
            prefs = prefs,
            batteryIntent = batteryIntent,
            activeNotifications = activeNotificationSnapshotsForChargingInfo()
        )
    }

    private fun handleLockscreenStateChanged(action: String?) {
        if (!prefs.getConverterEnabled() || !prefs.getHideLockscreenContentEnabled()) {
            return
        }
        Log.d(TAG, "Lockscreen privacy state changed: $action")
        mainHandler.removeCallbacks(lockscreenPrivacyRefreshRunnable)
        requestImmediateSnapshotSync()
        mainHandler.postDelayed(
            lockscreenPrivacyRefreshRunnable,
            LOCKSCREEN_PRIVACY_REFRESH_DELAY_MS
        )
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

    fun requestImmediateChargingInfoSync() {
        syncChargingInfoMonitoring()
    }

    fun requestImmediateSnapshotSync() {
        mainHandler.removeCallbacks(snapshotSyncRunnable)
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
        if (!result.removeSource && !sbn.isClearable) {
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
            result.removeSource ||
                appPresentationRemoveOriginal ||
                legacyDedup ||
                upstreamDismiss
        if (!shouldDismiss) {
            return
        }

        val mirrorNotificationId = result.notificationId
        if (mirrorNotificationId == null) {
            Log.w(TAG, "Skip original notification dismissal: missing mirror id for ${sbn.key}")
            return
        }
        scheduleOriginalSourceDismissal(
            sourceSbn = sbn,
            mirrorNotificationId = mirrorNotificationId,
            mirrorKey = result.mirrorKey,
            attempt = 0
        )
    }

    private fun scheduleOriginalSourceDismissal(
        sourceSbn: StatusBarNotification,
        mirrorNotificationId: Int,
        mirrorKey: String?,
        attempt: Int
    ) {
        mainHandler.postDelayed(
            {
                dismissOriginalSourceWhenMirrorActive(
                    sourceSbn = sourceSbn,
                    mirrorNotificationId = mirrorNotificationId,
                    mirrorKey = mirrorKey,
                    attempt = attempt
                )
            },
            ORIGINAL_DISMISS_RETRY_DELAY_MS
        )
    }

    private fun dismissOriginalSourceWhenMirrorActive(
        sourceSbn: StatusBarNotification,
        mirrorNotificationId: Int,
        mirrorKey: String?,
        attempt: Int
    ) {
        val sourceKey = sourceSbn.key
        val active = try {
            activeNotifications
        } catch (error: Throwable) {
            Log.w(TAG, "Failed to verify mirror before original dismissal: $sourceKey", error)
            retryOriginalSourceDismissal(sourceSbn, mirrorNotificationId, mirrorKey, attempt)
            return
        }
        if (active == null) {
            retryOriginalSourceDismissal(sourceSbn, mirrorNotificationId, mirrorKey, attempt)
            return
        }

        val snapshots = active.toList()
        if (snapshots.none { it.key == sourceKey }) {
            Log.v(TAG, "Skip original notification dismissal: source is no longer active: $sourceKey")
            return
        }
        if (snapshots.none { isExpectedMirrorNotification(it, mirrorNotificationId) }) {
            retryOriginalSourceDismissal(sourceSbn, mirrorNotificationId, mirrorKey, attempt)
            return
        }

        rememberSelfDismissedSource(sourceSbn, mirrorNotificationId, mirrorKey)
        val dismissRequested = requestDismissOrSnoozeSourceNotification(
            sourceKey = sourceKey,
            label = "original notification"
        )
        if (!dismissRequested) {
            forgetSelfDismissedSourceKey(sourceKey)
        }
    }

    private fun retryOriginalSourceDismissal(
        sourceSbn: StatusBarNotification,
        mirrorNotificationId: Int,
        mirrorKey: String?,
        attempt: Int
    ) {
        if (attempt >= ORIGINAL_DISMISS_MAX_ATTEMPTS) {
            Log.w(TAG, "Skip original notification dismissal: mirror not active for ${sourceSbn.key}")
            return
        }
        scheduleOriginalSourceDismissal(
            sourceSbn = sourceSbn,
            mirrorNotificationId = mirrorNotificationId,
            mirrorKey = mirrorKey,
            attempt = attempt + 1
        )
    }

    private fun isExpectedMirrorNotification(
        sbn: StatusBarNotification,
        mirrorNotificationId: Int
    ): Boolean {
        if (sbn.packageName != packageName || sbn.id != mirrorNotificationId) {
            return false
        }
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            LiveUpdateNotifier.isMirrorNotificationChannel(sbn.notification.channelId)
    }

    private fun requestDismissOrSnoozeSourceNotification(
        sourceKey: String,
        label: String
    ): Boolean {
        val cancelDirectRequested = runCatching {
            cancelNotification(sourceKey)
        }.onSuccess {
            Log.i(TAG, "Requested $label cancel via cancelNotification: $sourceKey")
        }.onFailure { error ->
            Log.w(TAG, "cancelNotification failed for $label: $sourceKey", error)
        }.isSuccess

        val cancelBatchRequested = runCatching {
            cancelNotifications(arrayOf(sourceKey))
        }.onSuccess {
            Log.i(TAG, "Requested $label cancel via cancelNotifications: $sourceKey")
        }.onFailure { error ->
            Log.w(TAG, "cancelNotifications failed for $label: $sourceKey", error)
        }.isSuccess

        val snoozeRequested = runCatching {
            snoozeNotification(sourceKey, ORIGINAL_SOURCE_SNOOZE_MS)
        }.onSuccess {
            Log.i(TAG, "Requested $label snooze fallback: $sourceKey")
        }.onFailure { error ->
            Log.w(TAG, "snoozeNotification failed for $label: $sourceKey", error)
        }.isSuccess

        if (!cancelDirectRequested && !cancelBatchRequested && !snoozeRequested) {
            Log.w(TAG, "Unable to dismiss or snooze $label: $sourceKey")
            return false
        }
        return true
    }

    private fun rememberSelfDismissedSource(
        sbn: StatusBarNotification,
        mirrorNotificationId: Int,
        mirrorKey: String?
    ) {
        synchronized(selfDismissLock) {
            val now = SystemClock.elapsedRealtime()
            pruneProtectedDismissalsLocked(now)
            val record = ProtectedSourceDismissal(
                sourceKey = sbn.key,
                sourcePackageName = sbn.packageName,
                sourceId = sbn.id,
                sourceTag = sbn.tag,
                sourceGroupKey = sbn.groupKey,
                sourceSbn = sbn,
                mirrorNotificationId = mirrorNotificationId,
                mirrorKey = mirrorKey,
                expiresAtMs = now + SELF_DISMISS_PROTECTION_MS
            )
            selfDismissedSourcesByKey[sbn.key]?.let(::removeProtectedDismissalLocked)
            selfDismissedSourcesByKey[sbn.key] = record
            protectedMirrorDismissalsById[mirrorNotificationId] = record
        }
    }

    private fun forgetSelfDismissedSourceKey(sbnKey: String) {
        synchronized(selfDismissLock) {
            val record = selfDismissedSourcesByKey[sbnKey] ?: return
            removeProtectedDismissalLocked(record)
        }
    }

    private fun consumeSelfDismissedSource(sbn: StatusBarNotification): Boolean {
        val record = synchronized(selfDismissLock) {
            val now = SystemClock.elapsedRealtime()
            pruneProtectedDismissalsLocked(now)
            val match = selfDismissedSourcesByKey[sbn.key]
                ?: selfDismissedSourcesByKey.values.firstOrNull { it.matchesSource(sbn) }
            match?.also { it.sourceRemovalConsumed = true }
        }
        if (record != null) {
            Log.v(
                TAG,
                "Skip mirrored cancel for self-dismissed source: ${sbn.key} -> ${record.mirrorKey}"
            )
            return true
        }
        return false
    }

    private fun handleProtectedMirrorRemoval(
        sbn: StatusBarNotification,
        reason: Int?
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            sbn.notification.channelId != LiveUpdateNotifier.CHANNEL_ID
        ) {
            return false
        }

        val mirrorId = sbn.id
        val shouldScheduleRepost = synchronized(selfDismissLock) {
            val now = SystemClock.elapsedRealtime()
            pruneProtectedDismissalsLocked(now)
            val record = protectedMirrorDismissalsById[mirrorId] ?: return false
            if (isUserDrivenMirrorRemoval(reason)) {
                removeProtectedDismissalLocked(record)
                return false
            }
            if (record.mirrorRepostAttempts >= PROTECTED_MIRROR_MAX_REPOSTS) {
                Log.w(
                    TAG,
                    "Protected mirror removed again; stop reposting id=$mirrorId reason=${removalReasonName(reason)}"
                )
                removeProtectedDismissalLocked(record)
                return true
            }
            record.mirrorRepostAttempts += 1
            true
        }

        if (shouldScheduleRepost) {
            Log.i(
                TAG,
                "Mirror removal protected after original dismissal: id=$mirrorId reason=${removalReasonName(reason)}"
            )
            scheduleProtectedMirrorRepost(mirrorId)
        }
        return true
    }

    private fun scheduleProtectedMirrorRepost(mirrorNotificationId: Int) {
        mainHandler.postDelayed(
            { repostProtectedMirrorIfMissing(mirrorNotificationId) },
            PROTECTED_MIRROR_REPOST_DELAY_MS
        )
    }

    private fun repostProtectedMirrorIfMissing(mirrorNotificationId: Int) {
        val record = synchronized(selfDismissLock) {
            val now = SystemClock.elapsedRealtime()
            pruneProtectedDismissalsLocked(now)
            protectedMirrorDismissalsById[mirrorNotificationId]
        } ?: return

        val mirrorStillActive = try {
            activeNotifications?.any { isExpectedMirrorNotification(it, mirrorNotificationId) } == true
        } catch (error: Throwable) {
            Log.w(TAG, "Failed to verify protected mirror before repost: $mirrorNotificationId", error)
            false
        }
        if (mirrorStillActive) {
            return
        }

        val result = LiveUpdateNotifier.maybeMirror(applicationContext, prefs, record.sourceSbn)
        if (!result.mirrored) {
            Log.w(
                TAG,
                "Protected mirror repost skipped: source no longer mirrors ${record.sourceKey}"
            )
        }
    }

    private fun pruneProtectedDismissalsLocked(now: Long) {
        val expiredRecords = selfDismissedSourcesByKey.values
            .filter { it.expiresAtMs < now }
            .toList()
        expiredRecords.forEach(::removeProtectedDismissalLocked)
    }

    private fun removeProtectedDismissalLocked(record: ProtectedSourceDismissal) {
        val sourceKeys = selfDismissedSourcesByKey
            .filterValues { it === record }
            .keys
            .toList()
        sourceKeys.forEach(selfDismissedSourcesByKey::remove)
        val mirrorIds = protectedMirrorDismissalsById
            .filterValues { it === record }
            .keys
            .toList()
        mirrorIds.forEach(protectedMirrorDismissalsById::remove)
    }

    private fun isUserDrivenMirrorRemoval(reason: Int?): Boolean {
        return reason == NotificationListenerService.REASON_CANCEL ||
            reason == NotificationListenerService.REASON_CANCEL_ALL ||
            reason == NotificationListenerService.REASON_CLICK
    }

    private fun removalReasonName(reason: Int?): String {
        return when (reason) {
            null -> "unknown"
            NotificationListenerService.REASON_CANCEL -> "cancel"
            NotificationListenerService.REASON_CANCEL_ALL -> "cancel_all"
            NotificationListenerService.REASON_CLICK -> "click"
            NotificationListenerService.REASON_APP_CANCEL -> "app_cancel"
            NotificationListenerService.REASON_APP_CANCEL_ALL -> "app_cancel_all"
            NotificationListenerService.REASON_LISTENER_CANCEL -> "listener_cancel"
            NotificationListenerService.REASON_LISTENER_CANCEL_ALL -> "listener_cancel_all"
            else -> "reason_$reason"
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
        private const val ORIGINAL_DISMISS_RETRY_DELAY_MS = 250L
        private const val ORIGINAL_DISMISS_MAX_ATTEMPTS = 6
        private const val ORIGINAL_SOURCE_SNOOZE_MS = 60_000L
        private const val SELF_DISMISS_PROTECTION_MS = 10_000L
        private const val PROTECTED_MIRROR_REPOST_DELAY_MS = 350L
        private const val PROTECTED_MIRROR_MAX_REPOSTS = 2
        private const val LOCKSCREEN_PRIVACY_REFRESH_DELAY_MS = 650L
        private const val CAPSULE_CLEAR_REFRESH_DELAY_MS = 250L
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

        fun requestSnapshotSync() {
            activeInstance?.requestImmediateSnapshotSync()
        }

        fun requestChargingInfoSync() {
            activeInstance?.requestImmediateChargingInfoSync()
        }

        fun requestClearPackageNotifications(packageName: String) {
            val listener = activeInstance
            if (listener == null) {
                Log.w(TAG, "Skip capsule clear: listener is not active")
                return
            }
            listener.clearNotificationsForPackage(packageName)
        }

        fun requestClearNotificationKeys(notificationKeys: Collection<String>) {
            val listener = activeInstance
            if (listener == null) {
                Log.w(TAG, "Skip capsule clear: listener is not active")
                return
            }
            listener.clearNotificationsForKeys(notificationKeys)
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
