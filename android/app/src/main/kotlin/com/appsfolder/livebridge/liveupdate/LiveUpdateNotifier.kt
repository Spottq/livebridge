package com.kakao.taxi.liveupdate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import android.util.Log
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
    private const val SAMSUNG_TRAY_ICON_SIZE = 48

    private const val CHANNEL_NAME = "LiveBridge Updates"
    private const val TAG = "LiveUpdateNotifier"
    private const val MAX_MIRRORED_ACTIONS = 3
    private const val OTP_REPEAT_SUPPRESS_MS = 60_000L
    private const val OTP_AUTOCOPY_COPIED_SHOW_DELAY_MS = 1_000L
    private const val OTP_AUTOCOPY_COPIED_SHOW_DURATION_MS = 1_500L
    private const val AOSP_ISLAND_TEXT_LIMIT = 7
    private const val CALL_DURATION_REFRESH_MS = 1_000L
    private val KNOWN_NAVIGATION_PACKAGES = setOf(
        YANDEX_MAPS_PACKAGE,
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
    private val NAVIGATION_DISTANCE_PATTERN = Regex(
        "(?<!\\d)\\d{1,4}(?:[\\s.,]\\d{1,2})?\\s*(?:км|km|м|m|mi|ft|миль|фут)\\b",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val TEXT_PROGRESS_PERCENT_PATTERN = Regex("(?<!\\d)(\\d{1,3})\\s*%")
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
        """(^|\s)(end|end\s*call|hang\s*up|hangup|disconnect|заверш|отбой|сбросить|挂断|掛斷|结束|結束|encerrar|terminar)(\s|$)""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val callActiveTextPattern = Regex(
        """((?:ongoing|active).{0,40}\bcall\b|call\s+in\s+progress|on\s+call|in\s+call|разговор|ид[её]т\s+звонок|текущий\s+звонок|通话中|通話中)""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val callContextTextPattern = Regex(
        """(\bcall\b|звонок|вызов|разговор|通话|通話)""",
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
    private var callMirrorGenerationCounter = 0L

    private data class AppIconAssets(
        val smallIcon: IconCompat?,
        val largeIconBitmap: Bitmap?
    )

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
        val current = manager.getNotificationChannel(channel.id)
        if (current == null) {
            manager.createNotificationChannel(createChannel(context, channel))
            return
        }

        val channelText = mirrorChannelText(context, channel)
        val shouldUpdate =
            current.name?.toString() != channelText.name ||
                    current.description != channelText.description ||
                    current.lockscreenVisibility != Notification.VISIBILITY_PUBLIC
        if (!shouldUpdate) {
            return
        }

        current.name = channelText.name
        current.description = channelText.description
        current.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
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
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
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
        val stateMirrorKeys = synchronized(stateLock) {
            callMirrorStates.keys.toList()
        }
        val stateNotificationIds = stateMirrorKeys.map(::mirrorIdForKey)
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

            if (!hasNativeProgress &&
                !isMediaPlaybackNotification &&
                otpMatch == null &&
                smartMatch == null &&
                textProgressMatch == null &&
                prefs.getOnlyWithProgress()
            ) {
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
                    val dedupKind = if (isNotificationDedupEligibleSmartRule(smartRuleId)) {
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
                        preferSmartShortTextAsPrimary = animatedIslandEnabled
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
                        preferSmartShortTextAsPrimary = animatedIslandEnabled
                    )
                    if (animatedIslandEnabled) {
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

                else -> {
                    val staleAggregateIds = synchronized(stateLock) {
                        clearAggregateTrackingForSbnKeyLocked(sbn.key)
                    }
                    staleAggregateIds.forEach { cancelMirroredNotification(manager, it) }

                    val mirrorChannel = if (hasNativeProgress) {
                        MirrorNotificationChannel.PROGRESS_NOTIFICATIONS
                    } else {
                        MirrorNotificationChannel.BYPASS
                    }
                    val notification = buildMirroredNotification(
                        context = context,
                        sbn = sbn,
                        appPresentationOverride = appPresentationOverride,
                        mirrorChannel = mirrorChannel,
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
                        mirrorChannel = mirrorChannel,
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
        val hasEndCallAction = actionTexts.any(callEndActionPattern::containsMatchIn)
        val hasActiveCallText = contentTexts.any(callActiveTextPattern::containsMatchIn)
        val hasCallContext =
            source.category == Notification.CATEGORY_CALL ||
                    contentTexts.any(callContextTextPattern::containsMatchIn)
        if (!hasCallContext) {
            return null
        }
        if (source.category != Notification.CATEGORY_CALL && !hasEndCallAction) {
            return null
        }
        if (!timeSeed.hasExplicitSource && !hasEndCallAction && !hasActiveCallText) {
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
        samsungReparse: SamsungReparsePayload?
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
        val isSamsungTwoGis =
            samsungBridge.enabled &&
                    samsungBridge.hasCustomRemoteCard &&
                    isTwoGisPackage
        val shouldTryNavigationArrowIcon =
            (appPresentationOverride.iconSource == NotificationIconSource.NOTIFICATION ||
                    isTwoGisPackage) &&
                    (smartRuleId == "navigation" ||
                            isTwoGisPackage ||
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
            shouldTryNavigationArrowIcon ->
                navigationDrawable?.bitmap ?: samsungLargeIcon ?: sourceLargeIcon
            else ->
                samsungLargeIcon ?: sourceLargeIcon
        }
        val preferredPrimaryIcon = when (appPresentationOverride.iconSource) {
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
            isYandexMapsPackage ->
                appSmallIcon ?: preferredPrimaryIcon
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
        val baseTitle = titleOverride?.takeIf { it.isNotBlank() }
            ?: samsungReparse?.title?.takeIf { it.isNotBlank() }
            ?: extractTitle(source, appName, allowRemoteViewTextFallback)
        val baseText = textOverride?.takeIf { it.isNotBlank() }
            ?: samsungReparse?.text?.takeIf { it.isNotBlank() }
            ?: extractText(source, allowRemoteViewTextFallback)
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
        val visibility = if (
            preferMediaControls &&
            !runtimePrefs.getSmartMediaPlaybackShowOnLockScreen()
        ) {
            NotificationCompat.VISIBILITY_SECRET
        } else {
            NotificationCompat.VISIBILITY_PUBLIC
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
        val weatherLockscreenOnly =
            smartRuleId == "weather" && runtimePrefs.getSmartWeatherLockscreenOnly()
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
            .setColor(progressColor)
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
        if (smartShortTextOverride != null && !hasProgress) {
            if (!preferSmartShortTextAsPrimary) {
                builder.setContentText(smartShortTextOverride)
            }
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
            val samsungTexts = SamsungBridgeContentPolicy.resolve(
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
            SamsungNowBarApplier.apply(
                context = context,
                builder = builder,
                source = source,
                sourcePackageName = sbn.packageName,
                primaryText = samsungTexts.nowBarPrimaryText,
                texts = samsungTexts,
                chipIcon = preferredChipIcon,
                nowBarIcon = preferredPrimaryIcon,
                rightIcon = nowBarRightIcon,
                suppressChipExpandedText = callChronometerStart != null,
                suppressSourceRemoteViews = false,
                suppressSourceNowBarRemoteView = suppressCallNowBarRemoteView,
                lockscreenOnly = weatherLockscreenOnly,
                hasProgress = hasProgress,
                progressValue = progressValue,
                progressMax = progressMax
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

        return builder.build()
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

    private fun isRussianLocale(context: Context): Boolean {
        val locale = currentLocale(context)
        val language = locale?.language?.lowercase(Locale.ROOT).orEmpty()
        return language.startsWith("ru")
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
        manager.notify(notificationId, notification)
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

    private enum class MirrorNotificationChannel(val id: String) {
        LEGACY("livebridge_promoted_updates"),
        PROGRESS_NOTIFICATIONS("livebridge_progress_notifications"),
        OTP_CODES("livebridge_otp_codes"),
        SMART_CONVERSIONS("livebridge_smart_conversions"),
        MEDIA_PLAYBACK("livebridge_media_playback"),
        CALLS("livebridge_calls"),
        NETWORK_CONNECTIONS("livebridge_network_connections"),
        MISCELLANEOUS("livebridge_miscellaneous_conversions"),
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
