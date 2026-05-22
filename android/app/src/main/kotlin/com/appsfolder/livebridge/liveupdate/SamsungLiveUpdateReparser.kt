package com.kakao.taxi.liveupdate

import android.app.Notification
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import java.util.Locale

internal class SamsungLiveUpdateReparser(private val context: Context) {
    fun applyNowBarBridge(
        builder: NotificationCompat.Builder,
        source: Notification,
        sourcePackageName: String,
        primaryText: String,
        secondaryText: String,
        nowBarPrimaryText: String,
        nowBarSecondaryText: String?,
        chipText: String?,
        chipIcon: IconCompat?,
        nowBarIcon: IconCompat?,
        rightIcon: IconCompat?,
        suppressChipExpandedText: Boolean = false,
        suppressSourceRemoteViews: Boolean = false,
        suppressSourceNowBarRemoteView: Boolean = false,
        hasProgress: Boolean,
        progressValue: Int,
        progressMax: Int,
        showSecondaryInNowBar: Boolean = true,
        preferCompactNowBarRemoteView: Boolean = false,
        disableNowBarRemoteView: Boolean = false,
        disableMiniRemoteView: Boolean = false,
        keepCollapsedRemoteView: Boolean = false,
        preferExpandedRemoteBody: Boolean = false,
        reuseNotificationRemoteViews: Boolean = true,
        showSmallIcon: Boolean = true,
        allowNowBarProgress: Boolean = true,
        lockscreenOnly: Boolean = false
    ) {
        val normalizedPrimary = primaryText.trim()
        if (normalizedPrimary.isEmpty()) {
            return
        }
        val normalizedSecondary = secondaryText.trim()
        val normalizedNowBarPrimary = nowBarPrimaryText.trim().ifEmpty { normalizedPrimary }
        val normalizedNowBarSecondary = nowBarSecondaryText
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        val normalizedChipText = chipText?.trim()?.takeIf { it.isNotEmpty() } ?: normalizedPrimary

        val nowBarRemoteView =
            if (suppressSourceNowBarRemoteView || disableMiniRemoteView || disableNowBarRemoteView) {
                null
            } else {
                resolveRemoteView(source, preferCompactNowBarRemoteView)
            }
        val hasNowBarRemoteView = nowBarRemoteView != null
        if (reuseNotificationRemoteViews && !suppressSourceRemoteViews) {
            applyRemoteViewsIfPresent(
                builder = builder,
                source = source,
                keepCollapsedRemoteView = keepCollapsedRemoteView,
                preferExpandedRemoteBody = preferExpandedRemoteBody
            )
        }

        val extras = Bundle().apply {
            putInt(KEY_STYLE, if (lockscreenOnly) STYLE_NOW_BAR_ONLY else STYLE_DEFAULT)
            putCharSequence(KEY_PRIMARY_INFO, normalizedPrimary)
            // Avoid duplicated bottom subtitle when custom RemoteViews is used.
            if (showSecondaryInNowBar &&
                normalizedSecondary.isNotEmpty() &&
                !hasNowBarRemoteView
            ) {
                putCharSequence(KEY_SECONDARY_INFO, normalizedSecondary)
            }
            if (!suppressChipExpandedText) {
                putCharSequence(KEY_CHIP_EXPANDED_TEXT, normalizedChipText)
            }
            putCharSequence(KEY_NOWBAR_PRIMARY_INFO, normalizedNowBarPrimary)
            if (showSecondaryInNowBar) {
                normalizedNowBarSecondary?.let {
                    putCharSequence(KEY_NOWBAR_SECONDARY_INFO, it)
                }
            }
            putInt(
                KEY_CHIP_BG_COLOR,
                SamsungLiveUpdateReparser.resolveChipBackgroundColor(source)
            )
            putBoolean(KEY_SHOW_SMALL_ICON, showSmallIcon)
        }

        val frameworkIcon = runCatching { chipIcon?.toIcon(context) }.getOrNull()
        frameworkIcon?.let { extras.putParcelable(KEY_CHIP_ICON, it) }
        val frameworkNowBarIcon = if (disableMiniRemoteView) {
            null
        } else {
            runCatching { nowBarIcon?.toIcon(context) }.getOrNull()
        }
        frameworkNowBarIcon?.let { icon ->
            extras.putParcelable(KEY_NOWBAR_ICON, icon)
        }
        val frameworkRightIcon = if (disableMiniRemoteView) {
            null
        } else {
            runCatching { rightIcon?.toIcon(context) }.getOrNull()
        }
        frameworkRightIcon?.let { icon ->
            extras.putParcelable(KEY_SECOND_ICON, icon)
        }

        if (source.actions?.isNotEmpty() == true) {
            extras.putInt(KEY_ACTION_TYPE, 1)
            extras.putInt(KEY_ACTION_PRIMARY_SET, 0)
        }

        // Do not expose progress style in collapsed Samsung chip/Now Bar.
        if (allowNowBarProgress && hasProgress && progressMax > 0 && !hasNowBarRemoteView) {
            extras.putInt(KEY_PROGRESS, progressValue.coerceIn(0, progressMax))
            extras.putInt(KEY_PROGRESS_MAX, progressMax.coerceAtLeast(1))
        }

        nowBarRemoteView?.let { view ->
            val sourceExtras = source.extras
            val remoteViewPosition = parseInt(sourceExtras.get(KEY_REMOTE_VIEW_POSITION)) ?: 1
            val nowBarChronometerPosition =
                parseInt(sourceExtras.get(KEY_NOWBAR_CHRONOMETER_POSITION)) ?: remoteViewPosition
            val remoteTag = sourceExtras.getString(KEY_REMOTE_VIEW_TAG)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: "livebridge_remote_${sourcePackageName.replace('.', '_')}"

            extras.putParcelable(KEY_REMOTE_VIEW, view)
            extras.putInt(KEY_REMOTE_VIEW_POSITION, remoteViewPosition)
            extras.putString(KEY_REMOTE_VIEW_TAG, remoteTag)
            extras.putInt(KEY_NOWBAR_CHRONOMETER_POSITION, nowBarChronometerPosition)
        }

        builder.addExtras(extras)
    }

    private fun applyRemoteViewsIfPresent(
        builder: NotificationCompat.Builder,
        source: Notification,
        keepCollapsedRemoteView: Boolean,
        preferExpandedRemoteBody: Boolean
    ) {
        val collapsed = if (keepCollapsedRemoteView) {
            source.contentView ?: source.bigContentView ?: source.headsUpContentView
        } else {
            null
        }
        val expanded = if (preferExpandedRemoteBody) {
            source.bigContentView ?: source.contentView ?: source.headsUpContentView
        } else {
            source.bigContentView ?: source.contentView ?: source.headsUpContentView
        }
        val headsUp = source.headsUpContentView

        if (collapsed != null) {
            builder.setCustomContentView(collapsed)
            builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        }
        if (expanded != null) {
            builder.setCustomBigContentView(expanded)
            if (collapsed == null) {
                builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
            }
        }
        headsUp?.let(builder::setCustomHeadsUpContentView)
    }

    private fun resolveRemoteView(source: Notification): RemoteViews? {
        val extras = source.extras
        return (extras.get(KEY_REMOTE_VIEW) as? RemoteViews)
            ?: source.bigContentView
            ?: source.contentView
            ?: source.headsUpContentView
    }

    private fun resolveRemoteView(
        source: Notification,
        preferCompactNowBarRemoteView: Boolean
    ): RemoteViews? {
        if (!preferCompactNowBarRemoteView) {
            return resolveRemoteView(source)
        }
        val extras = source.extras
        return source.contentView
            ?: (extras.get(KEY_REMOTE_VIEW) as? RemoteViews)
            ?: source.bigContentView
            ?: source.headsUpContentView
    }

    private fun parseInt(value: Any?): Int? {
        return when (value) {
            is Int -> value
            is Long -> value.toInt()
            is Float -> value.toInt()
            is Double -> value.toInt()
            is String -> value.trim().toIntOrNull()
            is Number -> value.toInt()
            else -> null
        }
    }

    companion object {
        private const val ONGOING_PREFIX = "android.ongoingActivityNoti."
        private const val KEY_STYLE = "${ONGOING_PREFIX}style"
        private const val KEY_PRIMARY_INFO = "${ONGOING_PREFIX}primaryInfo"
        private const val KEY_SECONDARY_INFO = "${ONGOING_PREFIX}secondaryInfo"
        private const val KEY_CHIP_BG_COLOR = "${ONGOING_PREFIX}chipBgColor"
        private const val KEY_CHIP_ICON = "${ONGOING_PREFIX}chipIcon"
        private const val KEY_CHIP_EXPANDED_TEXT = "${ONGOING_PREFIX}chipExpandedText"
        private const val KEY_ACTION_TYPE = "${ONGOING_PREFIX}actionType"
        private const val KEY_ACTION_PRIMARY_SET = "${ONGOING_PREFIX}actionPrimarySet"
        private const val KEY_PROGRESS = "${ONGOING_PREFIX}progress"
        private const val KEY_PROGRESS_MAX = "${ONGOING_PREFIX}progressMax"
        private const val KEY_NOWBAR_PRIMARY_INFO = "${ONGOING_PREFIX}nowbarPrimaryInfo"
        private const val KEY_NOWBAR_SECONDARY_INFO = "${ONGOING_PREFIX}nowbarSecondaryInfo"
        private const val KEY_NOWBAR_ICON = "${ONGOING_PREFIX}nowbarIcon"
        private const val KEY_SECOND_ICON = "${ONGOING_PREFIX}secondIcon"
        private const val KEY_SHOW_SMALL_ICON = "android.showSmallIcon"
        private const val KEY_REMOTE_VIEW = "${ONGOING_PREFIX}chronometerRemoteView"
        private const val KEY_REMOTE_VIEW_POSITION = "${ONGOING_PREFIX}chronometerRemoteViewPosition"
        private const val KEY_REMOTE_VIEW_TAG = "${ONGOING_PREFIX}chronometerRemoteViewTag"
        private const val KEY_NOWBAR_CHRONOMETER_POSITION = "${ONGOING_PREFIX}nowbarChronometerPosition"

        private const val DEFAULT_CHIP_BG_COLOR = 0xFF0F766E.toInt()
        private const val STYLE_DEFAULT = 1
        private const val STYLE_NOW_BAR_ONLY = 2

        internal fun resolveChipBackgroundColor(source: Notification): Int {
            val sourceColor = source.color
            if (sourceColor != Color.TRANSPARENT) {
                return sourceColor
            }
            return DEFAULT_CHIP_BG_COLOR
        }

        fun isSamsungDevice(): Boolean {
            val manufacturer = (Build.MANUFACTURER ?: "").lowercase(Locale.ROOT)
            val brand = (Build.BRAND ?: "").lowercase(Locale.ROOT)
            return manufacturer.contains("samsung") || brand.contains("samsung")
        }
    }
}
