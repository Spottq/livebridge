package com.kakao.taxi.liveupdate

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class NotificationPanelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_OPEN_NOTIFICATION_PANEL) {
            return
        }
        if (!openNotificationPanel(context.applicationContext)) {
            Log.w(TAG, "Unable to open notification panel from capsule")
        }
    }

    companion object {
        private const val TAG = "NotificationPanelReceiver"
        const val ACTION_OPEN_NOTIFICATION_PANEL =
            "com.kakao.taxi.liveupdate.OPEN_NOTIFICATION_PANEL"

        fun pendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, NotificationPanelReceiver::class.java).apply {
                action = ACTION_OPEN_NOTIFICATION_PANEL
                setPackage(context.packageName)
            }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_OPEN_NOTIFICATION_PANEL,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun openNotificationPanel(context: Context): Boolean {
            val statusBarManager = context.getSystemService("statusbar") ?: return false
            val methodNames = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                listOf("expandNotificationsPanel", "expandNotificationsPanelForCallers")
            } else {
                listOf("expandNotificationsPanel")
            }

            methodNames.forEach { methodName ->
                val opened = runCatching {
                    val method = statusBarManager.javaClass.getMethod(methodName)
                    method.invoke(statusBarManager)
                }.onFailure { error ->
                    Log.v(TAG, "Notification panel method failed: $methodName", error)
                }.isSuccess
                if (opened) {
                    return true
                }
            }
            return false
        }

        private const val REQUEST_CODE_OPEN_NOTIFICATION_PANEL = 41243
    }
}
