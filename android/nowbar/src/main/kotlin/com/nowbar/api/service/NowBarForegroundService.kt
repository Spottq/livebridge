package com.nowbar.api.service

import android.app.Notification
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import com.nowbar.api.NowBarConfig
import com.nowbar.api.NowBarManager
import com.nowbar.api.cards.NowBarCard
import com.nowbar.api.notification.SafeNotificationPoster

/**
 * Convenience base class for foreground-service driven use cases.
 *
 * Keep your service logic in the service.
 * Let this class handle only notification-related plumbing.
 */
abstract class NowBarForegroundService : LifecycleService() {

    companion object {
        const val DEFAULT_NOTIFICATION_ID = 200
    }

    private var currentNotificationId: Int = DEFAULT_NOTIFICATION_ID

    protected fun createNowBarChannel(config: NowBarConfig) {
        NowBarManager.createNotificationChannel(this, config)
    }

    protected fun buildNowBarNotification(
        config: NowBarConfig,
        card: NowBarCard
    ): Notification = NowBarManager.buildNotification(this, config, card)

    protected fun startNowBar(
        notification: Notification,
        notificationId: Int = DEFAULT_NOTIFICATION_ID,
        foregroundServiceType: Int = ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
    ) {
        currentNotificationId = notificationId
        ServiceCompat.startForeground(
            this,
            notificationId,
            notification,
            foregroundServiceType
        )
    }

    protected fun startNowBar(
        config: NowBarConfig,
        card: NowBarCard,
        foregroundServiceType: Int = ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
    ) {
        startNowBar(
            notification = buildNowBarNotification(config, card),
            notificationId = config.notificationId,
            foregroundServiceType = foregroundServiceType
        )
    }

    protected fun updateNowBar(notification: Notification) {
        SafeNotificationPoster.notify(
            context = this,
            notificationManager = NotificationManagerCompat.from(this),
            notificationId = currentNotificationId,
            notification = notification
        )
    }

    protected fun updateNowBar(config: NowBarConfig, card: NowBarCard) {
        updateNowBar(buildNowBarNotification(config, card))
    }

    /**
     * Stop foreground state but keep the notification alive.
     */
    protected fun stopNowBar() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
    }

    /**
     * Remove the notification completely.
     */
    protected fun dismissNowBar() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        NotificationManagerCompat.from(this).cancel(currentNotificationId)
    }

    abstract fun onCreateCard(): NowBarCard

    abstract fun onUpdateCard(): NowBarCard
}
