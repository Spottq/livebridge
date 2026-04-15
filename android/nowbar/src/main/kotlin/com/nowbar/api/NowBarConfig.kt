package com.nowbar.api

import android.app.NotificationManager
import com.nowbar.api.fallback.FallbackStrategy

/**
 * Small configuration object for the module.
 *
 * Keep it boring:
 * - one channel,
 * - one notification id,
 * - one rendering policy.
 */
data class NowBarConfig(
    val channelId: String,
    val channelName: String,
    val channelDescription: String = "",
    val channelImportance: Int = NotificationManager.IMPORTANCE_LOW,
    val notificationId: Int = DEFAULT_NOTIFICATION_ID,
    val fallbackStrategy: FallbackStrategy = FallbackStrategy.AUTO,
    val samsungStyle: Int = STYLE_BOTH,
    val requestPromotedOngoing: Boolean = true,
    val showSmallIcon: Boolean = true,
    val actionPrimarySet: Int = 1
) {
    companion object {
        const val DEFAULT_NOTIFICATION_ID = 200
        const val STYLE_NOTIFICATION_ONLY = 0
        const val STYLE_BOTH = 1
    }
}
