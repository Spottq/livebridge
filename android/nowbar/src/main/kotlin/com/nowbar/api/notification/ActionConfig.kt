package com.nowbar.api.notification

import android.app.PendingIntent
import androidx.annotation.DrawableRes

/**
 * Describes a single notification action button for Now Bar ongoing notifications.
 *
 * Samsung Health uses [Notification.Action] with icon + text + PendingIntent triples.
 * This data class captures the full configuration for each action so that
 * [NotificationActionBuilder] can produce platform-correct actions.
 *
 * @param id          Stable identifier for this action (used for dedup and logging).
 * @param text        User-visible button label (e.g. "Pause", "Resume", "Stop").
 * @param iconRes     Drawable resource for the action icon.
 * @param intent      [PendingIntent] fired when the user taps the action, or `null` for disabled actions.
 * @param semantic    Machine-readable semantic type for programmatic handling.
 */
data class ActionConfig(
    val id: String,
    val text: String,
    @DrawableRes val iconRes: Int,
    val intent: PendingIntent? = null,
    val semantic: ActionSemantic = ActionSemantic.CUSTOM
)

/**
 * Semantic types for notification actions, derived from Samsung Health action patterns.
 *
 * Samsung Health defines these broadcast actions:
 * - `com.samsung.android.app.shealth.tracker.action.PAUSE`
 * - `com.samsung.android.app.shealth.tracker.action.RESUME`
 * - `com.samsung.android.app.shealth.tracker.action.STOP`
 * - `com.samsung.android.app.shealth.tracker.action.NEXT`
 * - `com.samsung.android.app.shealth.tracker.action.DELETE_NOTIFICATION`
 * - `android.intent.action.LOCALE_CHANGED`
 *
 * Tracking status values that determine which actions to show:
 * - 0 = Stopped (show "View Result")
 * - 1 = Active/Running (show "Pause" + "Stop")
 * - 2 = Paused (show "Resume" + "Stop")
 * - 3 = Auto-paused (show disabled "Pause")
 */
enum class ActionSemantic {
    /** Pause an active workout / timer. */
    PAUSE,
    /** Resume a paused workout / timer. */
    RESUME,
    /** Stop/finish the current workout / timer. */
    STOP,
    /** Advance to next exercise in a routine. */
    NEXT,
    /** View workout result / details. */
    VIEW_RESULT,
    /** Delete the notification (dismiss action). */
    DELETE,
    /** Application-specific custom action. */
    CUSTOM
}

/**
 * Predefined action icon resource names from Samsung Health decompiled code.
 *
 * These correspond to `R.drawable.*` references used in the original implementation:
 * - `home_pause_exercise_mtrl` — pause action icon
 * - `home_finish_exercise_mtrl` — stop/finish action icon
 * - `home_resume_exercise_mtrl` — resume action icon
 * - `home_next_exercise_mtrl` — next workout action icon
 * - `home_view_details_mtrl` — view result action icon
 * - `quick_panel_icon_empty` — empty/transparent icon for program resume actions
 */
object SamsungActionIcons {
    const val PAUSE = "home_pause_exercise_mtrl"
    const val FINISH = "home_finish_exercise_mtrl"
    const val RESUME = "home_resume_exercise_mtrl"
    const val NEXT = "home_next_exercise_mtrl"
    const val VIEW_DETAILS = "home_view_details_mtrl"
    const val EMPTY = "quick_panel_icon_empty"
}

/**
 * Samsung Health broadcast action strings used for notification control.
 *
 * These are the exact action strings from `SportOngoingNotificationHelper`:
 * ```
 * ACTION_NOTIFICATION_FILTER_PAUSE  = "com.samsung.android.app.shealth.tracker.action.PAUSE"
 * ACTION_NOTIFICATION_FILTER_STOP   = "com.samsung.android.app.shealth.tracker.action.STOP"
 * ACTION_NOTIFICATION_FILTER_NEXT   = "com.samsung.android.app.shealth.tracker.action.NEXT"
 * ACTION_NOTIFICATION_FILTER_RESUME = "com.samsung.android.app.shealth.tracker.action.RESUME"
 * ACTION_NOTIFICATION_DELETE        = "com.samsung.android.app.shealth.tracker.action.DELETE_NOTIFICATION"
 * ACTION_NOTIFICATION_FILTER_LOCALE_CHANGED = "android.intent.action.LOCALE_CHANGED"
 * ```
 *
 * The caller ID extra is:
 * ```
 * NOTIFICATION_CALLER_ID = "com.samsung.android.app.shealth.tracker.exercise.notification_caller_id"
 * ```
 */
object SamsungHealthActions {
    const val PAUSE = "com.samsung.android.app.shealth.tracker.action.PAUSE"
    const val STOP = "com.samsung.android.app.shealth.tracker.action.STOP"
    const val NEXT = "com.samsung.android.app.shealth.tracker.action.NEXT"
    const val RESUME = "com.samsung.android.app.shealth.tracker.action.RESUME"
    const val DELETE = "com.samsung.android.app.shealth.tracker.action.DELETE_NOTIFICATION"
    const val LOCALE_CHANGED = "android.intent.action.LOCALE_CHANGED"
    const val CALLER_ID_KEY = "com.samsung.android.app.shealth.tracker.exercise.notification_caller_id"
}

/**
 * PendingIntent flag combinations observed in Samsung Health decompiled code.
 *
 * Samsung Health uses these numeric flag combinations:
 * - `201326592` = `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE` (0x0C000000)
 *   Used for: broadcast intents (pause/resume/stop), activity intents (open result)
 * - `335544320` = `FLAG_CANCEL_CURRENT | FLAG_IMMUTABLE` (0x14000000)
 *   Used for: removePendingIntent (empty intent to clear content intent)
 *
 * Intent flags for result activities:
 * - `603979776` = `FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP` (0x24000000)
 *   Used in: getOpenResultIntent — result activity launch flags
 * - `268484608` = `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_TASK_ON_HOME` (0x10004000)
 *   Used in: TaskStackBuilder — first intent in back-stack
 *
 * Broadcast request codes:
 * - `0` — standard broadcast actions (pause, resume, stop, next)
 * - `Random().nextInt()` — activity intents (open workout screen)
 * - `62005` — periodization training reminder
 */
object PendingIntentFlags {
    const val UPDATE_IMMUTABLE = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    const val CANCEL_IMMUTABLE = PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    const val BROADCAST_REQUEST_CODE = 0
}

/**
 * Action-level tracking status constants from Samsung Health.
 *
 * These determine which action buttons appear on the notification:
 * - [STOPPED] (0): "View Result" button only
 * - [ACTIVE] (1): "Pause" + "Stop" buttons
 * - [PAUSED] (2): "Resume" + "Stop" buttons
 * - [AUTO_PAUSED] (3): disabled "Pause" button (null PendingIntent)
 *
 * Additional routine-specific reason codes:
 * - `9020`: User manually finished routine → show "View Result"
 */
object ActionTrackingStatus {
    const val STOPPED = 0
    const val ACTIVE = 1
    const val PAUSED = 2
    const val AUTO_PAUSED = 3
    const val REASON_USER_MANUALLY_FINISHED_ROUTINE = 9020
}