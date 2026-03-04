package ai.perplexity.app.android.liveupdate

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import io.github.d4viddf.hyperisland_kit.HyperAction
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.HyperPicture
import java.util.Locale

internal object HyperBridgeAdapter {
    private const val TAG = "HyperBridgeAdapter"
    private const val PARAM_KEY = "miui.focus.param"
    private const val PIC_KEY_MAIN = "main"
    private const val MAX_HYPER_ACTIONS = 2
    private const val MAX_ACTION_TITLE_LENGTH = 22

    fun apply(
        context: Context,
        builder: NotificationCompat.Builder,
        sourcePackageName: String,
        appName: String,
        title: String,
        content: String,
        ticker: String,
        progressPercent: Int?,
        largeIcon: Bitmap?,
        sourceActions: Array<Notification.Action>?
    ) {
        try {
            val normalizedTitle = title.ifBlank { appName }.ifBlank { sourcePackageName }.take(96)
            val normalizedContent = content.ifBlank { appName }.ifBlank { normalizedTitle }.take(220)
            val normalizedTicker = ticker.ifBlank { normalizedTitle }.take(40)
            val normalizedAppName = appName.take(48).ifBlank { null }
            val businessName = buildBusinessName(sourcePackageName)

            val hyperBuilder = HyperIslandNotification.Builder(
                context = context,
                businessName = businessName,
                ticker = normalizedTicker
            )
            val hyperActions = buildHyperActions(sourceActions)
            hyperActions.forEach(hyperBuilder::addAction)

            if (largeIcon != null) {
                hyperBuilder
                    .addPicture(HyperPicture(PIC_KEY_MAIN, largeIcon))
                    .setChatInfo(
                        title = normalizedTitle,
                        content = normalizedContent,
                        pictureKey = PIC_KEY_MAIN
                    )
                    .setSmallIsland(PIC_KEY_MAIN)
            } else {
                hyperBuilder.setBaseInfo(
                    title = normalizedTitle,
                    content = normalizedContent,
                    subTitle = normalizedAppName
                )
            }
            if (hyperActions.isNotEmpty()) {
                hyperBuilder.setTextButtons(*hyperActions.toTypedArray())
            }

            progressPercent?.coerceIn(0, 100)?.let(hyperBuilder::setProgressBar)

            val resourceBundle = hyperBuilder.buildResourceBundle()
            if (resourceBundle.keySet().isEmpty()) {
                return
            }

            builder.addExtras(resourceBundle)
            builder.addExtras(
                Bundle().apply {
                    putString(PARAM_KEY, hyperBuilder.buildJsonParam())
                }
            )
        } catch (error: Throwable) {
            Log.w(TAG, "Failed to apply HyperBridge payload: ${error.message}")
        }
    }

    private fun buildBusinessName(sourcePackageName: String): String {
        val normalized = sourcePackageName
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9_.-]"), "_")
            .take(80)
        return "livebridge.$normalized"
    }

    private fun buildHyperActions(sourceActions: Array<Notification.Action>?): List<HyperAction> {
        if (sourceActions.isNullOrEmpty()) {
            return emptyList()
        }
        val result = mutableListOf<HyperAction>()
        val seenKeys = HashSet<String>()
        var index = 0
        while (index < sourceActions.size && result.size < MAX_HYPER_ACTIONS) {
            val action = sourceActions[index]
            val pendingIntent = action.actionIntent
            val title = action.title?.toString()?.trim().orEmpty()
            val icon = action.getIcon()
            val hasTitle = title.isNotBlank()
            val dedupeKey = title.lowercase(Locale.ROOT)
            if (pendingIntent != null && hasTitle && seenKeys.add(dedupeKey)) {
                val actionIntentType = resolveActionIntentType(pendingIntent)
                val normalizedIcon = normalizeActionIcon(icon)
                result += HyperAction(
                    "src_${index + 1}",
                    title.take(MAX_ACTION_TITLE_LENGTH),
                    normalizedIcon,
                    pendingIntent,
                    actionIntentType,
                    null,
                    null,
                    null,
                    null,
                    false,
                    0,
                    null,
                    null,
                    false
                )
            }
            index += 1
        }
        return result
    }

    private fun normalizeActionIcon(icon: Icon?): Icon? {
        return icon?.takeIf {
            it.type == Icon.TYPE_BITMAP ||
                    it.type == Icon.TYPE_RESOURCE ||
                    it.type == Icon.TYPE_URI ||
                    it.type == Icon.TYPE_ADAPTIVE_BITMAP ||
                    it.type == Icon.TYPE_URI_ADAPTIVE_BITMAP
        }
    }

    private fun resolveActionIntentType(pendingIntent: PendingIntent): Int {
        return when {
            pendingIntent.isActivity -> 1
            pendingIntent.isBroadcast -> 2
            else -> 3
        }
    }
}
