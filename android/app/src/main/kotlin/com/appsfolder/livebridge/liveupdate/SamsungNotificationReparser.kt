package com.kakao.taxi.liveupdate

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.service.notification.StatusBarNotification
import android.widget.RemoteViews
import androidx.core.graphics.drawable.IconCompat
import java.util.Locale

internal data class SamsungReparsePayload(
    val title: String?,
    val text: String?,
    val chipText: String?,
    val progressValue: Int?,
    val progressMax: Int?,
    val icon: IconCompat?,
    val rightIcon: IconCompat?,
    val largeIconBitmap: Bitmap?
) {
    val hasProgress: Boolean
        get() = (progressMax ?: 0) > 0
}

internal object SamsungNotificationReparser {
    private const val ONGOING_KEY_PREFIX = "android.ongoingActivityNoti."

    private const val KEY_STYLE = "${ONGOING_KEY_PREFIX}style"
    private const val KEY_PRIMARY_INFO = "${ONGOING_KEY_PREFIX}primaryInfo"
    private const val KEY_SECONDARY_INFO = "${ONGOING_KEY_PREFIX}secondaryInfo"
    private const val KEY_NOWBAR_PRIMARY_INFO = "${ONGOING_KEY_PREFIX}nowbarPrimaryInfo"
    private const val KEY_NOWBAR_SECONDARY_INFO = "${ONGOING_KEY_PREFIX}nowbarSecondaryInfo"
    private const val KEY_CHIP_EXPANDED_TEXT = "${ONGOING_KEY_PREFIX}chipExpandedText"
    private const val KEY_PROGRESS = "${ONGOING_KEY_PREFIX}progress"
    private const val KEY_PROGRESS_MAX = "${ONGOING_KEY_PREFIX}progressMax"
    private const val KEY_CHIP_ICON = "${ONGOING_KEY_PREFIX}chipIcon"
    private const val KEY_NOWBAR_ICON = "${ONGOING_KEY_PREFIX}nowbarIcon"
    private const val KEY_SECOND_ICON = "${ONGOING_KEY_PREFIX}secondIcon"
    private const val KEY_SECONDARY_INFO_ICON = "${ONGOING_KEY_PREFIX}secondaryInfoIcon"
    private const val KEY_CHRONOMETER_REMOTE_VIEW = "${ONGOING_KEY_PREFIX}chronometerRemoteView"

    fun parse(context: Context, sbn: StatusBarNotification): SamsungReparsePayload? {
        if (!isSamsungDevice()) {
            return null
        }

        val notification = sbn.notification
        val extras = notification.extras ?: return null
        val hasSamsungOngoingPayload = hasSamsungOngoingPayload(extras)
        if (!hasSamsungOngoingPayload) {
            return null
        }

        val packageContext = runCatching { context.createPackageContext(sbn.packageName, 0) }.getOrNull()
        val packageResources = packageContext?.resources

        val primaryInfo = normalizeText(extras.getCharSequence(KEY_PRIMARY_INFO))
        val secondaryInfo = normalizeText(extras.getCharSequence(KEY_SECONDARY_INFO))
        val nowbarPrimaryInfo = normalizeText(extras.getCharSequence(KEY_NOWBAR_PRIMARY_INFO))
        val nowbarSecondaryInfo = normalizeText(extras.getCharSequence(KEY_NOWBAR_SECONDARY_INFO))
        val chipExpandedText = normalizeText(extras.getCharSequence(KEY_CHIP_EXPANDED_TEXT))

        val fallbackTitle = normalizeText(
            extras.getCharSequence(Notification.EXTRA_TITLE)
                ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
        )
        val fallbackText = normalizeText(
            extras.getCharSequence(Notification.EXTRA_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
        )

        val remoteViews = mutableListOf<RemoteViews>()
        (extras.get(KEY_CHRONOMETER_REMOTE_VIEW) as? RemoteViews)?.let(remoteViews::add)
        notification.contentView?.let(remoteViews::add)
        notification.bigContentView?.let(remoteViews::add)
        notification.headsUpContentView?.let(remoteViews::add)
        val remoteTexts = extractRemoteViewTexts(remoteViews)

        val title = firstNotBlank(
            primaryInfo,
            nowbarPrimaryInfo,
            fallbackTitle,
            remoteTexts.firstOrNull(),
            chipExpandedText
        )
        val textCandidate = firstNotBlank(
            secondaryInfo,
            nowbarSecondaryInfo,
            fallbackText,
            remoteTexts.firstOrNull { candidate ->
                candidate.isNotBlank() && candidate != title
            }
        )
        val text = textCandidate ?: if (chipExpandedText != title) chipExpandedText else null

        val progressMax = readPositiveInt(extras.get(KEY_PROGRESS_MAX))
        val progressValue = readInt(extras.get(KEY_PROGRESS))?.coerceAtLeast(0)?.let { value ->
            if (progressMax != null) value.coerceIn(0, progressMax) else value
        }

        val chipIcon = resolveChipIcon(
            context = context,
            packageContext = packageContext,
            packageName = sbn.packageName,
            resources = packageResources,
            rawValue = extras.get(KEY_CHIP_ICON)
        )
        val rightIcon = listOf(
            KEY_SECOND_ICON,
            KEY_SECONDARY_INFO_ICON,
            KEY_NOWBAR_ICON
        ).firstNotNullOfOrNull { key ->
            resolveChipIcon(
                context = context,
                packageContext = packageContext,
                packageName = sbn.packageName,
                resources = packageResources,
                rawValue = extras.get(key)
            )
        }
        val remoteDrawable = resolveRemoteDrawable(
            packageContext = packageContext,
            packageName = sbn.packageName,
            remoteViews = remoteViews
        )
        val icon = chipIcon ?: remoteDrawable?.icon
        val largeIconBitmap = remoteDrawable?.bitmap

        val style = readInt(extras.get(KEY_STYLE))
        val hasUsefulData =
            title != null ||
                    text != null ||
                    chipExpandedText != null ||
                    (progressMax ?: 0) > 0 ||
                    icon != null ||
                    rightIcon != null ||
                    largeIconBitmap != null ||
                    style == 1
        if (!hasUsefulData) {
            return null
        }

        return SamsungReparsePayload(
            title = title,
            text = text,
            chipText = chipExpandedText,
            progressValue = progressValue,
            progressMax = progressMax,
            icon = icon,
            rightIcon = rightIcon,
            largeIconBitmap = largeIconBitmap
        )
    }

    private fun hasSamsungOngoingPayload(extras: android.os.Bundle): Boolean {
        return extras.keySet().any { key ->
            key.lowercase(Locale.ROOT).startsWith(ONGOING_KEY_PREFIX)
        }
    }

    private fun isSamsungDevice(): Boolean {
        val manufacturer = (Build.MANUFACTURER ?: "").lowercase(Locale.ROOT)
        val brand = (Build.BRAND ?: "").lowercase(Locale.ROOT)
        return manufacturer.contains("samsung") || brand.contains("samsung")
    }

    private fun resolveChipIcon(
        context: Context,
        packageContext: Context?,
        packageName: String,
        resources: android.content.res.Resources?,
        rawValue: Any?
    ): IconCompat? {
        if (rawValue == null) {
            return null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && rawValue is android.graphics.drawable.Icon) {
            runCatching {
                IconCompat.createFromIcon(packageContext ?: context, rawValue)
            }.getOrNull()?.let { return it }

            val bitmap = runCatching {
                rawValue.loadDrawable(packageContext ?: context)?.let(::drawableToBitmap)
            }.getOrNull()
            if (bitmap != null) {
                return runCatching { IconCompat.createWithBitmap(bitmap) }.getOrNull()
            }
        }

        val resId = when (rawValue) {
            is Int -> rawValue
            is Long -> rawValue.toInt()
            else -> null
        } ?: return null

        if (resId <= 0 || resources == null) {
            return null
        }

        return runCatching {
            val typeName = resources.getResourceTypeName(resId)
            if (typeName == "drawable" || typeName == "mipmap") {
                IconCompat.createWithResource(resources, packageName, resId)
            } else {
                null
            }
        }.getOrNull()
    }

    private fun resolveRemoteDrawable(
        packageContext: Context?,
        packageName: String,
        remoteViews: List<RemoteViews>
    ): RemoteDrawableAssets? {
        packageContext ?: return null
        val resources = packageContext.resources

        val drawableResId = remoteViews.firstNotNullOfOrNull { rv ->
            extractFirstRemoteDrawableResId(rv, resources)
        } ?: return null

        val bitmap = runCatching {
            packageContext.getDrawable(drawableResId)?.let(::drawableToBitmap)
        }.getOrNull()
        val icon = runCatching {
            IconCompat.createWithResource(resources, packageName, drawableResId)
        }.getOrNull() ?: bitmap?.let {
            runCatching { IconCompat.createWithBitmap(it) }.getOrNull()
        }

        if (icon == null && bitmap == null) {
            return null
        }
        return RemoteDrawableAssets(
            icon = icon,
            bitmap = bitmap
        )
    }

    private fun extractRemoteViewTexts(remoteViews: List<RemoteViews>): List<String> {
        val values = linkedSetOf<String>()
        for (remoteView in remoteViews) {
            val actions = getRemoteViewActions(remoteView)
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
                val likelyTextMethod =
                    methodName.contains("settext") || methodName.contains("setcharsequence")

                for (field in fields) {
                    val fieldName = field.name.removePrefix("m").lowercase(Locale.ROOT)
                    val value = runCatching {
                        field.isAccessible = true
                        field.get(action)
                    }.getOrNull()
                    when (value) {
                        is CharSequence -> {
                            val normalized = normalizeText(value)
                            if (normalized != null &&
                                !shouldSkipRemoteViewText(fieldName, normalized, methodName) &&
                                (likelyTextMethod || field.name.contains("text", ignoreCase = true))
                            ) {
                                values.add(normalized)
                            }
                        }

                        is Array<*> -> {
                            value.filterIsInstance<CharSequence>()
                                .mapNotNull(::normalizeText)
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
                normalizedText == "settext" ||
                normalizedText == "setcharsequence" ||
                normalizedText.startsWith("settext ") ||
                normalizedText.startsWith("setcharsequence ")
    }

    private fun extractFirstRemoteDrawableResId(
        remoteViews: RemoteViews,
        resources: android.content.res.Resources
    ): Int? {
        val actions = getRemoteViewActions(remoteViews)
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
                if (!isResourceField || resId <= 0) {
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

            is Long -> {
                if (value > 0L && value <= Int.MAX_VALUE.toLong()) {
                    candidates += fieldName to value.toInt()
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

    private fun getRemoteViewActions(remoteViews: RemoteViews): List<Any> {
        return try {
            val actionsField = remoteViews.javaClass.getDeclaredField("mActions")
            actionsField.isAccessible = true
            (actionsField.get(remoteViews) as? List<*>)?.filterNotNull() ?: emptyList()
        } catch (_: Throwable) {
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
        return runCatching {
            val typeName = resources.getResourceTypeName(resId)
            typeName == "drawable" || typeName == "mipmap"
        }.getOrDefault(false)
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

    private fun normalizeText(value: CharSequence?): String? {
        return NotificationTextNormalizer.normalize(value)
    }

    private fun firstNotBlank(vararg values: String?): String? {
        return values.firstOrNull { !it.isNullOrBlank() }
    }

    private fun readPositiveInt(value: Any?): Int? {
        val parsed = readInt(value) ?: return null
        return parsed.takeIf { it > 0 }
    }

    private fun readInt(value: Any?): Int? {
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

    private data class RemoteDrawableAssets(
        val icon: IconCompat?,
        val bitmap: Bitmap?
    )
}
