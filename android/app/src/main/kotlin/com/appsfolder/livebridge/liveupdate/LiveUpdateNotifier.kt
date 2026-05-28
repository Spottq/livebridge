package com.kakao.taxi.liveupdate

import android.app.Notification
import android.app.NotificationChannel
import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.icu.text.BreakIterator
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.kakao.taxi.R
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

object LiveUpdateNotifier {
    const val CHANNEL_ID = "livebridge_promoted_updates"
    private const val TWO_GIS_PACKAGE = "ru.dublgis.dgismobile"
    private const val YANDEX_MAPS_PACKAGE = "ru.yandex.yandexmaps"
    private const val YANGO_MAPS_PACKAGE = "com.yango.maps.android"
    private const val SAMSUNG_TRAY_ICON_SIZE = 48

    private const val CHANNEL_NAME = "LiveBridge Updates"
    private const val TAG = "LiveUpdateNotifier"
    private const val MEDIA_ONGOING_ACTIVITY_MARKER = "mediaongoingactivity"
    private const val MAX_MIRRORED_ACTIONS = 3
    private const val OTP_REPEAT_SUPPRESS_MS = 60_000L
    private const val OTP_AUTOCOPY_COPIED_SHOW_DELAY_MS = 1_000L
    private const val OTP_AUTOCOPY_COPIED_SHOW_DURATION_MS = 1_500L
    private const val AOSP_ISLAND_TEXT_LIMIT = 7
    private const val CALL_DURATION_REFRESH_MS = 1_000L
    private const val NOTIFICATION_CAPSULE_ID = 41242
    private const val CHARGING_INFO_ID = 41243
    private const val NOTIFICATION_CAPSULE_KEY_PREFIX = "notification_capsule:"
    private const val NOTIFICATION_CAPSULE_CHIP_COLOR = 0xFF5E5867.toInt()
    private const val CHARGING_INFO_FAST_COLOR = 0xFF65DE6B.toInt()
    private const val CHARGING_INFO_SUPER_FAST_COLOR = 0xFF24D9C7.toInt()
    private const val CHARGING_INFO_SUPER_FAST_2_COLOR = 0xFF35BDF7.toInt()
    private const val CHARGING_INFO_LOW_BATTERY_COLOR = 0xFFE53935.toInt()
    private const val BATTERY_EXTRA_MAX_CHARGING_CURRENT = "max_charging_current"
    private const val BATTERY_EXTRA_MAX_CHARGING_VOLTAGE = "max_charging_voltage"
    private const val CHARGING_INFO_APPEAR_DELAY_MS = 3_000L
    private const val LOW_BATTERY_THRESHOLD_PERCENT = 15
    private const val SYSTEMUI_PACKAGE = "com.android.systemui"
    private const val LOW_BATTERY_TAG = "low_battery"
    private const val LOW_BATTERY_CHANNEL_ID = "LOWBAT"
    private const val NOTIFICATION_CAPSULE_MAX_APP_NAMES = 25
    private const val NOTIFICATION_CAPSULE_MAX_MESSAGE_LINES = 2
    private const val NOTIFICATION_CAPSULE_SINGLE_LINE_GRAPHEME_LIMIT = 40
    private const val NOTIFICATION_CAPSULE_IMAGE_MAX_EDGE = 768
    private const val NOTIFICATION_EXTRA_PICTURE_ICON = "android.pictureIcon"
    private val KNOWN_NAVIGATION_PACKAGES = setOf(
        YANDEX_MAPS_PACKAGE,
        YANGO_MAPS_PACKAGE,
        "com.google.android.apps.maps",
        "com.waze"
    )
    private val NATIVE_IN_CALL_PACKAGES = setOf(
        "com.samsung.android.incallui",
        "com.samsung.android.dialer",
        "com.samsung.android.app.telephonyui",
        "com.android.incallui",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.google.android.apps.dialer"
    )
    private val WHATSAPP_CALL_MIRROR_BLOCKED_PACKAGES = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b"
    )
    private val DISCORD_PACKAGES = setOf(
        "com.discord",
        "com.discord.alpha",
        "com.discord.beta",
        "com.discord.canary",
        "com.hammerandchisel.discord"
    )
    private val CALL_MIRROR_EXCLUDED_PACKAGES = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b"
    )
    private val NOTIFICATION_CAPSULE_EXCLUDED_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.samsung.android.app.aodservice",
        "com.samsung.android.providers.context"
    )
    private val NAVIGATION_DISTANCE_PATTERN = Regex(
        "(?<!\\d)\\d{1,4}(?:[\\s.,]\\d{1,2})?\\s*(?:км|km|м|m|mi|ft|миль|фут)\\b",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val DELIVERY_ETA_INLINE_PATTERN = Regex(
        "(?<!\\d)(\\d{1,3})\\s*(мин(?:\\.|ут[\\p{L}]*)?|mins?|minutes?|ч(?:\\.|ас(?:а|ов)?)?|hrs?|hours?|h)(?=$|\\s|[,.;:!?)\\]])",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val DELIVERY_ETA_NUMBER_PATTERN = Regex("^\\d{1,3}$")
    private val DELIVERY_ETA_UNIT_PATTERN = Regex(
        "^(?:мин(?:\\.|ут[\\p{L}]*)?|mins?|minutes?|ч(?:\\.|ас(?:а|ов)?)?|hrs?|hours?|h)$",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val TEXT_PROGRESS_PERCENT_PATTERN = Regex("(?<!\\d)(\\d{1,3})\\s*%")
    private val LOW_BATTERY_TITLE_PERCENT_PATTERN = Regex("(?<!\\d)\\d{1,3}\\s*%")
    private val TEXT_PROGRESS_DISCOUNT_CONTEXT_PATTERN = Regex(
        "(скид|акци|промокод|промо|купон|распрод|кэшб[еэ]к|кешб[еэ]к|discount|promo|coupon|sale|cashback|off\\b|выгод|bonus|бонус|save|deal|special\\s+offer|limited\\s+time|дарим|подар)",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val TEXT_PROGRESS_OFFER_CONTEXT_PATTERN = Regex(
        "(при\\s+заказ|при\\s+покуп|minimum\\s+order|order\\s+from|в\\s+приложени\\S*\\s+акци)",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val TEXT_PROGRESS_MONEY_CONTEXT_PATTERN = Regex(
        "(\\d{2,7}\\s*(?:₽|руб\\.?|рубл(?:ей|я|ь)?|rur|usd|eur|\\$|€|kzt|тенге))",
        setOf(RegexOption.IGNORE_CASE)
    )
    private const val SMART_ISLAND_ANIMATION_MIN_DELAY_MS = 2_000L
    private const val SMART_ISLAND_ANIMATION_MAX_DELAY_MS = 3_000L
    private const val PROGRAMMATIC_MIRROR_CANCEL_GRACE_MS = 2_000L
    private const val FOOD_DELIVERY_AGGREGATE_ENTITY = "delivery"
    private const val LOCKSCREEN_CONTENT_HIDDEN_TEXT = "Content hidden"

    private val OTP_CODE_LENGTH = 4..8
    private val weatherHighLowPattern = Regex(
        """\bhighs?\s+([+\-−]?\d{1,3})\s*(?:°\s*(?:c|f|с|ф)?|℃|℉)?(?:\s*(?:to|-|–|—)\s*[+\-−]?\d{1,3}\s*(?:°\s*(?:c|f|с|ф)?|℃|℉)?)?[^\n]{0,40}?\blows?\s+([+\-−]?\d{1,3})\s*(?:°\s*(?:c|f|с|ф)?|℃|℉)?""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val FALLBACK_PRIVACY_REDACTION_PLACEHOLDERS = setOf(
        "sensitive content hidden",
        "content hidden",
        "unlock to view"
    )
    private val externalDeviceDebuggingPattern = Regex(
        """(\badb\b|android\s+debug\s+bridge|usb\s+debug(?:ging)?|wireless\s+debug(?:ging)?|\bdebug(?:ging|ger)?\b|developer\s+options?|usb[-\s]?отладк\p{L}*|беспровод\p{L}*\s+отладк\p{L}*|отладк\p{L}*|параметр\p{L}*\s+разработчик\p{L}*)""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val mediaProgressOnlyPattern = Regex("""^\d{1,3}\s*%$""")
    private val callDurationPattern = Regex("""(?<![\d:+-])(?:\d{1,2}:)?\d{1,2}:\d{2}(?!\d)""")
    private val callIncomingTextPattern = Regex(
        """(^|\s)(incoming|ringing|входящ\p{L}*|звонит|来电|來電)(\s|$)""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val callDialingTextPattern = Regex(
        """^\s*(calling|dialing|набор|вызываю|соединение)\b.*""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val callAnswerActionPattern = Regex(
        """(answer|accept|decline|reject|принять|ответить|отклонить|接听|拒绝|接聽|拒絕)""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val callEndActionPattern = Regex(
        """(^|\s)(end|end\s*call|hang\s*up|hangup|disconnect|leave|заверш\p{L}*|отбой|сбросить|отключ\p{L}*|покинуть|挂断|掛斷|结束|結束|encerrar|terminar)(\s|$)""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val callActiveTextPattern = Regex(
        """((?:ongoing|active).{0,40}\bcall\b|call\s+in\s+progress|on\s+call|in\s+call|(?:voice|голосов\p{L}*(?:\s+связ\p{L}*)?).{0,60}(?:connected|подключ\p{L}*)|разговор|ид[её]т\s+звонок|текущий\s+звонок|通话中|通話中)""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val callContextTextPattern = Regex(
        """(\bcall\b|\bvoice\s+(?:chat|channel|connection|connected)\b|голосов\p{L}*(?:\s+связ\p{L}*)?|звонок|вызов|разговор|通话|通話)""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val discordVoiceConnectedPattern = Regex(
        """(\bvoice\s+(?:connected|connection|channel)\b|голосов\p{L}*(?:\s+связ\p{L}*)?\s+подключ\p{L}*)""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val weatherCelsiusPattern =
        Regex("""(?:°\s*[cс](?!\p{L})|℃)""", setOf(RegexOption.IGNORE_CASE))
    private val weatherFahrenheitPattern =
        Regex("""(?:°\s*[fф](?!\p{L})|℉)""", setOf(RegexOption.IGNORE_CASE))
    private val explicitOrderEntityPrefixPattern = Regex(
        """(?:#|№|\border\b|\btrip\b|\bride\b|заказ|поездк|订单|訂單|行程)""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private const val MEDIA_SYMBOL_PLAY = "\u25B6\uFE0E"
    private const val MEDIA_SYMBOL_PAUSE = "\u2016\uFE0E"
    private const val MEDIA_SYMBOL_PREVIOUS = "\u23EE\uFE0E"
    private const val MEDIA_SYMBOL_NEXT = "\u23ED\uFE0E"
    private val transparentActionIcon by lazy {
        IconCompat.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    }
    private val progressColor = Color.valueOf(15f / 255f, 118f / 255f, 110f / 255f, 1f).toArgb()
    private const val SAMSUNG_EXTRA_CHIP_BG_COLOR = "android.ongoingActivityNoti.chipBgColor"
    private const val SAMSUNG_EXTRA_ACTION_BG_COLOR = "android.ongoingActivityNoti.actionBgColor"
    private const val SAMSUNG_EXTRA_ACTION_TYPE = "android.ongoingActivityNoti.actionType"
    private const val SAMSUNG_EXTRA_ACTION_PRIMARY_SET = "android.ongoingActivityNoti.actionPrimarySet"
    private const val NOWBAR_EXTRA_ACTION_ID = "com.nowbar.action.ID"
    private const val NOWBAR_EXTRA_ACTION_SEMANTIC = "com.nowbar.action.SEMANTIC"
    private const val NOWBAR_EXTRA_DELETE_INTENT = "com.nowbar.ongoing.DELETE_INTENT"
    private const val NOWBAR_EXTRA_DISMISSIBLE = "com.nowbar.ongoing.DISMISSIBLE"
    private const val NOWBAR_ACTION_SEMANTIC_DELETE = "DELETE"
    private const val NOTIFICATION_CAPSULE_CLEAR_ACTION_ID = "notification_capsule_clear"
    private const val CHARGING_INFO_POWER_SAVE_ACTION_ID = "charging_info_power_save"
    private val mainHandler = Handler(Looper.getMainLooper())
    private val appIconCacheLock = Any()
    private val appIconCache = mutableMapOf<String, AppIconAssets>()
    private val missingAppIconPackages = mutableSetOf<String>()

    private val stateLock = Any()
    private val sbnToAggregateKey = mutableMapOf<String, String>()
    private val aggregateStates = mutableMapOf<String, AggregateState>()
    private val sbnToOtpAggregateKey = mutableMapOf<String, String>()
    private val sbnToOtpSourceKey = mutableMapOf<String, String>()
    private val otpSourceStates = mutableMapOf<String, OtpSourceState>()
    private val otpAggregateStates = mutableMapOf<String, OtpAggregateState>()
    private val otpAnimationGenerations = mutableMapOf<String, Long>()
    private val smartAnimationGenerations = mutableMapOf<String, Long>()
    private val smartAnimationStates = mutableMapOf<String, SmartAnimationState>()
    private val callMirrorStates = mutableMapOf<String, CallMirrorState>()
    private val mirrorKeysByNotificationId = mutableMapOf<Int, String>()
    private val sourceSnapshotsByMirrorKey = mutableMapOf<String, StatusBarNotification>()
    private val userDismissedMirrorKeys = mutableSetOf<String>()
    private val programmaticMirrorCancelDeadlines = mutableMapOf<Int, Long>()
    private val notificationCapsuleIds = mutableSetOf<Int>()
    private var chargingInfoDelayScheduled = false
    private var chargingInfoDelayGeneration = 0L
    private var chargingInfoVisible = false
    private var retainedLowBatterySnapshot: RetainedLowBatterySnapshot? = null
    private var callMirrorGenerationCounter = 0L

    private data class AppIconAssets(
        val smallIcon: IconCompat?,
        val largeIconBitmap: Bitmap?
    )

    private data class NotificationCapsuleExpandedContent(
        val title: String,
        val body: String?,
        val image: Bitmap?
    )

    private data class NotificationCapsuleMessageLine(
        val conversationKey: String?,
        val text: String,
        val timestampMs: Long,
        val sourcePostTimeMs: Long,
        val sourceKey: String,
        val messageIndex: Int
    )

    private data class ChargingInfoSnapshot(
        val percent: Int,
        val title: String,
        val collapsedText: String,
        val expandedText: String,
        val iconRes: Int,
        val color: Int,
        val lowBattery: Boolean,
        val lowBatteryPowerSavingAction: LowBatteryPowerSavingAction? = null
    )

    private data class LowBatterySystemText(
        val title: String?,
        val collapsedText: String,
        val expandedText: String,
        val powerSavingAction: LowBatteryPowerSavingAction?
    )

    private data class RetainedLowBatterySnapshot(
        val titleTemplate: String?,
        val collapsedText: String,
        val expandedText: String,
        val powerSavingAction: LowBatteryPowerSavingAction?
    )

    private data class LowBatteryPowerSavingAction(
        val title: String,
        val pendingIntent: PendingIntent
    )

    private enum class ChargingInfoSpeed(
        val iconRes: Int,
        val color: Int
    ) {
        CHARGING(
            iconRes = R.drawable.ic_charging_bolt,
            color = CHARGING_INFO_FAST_COLOR
        ),
        FAST(
            iconRes = R.drawable.ic_charging_bolt,
            color = CHARGING_INFO_FAST_COLOR
        ),
        SUPER_FAST(
            iconRes = R.drawable.ic_charging_double_bolt,
            color = CHARGING_INFO_SUPER_FAST_COLOR
        ),
        SUPER_FAST_2(
            iconRes = R.drawable.ic_charging_double_bolt,
            color = CHARGING_INFO_SUPER_FAST_2_COLOR
        )
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        MirrorNotificationChannel.entries.forEach { channel ->
            ensureMirrorChannel(
                manager = manager,
                context = context,
                channel = channel
            )
        }
    }

    private fun ensureMirrorChannel(
        manager: NotificationManager,
        context: Context,
        channel: MirrorNotificationChannel
    ) {
        val lockscreenVisibility = mirrorChannelLockscreenVisibility(context)
        val current = manager.getNotificationChannel(channel.id)
        if (current == null) {
            manager.createNotificationChannel(createChannel(context, channel))
            return
        }

        val channelText = mirrorChannelText(context, channel)
        val shouldUpdate =
            current.name?.toString() != channelText.name ||
                    current.description != channelText.description ||
                    current.lockscreenVisibility != lockscreenVisibility
        if (!shouldUpdate) {
            return
        }

        current.name = channelText.name
        current.description = channelText.description
        current.lockscreenVisibility = lockscreenVisibility
        manager.createNotificationChannel(current)
    }

    private fun createChannel(
        context: Context,
        channel: MirrorNotificationChannel
    ): NotificationChannel {
        val channelText = mirrorChannelText(context, channel)
        return NotificationChannel(
            channel.id,
            channelText.name,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = channelText.description
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = mirrorChannelLockscreenVisibility(context)
        }
    }

    private fun mirrorChannelLockscreenVisibility(context: Context): Int {
        return if (ConverterPrefs(context).getHideLockscreenContentEnabled()) {
            Notification.VISIBILITY_PRIVATE
        } else {
            Notification.VISIBILITY_PUBLIC
        }
    }

    fun clearRuntimeState() {
        synchronized(stateLock) {
            sbnToAggregateKey.clear()
            aggregateStates.clear()
            sbnToOtpAggregateKey.clear()
            sbnToOtpSourceKey.clear()
            otpSourceStates.clear()
            otpAggregateStates.clear()
            otpAnimationGenerations.clear()
            smartAnimationGenerations.clear()
            smartAnimationStates.clear()
            callMirrorStates.clear()
            mirrorKeysByNotificationId.clear()
            sourceSnapshotsByMirrorKey.clear()
            userDismissedMirrorKeys.clear()
            programmaticMirrorCancelDeadlines.clear()
            notificationCapsuleIds.clear()
            chargingInfoDelayScheduled = false
            chargingInfoDelayGeneration += 1
            chargingInfoVisible = false
            retainedLowBatterySnapshot = null
        }
        synchronized(appIconCacheLock) {
            appIconCache.clear()
            missingAppIconPackages.clear()
        }
    }

    fun cancelAllMirrored(context: Context) {
        clearRuntimeState()
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val manager = NotificationManagerCompat.from(context)
        notificationManager.activeNotifications
            .filter { isMirrorNotificationChannel(it.notification.channelId) }
            .forEach { statusBarNotification ->
                manager.cancel(statusBarNotification.id)
            }
    }

    fun refreshWeatherMirrors(context: Context, prefs: ConverterPrefs): Int {
        val candidates = synchronized(stateLock) {
            val weatherSources = aggregateStates
                .filterKeys { smartRuleIdFromAggregateKey(it) == "weather" }
                .values
                .flatMap { state -> state.sourcesBySbnKey.values.map { it.sbn } }
            (weatherSources + sourceSnapshotsByMirrorKey.values)
                .distinctBy { it.key }
        }
        var refreshed = 0
        candidates.forEach { sbn ->
            if (maybeMirror(context, prefs, sbn).mirrored) {
                refreshed += 1
            }
        }
        return refreshed
    }

    fun cancelWeatherMirrors(context: Context): Int {
        val manager = NotificationManagerCompat.from(context)
        val aggregateKeys = synchronized(stateLock) {
            aggregateStates.keys
                .filter { smartRuleIdFromAggregateKey(it) == "weather" }
                .toList()
        }
        if (aggregateKeys.isEmpty()) {
            return 0
        }

        val notificationIds = synchronized(stateLock) {
            aggregateKeys.map { aggregateKey ->
                val state = aggregateStates.remove(aggregateKey)
                state?.activeSbnKeys?.forEach { sbnKey ->
                    if (sbnToAggregateKey[sbnKey] == aggregateKey) {
                        sbnToAggregateKey.remove(sbnKey)
                    }
                }
                smartAnimationGenerations.remove(aggregateKey)
                smartAnimationStates.remove(aggregateKey)
                userDismissedMirrorKeys.remove(aggregateKey)
                sourceSnapshotsByMirrorKey.remove(aggregateKey)
                mirrorIdForKey(aggregateKey)
            }
        }
        notificationIds.forEach { cancelMirroredNotification(manager, it) }
        return notificationIds.size
    }

    fun cancelCallMirrors(context: Context): Int {
        val manager = NotificationManagerCompat.from(context)
        val stateNotificationIds = synchronized(stateLock) {
            val keys = callMirrorStates.keys.toList()
            callMirrorStates.clear()
            keys.map(::mirrorIdForKey)
        }
        val activeNotificationIds = runCatching {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return@runCatching emptyList()
            notificationManager.activeNotifications
                .filter { it.notification.channelId == MirrorNotificationChannel.CALLS.id }
                .map { it.id }
        }.getOrDefault(emptyList())
        val notificationIds = (stateNotificationIds + activeNotificationIds).distinct()
        notificationIds.forEach { notificationId ->
            cancelMirroredNotification(manager, notificationId)
        }
        return notificationIds.size
    }

    fun refreshNotificationCapsule(
        context: Context,
        prefs: ConverterPrefs,
        snapshots: Collection<StatusBarNotification>
    ): Int {
        if (
            !prefs.getSmartNotificationCapsuleEnabled() ||
            !prefs.getConverterEnabled() ||
            DeviceBlocker.isBlockedDevice()
        ) {
            cancelNotificationCapsule(context)
            return 0
        }

        val childGroupKeys = snapshots
            .asSequence()
            .filterNot { sbn -> sbn.packageName == context.packageName }
            .filterNot { sbn -> sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0 }
            .map { sbn -> sbn.groupKey }
            .filter { groupKey -> groupKey.isNotBlank() }
            .toSet()
        val candidates = snapshots
            .asSequence()
            .filter { sbn -> isNotificationCapsuleCandidate(context, sbn) }
            .distinctBy { sbn -> sbn.key }
            .toList()
        val sources = candidates
            .filter { sbn ->
                sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0 ||
                    sbn.groupKey !in childGroupKeys
            }
            .sortedByDescending { sbn -> notificationCapsuleRecencyTime(sbn) }
        if (sources.isEmpty()) {
            cancelNotificationCapsule(context)
            return 0
        }

        val visibleSources = sources
            .filterNot { sbn -> prefs.isNotificationCapsulePackageExcluded(sbn.packageName) }
        if (visibleSources.isEmpty()) {
            cancelNotificationCapsule(context)
            return 0
        }

        val totalCount = visibleSources.sumOf { sbn ->
            notificationCapsuleItemCount(sbn.notification)
        }
        ensureChannel(context)
        val manager = NotificationManagerCompat.from(context)
        val desiredIds = mutableSetOf<Int>()
        val appGroups = visibleSources
            .groupBy { sbn -> sbn.packageName.lowercase(Locale.ROOT) }
            .values
            .sortedByDescending { group ->
                group.maxOfOrNull { sbn -> notificationCapsuleRecencyTime(sbn) } ?: 0L
            }

        fun postGeneralCapsule(generalSources: List<StatusBarNotification>) {
            if (generalSources.isEmpty()) {
                return
            }
            desiredIds.add(NOTIFICATION_CAPSULE_ID)
            val count = generalSources.sumOf { sbn ->
                notificationCapsuleItemCount(sbn.notification)
            }
            val title = notificationCapsuleTitle(context, count)
            val appNames = generalSources
                .map { sbn -> notificationCapsuleAppLabel(context, sbn.packageName) }
                .distinct()
                .take(NOTIFICATION_CAPSULE_MAX_APP_NAMES)
                .joinToString(", ")
            val notification = buildNotificationCapsuleNotification(
                context = context,
                title = title,
                description = appNames,
                postTime = generalSources.maxOfOrNull { sbn ->
                    notificationCapsuleRecencyTime(sbn)
                }
                    ?: System.currentTimeMillis(),
                notificationId = NOTIFICATION_CAPSULE_ID,
                smallIcon = IconCompat.createWithResource(
                    context,
                    R.drawable.ic_notification_capsule
                ),
                largeIcon = null,
                clearSourceKeys = generalSources.map { sbn -> sbn.key },
                showClearAction = prefs.getNotificationCapsuleClearActionEnabled()
            )
            notifyNotificationCapsule(manager, NOTIFICATION_CAPSULE_ID, notification)
        }

        fun postAppCapsule(groupSources: List<StatusBarNotification>) {
            if (groupSources.isEmpty()) {
                return
            }
            val packageName = groupSources.first().packageName
            val notificationId = notificationCapsuleIdForPackage(packageName)
            desiredIds.add(notificationId)
            val count = groupSources.sumOf { sbn ->
                notificationCapsuleItemCount(sbn.notification)
            }
            val appName = notificationCapsuleAppLabel(context, packageName)
            val countText = notificationCapsuleTitle(context, count)
            val expandedContent = notificationCapsuleExpandedContent(
                context = context,
                sources = groupSources,
                fallbackTitle = appName
            )
            val appIconAssets = resolveAppIconAssets(context, packageName)
            val notification = buildNotificationCapsuleNotification(
                context = context,
                title = appName,
                description = countText,
                expandedTitle = expandedContent.title,
                expandedDescription = expandedContent.body ?: countText,
                expandedImage = expandedContent.image,
                postTime = groupSources.maxOfOrNull { sbn ->
                    notificationCapsuleRecencyTime(sbn)
                }
                    ?: System.currentTimeMillis(),
                notificationId = notificationId,
                smallIcon = appIconAssets?.smallIcon ?: IconCompat.createWithResource(
                    context,
                    R.drawable.ic_notification_capsule
                ),
                largeIcon = appIconAssets?.largeIconBitmap,
                contentIntent = notificationCapsuleSourceContentIntent(groupSources),
                clearSourceKeys = groupSources.map { sbn -> sbn.key },
                showClearAction = prefs.getNotificationCapsuleClearActionEnabled()
            )
            notifyNotificationCapsule(manager, notificationId, notification)
        }

        if (
            prefs.getNotificationCapsuleSmartEnabled() &&
            appGroups.size == 1
        ) {
            postAppCapsule(appGroups.first())
        } else if (
            !prefs.getNotificationCapsuleSmartEnabled() &&
            prefs.getNotificationCapsuleMode() == "per_app"
        ) {
            appGroups.forEach(::postAppCapsule)
        } else {
            postGeneralCapsule(visibleSources)
        }

        cancelStaleNotificationCapsules(context, desiredIds)
        return totalCount
    }

    fun cancelNotificationCapsule(context: Context) {
        val ids = mutableSetOf(NOTIFICATION_CAPSULE_ID)
        synchronized(stateLock) {
            ids.addAll(notificationCapsuleIds)
            notificationCapsuleIds.clear()
        }
        ids.addAll(activeNotificationCapsuleIds(context))
        val manager = NotificationManagerCompat.from(context)
        ids.forEach { notificationId ->
            runCatching {
                manager.cancel(notificationId)
            }.onFailure { error ->
                Log.e(TAG, "Failed to cancel notification capsule", error)
            }
        }
    }

    fun refreshChargingInfo(
        context: Context,
        prefs: ConverterPrefs,
        batteryIntent: Intent? = null,
        activeNotifications: Collection<StatusBarNotification> = emptyList(),
        delayAppearance: Boolean = true
    ): Boolean {
        if (
            !prefs.getSmartChargingInfoEnabled() ||
            !prefs.getConverterEnabled() ||
            DeviceBlocker.isBlockedDevice()
        ) {
            cancelChargingInfo(context)
            return false
        }

        val snapshot = resolveChargingInfoSnapshot(
            context = context,
            batteryIntent = batteryIntent ?: stickyBatteryIntent(context),
            activeNotifications = activeNotifications
        )
        if (snapshot == null) {
            cancelChargingInfo(context)
            return false
        }

        if (delayAppearance && !snapshot.lowBattery && !isChargingInfoActive(context)) {
            scheduleChargingInfoAppearance(context)
            return false
        }

        ensureChannel(context)
        val notification = buildChargingInfoNotification(context, snapshot)
        return runCatching {
            NotificationManagerCompat.from(context).notify(CHARGING_INFO_ID, notification)
            synchronized(stateLock) {
                chargingInfoDelayScheduled = false
                chargingInfoDelayGeneration += 1
                chargingInfoVisible = true
            }
        }.onFailure { error ->
            Log.e(TAG, "Failed to post charging info capsule", error)
        }.isSuccess
    }

    fun cancelChargingInfo(context: Context) {
        synchronized(stateLock) {
            chargingInfoDelayScheduled = false
            chargingInfoDelayGeneration += 1
            chargingInfoVisible = false
            retainedLowBatterySnapshot = null
        }
        runCatching {
            NotificationManagerCompat.from(context).cancel(CHARGING_INFO_ID)
        }.onFailure { error ->
            Log.e(TAG, "Failed to cancel charging info capsule", error)
        }
    }

    private fun scheduleChargingInfoAppearance(context: Context) {
        val appContext = context.applicationContext
        val generation = synchronized(stateLock) {
            if (chargingInfoDelayScheduled) {
                return
            }
            chargingInfoDelayScheduled = true
            chargingInfoDelayGeneration += 1
            chargingInfoDelayGeneration
        }
        mainHandler.postDelayed(
            {
                val shouldRun = synchronized(stateLock) {
                    chargingInfoDelayScheduled && chargingInfoDelayGeneration == generation
                }
                if (!shouldRun) {
                    return@postDelayed
                }
                synchronized(stateLock) {
                    chargingInfoDelayScheduled = false
                }
                refreshChargingInfo(
                    context = appContext,
                    prefs = ConverterPrefs(appContext),
                    batteryIntent = null,
                    delayAppearance = false
                )
            },
            CHARGING_INFO_APPEAR_DELAY_MS
        )
    }

    private fun isChargingInfoActive(context: Context): Boolean {
        synchronized(stateLock) {
            if (chargingInfoVisible) {
                return true
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return false
        return runCatching {
            notificationManager.activeNotifications.any { sbn ->
                sbn.packageName == context.packageName && sbn.id == CHARGING_INFO_ID
            }
        }.getOrDefault(false)
    }

    private fun buildChargingInfoNotification(
        context: Context,
        snapshot: ChargingInfoSnapshot
    ): Notification {
        val title = snapshot.title
        val collapsedText = snapshot.collapsedText
        val expandedText = snapshot.expandedText
        val icon = IconCompat.createWithResource(context, snapshot.iconRes)
        val builder = NotificationCompat.Builder(
            context,
            MirrorNotificationChannel.CHARGING_INFO.id
        )
            .setSmallIcon(snapshot.iconRes)
            .setContentTitle(title)
            .setContentText(collapsedText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setColor(snapshot.color)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setDefaults(0)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(title)

        if (snapshot.lowBattery && !isPowerSaveMode(context)) {
            snapshot.lowBatteryPowerSavingAction?.let { action ->
                builder.addAction(buildLowBatteryPowerSavingAction(context, action))
                builder.addExtras(buildChargingInfoActionExtras())
            }
        }

        notificationCapsuleContentIntent(context, CHARGING_INFO_ID)?.let(builder::setContentIntent)

        @Suppress("DEPRECATION")
        val bridgeSource = Notification().apply {
            color = snapshot.color
            extras = Bundle()
        }
        SamsungLiveUpdateReparser(context).applyNowBarBridge(
            builder = builder,
            source = bridgeSource,
            sourcePackageName = context.packageName,
            primaryText = title,
            secondaryText = expandedText,
            nowBarPrimaryText = title,
            nowBarSecondaryText = collapsedText,
            chipText = if (snapshot.lowBattery) expandedText else title,
            chipIcon = icon,
            nowBarIcon = icon,
            rightIcon = null,
            suppressSourceRemoteViews = true,
            suppressSourceNowBarRemoteView = true,
            hasProgress = false,
            progressValue = 0,
            progressMax = 0,
            showSecondaryInNowBar = collapsedText.isNotBlank(),
            disableNowBarRemoteView = true,
            reuseNotificationRemoteViews = false,
            lockscreenOnly = true
        )
        return SamsungOneUi7NowBarCompat.markEligible(builder.build())
    }

    private fun buildLowBatteryPowerSavingAction(
        context: Context,
        action: LowBatteryPowerSavingAction
    ): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            transparentActionIcon,
            action.title,
            action.pendingIntent
        ).addExtras(
            Bundle().apply {
                putString(NOWBAR_EXTRA_ACTION_ID, CHARGING_INFO_POWER_SAVE_ACTION_ID)
            }
        ).build()
    }

    private fun buildChargingInfoActionExtras(): Bundle {
        return Bundle().apply {
            putInt(SAMSUNG_EXTRA_ACTION_TYPE, 1)
            putInt(SAMSUNG_EXTRA_ACTION_PRIMARY_SET, 0)
        }
    }

    private fun isPowerSaveMode(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return false
        return powerManager.isPowerSaveMode
    }

    private fun chargingInfoExpandedText(
        speedLabel: String,
        remainingText: String
    ): String {
        return if (remainingText.isBlank()) {
            speedLabel
        } else {
            "$speedLabel\n$remainingText"
        }
    }

    private fun chargingInfoRemainingText(context: Context, remainingMinutes: Int?): String {
        return when {
            remainingMinutes == null -> chargingInfoChargingText(context)
            remainingMinutes <= 0 -> chargingInfoFullText(context)
            else -> chargingInfoUntilFullText(context, remainingMinutes)
        }
    }

    private fun chargingInfoSpeedLabel(context: Context, speed: ChargingInfoSpeed): String {
        return when (speed) {
            ChargingInfoSpeed.CHARGING -> chargingInfoChargingText(context)
            ChargingInfoSpeed.FAST -> when (appLocale(context)) {
                AppLocale.RU -> "Быстрая зарядка"
                AppLocale.TR -> "Hızlı şarj"
                AppLocale.PT_BR -> "Carregamento rápido"
                AppLocale.ZH_HANS -> "快速充电"
                AppLocale.ZH_HANT -> "快速充電"
                AppLocale.KO -> "고속 충전"
                AppLocale.EN -> "Fast charging"
            }
            ChargingInfoSpeed.SUPER_FAST -> when (appLocale(context)) {
                AppLocale.RU -> "Очень быстрая зарядка"
                AppLocale.TR -> "Süper hızlı şarj"
                AppLocale.PT_BR -> "Carregamento super-rápido"
                AppLocale.ZH_HANS -> "超快速充电"
                AppLocale.ZH_HANT -> "超快速充電"
                AppLocale.KO -> "초고속 충전"
                AppLocale.EN -> "Super fast charging"
            }
            ChargingInfoSpeed.SUPER_FAST_2 -> when (appLocale(context)) {
                AppLocale.RU -> "Очень быстрая зарядка 2.0"
                AppLocale.TR -> "Süper hızlı şarj 2.0"
                AppLocale.PT_BR -> "Carregamento super-rápido 2.0"
                AppLocale.ZH_HANS -> "超快速充电 2.0"
                AppLocale.ZH_HANT -> "超快速充電 2.0"
                AppLocale.KO -> "초고속 충전 2.0"
                AppLocale.EN -> "Super fast charging 2.0"
            }
        }
    }

    private fun chargingInfoChargingText(context: Context): String {
        return when (appLocale(context)) {
            AppLocale.RU -> "Зарядка"
            AppLocale.TR -> "Şarj oluyor"
            AppLocale.PT_BR -> "Carregando"
            AppLocale.ZH_HANS -> "正在充电"
            AppLocale.ZH_HANT -> "正在充電"
            AppLocale.KO -> "충전 중"
            AppLocale.EN -> "Charging"
        }
    }

    private fun chargingInfoFullText(context: Context): String {
        return when (appLocale(context)) {
            AppLocale.RU -> "Заряжено"
            AppLocale.TR -> "Dolu"
            AppLocale.PT_BR -> "Completo"
            AppLocale.ZH_HANS -> "已充满"
            AppLocale.ZH_HANT -> "已充滿"
            AppLocale.KO -> "완충됨"
            AppLocale.EN -> "Full"
        }
    }

    private fun chargingInfoUntilFullText(context: Context, remainingMinutes: Int): String {
        return when (appLocale(context)) {
            AppLocale.RU -> "$remainingMinutes мин до полного заряда"
            AppLocale.TR -> "Dolmaya $remainingMinutes dk"
            AppLocale.PT_BR -> "$remainingMinutes min até completar"
            AppLocale.ZH_HANS -> "距离充满还有 $remainingMinutes 分钟"
            AppLocale.ZH_HANT -> "距離充滿還有 $remainingMinutes 分鐘"
            AppLocale.KO -> "완충까지 ${remainingMinutes}분"
            AppLocale.EN -> "$remainingMinutes min until full"
        }
    }

    private fun resolveChargingInfoSnapshot(
        context: Context,
        batteryIntent: Intent?,
        activeNotifications: Collection<StatusBarNotification>
    ): ChargingInfoSnapshot? {
        batteryIntent ?: return null
        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            .takeIf { it > 0 }
            ?: 100
        if (level < 0) {
            return null
        }
        val percent = ((level * 100f) / scale).roundToInt().coerceIn(0, 100)
        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val isCharging =
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL ||
                plugged != 0
        if (isCharging) {
            clearRetainedLowBatterySnapshot()
            return chargingInfoSnapshot(
                context = context,
                batteryIntent = batteryIntent,
                percent = percent
            )
        }

        if (percent > LOW_BATTERY_THRESHOLD_PERCENT) {
            clearRetainedLowBatterySnapshot()
            return null
        }
        lowBatterySystemText(activeNotifications, percent)?.let { lowBatteryText ->
            rememberRetainedLowBatterySnapshot(lowBatteryText)
            return lowBatterySnapshot(percent, lowBatteryText)
        }
        return retainedLowBatterySnapshot()?.let { retained ->
            lowBatterySnapshot(percent, retained)
        }
    }

    private fun chargingInfoSnapshot(
        context: Context,
        batteryIntent: Intent,
        percent: Int
    ): ChargingInfoSnapshot {
        val remainingText = chargingInfoRemainingText(
            context = context,
            remainingMinutes = remainingChargingMinutes(context, batteryIntent, percent)
        )
        val speed = resolveChargingInfoSpeed(batteryIntent)
        val speedLabel = chargingInfoSpeedLabel(context, speed)
        return ChargingInfoSnapshot(
            percent = percent,
            title = "$percent%",
            collapsedText = remainingText,
            expandedText = chargingInfoExpandedText(speedLabel, remainingText),
            iconRes = speed.iconRes,
            color = speed.color,
            lowBattery = false
        )
    }

    private fun lowBatterySnapshot(
        percent: Int,
        lowBatteryText: LowBatterySystemText
    ): ChargingInfoSnapshot {
        return ChargingInfoSnapshot(
            percent = percent,
            title = lowBatteryTitleForPercent(lowBatteryText.title, percent),
            collapsedText = lowBatteryText.collapsedText,
            expandedText = lowBatteryText.expandedText,
            iconRes = R.drawable.ic_charging_bolt,
            color = CHARGING_INFO_LOW_BATTERY_COLOR,
            lowBattery = true,
            lowBatteryPowerSavingAction = lowBatteryText.powerSavingAction
        )
    }

    private fun lowBatterySnapshot(
        percent: Int,
        retained: RetainedLowBatterySnapshot
    ): ChargingInfoSnapshot {
        return ChargingInfoSnapshot(
            percent = percent,
            title = lowBatteryTitleForPercent(retained.titleTemplate, percent),
            collapsedText = retained.collapsedText,
            expandedText = retained.expandedText,
            iconRes = R.drawable.ic_charging_bolt,
            color = CHARGING_INFO_LOW_BATTERY_COLOR,
            lowBattery = true,
            lowBatteryPowerSavingAction = retained.powerSavingAction
        )
    }

    private fun rememberRetainedLowBatterySnapshot(lowBatteryText: LowBatterySystemText) {
        synchronized(stateLock) {
            retainedLowBatterySnapshot = RetainedLowBatterySnapshot(
                titleTemplate = lowBatteryText.title,
                collapsedText = lowBatteryText.collapsedText,
                expandedText = lowBatteryText.expandedText,
                powerSavingAction = lowBatteryText.powerSavingAction
            )
        }
    }

    private fun retainedLowBatterySnapshot(): RetainedLowBatterySnapshot? {
        return synchronized(stateLock) {
            retainedLowBatterySnapshot
        }
    }

    private fun clearRetainedLowBatterySnapshot() {
        synchronized(stateLock) {
            retainedLowBatterySnapshot = null
        }
    }

    private fun lowBatteryTitleForPercent(titleTemplate: String?, percent: Int): String {
        val fallback = lowBatteryFallbackTitle(percent)
        val title = titleTemplate?.trim()?.takeIf { it.isNotEmpty() } ?: return fallback
        val percentText = "$percent%"
        if (LOW_BATTERY_TITLE_PERCENT_PATTERN.containsMatchIn(title)) {
            return LOW_BATTERY_TITLE_PERCENT_PATTERN.replace(title, percentText)
        }
        return "$title $percentText"
    }

    private fun lowBatterySystemText(
        activeNotifications: Collection<StatusBarNotification>,
        percent: Int
    ): LowBatterySystemText? {
        return activeNotifications
            .asSequence()
            .filter(::isLowBatterySystemNotification)
            .sortedByDescending { sbn -> sbn.postTime }
            .firstNotNullOfOrNull { sbn -> parseLowBatterySystemText(sbn, percent) }
    }

    private fun isLowBatterySystemNotification(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName != SYSTEMUI_PACKAGE) {
            return false
        }
        if (sbn.tag == LOW_BATTERY_TAG) {
            return true
        }
        if (sbn.notification.channelId?.equals(LOW_BATTERY_CHANNEL_ID, ignoreCase = true) == true) {
            return true
        }
        val title = NotificationTextNormalizer.normalize(
            sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)
                ?: sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
        ) ?: return false
        return title.contains("battery power", ignoreCase = true) ||
            title.contains("low battery", ignoreCase = true)
    }

    private fun parseLowBatterySystemText(
        sbn: StatusBarNotification,
        percent: Int
    ): LowBatterySystemText? {
        val notification = sbn.notification
        val extras = notification.extras
        val title = NotificationTextNormalizer.normalize(
            extras.getCharSequence(Notification.EXTRA_TITLE)
                ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
        )?.takeIf { text -> text.contains("$percent%") || text.isNotBlank() }
        val body = lowBatteryBodyText(notification) ?: return null
        val actionText = notification.actions
            ?.firstNotNullOfOrNull { action ->
                NotificationTextNormalizer.normalize(action.title)
            }
        val (collapsedText, expandedText) = splitLowBatteryBodyText(body, actionText)
            ?: return null
        return LowBatterySystemText(
            title = title,
            collapsedText = collapsedText,
            expandedText = expandedText,
            powerSavingAction = lowBatteryPowerSavingAction(notification)
        )
    }

    private fun lowBatteryPowerSavingAction(
        notification: Notification
    ): LowBatteryPowerSavingAction? {
        val actions = notification.actions?.filter { action ->
            action.actionIntent != null &&
                !NotificationTextNormalizer.normalize(action.title).isNullOrBlank()
        }.orEmpty()
        if (actions.isEmpty()) {
            return null
        }
        val action = actions.firstOrNull { action ->
            val title = NotificationTextNormalizer.normalize(action.title)
                ?.lowercase(Locale.ROOT)
                .orEmpty()
            title.contains("power") ||
                title.contains("saving") ||
                title.contains("энерг") ||
                title.contains("эконом") ||
                title.contains("省电") ||
                title.contains("省電") ||
                title.contains("절전")
        } ?: actions.first()
        val title = NotificationTextNormalizer.normalize(action.title) ?: return null
        val pendingIntent = action.actionIntent ?: return null
        return LowBatteryPowerSavingAction(
            title = title,
            pendingIntent = pendingIntent
        )
    }

    private fun lowBatteryBodyText(notification: Notification): String? {
        val extras = notification.extras
        val directText = listOf(
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
            extras.getCharSequence(Notification.EXTRA_TEXT),
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT),
            extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)
        ).firstNotNullOfOrNull(NotificationTextNormalizer::normalize)
        if (directText != null) {
            return directText
        }
        return extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapNotNull(NotificationTextNormalizer::normalize)
            ?.joinToString(" ")
            ?.takeIf { it.isNotBlank() }
    }

    private fun splitLowBatteryBodyText(
        body: String,
        actionText: String?
    ): Pair<String, String>? {
        val normalizedBody = NotificationTextNormalizer.normalize(body) ?: return null
        val normalizedAction = actionText?.trim()?.takeIf { it.isNotEmpty() }
        val actionIndex = normalizedAction
            ?.let { normalizedBody.indexOf(it, ignoreCase = true) }
            ?.takeIf { it > 0 }
        if (actionIndex != null) {
            val collapsedText = normalizedBody.substring(0, actionIndex).trim()
            val actionLine = normalizedBody.substring(actionIndex).trim()
            if (collapsedText.isNotEmpty() && actionLine.isNotEmpty()) {
                return collapsedText to "$collapsedText\n$actionLine"
            }
        }
        return normalizedBody to normalizedBody
    }

    private fun lowBatteryFallbackTitle(percent: Int): String {
        return "Battery power $percent%"
    }

    private fun stickyBatteryIntent(context: Context): Intent? {
        return runCatching {
            @Suppress("DEPRECATION")
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()
    }

    private fun remainingChargingMinutes(
        context: Context,
        batteryIntent: Intent,
        percent: Int
    ): Int? {
        if (percent >= 100) {
            return 0
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val millis = runCatching {
                batteryManager?.computeChargeTimeRemaining() ?: -1L
            }.getOrDefault(-1L)
            if (millis > 0L) {
                return ((millis + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
            }
        }

        batteryLongExtra(batteryIntent, "remain")?.let { seconds ->
            if (seconds >= 0L) {
                return ((seconds + 59L) / 60L).toInt().coerceAtLeast(1)
            }
        }
        batteryLongExtra(batteryIntent, "remaining_charging_time")?.let { seconds ->
            if (seconds >= 0L) {
                return ((seconds + 59L) / 60L).toInt().coerceAtLeast(1)
            }
        }
        batteryLongExtra(batteryIntent, "charge_time_remaining")?.let { seconds ->
            if (seconds >= 0L) {
                return ((seconds + 59L) / 60L).toInt().coerceAtLeast(1)
            }
        }
        batteryLongExtra(batteryIntent, "charge_time_remaining_ms")?.let { millis ->
            if (millis >= 0L) {
                return ((millis + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
            }
        }
        return null
    }

    private fun resolveChargingInfoSpeed(batteryIntent: Intent): ChargingInfoSpeed {
        chargingPowerWatts(batteryIntent)?.let { watts ->
            return when {
                watts <= 10f -> ChargingInfoSpeed.CHARGING
                watts >= 40f -> ChargingInfoSpeed.SUPER_FAST_2
                watts >= 22f -> ChargingInfoSpeed.SUPER_FAST
                else -> ChargingInfoSpeed.FAST
            }
        }

        val chargerType = batteryIntExtra(batteryIntent, "charger_type")
        if (chargerType != null && chargerType >= 4) {
            return ChargingInfoSpeed.SUPER_FAST_2
        }
        if (
            batteryBooleanExtra(batteryIntent, "hvc") == true ||
            (chargerType != null && chargerType >= 3)
        ) {
            return ChargingInfoSpeed.SUPER_FAST
        }
        return ChargingInfoSpeed.FAST
    }

    private fun chargingPowerWatts(batteryIntent: Intent): Float? {
        val currentUa = batteryIntent.getIntExtra(
            BATTERY_EXTRA_MAX_CHARGING_CURRENT,
            -1
        ).takeIf { it > 0 } ?: return null
        val maxVoltageUv = batteryIntent.getIntExtra(
            BATTERY_EXTRA_MAX_CHARGING_VOLTAGE,
            -1
        ).takeIf { it > 0 }
        val currentVoltageMv = batteryIntent.getIntExtra(
            BatteryManager.EXTRA_VOLTAGE,
            -1
        ).takeIf { it > 0 }
        val voltageUv = maxVoltageUv ?: currentVoltageMv?.let { it * 1000 }
        if (voltageUv == null || voltageUv <= 0) {
            return null
        }
        return (currentUa.toDouble() * voltageUv.toDouble() / 1_000_000_000_000.0)
            .toFloat()
            .takeIf { it > 0f }
    }

    private fun batteryIntExtra(intent: Intent, key: String): Int? {
        return when (val value = intent.extras?.get(key)) {
            is Int -> value
            is Long -> value.toInt()
            is Float -> value.toInt()
            is Double -> value.toInt()
            is Number -> value.toInt()
            is String -> value.trim().toIntOrNull()
            else -> null
        }
    }

    private fun batteryLongExtra(intent: Intent, key: String): Long? {
        return when (val value = intent.extras?.get(key)) {
            is Long -> value
            is Int -> value.toLong()
            is Float -> value.toLong()
            is Double -> value.toLong()
            is Number -> value.toLong()
            is String -> value.trim().toLongOrNull()
            else -> null
        }
    }

    private fun batteryBooleanExtra(intent: Intent, key: String): Boolean? {
        return when (val value = intent.extras?.get(key)) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> when (value.trim().lowercase(Locale.ROOT)) {
                "true", "1", "yes", "on" -> true
                "false", "0", "no", "off" -> false
                else -> null
            }
            else -> null
        }
    }

    fun maybeMirror(context: Context, prefs: ConverterPrefs, sbn: StatusBarNotification): MirrorResult {
        ensureChannel(context)

        val manager = NotificationManagerCompat.from(context)
        if (!prefs.getConverterEnabled()) {
            val staleAggregateIds = synchronized(stateLock) {
                clearAggregateTrackingForSbnKeyLocked(sbn.key)
            }
            staleAggregateIds.forEach { cancelMirroredNotification(manager, it) }
            cancelMirroredNotification(manager, mirrorIdForKey(sbn.key))
            return notMirroredResult()
        }
        if (prefs.getSyncDndEnabled() && isDoNotDisturbActive(context)) {
            val staleAggregateIds = synchronized(stateLock) {
                clearAggregateTrackingForSbnKeyLocked(sbn.key)
            }
            staleAggregateIds.forEach { cancelMirroredNotification(manager, it) }
            cancelMirroredNotification(manager, mirrorIdForKey(sbn.key))
            return notMirroredResult()
        }

        return try {
            if (!passesCoreFilters(context.packageName, sbn)) {
                val staleAggregateIds = synchronized(stateLock) {
                    clearAggregateTrackingForSbnKeyLocked(sbn.key)
                }
                staleAggregateIds.forEach { cancelMirroredNotification(manager, it) }
                cancelMirroredNotification(manager, mirrorIdForKey(sbn.key))
                return notMirroredResult()
            }
            if (isNativeInCallNotification(sbn)) {
                cancelMirrorsForIgnoredSource(manager, sbn)
                return notMirroredResult()
            }
            if (isWhatsAppCallMirrorBlocked(sbn)) {
                cancelMirrorsForIgnoredSource(manager, sbn)
                return notMirroredResult()
            }
            val parserDictionary = LiveParserDictionaryLoader.get(context, prefs)
            if (isPrivacyRedactedNotification(sbn.notification, parserDictionary)) {
                return notMirroredResult()
            }
            val appPresentationOverride = AppPresentationOverridesLoader
                .get(prefs)
                .resolve(sbn.packageName.lowercase(Locale.ROOT))
            if (isUserDismissedMirror(sbn.key)) {
                val staleAggregateIds = synchronized(stateLock) {
                    clearAggregateTrackingForSbnKeyLocked(sbn.key)
                }
                staleAggregateIds.forEach { cancelMirroredNotification(manager, it) }
                cancelMirroredNotification(manager, mirrorIdForKey(sbn.key))
                return notMirroredResult()
            }
            val source = sbn.notification
            val sourceHasEffectiveProgress = hasEffectiveProgress(sbn.packageName, source)
            val samsungBridge = SamsungBridgePreprocessor.build(
                context = context,
                prefs = prefs,
                sbn = sbn,
                sourceHasNativeProgress = sourceHasEffectiveProgress
            )
            val mediaPlaybackSmartEnabled = prefs.getSmartMediaPlaybackEnabled()
            val bypassesRules = prefs.shouldBypassAllRulesForPackage(sbn.packageName)
            val callMirrorSnapshot = if (prefs.getSmartCallsEnabled()) {
                detectActiveCallMirrorSnapshot(
                    sbn = sbn,
                    samsungReparse = samsungBridge.reparsePayload
                )
            } else {
                null
            }
            if (callMirrorSnapshot != null) {
                val nativeInCallMirror = isNativeInCallNotification(sbn)
                val callSamsungBridge = if (nativeInCallMirror) {
                    SamsungBridgeContext(
                        enabled = samsungBridge.enabled,
                        reparsePayload = null,
                        hasNativeOrSamsungProgress = false,
                        hasCustomRemoteCard = false
                    )
                } else {
                    samsungBridge
                }
                if (!bypassesRules &&
                    !passesBaseFilters(prefs, sbn, parserDictionary, mediaPlaybackSmartEnabled)
                ) {
                    val staleAggregateIds = synchronized(stateLock) {
                        clearAggregateTrackingForSbnKeyLocked(sbn.key)
                    }
                    staleAggregateIds.forEach { cancelMirroredNotification(manager, it) }
                    cancelMirroredNotification(manager, mirrorIdForKey(sbn.key))
                    return notMirroredResult()
                }
                val staleAggregateIds = synchronized(stateLock) {
                    clearAggregateTrackingForSbnKeyLocked(sbn.key)
                }
                staleAggregateIds.forEach { cancelMirroredNotification(manager, it) }
                val callStartedAtWallClockMs = upsertCallMirrorState(
                    context = context,
                    sbn = sbn,
                    appPresentationOverride = appPresentationOverride,
                    samsungBridge = callSamsungBridge,
                    snapshot = callMirrorSnapshot
                )
                val notification = buildMirroredNotification(
                    context = context,
                    sbn = sbn,
                    appPresentationOverride = appPresentationOverride,
                    mirrorChannel = MirrorNotificationChannel.CALLS,
                    progressOverride = null,
                    otpOverride = null,
                    smartShortTextOverride = null,
                    requestPromoted = true,
                    samsungBridge = callSamsungBridge,
                    allowNavigationIconHeuristics = false,
                    callMirrorActive = true,
                    callChronometerStartWallClockMs = callStartedAtWallClockMs
                )
                notifyWithPromotionFallback(
                    context = context,
                    manager = manager,
                    notificationId = mirrorIdForKey(sbn.key),
                    mirrorKey = sbn.key,
                    promotedNotification = notification,
                    sbn = sbn,
                    appPresentationOverride = appPresentationOverride,
                    mirrorChannel = MirrorNotificationChannel.CALLS,
                    progressOverride = null,
                    otpOverride = null,
                    smartShortTextOverride = null,
                    samsungBridge = callSamsungBridge,
                    allowNavigationIconHeuristics = false,
                    callMirrorActive = true,
                    callChronometerStartWallClockMs = callStartedAtWallClockMs
                )
                return mirroredResult(
                    notificationId = mirrorIdForKey(sbn.key),
                    mirrorKey = sbn.key
                )
            } else if (source.category == Notification.CATEGORY_CALL) {
                val staleAggregateIds = synchronized(stateLock) {
                    clearAggregateTrackingForSbnKeyLocked(sbn.key)
                }
                staleAggregateIds.forEach { cancelMirroredNotification(manager, it) }
                cancelMirroredNotification(manager, mirrorIdForKey(sbn.key))
                return notMirroredResult()
            }
            if (prefs.shouldBypassAllRulesForPackage(sbn.packageName)) {
                val staleAggregateIds = synchronized(stateLock) {
                    clearAggregateTrackingForSbnKeyLocked(sbn.key)
                }
                staleAggregateIds.forEach { cancelMirroredNotification(manager, it) }

                val notification = buildMirroredNotification(
                    context = context,
                    sbn = sbn,
                    appPresentationOverride = appPresentationOverride,
                    mirrorChannel = MirrorNotificationChannel.BYPASS,
                    progressOverride = null,
                    otpOverride = null,
                    smartShortTextOverride = null,
                    requestPromoted = true,
                    samsungBridge = samsungBridge,
                    allowNavigationIconHeuristics = false
                )
                notifyWithPromotionFallback(
                    context = context,
                    manager = manager,
                    notificationId = mirrorIdForKey(sbn.key),
                    mirrorKey = sbn.key,
                    promotedNotification = notification,
                    sbn = sbn,
                    appPresentationOverride = appPresentationOverride,
                    mirrorChannel = MirrorNotificationChannel.BYPASS,
                    progressOverride = null,
                    otpOverride = null,
                    smartShortTextOverride = null,
                    samsungBridge = samsungBridge,
                    allowNavigationIconHeuristics = false
                )
                return mirroredResult(
                    notificationId = mirrorIdForKey(sbn.key),
                    mirrorKey = sbn.key
                )
            }
            if (!passesBaseFilters(prefs, sbn, parserDictionary, mediaPlaybackSmartEnabled)) {
                val staleAggregateIds = synchronized(stateLock) {
                    clearAggregateTrackingForSbnKeyLocked(sbn.key)
                }
                staleAggregateIds.forEach { cancelMirroredNotification(manager, it) }
                cancelMirroredNotification(manager, mirrorIdForKey(sbn.key))
                return notMirroredResult()
            }
            val hasNativeProgress = samsungBridge.hasNativeOrSamsungProgress
            val animatedIslandEnabled = prefs.getAnimatedIslandEnabled()
            val isMediaPlaybackNotification = mediaPlaybackSmartEnabled &&
                    isLikelyMediaPlaybackNotification(source)
            val mediaPlaybackSnapshot = if (isMediaPlaybackNotification) {
                extractMediaPlaybackSnapshot(
                    context = context,
                    notification = source,
                    sourcePackageName = sbn.packageName
                )
            } else {
                null
            }

            val otpMatch = if (!isMediaPlaybackNotification &&
                !hasNativeProgress &&
                prefs.getOtpDetectionEnabled() &&
                prefs.isOtpPackageAllowed(sbn.packageName)
            ) {
                detectOtpCode(sbn.packageName, source, parserDictionary)
            } else {
                null
            }

            val smartMatch = if (!isMediaPlaybackNotification &&
                otpMatch == null &&
                prefs.getSmartStatusDetectionEnabled()
            ) {
                detectSmartStage(
                    packageName = sbn.packageName,
                    source = source,
                    parserDictionary = parserDictionary,
                    taxiEnabled = prefs.getSmartTaxiEnabled(),
                    deliveryEnabled = prefs.getSmartDeliveryEnabled(),
                    navigationEnabled = prefs.getSmartNavigationEnabled(),
                    weatherEnabled = prefs.getSmartWeatherEnabled(),
                    externalDevicesEnabled = prefs.getSmartExternalDevicesEnabled(),
                    externalDevicesIgnoreDebugging = prefs.getSmartExternalDevicesIgnoreDebugging(),
                    vpnEnabled = prefs.getSmartVpnEnabled(),
                    smartPackageAllowed = prefs.isSmartPackageAllowed(sbn.packageName),
                    hasNativeProgress = hasNativeProgress
                )
            } else {
                null
            }

            val textProgressMatch = if (!isMediaPlaybackNotification &&
                !hasNativeProgress &&
                otpMatch == null &&
                prefs.getTextProgressEnabled()
            ) {
                detectTextProgress(
                    packageName = sbn.packageName,
                    source = source,
                    parserDictionary = parserDictionary
                )
            } else {
                null
            }

            val shouldSuppressNonTrafficVpn = !isMediaPlaybackNotification &&
                    otpMatch == null &&
                    smartMatch == null &&
                    textProgressMatch == null &&
                    prefs.getSmartVpnEnabled() &&
                    shouldSuppressVpnWithoutTraffic(
                        packageName = sbn.packageName,
                        source = source,
                        parserDictionary = parserDictionary
                    )
            if (shouldSuppressNonTrafficVpn) {
                val staleAggregateIds = synchronized(stateLock) {
                    clearAggregateTrackingForSbnKeyLocked(sbn.key)
                }
                staleAggregateIds.forEach { cancelMirroredNotification(manager, it) }
                cancelMirroredNotification(manager, mirrorIdForKey(sbn.key))
                return notMirroredResult()
            }

            when {
                isMediaPlaybackNotification -> {
                    val staleAggregateIds = synchronized(stateLock) {
                        clearAggregateTrackingForSbnKeyLocked(sbn.key)
                    }
                    staleAggregateIds.forEach { cancelMirroredNotification(manager, it) }

                    val mediaProgressOverride = mediaPlaybackSnapshot?.toProgressOverride()
                    val mediaShortText = mediaPlaybackSnapshot?.let(::buildMediaPlaybackShortText)
                    val mediaTitle = mediaPlaybackSnapshot?.title
                    val mediaText = mediaPlaybackSnapshot?.artist
                    val mediaLargeIcon = mediaPlaybackSnapshot?.albumArt
                    val notification = buildMirroredNotification(
                        context = context,
                        sbn = sbn,
                        appPresentationOverride = appPresentationOverride,
                        mirrorChannel = MirrorNotificationChannel.MEDIA_PLAYBACK,
                        progressOverride = mediaProgressOverride,
                        otpOverride = null,
                        smartShortTextOverride = mediaShortText,
                        requestPromoted = true,
                        allowNavigationIconHeuristics = false,
                        preferMediaControls = true,
                        mediaPlaybackIsPlaying = mediaPlaybackSnapshot?.isPlaying,
                        titleOverride = mediaTitle,
                        textOverride = mediaText,
                        largeIconOverride = mediaLargeIcon
                    )
                    notifyWithPromotionFallback(
                        context = context,
                        manager = manager,
                        notificationId = mirrorIdForKey(sbn.key),
                        mirrorKey = sbn.key,
                        promotedNotification = notification,
                        sbn = sbn,
                        appPresentationOverride = appPresentationOverride,
                        mirrorChannel = MirrorNotificationChannel.MEDIA_PLAYBACK,
                        progressOverride = mediaProgressOverride,
                        otpOverride = null,
                        smartShortTextOverride = mediaShortText,
                        allowNavigationIconHeuristics = false,
                        preferMediaControls = true,
                        mediaPlaybackIsPlaying = mediaPlaybackSnapshot?.isPlaying,
                        titleOverride = mediaTitle,
                        textOverride = mediaText,
                        largeIconOverride = mediaLargeIcon
                    )
                    mirroredResult(
                        notificationId = mirrorIdForKey(sbn.key),
                        mirrorKey = sbn.key
                    )
                }

                otpMatch != null -> {
                    if (isUserDismissedMirror(otpMatch.aggregateKey)) {
                        return notMirroredResult()
                    }
                    val routeState = synchronized(stateLock) {
                        val staleAggregateIds = mutableListOf<Int>()
                        staleAggregateIds.addAll(clearSmartTrackingForSbnKeyLocked(sbn.key))

                        val sourceKey = otpSourceKeyForPackage(sbn.packageName)
                        val sourceState = otpSourceStates[sourceKey]
                        if (sourceState != null &&
                            sourceState.sbnKey != sbn.key &&
                            sbn.postTime < sourceState.postTimeMs
                        ) {
                            staleAggregateIds.addAll(clearOtpTrackingForSbnKeyLocked(sbn.key))
                            OtpRouteState(
                                staleAggregateIds = staleAggregateIds,
                                shouldPublish = false,
                                shouldAutoCopy = false,
                                otpCode = otpMatch.code
                            )
                        } else {
                            staleAggregateIds.addAll(clearOtpTrackingForSourceLocked(sourceKey, sbn.key))

                            val existingOtpAggregateKey = sbnToOtpAggregateKey[sbn.key]
                            if (existingOtpAggregateKey != null && existingOtpAggregateKey != otpMatch.aggregateKey) {
                                staleAggregateIds.addAll(clearOtpTrackingForSbnKeyLocked(sbn.key))
                            }

                            val state = otpAggregateStates.getOrPut(otpMatch.aggregateKey) { OtpAggregateState() }
                            state.activeSbnKeys.add(sbn.key)
                            sbnToOtpAggregateKey[sbn.key] = otpMatch.aggregateKey
                            sbnToOtpSourceKey[sbn.key] = sourceKey
                            otpSourceStates[sourceKey] = OtpSourceState(
                                sbnKey = sbn.key,
                                aggregateKey = otpMatch.aggregateKey,
                                postTimeMs = sbn.postTime
                            )

                            val now = System.currentTimeMillis()
                            val shouldPublish =
                                state.lastRenderedAtMs == 0L ||
                                        now - state.lastRenderedAtMs >= OTP_REPEAT_SUPPRESS_MS
                            if (shouldPublish) {
                                state.lastRenderedAtMs = now
                            }
                            val shouldAutoCopy =
                                prefs.getOtpAutoCopyEnabled() &&
                                        shouldAutoCopyOtpLocked(state, otpMatch.code)
                            OtpRouteState(
                                staleAggregateIds = staleAggregateIds,
                                shouldPublish = shouldPublish,
                                shouldAutoCopy = shouldAutoCopy,
                                otpCode = otpMatch.code
                            )
                        }
                    }
                    routeState.staleAggregateIds.forEach { cancelMirroredNotification(manager, it) }

                    if (routeState.shouldPublish) {
                        val notification = buildMirroredNotification(
                            context = context,
                            sbn = sbn,
                            appPresentationOverride = appPresentationOverride,
                            mirrorChannel = MirrorNotificationChannel.OTP_CODES,
                            progressOverride = null,
                            otpOverride = otpMatch,
                            smartShortTextOverride = null,
                            requestPromoted = true,
                            samsungBridge = samsungBridge
                        )
                        notifyWithPromotionFallback(
                            context = context,
                            manager = manager,
                            notificationId = mirrorIdForKey(otpMatch.aggregateKey),
                            mirrorKey = otpMatch.aggregateKey,
                            promotedNotification = notification,
                            sbn = sbn,
                            appPresentationOverride = appPresentationOverride,
                            mirrorChannel = MirrorNotificationChannel.OTP_CODES,
                            progressOverride = null,
                            otpOverride = otpMatch,
                            smartShortTextOverride = null,
                            samsungBridge = samsungBridge
                        )
                    }
                    if (routeState.shouldAutoCopy) {
                        copyOtpToClipboard(context, routeState.otpCode)
                        if (routeState.shouldPublish) {
                            startOtpAutoCopyAnimation(
                                context = context,
                                manager = manager,
                                sbn = sbn,
                                appPresentationOverride = appPresentationOverride,
                                otpMatch = otpMatch,
                                samsungBridge = samsungBridge
                            )
                        }
                    }
                    if (routeState.shouldPublish) {
                        mirroredResult(
                            notificationId = mirrorIdForKey(otpMatch.aggregateKey),
                            mirrorKey = otpMatch.aggregateKey,
                            dedupKind = MirrorDedupKind.OTP
                        )
                    } else {
                        notMirroredResult()
                    }
                }

                textProgressMatch != null -> {
                    val staleAggregateIds = synchronized(stateLock) {
                        clearAggregateTrackingForSbnKeyLocked(sbn.key)
                    }
                    staleAggregateIds.forEach { cancelMirroredNotification(manager, it) }

                    val notification = buildMirroredNotification(
                        context = context,
                        sbn = sbn,
                        appPresentationOverride = appPresentationOverride,
                        mirrorChannel = MirrorNotificationChannel.PROGRESS_NOTIFICATIONS,
                        progressOverride = ProgressOverride(
                            value = textProgressMatch.percent,
                            max = 100
                        ),
                        otpOverride = null,
                        smartShortTextOverride = textProgressMatch.shortText,
                        requestPromoted = true,
                        samsungBridge = samsungBridge
                    )
                    notifyWithPromotionFallback(
                        context = context,
                        manager = manager,
                        notificationId = mirrorIdForKey(sbn.key),
                        mirrorKey = sbn.key,
                        promotedNotification = notification,
                        sbn = sbn,
                        appPresentationOverride = appPresentationOverride,
                        mirrorChannel = MirrorNotificationChannel.PROGRESS_NOTIFICATIONS,
                        progressOverride = ProgressOverride(
                            value = textProgressMatch.percent,
                            max = 100
                        ),
                        otpOverride = null,
                        smartShortTextOverride = textProgressMatch.shortText,
                        samsungBridge = samsungBridge
                    )
                    mirroredResult(
                        notificationId = mirrorIdForKey(sbn.key),
                        mirrorKey = sbn.key
                    )
                }

                smartMatch != null -> {
                    if (isUserDismissedMirror(smartMatch.aggregateKey)) {
                        return notMirroredResult()
                    }
                    val routeState = synchronized(stateLock) {
                        val staleAggregateIds = mutableListOf<Int>()
                        staleAggregateIds.addAll(clearOtpTrackingForSbnKeyLocked(sbn.key))

                        val existingSmartAggregateKey = sbnToAggregateKey[sbn.key]
                        if (existingSmartAggregateKey != null &&
                            existingSmartAggregateKey != smartMatch.aggregateKey
                        ) {
                            staleAggregateIds.addAll(clearSmartTrackingForSbnKeyLocked(sbn.key))
                        }

                        val state = aggregateStates.getOrPut(smartMatch.aggregateKey) {
                            AggregateState(smartMatch.stageValue, smartMatch.maxStage)
                        }
                        state.activeSbnKeys.add(sbn.key)
                        state.sourcesBySbnKey[sbn.key] = SmartSourceEntry(
                            stageValue = smartMatch.stageValue,
                            postTimeMs = sbn.postTime,
                            sbn = sbn,
                            compactOrderCode = smartMatch.compactOrderCode
                        )
                        state.maxStageSeen = if (smartMatch.keepHighestStage) {
                            maxOf(state.maxStageSeen, smartMatch.stageValue)
                        } else {
                            smartMatch.stageValue
                        }
                        sbnToAggregateKey[sbn.key] = smartMatch.aggregateKey
                        val sourceEntry = selectSmartSourceEntryLocked(
                            aggregateState = state,
                            keepHighestStage = smartMatch.keepHighestStage
                        )

                        SmartRouteState(
                            staleAggregateIds = staleAggregateIds,
                            stageValue = state.maxStageSeen,
                            stageMax = state.maxStage,
                            compactOrderCode = sourceEntry?.compactOrderCode ?: smartMatch.compactOrderCode,
                            sourceSbn = sourceEntry?.sbn ?: sbn
                        )
                    }
                    routeState.staleAggregateIds.forEach { cancelMirroredNotification(manager, it) }
                    val sourceSbn = routeState.sourceSbn
                    val sourceNotification = sourceSbn.notification
                    val smartRuleId = smartRuleIdFromAggregateKey(smartMatch.aggregateKey)
                    val mirrorChannel = mirrorChannelForSmartRule(smartRuleId)
                    val deliveryEta = if (smartRuleId == "food") {
                        extractDeliveryEta(
                            context = context,
                            notification = sourceNotification,
                            packageName = sourceSbn.packageName,
                            fallbackTitle = sourceSbn.packageName
                        )
                    } else {
                        null
                    }
                    // Delivery ETA can be driven by custom RemoteViews; keep that source active
                    // so periodic snapshot sync can read the current value instead of freezing it.
                    val dedupKind = if (
                        deliveryEta == null &&
                        isNotificationDedupEligibleSmartRule(smartRuleId)
                    ) {
                        MirrorDedupKind.STATUS
                    } else {
                        MirrorDedupKind.NONE
                    }
                    val defaultSmartStatus = smartShortStatusText(
                        context = context,
                        ruleId = smartRuleId,
                        stageValue = routeState.stageValue,
                        parserDictionary = parserDictionary
                    )
                    val vpnTraffic = if (smartRuleId == "vpn") {
                        extractVpnTrafficSpeeds(
                            notification = sourceNotification,
                            fallbackTitle = sourceSbn.packageName,
                            parserDictionary = parserDictionary
                        )
                    } else {
                        null
                    }
                    val smartStatusText = when (smartRuleId) {
                        "navigation" -> extractNavigationDistanceText(
                            notification = sourceNotification,
                            fallbackTitle = sourceSbn.packageName,
                            parserDictionary = parserDictionary
                        ) ?: defaultSmartStatus

                        "weather" -> extractWeatherTemperatureText(
                            notification = sourceNotification,
                            fallbackTitle = sourceSbn.packageName,
                            parserDictionary = parserDictionary
                        ) ?: defaultSmartStatus

                        "external_device" -> extractExternalDeviceStatusText(
                            context = context,
                            notification = sourceNotification,
                            fallbackTitle = sourceSbn.packageName,
                            stageValue = routeState.stageValue,
                            parserDictionary = parserDictionary
                        ) ?: defaultSmartStatus

                        "food" -> deliveryEta?.text ?: defaultSmartStatus

                        "vpn" -> formatDominantVpnTrafficText(vpnTraffic) ?: defaultSmartStatus

                        else -> defaultSmartStatus
                    } ?: routeState.compactOrderCode
                    val smartProgressOverride = if (
                        smartRuleId == "weather" ||
                        smartRuleId == "external_device" ||
                        smartRuleId == "vpn"
                    ) {
                        null
                    } else {
                        ProgressOverride(routeState.stageValue, routeState.stageMax)
                    }
                    val shouldAnimateSmartIsland = animatedIslandEnabled && deliveryEta == null

                    val notification = buildMirroredNotification(
                        context = context,
                        sbn = sourceSbn,
                        appPresentationOverride = appPresentationOverride,
                        mirrorChannel = mirrorChannel,
                        progressOverride = smartProgressOverride,
                        otpOverride = null,
                        smartShortTextOverride = smartStatusText,
                        compactCodeOverride = routeState.compactOrderCode,
                        smartRuleId = smartRuleId,
                        requestPromoted = true,
                        samsungBridge = samsungBridge,
                        preferSmartShortTextAsPrimary = shouldAnimateSmartIsland
                    )
                    notifyWithPromotionFallback(
                        context = context,
                        manager = manager,
                        notificationId = mirrorIdForKey(smartMatch.aggregateKey),
                        mirrorKey = smartMatch.aggregateKey,
                        promotedNotification = notification,
                        sbn = sourceSbn,
                        appPresentationOverride = appPresentationOverride,
                        mirrorChannel = mirrorChannel,
                        progressOverride = smartProgressOverride,
                        otpOverride = null,
                        smartShortTextOverride = smartStatusText,
                        compactCodeOverride = routeState.compactOrderCode,
                        smartRuleId = smartRuleId,
                        samsungBridge = samsungBridge,
                        preferSmartShortTextAsPrimary = shouldAnimateSmartIsland
                    )
                    if (shouldAnimateSmartIsland) {
                        val animatedTokens = buildSmartAnimatedIslandTokens(
                            ruleId = smartRuleId,
                            notification = sourceNotification,
                            fallbackTitle = sourceSbn.packageName,
                            primaryStatus = smartStatusText,
                            compactOrderCode = routeState.compactOrderCode,
                            parserDictionary = parserDictionary
                        )
                        startSmartIslandAnimation(
                            context = context,
                            manager = manager,
                            aggregateKey = smartMatch.aggregateKey,
                            sbn = sourceSbn,
                            appPresentationOverride = appPresentationOverride,
                            mirrorChannel = mirrorChannel,
                            progressOverride = smartProgressOverride,
                            smartRuleId = smartRuleId,
                            tokens = animatedTokens,
                            initialToken = smartStatusText,
                            compactCodeOverride = routeState.compactOrderCode,
                            samsungBridge = samsungBridge
                        )
                    }
                    mirroredResult(
                        notificationId = mirrorIdForKey(smartMatch.aggregateKey),
                        mirrorKey = smartMatch.aggregateKey,
                        dedupKind = dedupKind
                    )
                }

                hasNativeProgress -> {
                    val staleAggregateIds = synchronized(stateLock) {
                        clearAggregateTrackingForSbnKeyLocked(sbn.key)
                    }
                    staleAggregateIds.forEach { cancelMirroredNotification(manager, it) }

                    val notification = buildMirroredNotification(
                        context = context,
                        sbn = sbn,
                        appPresentationOverride = appPresentationOverride,
                        mirrorChannel = MirrorNotificationChannel.PROGRESS_NOTIFICATIONS,
                        progressOverride = null,
                        otpOverride = null,
                        smartShortTextOverride = null,
                        requestPromoted = true,
                        samsungBridge = samsungBridge
                    )
                    notifyWithPromotionFallback(
                        context = context,
                        manager = manager,
                        notificationId = mirrorIdForKey(sbn.key),
                        mirrorKey = sbn.key,
                        promotedNotification = notification,
                        sbn = sbn,
                        appPresentationOverride = appPresentationOverride,
                        mirrorChannel = MirrorNotificationChannel.PROGRESS_NOTIFICATIONS,
                        progressOverride = null,
                        otpOverride = null,
                        smartShortTextOverride = null,
                        samsungBridge = samsungBridge
                    )
                    mirroredResult(
                        notificationId = mirrorIdForKey(sbn.key),
                        mirrorKey = sbn.key
                    )
                }

                else -> {
                    val staleAggregateIds = synchronized(stateLock) {
                        clearAggregateTrackingForSbnKeyLocked(sbn.key)
                    }
                    staleAggregateIds.forEach { cancelMirroredNotification(manager, it) }
                    cancelMirroredNotification(manager, mirrorIdForKey(sbn.key))
                    notMirroredResult()
                }
            }
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to mirror notification: ${sbn.key}", error)
            notMirroredResult()
        }
    }

    private fun isDoNotDisturbActive(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false
        return try {
            when (notificationManager.currentInterruptionFilter) {
                NotificationManager.INTERRUPTION_FILTER_NONE,
                NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                NotificationManager.INTERRUPTION_FILTER_ALARMS -> true

                else -> false
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun detectActiveCallMirrorSnapshot(
        sbn: StatusBarNotification,
        samsungReparse: SamsungReparsePayload?
    ): CallMirrorSnapshot? {
        if (sbn.packageName.lowercase(Locale.ROOT) in CALL_MIRROR_EXCLUDED_PACKAGES) {
            return null
        }

        val source = sbn.notification
        val ongoing = sbn.isOngoing ||
                source.flags and Notification.FLAG_ONGOING_EVENT != 0 ||
                !sbn.isClearable
        if (!ongoing) {
            return null
        }

        val contentTexts = collectCallContentTexts(
            notification = source,
            fallbackTitle = sbn.packageName,
            samsungReparse = samsungReparse
        )
        val actionTexts = collectCallActionTexts(source)
        if (hasIncomingOrDialingCallMarker(contentTexts, actionTexts)) {
            return null
        }

        val timeSeed = resolveCallTimeSeed(source, contentTexts)
        val isDiscordVoiceConnection = isDiscordVoiceConnectionNotification(
            sbn = sbn,
            contentTexts = contentTexts,
            actionTexts = actionTexts
        )
        val hasEndCallAction = actionTexts.any(callEndActionPattern::containsMatchIn)
        val hasActiveCallText = contentTexts.any(callActiveTextPattern::containsMatchIn)
        val hasCallContext =
            source.category == Notification.CATEGORY_CALL ||
                    contentTexts.any(callContextTextPattern::containsMatchIn) ||
                    isDiscordVoiceConnection
        if (!hasCallContext) {
            return null
        }
        if (source.category != Notification.CATEGORY_CALL &&
            !hasEndCallAction &&
            !isDiscordVoiceConnection
        ) {
            return null
        }
        if (!timeSeed.hasExplicitSource &&
            !hasEndCallAction &&
            !hasActiveCallText &&
            !isDiscordVoiceConnection
        ) {
            return null
        }

        return CallMirrorSnapshot(
            explicitStartWallClockMs = timeSeed.explicitStartWallClockMs,
            elapsedDurationMs = timeSeed.elapsedDurationMs
        )
    }

    private fun isNativeInCallNotification(sbn: StatusBarNotification): Boolean {
        val packageName = sbn.packageName.lowercase(Locale.ROOT)
        return packageName in NATIVE_IN_CALL_PACKAGES
    }

    private fun isWhatsAppCallMirrorBlocked(sbn: StatusBarNotification): Boolean {
        val packageName = sbn.packageName.lowercase(Locale.ROOT)
        if (packageName !in WHATSAPP_CALL_MIRROR_BLOCKED_PACKAGES) {
            return false
        }

        val source = sbn.notification
        if (source.category == Notification.CATEGORY_CALL) {
            return true
        }

        val ongoing = sbn.isOngoing ||
                source.flags and Notification.FLAG_ONGOING_EVENT != 0 ||
                !sbn.isClearable
        if (!ongoing) {
            return false
        }

        val actionTexts = collectCallActionTexts(source)
        if (actionTexts.any(callEndActionPattern::containsMatchIn)) {
            return true
        }

        val contentTexts = collectCallContentTexts(
            notification = source,
            fallbackTitle = sbn.packageName
        )
        return contentTexts.any(callActiveTextPattern::containsMatchIn) &&
                contentTexts.any(callDurationPattern::containsMatchIn)
    }

    private fun isDiscordVoiceConnectionNotification(
        sbn: StatusBarNotification,
        contentTexts: List<String>,
        actionTexts: List<String>
    ): Boolean {
        val packageName = sbn.packageName.lowercase(Locale.ROOT)
        if (packageName !in DISCORD_PACKAGES) {
            return false
        }
        val source = sbn.notification
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            source.channelId?.trim().orEmpty()
        } else {
            ""
        }
        val hasVoiceChannel = channelId.equals("mediaConnections", ignoreCase = true)
        val hasVoiceText = contentTexts.any(discordVoiceConnectedPattern::containsMatchIn)
        val hasDisconnectAction = actionTexts.any(callEndActionPattern::containsMatchIn)
        return (hasVoiceChannel || hasVoiceText) && (hasVoiceText || hasDisconnectAction)
    }

    private fun upsertCallMirrorState(
        context: Context,
        sbn: StatusBarNotification,
        appPresentationOverride: AppPresentationOverride,
        samsungBridge: SamsungBridgeContext,
        snapshot: CallMirrorSnapshot
    ): Long {
        val now = System.currentTimeMillis()
        var scheduleGeneration: Long? = null
        val startedAtWallClockMs = synchronized(stateLock) {
            val existing = callMirrorStates[sbn.key]
            val resolvedStart = resolveCallStartedAtWallClockMs(
                sbn = sbn,
                snapshot = snapshot,
                existingStartedAtWallClockMs = existing?.startedAtWallClockMs,
                nowWallClockMs = now
            )
            if (existing == null) {
                callMirrorGenerationCounter += 1L
                val generation = callMirrorGenerationCounter
                callMirrorStates[sbn.key] = CallMirrorState(
                    sbn = sbn,
                    appPresentationOverride = appPresentationOverride,
                    samsungBridge = samsungBridge,
                    startedAtWallClockMs = resolvedStart,
                    generation = generation
                )
                scheduleGeneration = generation
            } else {
                existing.sbn = sbn
                existing.appPresentationOverride = appPresentationOverride
                existing.samsungBridge = samsungBridge
                existing.startedAtWallClockMs = resolvedStart
            }
            callMirrorStates[sbn.key]?.startedAtWallClockMs ?: resolvedStart
        }

        scheduleGeneration?.let { generation ->
            scheduleCallMirrorRefresh(
                context = context.applicationContext,
                mirrorKey = sbn.key,
                generation = generation
            )
        }
        return startedAtWallClockMs
    }

    private fun scheduleCallMirrorRefresh(
        context: Context,
        mirrorKey: String,
        generation: Long
    ) {
        mainHandler.postDelayed({
            val frame = synchronized(stateLock) {
                val state = callMirrorStates[mirrorKey] ?: return@synchronized null
                if (state.generation != generation || isUserDismissedMirrorLocked(mirrorKey)) {
                    if (state.generation == generation) {
                        callMirrorStates.remove(mirrorKey)
                    }
                    return@synchronized null
                }
                CallMirrorFrame(
                    sbn = state.sbn,
                    appPresentationOverride = state.appPresentationOverride,
                    samsungBridge = state.samsungBridge,
                    startedAtWallClockMs = state.startedAtWallClockMs
                )
            } ?: return@postDelayed

            val manager = NotificationManagerCompat.from(context)
            try {
                val notification = buildMirroredNotification(
                    context = context,
                    sbn = frame.sbn,
                    appPresentationOverride = frame.appPresentationOverride,
                    mirrorChannel = MirrorNotificationChannel.CALLS,
                    progressOverride = null,
                    otpOverride = null,
                    smartShortTextOverride = null,
                    requestPromoted = true,
                    samsungBridge = frame.samsungBridge,
                    allowNavigationIconHeuristics = false,
                    callMirrorActive = true,
                    callChronometerStartWallClockMs = frame.startedAtWallClockMs
                )
                notifyWithPromotionFallback(
                    context = context,
                    manager = manager,
                    notificationId = mirrorIdForKey(mirrorKey),
                    mirrorKey = mirrorKey,
                    promotedNotification = notification,
                    sbn = frame.sbn,
                    appPresentationOverride = frame.appPresentationOverride,
                    mirrorChannel = MirrorNotificationChannel.CALLS,
                    progressOverride = null,
                    otpOverride = null,
                    smartShortTextOverride = null,
                    samsungBridge = frame.samsungBridge,
                    allowNavigationIconHeuristics = false,
                    callMirrorActive = true,
                    callChronometerStartWallClockMs = frame.startedAtWallClockMs
                )
            } catch (error: Throwable) {
                Log.e(TAG, "Failed call duration mirror update: $mirrorKey", error)
            }

            if (isCallMirrorGenerationCurrent(mirrorKey, generation)) {
                scheduleCallMirrorRefresh(
                    context = context,
                    mirrorKey = mirrorKey,
                    generation = generation
                )
            }
        }, CALL_DURATION_REFRESH_MS)
    }

    private fun isCallMirrorGenerationCurrent(mirrorKey: String, generation: Long): Boolean {
        return synchronized(stateLock) {
            val state = callMirrorStates[mirrorKey] ?: return@synchronized false
            state.generation == generation && !isUserDismissedMirrorLocked(mirrorKey)
        }
    }

    private fun resolveCallStartedAtWallClockMs(
        sbn: StatusBarNotification,
        snapshot: CallMirrorSnapshot,
        existingStartedAtWallClockMs: Long?,
        nowWallClockMs: Long
    ): Long {
        val resolved = when {
            snapshot.explicitStartWallClockMs != null -> snapshot.explicitStartWallClockMs
            snapshot.elapsedDurationMs != null -> nowWallClockMs - snapshot.elapsedDurationMs
            existingStartedAtWallClockMs != null -> existingStartedAtWallClockMs
            sbn.postTime > 0L -> sbn.postTime
            else -> nowWallClockMs
        }
        return resolved.coerceIn(0L, nowWallClockMs)
    }

    private fun resolveCallTimeSeed(
        notification: Notification,
        contentTexts: List<String>
    ): CallTimeSeed {
        resolveCallChronometerStartWallClockMs(notification)?.let { startMs ->
            return CallTimeSeed(
                explicitStartWallClockMs = startMs,
                elapsedDurationMs = null
            )
        }

        val parsedDurationMs = contentTexts
            .asSequence()
            .flatMap { text -> callDurationPattern.findAll(text).map { it.value } }
            .mapNotNull(::parseClockDurationMs)
            .maxOrNull()

        return CallTimeSeed(
            explicitStartWallClockMs = null,
            elapsedDurationMs = parsedDurationMs
        )
    }

    private fun resolveCallChronometerStartWallClockMs(notification: Notification): Long? {
        val extras = notification.extras
        if (!extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false)) {
            return null
        }
        if (extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN, false)) {
            return null
        }
        return notification.`when`.takeIf { it > 0L }
    }

    private fun parseClockDurationMs(value: String): Long? {
        val parts = value.split(":")
        if (parts.size !in 2..3) {
            return null
        }
        val numbers = parts.map { it.toLongOrNull() ?: return null }
        val totalSeconds = if (numbers.size == 3) {
            val hours = numbers[0]
            val minutes = numbers[1]
            val seconds = numbers[2]
            if (minutes !in 0..59 || seconds !in 0..59) {
                return null
            }
            hours * 3_600L + minutes * 60L + seconds
        } else {
            val minutes = numbers[0]
            val seconds = numbers[1]
            if (seconds !in 0..59) {
                return null
            }
            minutes * 60L + seconds
        }
        return (totalSeconds * 1_000L).coerceAtLeast(0L)
    }

    private fun hasIncomingOrDialingCallMarker(
        contentTexts: List<String>,
        actionTexts: List<String>
    ): Boolean {
        if (actionTexts.any(callAnswerActionPattern::containsMatchIn)) {
            return true
        }
        return contentTexts.any { text ->
            callIncomingTextPattern.containsMatchIn(text) ||
                    callDialingTextPattern.containsMatchIn(text)
        }
    }

    private fun collectCallActionTexts(notification: Notification): List<String> {
        return notification.actions
            ?.mapNotNull { action -> NotificationTextNormalizer.normalize(action.title) }
            ?.distinct()
            .orEmpty()
    }

    private fun collectCallContentTexts(
        notification: Notification,
        fallbackTitle: String,
        samsungReparse: SamsungReparsePayload? = null
    ): List<String> {
        val extras = notification.extras
        val parts = mutableListOf<String>()

        fun add(value: CharSequence?) {
            NotificationTextNormalizer.normalize(value)?.let(parts::add)
        }

        fun addString(value: String?) {
            value?.let { add(it) }
        }

        add(extras.getCharSequence(Notification.EXTRA_TITLE))
        add(extras.getCharSequence(Notification.EXTRA_TITLE_BIG))
        add(extras.getCharSequence(Notification.EXTRA_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_INFO_TEXT))
        add(notification.tickerText)
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach(::add)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            extras.getParcelableArray(Notification.EXTRA_MESSAGES)
                ?.let(Notification.MessagingStyle.Message::getMessagesFromBundleArray)
                ?.forEach { message -> add(message.text) }
        }
        extractRemoteViewTexts(notification).forEach { add(it) }
        addString(samsungReparse?.title)
        addString(samsungReparse?.text)
        addString(samsungReparse?.chipText)

        if (parts.isEmpty()) {
            parts.add(fallbackTitle)
        }
        return parts.distinct()
    }

    fun cancelMirrored(context: Context, sbn: StatusBarNotification) {
        try {
            val manager = NotificationManagerCompat.from(context)
            val staleAggregateIds = synchronized(stateLock) {
                val directMirrorId = mirrorIdForKey(sbn.key)
                userDismissedMirrorKeys.remove(sbn.key)
                sourceSnapshotsByMirrorKey.remove(sbn.key)
                callMirrorStates.remove(sbn.key)
                mirrorKeysByNotificationId.remove(directMirrorId)
                clearAggregateTrackingForSbnKeyLocked(sbn.key)
            }
            staleAggregateIds.forEach { cancelMirroredNotification(manager, it) }
            cancelMirroredNotification(manager, mirrorIdForKey(sbn.key))
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to cancel mirrored notification: ${sbn.key}", error)
        }
    }

    private fun cancelMirrorsForIgnoredSource(
        manager: NotificationManagerCompat,
        sbn: StatusBarNotification
    ) {
        val directMirrorId = mirrorIdForKey(sbn.key)
        val staleAggregateIds = synchronized(stateLock) {
            userDismissedMirrorKeys.remove(sbn.key)
            sourceSnapshotsByMirrorKey.remove(sbn.key)
            callMirrorStates.remove(sbn.key)
            mirrorKeysByNotificationId.remove(directMirrorId)
            clearAggregateTrackingForSbnKeyLocked(sbn.key)
        }
        staleAggregateIds.forEach { cancelMirroredNotification(manager, it) }
        cancelMirroredNotification(manager, directMirrorId)
    }

    fun handleMirroredRemoved(context: Context, sbn: StatusBarNotification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !isMirrorNotificationChannel(sbn.notification.channelId)
        ) {
            return
        }
        if (ConverterPrefs(context).getPreventMirrorDismissEnabled()) {
            return
        }

        synchronized(stateLock) {
            val now = SystemClock.elapsedRealtime()
            pruneProgrammaticMirrorCancelsLocked(now)
            if (consumeProgrammaticMirrorCancelLocked(sbn.id, now)) {
                return
            }

            val mirrorKey = mirrorKeysByNotificationId.remove(sbn.id) ?: return
            sourceSnapshotsByMirrorKey.remove(mirrorKey)
            callMirrorStates.remove(mirrorKey)
            userDismissedMirrorKeys.add(mirrorKey)
            callMirrorStates.remove(mirrorKey)
            smartAnimationGenerations.remove(mirrorKey)
            smartAnimationStates.remove(mirrorKey)
            otpAnimationGenerations.remove(mirrorKey)
        }
    }
    private fun notMirroredResult(): MirrorResult {
        return MirrorResult(mirrored = false)
    }

    private fun mirroredResult(
        notificationId: Int,
        mirrorKey: String,
        dedupKind: MirrorDedupKind = MirrorDedupKind.NONE,
        removeSource: Boolean = false
    ): MirrorResult {
        return MirrorResult(
            mirrored = true,
            dedupKind = dedupKind,
            notificationId = notificationId,
            mirrorKey = mirrorKey,
            removeSource = removeSource
        )
    }

    fun isMirrorNotificationChannel(channelId: String?): Boolean {
        val normalized = channelId?.trim().orEmpty()
        return normalized.isNotEmpty() &&
                MirrorNotificationChannel.entries.any { it.id == normalized }
    }

    private fun mirrorChannelForSmartRule(ruleId: String?): MirrorNotificationChannel {
        return when (ruleId) {
            "vpn", "external_device" -> MirrorNotificationChannel.NETWORK_CONNECTIONS
            "navigation", "weather" -> MirrorNotificationChannel.MISCELLANEOUS
            else -> MirrorNotificationChannel.SMART_CONVERSIONS
        }
    }

    private fun mirrorChannelText(
        context: Context,
        channel: MirrorNotificationChannel
    ): MirrorChannelText {
        val isRussian = isRussianLocale(context)
        return when (channel) {
            MirrorNotificationChannel.LEGACY -> {
                if (isRussian) {
                    MirrorChannelText(
                        name = "LiveBridge",
                        description = "Старый общий канал конвертированных уведомлений"
                    )
                } else {
                    MirrorChannelText(
                        name = CHANNEL_NAME,
                        description = "Legacy channel for converted notifications"
                    )
                }
            }

            MirrorNotificationChannel.PROGRESS_NOTIFICATIONS -> {
                if (isRussian) {
                    MirrorChannelText(
                        name = "Progress notifications",
                        description = "Конвертированные уведомления с прогрессом"
                    )
                } else {
                    MirrorChannelText(
                        name = "Progress notifications",
                        description = "Converted notifications with progress"
                    )
                }
            }

            MirrorNotificationChannel.OTP_CODES -> {
                if (isRussian) {
                    MirrorChannelText(
                        name = "OTP codes",
                        description = "Коды подтверждения и действия с ними"
                    )
                } else {
                    MirrorChannelText(
                        name = "OTP codes",
                        description = "Verification code conversions"
                    )
                }
            }

            MirrorNotificationChannel.SMART_CONVERSIONS -> {
                if (isRussian) {
                    MirrorChannelText(
                        name = "Smart conversions",
                        description = "Такси, доставки и похожие smart-конверсии"
                    )
                } else {
                    MirrorChannelText(
                        name = "Smart conversions",
                        description = "Taxi, deliveries and similar smart conversions"
                    )
                }
            }

            MirrorNotificationChannel.MEDIA_PLAYBACK -> {
                if (isRussian) {
                    MirrorChannelText(
                        name = "Media playback",
                        description = "Конвертированный медиаплеер"
                    )
                } else {
                    MirrorChannelText(
                        name = "Media playback",
                        description = "Converted media playback notifications"
                    )
                }
            }

            MirrorNotificationChannel.CALLS -> {
                if (isRussian) {
                    MirrorChannelText(
                        name = "Calls",
                        description = "Активные звонки с таймером разговора"
                    )
                } else {
                    MirrorChannelText(
                        name = "Calls",
                        description = "Active calls with elapsed call time"
                    )
                }
            }

            MirrorNotificationChannel.NETWORK_CONNECTIONS -> {
                if (isRussian) {
                    MirrorChannelText(
                        name = "Network & connections",
                        description = "VPN и внешние устройства"
                    )
                } else {
                    MirrorChannelText(
                        name = "Network & connections",
                        description = "VPN and external device conversions"
                    )
                }
            }

            MirrorNotificationChannel.MISCELLANEOUS -> {
                if (isRussian) {
                    MirrorChannelText(
                        name = "Miscellaneous conversions",
                        description = "Навигация, погода и прочие конверсии"
                    )
                } else {
                    MirrorChannelText(
                        name = "Miscellaneous conversions",
                        description = "Navigation, weather and other conversions"
                    )
                }
            }

            MirrorNotificationChannel.NOTIFICATION_CAPSULE ->
                notificationCapsuleChannelText(context)

            MirrorNotificationChannel.CHARGING_INFO ->
                chargingInfoChannelText(context)

            MirrorNotificationChannel.BYPASS -> {
                if (isRussian) {
                    MirrorChannelText(
                        name = "Bypass applications",
                        description = "Уведомления приложений из bypass-списка"
                    )
                } else {
                    MirrorChannelText(
                        name = "Bypass applications",
                        description = "Notifications from bypassed apps"
                    )
                }
            }
        }
    }

    private fun passesBaseFilters(
        prefs: ConverterPrefs,
        sbn: StatusBarNotification,
        parserDictionary: LiveParserDictionary,
        mediaPlaybackSmartEnabled: Boolean
    ): Boolean {
        val source = sbn.notification

        if (isLikelyMediaPlaybackNotification(source) && !mediaPlaybackSmartEnabled) {
            return false
        }

        val packageNameLower = sbn.packageName.lowercase(Locale.ROOT)
        val allowTwoGisOverride = packageNameLower == TWO_GIS_PACKAGE
        if (parserDictionary.blockedSourcePackages.contains(packageNameLower) &&
            !allowTwoGisOverride
        ) {
            return false
        }

        return prefs.isPackageAllowed(sbn.packageName)
    }

    private fun passesCoreFilters(
        appPackageName: String,
        sbn: StatusBarNotification
    ): Boolean {
        val packageNameLower = sbn.packageName.lowercase(Locale.ROOT)
        val allowTwoGisGroupSummary = packageNameLower == TWO_GIS_PACKAGE
        if (appPackageName.isNotEmpty() && sbn.packageName == appPackageName) {
            return false
        }
        val source = sbn.notification
        if (isPermanentlyBlockedSource(sbn, source)) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            isMirrorNotificationChannel(source.channelId)
        ) {
            return false
        }
        if (Build.VERSION.SDK_INT >= 36 && source.flags and 0x40000 != 0) {
            return false
        }
        if (source.flags and Notification.FLAG_GROUP_SUMMARY != 0 &&
            !allowTwoGisGroupSummary
        ) {
            return false
        }
        return true
    }

    private fun isPermanentlyBlockedSource(
        sbn: StatusBarNotification,
        source: Notification
    ): Boolean {
        val candidates = buildList {
            add(sbn.packageName)
            add(sbn.key)
            sbn.tag?.let(::add)
            source.extras.getString(Notification.EXTRA_TEMPLATE)?.let(::add)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                source.channelId?.let(::add)
            }
        }
        return candidates.any { value ->
            value.lowercase(Locale.ROOT).contains(MEDIA_ONGOING_ACTIVITY_MARKER)
        }
    }

    private fun isNotificationCapsuleCandidate(
        context: Context,
        sbn: StatusBarNotification
    ): Boolean {
        val packageNameLower = sbn.packageName.lowercase(Locale.ROOT)
        if (sbn.packageName == context.packageName) {
            return false
        }
        if (packageNameLower in NOTIFICATION_CAPSULE_EXCLUDED_PACKAGES) {
            return false
        }

        val source = sbn.notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            isMirrorNotificationChannel(source.channelId)
        ) {
            return false
        }
        if (isLikelyMediaPlaybackNotification(source)) {
            return false
        }
        if (isNotificationCapsuleMirroredSource(sbn)) {
            return false
        }

        return true
    }

    private fun isNotificationCapsuleMirroredSource(sbn: StatusBarNotification): Boolean {
        return synchronized(stateLock) {
            sourceSnapshotsByMirrorKey.containsKey(sbn.key) ||
                callMirrorStates.containsKey(sbn.key) ||
                sbnToAggregateKey.containsKey(sbn.key) ||
                sbnToOtpAggregateKey.containsKey(sbn.key)
        }
    }

    private fun notificationCapsuleItemCount(notification: Notification): Int {
        val extras = notification.extras ?: return 1
        val messageCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            extras.getParcelableArray(Notification.EXTRA_MESSAGES)
                ?.let(Notification.MessagingStyle.Message::getMessagesFromBundleArray)
                ?.count { message -> !message.text.isNullOrBlank() } ?: 0
        } else {
            0
        }
        val lineCount = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.count { line -> !line.isNullOrBlank() } ?: 0

        return maxOf(messageCount, lineCount, 1)
    }

    private fun notificationCapsuleRecencyTime(sbn: StatusBarNotification): Long {
        val source = sbn.notification
        return latestNotificationCapsuleMessage(source)?.timestamp?.takeIf { it > 0L }
            ?: source.`when`.takeIf { it > 0L }
            ?: sbn.postTime
    }

    private fun latestNotificationCapsuleMessage(
        notification: Notification
    ): Notification.MessagingStyle.Message? {
        val messages = notificationCapsuleMessages(notification)
        return messages
            .mapIndexed { index, message -> index to message }
            .maxWithOrNull(
                compareBy<Pair<Int, Notification.MessagingStyle.Message>>(
                    { it.second.timestamp },
                    { it.first }
                )
            )
            ?.second
    }

    private fun notificationCapsuleMessages(
        notification: Notification
    ): List<Notification.MessagingStyle.Message> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return emptyList()
        }
        @Suppress("DEPRECATION")
        return notification.extras
            .getParcelableArray(Notification.EXTRA_MESSAGES)
            ?.let(Notification.MessagingStyle.Message::getMessagesFromBundleArray)
            ?.filter { message -> !message.text.isNullOrBlank() }
            .orEmpty()
    }

    private fun notificationCapsuleExpandedContent(
        context: Context,
        sources: List<StatusBarNotification>,
        fallbackTitle: String
    ): NotificationCapsuleExpandedContent {
        val latestSource = sources.maxByOrNull { sbn -> notificationCapsuleRecencyTime(sbn) }
        val extracted = latestSource?.let { sbn ->
            extractNotificationCapsuleExpandedContent(
                context = context,
                sbn = sbn,
                fallbackTitle = fallbackTitle
            )
        }
        val groupedMessageBody = if (latestSource != null && extracted != null) {
            notificationCapsuleGroupedMessageBody(
                sources = sources,
                latestSource = latestSource,
                latestTitle = extracted.title,
                fallbackTitle = fallbackTitle
            )
        } else {
            null
        }
        return extracted?.copy(body = groupedMessageBody ?: extracted.body)
            ?: NotificationCapsuleExpandedContent(
                title = fallbackTitle,
                body = null,
                image = null
            )
    }

    private fun extractNotificationCapsuleExpandedContent(
        context: Context,
        sbn: StatusBarNotification,
        fallbackTitle: String
    ): NotificationCapsuleExpandedContent? {
        val source = sbn.notification
        val extras = source.extras ?: return null
        val titleCandidates = linkedSetOf<String>()
        val bodyCandidates = linkedSetOf<String>()

        fun normalize(value: CharSequence?): String? {
            return NotificationTextNormalizer.normalize(value)
        }

        fun addTitle(value: CharSequence?) {
            normalize(value)?.let(titleCandidates::add)
        }

        fun addBody(value: CharSequence?) {
            normalize(value)?.let(bodyCandidates::add)
        }

        val latestMessage = latestNotificationCapsuleMessage(source)
        latestMessage?.let { message ->
            val sender = notificationCapsuleMessageSender(message)
            if (!sender.isNullOrBlank() &&
                !isEquivalentText(sender, fallbackTitle) &&
                titleCandidates.none { title -> isEquivalentText(sender, title) }
            ) {
                titleCandidates.add(sender)
            }
            addBody(message.text)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            addTitle(extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE))
        }
        addTitle(extras.getCharSequence(Notification.EXTRA_TITLE))
        addTitle(extras.getCharSequence(Notification.EXTRA_TITLE_BIG))

        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.asList()
            ?.asReversed()
            ?.forEach(::addBody)
        addBody(extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
        addBody(extras.getCharSequence(Notification.EXTRA_TEXT))
        addBody(extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
        addBody(extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT))
        addBody(extras.getCharSequence(Notification.EXTRA_INFO_TEXT))
        extractRemoteViewTexts(source).forEach(::addBody)
        extractRenderedRemoteViewTexts(
            context = context,
            notification = source,
            packageName = sbn.packageName
        ).forEach(::addBody)

        val title = titleCandidates.firstOrNull()
            ?: fallbackTitle.takeIf { it.isNotBlank() }
            ?: return null
        val body = bodyCandidates.firstOrNull { candidate ->
            !isEquivalentText(candidate, title) &&
                !isEquivalentText(candidate, fallbackTitle)
        } ?: bodyCandidates.firstOrNull { candidate ->
            !isEquivalentText(candidate, title)
        }

        return NotificationCapsuleExpandedContent(
            title = title,
            body = body,
            image = resolveNotificationCapsuleImageBitmap(context, sbn)
        )
    }

    private fun notificationCapsuleGroupedMessageBody(
        sources: List<StatusBarNotification>,
        latestSource: StatusBarNotification,
        latestTitle: String,
        fallbackTitle: String
    ): String? {
        val latestConversationKey = notificationCapsuleConversationKey(latestSource.notification)
            ?: latestTitle.takeUnless { title -> isEquivalentText(title, fallbackTitle) }
            ?: return null
        val lines = sources
            .flatMap(::notificationCapsuleMessageLines)
            .filter { line ->
                line.conversationKey?.let { key -> isEquivalentText(key, latestConversationKey) } == true
            }
            .filter { line ->
                !isEquivalentText(line.text, latestConversationKey) &&
                    !isEquivalentText(line.text, fallbackTitle)
            }
            .sortedWith(
                compareBy<NotificationCapsuleMessageLine>(
                    { it.timestampMs },
                    { it.sourcePostTimeMs },
                    { it.sourceKey },
                    { it.messageIndex }
                )
            )
        val uniqueLines = mutableListOf<NotificationCapsuleMessageLine>()
        val seen = mutableSetOf<String>()
        lines.forEach { line ->
            val key = "${line.timestampMs}\u0000${line.text}"
            if (seen.add(key)) {
                uniqueLines.add(line)
            }
        }
        val selectedLines = uniqueLines.takeLast(NOTIFICATION_CAPSULE_MAX_MESSAGE_LINES)
        if (selectedLines.size < 2) {
            return null
        }
        val latestLine = selectedLines.last()
        if (selectedLines.any { line -> !notificationCapsuleFitsSingleLine(line.text) }) {
            return latestLine.text
        }
        return selectedLines.joinToString("\n") { line -> line.text }
    }

    private fun notificationCapsuleMessageLines(
        sbn: StatusBarNotification
    ): List<NotificationCapsuleMessageLine> {
        val source = sbn.notification
        val sourceConversationKey = notificationCapsuleConversationKey(source)
        val messages = notificationCapsuleMessages(source)
        if (messages.isNotEmpty()) {
            return messages.mapIndexedNotNull { index, message ->
                val text = NotificationTextNormalizer.normalize(message.text) ?: return@mapIndexedNotNull null
                NotificationCapsuleMessageLine(
                    conversationKey = notificationCapsuleMessageSender(message) ?: sourceConversationKey,
                    text = text,
                    timestampMs = message.timestamp.takeIf { it > 0L }
                        ?: source.`when`.takeIf { it > 0L }
                        ?: sbn.postTime,
                    sourcePostTimeMs = sbn.postTime,
                    sourceKey = sbn.key,
                    messageIndex = index
                )
            }
        }

        val text = notificationCapsuleSingleLineText(source) ?: return emptyList()
        val conversationKey = sourceConversationKey ?: return emptyList()
        return listOf(
            NotificationCapsuleMessageLine(
                conversationKey = conversationKey,
                text = text,
                timestampMs = source.`when`.takeIf { it > 0L } ?: sbn.postTime,
                sourcePostTimeMs = sbn.postTime,
                sourceKey = sbn.key,
                messageIndex = 0
            )
        )
    }

    private fun notificationCapsuleMessageSender(
        message: Notification.MessagingStyle.Message
    ): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            NotificationTextNormalizer.normalize(message.senderPerson?.name)
        } else {
            @Suppress("DEPRECATION")
            NotificationTextNormalizer.normalize(message.sender)
        }
    }

    private fun notificationCapsuleConversationKey(notification: Notification): String? {
        latestNotificationCapsuleMessage(notification)
            ?.let(::notificationCapsuleMessageSender)
            ?.let { sender -> return sender }
        val extras = notification.extras ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            NotificationTextNormalizer.normalize(
                extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
            )?.let { return it }
        }
        return sequenceOf(
            extras.getCharSequence(Notification.EXTRA_TITLE),
            extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
        ).mapNotNull(NotificationTextNormalizer::normalize)
            .firstOrNull()
    }

    private fun notificationCapsuleSingleLineText(notification: Notification): String? {
        val extras = notification.extras ?: return null
        return sequenceOf(
            extras.getCharSequence(Notification.EXTRA_TEXT),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT),
            extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT),
            extras.getCharSequence(Notification.EXTRA_INFO_TEXT)
        ).mapNotNull(NotificationTextNormalizer::normalize)
            .firstOrNull()
    }

    private fun notificationCapsuleFitsSingleLine(text: String): Boolean {
        return notificationCapsuleGraphemeCount(text) <= NOTIFICATION_CAPSULE_SINGLE_LINE_GRAPHEME_LIMIT
    }

    private fun notificationCapsuleGraphemeCount(text: String): Int {
        val iterator = BreakIterator.getCharacterInstance(Locale.getDefault())
        iterator.setText(text)
        var count = 0
        while (iterator.next() != BreakIterator.DONE) {
            count += 1
        }
        return count
    }

    private fun resolveNotificationCapsuleImageBitmap(
        context: Context,
        sbn: StatusBarNotification
    ): Bitmap? {
        val source = sbn.notification
        val extras = source.extras
        sequenceOf(
            extras.get(Notification.EXTRA_PICTURE),
            extras.get(NOTIFICATION_EXTRA_PICTURE_ICON)
        ).forEach { value ->
            resolveNotificationBitmapValue(context, value)
                ?.let(::normalizeNotificationCapsuleImageBitmap)
                ?.let { return it }
        }
        return resolveSourceLargeIconBitmap(context, source)
            ?.let(::normalizeNotificationCapsuleImageBitmap)
    }

    private fun resolveNotificationBitmapValue(context: Context, value: Any?): Bitmap? {
        return when (value) {
            is Bitmap -> value
            is android.graphics.drawable.Icon -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    iconToBitmap(context, value)
                } else {
                    null
                }
            }
            is IconCompat -> runCatching {
                value.toIcon(context)
            }.getOrNull()?.let { iconToBitmap(context, it) }
            else -> null
        }
    }

    private fun normalizeNotificationCapsuleImageBitmap(bitmap: Bitmap): Bitmap? {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
            return null
        }
        val maxEdge = maxOf(bitmap.width, bitmap.height)
        if (maxEdge <= NOTIFICATION_CAPSULE_IMAGE_MAX_EDGE) {
            return bitmap
        }
        val scale = NOTIFICATION_CAPSULE_IMAGE_MAX_EDGE.toFloat() / maxEdge.toFloat()
        val width = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        return runCatching {
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        }.getOrNull()
    }

    private fun notifyNotificationCapsule(
        manager: NotificationManagerCompat,
        notificationId: Int,
        notification: Notification
    ) {
        runCatching {
            manager.notify(notificationId, notification)
        }.onSuccess {
            synchronized(stateLock) {
                notificationCapsuleIds.add(notificationId)
            }
        }.onFailure { error ->
            Log.e(TAG, "Failed to post notification capsule", error)
        }
    }

    private fun cancelStaleNotificationCapsules(
        context: Context,
        desiredIds: Set<Int>
    ) {
        val staleIds = synchronized(stateLock) {
            notificationCapsuleIds.toMutableSet()
        }
        staleIds.addAll(activeNotificationCapsuleIds(context))
        staleIds.removeAll(desiredIds)

        val manager = NotificationManagerCompat.from(context)
        staleIds.forEach { notificationId ->
            runCatching {
                manager.cancel(notificationId)
            }.onFailure { error ->
                Log.e(TAG, "Failed to cancel stale notification capsule", error)
            }
        }

        synchronized(stateLock) {
            notificationCapsuleIds.clear()
            notificationCapsuleIds.addAll(desiredIds)
        }
    }

    private fun activeNotificationCapsuleIds(context: Context): Set<Int> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return emptySet()
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return runCatching {
            notificationManager.activeNotifications
                .asSequence()
                .filter { sbn -> sbn.packageName == context.packageName }
                .filter { sbn ->
                    sbn.notification.channelId == MirrorNotificationChannel.NOTIFICATION_CAPSULE.id
                }
                .map { sbn -> sbn.id }
                .toSet()
        }.getOrDefault(emptySet())
    }

    private fun notificationCapsuleIdForPackage(packageName: String): Int {
        val id = mirrorIdForKey(
            "$NOTIFICATION_CAPSULE_KEY_PREFIX${packageName.lowercase(Locale.ROOT)}"
        )
        return if (id == NOTIFICATION_CAPSULE_ID) {
            NOTIFICATION_CAPSULE_ID + 1
        } else {
            id
        }
    }

    private fun notificationCapsuleClearPendingIntent(
        context: Context,
        notificationId: Int,
        sourceKeys: List<String>
    ): PendingIntent {
        val normalizedSourceKeys = sourceKeys
            .map { key -> key.trim() }
            .filter { key -> key.isNotBlank() }
        val intent = Intent(context, NotificationCapsuleActionReceiver::class.java).apply {
            action = NotificationCapsuleActionReceiver.ACTION_CLEAR_NOTIFICATION_KEYS
            putStringArrayListExtra(
                NotificationCapsuleActionReceiver.EXTRA_NOTIFICATION_KEYS,
                ArrayList(normalizedSourceKeys)
            )
        }
        return PendingIntent.getBroadcast(
            context,
            mirrorIdForKey("$NOTIFICATION_CAPSULE_KEY_PREFIX$notificationId:clear"),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun notificationCapsuleSourceContentIntent(
        sources: List<StatusBarNotification>
    ): PendingIntent? {
        return sources
            .sortedByDescending { sbn -> notificationCapsuleRecencyTime(sbn) }
            .firstNotNullOfOrNull { sbn -> sbn.notification.contentIntent }
    }

    private fun buildNotificationCapsuleClearAction(
        context: Context,
        notificationId: Int,
        pendingIntent: PendingIntent
    ): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            IconCompat.createWithResource(context, R.drawable.ic_clear_24),
            notificationCapsuleClearActionText(context),
            pendingIntent
        ).addExtras(
            Bundle().apply {
                putString(
                    NOWBAR_EXTRA_ACTION_ID,
                    "$NOTIFICATION_CAPSULE_CLEAR_ACTION_ID:$notificationId"
                )
                putString(NOWBAR_EXTRA_ACTION_SEMANTIC, NOWBAR_ACTION_SEMANTIC_DELETE)
            }
        ).build()
    }

    private fun buildNotificationCapsuleDeleteExtras(
        pendingIntent: PendingIntent
    ): Bundle {
        return Bundle().apply {
            putInt(SAMSUNG_EXTRA_ACTION_TYPE, 1)
            putInt(SAMSUNG_EXTRA_ACTION_PRIMARY_SET, 0)
            putParcelable(NOWBAR_EXTRA_DELETE_INTENT, pendingIntent)
            putBoolean(NOWBAR_EXTRA_DISMISSIBLE, true)
        }
    }

    private fun buildNotificationCapsuleNotification(
        context: Context,
        title: String,
        description: String,
        expandedTitle: String = title,
        expandedDescription: String = description,
        expandedImage: Bitmap? = null,
        postTime: Long,
        notificationId: Int,
        smallIcon: IconCompat,
        largeIcon: Bitmap?,
        contentIntent: PendingIntent? = null,
        clearSourceKeys: List<String>,
        showClearAction: Boolean
    ): Notification {
        val builder = NotificationCompat.Builder(
            context,
            MirrorNotificationChannel.NOTIFICATION_CAPSULE.id
        )
            .setContentTitle(title)
            .setContentText(description)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setDefaults(0)
            .setOngoing(true)
            .setAutoCancel(false)
            .setWhen(postTime)
            .setShowWhen(false)
            .setColor(NOTIFICATION_CAPSULE_CHIP_COLOR)
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setRequestPromotedOngoing(true)
            .setSmallIcon(smallIcon)

        largeIcon?.let(builder::setLargeIcon)
        (contentIntent ?: notificationCapsuleContentIntent(context, notificationId))
            ?.let(builder::setContentIntent)

        if (description.isNotBlank()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(description))
        }
        val expandedRightIcon = expandedImage?.let { image ->
            runCatching { IconCompat.createWithBitmap(image) }.getOrNull()
        }
        val clearPendingIntent = if (clearSourceKeys.isNotEmpty()) {
            notificationCapsuleClearPendingIntent(context, notificationId, clearSourceKeys)
        } else {
            null
        }
        if (showClearAction) {
            clearPendingIntent?.let { pendingIntent ->
                builder.addAction(
                    buildNotificationCapsuleClearAction(
                        context = context,
                        notificationId = notificationId,
                        pendingIntent = pendingIntent
                    )
                )
            }
        }

        @Suppress("DEPRECATION")
        val bridgeSource = Notification().apply {
            color = NOTIFICATION_CAPSULE_CHIP_COLOR
            extras = Bundle()
        }
        SamsungLiveUpdateReparser(context).applyNowBarBridge(
            builder = builder,
            source = bridgeSource,
            sourcePackageName = context.packageName,
            primaryText = expandedTitle,
            secondaryText = expandedDescription,
            nowBarPrimaryText = title,
            nowBarSecondaryText = description,
            chipText = title,
            chipIcon = smallIcon,
            nowBarIcon = smallIcon,
            rightIcon = expandedRightIcon,
            suppressSourceRemoteViews = true,
            suppressSourceNowBarRemoteView = true,
            hasProgress = false,
            progressValue = 0,
            progressMax = 0,
            showSecondaryInNowBar = description.isNotBlank(),
            disableNowBarRemoteView = true,
            reuseNotificationRemoteViews = false,
            lockscreenOnly = true
        )
        clearPendingIntent?.let { pendingIntent ->
            builder.setDeleteIntent(pendingIntent)
            builder.addExtras(buildNotificationCapsuleDeleteExtras(pendingIntent))
        }

        return SamsungOneUi7NowBarCompat.markEligible(builder.build())
    }

    private fun notificationCapsuleContentIntent(
        context: Context,
        notificationId: Int
    ): PendingIntent? {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return null
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun notificationCapsuleTitle(context: Context, count: Int): String {
        return when (appLocale(context)) {
            AppLocale.RU -> "$count ${russianNotificationWord(count)}"
            AppLocale.TR -> "$count bildirim"
            AppLocale.PT_BR -> if (count == 1) {
                "1 notificação"
            } else {
                "$count notificações"
            }
            AppLocale.ZH_HANS -> "$count 条通知"
            AppLocale.ZH_HANT -> "$count 則通知"
            AppLocale.KO -> "알림 ${count}개"
            AppLocale.EN -> if (count == 1) {
                "1 notification"
            } else {
                "$count notifications"
            }
        }
    }

    private fun notificationCapsuleAppLabel(context: Context, packageName: String): String {
        val normalizedPackageName = packageName.trim()
        val packageLabel = runCatching {
            val appInfo = context.packageManager.getApplicationInfo(normalizedPackageName, 0)
            context.packageManager.getApplicationLabel(appInfo)
                ?.toString()
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }.getOrNull()
        if (!packageLabel.isNullOrBlank() &&
            !packageLabel.equals(normalizedPackageName, ignoreCase = true)
        ) {
            return packageLabel
        }
        return packageLabel ?: normalizedPackageName.ifBlank {
            notificationCapsuleSystemLabel(context)
        }
    }

    private fun isPrivacyRedactedNotification(
        source: Notification,
        parserDictionary: LiveParserDictionary
    ): Boolean {
        val contentTexts = collectNotificationContentTexts(source)
        if (contentTexts.isEmpty()) {
            return false
        }
        val placeholders = parserDictionary.privacyRedactionPlaceholders
            .ifEmpty { FALLBACK_PRIVACY_REDACTION_PLACEHOLDERS }

        return contentTexts.any { text ->
            isPrivacyRedactionPlaceholder(text, placeholders)
        }
    }

    private fun collectNotificationContentTexts(source: Notification): List<String> {
        val extras = source.extras
        val parts = mutableListOf<String>()

        fun add(value: CharSequence?) {
            val text = value?.toString()?.trim().orEmpty()
            if (text.isNotBlank()) {
                parts.add(text)
            }
        }

        add(extras.getCharSequence(Notification.EXTRA_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_INFO_TEXT))
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach(::add)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            extras.getParcelableArray(Notification.EXTRA_MESSAGES)
                ?.let(Notification.MessagingStyle.Message::getMessagesFromBundleArray)
                ?.mapNotNull { it.text }
                ?.forEach(::add)
        }

        return parts.distinct()
    }

    private fun isPrivacyRedactionPlaceholder(text: String, placeholders: Set<String>): Boolean {
        val normalized = text
            .trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")

        return placeholders.any { placeholder ->
            val normalizedPlaceholder = placeholder
                .trim()
                .lowercase(Locale.ROOT)
                .replace(Regex("\\s+"), " ")
            normalizedPlaceholder.isNotBlank() &&
                    (normalized == normalizedPlaceholder ||
                            normalized.contains(normalizedPlaceholder))
        }
    }

    private fun hiddenSamsungBridgeTexts(): SamsungBridgeTexts {
        return SamsungBridgeTexts(
            shouldClearContentText = false,
            secondaryText = "",
            chipText = LOCKSCREEN_CONTENT_HIDDEN_TEXT,
            nowBarPrimaryText = LOCKSCREEN_CONTENT_HIDDEN_TEXT,
            nowBarSecondaryText = null,
            showSecondaryInNowBar = false,
            preferCompactNowBarRemoteView = false,
            disableNowBarRemoteView = true,
            disableMiniRemoteView = false,
            showMiniIcon = true,
            showSmallIcon = true,
            allowNowBarProgress = false,
            keepCollapsedRemoteView = false,
            preferExpandedRemoteBody = false,
            reuseNotificationRemoteViews = false
        )
    }

    private fun isDeviceShowingLockscreen(context: Context): Boolean {
        val keyguardManager =
            context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            keyguardManager.isDeviceLocked || keyguardManager.isKeyguardLocked
        } else {
            keyguardManager.isKeyguardLocked
        }
    }

    private fun buildMirroredNotification(
        context: Context,
        sbn: StatusBarNotification,
        appPresentationOverride: AppPresentationOverride,
        mirrorChannel: MirrorNotificationChannel,
        progressOverride: ProgressOverride?,
        otpOverride: OtpMatch?,
        smartShortTextOverride: String?,
        compactCodeOverride: String? = null,
        smartRuleId: String? = null,
        requestPromoted: Boolean,
        otpShortTextOverride: String? = null,
        samsungBridge: SamsungBridgeContext = SamsungBridgeContext.disabled(
            sourceHasNativeProgress = false
        ),
        allowNavigationIconHeuristics: Boolean = true,
        preferMediaControls: Boolean = false,
        mediaPlaybackIsPlaying: Boolean? = null,
        titleOverride: String? = null,
        textOverride: String? = null,
        largeIconOverride: Bitmap? = null,
        preferSmartShortTextAsPrimary: Boolean = false,
        callMirrorActive: Boolean = false,
        callChronometerStartWallClockMs: Long? = null
    ): Notification {
        val runtimePrefs = ConverterPrefs(context)
        val parserDictionary = LiveParserDictionaryLoader.get(context, runtimePrefs)
        val source = sbn.notification
        val samsungReparse = samsungBridge.reparsePayload
        val sourceSmallIcon = resolveSourceSmallIcon(context, sbn)
        val appIconAssets = resolveAppIconAssets(context, sbn.packageName)
        val appSmallIcon = appIconAssets?.smallIcon
        val samsungSmallIcon = samsungReparse?.icon
        val samsungLargeIcon = samsungReparse?.largeIconBitmap
        val remoteDrawableAssets = resolveRemoteDrawableAssets(context, sbn)
        val sourcePackageNameLower = sbn.packageName.lowercase(Locale.ROOT)
        val isTwoGisPackage = sourcePackageNameLower == TWO_GIS_PACKAGE
        val isYandexMapsPackage = sourcePackageNameLower == YANDEX_MAPS_PACKAGE
        val isYangoMapsPackage = sourcePackageNameLower == YANGO_MAPS_PACKAGE
        val isYandexMapsLikePackage = isYandexMapsPackage || isYangoMapsPackage
        val isSamsungTwoGis =
            samsungBridge.enabled &&
                    samsungBridge.hasCustomRemoteCard &&
                    isTwoGisPackage
        val effectiveIconSource =
            if (DeviceProps.isSamsungOneUi7Android15() && appPresentationOverride.isDefault()) {
                NotificationIconSource.NOTIFICATION
            } else {
                appPresentationOverride.iconSource
            }
        val shouldTryNavigationArrowIcon =
            (effectiveIconSource == NotificationIconSource.NOTIFICATION ||
                    isTwoGisPackage ||
                    isYandexMapsLikePackage) &&
                    (smartRuleId == "navigation" ||
                            isTwoGisPackage ||
                            isYandexMapsLikePackage ||
                            (allowNavigationIconHeuristics &&
                                    isLikelyNavigationPackage(sbn.packageName, parserDictionary)))
        val navigationDrawable =
            if (shouldTryNavigationArrowIcon) {
                remoteDrawableAssets
            } else {
                null
            }
        val sourceLargeIcon = resolveSourceLargeIconBitmap(context, source)
        val preferredLargeIcon = when {
            largeIconOverride != null -> largeIconOverride
            shouldTryNavigationArrowIcon && !isYandexMapsLikePackage ->
                navigationDrawable?.bitmap ?: samsungLargeIcon ?: sourceLargeIcon
            else ->
                samsungLargeIcon ?: sourceLargeIcon
        }
        val preferredPrimaryIcon = when (effectiveIconSource) {
            NotificationIconSource.NOTIFICATION -> when {
                shouldTryNavigationArrowIcon ->
                    navigationDrawable?.icon ?: sourceSmallIcon ?: samsungSmallIcon ?: appSmallIcon
                samsungBridge.hasCustomRemoteCard ->
                    samsungSmallIcon ?: sourceSmallIcon ?: appSmallIcon
                else ->
                    sourceSmallIcon ?: samsungSmallIcon ?: appSmallIcon
            }
            NotificationIconSource.APP ->
                appSmallIcon ?: sourceSmallIcon ?: samsungSmallIcon
        }
        val preferredChipIcon = when {
            isYandexMapsLikePackage ->
                navigationDrawable?.icon ?: samsungSmallIcon ?: sourceSmallIcon ?: appSmallIcon
            shouldTryNavigationArrowIcon ->
                navigationDrawable?.icon ?: sourceSmallIcon ?: samsungSmallIcon ?: appSmallIcon
            samsungBridge.hasCustomRemoteCard ->
                samsungSmallIcon ?: sourceSmallIcon ?: appSmallIcon
            else ->
                sourceSmallIcon ?: samsungSmallIcon ?: appSmallIcon
        }
        val nowBarRightIcon = if (samsungBridge.hasCustomRemoteCard && !isSamsungTwoGis) {
            null
        } else {
            if (isSamsungTwoGis) {
                navigationDrawable?.icon
                    ?: samsungReparse?.rightIcon
                    ?: remoteDrawableAssets?.icon
                    ?: preferredLargeIcon?.let { bitmap ->
                        runCatching { IconCompat.createWithBitmap(bitmap) }.getOrNull()
                    }
            } else {
                samsungReparse?.rightIcon
                    ?: remoteDrawableAssets?.icon
                    ?: preferredLargeIcon?.let { bitmap ->
                        runCatching { IconCompat.createWithBitmap(bitmap) }.getOrNull()
                    }
            }
        }
        val appName = resolveAppName(context, sbn.packageName)
        val allowRemoteViewTextFallback = shouldTryNavigationArrowIcon
        val sourceNotificationTitle = extractTitle(source, appName, allowRemoteViewTextFallback)
        val baseTitle = titleOverride?.takeIf { it.isNotBlank() }
            ?: if (smartRuleId == "vpn") {
                sourceNotificationTitle.takeIf { it.isNotBlank() }
                    ?: samsungReparse?.title?.takeIf { it.isNotBlank() }
                    ?: appName
            } else {
                samsungReparse?.title?.takeIf { it.isNotBlank() }
                    ?: sourceNotificationTitle
            }
        val sourceNotificationText = extractText(source, allowRemoteViewTextFallback)
        val baseText = textOverride?.takeIf { it.isNotBlank() }
            ?: if (smartRuleId == "vpn") {
                sourceNotificationText
                    .takeIf { !isGeneratedCallBodyFallback(it) }
                    ?: samsungReparse?.text?.takeIf { it.isNotBlank() }
                    ?: sourceNotificationText
            } else {
                samsungReparse?.text?.takeIf { it.isNotBlank() }
                    ?: sourceNotificationText
            }
        val nonSamsungTwoGisTextPair = if (
            !samsungBridge.enabled &&
            isTwoGisPackage
        ) {
            resolveTwoGisRemoteViewMiniTextPair(
                notification = source,
                displayTitle = baseTitle,
                displayText = baseText,
                parserDictionary = parserDictionary
            )
        } else {
            null
        }
        val title = nonSamsungTwoGisTextPair?.primaryText
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: baseTitle
        val text = nonSamsungTwoGisTextPair?.secondaryText
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: baseText
        val configuredDisplayTitle = if (appPresentationOverride.usesExplicitSources()) {
            when (appPresentationOverride.resolvedTitleSource()) {
                NotificationTitleSource.NOTIFICATION_TITLE -> title.ifBlank { appName }
                NotificationTitleSource.APP_TITLE -> appName.ifBlank { title }
            }
        } else {
            when (appPresentationOverride.compactTextSource) {
                CompactTextSource.TEXT -> text.ifBlank { title }
                CompactTextSource.TITLE -> title
            }
        }
        val configuredDisplayText = if (appPresentationOverride.usesExplicitSources()) {
            when (appPresentationOverride.resolvedContentSource()) {
                NotificationContentSource.NOTIFICATION_TEXT -> text.ifBlank { title }
                NotificationContentSource.NOTIFICATION_TITLE -> title.ifBlank { text }
            }
        } else if (
            appPresentationOverride.compactTextSource == CompactTextSource.TEXT &&
            title.isNotBlank() &&
            title != configuredDisplayTitle
        ) {
            title
        } else {
            text
        }
        val displayTitle = if (preferMediaControls) {
            title.takeIfMeaningfulMediaPlaybackText()
                ?: configuredDisplayTitle.takeIfMeaningfulMediaPlaybackText()
                ?: appName
        } else {
            configuredDisplayTitle
        }
        val displayText = if (preferMediaControls) {
            text.takeIfMeaningfulMediaPlaybackText()
                ?: configuredDisplayText.takeIfMeaningfulMediaPlaybackText()
                ?: ""
        } else {
            configuredDisplayText
        }
        val nativeInCallMirror = callMirrorActive && isNativeInCallNotification(sbn)
        val callMirrorBodyText = if (callMirrorActive) {
            resolveCallMirrorBodyText(
                notification = source,
                displayTitle = displayTitle,
                displayText = displayText,
                includeRemoteViewTexts = !nativeInCallMirror
            )
        } else {
            null
        }
        val samsungPolicyDisplayText = if (callMirrorActive) {
            val bodyText = callMirrorBodyText?.trim().orEmpty()
            when {
                bodyText.isNotEmpty() && !isEquivalentText(bodyText, displayTitle) -> bodyText
                !isGeneratedCallBodyFallback(displayText) -> displayText
                else -> ""
            }
        } else {
            displayText
        }
        val otpPresentationText = otpShortTextOverride ?: otpOverride?.code
        val contentTitle = otpPresentationText ?: displayTitle
        val contentText = if (otpOverride != null) {
            appName
        } else if (callMirrorBodyText != null) {
            callMirrorBodyText
        } else {
            displayText
        }
        val hideLockscreenContent = runtimePrefs.getHideLockscreenContentEnabled()
        val redactSamsungNowBarContent =
            hideLockscreenContent && isDeviceShowingLockscreen(context)
        val visibility = when {
            preferMediaControls &&
                    !runtimePrefs.getSmartMediaPlaybackShowOnLockScreen() ->
                NotificationCompat.VISIBILITY_SECRET

            hideLockscreenContent -> NotificationCompat.VISIBILITY_PRIVATE
            else -> NotificationCompat.VISIBILITY_PUBLIC
        }
        val useMediaActionSymbols = preferMediaControls &&
                runtimePrefs.getSmartMediaPlaybackUseSymbolsInPlayer()
        val compactPrimaryText = sequenceOf(
            otpShortTextOverride?.trim(),
            otpOverride?.code?.trim(),
            compactCodeOverride?.trim(),
            displayTitle.trim()
        ).firstOrNull { !it.isNullOrEmpty() } ?: displayTitle
        val aospCuttingEnabled = runtimePrefs.getAospCuttingEnabled()
        val aospCuttingLength = runtimePrefs.getAospCuttingLength()
        val hyperBridgeEnabled = runtimePrefs.getHyperBridgeEnabled()
        val smartLockscreenOnly = when (smartRuleId) {
            "weather" -> runtimePrefs.getSmartWeatherLockscreenOnly()
            "vpn" -> runtimePrefs.getSmartVpnLockscreenOnly()
            else -> false
        }
        val callChronometerStart = callChronometerStartWallClockMs
            ?.takeIf { callMirrorActive && it > 0L }
            ?.coerceAtMost(System.currentTimeMillis())
        val suppressCallNowBarRemoteView = callChronometerStart != null

        val sourceHasProgress = hasEffectiveProgress(sbn.packageName, source)
        val samsungProgressMax = samsungReparse?.progressMax ?: 0
        val samsungProgressValue = samsungReparse?.progressValue ?: 0
        val progressMax = when {
            sourceHasProgress -> source.extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
            progressOverride != null -> progressOverride.max
            else -> samsungProgressMax
        }
        val progressValue = when {
            sourceHasProgress -> source.extras.getInt(Notification.EXTRA_PROGRESS, 0)
            progressOverride != null -> progressOverride.value
            else -> samsungProgressValue
        }
        val indeterminate = when {
            sourceHasProgress ->
                source.extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)
            progressOverride != null -> false
            else -> false
        }
        val hasProgress = sourceHasProgress || progressOverride != null || samsungProgressMax > 0
        val suppressFrameworkProgressBody = isTwoGisPackage
        var resolvedProgressChipText: String? = null
        val customNotificationColor = appPresentationOverride.effectiveNotificationColorArgb()
        val sourceChipAccentColor = if (samsungBridge.enabled) {
            SamsungLiveUpdateReparser.resolveChipBackgroundColor(source)
        } else {
            progressColor
        }
        val builderAccentColor = customNotificationColor ?: sourceChipAccentColor
        val determinateProgressPercent = if (hasProgress && !indeterminate && progressMax > 0) {
            val safeMax = progressMax.coerceAtLeast(1)
            val safeProgress = progressValue.coerceIn(0, safeMax)
            ((safeProgress.toFloat() / safeMax.toFloat()) * 100f)
                .roundToInt()
                .coerceIn(0, 100)
        } else {
            null
        }
        val samsungRemoteViewMiniTextPair = if (isTwoGisPackage) {
            resolveTwoGisRemoteViewMiniTextPair(
                notification = source,
                displayTitle = displayTitle,
                displayText = displayText,
                parserDictionary = parserDictionary,
                preferInstructionPrimary = true
            )
        } else {
            null
        }
        val samsungTwoGisEtaDistanceText = if (isTwoGisPackage) {
            extractTwoGisEtaDistanceText(
                notification = source,
                displayTitle = displayTitle,
                displayText = displayText,
                parserDictionary = parserDictionary
            )
        } else {
            null
        }
        val samsungTwoGisMainTitleText = if (isTwoGisPackage) {
            samsungRemoteViewMiniTextPair?.primaryText
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: compactPrimaryText.trim().takeIf { it.isNotEmpty() }
        } else {
            null
        }
        val samsungTwoGisTurnDistanceText = if (isTwoGisPackage) {
            extractNavigationDistanceText(
                notification = source,
                fallbackTitle = displayTitle,
                parserDictionary = parserDictionary
            )
                ?: samsungRemoteViewMiniTextPair?.secondaryText
                    ?.trim()
                    ?.takeIf { candidate ->
                        candidate.isNotEmpty() &&
                                isNavigationDistanceText(candidate, parserDictionary)
                    }
        } else {
            null
        }
        val samsungTwoGisPrimaryText = if (isTwoGisPackage) {
            sequenceOf(
                samsungTwoGisTurnDistanceText?.trim()?.takeIf { it.isNotEmpty() },
                samsungTwoGisMainTitleText?.trim()?.takeIf { it.isNotEmpty() }
            ).firstOrNull { !it.isNullOrEmpty() }
        } else {
            null
        }
        val samsungTwoGisSecondaryTitleText = if (isTwoGisPackage) {
            samsungTwoGisMainTitleText
                ?.trim()
                ?.takeIf { title ->
                    title.isNotEmpty() &&
                            !isEquivalentText(title, samsungTwoGisPrimaryText.orEmpty())
                }
        } else {
            null
        }
        val samsungTwoGisVisibleSecondaryText = if (isTwoGisPackage) {
            composeTwoGisVisibleSecondaryText(
                leadingText = samsungTwoGisSecondaryTitleText,
                etaDistanceText = samsungTwoGisEtaDistanceText,
                fallbackText = displayText
            )
        } else {
            null
        }

        val builder = NotificationCompat.Builder(context, mirrorChannel.id)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setSubText(appName)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setDefaults(0)
            .setOngoing(true)
            .setAutoCancel(false)
            .setWhen(callChronometerStart ?: resolveStableWhen(source, sbn.postTime))
            .setShowWhen(callChronometerStart != null)
            .setColor(builderAccentColor)
            .setCategory(
                if (callMirrorActive) {
                    Notification.CATEGORY_CALL
                } else if (hasProgress && !suppressFrameworkProgressBody) {
                    Notification.CATEGORY_PROGRESS
                } else {
                    Notification.CATEGORY_STATUS
                }
            )
            .setVisibility(visibility)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (callChronometerStart != null) {
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(false)
        }

        applySmallIcon(context, builder, preferredPrimaryIcon)
        preferredLargeIcon?.let(builder::setLargeIcon)

        if (hideLockscreenContent && visibility == NotificationCompat.VISIBILITY_PRIVATE) {
            val publicBuilder = NotificationCompat.Builder(context, mirrorChannel.id)
                .setContentTitle(LOCKSCREEN_CONTENT_HIDDEN_TEXT)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setDefaults(0)
                .setOngoing(true)
                .setAutoCancel(false)
                .setWhen(resolveStableWhen(source, sbn.postTime))
                .setShowWhen(false)
                .setColor(builderAccentColor)
                .setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
            applySmallIcon(context, publicBuilder, preferredPrimaryIcon)
            if (samsungBridge.enabled) {
                val hiddenTexts = hiddenSamsungBridgeTexts()
                SamsungNowBarApplier.apply(
                    context = context,
                    builder = publicBuilder,
                    source = source,
                    sourcePackageName = sbn.packageName,
                    primaryText = LOCKSCREEN_CONTENT_HIDDEN_TEXT,
                    texts = hiddenTexts,
                    chipIcon = preferredChipIcon,
                    nowBarIcon = preferredPrimaryIcon,
                    rightIcon = null,
                    suppressChipExpandedText = false,
                    suppressSourceRemoteViews = true,
                    suppressSourceNowBarRemoteView = true,
                    lockscreenOnly = smartLockscreenOnly,
                    hasProgress = false,
                    progressValue = 0,
                    progressMax = 0
                )
            }
            builder.setPublicVersion(publicBuilder.build())
        }

        if (requestPromoted) {
            builder.setRequestPromotedOngoing(true)
        }

        if (otpOverride != null) {
            builder.addAction(buildCopyOtpAction(context, sbn, otpOverride.code))
        }

        source.contentIntent?.let(builder::setContentIntent)
        copySourceActions(
            source = source,
            builder = builder,
            maxActions = if (otpOverride != null) {
                MAX_MIRRORED_ACTIONS - 1
            } else {
                MAX_MIRRORED_ACTIONS
            },
            preferMediaControls = preferMediaControls,
            mediaPlaybackIsPlaying = mediaPlaybackIsPlaying,
            useMediaActionSymbols = useMediaActionSymbols
        )

        if (hasProgress) {
            if (indeterminate || progressMax <= 0) {
                if (!suppressFrameworkProgressBody) {
                    builder.setProgress(0, 0, true)
                    builder.setStyle(
                        NotificationCompat.ProgressStyle()
                            .setProgressIndeterminate(true)
                            .setStyledByProgress(true)
                    )
                } else {
                    builder.setStyle(NotificationCompat.BigTextStyle().bigText(text))
                }
            } else {
                val safeMax = progressMax.coerceAtLeast(1)
                val safeProgress = progressValue.coerceIn(0, safeMax)
                val percent = determinateProgressPercent ?: 0

                if (!suppressFrameworkProgressBody) {
                    builder.setProgress(safeMax, safeProgress, false)
                    builder.setStyle(
                        NotificationCompat.ProgressStyle()
                            .setProgress(percent)
                            .setStyledByProgress(true)
                    )
                } else {
                    builder.setStyle(NotificationCompat.BigTextStyle().bigText(text))
                }
                resolvedProgressChipText = if (sourcePackageNameLower == TWO_GIS_PACKAGE) {
                    samsungTwoGisTurnDistanceText?.trim()?.takeIf { it.isNotEmpty() }
                        ?: smartShortTextOverride?.trim()?.takeIf { it.isNotEmpty() }
                        ?: samsungRemoteViewMiniTextPair?.secondaryText?.trim()
                            ?.takeIf { it.isNotEmpty() }
                        ?: samsungRemoteViewMiniTextPair?.primaryText?.trim()
                            ?.takeIf { it.isNotEmpty() }
                        ?: smartShortTextOverride
                        ?: "$percent%"
                } else {
                    smartShortTextOverride ?: "$percent%"
                }
                val progressShortText = if (preferMediaControls) {
                    smartShortTextOverride.takeIfMeaningfulMediaPlaybackText()
                        ?: displayTitle.takeIfMeaningfulMediaPlaybackText()
                        ?: displayText.takeIfMeaningfulMediaPlaybackText()
                        ?: appName
                } else {
                    resolvedProgressChipText ?: smartShortTextOverride ?: "$percent%"
                }
                builder.setShortCriticalText(
                    limitIslandText(
                        progressShortText,
                        aospCuttingEnabled,
                        aospCuttingLength
                    )
                )
            }
        } else if (otpOverride != null) {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(contentTitle)
                    .bigText(text)
            )
            builder.setShortCriticalText(
                limitIslandText(
                    otpPresentationText ?: otpOverride.code,
                    aospCuttingEnabled,
                    aospCuttingLength
                )
            )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(callMirrorBodyText ?: text))
        }
        if (smartShortTextOverride != null && !hasProgress && smartRuleId != "vpn") {
            if (!preferSmartShortTextAsPrimary) {
                builder.setContentText(smartShortTextOverride)
            }
        }
        if (callChronometerStart != null && !hasProgress) {
            builder.setShortCriticalText(
                limitIslandText(
                    formatMillisecondsAsClock(System.currentTimeMillis() - callChronometerStart),
                    aospCuttingEnabled,
                    aospCuttingLength
                )
            )
        }
        if (smartShortTextOverride != null && !hasProgress) {
            builder.setShortCriticalText(
                limitIslandText(
                    smartShortTextOverride,
                    aospCuttingEnabled,
                    aospCuttingLength
                )
            )
        }

        if (isTwoGisPackage) {
            val bodyTitle = samsungTwoGisPrimaryText
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: compactPrimaryText.trim()
            val bodySubtitle = samsungTwoGisSecondaryTitleText
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            val bodyBottomText = samsungTwoGisVisibleSecondaryText
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

            builder.setContentTitle(bodyTitle)
            if (bodyBottomText != null) {
                builder.setContentText(bodyBottomText)
            }

            val bodyBigText = sequenceOf(
                bodySubtitle?.takeIf { subtitle ->
                    bodyBottomText == null || !bodyBottomText.startsWith(subtitle)
                },
                bodyBottomText
            ).filterNotNull()
                .distinct()
                .joinToString(separator = "\n")
                .trim()
                .ifBlank { null }

            if (bodyBigText != null) {
                builder.setStyle(NotificationCompat.BigTextStyle().bigText(bodyBigText))
            }
        }

        if (samsungBridge.enabled) {
            val samsungTexts = if (redactSamsungNowBarContent) {
                hiddenSamsungBridgeTexts()
            } else {
                SamsungBridgeContentPolicy.resolve(
                    sourcePackageName = sbn.packageName,
                    hasCustomRemoteCard = samsungBridge.hasCustomRemoteCard,
                    hasProgress = hasProgress,
                    smartRuleId = smartRuleId,
                    smartShortTextOverride = smartShortTextOverride,
                    displayText = samsungPolicyDisplayText,
                    compactPrimaryText = compactPrimaryText,
                    resolvedProgressChipText = resolvedProgressChipText,
                    otpShortTextOverride = otpShortTextOverride,
                    otpCode = otpOverride?.code,
                    compactCodeOverride = compactCodeOverride,
                    samsungReparseChipText = samsungReparse?.chipText,
                    remoteViewMiniTextPair = samsungRemoteViewMiniTextPair,
                    twoGisPrimaryText = samsungTwoGisPrimaryText,
                    twoGisEtaDistanceText = samsungTwoGisEtaDistanceText,
                    twoGisVisibleSecondaryText = samsungTwoGisVisibleSecondaryText,
                    preferSmartShortTextAsPrimary = preferSmartShortTextAsPrimary
                ).let { texts ->
                    if (callMirrorActive) {
                        texts.copy(shouldClearContentText = false)
                    } else {
                        texts
                    }
                }
            }
            val samsungNowBarHasProgress = hasProgress && !redactSamsungNowBarContent
            val samsungNowBarProgressValue = if (redactSamsungNowBarContent) {
                0
            } else {
                progressValue
            }
            val samsungNowBarProgressMax = if (redactSamsungNowBarContent) {
                0
            } else {
                progressMax
            }
            val samsungNowBarRightIcon = if (redactSamsungNowBarContent) {
                null
            } else {
                nowBarRightIcon
            }
            val suppressSamsungSourceRemoteViews = redactSamsungNowBarContent
            val suppressSamsungNowBarRemoteView = if (redactSamsungNowBarContent) {
                true
            } else {
                suppressCallNowBarRemoteView
            }
            val suppressSamsungChipExpandedText = if (redactSamsungNowBarContent) {
                false
            } else {
                callChronometerStart != null
            }
            SamsungNowBarApplier.apply(
                context = context,
                builder = builder,
                source = source,
                sourcePackageName = sbn.packageName,
                primaryText = samsungTexts.nowBarPrimaryText,
                texts = samsungTexts,
                chipIcon = preferredChipIcon,
                nowBarIcon = preferredPrimaryIcon,
                rightIcon = samsungNowBarRightIcon,
                suppressChipExpandedText = suppressSamsungChipExpandedText,
                suppressSourceRemoteViews = suppressSamsungSourceRemoteViews,
                suppressSourceNowBarRemoteView = suppressSamsungNowBarRemoteView,
                lockscreenOnly = smartLockscreenOnly,
                hasProgress = samsungNowBarHasProgress,
                progressValue = samsungNowBarProgressValue,
                progressMax = samsungNowBarProgressMax
            )
        }

        if (hyperBridgeEnabled) {
            val mediaTicker = if (preferMediaControls) {
                smartShortTextOverride.takeIfMeaningfulMediaPlaybackText()
                    ?: displayTitle.takeIfMeaningfulMediaPlaybackText()
                    ?: displayText.takeIfMeaningfulMediaPlaybackText()
                    ?: appName
            } else {
                null
            }
            val hyperTicker = when {
                otpOverride != null -> otpPresentationText ?: otpOverride.code
                callChronometerStart != null ->
                    formatMillisecondsAsClock(System.currentTimeMillis() - callChronometerStart)
                mediaTicker != null -> mediaTicker
                !smartShortTextOverride.isNullOrBlank() -> smartShortTextOverride
                determinateProgressPercent != null -> "$determinateProgressPercent%"
                else -> displayTitle
            }
            HyperBridgeAdapter.apply(
                context = context,
                builder = builder,
                sourcePackageName = sbn.packageName,
                appName = appName,
                title = contentTitle,
                content = contentText,
                ticker = hyperTicker,
                progressPercent = determinateProgressPercent,
                largeIcon = preferredLargeIcon,
                fallbackSmallIcon = preferredPrimaryIcon,
                sourceActions = source.actions
            )
        }

        customNotificationColor?.let { color ->
            builder.addExtras(buildCustomNotificationColorExtras(color))
        }

        val notification = builder.build()
        return if (requestPromoted || samsungBridge.enabled) {
            SamsungOneUi7NowBarCompat.markEligible(notification)
        } else {
            notification
        }
    }

    private fun buildCustomNotificationColorExtras(color: Int): Bundle {
        return Bundle().apply {
            putInt(SAMSUNG_EXTRA_CHIP_BG_COLOR, color)
            putInt(SAMSUNG_EXTRA_ACTION_BG_COLOR, color)
        }
    }

    private fun notifyWithPromotionFallback(
        context: Context,
        manager: NotificationManagerCompat,
        notificationId: Int,
        mirrorKey: String,
        promotedNotification: Notification,
        sbn: StatusBarNotification,
        appPresentationOverride: AppPresentationOverride,
        mirrorChannel: MirrorNotificationChannel,
        progressOverride: ProgressOverride?,
        otpOverride: OtpMatch?,
        smartShortTextOverride: String?,
        compactCodeOverride: String? = null,
        smartRuleId: String? = null,
        otpShortTextOverride: String? = null,
        samsungBridge: SamsungBridgeContext = SamsungBridgeContext.disabled(
            sourceHasNativeProgress = false
        ),
        allowNavigationIconHeuristics: Boolean = true,
        preferMediaControls: Boolean = false,
        mediaPlaybackIsPlaying: Boolean? = null,
        titleOverride: String? = null,
        textOverride: String? = null,
        largeIconOverride: Bitmap? = null,
        preferSmartShortTextAsPrimary: Boolean = false,
        callMirrorActive: Boolean = false,
        callChronometerStartWallClockMs: Long? = null
    ) {
        try {
            notifyMirroredNotification(
                manager = manager,
                notificationId = notificationId,
                notification = promotedNotification,
                mirrorKey = mirrorKey,
                sourceSbn = sbn
            )
        } catch (error: Throwable) {
            val fallback = buildMirroredNotification(
                context = context,
                sbn = sbn,
                appPresentationOverride = appPresentationOverride,
                mirrorChannel = mirrorChannel,
                progressOverride = progressOverride,
                otpOverride = otpOverride,
                smartShortTextOverride = smartShortTextOverride,
                compactCodeOverride = compactCodeOverride,
                smartRuleId = smartRuleId,
                requestPromoted = false,
                otpShortTextOverride = otpShortTextOverride,
                samsungBridge = samsungBridge,
                allowNavigationIconHeuristics = allowNavigationIconHeuristics,
                preferMediaControls = preferMediaControls,
                mediaPlaybackIsPlaying = mediaPlaybackIsPlaying,
                titleOverride = titleOverride,
                textOverride = textOverride,
                largeIconOverride = largeIconOverride,
                preferSmartShortTextAsPrimary = preferSmartShortTextAsPrimary,
                callMirrorActive = callMirrorActive,
                callChronometerStartWallClockMs = callChronometerStartWallClockMs
            )
            notifyMirroredNotification(
                manager = manager,
                notificationId = notificationId,
                notification = fallback,
                mirrorKey = mirrorKey,
                sourceSbn = sbn
            )
        }
    }

    private fun detectSmartStage(
        packageName: String,
        source: Notification,
        parserDictionary: LiveParserDictionary,
        taxiEnabled: Boolean,
        deliveryEnabled: Boolean,
        navigationEnabled: Boolean,
        weatherEnabled: Boolean,
        externalDevicesEnabled: Boolean,
        externalDevicesIgnoreDebugging: Boolean,
        vpnEnabled: Boolean,
        smartPackageAllowed: Boolean,
        hasNativeProgress: Boolean
    ): SmartStageMatch? {
        val isNavigationPackage = isLikelyNavigationPackage(packageName, parserDictionary)
        val packageLower = packageName.lowercase(Locale.ROOT)
        val isWeatherPackage = isLikelyWeatherPackage(packageLower, parserDictionary)
        val isExternalDevicePackage = isLikelySmartRulePackage(
            packageNameLower = packageLower,
            ruleId = "external_device",
            parserDictionary = parserDictionary
        )
        val isVpnPackage = isLikelyVpnPackage(
            packageNameLower = packageLower,
            parserDictionary = parserDictionary
        )
        val isFoodDeliveryPackage = isLikelySmartRulePackage(
            packageNameLower = packageLower,
            ruleId = "food",
            parserDictionary = parserDictionary
        )
        val combinedText = collectNotificationText(
            notification = source,
            fallbackTitle = packageName,
            includeRemoteViewTexts = isNavigationPackage ||
                    isFoodDeliveryPackage ||
                    isWeatherPackage ||
                    isExternalDevicePackage ||
                    isVpnPackage
        ).lowercase(Locale.ROOT)

        for (rule in parserDictionary.smartRules) {
            if (hasNativeProgress && rule.id != "weather") {
                continue
            }
            if (rule.id == "taxi" && (!taxiEnabled || !smartPackageAllowed)) {
                continue
            }
            if (rule.id == "food" && (!deliveryEnabled || !smartPackageAllowed)) {
                continue
            }
            if (rule.id == "navigation" && !navigationEnabled) {
                continue
            }
            if (rule.id == "weather" && !weatherEnabled) {
                continue
            }
            if (rule.id == "external_device" && !externalDevicesEnabled) {
                continue
            }
            if (rule.id == "external_device" &&
                externalDevicesIgnoreDebugging &&
                isExternalDeviceDebuggingNotification(combinedText)
            ) {
                continue
            }
            if (rule.id == "vpn" && !vpnEnabled) {
                continue
            }
            if (rule.id == "vpn" && !hasVpnSpeedPattern(combinedText, parserDictionary)) {
                continue
            }
            if (!rule.isRelevant(packageLower, combinedText)) {
                continue
            }
            if (rule.isExcluded(combinedText)) {
                continue
            }
            if (rule.id == "external_device" &&
                extractConnectedDeviceName(
                    text = combinedText,
                    parserDictionary = parserDictionary
                ).isNullOrBlank()
            ) {
                continue
            }

            val matchedSignal = rule.signals.firstOrNull { it.pattern.containsMatchIn(combinedText) } ?: continue
            val entityToken = when (rule.id) {
                "navigation" -> "route"
                "weather" -> "weather"
                "external_device" -> "device"
                "vpn" -> "vpn"
                else -> extractEntityToken(combinedText, parserDictionary)
            }
            val compactOrderCode = if (rule.id == "food") {
                extractCompactOrderCode(entityToken)
                    ?.takeIf { isExplicitOrderEntityToken(combinedText, entityToken) }
            } else {
                null
            }
            val aggregateEntityToken = when {
                rule.id == "food" && compactOrderCode == null -> FOOD_DELIVERY_AGGREGATE_ENTITY
                rule.id == "food" -> compactOrderCode
                else -> entityToken
            }

            return SmartStageMatch(
                aggregateKey = "$packageLower:${rule.id}:$aggregateEntityToken",
                stageValue = matchedSignal.stage,
                maxStage = rule.maxStage,
                compactOrderCode = compactOrderCode,
                keepHighestStage = rule.id != "navigation" &&
                        rule.id != "weather" &&
                        rule.id != "external_device" &&
                        rule.id != "vpn"
            )
        }

        if (weatherEnabled) {
            detectWeatherSmartStage(
                packageNameLower = packageLower,
                source = source,
                parserDictionary = parserDictionary
            )?.let { return it }
        }

        if (vpnEnabled) {
            detectVpnTrafficSmartStage(
                packageNameLower = packageLower,
                source = source,
                parserDictionary = parserDictionary
            )?.let { return it }
        }

        return null
    }

    private fun isNotificationDedupEligibleSmartRule(ruleId: String): Boolean {
        return ruleId != "navigation" &&
                ruleId != "weather" &&
                ruleId != "external_device" &&
                ruleId != "vpn"
    }

    private fun isExternalDeviceDebuggingNotification(text: String): Boolean {
        return externalDeviceDebuggingPattern.containsMatchIn(text)
    }

    private fun detectWeatherSmartStage(
        packageNameLower: String,
        source: Notification,
        parserDictionary: LiveParserDictionary
    ): SmartStageMatch? {
        val combinedText = collectNotificationText(
            notification = source,
            fallbackTitle = packageNameLower,
            includeRemoteViewTexts = true
        )
        if (combinedText.isBlank()) {
            return null
        }
        val likelyWeatherPackage = isLikelyWeatherPackage(packageNameLower, parserDictionary)
        val hasWeatherContext = parserDictionary.weatherContextPattern.containsMatchIn(combinedText)
        if (!likelyWeatherPackage && !hasWeatherContext) {
            return null
        }

        val temperature = extractWeatherTemperatureFromText(combinedText, parserDictionary) ?: return null
        if (temperature.isBlank()) {
            return null
        }

        return SmartStageMatch(
            aggregateKey = "$packageNameLower:weather:weather",
            stageValue = 1,
            maxStage = 1,
            compactOrderCode = null,
            keepHighestStage = false
        )
    }

    private fun detectVpnTrafficSmartStage(
        packageNameLower: String,
        source: Notification,
        parserDictionary: LiveParserDictionary
    ): SmartStageMatch? {
        val combinedText = collectNotificationText(
            notification = source,
            fallbackTitle = packageNameLower,
            includeRemoteViewTexts = true
        )
        if (combinedText.isBlank()) {
            return null
        }
        if (!hasVpnSpeedPattern(combinedText, parserDictionary)) {
            return null
        }

        val likelyVpnPackage = isLikelyVpnPackage(packageNameLower, parserDictionary)
        val hasVpnContext = parserDictionary.vpnContextPattern.containsMatchIn(combinedText)
        if (!likelyVpnPackage && !hasVpnContext) {
            return null
        }

        return SmartStageMatch(
            aggregateKey = "$packageNameLower:vpn:vpn",
            stageValue = 1,
            maxStage = 1,
            compactOrderCode = null,
            keepHighestStage = false
        )
    }

    private fun detectTextProgress(
        packageName: String,
        source: Notification,
        parserDictionary: LiveParserDictionary
    ): TextProgressMatch? {
        val combinedText = collectNotificationText(
            notification = source,
            fallbackTitle = packageName,
            includeRemoteViewTexts = true
        )
        if (combinedText.isBlank()) {
            return null
        }

        val percentPattern = parserDictionary.textProgressPercentPattern
        val combinedLower = combinedText.lowercase(Locale.ROOT)
        val matches = percentPattern.findAll(combinedText)
        for (match in matches) {
            val percentValue = match.groupValues.getOrNull(1)?.toIntOrNull() ?: continue
            if (percentValue !in 0..100) {
                continue
            }
            if (!hasTextProgressContextHint(
                    textLower = combinedLower,
                    start = match.range.first,
                    endExclusive = match.range.last + 1,
                    parserDictionary = parserDictionary
                )
            ) {
                continue
            }
            if (isExcludedTextProgressContext(
                    textLower = combinedLower,
                    start = match.range.first,
                    endExclusive = match.range.last + 1,
                    parserDictionary = parserDictionary
                )
            ) {
                continue
            }
            return TextProgressMatch(
                percent = percentValue,
                shortText = "$percentValue%"
            )
        }
        return null
    }

    private fun hasTextProgressContextHint(
        textLower: String,
        start: Int,
        endExclusive: Int,
        parserDictionary: LiveParserDictionary
    ): Boolean {
        val contextWindow = parserDictionary.textProgressContextWindow
        val windowStart = (start - contextWindow).coerceAtLeast(0)
        val windowEnd = (endExclusive + contextWindow).coerceAtMost(textLower.length)
        val context = textLower.substring(windowStart, windowEnd)
        return parserDictionary.textProgressIncludeContextPattern.containsMatchIn(context)
    }

    private fun isExcludedTextProgressContext(
        textLower: String,
        start: Int,
        endExclusive: Int,
        parserDictionary: LiveParserDictionary
    ): Boolean {
        val contextWindow = parserDictionary.textProgressContextWindow
        val windowStart = (start - contextWindow).coerceAtLeast(0)
        val windowEnd = (endExclusive + contextWindow).coerceAtMost(textLower.length)
        val context = textLower.substring(windowStart, windowEnd)
        return parserDictionary.textProgressExcludeContextPattern.containsMatchIn(context)
    }

    private fun detectOtpCode(
        packageName: String,
        source: Notification,
        parserDictionary: LiveParserDictionary
    ): OtpMatch? {
        val combinedText = collectNotificationText(
            notification = source,
            fallbackTitle = packageName,
            includeRemoteViewTexts = false
        )
        if (combinedText.isBlank()) {
            return null
        }

        val combinedLower = combinedText.lowercase(Locale.ROOT)
        val hasStrongTrigger = parserDictionary.otpStrongTriggers.any(combinedLower::contains)
        val hasLooseTrigger = parserDictionary.otpLooseTriggerPattern.containsMatchIn(combinedLower)
        if (!hasStrongTrigger && !hasLooseTrigger) {
            return null
        }
        if (!hasStrongTrigger && looksLikeOrderContext(combinedLower, parserDictionary)) {
            return null
        }

        for (pattern in parserDictionary.otpCodePatterns) {
            for (match in pattern.findAll(combinedText)) {
                val rawValue = match.groupValues.getOrNull(1)?.ifBlank { match.value } ?: match.value
                val digits = rawValue.filter(Char::isDigit)
                if (digits.length !in OTP_CODE_LENGTH) {
                    continue
                }
                if (!hasOtpTokenBoundaries(combinedText, match.range.first, match.range.last + 1)) {
                    continue
                }
                if (isLikelyMoneyCandidate(combinedLower, match.range.first, match.range.last + 1, parserDictionary)) {
                    continue
                }
                if (looksLikeOrderContextAroundMatch(
                        combinedLower,
                        match.range.first,
                        match.range.last + 1,
                        parserDictionary
                    ) &&
                    !hasStrongTrigger
                ) {
                    continue
                }
                if (digits.length in OTP_CODE_LENGTH) {
                    return OtpMatch(
                        code = digits,
                        aggregateKey = otpAggregateKeyForCode(packageName, digits)
                    )
                }
            }
        }

        return null
    }

    private fun otpAggregateKeyForCode(packageName: String, code: String): String {
        return "otp:${packageName.lowercase(Locale.ROOT)}:$code"
    }

    private fun otpSourceKeyForPackage(packageName: String): String {
        return packageName.lowercase(Locale.ROOT)
    }

    private fun hasOtpTokenBoundaries(
        text: String,
        start: Int,
        endExclusive: Int
    ): Boolean {
        val left = if (start > 0) text[start - 1] else null
        val right = if (endExclusive < text.length) text[endExclusive] else null
        val leftOk = left == null || !left.isLetterOrDigit()
        val rightOk = right == null || !right.isLetterOrDigit()
        return leftOk && rightOk
    }

    private fun extractEntityToken(combinedText: String, parserDictionary: LiveParserDictionary): String {
        for (pattern in parserDictionary.entityTokenPatterns) {
            val match = pattern.find(combinedText) ?: continue
            val token = match.groupValues.getOrNull(1)?.trim().orEmpty()
            if (token.isNotEmpty()) {
                return token
            }
        }

        return "default"
    }

    private fun isExplicitOrderEntityToken(combinedText: String, token: String): Boolean {
        if (token == "default" || token.isBlank()) {
            return false
        }
        val tokenIndex = combinedText.indexOf(token.lowercase(Locale.ROOT))
        if (tokenIndex < 0) {
            return false
        }
        val prefixStart = (tokenIndex - 32).coerceAtLeast(0)
        val prefix = combinedText.substring(prefixStart, tokenIndex)
        return explicitOrderEntityPrefixPattern.containsMatchIn(prefix)
    }

    private fun extractCompactOrderCode(token: String): String? {
        if (token == "default" || token.isBlank()) {
            return null
        }
        if (!token.any(Char::isDigit)) {
            return null
        }

        val compact = token
            .filter { it.isLetterOrDigit() || it == '-' }
            .uppercase(Locale.ROOT)
            .take(12)

        return compact.ifBlank { null }
    }

    private fun smartRuleIdFromAggregateKey(aggregateKey: String): String {
        val firstSeparator = aggregateKey.indexOf(':')
        if (firstSeparator < 0) {
            return ""
        }
        val secondSeparator = aggregateKey.indexOf(':', firstSeparator + 1)
        if (secondSeparator < 0) {
            return ""
        }
        return aggregateKey.substring(firstSeparator + 1, secondSeparator)
    }

    private fun smartShortStatusText(
        context: Context,
        ruleId: String,
        stageValue: Int,
        parserDictionary: LiveParserDictionary
    ): String? {
        return parserDictionary.resolveStatusText(
            ruleId = ruleId,
            stageValue = stageValue,
            locale = currentLocale(context)
        )
    }

    private fun extractExternalDeviceStatusText(
        context: Context,
        notification: Notification,
        fallbackTitle: String,
        stageValue: Int,
        parserDictionary: LiveParserDictionary
    ): String? {
        val combinedText = collectNotificationText(
            notification = notification,
            fallbackTitle = fallbackTitle,
            includeRemoteViewTexts = true
        )
        val deviceName = extractConnectedDeviceName(
            text = combinedText,
            parserDictionary = parserDictionary
        )
        val statusText = parserDictionary.resolveStatusText(
            ruleId = "external_device",
            stageValue = stageValue,
            locale = currentLocale(context)
        )

        return when {
            !deviceName.isNullOrBlank() && !statusText.isNullOrBlank() -> "$deviceName · $statusText"
            !deviceName.isNullOrBlank() -> deviceName
            else -> statusText
        }
    }

    private fun extractConnectedDeviceName(
        text: String,
        parserDictionary: LiveParserDictionary
    ): String? {
        for (pattern in parserDictionary.externalDeviceNamePatterns) {
            val match = pattern.find(text) ?: continue
            val candidate = normalizeExternalDeviceName(
                raw = match.groupValues.getOrNull(1),
                parserDictionary = parserDictionary
            )
            if (!candidate.isNullOrBlank()) {
                return candidate
            }
        }
        return null
    }

    private fun normalizeExternalDeviceName(
        raw: String?,
        parserDictionary: LiveParserDictionary
    ): String? {
        val normalized = raw.orEmpty()
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('"', '\'', '\u00AB', '\u00BB', '.', ',', ':', ';')
        if (normalized.length < 2) {
            return null
        }
        val lower = normalized.lowercase(Locale.ROOT)
        if (lower in parserDictionary.externalDeviceGenericNames) {
            return null
        }
        return normalized
    }

    private fun extractVpnTrafficSpeeds(
        notification: Notification,
        fallbackTitle: String,
        parserDictionary: LiveParserDictionary
    ): VpnTrafficSpeeds? {
        val combinedText = collectNotificationText(
            notification = notification,
            fallbackTitle = fallbackTitle,
            includeRemoteViewTexts = true
        )
        return extractVpnTrafficSpeedsFromText(combinedText, parserDictionary)
    }

    private fun extractVpnTrafficSpeedsFromText(
        combinedText: String,
        parserDictionary: LiveParserDictionary
    ): VpnTrafficSpeeds? {
        if (combinedText.isBlank()) {
            return null
        }

        val fallbackSpeeds = parserDictionary.vpnSpeedPattern.findAll(combinedText)
            .map { normalizeVpnSpeedToken(it.value) }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .take(2)
            .toList()

        var incoming = extractDirectionalVpnSpeed(
            text = combinedText,
            speedPattern = parserDictionary.vpnSpeedPattern,
            markers = parserDictionary.vpnDownloadMarkers
        )
        var outgoing = extractDirectionalVpnSpeed(
            text = combinedText,
            speedPattern = parserDictionary.vpnSpeedPattern,
            markers = parserDictionary.vpnUploadMarkers
        )
        if (!incoming.isNullOrBlank() || !outgoing.isNullOrBlank()) {
            if (outgoing.isNullOrBlank()) {
                outgoing = pickFallbackVpnSpeed(
                    candidates = fallbackSpeeds,
                    exclude = incoming
                )
            }
            if (incoming.isNullOrBlank()) {
                incoming = pickFallbackVpnSpeed(
                    candidates = fallbackSpeeds,
                    exclude = outgoing
                )
            }
            return VpnTrafficSpeeds(
                outgoingSpeed = outgoing,
                incomingSpeed = incoming
            )
        }

        if (fallbackSpeeds.isEmpty()) {
            return null
        }
        if (fallbackSpeeds.size == 1) {
            return VpnTrafficSpeeds(
                outgoingSpeed = fallbackSpeeds.first(),
                incomingSpeed = null
            )
        }
        return VpnTrafficSpeeds(
            outgoingSpeed = fallbackSpeeds[0],
            incomingSpeed = fallbackSpeeds[1]
        )
    }

    private fun pickFallbackVpnSpeed(
        candidates: List<String>,
        exclude: String?
    ): String? {
        if (candidates.isEmpty()) {
            return null
        }
        if (exclude.isNullOrBlank()) {
            return candidates.first()
        }
        val different = candidates.firstOrNull { !it.equals(exclude, ignoreCase = true) }
        return different ?: candidates.firstOrNull()
    }

    private fun formatDominantVpnTrafficText(vpnTraffic: VpnTrafficSpeeds?): String? {
        vpnTraffic ?: return null
        val outgoing = vpnTraffic.outgoingSpeed
        val incoming = vpnTraffic.incomingSpeed
        if (outgoing.isNullOrBlank() && incoming.isNullOrBlank()) {
            return null
        }
        if (outgoing.isNullOrBlank()) {
            return formatVpnIncomingToken(incoming)
        }
        if (incoming.isNullOrBlank()) {
            return formatVpnOutgoingToken(outgoing)
        }

        val outgoingMagnitude = parseVpnSpeedMagnitude(outgoing)
        val incomingMagnitude = parseVpnSpeedMagnitude(incoming)

        return when {
            outgoingMagnitude == null && incomingMagnitude == null ->
                formatVpnOutgoingToken(outgoing)

            outgoingMagnitude == null ->
                formatVpnIncomingToken(incoming)

            incomingMagnitude == null ->
                formatVpnOutgoingToken(outgoing)

            outgoingMagnitude > incomingMagnitude ->
                formatVpnOutgoingToken(outgoing)

            else ->
                formatVpnIncomingToken(incoming)
        }
    }

    private fun parseVpnSpeedMagnitude(speed: String?): Double? {
        val normalized = speed.orEmpty()
            .replace(" ", "")
            .replace(',', '.')
            .lowercase(Locale.ROOT)
        if (normalized.isBlank()) {
            return null
        }

        var numberEnd = 0
        while (numberEnd < normalized.length) {
            val ch = normalized[numberEnd]
            if (!ch.isDigit() && ch != '.') {
                break
            }
            numberEnd += 1
        }
        if (numberEnd == 0) {
            return null
        }

        val numericValue = normalized.substring(0, numberEnd).toDoubleOrNull() ?: return null
        val unitChar = normalized.drop(numberEnd).firstOrNull()
        val multiplier = when (unitChar) {
            'k', '\u043A' -> 1_000.0
            'm', '\u043C' -> 1_000_000.0
            'g', '\u0433' -> 1_000_000_000.0
            't', '\u0442' -> 1_000_000_000_000.0
            else -> 1.0
        }
        return numericValue * multiplier
    }

    private fun formatVpnOutgoingToken(speed: String?): String? {
        if (speed.isNullOrBlank()) {
            return null
        }
        return "\u2191$speed"
    }

    private fun formatVpnIncomingToken(speed: String?): String? {
        if (speed.isNullOrBlank()) {
            return null
        }
        return "\u2193$speed"
    }

    private fun extractDirectionalVpnSpeed(
        text: String,
        speedPattern: Regex,
        markers: Set<String>
    ): String? {
        var bestSpeed: String? = null
        var bestDistance: Int? = null
        for (match in speedPattern.findAll(text)) {
            val distance = nearestMarkerDistance(
                text = text,
                start = match.range.first,
                endExclusive = match.range.last + 1,
                markers = markers
            ) ?: continue
            val normalizedSpeed = normalizeVpnSpeedToken(match.value)
            if (normalizedSpeed.isBlank()) {
                continue
            }
            if (bestDistance == null || distance < bestDistance) {
                bestDistance = distance
                bestSpeed = normalizedSpeed
            }
        }
        return bestSpeed
    }

    private fun nearestMarkerDistance(
        text: String,
        start: Int,
        endExclusive: Int,
        markers: Set<String>
    ): Int? {
        if (markers.isEmpty() || text.isEmpty()) {
            return null
        }
        val windowStart = (start - 24).coerceAtLeast(0)
        val windowEnd = (endExclusive + 24).coerceAtMost(text.length)
        val context = text.substring(windowStart, windowEnd)

        var bestDistance: Int? = null
        for (marker in markers) {
            val ranges = markerRangesInContext(context, marker)
            for (range in ranges) {
                val markerStart = windowStart + range.first
                val markerEndExclusive = windowStart + range.last + 1
                val distance = when {
                    markerEndExclusive <= start -> start - markerEndExclusive
                    markerStart >= endExclusive -> markerStart - endExclusive
                    else -> 0
                }
                if (bestDistance == null || distance < bestDistance) {
                    bestDistance = distance
                }
            }
        }
        return bestDistance
    }

    private fun markerRangesInContext(context: String, marker: String): List<IntRange> {
        val normalized = marker.trim()
        if (normalized.isEmpty()) {
            return emptyList()
        }

        val hasWordChars = normalized.any { it.isLetterOrDigit() }
        if (hasWordChars) {
            return Regex("\\b${Regex.escape(normalized)}\\b", setOf(RegexOption.IGNORE_CASE))
                .findAll(context)
                .map { it.range }
                .toList()
        }

        val ranges = mutableListOf<IntRange>()
        var fromIndex = 0
        while (fromIndex < context.length) {
            val index = context.indexOf(normalized, fromIndex)
            if (index < 0) {
                break
            }
            ranges += index until (index + normalized.length)
            fromIndex = index + normalized.length
        }
        return ranges
    }

    private fun normalizeVpnSpeedToken(raw: String): String {
        return NotificationTextNormalizer.repair(raw)
            .replace(Regex("\\s+"), "")
            .replace("/с", "/s", ignoreCase = true)
            .replace("сек", "s", ignoreCase = true)
    }

    private fun extractNavigationDistanceText(
        notification: Notification,
        fallbackTitle: String,
        parserDictionary: LiveParserDictionary
    ): String? {
        val combinedText = collectNotificationText(
            notification = notification,
            fallbackTitle = fallbackTitle,
            includeRemoteViewTexts = true
        )
        val match = parserDictionary.navigationDistancePattern.find(combinedText) ?: return null
        return match.value
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { null }
    }

    private fun extractWeatherTemperatureText(
        notification: Notification,
        fallbackTitle: String,
        parserDictionary: LiveParserDictionary
    ): String? {
        val combinedText = collectNotificationText(
            notification = notification,
            fallbackTitle = fallbackTitle,
            includeRemoteViewTexts = true
        )
        return extractWeatherTemperatureFromText(combinedText, parserDictionary)
    }

    private fun extractWeatherTemperatureFromText(
        combinedText: String,
        parserDictionary: LiveParserDictionary
    ): String? {
        extractWeatherHighLowTemperatureSummary(combinedText, parserDictionary)?.let { return it }

        val match = parserDictionary.weatherTemperaturePattern.find(combinedText) ?: return null
        val rawNumber = normalizeWeatherTemperatureValue(match.groupValues.getOrNull(1))
        if (rawNumber.isBlank()) {
            return null
        }
        val baseTemperature = formatWeatherTemperature(
            value = rawNumber,
            unit = inferWeatherTemperatureUnit(combinedText)
        )
        val conditionEmoji = extractWeatherConditionEmoji(combinedText, parserDictionary)
        return if (conditionEmoji != null) {
            "$conditionEmoji $baseTemperature"
        } else {
            baseTemperature
        }
    }

    private fun extractWeatherHighLowTemperatureSummary(
        combinedText: String,
        parserDictionary: LiveParserDictionary
    ): String? {
        val match = weatherHighLowPattern.find(combinedText) ?: return null
        val high = normalizeWeatherTemperatureValue(match.groupValues.getOrNull(1))
        val low = normalizeWeatherTemperatureValue(match.groupValues.getOrNull(2))
        if (high.isBlank() || low.isBlank()) {
            return null
        }

        val unit = inferWeatherTemperatureUnit(match.value) ?: inferWeatherTemperatureUnit(combinedText)
        val summary = "${formatWeatherTemperature(high, unit)} / ${formatWeatherTemperature(low, unit)}"
        val conditionEmoji = extractWeatherConditionEmoji(combinedText, parserDictionary)
        return if (conditionEmoji != null) {
            "$conditionEmoji $summary"
        } else {
            summary
        }
    }

    private fun normalizeWeatherTemperatureValue(rawValue: String?): String {
        return rawValue.orEmpty()
            .replace('\u2212', '-')
            .trim()
    }

    private fun inferWeatherTemperatureUnit(text: String): String? {
        return when {
            weatherCelsiusPattern.containsMatchIn(text) -> "C"
            weatherFahrenheitPattern.containsMatchIn(text) -> "F"
            else -> null
        }
    }

    private fun formatWeatherTemperature(value: String, unit: String?): String {
        return if (unit != null) "${value}\u00B0$unit" else "${value}\u00B0"
    }

    private fun currentLocale(context: Context): Locale? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
    }

    private fun appLocale(context: Context): AppLocale {
        appLocaleFromLanguageTag(ConverterPrefs(context).getAppLanguageTag())?.let { locale ->
            return locale
        }
        val locale = currentLocale(context) ?: return AppLocale.EN
        return appLocaleFromLocale(locale)
    }

    private fun appLocaleFromLocale(locale: Locale): AppLocale {
        val language = locale.language.lowercase(Locale.ROOT)
        return when {
            language.startsWith("ru") -> AppLocale.RU
            language.startsWith("tr") -> AppLocale.TR
            language.startsWith("pt") -> AppLocale.PT_BR
            language.startsWith("zh") && isTraditionalChinese(locale) -> AppLocale.ZH_HANT
            language.startsWith("zh") -> AppLocale.ZH_HANS
            language.startsWith("ko") -> AppLocale.KO
            else -> AppLocale.EN
        }
    }

    private fun appLocaleFromLanguageTag(languageTag: String?): AppLocale? {
        val normalized = languageTag
            ?.trim()
            ?.replace('_', '-')
            ?.lowercase(Locale.ROOT)
            .orEmpty()
        return when {
            normalized.isBlank() || normalized == "system" -> null
            normalized.startsWith("ru") -> AppLocale.RU
            normalized.startsWith("tr") -> AppLocale.TR
            normalized.startsWith("pt") -> AppLocale.PT_BR
            isTraditionalChineseLanguageTag(normalized) -> AppLocale.ZH_HANT
            normalized.startsWith("zh") -> AppLocale.ZH_HANS
            normalized.startsWith("ko") -> AppLocale.KO
            else -> AppLocale.EN
        }
    }

    private fun isTraditionalChineseLanguageTag(languageTag: String): Boolean {
        return languageTag.startsWith("zh") &&
            (
                languageTag.contains("hant") ||
                    languageTag.contains("-tw") ||
                    languageTag.contains("-hk") ||
                    languageTag.contains("-mo")
                )
    }

    private fun isTraditionalChinese(locale: Locale): Boolean {
        val script = locale.script.lowercase(Locale.ROOT)
        val country = locale.country.uppercase(Locale.ROOT)
        return script == "hant" || country in setOf("TW", "HK", "MO")
    }

    private fun isRussianLocale(context: Context): Boolean {
        return appLocale(context) == AppLocale.RU
    }

    private fun notificationCapsuleChannelText(context: Context): MirrorChannelText {
        return when (appLocale(context)) {
            AppLocale.RU -> MirrorChannelText(
                name = "Капсула уведомлений",
                description = "Капсула со счётчиком уведомлений на экране блокировки"
            )
            AppLocale.TR -> MirrorChannelText(
                name = "Bildirim kapsülü",
                description = "Kilit ekranında bildirim sayacı kapsülü"
            )
            AppLocale.PT_BR -> MirrorChannelText(
                name = "Cápsula de notificações",
                description = "Cápsula com contador de notificações na tela de bloqueio"
            )
            AppLocale.ZH_HANS -> MirrorChannelText(
                name = "通知胶囊",
                description = "锁屏通知计数胶囊"
            )
            AppLocale.ZH_HANT -> MirrorChannelText(
                name = "通知膠囊",
                description = "鎖定畫面通知計數膠囊"
            )
            AppLocale.KO -> MirrorChannelText(
                name = "알림 캡슐",
                description = "잠금화면 알림 개수 캡슐"
            )
            AppLocale.EN -> MirrorChannelText(
                name = "Notification capsule",
                description = "Lock screen notification counter capsule"
            )
        }
    }

    private fun chargingInfoChannelText(context: Context): MirrorChannelText {
        return when (appLocale(context)) {
            AppLocale.RU -> MirrorChannelText(
                name = "Информация о зарядке",
                description = "Заряд батареи, время до полного заряда, скорость зарядки и предупреждения о разрядке"
            )
            AppLocale.TR -> MirrorChannelText(
                name = "Şarj bilgisi",
                description = "Kilit ekranında pil seviyesi, kalan süre, şarj hızı ve düşük pil uyarıları"
            )
            AppLocale.PT_BR -> MirrorChannelText(
                name = "Informações de carregamento",
                description = "Nível da bateria, tempo restante, velocidade de carregamento e alertas de bateria fraca"
            )
            AppLocale.ZH_HANS -> MirrorChannelText(
                name = "充电信息",
                description = "在锁屏显示电量、剩余时间、充电速度和低电量提醒"
            )
            AppLocale.ZH_HANT -> MirrorChannelText(
                name = "充電資訊",
                description = "在鎖定畫面顯示電量、剩餘時間、充電速度和低電量提醒"
            )
            AppLocale.KO -> MirrorChannelText(
                name = "충전 정보",
                description = "잠금 화면에 배터리 잔량, 남은 시간, 충전 속도와 배터리 부족 알림 표시"
            )
            AppLocale.EN -> MirrorChannelText(
                name = "Charging information",
                description = "Battery charge, charging speed, and low-battery warnings on the lock screen"
            )
        }
    }

    private fun notificationCapsuleClearActionText(context: Context): String {
        return when (appLocale(context)) {
            AppLocale.TR -> "Temizle"
            AppLocale.PT_BR -> "Limpar"
            AppLocale.ZH_HANS -> "清除"
            AppLocale.ZH_HANT -> "清除"
            AppLocale.KO -> "지우기"
            AppLocale.RU,
            AppLocale.EN -> "Clear"
        }
    }

    private fun notificationCapsuleSystemLabel(context: Context): String {
        return when (appLocale(context)) {
            AppLocale.RU -> "Система"
            AppLocale.TR -> "Sistem"
            AppLocale.PT_BR -> "Sistema"
            AppLocale.ZH_HANS -> "系统"
            AppLocale.ZH_HANT -> "系統"
            AppLocale.KO -> "시스템"
            AppLocale.EN -> "System"
        }
    }

    private fun russianNotificationWord(count: Int): String {
        val mod100 = count % 100
        if (mod100 in 11..14) {
            return "уведомлений"
        }
        return when (count % 10) {
            1 -> "уведомление"
            in 2..4 -> "уведомления"
            else -> "уведомлений"
        }
    }

    private fun isLikelyMoneyCandidate(
        textLower: String,
        start: Int,
        endExclusive: Int,
        parserDictionary: LiveParserDictionary
    ): Boolean {
        val windowStart = (start - 18).coerceAtLeast(0)
        val windowEnd = (endExclusive + 18).coerceAtMost(textLower.length)
        val context = textLower.substring(windowStart, windowEnd)
        return parserDictionary.moneyContextPattern.containsMatchIn(context)
    }

    private fun looksLikeOrderContext(textLower: String, parserDictionary: LiveParserDictionary): Boolean {
        return parserDictionary.orderContextHints.any(textLower::contains)
    }

    private fun looksLikeOrderContextAroundMatch(
        textLower: String,
        start: Int,
        endExclusive: Int,
        parserDictionary: LiveParserDictionary
    ): Boolean {
        val windowStart = (start - 24).coerceAtLeast(0)
        val windowEnd = (endExclusive + 24).coerceAtMost(textLower.length)
        val context = textLower.substring(windowStart, windowEnd)
        return looksLikeOrderContext(context, parserDictionary)
    }

    private fun shouldAutoCopyOtpLocked(
        state: OtpAggregateState,
        code: String
    ): Boolean {
        if (state.lastAutoCopiedCode != code) {
            state.lastAutoCopiedCode = code
            return true
        }

        return false
    }

    private fun copyOtpToClipboard(context: Context, code: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("OTP", code))
    }

    private fun startOtpAutoCopyAnimation(
        context: Context,
        manager: NotificationManagerCompat,
        sbn: StatusBarNotification,
        appPresentationOverride: AppPresentationOverride,
        otpMatch: OtpMatch,
        samsungBridge: SamsungBridgeContext
    ) {
        val generation = synchronized(stateLock) {
            val nextGeneration = (otpAnimationGenerations[otpMatch.aggregateKey] ?: 0L) + 1L
            otpAnimationGenerations[otpMatch.aggregateKey] = nextGeneration
            nextGeneration
        }

        val copiedLabel = when {
            isRussianLocale(context) -> "Скопировано"
            currentLocale(context)?.language?.lowercase(Locale.ROOT) == "zh" -> "已复制"
            else -> "Copied"
        }

        scheduleOtpAnimationStep(
            context = context,
            manager = manager,
            sbn = sbn,
            appPresentationOverride = appPresentationOverride,
            otpMatch = otpMatch,
            samsungBridge = samsungBridge,
            generation = generation,
            delayMs = OTP_AUTOCOPY_COPIED_SHOW_DELAY_MS,
            otpShortTextOverride = copiedLabel
        )

        scheduleOtpAnimationStep(
            context = context,
            manager = manager,
            sbn = sbn,
            appPresentationOverride = appPresentationOverride,
            otpMatch = otpMatch,
            samsungBridge = samsungBridge,
            generation = generation,
            delayMs = OTP_AUTOCOPY_COPIED_SHOW_DELAY_MS + OTP_AUTOCOPY_COPIED_SHOW_DURATION_MS,
            otpShortTextOverride = null
        )
    }

    private fun scheduleOtpAnimationStep(
        context: Context,
        manager: NotificationManagerCompat,
        sbn: StatusBarNotification,
        appPresentationOverride: AppPresentationOverride,
        otpMatch: OtpMatch,
        samsungBridge: SamsungBridgeContext,
        generation: Long,
        delayMs: Long,
        otpShortTextOverride: String?
    ) {
        mainHandler.postDelayed({
            if (!isOtpAnimationGenerationCurrent(otpMatch.aggregateKey, generation)) {
                return@postDelayed
            }
            try {
                val notification = buildMirroredNotification(
                    context = context,
                    sbn = sbn,
                    appPresentationOverride = appPresentationOverride,
                    mirrorChannel = MirrorNotificationChannel.OTP_CODES,
                    progressOverride = null,
                    otpOverride = otpMatch,
                    smartShortTextOverride = null,
                    requestPromoted = true,
                    otpShortTextOverride = otpShortTextOverride,
                    samsungBridge = samsungBridge
                )
                notifyWithPromotionFallback(
                    context = context,
                    manager = manager,
                    notificationId = mirrorIdForKey(otpMatch.aggregateKey),
                    mirrorKey = otpMatch.aggregateKey,
                    promotedNotification = notification,
                    sbn = sbn,
                    appPresentationOverride = appPresentationOverride,
                    mirrorChannel = MirrorNotificationChannel.OTP_CODES,
                    progressOverride = null,
                    otpOverride = otpMatch,
                    smartShortTextOverride = null,
                    otpShortTextOverride = otpShortTextOverride,
                    samsungBridge = samsungBridge
                )
            } catch (error: Throwable) {
                Log.e(TAG, "Failed OTP auto-copy animation update: ${otpMatch.aggregateKey}", error)
            }
        }, delayMs)
    }

    private fun isOtpAnimationGenerationCurrent(aggregateKey: String, generation: Long): Boolean {
        return synchronized(stateLock) {
            val state = otpAggregateStates[aggregateKey] ?: return@synchronized false
            if (state.activeSbnKeys.isEmpty()) {
                return@synchronized false
            }
            otpAnimationGenerations[aggregateKey] == generation
        }
    }

    private fun buildSmartAnimatedIslandTokens(
        ruleId: String,
        notification: Notification,
        fallbackTitle: String,
        primaryStatus: String?,
        compactOrderCode: String?,
        parserDictionary: LiveParserDictionary
    ): List<String?> {
        val combinedText = collectNotificationText(
            notification = notification,
            fallbackTitle = fallbackTitle,
            includeRemoteViewTexts = true
        )
        return when (ruleId) {
            "food" -> {
                listOf(
                    primaryStatus,
                    compactOrderCode ?: extractCompactOrderCode(combinedText)
                )
            }

            "navigation" -> {
                listOf(
                    primaryStatus,
                    extractNavigationInstructionToken(combinedText, parserDictionary)
                )
            }

            "weather" -> {
                listOf(
                    extractWeatherDayToken(combinedText, parserDictionary),
                    primaryStatus,
                    extractWeatherConditionToken(combinedText, parserDictionary)
                )
            }

            else -> listOf(primaryStatus)
        }
    }

    private fun startSmartIslandAnimation(
        context: Context,
        manager: NotificationManagerCompat,
        aggregateKey: String,
        sbn: StatusBarNotification,
        appPresentationOverride: AppPresentationOverride,
        mirrorChannel: MirrorNotificationChannel,
        progressOverride: ProgressOverride?,
        smartRuleId: String,
        tokens: List<String?>,
        initialToken: String?,
        compactCodeOverride: String?,
        samsungBridge: SamsungBridgeContext
    ) {
        if (tokens.isEmpty()) {
            return
        }
        val prefs = ConverterPrefs(context)
        val aospCuttingEnabled = prefs.getAospCuttingEnabled()
        val aospCuttingLength = prefs.getAospCuttingLength()
        val normalizedTokens = tokens.map {
            normalizeAnimatedToken(it, aospCuttingEnabled, aospCuttingLength)
        }
        val normalizedInitial = normalizeAnimatedToken(
            initialToken,
            aospCuttingEnabled,
            aospCuttingLength
        )
        val uniqueRenderableTokens = normalizedTokens
            .mapNotNull { it }
            .distinctBy { it.lowercase(Locale.ROOT) }
        val generationToStart = synchronized(stateLock) {
            if (uniqueRenderableTokens.size < 2) {
                smartAnimationGenerations.remove(aggregateKey)
                smartAnimationStates.remove(aggregateKey)
                return@synchronized null
            }

            val existingState = smartAnimationStates[aggregateKey]
            if (existingState != null && smartAnimationGenerations.containsKey(aggregateKey)) {
                existingState.sbn = sbn
                existingState.appPresentationOverride = appPresentationOverride
                existingState.mirrorChannel = mirrorChannel
                existingState.progressOverride = progressOverride
                existingState.smartRuleId = smartRuleId
                existingState.tokens = normalizedTokens
                existingState.compactCodeOverride = compactCodeOverride
                existingState.samsungBridge = samsungBridge
                if (!normalizedInitial.isNullOrBlank() &&
                    normalizedTokens.any { it.equals(normalizedInitial, ignoreCase = true) } &&
                    existingState.lastShownToken.isNullOrBlank()
                ) {
                    existingState.lastShownToken = normalizedInitial
                }
                return@synchronized null
            }

            val nextGeneration = (smartAnimationGenerations[aggregateKey] ?: 0L) + 1L
            smartAnimationGenerations[aggregateKey] = nextGeneration
            smartAnimationStates[aggregateKey] = SmartAnimationState(
                sbn = sbn,
                appPresentationOverride = appPresentationOverride,
                mirrorChannel = mirrorChannel,
                progressOverride = progressOverride,
                smartRuleId = smartRuleId,
                tokens = normalizedTokens,
                nextIndex = 0,
                lastShownToken = normalizedInitial,
                compactCodeOverride = compactCodeOverride,
                samsungBridge = samsungBridge
            )
            nextGeneration
        } ?: return

        scheduleSmartAnimationStep(
            context = context,
            manager = manager,
            aggregateKey = aggregateKey,
            generation = generationToStart
        )
    }

    private fun scheduleSmartAnimationStep(
        context: Context,
        manager: NotificationManagerCompat,
        aggregateKey: String,
        generation: Long
    ) {
        mainHandler.postDelayed({
            val frame = synchronized(stateLock) {
                if (!isSmartAnimationGenerationCurrentLocked(aggregateKey, generation)) {
                    return@synchronized null
                }
                if (!ConverterPrefs(context).getAnimatedIslandEnabled()) {
                    if (smartAnimationGenerations[aggregateKey] == generation) {
                        smartAnimationGenerations.remove(aggregateKey)
                    }
                    smartAnimationStates.remove(aggregateKey)
                    return@synchronized null
                }
                val animationState = smartAnimationStates[aggregateKey] ?: return@synchronized null
                val nextToken = pickNextSmartAnimationToken(
                    tokens = animationState.tokens,
                    startIndex = animationState.nextIndex,
                    lastShownToken = animationState.lastShownToken
                ) ?: return@synchronized null

                animationState.nextIndex = nextToken.nextIndex
                animationState.lastShownToken = nextToken.token
                SmartAnimationFrame(
                    sbn = animationState.sbn,
                    appPresentationOverride = animationState.appPresentationOverride,
                    mirrorChannel = animationState.mirrorChannel,
                    progressOverride = animationState.progressOverride,
                    smartRuleId = animationState.smartRuleId,
                    token = nextToken.token,
                    compactCodeOverride = animationState.compactCodeOverride,
                    samsungBridge = animationState.samsungBridge
                )
            } ?: return@postDelayed

            try {
                val notification = buildMirroredNotification(
                    context = context,
                    sbn = frame.sbn,
                    appPresentationOverride = frame.appPresentationOverride,
                    mirrorChannel = frame.mirrorChannel,
                    progressOverride = frame.progressOverride,
                    otpOverride = null,
                    smartShortTextOverride = frame.token,
                    compactCodeOverride = frame.compactCodeOverride,
                    smartRuleId = frame.smartRuleId,
                    requestPromoted = true,
                    samsungBridge = frame.samsungBridge,
                    preferSmartShortTextAsPrimary = true
                )
                notifyWithPromotionFallback(
                    context = context,
                    manager = manager,
                    notificationId = mirrorIdForKey(aggregateKey),
                    mirrorKey = aggregateKey,
                    promotedNotification = notification,
                    sbn = frame.sbn,
                    appPresentationOverride = frame.appPresentationOverride,
                    mirrorChannel = frame.mirrorChannel,
                    progressOverride = frame.progressOverride,
                    otpOverride = null,
                    smartShortTextOverride = frame.token,
                    compactCodeOverride = frame.compactCodeOverride,
                    smartRuleId = frame.smartRuleId,
                    samsungBridge = frame.samsungBridge,
                    preferSmartShortTextAsPrimary = true
                )
            } catch (error: Throwable) {
                Log.e(TAG, "Failed smart island animation update: $aggregateKey", error)
            }
            if (!isSmartAnimationGenerationCurrent(aggregateKey, generation)) {
                return@postDelayed
            }
            scheduleSmartAnimationStep(
                context = context,
                manager = manager,
                aggregateKey = aggregateKey,
                generation = generation
            )
        }, nextSmartIslandDelayMs(context))
    }

    private fun isSmartAnimationGenerationCurrent(aggregateKey: String, generation: Long): Boolean {
        return synchronized(stateLock) {
            isSmartAnimationGenerationCurrentLocked(aggregateKey, generation)
        }
    }

    private fun isSmartAnimationGenerationCurrentLocked(aggregateKey: String, generation: Long): Boolean {
        val state = aggregateStates[aggregateKey] ?: return false
        if (state.activeSbnKeys.isEmpty()) {
            return false
        }
        if (!smartAnimationStates.containsKey(aggregateKey)) {
            return false
        }
        return smartAnimationGenerations[aggregateKey] == generation
    }

    private fun pickNextSmartAnimationToken(
        tokens: List<String?>,
        startIndex: Int,
        lastShownToken: String?
    ): SmartAnimationToken? {
        if (tokens.isEmpty()) {
            return null
        }
        var index = ((startIndex % tokens.size) + tokens.size) % tokens.size
        var attemptsLeft = tokens.size
        while (attemptsLeft > 0) {
            val token = tokens[index]
            if (!token.isNullOrBlank() && !token.equals(lastShownToken, ignoreCase = true)) {
                return SmartAnimationToken(
                    token = token,
                    nextIndex = (index + 1) % tokens.size
                )
            }
            index = (index + 1) % tokens.size
            attemptsLeft -= 1
        }
        return null
    }

    private fun nextSmartIslandDelayMs(context: Context): Long {
        return ConverterPrefs(context).getAnimatedIslandUpdateFrequencyMs().toLong()
    }

    private fun normalizeAnimatedToken(
        raw: String?,
        aospCuttingEnabled: Boolean,
        aospCuttingLength: Int
    ): String? {
        val normalized = raw.orEmpty()
            .replace(Regex("\\s+"), " ")
            .trim()
        if (normalized.isBlank()) {
            return null
        }
        return limitIslandText(
            normalized,
            aospCuttingEnabled,
            aospCuttingLength
        )
            .trim()
            .ifBlank { null }
    }

    private fun extractNavigationInstructionToken(
        text: String,
        parserDictionary: LiveParserDictionary
    ): String? {
        val match = parserDictionary.navigationInstructionPattern.find(text) ?: return null
        return match.value
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { null }
    }

    private fun extractTwoGisEtaDistanceText(
        notification: Notification,
        displayTitle: String,
        displayText: String,
        parserDictionary: LiveParserDictionary
    ): String? {
        val candidateLines = linkedSetOf<String>()
        splitNotificationTextLines(displayTitle).forEach(candidateLines::add)
        splitNotificationTextLines(displayText).forEach(candidateLines::add)
        extractRemoteViewTexts(notification)
            .flatMap(::splitNotificationTextLines)
            .filterNot(::isTwoGisAuxiliaryLine)
            .forEach(candidateLines::add)

        val etaRegex = Regex(
            "\\b\\d+\\s*(?:мин|min|minutes?|hrs?|hours?|hr|ч|час(?:а|ов)?)\\b",
            setOf(RegexOption.IGNORE_CASE)
        )

        return candidateLines.firstNotNullOfOrNull { candidate ->
            val etaMatch = etaRegex.find(candidate) ?: return@firstNotNullOfOrNull null
            val distanceMatch =
                parserDictionary.navigationDistancePattern.find(candidate, etaMatch.range.last + 1)
                    ?: return@firstNotNullOfOrNull null
            candidate.substring(etaMatch.range.first, distanceMatch.range.last + 1)
                .replace(Regex("\\s+"), " ")
                .trim()
                .ifBlank { null }
        }
    }

    private fun extractDeliveryEta(
        context: Context,
        notification: Notification,
        packageName: String,
        fallbackTitle: String
    ): DeliveryEtaText? {
        val renderedLines = extractRenderedRemoteViewTexts(
            context = context,
            notification = notification,
            packageName = packageName
        ).flatMap(::splitNotificationTextLines)
        val remoteLines = extractRemoteViewTexts(notification)
            .flatMap(::splitNotificationTextLines)
        val candidateLines = linkedSetOf<String>()
        renderedLines.forEach(candidateLines::add)
        splitNotificationTextLines(
            collectNotificationText(
                notification = notification,
                fallbackTitle = fallbackTitle,
                includeRemoteViewTexts = true
            )
        ).forEach(candidateLines::add)
        remoteLines.forEach(candidateLines::add)

        val inlineCandidates = candidateLines.mapNotNull { candidate ->
            DELIVERY_ETA_INLINE_PATTERN.find(candidate)
                ?.value
                ?.let(::parseDeliveryEtaText)
        }
        val adjacentCandidates = listOfNotNull(
            extractAdjacentDeliveryEta(renderedLines),
            extractAdjacentDeliveryEta(remoteLines)
        )
        val candidates = inlineCandidates + adjacentCandidates
        if (candidates.isEmpty()) {
            return null
        }
        val minuteCandidates = candidates.filter { it.totalMinutes != null }
        return if (minuteCandidates.isNotEmpty()) {
            minuteCandidates.minByOrNull { it.totalMinutes ?: Int.MAX_VALUE }
        } else {
            candidates.firstOrNull()
        }
    }

    private fun extractAdjacentDeliveryEta(lines: List<String>): DeliveryEtaText? {
        for (index in 0 until lines.lastIndex) {
            val number = lines[index].trim()
            val unit = lines[index + 1].trim()
            if (DELIVERY_ETA_NUMBER_PATTERN.matches(number) &&
                DELIVERY_ETA_UNIT_PATTERN.matches(unit)
            ) {
                return parseDeliveryEtaText("$number $unit")
            }
        }
        return null
    }

    private fun parseDeliveryEtaText(value: String): DeliveryEtaText? {
        val normalized = value
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { null }
            ?: return null
        val match = DELIVERY_ETA_INLINE_PATTERN.find(normalized) ?: return DeliveryEtaText(
            text = normalized,
            totalMinutes = null
        )
        val amount = match.groupValues.getOrNull(1)?.toIntOrNull()
        val unitText = match.groupValues.getOrNull(2)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "min"
        val normalizedUnit = unitText.lowercase(Locale.ROOT)
        val isMinuteUnit =
            normalizedUnit.startsWith("мин") ||
                    normalizedUnit.startsWith("min")
        return DeliveryEtaText(
            text = normalized,
            totalMinutes = amount?.takeIf { isMinuteUnit }
        )
    }

    private fun composeTwoGisVisibleSecondaryText(
        leadingText: String?,
        etaDistanceText: String?,
        fallbackText: String
    ): String? {
        val normalizedLeading = leadingText
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()
        val normalizedEtaDistance = etaDistanceText
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()
        val normalizedFallback = fallbackText
            .replace(Regex("\\s+"), " ")
            .trim()

        return when {
            normalizedLeading.isNotEmpty() && normalizedEtaDistance.isNotEmpty() ->
                "$normalizedLeading · $normalizedEtaDistance"
            normalizedEtaDistance.isNotEmpty() -> normalizedEtaDistance
            normalizedLeading.isNotEmpty() -> normalizedLeading
            normalizedFallback.isNotEmpty() -> normalizedFallback
            else -> null
        }
    }

    private fun resolveTwoGisRemoteViewMiniTextPair(
        notification: Notification,
        displayTitle: String,
        displayText: String,
        parserDictionary: LiveParserDictionary,
        preferInstructionPrimary: Boolean = false
    ): SamsungMiniTextPair? {
        val titleLines = splitNotificationTextLines(displayTitle)
        val displayLines = splitNotificationTextLines(displayText)
        val remoteLines = extractRemoteViewTexts(notification)
            .flatMap(::splitNotificationTextLines)
            .filterNot(::isTwoGisAuxiliaryLine)

        val candidateLines = linkedSetOf<String>()
        titleLines.forEach(candidateLines::add)
        displayLines.forEach(candidateLines::add)
        remoteLines.forEach(candidateLines::add)
        if (candidateLines.isEmpty()) {
            return null
        }

        if (!preferInstructionPrimary) {
            val primary = sequenceOf(
                candidateLines.firstOrNull { candidate ->
                    isNavigationDistanceText(candidate, parserDictionary)
                },
                pickFirstTwoGisNonAuxiliaryLine(displayLines),
                pickFirstTwoGisNonAuxiliaryLine(remoteLines),
                pickFirstTwoGisNonAuxiliaryLine(titleLines),
                candidateLines.firstOrNull { candidate ->
                    !isTwoGisAuxiliaryLine(candidate)
                },
                candidateLines.firstOrNull()
            ).firstOrNull { !it.isNullOrEmpty() } ?: return null

            val secondary = sequenceOf(
                displayLines.drop(1).firstOrNull { candidate ->
                    !isEquivalentText(candidate, primary) &&
                            !isTwoGisAuxiliaryLine(candidate)
                },
                pickFirstTwoGisDescriptiveLine(
                    lines = displayLines,
                    parserDictionary = parserDictionary,
                    excludedText = primary
                ),
                pickFirstTwoGisDescriptiveLine(
                    lines = remoteLines,
                    parserDictionary = parserDictionary,
                    excludedText = primary
                ),
                pickFirstTwoGisDescriptiveLine(
                    lines = titleLines,
                    parserDictionary = parserDictionary,
                    excludedText = primary
                ),
                candidateLines.firstOrNull { candidate ->
                    !isEquivalentText(candidate, primary) &&
                            !isNavigationDistanceText(candidate, parserDictionary) &&
                            !isTwoGisAuxiliaryLine(candidate)
                }
            ).firstOrNull { candidate ->
                !candidate.isNullOrEmpty() && !isEquivalentText(candidate, primary)
            } ?: return null

            return SamsungMiniTextPair(primaryText = primary, secondaryText = secondary)
        }

        val combinedText = buildString {
            if (displayTitle.isNotBlank()) {
                appendLine(displayTitle)
            }
            if (displayText.isNotBlank()) {
                appendLine(displayText)
            }
            remoteLines.forEach(::appendLine)
        }

        val primary = sequenceOf(
            extractNavigationInstructionToken(combinedText, parserDictionary),
            displayLines.firstNotNullOfOrNull { candidate ->
                extractTwoGisRouteTextCandidate(candidate, parserDictionary)
            },
            remoteLines.firstNotNullOfOrNull { candidate ->
                extractTwoGisRouteTextCandidate(candidate, parserDictionary)
            },
            titleLines.firstNotNullOfOrNull { candidate ->
                extractTwoGisRouteTextCandidate(candidate, parserDictionary)
            },
            pickFirstTwoGisDescriptiveLine(displayLines, parserDictionary),
            pickFirstTwoGisDescriptiveLine(remoteLines, parserDictionary),
            pickFirstTwoGisDescriptiveLine(titleLines, parserDictionary),
            pickFirstTwoGisNonAuxiliaryLine(displayLines),
            pickFirstTwoGisNonAuxiliaryLine(remoteLines),
            pickFirstTwoGisNonAuxiliaryLine(titleLines),
            candidateLines.firstOrNull { candidate ->
                !isTwoGisAuxiliaryLine(candidate)
            },
            candidateLines.firstOrNull()
        ).firstOrNull { !it.isNullOrEmpty() } ?: return null

        val secondary = sequenceOf(
            displayLines.firstOrNull { candidate ->
                !isEquivalentText(candidate, primary) &&
                        isNavigationDistanceText(candidate, parserDictionary)
            },
            remoteLines.firstOrNull { candidate ->
                !isEquivalentText(candidate, primary) &&
                        isNavigationDistanceText(candidate, parserDictionary)
            },
            titleLines.firstOrNull { candidate ->
                !isEquivalentText(candidate, primary) &&
                        isNavigationDistanceText(candidate, parserDictionary)
            },
            displayLines.firstNotNullOfOrNull { candidate ->
                extractTwoGisRouteTextCandidate(candidate, parserDictionary)
                    ?.takeIf { !isEquivalentText(it, primary) }
            },
            remoteLines.firstNotNullOfOrNull { candidate ->
                extractTwoGisRouteTextCandidate(candidate, parserDictionary)
                    ?.takeIf { !isEquivalentText(it, primary) }
            },
            titleLines.firstNotNullOfOrNull { candidate ->
                extractTwoGisRouteTextCandidate(candidate, parserDictionary)
                    ?.takeIf { !isEquivalentText(it, primary) }
            },
            pickFirstTwoGisDescriptiveLine(
                lines = displayLines,
                parserDictionary = parserDictionary,
                excludedText = primary
            ),
            pickFirstTwoGisDescriptiveLine(
                lines = remoteLines,
                parserDictionary = parserDictionary,
                excludedText = primary
            ),
            pickFirstTwoGisDescriptiveLine(
                lines = titleLines,
                parserDictionary = parserDictionary,
                excludedText = primary
            ),
            titleLines.drop(1).firstOrNull { candidate ->
                !isEquivalentText(candidate, primary) &&
                        !isTwoGisAuxiliaryLine(candidate)
            },
            displayLines.firstOrNull { candidate ->
                !isEquivalentText(candidate, primary) &&
                        !isTwoGisAuxiliaryLine(candidate)
            },
            remoteLines.firstOrNull { candidate ->
                !isEquivalentText(candidate, primary) &&
                        !isTwoGisAuxiliaryLine(candidate)
            },
            candidateLines.firstOrNull { candidate ->
                !isEquivalentText(candidate, primary) &&
                        !isTwoGisAuxiliaryLine(candidate)
            }
        ).firstOrNull { candidate ->
            !candidate.isNullOrEmpty() && !isEquivalentText(candidate, primary)
        } ?: return null

        return SamsungMiniTextPair(primaryText = primary, secondaryText = secondary)
    }

    private fun pickFirstTwoGisDescriptiveLine(
        lines: List<String>,
        parserDictionary: LiveParserDictionary,
        excludedText: String? = null
    ): String? {
        return lines.firstOrNull { candidate ->
            !isEquivalentText(candidate, excludedText.orEmpty()) &&
                    !isTwoGisAuxiliaryLine(candidate) &&
                    !isNavigationDistanceText(candidate, parserDictionary)
        }
    }

    private fun pickFirstTwoGisNonAuxiliaryLine(
        lines: List<String>,
        excludedText: String? = null
    ): String? {
        return lines.firstOrNull { candidate ->
            !isEquivalentText(candidate, excludedText.orEmpty()) &&
                    !isTwoGisAuxiliaryLine(candidate)
        }
    }

    private fun extractTwoGisRouteTextCandidate(
        value: String,
        parserDictionary: LiveParserDictionary
    ): String? {
        val normalized = value
            .replace(Regex("\\s+"), " ")
            .trim()
        if (normalized.isEmpty() || isTwoGisAuxiliaryLine(normalized)) {
            return null
        }

        val distanceMatch = parserDictionary.navigationDistancePattern.find(normalized)
            ?: return normalized.takeIf {
                !isNavigationDistanceText(it, parserDictionary) && !isTwoGisAuxiliaryLine(it)
            }
        if (distanceMatch.range.first != 0) {
            return null
        }

        val remainder = normalized
            .substring(distanceMatch.range.last + 1)
            .trimStart(' ', '-', '–', '—', '·', '•', ':')
            .trim()
        return remainder.takeIf { it.isNotEmpty() && !isTwoGisAuxiliaryLine(it) }
    }

    private fun splitNotificationTextLines(value: String): List<String> {
        return value
            .lineSequence()
            .map { line -> line.replace(Regex("\\s+"), " ").trim() }
            .filter { line -> line.isNotEmpty() }
            .toList()
    }

    private fun isTwoGisAuxiliaryLine(value: String): Boolean {
        val normalized = normalizeComparableText(value)
        return normalized == "2gis" ||
                isLikelyNotificationActionLabel(normalized) ||
                isRemoteViewMethodLabel(normalized)
    }

    private fun isLikelyNotificationActionLabel(normalizedValue: String): Boolean {
        return normalizedValue == "finish route" ||
                normalizedValue == "end route" ||
                normalizedValue == "stop navigation" ||
                normalizedValue.contains("route") ||
                normalizedValue.contains("navigation")
    }

    private fun isRemoteViewMethodLabel(normalizedValue: String): Boolean {
        return normalizedValue == "settext" ||
                normalizedValue == "setcharsequence" ||
                normalizedValue.startsWith("settext ") ||
                normalizedValue.startsWith("setcharsequence ")
    }

    private fun resolveRemoteViewMiniTextPair(
        notification: Notification,
        fallbackTitle: String,
        parserDictionary: LiveParserDictionary,
        smartRuleId: String?,
        compactPrimaryText: String,
        displayTitle: String,
        displayText: String,
        smartShortTextOverride: String?,
        hasProgress: Boolean
    ): SamsungMiniTextPair? {
        val remoteTexts = extractRemoteViewTexts(notification)
        if (remoteTexts.isEmpty()) {
            return null
        }

        if (smartRuleId == "navigation") {
            val combinedText = collectNotificationText(
                notification = notification,
                fallbackTitle = fallbackTitle,
                includeRemoteViewTexts = true
            )
            val primary = sequenceOf(
                smartShortTextOverride?.trim()?.takeIf {
                    isNavigationDistanceText(it, parserDictionary)
                },
                extractNavigationDistanceText(
                    notification = notification,
                    fallbackTitle = fallbackTitle,
                    parserDictionary = parserDictionary
                ),
                displayText.trim().takeIf { isNavigationDistanceText(it, parserDictionary) },
                compactPrimaryText.trim().takeIf { isNavigationDistanceText(it, parserDictionary) },
                remoteTexts.firstOrNull { candidate ->
                    isNavigationDistanceText(candidate, parserDictionary)
                }?.trim()
            ).firstOrNull { !it.isNullOrEmpty() }

            val secondary = sequenceOf(
                extractNavigationInstructionToken(combinedText, parserDictionary),
                displayTitle.trim(),
                displayText.trim(),
                compactPrimaryText.trim(),
                remoteTexts.firstOrNull { candidate ->
                    !isEquivalentText(candidate, primary) &&
                            !isNavigationDistanceText(candidate, parserDictionary)
                }?.trim()
            ).firstOrNull { candidate ->
                !candidate.isNullOrEmpty() &&
                        !isEquivalentText(candidate, primary) &&
                        !isNavigationDistanceText(candidate, parserDictionary)
            }

            return if (!primary.isNullOrBlank() && !secondary.isNullOrBlank()) {
                SamsungMiniTextPair(primaryText = primary, secondaryText = secondary)
            } else {
                null
            }
        }

        val primary = sequenceOf(
            compactPrimaryText.trim().takeIf { candidate ->
                candidate.isNotEmpty() && !isEquivalentText(candidate, fallbackTitle)
            },
            displayTitle.trim().takeIf { candidate ->
                candidate.isNotEmpty() && !isEquivalentText(candidate, fallbackTitle)
            },
            remoteTexts.firstOrNull()?.trim()
        ).firstOrNull { !it.isNullOrEmpty() } ?: return null

        val secondary = sequenceOf(
            smartShortTextOverride?.trim()?.takeIf { !hasProgress },
            remoteTexts.firstOrNull { candidate ->
                !isEquivalentText(candidate, primary)
            }?.trim(),
            displayText.trim().takeIf {
                it.isNotEmpty() && !isGenericLiveUpdatePlaceholder(it)
            }
        ).firstOrNull { candidate ->
            !candidate.isNullOrEmpty() && !isEquivalentText(candidate, primary)
        }

        return if (!secondary.isNullOrBlank()) {
            SamsungMiniTextPair(primaryText = primary, secondaryText = secondary)
        } else {
            null
        }
    }

    private fun isNavigationDistanceText(
        value: String?,
        parserDictionary: LiveParserDictionary
    ): Boolean {
        val normalized = value?.trim().orEmpty()
        if (normalized.isEmpty()) {
            return false
        }
        return parserDictionary.navigationDistancePattern.containsMatchIn(normalized) ||
                NAVIGATION_DISTANCE_PATTERN.containsMatchIn(normalized)
    }

    private fun isEquivalentText(left: String?, right: String?): Boolean {
        val normalizedLeft = normalizeComparableText(left)
        val normalizedRight = normalizeComparableText(right)
        return normalizedLeft.isNotEmpty() &&
                normalizedRight.isNotEmpty() &&
                normalizedLeft == normalizedRight
    }

    private fun normalizeComparableText(value: String?): String {
        return value.orEmpty()
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase(Locale.ROOT)
    }

    private fun isGenericLiveUpdatePlaceholder(value: String?): Boolean {
        return isEquivalentText(value, "Live update in progress")
    }

    private fun extractWeatherDayToken(
        text: String,
        parserDictionary: LiveParserDictionary
    ): String? {
        val match = parserDictionary.weatherDayPattern.find(text) ?: return null
        return match.value.trim().ifBlank { null }
    }

    private fun extractWeatherConditionToken(
        text: String,
        parserDictionary: LiveParserDictionary
    ): String? {
        val match = parserDictionary.weatherConditionPattern.find(text) ?: return null
        return match.value.trim().ifBlank { null }
    }

    private fun extractWeatherConditionEmoji(
        text: String,
        parserDictionary: LiveParserDictionary
    ): String? {
        return when {
            parserDictionary.weatherConditionThunderPattern.containsMatchIn(text) -> "\u26c8\ufe0f"
            parserDictionary.weatherConditionRainPattern.containsMatchIn(text) -> "\ud83c\udf27\ufe0f"
            parserDictionary.weatherConditionSnowPattern.containsMatchIn(text) -> "\u2744\ufe0f"
            parserDictionary.weatherConditionFogPattern.containsMatchIn(text) -> "\ud83c\udf2b\ufe0f"
            parserDictionary.weatherConditionWindPattern.containsMatchIn(text) -> "\ud83c\udf2c\ufe0f"
            parserDictionary.weatherConditionSunPattern.containsMatchIn(text) -> "\u2600\ufe0f"
            parserDictionary.weatherConditionCloudPattern.containsMatchIn(text) -> "\u2601\ufe0f"
            else -> null
        }
    }

    private fun applySmallIcon(
        context: Context,
        builder: NotificationCompat.Builder,
        sourceIcon: IconCompat?
    ) {
        builder.setSmallIcon(
            sourceIcon ?: IconCompat.createWithResource(context, R.drawable.ic_stat_liveupdate)
        )
    }

    private fun resolveSourceSmallIcon(context: Context, sbn: StatusBarNotification): IconCompat? {
        val source = sbn.notification
        val packageContext = runCatching {
            context.createPackageContext(sbn.packageName, 0)
        }.getOrNull()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val frameworkSmallIcon = source.smallIcon
            if (frameworkSmallIcon != null) {
                iconToBitmap(packageContext ?: context, frameworkSmallIcon)?.let { bitmap ->
                    runCatching { IconCompat.createWithBitmap(bitmap) }.getOrNull()?.let { return it }
                }
                try {
                    return IconCompat.createFromIcon(context, frameworkSmallIcon)
                } catch (_: Exception) {
                }
            }
        }

        val legacyIconRes = source.icon
        if (legacyIconRes == 0) {
            return null
        }

        packageContext?.let { packageCtx ->
            runCatching {
                packageCtx.getDrawable(legacyIconRes)?.let(::drawableToBitmap)
            }.getOrNull()?.let { bitmap ->
                runCatching { IconCompat.createWithBitmap(bitmap) }.getOrNull()?.let { return it }
            }
        }

        return try {
            IconCompat.createWithResource(
                (packageContext ?: context).resources,
                sbn.packageName,
                legacyIconRes
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveAppSmallIcon(context: Context, packageName: String): IconCompat? {
        return resolveAppIconAssets(context, packageName)?.smallIcon
    }

    private fun resolveAppLargeIconBitmap(context: Context, packageName: String): Bitmap? {
        return resolveAppIconAssets(context, packageName)?.largeIconBitmap
    }

    private fun resolveAppIconAssets(context: Context, packageName: String): AppIconAssets? {
        val normalizedPackage = packageName.trim()
        if (normalizedPackage.isEmpty()) {
            return null
        }

        synchronized(appIconCacheLock) {
            appIconCache[normalizedPackage]?.let { return it }
            if (missingAppIconPackages.contains(normalizedPackage)) {
                return null
            }
        }

        val resolved = try {
            val appInfo = context.packageManager.getApplicationInfo(normalizedPackage, 0)
            if (appInfo.icon == 0) {
                null
            } else {
                val packageContext = context.createPackageContext(normalizedPackage, 0)
                val samsungTrayBitmap = resolveSamsungTrayIconBitmap(
                    context = context,
                    packageName = normalizedPackage
                )
                val resourceIcon = runCatching {
                    IconCompat.createWithResource(
                        packageContext.resources,
                        normalizedPackage,
                        appInfo.icon
                    )
                }.getOrNull()
                val bitmap = runCatching {
                    packageContext.getDrawable(appInfo.icon)?.let { drawable ->
                        drawableToBitmap(drawable, clipAdaptiveIcon = true)
                    }
                }.getOrNull()
                val smallIcon = samsungTrayBitmap
                    ?.let { runCatching { IconCompat.createWithBitmap(it) }.getOrNull() }
                    ?: resourceIcon
                    ?: bitmap?.let { runCatching { IconCompat.createWithBitmap(it) }.getOrNull() }
                val largeIconBitmap = bitmap ?: samsungTrayBitmap

                if (smallIcon == null && largeIconBitmap == null) {
                    null
                } else {
                    AppIconAssets(
                        smallIcon = smallIcon,
                        largeIconBitmap = largeIconBitmap
                    )
                }
            }
        } catch (_: Exception) {
            null
        }

        synchronized(appIconCacheLock) {
            if (resolved != null) {
                appIconCache[normalizedPackage] = resolved
            } else {
                missingAppIconPackages.add(normalizedPackage)
            }
        }

        return resolved
    }

    // Follow nowbar-sdk on Samsung and prefer the icon-tray variant for small app icons.
    private fun resolveSamsungTrayIconBitmap(
        context: Context,
        packageName: String
    ): Bitmap? {
        return runCatching {
            val method = context.packageManager.javaClass.getMethod(
                "semGetApplicationIconForIconTray",
                String::class.java,
                Int::class.javaPrimitiveType
            )
            val drawable = method.invoke(
                context.packageManager,
                packageName,
                SAMSUNG_TRAY_ICON_SIZE
            ) as? Drawable
            drawable?.let(::drawableToBitmap)
        }.getOrNull()
    }

    private fun resolveSourceLargeIconBitmap(context: Context, notification: Notification): Bitmap? {
        val extras = notification.extras
        val fromExtras = extras.get(Notification.EXTRA_LARGE_ICON)
        when (fromExtras) {
            is Bitmap -> return fromExtras
            is android.graphics.drawable.Icon -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    iconToBitmap(context, fromExtras)?.let { return it }
                }
            }
        }

        val fromBigExtras = extras.get(Notification.EXTRA_LARGE_ICON_BIG)
        when (fromBigExtras) {
            is Bitmap -> return fromBigExtras
            is android.graphics.drawable.Icon -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    iconToBitmap(context, fromBigExtras)?.let { return it }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notification.getLargeIcon()?.let { return iconToBitmap(context, it) }
        }

        @Suppress("DEPRECATION")
        return notification.largeIcon
    }

    private fun resolveRemoteDrawableAssets(
        context: Context,
        sbn: StatusBarNotification
    ): RemoteDrawableAssets? {
        val packageContext = try {
            context.createPackageContext(sbn.packageName, 0)
        } catch (_: Exception) {
            return null
        }

        val resources = packageContext.resources
        val source = sbn.notification
        val drawableResId =
            extractFirstRemoteDrawableResId(source.contentView, resources)
                ?: extractFirstRemoteDrawableResId(source.bigContentView, resources)
                ?: extractFirstRemoteDrawableResId(source.headsUpContentView, resources)
                ?: return null

        val rawBitmap = try {
            packageContext.getDrawable(drawableResId)?.let { drawable ->
                drawableToBitmap(drawable)
            }
        } catch (_: Exception) {
            null
        }
        val bitmap = rawBitmap?.let(::tintBitmapWhite)
        val icon = bitmap?.let {
            runCatching { IconCompat.createWithBitmap(it) }.getOrNull()
        } ?: runCatching {
            IconCompat.createWithResource(resources, sbn.packageName, drawableResId)
        }.getOrNull()

        if (icon == null && bitmap == null) {
            return null
        }
        return RemoteDrawableAssets(
            icon = icon,
            bitmap = bitmap
        )
    }

    private fun iconToBitmap(context: Context, icon: android.graphics.drawable.Icon): Bitmap? {
        return try {
            icon.loadDrawable(context)?.let(::drawableToBitmap)
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToBitmap(
        drawable: Drawable,
        clipAdaptiveIcon: Boolean = false
    ): Bitmap {
        if (drawable is android.graphics.drawable.BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        if (clipAdaptiveIcon &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            drawable is AdaptiveIconDrawable
        ) {
            return adaptiveIconToBitmap(drawable)
        }
        val width = drawable.intrinsicWidth.coerceAtLeast(1).coerceAtMost(512)
        val height = drawable.intrinsicHeight.coerceAtLeast(1).coerceAtMost(512)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun adaptiveIconToBitmap(drawable: AdaptiveIconDrawable): Bitmap {
        val size = maxOf(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1)
        ).coerceAtMost(512)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)

        val originalMask = Path(drawable.iconMask)
        val maskBounds = RectF()
        originalMask.computeBounds(maskBounds, true)
        if (maskBounds.width() <= 0f || maskBounds.height() <= 0f) {
            drawable.draw(canvas)
            return bitmap
        }

        val normalizedBounds = RectF(0f, 0f, maskBounds.width(), maskBounds.height())
        val scale = minOf(
            size / normalizedBounds.width(),
            size / normalizedBounds.height()
        )
        val dx = (size - normalizedBounds.width() * scale) / 2f
        val dy = (size - normalizedBounds.height() * scale) / 2f
        val maskMatrix = Matrix().apply {
            postTranslate(-maskBounds.left, -maskBounds.top)
            postScale(scale, scale)
            postTranslate(dx, dy)
        }
        val scaledMask = Path()
        originalMask.transform(maskMatrix, scaledMask)

        canvas.save()
        canvas.clipPath(scaledMask)
        drawable.draw(canvas)
        canvas.restore()
        return bitmap
    }

    private fun tintBitmapWhite(source: Bitmap): Bitmap {
        val mutableSource =
            if (source.config == Bitmap.Config.ARGB_8888 && source.isMutable) {
                source
            } else {
                source.copy(Bitmap.Config.ARGB_8888, true)
            } ?: source

        val result = Bitmap.createBitmap(
            mutableSource.width,
            mutableSource.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        }
        canvas.drawBitmap(mutableSource, 0f, 0f, paint)
        return result
    }

    private fun extractRenderedRemoteViewTexts(
        context: Context,
        notification: Notification,
        packageName: String
    ): List<String> {
        val remoteViews = listOfNotNull(
            notification.contentView,
            notification.bigContentView,
            notification.headsUpContentView
        )
        if (remoteViews.isEmpty()) {
            return emptyList()
        }

        val packageContext = runCatching {
            context.createPackageContext(packageName, Context.CONTEXT_RESTRICTED)
        }.getOrElse {
            context
        }
        val applyContexts = listOf(context, packageContext)
            .distinctBy { it.packageName }
        val values = linkedSetOf<String>()
        for (remoteView in remoteViews) {
            for (applyContext in applyContexts) {
                val root = runCatching {
                    val parent = FrameLayout(applyContext)
                    remoteView.apply(applyContext, parent)
                }.getOrNull() ?: continue
                collectRenderedTextViews(root, values)
            }
        }
        return values.toList()
    }

    private fun collectRenderedTextViews(view: View, values: MutableSet<String>) {
        if (view is TextView) {
            val normalized = NotificationTextNormalizer.normalize(view.text)
            if (!normalized.isNullOrEmpty()) {
                values.add(normalized)
            }
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                collectRenderedTextViews(view.getChildAt(index), values)
            }
        }
    }

    private fun extractRemoteViewTexts(notification: Notification): List<String> {
        val values = linkedSetOf<String>()
        val remoteViews = listOfNotNull(
            notification.contentView,
            notification.bigContentView,
            notification.headsUpContentView
        )

        for (rv in remoteViews) {
            val actions = getRemoteViewActions(rv)
            for (action in actions) {
                val fields = collectAllDeclaredFields(action.javaClass)
                val methodName = fields.firstNotNullOfOrNull { field ->
                    val normalized = field.name.removePrefix("m").lowercase(Locale.ROOT)
                    if (normalized != "methodname") {
                        null
                    } else {
                        runCatching {
                            field.isAccessible = true
                            field.get(action) as? String
                        }.getOrNull()
                    }
                }?.lowercase(Locale.ROOT).orEmpty()
                val likelyTextAction =
                    methodName.contains("settext") || methodName.contains("setcharsequence")

                for (field in fields) {
                    val fieldName = field.name.removePrefix("m").lowercase(Locale.ROOT)
                    val value = runCatching {
                        field.isAccessible = true
                        field.get(action)
                    }.getOrNull()
                    when (value) {
                        is CharSequence -> {
                            val normalized = NotificationTextNormalizer.normalize(value)
                            if (!normalized.isNullOrEmpty()) {
                                if (!shouldSkipRemoteViewText(fieldName, normalized, methodName) &&
                                    (likelyTextAction || field.name.contains("text", ignoreCase = true))
                                ) {
                                    values.add(normalized)
                                }
                            }
                        }

                        is Array<*> -> {
                            value.filterIsInstance<CharSequence>()
                                .mapNotNull { NotificationTextNormalizer.normalize(it) }
                                .forEach(values::add)
                        }
                    }
                }
            }
        }
        return values.toList()
    }

    private fun shouldSkipRemoteViewText(
        fieldName: String,
        text: String,
        methodName: String
    ): Boolean {
        val normalizedText = text.lowercase(Locale.ROOT)
        return fieldName == "methodname" ||
                normalizedText == methodName ||
                isRemoteViewMethodLabel(normalizedText)
    }

    private fun extractFirstRemoteDrawableResId(
        rv: android.widget.RemoteViews?,
        resources: android.content.res.Resources
    ): Int? {
        val actions = getRemoteViewActions(rv)
        if (actions.isEmpty()) {
            return null
        }

        for (action in actions) {
            val fields = collectAllDeclaredFields(action.javaClass)
            val actionClassName = action.javaClass.name.lowercase(Locale.ROOT)
            var methodName = ""
            val candidates = mutableListOf<Pair<String, Int>>()

            for (field in fields) {
                val value = runCatching {
                    field.isAccessible = true
                    field.get(action)
                }.getOrNull() ?: continue
                val normalizedName = field.name.removePrefix("m").lowercase(Locale.ROOT)
                if (normalizedName == "methodname" && value is String) {
                    methodName = value.lowercase(Locale.ROOT)
                }
                candidates.addAll(extractDrawableResIdCandidates(value, normalizedName))
            }

            val looksLikeImageAction =
                methodName.contains("icon") ||
                        methodName.contains("image") ||
                        methodName.contains("drawable") ||
                        actionClassName.contains("icon") ||
                        actionClassName.contains("image") ||
                        actionClassName.contains("drawable")
            if (!looksLikeImageAction) {
                continue
            }

            for ((fieldName, resId) in candidates) {
                val isResourceField =
                    fieldName.contains("res") ||
                            fieldName.contains("icon") ||
                            fieldName.contains("drawable") ||
                            fieldName.contains("value")
                if (!isResourceField) {
                    continue
                }
                if (isDrawableResource(resources, resId)) {
                    return resId
                }
            }
        }
        return null
    }

    private fun extractDrawableResIdCandidates(value: Any, fieldName: String): List<Pair<String, Int>> {
        val candidates = mutableListOf<Pair<String, Int>>()
        when (value) {
            is Int -> {
                if (value > 0) {
                    candidates += fieldName to value
                }
            }

            is IntArray -> {
                value.filter { it > 0 }.forEachIndexed { index, item ->
                    candidates += "$fieldName:$index" to item
                }
            }

            is Array<*> -> {
                value.forEachIndexed { index, item ->
                    if (item != null) {
                        candidates += extractDrawableResIdCandidates(item, "$fieldName:$index")
                    }
                }
            }

            is List<*> -> {
                value.forEachIndexed { index, item ->
                    if (item != null) {
                        candidates += extractDrawableResIdCandidates(item, "$fieldName:$index")
                    }
                }
            }

            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    value is android.graphics.drawable.Icon &&
                    value.type == android.graphics.drawable.Icon.TYPE_RESOURCE
                ) {
                    val resId = value.resId
                    if (resId > 0) {
                        candidates += "$fieldName:icon" to resId
                    }
                }
            }
        }
        return candidates
    }

    private fun getRemoteViewActions(rv: android.widget.RemoteViews?): List<Any> {
        rv ?: return emptyList()
        return try {
            val actionsField = rv.javaClass.getDeclaredField("mActions")
            actionsField.isAccessible = true
            (actionsField.get(rv) as? List<*>)?.filterNotNull() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun collectAllDeclaredFields(clazz: Class<*>): List<java.lang.reflect.Field> {
        val fields = mutableListOf<java.lang.reflect.Field>()
        var current: Class<*>? = clazz
        while (current != null && current != Any::class.java) {
            fields.addAll(current.declaredFields)
            current = current.superclass
        }
        return fields
    }

    private fun isDrawableResource(resources: android.content.res.Resources, resId: Int): Boolean {
        return try {
            val typeName = resources.getResourceTypeName(resId)
            typeName == "drawable" || typeName == "mipmap"
        } catch (_: Exception) {
            false
        }
    }

    private fun buildCopyOtpAction(
        context: Context,
        sbn: StatusBarNotification,
        otpCode: String
    ): NotificationCompat.Action {
        val copyIntent = Intent(context, OtpCopyReceiver::class.java).apply {
            action = OtpCopyReceiver.ACTION_COPY_OTP
            putExtra(OtpCopyReceiver.EXTRA_OTP_CODE, otpCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            mirrorIdForKey("${sbn.key}:otp_copy"),
            copyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            IconCompat.createWithResource(context, R.drawable.ic_content_copy_24),
            otpActionLabel(context),
            pendingIntent
        ).build()
    }

    private fun otpActionLabel(context: Context): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }

        val language = locale?.language?.lowercase(Locale.ROOT).orEmpty()
        return if (language.startsWith("ru")) "Скопировать код" else "Copy code"
    }

    private fun copySourceActions(
        source: Notification,
        builder: NotificationCompat.Builder,
        maxActions: Int,
        preferMediaControls: Boolean = false,
        mediaPlaybackIsPlaying: Boolean? = null,
        useMediaActionSymbols: Boolean = false
    ) {
        val actions = source.actions ?: return
        if (actions.isEmpty()) {
            return
        }

        val safeMaxActions = maxActions.coerceAtLeast(0)
        if (safeMaxActions == 0) {
            return
        }

        if (preferMediaControls) {
            val preferredMediaActions = selectPreferredMediaActions(
                actions = actions.toList(),
                isPlaying = mediaPlaybackIsPlaying,
                useSymbols = useMediaActionSymbols
            )
            if (preferredMediaActions.isNotEmpty()) {
                preferredMediaActions
                    .take(safeMaxActions)
                    .forEach { preferredAction ->
                        val compatAction = toCompatAction(
                            frameworkAction = preferredAction.action,
                            titleOverride = preferredAction.shortTitle
                        ) ?: return@forEach
                        builder.addAction(compatAction)
                    }
                return
            }
        }

        actions.take(safeMaxActions).forEach { frameworkAction ->
            val compatAction = toCompatAction(frameworkAction) ?: return@forEach
            builder.addAction(compatAction)
        }
    }

    private fun selectPreferredMediaActions(
        actions: List<Notification.Action>,
        isPlaying: Boolean?,
        useSymbols: Boolean
    ): List<MediaPreferredAction> {
        if (actions.isEmpty()) {
            return emptyList()
        }

        val indexed = actions.withIndex()
            .filter { it.value.actionIntent != null }
            .toList()
        if (indexed.isEmpty()) {
            return emptyList()
        }

        val usedIndexes = mutableSetOf<Int>()

        fun pickByKeywords(keywords: List<String>): Notification.Action? {
            val candidate = indexed.firstOrNull { (index, action) ->
                if (index in usedIndexes) {
                    return@firstOrNull false
                }
                val title = NotificationTextNormalizer.normalize(action.title)
                    ?.lowercase(Locale.ROOT)
                    .orEmpty()
                keywords.any(title::contains)
            } ?: return null
            usedIndexes += candidate.index
            return candidate.value
        }

        val previousAction = pickByKeywords(
            listOf("previous", "prev", "назад", "пред", "rewind", "⏮")
        )
        val pauseAction = pickByKeywords(
            listOf("pause", "пауза", "⏸")
        )
        val playAction = pickByKeywords(
            listOf("play", "играть", "воспроиз", "resume", "▶", "⏯")
        )
        val nextAction = pickByKeywords(
            listOf("next", "след", "skip", "forward", "⏭")
        )

        val centerAction = when (isPlaying) {
            true -> pauseAction ?: playAction
            false -> playAction ?: pauseAction
            null -> pauseAction ?: playAction
        }

        fun actionTitle(text: String, symbol: String): String {
            return if (useSymbols) symbol else text
        }

        val centerShortTitle = when {
            centerAction != null && centerAction == playAction ->
                actionTitle("Play", MEDIA_SYMBOL_PLAY)

            centerAction != null && centerAction == pauseAction ->
                actionTitle("Pause", MEDIA_SYMBOL_PAUSE)

            isPlaying == false -> actionTitle("Play", MEDIA_SYMBOL_PLAY)
            else -> actionTitle("Pause", MEDIA_SYMBOL_PAUSE)
        }

        val ordered = listOfNotNull(
            previousAction?.let {
                MediaPreferredAction(it, actionTitle("Previous", MEDIA_SYMBOL_PREVIOUS))
            },
            centerAction?.let {
                MediaPreferredAction(
                    action = it,
                    shortTitle = centerShortTitle
                )
            },
            nextAction?.let { MediaPreferredAction(it, actionTitle("Next", MEDIA_SYMBOL_NEXT)) }
        )
        return if (ordered.size >= 2) ordered else emptyList()
    }

    private fun toCompatAction(
        frameworkAction: Notification.Action,
        titleOverride: String? = null
    ): NotificationCompat.Action? {
        if (frameworkAction.actionIntent == null) {
            return null
        }

        return try {
            val copied = NotificationCompat.Action.Builder.fromAndroidAction(frameworkAction).build()
            NotificationCompat.Action.Builder(
                transparentActionIcon,
                titleOverride?.takeIf { it.isNotBlank() }
                    ?: NotificationTextNormalizer.normalize(copied.title)
                    ?: NotificationTextNormalizer.normalize(frameworkAction.title)
                    ?: "Action",
                copied.actionIntent ?: frameworkAction.actionIntent
            ).build()
        } catch (_: Exception) {
            val title = titleOverride?.takeIf { it.isNotBlank() }
                ?: NotificationTextNormalizer.normalize(frameworkAction.title)
                ?: "Action"
            NotificationCompat.Action.Builder(
                transparentActionIcon,
                title,
                frameworkAction.actionIntent
            ).build()
        }
    }

    private fun hasEffectiveProgress(sourcePackageName: String, notification: Notification): Boolean {
        if (!hasProgress(notification)) {
            return false
        }
        return !shouldIgnoreNativeProgress(sourcePackageName, notification)
    }

    private fun hasProgress(notification: Notification): Boolean {
        val extras = notification.extras
        val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val indeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)
        return max > 0 || indeterminate
    }

    private fun shouldIgnoreNativeProgress(
        sourcePackageName: String,
        notification: Notification
    ): Boolean {
        if (SamsungLiveUpdateReparser.isSamsungDevice()) {
            return false
        }
        if (sourcePackageName.lowercase(Locale.ROOT) != TWO_GIS_PACKAGE) {
            return false
        }
        val extras = notification.extras
        val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val progressValue = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        val indeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)
        return !indeterminate && progressMax == 100 && progressValue == 0
    }

    private fun extractTitle(
        notification: Notification,
        fallbackName: String,
        allowRemoteViewFallback: Boolean
    ): String {
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
        val normalizedTitle = NotificationTextNormalizer.normalize(title)
        if (normalizedTitle != null) {
            return normalizedTitle
        }
        if (!allowRemoteViewFallback) {
            return fallbackName
        }
        val remoteTitle = extractRemoteViewTexts(notification).firstOrNull()
        return remoteTitle ?: fallbackName
    }

    private fun extractText(notification: Notification, allowRemoteViewFallback: Boolean): String {
        val extras = notification.extras
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
        val normalized = NotificationTextNormalizer.normalize(text)
        if (normalized != null) {
            return normalized
        }
        if (!allowRemoteViewFallback) {
            return "Live update in progress"
        }
        val remoteText = extractRemoteViewTexts(notification).firstOrNull()
        return remoteText ?: "Live update in progress"
    }

    private fun resolveCallMirrorBodyText(
        notification: Notification,
        displayTitle: String,
        displayText: String,
        includeRemoteViewTexts: Boolean
    ): String? {
        val extras = notification.extras
        val titleTexts = linkedSetOf<String>()
        val candidates = linkedSetOf<String>()

        fun normalized(value: CharSequence?): String? {
            return NotificationTextNormalizer.normalize(value)
        }

        fun addTitle(value: CharSequence?) {
            normalized(value)?.let(titleTexts::add)
        }

        fun addCandidate(value: CharSequence?) {
            val text = normalized(value) ?: return
            if (!isGeneratedCallBodyFallback(text)) {
                candidates.add(text)
            }
        }

        addTitle(extras.getCharSequence(Notification.EXTRA_TITLE))
        addTitle(extras.getCharSequence(Notification.EXTRA_TITLE_BIG))
        addTitle(displayTitle)

        addCandidate(extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
        addCandidate(extras.getCharSequence(Notification.EXTRA_TEXT))
        addCandidate(extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
        addCandidate(extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT))
        addCandidate(extras.getCharSequence(Notification.EXTRA_INFO_TEXT))
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach(::addCandidate)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            extras.getParcelableArray(Notification.EXTRA_MESSAGES)
                ?.let(Notification.MessagingStyle.Message::getMessagesFromBundleArray)
                ?.asReversed()
                ?.firstOrNull { message -> !message.text.isNullOrBlank() }
                ?.let { message -> addCandidate(message.text) }
        }
        if (includeRemoteViewTexts) {
            extractRemoteViewTexts(notification).forEach(::addCandidate)
        }
        addCandidate(displayText)

        val nonTitleCandidate = candidates.firstOrNull { candidate ->
            titleTexts.none { title -> isEquivalentText(candidate, title) }
        }
        return nonTitleCandidate
            ?: candidates.firstOrNull()
            ?: titleTexts.firstOrNull { !isGeneratedCallBodyFallback(it) }
    }

    private fun isGeneratedCallBodyFallback(text: String): Boolean {
        return text.equals("Live update in progress", ignoreCase = true)
    }

    private fun collectNotificationText(
        notification: Notification,
        fallbackTitle: String,
        includeRemoteViewTexts: Boolean
    ): String {
        val extras = notification.extras
        val parts = mutableListOf<String>()

        fun add(value: CharSequence?) {
            val text = NotificationTextNormalizer.normalize(value)
            if (text != null) {
                parts.add(text)
            }
        }

        add(extras.getCharSequence(Notification.EXTRA_TITLE))
        add(extras.getCharSequence(Notification.EXTRA_TITLE_BIG))
        add(extras.getCharSequence(Notification.EXTRA_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_INFO_TEXT))
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.forEach(::add)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            messages
                ?.let(Notification.MessagingStyle.Message::getMessagesFromBundleArray)
                ?.asReversed()
                ?.firstOrNull { message -> !message.text.isNullOrBlank() }
                ?.let { message -> add(message.text) }
        }
        if (includeRemoteViewTexts) {
            extractRemoteViewTexts(notification).forEach { add(it) }
        }

        if (parts.isEmpty()) {
            parts.add(fallbackTitle)
        }

        return parts
            .distinct()
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isLikelyMediaPlaybackNotification(notification: Notification): Boolean {
        if (notification.category == Notification.CATEGORY_TRANSPORT) {
            return true
        }
        val extras = notification.extras
        if (extras.get(Notification.EXTRA_MEDIA_SESSION) != null) {
            return true
        }
        val template = extras.getString("android.template")
        return template?.contains("MediaStyle", ignoreCase = true) == true
    }

    private fun extractMediaPlaybackSnapshot(
        context: Context,
        notification: Notification,
        sourcePackageName: String
    ): MediaPlaybackSnapshot? {
        val sessionToken = extractMediaSessionToken(notification)
        val mediaController = resolveMediaController(
            context = context,
            sessionToken = sessionToken,
            sourcePackageName = sourcePackageName
        ) ?: return null

        return try {
            val playbackState = mediaController.playbackState ?: return null
            if (playbackState.state == PlaybackState.STATE_STOPPED ||
                playbackState.state == PlaybackState.STATE_NONE
            ) {
                return null
            }

            val metadata = mediaController.metadata
            val durationMs =
                metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0L) ?: 0L
            val rawPositionMs = resolvePlaybackStatePositionMs(playbackState).coerceAtLeast(0L)
            val positionMs = if (durationMs > 0L) {
                rawPositionMs.coerceIn(0L, durationMs)
            } else {
                rawPositionMs
            }
            val description = metadata?.description
            val title = metadata
                ?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?.trim()
                ?.ifBlank { null }
                ?: description
                    ?.title
                    ?.toString()
                    ?.trim()
                    ?.ifBlank { null }
            val artist = metadata
                ?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?.trim()
                ?.ifBlank { null }
                ?: metadata
                    ?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                    ?.trim()
                    ?.ifBlank { null }
                ?: description
                    ?.subtitle
                    ?.toString()
                    ?.trim()
                    ?.ifBlank { null }
            val albumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: description?.iconBitmap

            MediaPlaybackSnapshot(
                title = title,
                artist = artist,
                albumArt = albumArt,
                durationMs = durationMs,
                positionMs = positionMs,
                isPlaying = playbackState.state == PlaybackState.STATE_PLAYING
            )
        } catch (error: Throwable) {
            Log.w(TAG, "Failed to resolve media playback snapshot", error)
            null
        }
    }

    private fun resolveMediaController(
        context: Context,
        sessionToken: MediaSession.Token?,
        sourcePackageName: String
    ): MediaController? {
        if (sessionToken != null) {
            try {
                return MediaController(context, sessionToken)
            } catch (_: Throwable) {
            }
        }

        return try {
            val mediaSessionManager =
                context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                    ?: return null
            val componentName = ComponentName(
                context,
                LiveUpdateNotificationListenerService::class.java
            )
            val activeControllers = mediaSessionManager.getActiveSessions(componentName)
            activeControllers.firstOrNull { it.packageName == sourcePackageName }
                ?: activeControllers.firstOrNull()
        } catch (_: Throwable) {
            null
        }
    }

    private fun extractMediaSessionToken(notification: Notification): MediaSession.Token? {
        val extras = notification.extras
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(Notification.EXTRA_MEDIA_SESSION) as? MediaSession.Token
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun resolvePlaybackStatePositionMs(playbackState: PlaybackState): Long {
        val basePositionMs = playbackState.position.coerceAtLeast(0L)
        if (playbackState.state != PlaybackState.STATE_PLAYING) {
            return basePositionMs
        }

        val lastUpdateElapsedMs = playbackState.lastPositionUpdateTime
        if (lastUpdateElapsedMs <= 0L) {
            return basePositionMs
        }

        val elapsedSinceUpdateMs =
            (SystemClock.elapsedRealtime() - lastUpdateElapsedMs).coerceAtLeast(0L)
        val speed = playbackState.playbackSpeed.takeIf { it > 0f } ?: 1f
        return (basePositionMs + (elapsedSinceUpdateMs * speed)).toLong()
    }

    private fun MediaPlaybackSnapshot.toProgressOverride(): ProgressOverride? {
        if (durationMs <= 0L) {
            return null
        }
        val max = durationMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt().coerceAtLeast(1)
        val value = positionMs.coerceIn(0L, max.toLong()).toInt()
        return ProgressOverride(value = value, max = max)
    }

    private fun buildMediaPlaybackShortText(snapshot: MediaPlaybackSnapshot): String {
        if (!snapshot.title.isNullOrBlank()) {
            return snapshot.title
        }
        if (!snapshot.artist.isNullOrBlank()) {
            return snapshot.artist
        }
        if (snapshot.durationMs > 0L) {
            return formatMillisecondsAsClock(snapshot.positionMs)
        }
        return if (snapshot.isPlaying) "PLAY" else "PAUSE"
    }

    private fun String?.takeIfMeaningfulMediaPlaybackText(): String? {
        val normalized = this?.trim().orEmpty()
        if (normalized.isBlank() || mediaProgressOnlyPattern.matches(normalized)) {
            return null
        }
        return normalized
    }

    private fun formatMillisecondsAsClock(milliseconds: Long): String {
        val totalSeconds = (milliseconds / 1_000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
        }
    }

    private fun resolveAppName(context: Context, packageName: String): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun resolveStableWhen(source: Notification, fallbackPostTime: Long): Long {
        val sourceWhen = source.`when`
        return if (sourceWhen > 0L) {
            sourceWhen
        } else {
            fallbackPostTime
        }
    }

    private fun isLikelyNavigationPackage(
        packageName: String,
        parserDictionary: LiveParserDictionary
    ): Boolean {
        val packageLower = packageName.lowercase(Locale.ROOT)
        if (parserDictionary.knownNavigationPackages.contains(packageLower)) {
            return true
        }
        return parserDictionary.navigationPackageMarkers.any(packageLower::contains)
    }

    private fun isLikelyWeatherPackage(
        packageNameLower: String,
        parserDictionary: LiveParserDictionary
    ): Boolean {
        return parserDictionary.weatherPackageHints.any(packageNameLower::contains)
    }

    private fun isLikelySmartRulePackage(
        packageNameLower: String,
        ruleId: String,
        parserDictionary: LiveParserDictionary
    ): Boolean {
        val targetRule = parserDictionary.smartRules.firstOrNull { it.id == ruleId } ?: return false
        return targetRule.packageHints.any(packageNameLower::contains)
    }

    private fun isLikelyVpnPackage(
        packageNameLower: String,
        parserDictionary: LiveParserDictionary
    ): Boolean {
        if (isLikelySmartRulePackage(packageNameLower, "vpn", parserDictionary)) {
            return true
        }
        return parserDictionary.vpnPackageMarkers.any(packageNameLower::contains)
    }

    private fun shouldSuppressVpnWithoutTraffic(
        packageName: String,
        source: Notification,
        parserDictionary: LiveParserDictionary
    ): Boolean {
        val packageLower = packageName.lowercase(Locale.ROOT)
        val combinedText = collectVpnDetectionText(
            notification = source,
            fallbackTitle = packageName,
            includeRemoteViewTexts = true
        )
        if (combinedText.isBlank()) {
            return false
        }
        if (hasVpnSpeedPattern(combinedText, parserDictionary)) {
            return false
        }
        val likelyVpnPackage = isLikelyVpnPackage(packageLower, parserDictionary)
        val hasVpnContext = parserDictionary.vpnContextPattern.containsMatchIn(combinedText)
        return likelyVpnPackage || hasVpnContext
    }

    private fun collectVpnDetectionText(
        notification: Notification,
        fallbackTitle: String,
        includeRemoteViewTexts: Boolean
    ): String {
        val base = collectNotificationText(
            notification = notification,
            fallbackTitle = fallbackTitle,
            includeRemoteViewTexts = includeRemoteViewTexts
        )
        val parts = mutableListOf<String>()
        if (base.isNotBlank()) {
            parts += base
        }
        NotificationTextNormalizer.normalize(notification.tickerText)?.let(parts::add)
        notification.channelId?.trim()?.takeIf { it.isNotBlank() }?.let(parts::add)
        notification.group?.trim()?.takeIf { it.isNotBlank() }?.let(parts::add)
        notification.sortKey?.trim()?.takeIf { it.isNotBlank() }?.let(parts::add)
        notification.category?.trim()?.takeIf { it.isNotBlank() }?.let(parts::add)
        notification.actions
            ?.mapNotNull { NotificationTextNormalizer.normalize(it.title) }
            ?.forEach(parts::add)

        if (parts.isEmpty()) {
            return ""
        }
        return parts
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun hasVpnSpeedPattern(
        text: String,
        parserDictionary: LiveParserDictionary
    ): Boolean {
        if (text.isBlank()) {
            return false
        }
        return parserDictionary.vpnSpeedPattern.containsMatchIn(text)
    }

    private fun mirrorIdForKey(key: String): Int {
        val value = key.hashCode()
        return if (value == Int.MIN_VALUE) 0 else abs(value)
    }

    private fun notifyMirroredNotification(
        manager: NotificationManagerCompat,
        notificationId: Int,
        notification: Notification,
        mirrorKey: String,
        sourceSbn: StatusBarNotification
    ) {
        manager.notify(
            notificationId,
            SamsungOneUi7NowBarCompat.markEligible(notification)
        )
        synchronized(stateLock) {
            pruneProgrammaticMirrorCancelsLocked(SystemClock.elapsedRealtime())
            mirrorKeysByNotificationId[notificationId] = mirrorKey
            sourceSnapshotsByMirrorKey[mirrorKey] = sourceSbn
        }
    }

    private fun cancelMirroredNotification(
        manager: NotificationManagerCompat,
        notificationId: Int
    ) {
        synchronized(stateLock) {
            programmaticMirrorCancelDeadlines[notificationId] =
                SystemClock.elapsedRealtime() + PROGRAMMATIC_MIRROR_CANCEL_GRACE_MS
            val mirrorKey = mirrorKeysByNotificationId.remove(notificationId)
            if (mirrorKey != null) {
                sourceSnapshotsByMirrorKey.remove(mirrorKey)
                callMirrorStates.remove(mirrorKey)
                smartAnimationGenerations.remove(mirrorKey)
                smartAnimationStates.remove(mirrorKey)
            }
        }
        manager.cancel(notificationId)
    }

    private fun consumeProgrammaticMirrorCancelLocked(
        notificationId: Int,
        now: Long
    ): Boolean {
        val deadline = programmaticMirrorCancelDeadlines[notificationId] ?: return false
        programmaticMirrorCancelDeadlines.remove(notificationId)
        return deadline >= now
    }

    private fun pruneProgrammaticMirrorCancelsLocked(now: Long) {
        val expiredIds = programmaticMirrorCancelDeadlines
            .filterValues { it < now }
            .keys
            .toList()
        expiredIds.forEach(programmaticMirrorCancelDeadlines::remove)
    }

    private fun isUserDismissedMirrorLocked(mirrorKey: String): Boolean {
        return userDismissedMirrorKeys.contains(mirrorKey)
    }

    private fun isUserDismissedMirror(mirrorKey: String): Boolean {
        return synchronized(stateLock) {
            isUserDismissedMirrorLocked(mirrorKey)
        }
    }

    private fun limitIslandText(value: String?, enabled: Boolean, maxLength: Int): String {
        val normalized = value.orEmpty()
        if (!enabled) {
            return normalized
        }
        return safeTakeByGraphemes(normalized, maxLength)
    }

    private fun safeTakeByGraphemes(value: String, maxGraphemes: Int): String {
        if (maxGraphemes <= 0 || value.isEmpty()) {
            return ""
        }

        val iterator = BreakIterator.getCharacterInstance(Locale.ROOT)
        iterator.setText(value)

        var endIndex = 0
        var consumed = 0
        while (consumed < maxGraphemes) {
            val nextBoundary = iterator.next()
            if (nextBoundary == BreakIterator.DONE) {
                break
            }
            endIndex = nextBoundary
            consumed += 1
        }
        return if (endIndex > 0) value.substring(0, endIndex) else ""
    }

    private fun clearAggregateTrackingForSbnKeyLocked(sbnKey: String): List<Int> {
        val idsToCancel = mutableListOf<Int>()
        idsToCancel.addAll(clearSmartTrackingForSbnKeyLocked(sbnKey))
        idsToCancel.addAll(clearOtpTrackingForSbnKeyLocked(sbnKey))
        return idsToCancel
    }

    private fun clearSmartTrackingForSbnKeyLocked(sbnKey: String): List<Int> {
        val idsToCancel = mutableListOf<Int>()
        val smartAggregateKey = sbnToAggregateKey.remove(sbnKey)
        if (smartAggregateKey != null) {
            val state = aggregateStates[smartAggregateKey]
            if (state != null) {
                state.activeSbnKeys.remove(sbnKey)
                state.sourcesBySbnKey.remove(sbnKey)
                if (state.activeSbnKeys.isEmpty()) {
                    aggregateStates.remove(smartAggregateKey)
                    smartAnimationGenerations.remove(smartAggregateKey)
                    smartAnimationStates.remove(smartAggregateKey)
                    userDismissedMirrorKeys.remove(smartAggregateKey)
                    sourceSnapshotsByMirrorKey.remove(smartAggregateKey)
                    mirrorKeysByNotificationId.remove(mirrorIdForKey(smartAggregateKey))
                    idsToCancel.add(mirrorIdForKey(smartAggregateKey))
                }
            } else {
                smartAnimationGenerations.remove(smartAggregateKey)
                smartAnimationStates.remove(smartAggregateKey)
                userDismissedMirrorKeys.remove(smartAggregateKey)
                sourceSnapshotsByMirrorKey.remove(smartAggregateKey)
                mirrorKeysByNotificationId.remove(mirrorIdForKey(smartAggregateKey))
                idsToCancel.add(mirrorIdForKey(smartAggregateKey))
            }
        }
        return idsToCancel
    }

    private fun selectSmartSourceEntryLocked(
        aggregateState: AggregateState,
        keepHighestStage: Boolean
    ): SmartSourceEntry? {
        if (aggregateState.sourcesBySbnKey.isEmpty()) {
            return null
        }
        val activeEntries = aggregateState.activeSbnKeys
            .mapNotNull(aggregateState.sourcesBySbnKey::get)
        if (activeEntries.isEmpty()) {
            aggregateState.sourcesBySbnKey.clear()
            return null
        }
        return if (keepHighestStage) {
            activeEntries.maxWithOrNull(
                compareBy<SmartSourceEntry>(
                    { it.stageValue },
                    { it.postTimeMs },
                    { it.sbn.key }
                )
            )
        } else {
            activeEntries.maxWithOrNull(
                compareBy<SmartSourceEntry>(
                    { it.postTimeMs },
                    { it.stageValue },
                    { it.sbn.key }
                )
            )
        }
    }

    private fun clearOtpTrackingForSbnKeyLocked(sbnKey: String): List<Int> {
        val idsToCancel = mutableListOf<Int>()
        val sourceKey = sbnToOtpSourceKey.remove(sbnKey)
        val otpAggregateKey = sbnToOtpAggregateKey.remove(sbnKey)
        if (otpAggregateKey != null) {
            val state = otpAggregateStates[otpAggregateKey]
            if (state != null) {
                state.activeSbnKeys.remove(sbnKey)
                if (state.activeSbnKeys.isEmpty()) {
                    otpAggregateStates.remove(otpAggregateKey)
                    otpAnimationGenerations.remove(otpAggregateKey)
                    userDismissedMirrorKeys.remove(otpAggregateKey)
                    sourceSnapshotsByMirrorKey.remove(otpAggregateKey)
                    mirrorKeysByNotificationId.remove(mirrorIdForKey(otpAggregateKey))
                    idsToCancel.add(mirrorIdForKey(otpAggregateKey))
                }
            } else {
                otpAnimationGenerations.remove(otpAggregateKey)
                userDismissedMirrorKeys.remove(otpAggregateKey)
                sourceSnapshotsByMirrorKey.remove(otpAggregateKey)
                mirrorKeysByNotificationId.remove(mirrorIdForKey(otpAggregateKey))
                idsToCancel.add(mirrorIdForKey(otpAggregateKey))
            }
        }
        if (sourceKey != null) {
            val sourceState = otpSourceStates[sourceKey]
            if (sourceState != null && sourceState.sbnKey == sbnKey) {
                otpSourceStates.remove(sourceKey)
            }
        }
        return idsToCancel
    }

    private fun clearOtpTrackingForSourceLocked(sourceKey: String, exceptSbnKey: String): List<Int> {
        val idsToCancel = mutableListOf<Int>()
        val sbnKeysToClear = sbnToOtpSourceKey.entries
            .filter { it.value == sourceKey && it.key != exceptSbnKey }
            .map { it.key }

        for (sbnKey in sbnKeysToClear) {
            idsToCancel.addAll(clearOtpTrackingForSbnKeyLocked(sbnKey))
        }
        return idsToCancel
    }

    private data class ProgressOverride(
        val value: Int,
        val max: Int
    )

    private data class AggregateState(
        var maxStageSeen: Int,
        val maxStage: Int,
        val activeSbnKeys: MutableSet<String> = mutableSetOf(),
        val sourcesBySbnKey: MutableMap<String, SmartSourceEntry> = mutableMapOf()
    )

    private data class SmartSourceEntry(
        val stageValue: Int,
        val postTimeMs: Long,
        val sbn: StatusBarNotification,
        val compactOrderCode: String?
    )

    private data class OtpAggregateState(
        val activeSbnKeys: MutableSet<String> = mutableSetOf(),
        var lastRenderedAtMs: Long = 0L,
        var lastAutoCopiedCode: String = ""
    )

    private data class OtpSourceState(
        val sbnKey: String,
        val aggregateKey: String,
        val postTimeMs: Long
    )

    private data class OtpRouteState(
        val staleAggregateIds: List<Int>,
        val shouldPublish: Boolean,
        val shouldAutoCopy: Boolean,
        val otpCode: String
    )

    private data class SmartRouteState(
        val staleAggregateIds: List<Int>,
        val stageValue: Int,
        val stageMax: Int,
        val compactOrderCode: String?,
        val sourceSbn: StatusBarNotification
    )

    private data class RemoteDrawableAssets(
        val icon: IconCompat?,
        val bitmap: Bitmap?
    )

    data class MirrorResult(
        val mirrored: Boolean,
        val dedupKind: MirrorDedupKind = MirrorDedupKind.NONE,
        val notificationId: Int? = null,
        val mirrorKey: String? = null,
        val removeSource: Boolean = false
    )

    enum class MirrorDedupKind {
        NONE,
        OTP,
        STATUS
    }

    private enum class AppLocale {
        EN,
        RU,
        TR,
        PT_BR,
        ZH_HANS,
        ZH_HANT,
        KO
    }

    private enum class MirrorNotificationChannel(val id: String) {
        LEGACY("livebridge_promoted_updates"),
        PROGRESS_NOTIFICATIONS("livebridge_progress_notifications"),
        OTP_CODES("livebridge_otp_codes"),
        SMART_CONVERSIONS("livebridge_smart_conversions"),
        MEDIA_PLAYBACK("livebridge_media_playback"),
        CALLS("livebridge_calls"),
        NETWORK_CONNECTIONS("livebridge_network_connections"),
        MISCELLANEOUS("livebridge_miscellaneous_conversions"),
        NOTIFICATION_CAPSULE("livebridge_notification_capsule"),
        CHARGING_INFO("livebridge_charging_info"),
        BYPASS("livebridge_bypass_applications")
    }

    private data class MirrorChannelText(
        val name: String,
        val description: String
    )

    private data class SmartStageMatch(
        val aggregateKey: String,
        val stageValue: Int,
        val maxStage: Int,
        val compactOrderCode: String?,
        val keepHighestStage: Boolean
    )

    private data class VpnTrafficSpeeds(
        val outgoingSpeed: String?,
        val incomingSpeed: String?
    )

    private data class SmartAnimationState(
        var sbn: StatusBarNotification,
        var appPresentationOverride: AppPresentationOverride,
        var mirrorChannel: MirrorNotificationChannel,
        var progressOverride: ProgressOverride?,
        var smartRuleId: String,
        var tokens: List<String?>,
        var nextIndex: Int,
        var lastShownToken: String?,
        var compactCodeOverride: String?,
        var samsungBridge: SamsungBridgeContext
    )

    private data class SmartAnimationFrame(
        val sbn: StatusBarNotification,
        val appPresentationOverride: AppPresentationOverride,
        val mirrorChannel: MirrorNotificationChannel,
        val progressOverride: ProgressOverride?,
        val smartRuleId: String,
        val token: String,
        val compactCodeOverride: String?,
        val samsungBridge: SamsungBridgeContext
    )

    private data class SmartAnimationToken(
        val token: String,
        val nextIndex: Int
    )

    private data class TextProgressMatch(
        val percent: Int,
        val shortText: String
    )

    private data class CallMirrorSnapshot(
        val explicitStartWallClockMs: Long?,
        val elapsedDurationMs: Long?
    )

    private data class CallTimeSeed(
        val explicitStartWallClockMs: Long?,
        val elapsedDurationMs: Long?
    ) {
        val hasExplicitSource: Boolean
            get() = explicitStartWallClockMs != null || elapsedDurationMs != null
    }

    private data class CallMirrorState(
        var sbn: StatusBarNotification,
        var appPresentationOverride: AppPresentationOverride,
        var samsungBridge: SamsungBridgeContext,
        var startedAtWallClockMs: Long,
        val generation: Long
    )

    private data class CallMirrorFrame(
        val sbn: StatusBarNotification,
        val appPresentationOverride: AppPresentationOverride,
        val samsungBridge: SamsungBridgeContext,
        val startedAtWallClockMs: Long
    )

    private data class DeliveryEtaText(
        val text: String,
        val totalMinutes: Int?
    )

    private data class MediaPlaybackSnapshot(
        val title: String?,
        val artist: String?,
        val albumArt: Bitmap?,
        val durationMs: Long,
        val positionMs: Long,
        val isPlaying: Boolean
    )

    private data class MediaPreferredAction(
        val action: Notification.Action,
        val shortTitle: String
    )

    private data class OtpMatch(
        val code: String,
        val aggregateKey: String
    )
}
