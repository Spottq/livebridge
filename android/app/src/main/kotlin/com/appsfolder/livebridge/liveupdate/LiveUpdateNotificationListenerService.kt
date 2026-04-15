package com.kakao.taxi.liveupdate

import android.content.ComponentName
import android.content.Context
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
    private val mainHandler = Handler(Looper.getMainLooper())
    private val selfDismissLock = Any()
    private val selfDismissedSourceKeys = mutableSetOf<String>()
    private var rebindAttempts = 0
    private var rebindScheduled = false
    private var snapshotSyncScheduled = false

    private val snapshotSyncRunnable = object : Runnable {
        override fun run() {
            snapshotSyncScheduled = false
            if (isUnsupportedDevice()) {
                LiveUpdateNotifier.cancelAllMirrored(applicationContext)
                return
            }
            if (!prefs.getConverterEnabled()) {
                scheduleSnapshotSync()
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

            for (sbn in snapshots) {
                if (sbn.packageName == packageName) {
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

        if (isUnsupportedDevice()) {
            LiveUpdateNotifier.cancelAllMirrored(applicationContext)
            return
        }
        if (!prefs.getConverterEnabled()) {
            LiveUpdateNotifier.cancelAllMirrored(applicationContext)
        }

        LiveUpdateNotifier.ensureChannel(applicationContext)
        scheduleSnapshotSync()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        rebindAttempts = 0
        rebindScheduled = false

        if (isUnsupportedDevice()) {
            LiveUpdateNotifier.cancelAllMirrored(applicationContext)
            return
        }
        if (!prefs.getConverterEnabled()) {
            LiveUpdateNotifier.cancelAllMirrored(applicationContext)
            scheduleSnapshotSync()
            return
        }

        val snapshots = try {
            activeNotifications?.toList().orEmpty()
        } catch (error: Throwable) {
            Log.w(TAG, "Unable to read active notifications on connect", error)
            emptyList()
        }

        if (snapshots.isEmpty()) {
            return
        }

        for (sbn in snapshots) {
            if (sbn.packageName == packageName) {
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
            return
        }
        if (consumeSelfDismissedSourceKey(sbn.key)) {
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
        mainHandler.removeCallbacksAndMessages(null)
        rebindScheduled = false
        snapshotSyncScheduled = false
        super.onDestroy()
    }

    private fun processIncomingNotification(sbn: StatusBarNotification) {
        val result = LiveUpdateNotifier.maybeMirror(applicationContext, prefs, sbn)
        maybeDismissOriginalSource(sbn, result)
    }

    private fun maybeDismissOriginalSource(
        sbn: StatusBarNotification,
        result: LiveUpdateNotifier.MirrorResult
    ) {
        if (!result.mirrored) {
            return
        }
        if (!prefs.getNotificationDedupEnabled()) {
            return
        }
        if (!sbn.isClearable) {
            return
        }
        if (!prefs.isNotificationDedupPackageAllowed(sbn.packageName)) {
            return
        }
        val shouldDismiss = when (prefs.getNotificationDedupMode()) {
            "otp_only" -> result.dedupKind == LiveUpdateNotifier.MirrorDedupKind.OTP
            else -> {
                result.dedupKind == LiveUpdateNotifier.MirrorDedupKind.OTP ||
                        result.dedupKind == LiveUpdateNotifier.MirrorDedupKind.STATUS
            }
        }
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
