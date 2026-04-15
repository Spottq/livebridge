package com.nowbar.api.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

internal object SafeNotificationPoster {

    fun canPost(
        context: Context,
        notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(context)
    ): Boolean {
        if (!notificationManager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun notify(
        context: Context,
        notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(context),
        notificationId: Int,
        notification: Notification
    ): Boolean {
        if (!canPost(context, notificationManager)) return false

        notificationManager.notify(notificationId, notification)
        return true
    }
}
