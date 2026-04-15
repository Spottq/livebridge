package com.nowbar.api.types

/**
 * Exercise tracking statuses used by Samsung Health live tracker.
 * Values map directly to Samsung Health internal tracking status codes.
 */
enum class TrackingStatus(val code: Int) {
    /** Exercise stopped / not tracking. */
    STOPPED(0),

    /** Exercise is actively tracking. */
    RUNNING(1),

    /** Exercise is paused (manual or auto-pause). */
    PAUSED(2),

    /** Delayed start / countdown in progress. */
    PREPARING(3);

    companion object {
        private val codeMap = entries.associateBy { it.code }

        fun fromCode(code: Int): TrackingStatus? = codeMap[code]

        fun fromCodeOrStopped(code: Int): TrackingStatus = codeMap[code] ?: STOPPED
    }
}

/**
 * Reasons for tracking status changes.
 * These codes are used internally by Samsung Health to record why the status changed.
 */
enum class TrackingStatusChangeReason(val code: Int) {
    /** User-initiated action from UI (pause, stop, resume). */
    USER_ACTION(9000),

    /** Stopped because maximum exercise duration was reached. */
    MAX_DURATION(9001),

    /** GPS signal lost or degraded. */
    GPS_SIGNAL_LOST(9002),

    /** App crash recovery — tracker restarted after crash. */
    CRASH_RECOVERY(9004),

    /** Auto-pause triggered by low speed/no movement. */
    AUTO_PAUSE(9006),

    /** Non-crash restart — tracker finalized and restarted. */
    NON_CRASH_RESTART(9008),

    /** System-level event triggered the change. */
    SYSTEM_EVENT(9010),

    /** Internal stop during finalization. */
    INTERNAL_STOP(9011),

    /** Lock screen triggered auto-pause. */
    LOCK_SCREEN(9012),

    /** Reconnected from remote tracker (Galaxy Watch). */
    REMOTE_RECONNECT(9014),

    /** Disconnected from remote tracker (Galaxy Watch). */
    REMOTE_DISCONNECT(9015),

    /** Internal alias for USER_ACTION in resume path. */
    ALIAS_USER_ACTION(9016),

    /** GPS-related status change. */
    GPS_RELATED(9019);

    companion object {
        private val codeMap = entries.associateBy { it.code }

        fun fromCode(code: Int): TrackingStatusChangeReason? = codeMap[code]
    }
}