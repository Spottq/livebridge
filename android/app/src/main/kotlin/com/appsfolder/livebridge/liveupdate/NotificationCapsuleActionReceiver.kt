package com.kakao.taxi.liveupdate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationCapsuleActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_CLEAR_NOTIFICATIONS) {
            return
        }

        LiveUpdateNotificationListenerService.requestClearNotificationCapsuleNotifications(
            context.applicationContext
        )
    }

    companion object {
        const val ACTION_CLEAR_NOTIFICATIONS = "com.kakao.taxi.action.CLEAR_NOTIFICATIONS"
    }
}
