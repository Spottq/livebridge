package com.nowbar.api.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Notification channel configuration for Now Bar ongoing notifications.
 *
 * ## Samsung Health Channel Registry (from `r0k.java`)
 *
 * Samsung Health defines the following notification channels:
 *
 * | Channel ID                        | Description                    | Vibration |
 * |-----------------------------------|--------------------------------|-----------|
 * | `channel.03.sport`                | Ongoing workouts               | false     |
 * | `channel.04.workout.detection`    | Detected workouts              | true      |
 * | `channel.040.women.period_begins` | Women period begins            | true      |
 * | `channel.041.women.fertile_window`| Women fertile window           | true      |
 * | `channel.042.body.composition`    | Body composition               | true      |
 * | `channel.05.food`                 | Food tracking                  | true      |
 * | `channel.051.water`               | Water tracking                 | true      |
 * | `channel.06.sleep`                | Sleep                          | true      |
 * | `channel.061.heartrate`           | Heart rate alerts              | true      |
 * | `channel.062.medications`         | Medications reminders          | n/a       |
 * | `channel.063.medications.strong`  | Medications strong reminders   | n/a       |
 * | `channel.066.health_records`      | Health records                 | n/a       |
 * | `channel.071.mindfulness`         | Mindfulness (importance LOW=2) | n/a       |
 * | `channel.072.vascular_load`       | Vascular load                  | true      |
 * | `channel.09.enrolled.programs`    | Enrolled programs              | true      |
 * | `channel.100.challenges`          | Challenges                     | n/a       |
 * | `channel.11.global.challenges`    | Global challenges              | n/a       |
 * | `channel.12.family.sharing`       | Family sharing                 | true      |
 * | `channel.13.weekly.summary`       | Weekly summary                 | true      |
 * | `channel.14.health.insights`      | Coaching insights              | true      |
 * | `channel.141.rewards`             | Rewards                        | true      |
 * | `channel.15.marketing.information`| Marketing information          | true      |
 * | `channel.16.password`             | Password (importance HIGH=4)   | n/a       |
 * | `channel.046.energy_score`        | Energy score                   | n/a       |
 * | `channel.98.all.others`           | Default / all others           | false     |
 *
 * For Now Bar / ongoing workout notifications, **`channel.03.sport`** is always used.
 * It has vibration disabled and the string resource `baseui_noti_channel_ongoing_workouts`.
 *
 * ## Key Observations
 *
 * - The sport channel (`channel.03.sport`) uses default importance (IMPORTANCE_DEFAULT = 3).
 * - Mindfulness channel (`channel.071.mindfulness`) explicitly uses IMPORTANCE_LOW (2).
 * - Password channel (`channel.16.password`) uses IMPORTANCE_HIGH (4) via `o8l.L()`.
 * - The Samsung push channels (`ppmt_notice_cid`, `ppmt_marketing_cid`) use IMPORTANCE_HIGH (4).
 *
 * ## Notification Builder Patterns (from `TrackerSportBaseNotificationManager`)
 *
 * Sport notifications are built with these settings:
 * ```
 * Notification.Builder(context, "channel.03.sport")
 *     .setContentTitle(titleString)
 *     .setContentText(textMsg)
 *     .setSmallIcon(holder.pngIconId)       // sport-specific icon
 *     .setContentIntent(pendingIntentMain)   // opens workout activity
 *     .setColor(R.color.tracker_sport_primary_dark_color_green)
 *     .addExtras(ongoingExtraBundle)         // Now Bar extras
 *     .setOngoing(true)                      // non-dismissable
 *     .setWhen(whenTimestamp)                // chronometer base
 *     .setOnlyAlertOnce(true)               // suppress re-alert
 *     .setDeleteIntent(deletePendingIntent)  // ACTION_NOTIFICATION_DELETE
 *     .setActions(actionsArray)              // or .addAction() on non-NowBar devices
 * ```
 *
 * The notification is posted with:
 * - Notification ID: `200` (PROCESSING_NOTIFICATION_ID)
 * - `notification.flags |= Notification.FLAG_ONGOING_EVENT` (flag 2)
 * - Used with `startForeground(200, notification)` in LiveTrackerService
 */
class NowBarChannelConfig private constructor(
    val channelId: String,
    val channelName: String,
    val importance: Int,
    val enableVibration: Boolean,
    val description: String?
) {

    /**
     * Ensures the notification channel exists, creating it if necessary.
     * No-op on API < 26.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(channelId) != null) return

        val channel = NotificationChannel(channelId, channelName, importance).apply {
            enableVibration(this@NowBarChannelConfig.enableVibration)
            if (!this@NowBarChannelConfig.enableVibration) {
                vibrationPattern = longArrayOf(0)
            }
            this@NowBarChannelConfig.description?.let { setDescription(it) }
        }
        manager.createNotificationChannel(channel)
    }

    class Builder(private val channelId: String) {
        private var channelName: String = "Now Bar"
        private var importance: Int = NotificationManager.IMPORTANCE_DEFAULT
        private var enableVibration: Boolean = false
        private var description: String? = null

        fun channelName(name: String) = apply { this.channelName = name }
        fun importance(importance: Int) = apply { this.importance = importance }
        fun enableVibration(enable: Boolean) = apply { this.enableVibration = enable }
        fun description(desc: String) = apply { this.description = desc }

        fun build() = NowBarChannelConfig(
            channelId = channelId,
            channelName = channelName,
            importance = importance,
            enableVibration = enableVibration,
            description = description
        )
    }

    companion object {
        /**
         * Default channel matching Samsung Health's sport workout channel.
         *
         * - Channel ID: `channel.03.sport`
         * - Importance: DEFAULT (3)
         * - Vibration: disabled
         */
        fun samsungSportDefault(): NowBarChannelConfig = Builder("channel.03.sport")
            .channelName("Ongoing Workouts")
            .importance(NotificationManager.IMPORTANCE_DEFAULT)
            .enableVibration(false)
            .build()

        /**
         * Low-importance channel matching Samsung Health's mindfulness channel.
         *
         * - Channel ID: `channel.071.mindfulness`
         * - Importance: LOW (2)
         * - Vibration: disabled
         */
        fun samsungMindfulness(): NowBarChannelConfig = Builder("channel.071.mindfulness")
            .channelName("Mindfulness")
            .importance(NotificationManager.IMPORTANCE_LOW)
            .enableVibration(false)
            .build()

        /**
         * Creates a custom Now Bar channel with the given parameters.
         * Use this for third-party apps that need their own channel.
         */
        fun custom(
            channelId: String,
            channelName: String,
            importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
            enableVibration: Boolean = false,
            description: String? = null
        ): NowBarChannelConfig = Builder(channelId)
            .channelName(channelName)
            .importance(importance)
            .enableVibration(enableVibration)
            .apply { description?.let { description(it) } }
            .build()

        /**
         * All Samsung Health notification channel IDs, extracted from `r0k.java`.
         */
        object SamsungChannels {
            const val SPORT = "channel.03.sport"
            const val WORKOUT_DETECTION = "channel.04.workout.detection"
            const val WOMEN_PERIOD = "channel.040.women.period_begins"
            const val WOMEN_FERTILE = "channel.041.women.fertile_window"
            const val BODY_COMPOSITION = "channel.042.body.composition"
            const val ENERGY_SCORE = "channel.046.energy_score"
            const val FOOD = "channel.05.food"
            const val WATER = "channel.051.water"
            const val SLEEP = "channel.06.sleep"
            const val HEART_RATE = "channel.061.heartrate"
            const val MEDICATIONS = "channel.062.medications"
            const val MEDICATIONS_STRONG = "channel.063.medications.strong"
            const val HEALTH_RECORDS = "channel.066.health_records"
            const val MINDFULNESS = "channel.071.mindfulness"
            const val VASCULAR_LOAD = "channel.072.vascular_load"
            const val ENROLLED_PROGRAMS = "channel.09.enrolled.programs"
            const val CHALLENGES = "channel.100.challenges"
            const val GLOBAL_CHALLENGES = "channel.11.global.challenges"
            const val FAMILY_SHARING = "channel.12.family.sharing"
            const val WEEKLY_SUMMARY = "channel.13.weekly.summary"
            const val HEALTH_INSIGHTS = "channel.14.health.insights"
            const val REWARDS = "channel.141.rewards"
            const val MARKETING = "channel.15.marketing.information"
            const val PASSWORD = "channel.16.password"
            const val ALL_OTHERS = "channel.98.all.others"
        }

        /**
         * Notification IDs used across Samsung Health.
         *
         * - `200` — Sport workout ongoing notification (PROCESSING_NOTIFICATION_ID)
         * - `1001` — Mindfulness player service
         * - `111` — Remote confirm test service
         * - `15000` — Water reminder
         * - `10004` — Weight reminder
         * - `62001` — Heart rate alert
         * - `62005` — Periodization training reminder (also used as request code)
         * - `9999999` — Medication reminder popup foreground service
         */
        object NotificationIds {
            const val SPORT_WORKOUT = 200
            const val MINDFULNESS = 1001
            const val REMOTE_CONFIRM = 111
            const val WATER_REMINDER = 15000
            const val WEIGHT_REMINDER = 10004
            const val HEART_RATE_ALERT = 62001
            const val PERIODIZATION_TRAINING = 62005
            const val MEDICATION_POPUP = 9999999
        }
    }
}