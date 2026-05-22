package com.kakao.taxi.liveupdate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationCapsuleActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_CLEAR_PACKAGE) {
            return
        }
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
            ?.trim()
            .orEmpty()
        if (packageName.isBlank()) {
            return
        }
        LiveUpdateNotificationListenerService.requestClearPackageNotifications(packageName)
    }

    companion object {
        const val ACTION_CLEAR_PACKAGE = "com.kakao.taxi.action.CLEAR_NOTIFICATION_CAPSULE_PACKAGE"
        const val EXTRA_PACKAGE_NAME = "package_name"
    }
}
