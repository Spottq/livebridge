package com.nowbar.api.cards

import android.app.PendingIntent
import androidx.core.graphics.drawable.IconCompat
import kotlin.time.Duration

enum class WorkoutType {
    RUNNING, CYCLING, WALKING, SWIMMING, GYM, OTHER
}

enum class DistanceUnit(val symbol: String) {
    KM("km"), MI("mi"), M("m")
}

data class WorkoutCard(
    override val title: String,
    override val icon: IconCompat,
    override val accentColor: Int? = null,
    override val tapAction: PendingIntent? = null,
    override val chipText: String? = null,
    val activityType: WorkoutType,
    val elapsed: Duration,
    val distance: Double? = null,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val heartRate: Int? = null,
    val calories: Int? = null,
    val pace: String? = null,
    val progress: Int = 0,
    val actionBgColor: Int? = null
) : NowBarCard(
    type = CardType.WORKOUT,
    title = title,
    icon = icon,
    accentColor = accentColor,
    tapAction = tapAction,
    chipText = chipText
) {
    override fun toPrimaryInfo(): String {
        return when (activityType) {
            WorkoutType.RUNNING -> "Running"
            WorkoutType.CYCLING -> "Cycling"
            WorkoutType.WALKING -> "Walking"
            WorkoutType.SWIMMING -> "Swimming"
            WorkoutType.GYM -> "Gym"
            WorkoutType.OTHER -> title
        }
    }

    override fun toSecondaryInfo(): String {
        val parts = mutableListOf<String>()
        distance?.let { d ->
            parts.add("%.2f %s".format(d, distanceUnit.symbol))
        }
        val totalSeconds = elapsed.inWholeSeconds
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val timeStr = if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
        parts.add(timeStr)
        return parts.joinToString(" | ")
    }

    override fun toNowBarSecondaryInfo(): String {
        return distance?.let { d ->
            "%.1f %s".format(d, distanceUnit.symbol)
        } ?: toSecondaryInfo()
    }

    override fun toProgress(): Int = progress.coerceIn(0, 100)

    override fun toProgressMax(): Int = 100

    override fun toNowBarPrimaryInfo(): String = toPrimaryInfo()

    override fun toSubstName(): String = title

    override fun toActionBgColor(): Int? = actionBgColor ?: accentColor

    override fun hasChronometerSupport(): Boolean = true

    class Builder(
        private val title: String,
        private val icon: IconCompat,
        private val activityType: WorkoutType,
        private val elapsed: Duration
    ) {
        private var accentColor: Int? = null
        private var tapAction: PendingIntent? = null
        private var chipText: String? = null
        private var distance: Double? = null
        private var distanceUnit: DistanceUnit = DistanceUnit.KM
        private var heartRate: Int? = null
        private var calories: Int? = null
        private var pace: String? = null
        private var progress: Int = 0
        private var actionBgColor: Int? = null

        fun accentColor(color: Int) = apply { this.accentColor = color }
        fun tapAction(action: PendingIntent) = apply { this.tapAction = action }
        fun chipText(text: String) = apply { this.chipText = text }
        fun distance(distance: Double) = apply { this.distance = distance }
        fun distanceUnit(unit: DistanceUnit) = apply { this.distanceUnit = unit }
        fun heartRate(rate: Int) = apply { this.heartRate = rate }
        fun calories(cal: Int) = apply { this.calories = cal }
        fun pace(pace: String) = apply { this.pace = pace }
        fun progress(progress: Int) = apply { this.progress = progress }
        fun actionBgColor(color: Int) = apply { this.actionBgColor = color }

        fun build(): WorkoutCard = WorkoutCard(
            title = title,
            icon = icon,
            accentColor = accentColor,
            tapAction = tapAction,
            chipText = chipText,
            activityType = activityType,
            elapsed = elapsed,
            distance = distance,
            distanceUnit = distanceUnit,
            heartRate = heartRate,
            calories = calories,
            pace = pace,
            progress = progress,
            actionBgColor = actionBgColor
        )

        companion object {
            @JvmStatic
            fun create(
                title: String,
                icon: IconCompat,
                activityType: WorkoutType,
                elapsed: Duration
            ) = Builder(title, icon, activityType, elapsed)
        }
    }
}