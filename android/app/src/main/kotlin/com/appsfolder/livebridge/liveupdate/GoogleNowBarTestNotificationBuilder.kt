package com.kakao.taxi.liveupdate

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kakao.taxi.MainActivity
import com.kakao.taxi.R
import java.util.ArrayList

internal class GoogleNowBarTestNotificationBuilder(private val context: Context) {
    fun postSports(): Boolean {
        return postFixture(SPORTS)
    }

    fun postFinance(): Boolean {
        return postFixture(FINANCE)
    }

    fun cancelAll() {
        val manager = NotificationManagerCompat.from(context)
        TEST_NOTIFICATION_IDS.forEach(manager::cancel)
    }

    @SuppressLint("MissingPermission")
    private fun postFixture(fixture: Fixture): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) {
            return false
        }

        ensureChannel(fixture)
        manager.notify(fixture.summaryId, buildSummary(fixture))
        manager.notify(fixture.childId, buildChild(fixture))
        return true
    }

    private fun ensureChannel(fixture: Fixture) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(fixture.channelId) != null) {
            return
        }

        val channel = NotificationChannel(
            fixture.channelId,
            fixture.channelName,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Google Now Bar test fixture"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildSummary(fixture: Fixture): Notification {
        return NotificationCompat.Builder(context, fixture.channelId)
            .setSmallIcon(R.drawable.ic_stat_liveupdate)
            .setContentTitle(fixture.remoteAppName)
            .setContentText(fixture.summaryText)
            .setSubText(fixture.substituteName)
            .setGroup(fixture.groupKey)
            .setGroupSummary(true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setDefaults(0)
            .setColor(Color.TRANSPARENT)
            .setWhen(fixture.whenMs)
            .setShowWhen(true)
            .setTimeoutAfter(TIMEOUT_MS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setRequestPromotedOngoing(true)
            .addExtras(buildSummaryExtras(fixture))
            .build()
    }

    private fun buildChild(fixture: Fixture): Notification {
        val contentIntent = contentPendingIntent(fixture.childId)
        return NotificationCompat.Builder(context, fixture.channelId)
            .setSmallIcon(R.drawable.ic_stat_liveupdate)
            .setContentTitle(fixture.title)
            .setContentText(fixture.secondaryInfo)
            .setSubText(fixture.substituteName)
            .setContentIntent(contentIntent)
            .setGroup(fixture.groupKey)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setDefaults(0)
            .setColor(Color.TRANSPARENT)
            .setWhen(fixture.whenMs)
            .setShowWhen(true)
            .setTimeoutAfter(TIMEOUT_MS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setShortCriticalText(fixture.chipText)
            .setRequestPromotedOngoing(true)
            .addExtras(buildChildExtras(fixture, contentIntent))
            .build()
    }

    private fun buildSummaryExtras(fixture: Fixture): Bundle {
        return Bundle().apply {
            putCharSequence(Notification.EXTRA_TITLE, fixture.title)
            putBoolean(KEY_REDUCED_IMAGES, true)
            putParcelable(KEY_AOD_REMOTE_APP_PENDING_INTENT, contentPendingIntent(fixture.summaryId))
            putParcelable(KEY_AOD_REMOTE_APP_ICON, appIcon())
            putCharSequence(KEY_AOD_REMOTE_APP_NAME, fixture.remoteAppName)
            putString(KEY_SUBSTITUTE_NAME, fixture.substituteName)
            putInt(KEY_STYLE, STYLE_DEFAULT)
            putBoolean(KEY_SHOW_WHEN, true)
        }
    }

    private fun buildChildExtras(
        fixture: Fixture,
        contentIntent: PendingIntent
    ): Bundle {
        val appIcon = appIcon()
        val expandedView = buildExpandedRemoteView(fixture)
        val nowBarView = buildNowBarRemoteView(fixture)

        return Bundle().apply {
            putParcelable(KEY_AOD_REMOTE_APP_PENDING_INTENT, contentIntent)
            putCharSequence(Notification.EXTRA_TITLE, fixture.title)
            putBoolean(KEY_REDUCED_IMAGES, true)
            putCharSequence(KEY_RAW_PRIMARY_INFO, fixture.primaryInfo)
            putCharSequence(KEY_PRIMARY_INFO, fixture.primaryInfo)
            putCharSequence(KEY_RAW_SECONDARY_INFO, fixture.secondaryInfo)
            putCharSequence(KEY_SECONDARY_INFO, fixture.secondaryInfo)
            putCharSequence(KEY_NOWBAR_PRIMARY_INFO, fixture.nowBarPrimaryInfo)
            putCharSequence(KEY_NOWBAR_SECONDARY_INFO, fixture.nowBarSecondaryInfo)
            fixture.chipExpandedText?.let {
                putCharSequence(KEY_CHIP_EXPANDED_TEXT, it)
            }
            putString(Notification.EXTRA_TEMPLATE, TEMPLATE_ONGOING_ACTIVITY_STYLE)
            putParcelable(KEY_AOD_REMOTE_APP_ICON, appIcon)
            putCharSequence(KEY_AOD_REMOTE_APP_NAME, fixture.remoteAppName)
            putBoolean(KEY_SHOW, true)
            putParcelable(KEY_RAW_CHIP_ICON, appIcon)
            putParcelable(KEY_CHIP_ICON, appIcon)
            putBoolean(KEY_SHOW_WHEN, true)
            putBoolean(KEY_RAW_CHRONOMETER_COUNTDOWN, false)
            putString(KEY_RAW_CHRONOMETER_FORMAT, "")
            putLong(KEY_RAW_CHRONOMETER_BASE, 0L)
            putFloat(KEY_RAW_CHRONOMETER_SPEED, 0f)
            putBoolean(KEY_RAW_CHRONOMETER_START, false)
            putParcelable(KEY_EXPANDED_REMOTE_VIEW, expandedView)
            putParcelable(KEY_NOWBAR_REMOTE_VIEW, nowBarView)
            putParcelable(KEY_CHRONOMETER_REMOTE_VIEW, nowBarView)
            putParcelable(KEY_CHIP_EXPANDED_VIEW, nowBarView)
            putInt(KEY_RAW_PRIMARY_ACTION, 0)
            putInt(KEY_CARD_BACKGROUND, 0)
            putInt(KEY_CHIP_BACKGROUND, 0)
            putInt(KEY_CHIP_BG_COLOR, fixture.chipBgColor)
            putInt(KEY_ACTION_TYPE, ACTION_TYPE_BUTTON_TEXT)
            putInt(KEY_ACTION_PRIMARY_SET, ACTION_PRIMARY_SET)
            putBoolean(KEY_SHOW_SMALL_ICON, true)
            putString(KEY_CHRONOMETER_REMOTE_VIEW_TAG, fixture.remoteViewTag)
            putInt(KEY_CHRONOMETER_REMOTE_VIEW_POSITION, REMOTE_VIEW_POSITION)
            putInt(KEY_NOWBAR_CHRONOMETER_POSITION, REMOTE_VIEW_POSITION)
            putIntegerArrayList(KEY_ACTION_BG_COLORS, ArrayList())
            putInt(KEY_NOWBAR_EXPANDABLE_TYPE, 0)
            putString(KEY_PDE_NOTI_PKG, fixture.pdePackageName)
            putInt(KEY_PDE_NOTI_ID, fixture.childId)
            putLong(KEY_PDE_ENQUEUED_TIME_MS, fixture.whenMs)
        }
    }

    private fun buildExpandedRemoteView(fixture: Fixture): RemoteViews {
        return RemoteViews(context.packageName, R.layout.notification_google_nowbar_expanded).apply {
            setTextViewText(R.id.google_nowbar_title, fixture.title)
            setTextViewText(R.id.google_nowbar_primary, fixture.nowBarPrimaryInfo)
            setTextViewText(R.id.google_nowbar_secondary, fixture.secondaryInfo)
            setTextViewText(R.id.google_nowbar_badge, fixture.remoteAppName)
        }
    }

    private fun buildNowBarRemoteView(fixture: Fixture): RemoteViews {
        return RemoteViews(context.packageName, R.layout.notification_google_nowbar_nowbar).apply {
            setTextViewText(R.id.google_nowbar_title, fixture.nowBarPrimaryInfo)
            setTextViewText(R.id.google_nowbar_secondary, fixture.nowBarSecondaryInfo)
        }
    }

    private fun appIcon(): Icon {
        return Icon.createWithResource(context, R.drawable.ic_stat_liveupdate)
    }

    private fun contentPendingIntent(requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_BASE + requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private data class Fixture(
        val summaryId: Int,
        val childId: Int,
        val channelId: String,
        val channelName: String,
        val groupKey: String,
        val substituteName: String,
        val remoteAppName: String,
        val title: String,
        val primaryInfo: String,
        val secondaryInfo: String,
        val nowBarPrimaryInfo: String,
        val nowBarSecondaryInfo: String,
        val chipExpandedText: String?,
        val chipText: String,
        val chipBgColor: Int,
        val summaryText: String,
        val pdePackageName: String,
        val remoteViewTag: String,
        val whenMs: Long
    )

    companion object {
        private const val SPORTS_SUMMARY_ID = 1000
        private const val SPORTS_CHILD_ID = 1123
        private const val FINANCE_SUMMARY_ID = 3000
        private const val FINANCE_CHILD_ID = 3115
        private const val REQUEST_CODE_BASE = 65000
        private const val TIMEOUT_MS = 72L * 60L * 60L * 1000L

        private const val TEMPLATE_ONGOING_ACTIVITY_STYLE =
            "android.app.Notification\$OngoingActivityStyle"
        private const val ONGOING_PREFIX = "android.ongoingActivityNoti."
        private const val KEY_AOD_REMOTE_APP_PENDING_INTENT =
            "${ONGOING_PREFIX}aodRemoteAppPendingIntent"
        private const val KEY_AOD_REMOTE_APP_ICON = "${ONGOING_PREFIX}aodRemoteAppIcon"
        private const val KEY_AOD_REMOTE_APP_NAME = "${ONGOING_PREFIX}aodRemoteAppName"
        private const val KEY_PRIMARY_INFO = "${ONGOING_PREFIX}primaryInfo"
        private const val KEY_SECONDARY_INFO = "${ONGOING_PREFIX}secondaryInfo"
        private const val KEY_NOWBAR_PRIMARY_INFO = "${ONGOING_PREFIX}nowbarPrimaryInfo"
        private const val KEY_NOWBAR_SECONDARY_INFO = "${ONGOING_PREFIX}nowbarSecondaryInfo"
        private const val KEY_CHIP_EXPANDED_TEXT = "${ONGOING_PREFIX}chipExpandedText"
        private const val KEY_CHIP_EXPANDED_VIEW = "${ONGOING_PREFIX}chipExpandedView"
        private const val KEY_EXPANDED_REMOTE_VIEW = "${ONGOING_PREFIX}expandedRemoteView"
        private const val KEY_NOWBAR_REMOTE_VIEW = "${ONGOING_PREFIX}nowbarRemoteView"
        private const val KEY_CHIP_BG_COLOR = "${ONGOING_PREFIX}chipBgColor"
        private const val KEY_CHIP_ICON = "${ONGOING_PREFIX}chipIcon"
        private const val KEY_ACTION_TYPE = "${ONGOING_PREFIX}actionType"
        private const val KEY_ACTION_PRIMARY_SET = "${ONGOING_PREFIX}actionPrimarySet"
        private const val KEY_CHRONOMETER_REMOTE_VIEW = "${ONGOING_PREFIX}chronometerRemoteView"
        private const val KEY_CHRONOMETER_REMOTE_VIEW_TAG =
            "${ONGOING_PREFIX}chronometerRemoteViewTag"
        private const val KEY_CHRONOMETER_REMOTE_VIEW_POSITION =
            "${ONGOING_PREFIX}chronometerRemoteViewPosition"
        private const val KEY_NOWBAR_CHRONOMETER_POSITION =
            "${ONGOING_PREFIX}nowbarChronometerPosition"
        private const val KEY_SHOW = "${ONGOING_PREFIX}show"
        private const val KEY_STYLE = "${ONGOING_PREFIX}style"
        private const val KEY_SHOW_SMALL_ICON = "android.showSmallIcon"
        private const val KEY_REDUCED_IMAGES = "android.reduced.images"
        private const val KEY_SHOW_WHEN = "android.showWhen"
        private const val KEY_SUBSTITUTE_NAME = "android.substName"
        private const val KEY_RAW_PRIMARY_INFO = "android.ongoingActivityPrimaryInfo"
        private const val KEY_RAW_SECONDARY_INFO = "android.ongoingActivitySecondaryInfo"
        private const val KEY_RAW_PRIMARY_ACTION = "android.ongoingActivityPrimaryAction"
        private const val KEY_RAW_CHIP_ICON = "android.ongoingActivityChipIcon"
        private const val KEY_RAW_CHRONOMETER_FORMAT = "android.ongoingActivityChronometerFormat"
        private const val KEY_RAW_CHRONOMETER_COUNTDOWN =
            "android.ongoingActivityChronometerCountdown"
        private const val KEY_RAW_CHRONOMETER_BASE = "android.ongoingActivityChronometerBase"
        private const val KEY_RAW_CHRONOMETER_SPEED = "android.ongoingActivityChronometerSpeed"
        private const val KEY_RAW_CHRONOMETER_START = "android.ongoingActivityChronometerStart"
        private const val KEY_CARD_BACKGROUND = "android.ongoingActivityCardBackground"
        private const val KEY_CHIP_BACKGROUND = "android.ongoingActivityChipBackground"
        private const val KEY_ACTION_BG_COLORS = "android.ongoingActivityActionBgColors"
        private const val KEY_NOWBAR_EXPANDABLE_TYPE = "android.ongoingActivityNowBarExpandableType"
        private const val KEY_PDE_NOTI_PKG = "pde_noti_pkg"
        private const val KEY_PDE_NOTI_ID = "pde_noti_id"
        private const val KEY_PDE_ENQUEUED_TIME_MS = "pde_enqueued_time_ms"
        private const val STYLE_DEFAULT = 1
        private const val ACTION_TYPE_BUTTON_TEXT = 1
        private const val ACTION_PRIMARY_SET = 1
        private const val REMOTE_VIEW_POSITION = 1

        private val TEST_NOTIFICATION_IDS = intArrayOf(
            SPORTS_SUMMARY_ID,
            SPORTS_CHILD_ID,
            FINANCE_SUMMARY_ID,
            FINANCE_CHILD_ID
        )

        private val SPORTS = Fixture(
            summaryId = SPORTS_SUMMARY_ID,
            childId = SPORTS_CHILD_ID,
            channelId = "google_sports_nowbar_ongoing_channel",
            channelName = "NowbarGoogleSports",
            groupKey = "google_sports_nowbar_group_key",
            substituteName = "Sports from Google",
            remoteAppName = "Sports from Google",
            title = "Arsenal 2-1 Chelsea",
            primaryInfo = "ARS 2 - 1 CHE",
            secondaryInfo = "Premier League 78' - Chelsea attacking",
            nowBarPrimaryInfo = "Arsenal leads Chelsea 2-1",
            nowBarSecondaryInfo = "Premier League - 78' - Chelsea attacking",
            chipExpandedText = null,
            chipText = "ARS 2-1",
            chipBgColor = -13736492,
            summaryText = "Google Sports live score",
            pdePackageName = "com.google.android.googlequicksearchbox",
            remoteViewTag = "google_sports_live_score",
            whenMs = 1778878475522L
        )

        private val FINANCE = Fixture(
            summaryId = FINANCE_SUMMARY_ID,
            childId = FINANCE_CHILD_ID,
            channelId = "google_finance_nowbar_ongoing_channel",
            channelName = "NowbarGoogleFinance",
            groupKey = "google_finance_nowbar_group_key",
            substituteName = "Google Finance",
            remoteAppName = "Google Finance",
            title = "Google Finance",
            primaryInfo = "",
            secondaryInfo = "Alphabet Inc. is up 1.42% today while major indexes stay mixed before close.",
            nowBarPrimaryInfo = "GOOGL 175.24 +1.42%",
            nowBarSecondaryInfo = "NASDAQ - live market update",
            chipExpandedText = "GOOG",
            chipText = "GOOG",
            chipBgColor = -12961222,
            summaryText = "Google Finance market update",
            pdePackageName = "com.google.android.googlequicksearchbox",
            remoteViewTag = "google_finance_market_update",
            whenMs = 1778878770983L
        )
    }
}
