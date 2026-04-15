package com.nowbar.api.types

/**
 * All trackable workout metrics from Samsung Health ExerciseRecord.
 * This mirrors the data fields available during a live exercise session.
 */
data class WorkoutMetrics(
    // -- Time --
    /** Timestamp of the data point (epoch millis). */
    val timeStamp: Long = 0L,
    /** Total elapsed time since exercise start (millis). */
    val elapsedMilliSeconds: Long = 0L,
    /** Actual moving time excluding pauses (millis). */
    val movingTime: Long = 0L,
    /** Time spent on incline terrain (millis). */
    val inclineTime: Long = 0L,
    /** Time spent on decline terrain (millis). */
    val declineTime: Long = 0L,
    /** Time spent on flat terrain (millis). */
    val flatTime: Long = 0L,
    /** Remaining time to reach goal (millis). */
    val remainingTime: Long = 0L,

    // -- Distance --
    /** Total distance covered (meters). */
    val totalDistance: Float = 0f,
    /** Distance on incline terrain (meters). */
    val inclineDistance: Float = 0f,
    /** Distance on decline terrain (meters). */
    val declineDistance: Float = 0f,
    /** Distance on flat terrain (meters). */
    val flatDistance: Float = 0f,
    /** Remaining distance to reach goal (meters). */
    val remainingDistance: Float = 0f,

    // -- Speed & Pace --
    /** Current speed (m/s). -1 if unavailable. */
    val speed: Float = -1f,
    /** Maximum speed recorded (m/s). */
    val maxSpeed: Float = -1f,
    /** Average speed over the session (m/s). */
    val averageSpeed: Float = -1f,
    /** Current pace (min/km). -1 if unavailable. */
    val pace: Float = -1f,
    /** Fastest (max) pace recorded (min/km). */
    val maxPace: Float = -1f,
    /** Average pace over the session (min/km). */
    val averagePace: Float = -1f,

    // -- Altitude & Elevation --
    /** Current altitude (meters). -1001 if unavailable. */
    val altitude: Float = ALTITUDE_INVALID,
    /** Maximum altitude recorded (meters). */
    val maxAltitude: Float = ALTITUDE_INVALID,
    /** Minimum altitude recorded (meters). */
    val minAltitude: Float = ALTITUDE_INVALID,
    /** Cumulative elevation gain (meters). */
    val cumulativeElevationGain: Float = 0f,
    /** Cumulative elevation loss (meters). */
    val cumulativeElevationLoss: Float = 0f,
    /** Current slope gradient. Float.MAX_VALUE if unavailable. */
    val slope: Float = Float.MAX_VALUE,
    /** Current incline value. -1 if unavailable. */
    val incline: Double = -1.0,

    // -- Calories --
    /** Calories burned so far. */
    val consumedCalorie: Float = 0f,
    /** Remaining calories to reach goal. */
    val remainingCalorie: Float = 0f,

    // -- Location --
    /** Current latitude. 200.0 indicates invalid/no GPS. */
    val latitude: Double = LOCATION_INVALID,
    /** Current longitude. 200.0 indicates invalid/no GPS. */
    val longitude: Double = LOCATION_INVALID,
    /** GPS accuracy in meters. */
    val gpsAccuracy: Float = 0f,

    // -- Steps & Cadence --
    /** Total step count. */
    val stepCount: Int = 0,
    /** Current step cadence (steps/min). -1 if unavailable. */
    val stepCadence: Float = -1f,
    /** Maximum step cadence recorded. */
    val maxStepCadence: Float = -1f,
    /** Average step cadence over the session. */
    val averageStepCadence: Float = -1f,

    // -- Strength Training --
    /** Current repetition count. */
    val repetition: Int = 0,
    /** Current set number. */
    val set: Int = 0,
    /** Interval number (default 1). */
    val interval: Int = 1
) {
    /** Whether the current GPS location is valid. */
    val isLocationValid: Boolean
        get() = latitude != LOCATION_INVALID && longitude != LOCATION_INVALID

    /** Whether altitude data is available. */
    val isAltitudeValid: Boolean
        get() = altitude != ALTITUDE_INVALID

    companion object {
        const val LOCATION_INVALID = 200.0
        const val ALTITUDE_INVALID = -1001.0f
    }
}