package com.kakao.taxi.liveupdate

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.kakao.taxi.MainActivity
import com.kakao.taxi.R

internal class FlashlightNotificationBuilder(
    private val context: Context
) {
    fun build(
        prefs: ConverterPrefs,
        capability: FlashlightCapability
    ): Notification {
        val title = notificationTitle()
        val levelIndex = prefs.getSmartFlashlightLevel().coerceIn(0, FlashlightController.FLASHLIGHT_LEVEL_COUNT - 1)
        val effectiveLevelIndex = if (capability.supportsFiveLevels) {
            levelIndex
        } else {
            FlashlightController.DEFAULT_LEVEL_INDEX
        }
        val secondaryText = secondaryText(capability)
        val chipText = chipText()
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val expandedView = buildExpandedRemoteViews(
            title = title,
            capability = capability,
            effectiveLevelIndex = effectiveLevelIndex
        )
        val chipIconCompat = IconCompat.createWithResource(
            context,
            R.drawable.ic_flashlight_system_notification
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_flashlight_system_notification)
            .setContentTitle(title)
            .setContentIntent(contentIntent)
            .setColor(DEFAULT_ICON_ACCENT_COLOR)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCustomBigContentView(expandedView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setRequestPromotedOngoing(true)

        if (!secondaryText.isNullOrEmpty()) {
            builder.setContentText(secondaryText)
        }
        if (!chipText.isNullOrEmpty()) {
            builder.setShortCriticalText(chipText)
        }

        if (SamsungLiveUpdateReparser.isSamsungDevice()) {
            builder.addExtras(
                buildSamsungExtras(
                    title = title,
                    chipText = chipText,
                    chipIcon = chipIconCompat,
                    chipBackgroundColor = DEFAULT_ICON_ACCENT_COLOR
                )
            )
        }

        return builder.build()
    }

    private fun buildExpandedRemoteViews(
        title: String,
        capability: FlashlightCapability,
        effectiveLevelIndex: Int
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.notification_flashlight_expanded).apply {
            setTextViewText(R.id.flashlight_title, title)
            setTextViewText(R.id.flashlight_action_button, disableButtonText())
            setOnClickPendingIntent(R.id.flashlight_action_button, disablePendingIntent())
            val warning = warningText(capability)
            if (warning.isNullOrEmpty()) {
                setViewVisibility(R.id.flashlight_warning, View.GONE)
            } else {
                setViewVisibility(R.id.flashlight_warning, View.VISIBLE)
                setTextViewText(R.id.flashlight_warning, warning)
            }
            applySliderState(
                remoteViews = this,
                capability = capability,
                effectiveLevelIndex = effectiveLevelIndex,
                interactive = capability.supportsFiveLevels
            )
        }
    }

    private fun applySliderState(
        remoteViews: RemoteViews,
        capability: FlashlightCapability,
        effectiveLevelIndex: Int,
        interactive: Boolean
    ) {
        val segmentSlotIds = intArrayOf(
            R.id.flashlight_segment_slot_1,
            R.id.flashlight_segment_slot_2,
            R.id.flashlight_segment_slot_3,
            R.id.flashlight_segment_slot_4,
            R.id.flashlight_segment_slot_5
        )
        val segmentImageIds = intArrayOf(
            R.id.flashlight_segment_1,
            R.id.flashlight_segment_2,
            R.id.flashlight_segment_3,
            R.id.flashlight_segment_4,
            R.id.flashlight_segment_5
        )
        val selectedIndex = effectiveLevelIndex.coerceIn(0, segmentImageIds.lastIndex)
        for (index in segmentImageIds.indices) {
            val isSelected = interactive && index == selectedIndex
            remoteViews.setImageViewResource(
                segmentImageIds[index],
                if (isSelected) {
                    R.drawable.flashlight_segment_active
                } else {
                    R.drawable.flashlight_segment_inactive
                }
            )
            if (interactive) {
                val pendingIntent = levelPendingIntent(index)
                remoteViews.setOnClickPendingIntent(
                    segmentSlotIds[index],
                    pendingIntent
                )
                remoteViews.setOnClickPendingIntent(
                    segmentImageIds[index],
                    pendingIntent
                )
            }
        }
        val unsupported = capability.available && !capability.supportsFiveLevels
        remoteViews.setViewVisibility(
            R.id.flashlight_disabled_overlay,
            if (unsupported) View.VISIBLE else View.GONE
        )
    }

    private fun buildSamsungExtras(
        title: String,
        chipText: String?,
        chipIcon: IconCompat,
        chipBackgroundColor: Int
    ): Bundle {
        val icon = runCatching { chipIcon.toIcon(context) }.getOrNull()
        return Bundle().apply {
            putInt(KEY_STYLE, STYLE_DEFAULT)
            putCharSequence(KEY_PRIMARY_INFO, title)
            putCharSequence(KEY_CHIP_EXPANDED_TEXT, chipText)
            putCharSequence(KEY_NOWBAR_PRIMARY_INFO, title)
            putInt(KEY_CHIP_BG_COLOR, chipBackgroundColor)
            putBoolean(KEY_SHOW_SMALL_ICON, false)
            icon?.let {
                putParcelable(KEY_CHIP_ICON, it)
                putParcelable(KEY_NOWBAR_ICON, it)
            }
        }
    }

    private fun levelPendingIntent(levelIndex: Int): PendingIntent {
        return PendingIntent.getService(
            context,
            levelIndex + 1,
            Intent(context, FlashlightForegroundService::class.java).apply {
                action = FlashlightForegroundService.ACTION_SET_LEVEL
                putExtra(FlashlightForegroundService.EXTRA_LEVEL_INDEX, levelIndex)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun disablePendingIntent(): PendingIntent {
        return PendingIntent.getService(
            context,
            REQUEST_CODE_DISABLE,
            Intent(context, FlashlightForegroundService::class.java).apply {
                action = FlashlightForegroundService.ACTION_DISABLE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun secondaryText(
        capability: FlashlightCapability
    ): String? {
        if (!capability.available) {
            return context.getString(R.string.flashlight_notification_unavailable)
        }
        if (!capability.supportsFiveLevels) {
            return context.getString(R.string.flashlight_notification_levels_unavailable)
        }
        return null
    }

    private fun chipText(): String {
        return context.getString(R.string.flashlight_chip_text)
    }

    private fun warningText(capability: FlashlightCapability): String? {
        if (!capability.available) {
            return context.getString(R.string.flashlight_warning_unavailable)
        }
        if (capability.supportsFiveLevels) {
            return null
        }
        return context.getString(R.string.flashlight_warning_levels_unavailable)
    }

    private fun notificationTitle(): String {
        return context.getString(R.string.flashlight_notification_title)
    }

    private fun disableButtonText(): String {
        return context.getString(R.string.flashlight_disable_button)
    }

    companion object {
        const val CHANNEL_ID = "livebridge_flashlight_nowbar"
        const val NOTIFICATION_ID = 41241

        private const val ONGOING_PREFIX = "android.ongoingActivityNoti."
        private const val KEY_STYLE = "${ONGOING_PREFIX}style"
        private const val KEY_PRIMARY_INFO = "${ONGOING_PREFIX}primaryInfo"
        private const val KEY_CHIP_BG_COLOR = "${ONGOING_PREFIX}chipBgColor"
        private const val KEY_CHIP_ICON = "${ONGOING_PREFIX}chipIcon"
        private const val KEY_CHIP_EXPANDED_TEXT = "${ONGOING_PREFIX}chipExpandedText"
        private const val KEY_NOWBAR_ICON = "${ONGOING_PREFIX}nowbarIcon"
        private const val KEY_NOWBAR_PRIMARY_INFO = "${ONGOING_PREFIX}nowbarPrimaryInfo"
        private const val KEY_SHOW_SMALL_ICON = "android.showSmallIcon"
        private const val STYLE_DEFAULT = 1
        private const val DEFAULT_ICON_ACCENT_COLOR = 0xFF387AFF.toInt()
        private const val REQUEST_CODE_DISABLE = 500
    }
}
