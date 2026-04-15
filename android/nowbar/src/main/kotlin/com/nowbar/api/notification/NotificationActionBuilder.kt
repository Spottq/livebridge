package com.nowbar.api.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon

/**
 * Builds [Notification.Action] arrays for Now Bar ongoing notifications,
 * replicating the exact patterns found in Samsung Health's
 * `SportOngoingNotificationHelper` and `TrackerSportNotificationManager`.
 *
 * ## Samsung Health Action Patterns
 *
 * Samsung Health determines which actions to show based on two factors:
 * 1. **Tracking status** (0=stopped, 1=active, 2=paused, 3=auto-paused)
 * 2. **Workout context** (single workout, routine middle, routine last, routine cool-down)
 *
 * ### Single Workout Actions (`getActionForSingleWorkout`)
 * | Status | Actions                    |
 * |--------|----------------------------|
 * | 0      | "View Result"              |
 * | 1      | "Pause" + "Stop"           |
 * | 2      | "Resume" + "Stop"          |
 *
 * ### Last/Cool-Down Workout Actions (`getActionsForLastWorkout`)
 * | Status | Actions                    |
 * |--------|----------------------------|
 * | 0      | "View Result"              |
 * | 1      | "Pause" + "Stop"           |
 * | 2      | "Resume" + "Stop"          |
 * | 3      | "Pause" (disabled)         |
 *
 * ### Middle Workout Actions (`getActionsForMiddleWorkout`)
 * | Status | Actions                    |
 * |--------|----------------------------|
 * | 1      | "Pause" + "Next"           |
 * | 2      | "Resume" + "Stop"          |
 * | 3      | "Pause" (disabled) + "Next" (disabled) |
 *
 * ### Routine Workout Actions (`getActionForRoutineWorkout`)
 * Decision tree:
 * 1. If reason == 9020 (user manually finished) → "View Result"
 * 2. If cool-down or last workout → delegate to `getActionsForLastWorkout`
 * 3. If cool-down/last finished and status == 0 → "View Result"
 * 4. Otherwise → delegate to `getActionsForMiddleWorkout`
 *
 * ### Program Resume Actions (`getProgramResumeActions`)
 * Used when `programInfo.isProgramWorkout()` and status is paused:
 * - "Resume" (with program-specific PendingIntent) + "Stop"
 *
 * ## PendingIntent Construction
 *
 * Broadcast intents (pause/resume/stop/next):
 * ```
 * Intent(actionString).apply {
 *     setPackage(context.packageName)
 *     putExtra(NOTIFICATION_CALLER_ID, NOTIFICATION_CALLER_ID)
 * }
 * PendingIntent.getBroadcast(context, 0, intent, FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE)
 * ```
 *
 * Result activity intents use TaskStackBuilder with:
 * ```
 * intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP)  // 603979776
 * intent.putExtra("tracker_start_from_notification", 200)
 * PendingIntent.getActivities(context, 0, intents, FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE)
 * ```
 *
 * ## Now Bar Feature Detection
 *
 * Samsung Health checks for Now Bar support before setting actions:
 * ```
 * context.packageManager.hasSystemFeature("com.samsung.feature.nowbar")
 * ```
 * - **With Now Bar**: uses `setActions(Array)` to replace all actions at once
 * - **Without Now Bar**: uses `addAction(Action)` to append individual actions
 */
class NotificationActionBuilder(private val context: Context) {

    /**
     * Builds actions for a single (non-routine) workout notification.
     *
     * Replicates `SportOngoingNotificationHelper.getActionForSingleWorkout()`.
     */
    fun buildSingleWorkoutActions(
        trackingStatus: Int,
        actionFactory: ActionFactory
    ): List<Notification.Action> = when (trackingStatus) {
        ActionTrackingStatus.STOPPED -> listOf(
            actionFactory.viewResult()
        )
        ActionTrackingStatus.ACTIVE -> listOf(
            actionFactory.pause(),
            actionFactory.stop()
        )
        ActionTrackingStatus.PAUSED -> listOf(
            actionFactory.resume(),
            actionFactory.stop()
        )
        else -> emptyList()
    }

    /**
     * Builds actions for the last workout or cool-down in a routine.
     *
     * Replicates `SportOngoingNotificationHelper.getActionsForLastWorkout()`.
     */
    fun buildLastWorkoutActions(
        trackingStatus: Int,
        actionFactory: ActionFactory
    ): List<Notification.Action> = when (trackingStatus) {
        ActionTrackingStatus.STOPPED -> listOf(
            actionFactory.viewResult()
        )
        ActionTrackingStatus.ACTIVE -> listOf(
            actionFactory.pause(),
            actionFactory.stop()
        )
        ActionTrackingStatus.PAUSED -> listOf(
            actionFactory.resume(),
            actionFactory.stop()
        )
        ActionTrackingStatus.AUTO_PAUSED -> listOf(
            actionFactory.pauseDisabled()
        )
        else -> emptyList()
    }

    /**
     * Builds actions for a middle workout in a routine (not last, not cool-down).
     *
     * Replicates `SportOngoingNotificationHelper.getActionsForMiddleWorkout()`.
     */
    fun buildMiddleWorkoutActions(
        trackingStatus: Int,
        actionFactory: ActionFactory
    ): List<Notification.Action> = when (trackingStatus) {
        ActionTrackingStatus.ACTIVE -> listOf(
            actionFactory.pause(),
            actionFactory.next()
        )
        ActionTrackingStatus.PAUSED -> listOf(
            actionFactory.resume(),
            actionFactory.stop()
        )
        ActionTrackingStatus.AUTO_PAUSED -> listOf(
            actionFactory.pauseDisabled(),
            actionFactory.nextDisabled()
        )
        else -> emptyList()
    }

    /**
     * Builds actions for a routine workout, selecting the appropriate sub-pattern
     * based on reason code and workout position.
     *
     * Replicates `SportOngoingNotificationHelper.getActionForRoutineWorkout()`.
     *
     * @param reason             Reason code (9020 = user manually finished).
     * @param trackingStatus     Current tracking status (0-3).
     * @param isCoolDownOrLast   Whether the current workout is cool-down or last.
     * @param isCoolDownOrLastFinished  Whether cool-down/last workout has finished.
     * @param actionFactory      Factory to create platform-specific actions.
     */
    fun buildRoutineWorkoutActions(
        reason: Int,
        trackingStatus: Int,
        isCoolDownOrLast: Boolean,
        isCoolDownOrLastFinished: Boolean,
        actionFactory: ActionFactory
    ): List<Notification.Action> {
        if (reason == ActionTrackingStatus.REASON_USER_MANUALLY_FINISHED_ROUTINE) {
            return listOf(actionFactory.viewResult())
        }
        if (isCoolDownOrLast) {
            return buildLastWorkoutActions(trackingStatus, actionFactory)
        }
        if (isCoolDownOrLastFinished && trackingStatus == ActionTrackingStatus.STOPPED) {
            return listOf(actionFactory.viewResult())
        }
        return buildMiddleWorkoutActions(trackingStatus, actionFactory)
    }

    /**
     * Builds program-specific resume actions.
     *
     * Replicates `TrackerSportNotificationManager.getProgramResumeActions()`.
     *
     * @param programResumeIntent  PendingIntent that resumes the fitness program.
     * @param actionFactory        Factory to create platform-specific actions.
     */
    fun buildProgramResumeActions(
        programResumeIntent: PendingIntent,
        actionFactory: ActionFactory
    ): List<Notification.Action> = listOf(
        actionFactory.resumeWithIntent(programResumeIntent),
        actionFactory.stop()
    )

    /**
     * Applies actions to a notification builder, choosing the appropriate method
     * based on Now Bar support.
     *
     * Samsung Health checks `hasSystemFeature("com.samsung.feature.nowbar")`:
     * - **With Now Bar**: `builder.setActions(actions)` — replaces all actions
     * - **Without Now Bar**: `builder.addAction(action)` — appends each action
     *
     * @param builder          The notification builder to apply actions to.
     * @param actions          The actions to apply.
     * @param hasNowBarFeature Whether the device supports Now Bar.
     */
    fun applyActions(
        builder: Notification.Builder,
        actions: List<Notification.Action>,
        hasNowBarFeature: Boolean
    ) {
        if (hasNowBarFeature) {
            builder.setActions(*actions.toTypedArray())
        } else {
            actions.forEach { builder.addAction(it) }
        }
    }

    /**
     * Creates a broadcast [PendingIntent] for notification control actions,
     * replicating Samsung Health's `SportOngoingNotificationHelper.getPendingIntent()`.
     *
     * @param action  Broadcast action string (e.g. [SamsungHealthActions.PAUSE]).
     */
    fun createBroadcastIntent(action: String): PendingIntent {
        val intent = Intent(action).apply {
            setPackage(context.packageName)
        }
        return PendingIntent.getBroadcast(
            context,
            PendingIntentFlags.BROADCAST_REQUEST_CODE,
            intent,
            PendingIntentFlags.UPDATE_IMMUTABLE
        )
    }

    /**
     * Creates a delete [PendingIntent] for the notification dismiss action.
     */
    fun createDeleteIntent(): PendingIntent =
        createBroadcastIntent(SamsungHealthActions.DELETE)

    /**
     * Factory interface for creating [Notification.Action] instances.
     *
     * Implementations should provide the correct icon resources and text
     * for their specific use case (workout, timer, etc.).
     */
    interface ActionFactory {
        fun pause(): Notification.Action
        fun resume(): Notification.Action
        fun stop(): Notification.Action
        fun next(): Notification.Action
        fun viewResult(): Notification.Action

        /** Pause action with null PendingIntent (disabled state for auto-pause). */
        fun pauseDisabled(): Notification.Action
        /** Next action with null PendingIntent (disabled state for auto-pause). */
        fun nextDisabled(): Notification.Action
        /** Resume with a custom PendingIntent (for program workouts). */
        fun resumeWithIntent(intent: PendingIntent): Notification.Action
    }

    /**
     * Default [ActionFactory] that creates actions using broadcast intents,
     * matching Samsung Health's exact patterns.
     *
     * Action construction follows `SportOngoingNotificationHelper.getAction()`:
     * ```
     * Icon.createWithResource(context, iconResource)
     * Notification.Action.Builder(icon, actionText, pendingIntent).build()
     * ```
     *
     * @param pauseText      Text for pause button (e.g. `getString(R.string.tracker_sport_pause_button)`).
     * @param resumeText     Text for resume button.
     * @param stopText       Text for stop button.
     * @param nextText       Text for next button.
     * @param viewResultText Text for view result button.
     * @param pauseIcon      Drawable resource for pause icon.
     * @param resumeIcon     Drawable resource for resume icon.
     * @param stopIcon       Drawable resource for stop icon.
     * @param nextIcon       Drawable resource for next icon.
     * @param viewResultIcon Drawable resource for view result icon.
     * @param resultIntent   PendingIntent for "View Result" action.
     */
    inner class DefaultActionFactory(
        private val pauseText: String,
        private val resumeText: String,
        private val stopText: String,
        private val nextText: String,
        private val viewResultText: String,
        private val pauseIcon: Int,
        private val resumeIcon: Int,
        private val stopIcon: Int,
        private val nextIcon: Int,
        private val viewResultIcon: Int,
        private val resultIntent: PendingIntent? = null
    ) : ActionFactory {

        private fun buildAction(text: String, iconRes: Int, intent: PendingIntent?): Notification.Action {
            val icon = Icon.createWithResource(context, iconRes)
            return Notification.Action.Builder(icon, text, intent).build()
        }

        override fun pause() = buildAction(pauseText, pauseIcon, createBroadcastIntent(SamsungHealthActions.PAUSE))
        override fun resume() = buildAction(resumeText, resumeIcon, createBroadcastIntent(SamsungHealthActions.RESUME))
        override fun stop() = buildAction(stopText, stopIcon, createBroadcastIntent(SamsungHealthActions.STOP))
        override fun next() = buildAction(nextText, nextIcon, createBroadcastIntent(SamsungHealthActions.NEXT))
        override fun viewResult() = buildAction(viewResultText, viewResultIcon, resultIntent)
        override fun pauseDisabled() = buildAction(pauseText, pauseIcon, null)
        override fun nextDisabled() = buildAction(nextText, nextIcon, null)
        override fun resumeWithIntent(intent: PendingIntent) = buildAction(resumeText, resumeIcon, intent)
    }
}