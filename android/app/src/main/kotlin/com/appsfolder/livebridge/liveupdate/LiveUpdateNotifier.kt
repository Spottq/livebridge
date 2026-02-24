package ai.perplexity.app.android.liveupdate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import ai.perplexity.app.android.R
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object LiveUpdateNotifier {
    const val CHANNEL_ID = "livebridge_promoted_updates"

    private const val CHANNEL_NAME = "LiveBridge Updates"
    private const val TAG = "LiveUpdateNotifier"
    private const val MAX_MIRRORED_ACTIONS = 3
    private const val OTP_REPEAT_SUPPRESS_MS = 60_000L
    private const val OTP_AUTOCOPY_COPIED_SHOW_DELAY_MS = 1_000L
    private const val OTP_AUTOCOPY_COPIED_SHOW_DURATION_MS = 1_500L
    private const val AOSP_ISLAND_TEXT_LIMIT = 7
    private const val POST_PROMOTED_NOTIFICATIONS_PERMISSION =
        "android.permission.POST_PROMOTED_NOTIFICATIONS"
    private val BLOCKED_SOURCE_PACKAGES = setOf(
        "ru.dublgis.dgismobile"
    )
    private val KNOWN_NAVIGATION_PACKAGES = setOf(
        "ru.yandex.yandexmaps",
        "com.google.android.apps.maps",
        "com.waze"
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

    private val OTP_CODE_LENGTH = 4..8
    private val progressColor = Color.valueOf(15f / 255f, 118f / 255f, 110f / 255f, 1f).toArgb()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val stateLock = Any()
    private val sbnToAggregateKey = mutableMapOf<String, String>()
    private val aggregateStates = mutableMapOf<String, AggregateState>()
    private val sbnToOtpAggregateKey = mutableMapOf<String, String>()
    private val sbnToOtpSourceKey = mutableMapOf<String, String>()
    private val otpSourceStates = mutableMapOf<String, OtpSourceState>()
    private val otpAggregateStates = mutableMapOf<String, OtpAggregateState>()
    private val otpAnimationGenerations = mutableMapOf<String, Long>()

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val current = manager.getNotificationChannel(CHANNEL_ID)

        if (current == null) {
            manager.createNotificationChannel(createChannel())
            return
        }

        if (current.lockscreenVisibility != Notification.VISIBILITY_PUBLIC) {
            current.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            manager.createNotificationChannel(current)
        }
    }

    private fun createChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Converted promoted ongoing notifications"
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
        }
    }

    fun maybeMirror(context: Context, prefs: ConverterPrefs, sbn: StatusBarNotification): Boolean {
        ensureChannel(context)

        val manager = NotificationManagerCompat.from(context)
        if (!prefs.getConverterEnabled()) {
            val staleAggregateIds = synchronized(stateLock) {
                clearAggregateTrackingForSbnKeyLocked(sbn.key)
            }
            staleAggregateIds.forEach(manager::cancel)
            manager.cancel(mirrorIdForKey(sbn.key))
            return false
        }

        if (!passesBaseFilters(context.packageName, prefs, sbn)) {
            val staleAggregateIds = synchronized(stateLock) {
                clearAggregateTrackingForSbnKeyLocked(sbn.key)
            }
            staleAggregateIds.forEach(manager::cancel)
            manager.cancel(mirrorIdForKey(sbn.key))
            return false
        }

        return try {
            val parserDictionary = LiveParserDictionaryLoader.get(context, prefs)
            val appPresentationOverride = AppPresentationOverridesLoader
                .get(prefs)
                .resolve(sbn.packageName.lowercase(Locale.ROOT))
            val source = sbn.notification
            val samsungNowBarBridgeEnabled =
                prefs.getSamsungRemoteReparserEnabled() && SamsungLiveUpdateReparser.isSamsungDevice()
            val samsungReparse = if (samsungNowBarBridgeEnabled) {
                SamsungNotificationReparser.parse(context, sbn)
            } else {
                null
            }
            val hasNativeProgress = hasProgress(source) || (samsungReparse?.hasProgress == true)

            val otpMatch = if (!hasNativeProgress &&
                prefs.getOtpDetectionEnabled() &&
                prefs.isOtpPackageAllowed(sbn.packageName)
            ) {
                detectOtpCode(sbn.packageName, source, parserDictionary)
            } else {
                null
            }

            val smartMatch = if (!hasNativeProgress && otpMatch == null && prefs.getSmartStatusDetectionEnabled()) {
                detectSmartStage(
                    packageName = sbn.packageName,
                    source = source,
                    parserDictionary = parserDictionary,
                    navigationEnabled = prefs.getSmartNavigationEnabled()
                )
            } else {
                null
            }

            val textProgressMatch = if (!hasNativeProgress &&
                otpMatch == null &&
                prefs.getTextProgressEnabled()
            ) {
                detectTextProgress(
                    packageName = sbn.packageName,
                    source = source
                )
            } else {
                null
            }

            if (!hasNativeProgress &&
                otpMatch == null &&
                smartMatch == null &&
                textProgressMatch == null &&
                prefs.getOnlyWithProgress()
            ) {
                val staleAggregateIds = synchronized(stateLock) {
                    clearAggregateTrackingForSbnKeyLocked(sbn.key)
                }
                staleAggregateIds.forEach(manager::cancel)
                manager.cancel(mirrorIdForKey(sbn.key))
                return false
            }

            when {
                otpMatch != null -> {
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
                                        shouldAutoCopyOtpLocked(state, otpMatch.code, now)
                            OtpRouteState(
                                staleAggregateIds = staleAggregateIds,
                                shouldPublish = shouldPublish,
                                shouldAutoCopy = shouldAutoCopy,
                                otpCode = otpMatch.code
                            )
                        }
                    }
                    routeState.staleAggregateIds.forEach(manager::cancel)

                    if (routeState.shouldPublish) {
                        val notification = buildMirroredNotification(
                            context = context,
                            sbn = sbn,
                            appPresentationOverride = appPresentationOverride,
                            progressOverride = null,
                            otpOverride = otpMatch,
                            smartShortTextOverride = null,
                            requestPromoted = true,
                            samsungReparse = samsungReparse,
                            samsungNowBarBridgeEnabled = samsungNowBarBridgeEnabled
                        )
                        notifyWithPromotionFallback(
                            context = context,
                            manager = manager,
                            notificationId = mirrorIdForKey(otpMatch.aggregateKey),
                            promotedNotification = notification,
                            sbn = sbn,
                            appPresentationOverride = appPresentationOverride,
                            progressOverride = null,
                            otpOverride = otpMatch,
                            smartShortTextOverride = null,
                            samsungReparse = samsungReparse,
                            samsungNowBarBridgeEnabled = samsungNowBarBridgeEnabled
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
                                samsungReparse = samsungReparse,
                                samsungNowBarBridgeEnabled = samsungNowBarBridgeEnabled
                            )
                        }
                    }
                    true
                }

                textProgressMatch != null -> {
                    val staleAggregateIds = synchronized(stateLock) {
                        clearAggregateTrackingForSbnKeyLocked(sbn.key)
                    }
                    staleAggregateIds.forEach(manager::cancel)

                    val notification = buildMirroredNotification(
                        context = context,
                        sbn = sbn,
                        appPresentationOverride = appPresentationOverride,
                        progressOverride = ProgressOverride(
                            value = textProgressMatch.percent,
                            max = 100
                        ),
                        otpOverride = null,
                        smartShortTextOverride = textProgressMatch.shortText,
                        requestPromoted = true,
                        samsungReparse = samsungReparse,
                        samsungNowBarBridgeEnabled = samsungNowBarBridgeEnabled
                    )
                    notifyWithPromotionFallback(
                        context = context,
                        manager = manager,
                        notificationId = mirrorIdForKey(sbn.key),
                        promotedNotification = notification,
                        sbn = sbn,
                        appPresentationOverride = appPresentationOverride,
                        progressOverride = ProgressOverride(
                            value = textProgressMatch.percent,
                            max = 100
                        ),
                        otpOverride = null,
                        smartShortTextOverride = textProgressMatch.shortText,
                        samsungReparse = samsungReparse,
                        samsungNowBarBridgeEnabled = samsungNowBarBridgeEnabled
                    )
                    true
                }

                smartMatch != null -> {
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
                        state.maxStageSeen = if (smartMatch.keepHighestStage) {
                            maxOf(state.maxStageSeen, smartMatch.stageValue)
                        } else {
                            smartMatch.stageValue
                        }
                        sbnToAggregateKey[sbn.key] = smartMatch.aggregateKey

                        SmartRouteState(
                            staleAggregateIds = staleAggregateIds,
                            stageValue = state.maxStageSeen,
                            stageMax = state.maxStage,
                            compactOrderCode = smartMatch.compactOrderCode
                        )
                    }
                    routeState.staleAggregateIds.forEach(manager::cancel)
                    val smartRuleId = smartRuleIdFromAggregateKey(smartMatch.aggregateKey)
                    val smartStatusText =
                        if (smartRuleId == "navigation") {
                            extractNavigationDistanceText(
                                notification = source,
                                fallbackTitle = sbn.packageName
                            ) ?: smartShortStatusText(
                                context = context,
                                ruleId = smartRuleId,
                                stageValue = routeState.stageValue,
                                parserDictionary = parserDictionary
                            )
                        } else {
                            smartShortStatusText(
                                context = context,
                                ruleId = smartRuleId,
                                stageValue = routeState.stageValue,
                                parserDictionary = parserDictionary
                            )
                        } ?: routeState.compactOrderCode

                    val notification = buildMirroredNotification(
                        context = context,
                        sbn = sbn,
                        appPresentationOverride = appPresentationOverride,
                        progressOverride = ProgressOverride(routeState.stageValue, routeState.stageMax),
                        otpOverride = null,
                        smartShortTextOverride = smartStatusText,
                        smartRuleId = smartRuleId,
                        requestPromoted = true,
                        samsungReparse = samsungReparse,
                        samsungNowBarBridgeEnabled = samsungNowBarBridgeEnabled
                    )
                    notifyWithPromotionFallback(
                        context = context,
                        manager = manager,
                        notificationId = mirrorIdForKey(smartMatch.aggregateKey),
                        promotedNotification = notification,
                        sbn = sbn,
                        appPresentationOverride = appPresentationOverride,
                        progressOverride = ProgressOverride(routeState.stageValue, routeState.stageMax),
                        otpOverride = null,
                        smartShortTextOverride = smartStatusText,
                        smartRuleId = smartRuleId,
                        samsungReparse = samsungReparse,
                        samsungNowBarBridgeEnabled = samsungNowBarBridgeEnabled
                    )
                    true
                }

                else -> {
                    val staleAggregateIds = synchronized(stateLock) {
                        clearAggregateTrackingForSbnKeyLocked(sbn.key)
                    }
                    staleAggregateIds.forEach(manager::cancel)

                    val notification = buildMirroredNotification(
                        context = context,
                        sbn = sbn,
                        appPresentationOverride = appPresentationOverride,
                        progressOverride = null,
                        otpOverride = null,
                        smartShortTextOverride = null,
                        requestPromoted = true,
                        samsungReparse = samsungReparse,
                        samsungNowBarBridgeEnabled = samsungNowBarBridgeEnabled
                    )
                    notifyWithPromotionFallback(
                        context = context,
                        manager = manager,
                        notificationId = mirrorIdForKey(sbn.key),
                        promotedNotification = notification,
                        sbn = sbn,
                        appPresentationOverride = appPresentationOverride,
                        progressOverride = null,
                        otpOverride = null,
                        smartShortTextOverride = null,
                        samsungReparse = samsungReparse,
                        samsungNowBarBridgeEnabled = samsungNowBarBridgeEnabled
                    )
                    true
                }
            }
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to mirror notification: ${sbn.key}", error)
            false
        }
    }

    fun cancelMirrored(context: Context, sbn: StatusBarNotification) {
        try {
            val manager = NotificationManagerCompat.from(context)
            val staleAggregateIds = synchronized(stateLock) {
                clearAggregateTrackingForSbnKeyLocked(sbn.key)
            }
            staleAggregateIds.forEach(manager::cancel)
            manager.cancel(mirrorIdForKey(sbn.key))
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to cancel mirrored notification: ${sbn.key}", error)
        }
    }

    private fun passesBaseFilters(
        appPackageName: String,
        prefs: ConverterPrefs,
        sbn: StatusBarNotification
    ): Boolean {
        if (sbn.packageName == appPackageName) {
            return false
        }

        val source = sbn.notification

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && source.channelId == CHANNEL_ID) {
            return false
        }

        if (Build.VERSION.SDK_INT >= 36 && source.flags and 0x40000 != 0) {
            return false
        }

        if (source.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            return false
        }

        if (BLOCKED_SOURCE_PACKAGES.contains(sbn.packageName.lowercase(Locale.ROOT))) {
            return false
        }

        return prefs.isPackageAllowed(sbn.packageName)
    }

    private fun buildMirroredNotification(
        context: Context,
        sbn: StatusBarNotification,
        appPresentationOverride: AppPresentationOverride,
        progressOverride: ProgressOverride?,
        otpOverride: OtpMatch?,
        smartShortTextOverride: String?,
        smartRuleId: String? = null,
        requestPromoted: Boolean,
        otpShortTextOverride: String? = null,
        samsungReparse: SamsungReparsePayload? = null,
        samsungNowBarBridgeEnabled: Boolean = false
    ): Notification {
        val source = sbn.notification
        val sourceSmallIcon = resolveSourceSmallIcon(context, sbn)
        val appSmallIcon = resolveAppSmallIcon(context, sbn.packageName)
        val samsungSmallIcon = samsungReparse?.icon
        val samsungLargeIcon = samsungReparse?.largeIconBitmap
        val shouldTryNavigationArrowIcon =
            appPresentationOverride.iconSource == NotificationIconSource.NOTIFICATION &&
                    (smartRuleId == "navigation" || isLikelyNavigationPackage(sbn.packageName))
        val navigationDrawable =
            if (shouldTryNavigationArrowIcon) {
                resolveRemoteDrawableAssets(context, sbn)
            } else {
                null
            }
        val sourceLargeIcon = resolveSourceLargeIconBitmap(context, source)
        val preferredLargeIcon = when {
            appPresentationOverride.iconSource == NotificationIconSource.APP -> sourceLargeIcon
            shouldTryNavigationArrowIcon -> navigationDrawable?.bitmap ?: samsungLargeIcon ?: sourceLargeIcon
            else -> samsungLargeIcon ?: sourceLargeIcon
        }

        val appName = resolveAppName(context, sbn.packageName)
        val allowRemoteViewTextFallback = shouldTryNavigationArrowIcon
        val title = samsungReparse?.title?.takeIf { it.isNotBlank() }
            ?: extractTitle(source, appName, allowRemoteViewTextFallback)
        val text = samsungReparse?.text?.takeIf { it.isNotBlank() }
            ?: extractText(source, allowRemoteViewTextFallback)
        val displayTitle = when (appPresentationOverride.compactTextSource) {
            CompactTextSource.TEXT -> text.ifBlank { title }
            CompactTextSource.TITLE -> title
        }
        val displayText = if (
            appPresentationOverride.compactTextSource == CompactTextSource.TEXT &&
            title.isNotBlank() &&
            title != displayTitle
        ) {
            title
        } else {
            text
        }
        val aospCuttingEnabled = ConverterPrefs(context).getAospCuttingEnabled()

        val sourceHasProgress = hasProgress(source)
        val samsungProgressMax = samsungReparse?.progressMax ?: 0
        val samsungProgressValue = samsungReparse?.progressValue ?: 0
        val progressMax = progressOverride?.max ?: if (sourceHasProgress) {
            source.extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        } else {
            samsungProgressMax
        }
        val progressValue = progressOverride?.value ?: if (sourceHasProgress) {
            source.extras.getInt(Notification.EXTRA_PROGRESS, 0)
        } else {
            samsungProgressValue
        }
        val indeterminate = if (progressOverride != null) {
            false
        } else if (sourceHasProgress) {
            source.extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)
        } else {
            false
        }
        val hasProgress = progressOverride != null || sourceHasProgress || samsungProgressMax > 0

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(displayTitle)
            .setContentText(displayText)
            .setSubText(appName)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setWhen(resolveStableWhen(source, sbn.postTime))
            .setShowWhen(false)
            .setColor(progressColor)
            .setCategory(if (hasProgress) Notification.CATEGORY_PROGRESS else Notification.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val preferredSmallIcon = when (appPresentationOverride.iconSource) {
            NotificationIconSource.NOTIFICATION ->
                samsungSmallIcon ?: navigationDrawable?.icon ?: sourceSmallIcon ?: appSmallIcon
            NotificationIconSource.APP -> appSmallIcon ?: sourceSmallIcon
        }
        applySmallIcon(context, builder, preferredSmallIcon)
        preferredLargeIcon?.let(builder::setLargeIcon)

        if (requestPromoted && canRequestPromotedOngoing(context)) {
            builder.setRequestPromotedOngoing(true)
        }

        if (otpOverride != null) {
            builder.addAction(buildCopyOtpAction(context, sbn, otpOverride.code))
        }

        source.contentIntent?.let(builder::setContentIntent)
        source.deleteIntent?.let(builder::setDeleteIntent)

        copySourceActions(
            context = context,
            source = source,
            builder = builder,
            maxActions = if (otpOverride != null) MAX_MIRRORED_ACTIONS - 1 else MAX_MIRRORED_ACTIONS
        )

        if (hasProgress) {
            if (indeterminate || progressMax <= 0) {
                builder.setProgress(0, 0, true)
                builder.setStyle(
                    NotificationCompat.ProgressStyle()
                        .setProgressIndeterminate(true)
                        .setStyledByProgress(true)
                )
            } else {
                val safeMax = progressMax.coerceAtLeast(1)
                val safeProgress = progressValue.coerceIn(0, safeMax)
                val percent = ((safeProgress.toFloat() / safeMax.toFloat()) * 100f)
                    .roundToInt()
                    .coerceIn(0, 100)

                builder.setProgress(safeMax, safeProgress, false)
                builder.setStyle(
                    NotificationCompat.ProgressStyle()
                        .setProgress(percent)
                        .setStyledByProgress(true)
                )
                builder.setShortCriticalText(
                    limitIslandText(smartShortTextOverride ?: "$percent%", aospCuttingEnabled)
                )
            }
        } else if (otpOverride != null) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(text))
            builder.setShortCriticalText(
                limitIslandText(otpShortTextOverride ?: otpOverride.code, aospCuttingEnabled)
            )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(text))
        }
        if (smartShortTextOverride != null && !hasProgress) {
            builder.setContentText(smartShortTextOverride)
            builder.setShortCriticalText(limitIslandText(smartShortTextOverride, aospCuttingEnabled))
        }

        if (samsungNowBarBridgeEnabled && smartRuleId == "navigation") {
            // Prevent duplicated navigation direction line under the custom card.
            builder.setContentText("")
        }

        if (samsungNowBarBridgeEnabled) {
            val effectiveSecondaryText = if (smartShortTextOverride != null && !hasProgress) {
                smartShortTextOverride
            } else {
                displayText
            }
            val chipText = samsungReparse?.chipText?.takeIf { it.isNotBlank() }
                ?: otpShortTextOverride
                ?: smartShortTextOverride
                ?: displayTitle
            SamsungLiveUpdateReparser(context).applyNowBarBridge(
                builder = builder,
                source = source,
                sourcePackageName = sbn.packageName,
                primaryText = displayTitle,
                secondaryText = effectiveSecondaryText,
                chipText = chipText,
                chipIcon = preferredSmallIcon,
                hasProgress = hasProgress,
                progressValue = progressValue,
                progressMax = progressMax,
                showSecondaryInNowBar = smartRuleId != "navigation"
            )
        }

        return builder.build()
    }

    private fun notifyWithPromotionFallback(
        context: Context,
        manager: NotificationManagerCompat,
        notificationId: Int,
        promotedNotification: Notification,
        sbn: StatusBarNotification,
        appPresentationOverride: AppPresentationOverride,
        progressOverride: ProgressOverride?,
        otpOverride: OtpMatch?,
        smartShortTextOverride: String?,
        smartRuleId: String? = null,
        otpShortTextOverride: String? = null,
        samsungReparse: SamsungReparsePayload? = null,
        samsungNowBarBridgeEnabled: Boolean = false
    ) {
        try {
            manager.notify(notificationId, promotedNotification)
        } catch (error: Throwable) {
            val fallback = buildMirroredNotification(
                context = context,
                sbn = sbn,
                appPresentationOverride = appPresentationOverride,
                progressOverride = progressOverride,
                otpOverride = otpOverride,
                smartShortTextOverride = smartShortTextOverride,
                smartRuleId = smartRuleId,
                requestPromoted = false,
                otpShortTextOverride = otpShortTextOverride,
                samsungReparse = samsungReparse,
                samsungNowBarBridgeEnabled = samsungNowBarBridgeEnabled
            )
            manager.notify(notificationId, fallback)
        }
    }

    private fun canRequestPromotedOngoing(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 36) {
            return true
        }
        return context.checkSelfPermission(POST_PROMOTED_NOTIFICATIONS_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun detectSmartStage(
        packageName: String,
        source: Notification,
        parserDictionary: LiveParserDictionary,
        navigationEnabled: Boolean
    ): SmartStageMatch? {
        val combinedText = collectNotificationText(
            notification = source,
            fallbackTitle = packageName,
            includeRemoteViewTexts = isLikelyNavigationPackage(packageName)
        ).lowercase(Locale.ROOT)
        val packageLower = packageName.lowercase(Locale.ROOT)

        for (rule in parserDictionary.smartRules) {
            if (rule.id == "navigation" && !navigationEnabled) {
                continue
            }
            if (!rule.isRelevant(packageLower, combinedText)) {
                continue
            }
            if (rule.isExcluded(combinedText)) {
                continue
            }

            val matchedSignal = rule.signals.firstOrNull { it.pattern.containsMatchIn(combinedText) } ?: continue
            val entityToken = if (rule.id == "navigation") {
                "route"
            } else {
                extractEntityToken(combinedText, parserDictionary)
            }
            val compactOrderCode = if (rule.id == "food") {
                extractCompactOrderCode(entityToken)
            } else {
                null
            }

            return SmartStageMatch(
                aggregateKey = "$packageLower:${rule.id}:$entityToken",
                stageValue = matchedSignal.stage,
                maxStage = rule.maxStage,
                compactOrderCode = compactOrderCode,
                keepHighestStage = rule.id != "navigation"
            )
        }

        return null
    }

    private fun detectTextProgress(
        packageName: String,
        source: Notification
    ): TextProgressMatch? {
        val combinedText = collectNotificationText(
            notification = source,
            fallbackTitle = packageName,
            includeRemoteViewTexts = true
        )
        if (combinedText.isBlank()) {
            return null
        }

        val combinedLower = combinedText.lowercase(Locale.ROOT)
        val matches = TEXT_PROGRESS_PERCENT_PATTERN.findAll(combinedText)
        for (match in matches) {
            val percentValue = match.groupValues.getOrNull(1)?.toIntOrNull() ?: continue
            if (percentValue !in 0..100) {
                continue
            }
            if (isDiscountPercentContext(
                    textLower = combinedLower,
                    start = match.range.first,
                    endExclusive = match.range.last + 1
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

    private fun isDiscountPercentContext(
        textLower: String,
        start: Int,
        endExclusive: Int
    ): Boolean {
        val windowStart = (start - 56).coerceAtLeast(0)
        val windowEnd = (endExclusive + 56).coerceAtMost(textLower.length)
        val context = textLower.substring(windowStart, windowEnd)
        return TEXT_PROGRESS_DISCOUNT_CONTEXT_PATTERN.containsMatchIn(context) ||
                TEXT_PROGRESS_OFFER_CONTEXT_PATTERN.containsMatchIn(context) ||
                TEXT_PROGRESS_MONEY_CONTEXT_PATTERN.containsMatchIn(context)
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
            isRussianLocale = isRussianLocale(context)
        )
    }

    private fun extractNavigationDistanceText(notification: Notification, fallbackTitle: String): String? {
        val combinedText = collectNotificationText(
            notification = notification,
            fallbackTitle = fallbackTitle,
            includeRemoteViewTexts = true
        )
        val match = NAVIGATION_DISTANCE_PATTERN.find(combinedText) ?: return null
        return match.value
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { null }
    }

    private fun isRussianLocale(context: Context): Boolean {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
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
        code: String,
        nowMs: Long
    ): Boolean {
        if (state.lastAutoCopiedCode != code) {
            state.lastAutoCopiedCode = code
            state.lastAutoCopiedAtMs = nowMs
            return true
        }

        if (nowMs - state.lastAutoCopiedAtMs >= OTP_REPEAT_SUPPRESS_MS) {
            state.lastAutoCopiedAtMs = nowMs
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
        samsungReparse: SamsungReparsePayload?,
        samsungNowBarBridgeEnabled: Boolean
    ) {
        val generation = synchronized(stateLock) {
            val nextGeneration = (otpAnimationGenerations[otpMatch.aggregateKey] ?: 0L) + 1L
            otpAnimationGenerations[otpMatch.aggregateKey] = nextGeneration
            nextGeneration
        }

        val copiedLabel = if (isRussianLocale(context)) "Скопировано" else "Copied"

        scheduleOtpAnimationStep(
            context = context,
            manager = manager,
            sbn = sbn,
            appPresentationOverride = appPresentationOverride,
            otpMatch = otpMatch,
            samsungReparse = samsungReparse,
            samsungNowBarBridgeEnabled = samsungNowBarBridgeEnabled,
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
            samsungReparse = samsungReparse,
            samsungNowBarBridgeEnabled = samsungNowBarBridgeEnabled,
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
        samsungReparse: SamsungReparsePayload?,
        samsungNowBarBridgeEnabled: Boolean,
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
                    progressOverride = null,
                    otpOverride = otpMatch,
                    smartShortTextOverride = null,
                    requestPromoted = true,
                    otpShortTextOverride = otpShortTextOverride,
                    samsungReparse = samsungReparse,
                    samsungNowBarBridgeEnabled = samsungNowBarBridgeEnabled
                )
                notifyWithPromotionFallback(
                    context = context,
                    manager = manager,
                    notificationId = mirrorIdForKey(otpMatch.aggregateKey),
                    promotedNotification = notification,
                    sbn = sbn,
                    appPresentationOverride = appPresentationOverride,
                    progressOverride = null,
                    otpOverride = otpMatch,
                    smartShortTextOverride = null,
                    otpShortTextOverride = otpShortTextOverride,
                    samsungReparse = samsungReparse,
                    samsungNowBarBridgeEnabled = samsungNowBarBridgeEnabled
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val frameworkSmallIcon = source.smallIcon
            if (frameworkSmallIcon != null) {
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

        return try {
            val packageContext = context.createPackageContext(sbn.packageName, 0)
            IconCompat.createWithResource(
                packageContext.resources,
                sbn.packageName,
                legacyIconRes
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveAppSmallIcon(context: Context, packageName: String): IconCompat? {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            if (appInfo.icon == 0) {
                null
            } else {
                val packageContext = context.createPackageContext(packageName, 0)
                IconCompat.createWithResource(
                    packageContext.resources,
                    packageName,
                    appInfo.icon
                )
            }
        } catch (_: Exception) {
            null
        }
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

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is android.graphics.drawable.BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = drawable.intrinsicWidth.coerceAtLeast(1).coerceAtMost(512)
        val height = drawable.intrinsicHeight.coerceAtLeast(1).coerceAtMost(512)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
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
                    val value = runCatching {
                        field.isAccessible = true
                        field.get(action)
                    }.getOrNull()
                    when (value) {
                        is CharSequence -> {
                            val normalized = value.toString().trim()
                            if (normalized.isNotEmpty()) {
                                if (likelyTextAction || field.name.contains("text", ignoreCase = true)) {
                                    values.add(normalized)
                                }
                            }
                        }

                        is Array<*> -> {
                            value.filterIsInstance<CharSequence>()
                                .map { it.toString().trim() }
                                .filter { it.isNotEmpty() }
                                .forEach(values::add)
                        }
                    }
                }
            }
        }
        return values.toList()
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
        context: Context,
        source: Notification,
        builder: NotificationCompat.Builder,
        maxActions: Int
    ) {
        val actions = source.actions ?: return
        if (actions.isEmpty()) {
            return
        }

        actions.take(maxActions.coerceAtLeast(0)).forEach { frameworkAction ->
            val compatAction = toCompatAction(context, frameworkAction) ?: return@forEach
            builder.addAction(compatAction)
        }
    }

    private fun toCompatAction(
        context: Context,
        frameworkAction: Notification.Action
    ): NotificationCompat.Action? {
        if (frameworkAction.actionIntent == null) {
            return null
        }

        return try {
            NotificationCompat.Action.Builder.fromAndroidAction(frameworkAction).build()
        } catch (_: Exception) {
            val title = frameworkAction.title?.toString()?.takeIf { it.isNotBlank() } ?: "Action"
            NotificationCompat.Action.Builder(
                IconCompat.createWithResource(context, R.drawable.ic_stat_liveupdate),
                title,
                frameworkAction.actionIntent
            ).build()
        }
    }

    private fun hasProgress(notification: Notification): Boolean {
        val extras = notification.extras
        val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val indeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)
        return max > 0 || indeterminate
    }

    private fun extractTitle(
        notification: Notification,
        fallbackName: String,
        allowRemoteViewFallback: Boolean
    ): String {
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
        val normalizedTitle = title?.toString()?.takeIf { it.isNotBlank() }
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
        val normalized = text?.toString()?.takeIf { it.isNotBlank() }
        if (normalized != null) {
            return normalized
        }
        if (!allowRemoteViewFallback) {
            return "Live update in progress"
        }
        val remoteText = extractRemoteViewTexts(notification).firstOrNull()
        return remoteText ?: "Live update in progress"
    }

    private fun collectNotificationText(
        notification: Notification,
        fallbackTitle: String,
        includeRemoteViewTexts: Boolean
    ): String {
        val extras = notification.extras
        val parts = mutableListOf<String>()

        fun add(value: CharSequence?) {
            val text = value?.toString()?.trim().orEmpty()
            if (text.isNotBlank()) {
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

    private fun isLikelyNavigationPackage(packageName: String): Boolean {
        val packageLower = packageName.lowercase(Locale.ROOT)
        if (KNOWN_NAVIGATION_PACKAGES.contains(packageLower)) {
            return true
        }
        return packageLower.contains("navigation") ||
                packageLower.contains("navigator") ||
                packageLower.contains(".maps")
    }

    private fun mirrorIdForKey(key: String): Int {
        val value = key.hashCode()
        return if (value == Int.MIN_VALUE) 0 else abs(value)
    }

    private fun limitIslandText(value: String?, enabled: Boolean): String {
        val normalized = value.orEmpty()
        if (!enabled) {
            return normalized
        }
        return normalized.take(AOSP_ISLAND_TEXT_LIMIT)
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
                if (state.activeSbnKeys.isEmpty()) {
                    aggregateStates.remove(smartAggregateKey)
                    idsToCancel.add(mirrorIdForKey(smartAggregateKey))
                }
            } else {
                idsToCancel.add(mirrorIdForKey(smartAggregateKey))
            }
        }
        return idsToCancel
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
                    idsToCancel.add(mirrorIdForKey(otpAggregateKey))
                }
            } else {
                otpAnimationGenerations.remove(otpAggregateKey)
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
        val activeSbnKeys: MutableSet<String> = mutableSetOf()
    )

    private data class OtpAggregateState(
        val activeSbnKeys: MutableSet<String> = mutableSetOf(),
        var lastRenderedAtMs: Long = 0L,
        var lastAutoCopiedCode: String = "",
        var lastAutoCopiedAtMs: Long = 0L
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
        val compactOrderCode: String?
    )

    private data class RemoteDrawableAssets(
        val icon: IconCompat?,
        val bitmap: Bitmap?
    )

    private data class SmartStageMatch(
        val aggregateKey: String,
        val stageValue: Int,
        val maxStage: Int,
        val compactOrderCode: String?,
        val keepHighestStage: Boolean
    )

    private data class TextProgressMatch(
        val percent: Int,
        val shortText: String
    )

    private data class OtpMatch(
        val code: String,
        val aggregateKey: String
    )
}
