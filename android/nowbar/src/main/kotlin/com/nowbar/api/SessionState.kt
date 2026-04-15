package com.nowbar.api

/**
 * Lifecycle state of a [NowBarSession].
 */
enum class SessionState {
    /** Session created but not yet started. */
    IDLE,

    /** Session is actively displaying a Now Bar card. */
    ACTIVE,

    /** Session is temporarily paused (e.g. app backgrounded). */
    PAUSED,

    /** Session has been stopped and cannot be restarted. */
    STOPPED
}