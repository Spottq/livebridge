package com.kakao.taxi.liveupdate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationCapsuleActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_CLEAR_PACKAGE -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
                    ?.trim()
                    .orEmpty()
                if (packageName.isBlank()) {
                    return
                }
                LiveUpdateNotificationListenerService.requestClearPackageNotifications(packageName)
            }
            ACTION_CLEAR_NOTIFICATION_KEYS -> {
                val notificationKeys = intent.getStringArrayListExtra(EXTRA_NOTIFICATION_KEYS)
                    ?.map { key -> key.trim() }
                    ?.filter { key -> key.isNotBlank() }
                    .orEmpty()
                if (notificationKeys.isEmpty()) {
                    return
                }
                LiveUpdateNotificationListenerService.requestClearNotificationKeys(notificationKeys)
            }
        }
    }

    companion object {
        const val ACTION_CLEAR_PACKAGE = "com.kakao.taxi.action.CLEAR_NOTIFICATION_CAPSULE_PACKAGE"
        const val ACTION_CLEAR_NOTIFICATION_KEYS =
            "com.kakao.taxi.action.CLEAR_NOTIFICATION_CAPSULE_KEYS"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_NOTIFICATION_KEYS = "notification_keys"
    }
}
