package com.nowbar.api.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat

/**
 * Singleton helper for starting and stopping [NowBarForegroundService] implementations
 * from outside the service (e.g., from an Activity or BroadcastReceiver).
 *
 * Uses [ContextCompat.startForegroundService] to ensure correct behavior on
 * Android 8.0 (API 26+) where foreground services must call [android.app.Service.startForeground]
 * within 5 seconds.
 *
 * ```kotlin
 * // Start the service
 * ServiceLifecycleManager.startService(context, TimerNowBarService::class.java)
 *
 * // Start with extras
 * val extras = bundleOf("duration" to 60_000L)
 * ServiceLifecycleManager.startService(context, TimerNowBarService::class.java, extras)
 *
 * // Stop the service
 * ServiceLifecycleManager.stopService(context, TimerNowBarService::class.java)
 * ```
 */
object ServiceLifecycleManager {

    /**
     * Starts the specified [NowBarForegroundService] as a foreground service.
     *
     * @param T The concrete service type extending [NowBarForegroundService].
     * @param context Application or activity context.
     * @param serviceClass The service class to start.
     * @param extras Optional [Bundle] of extras to pass to the service intent.
     */
    fun <T : NowBarForegroundService> startService(
        context: Context,
        serviceClass: Class<T>,
        extras: Bundle? = null
    ) {
        val intent = Intent(context, serviceClass)
        extras?.let { intent.putExtras(it) }
        ContextCompat.startForegroundService(context, intent)
    }

    /**
     * Stops the specified [NowBarForegroundService].
     *
     * @param T The concrete service type extending [NowBarForegroundService].
     * @param context Application or activity context.
     * @param serviceClass The service class to stop.
     */
    fun <T : NowBarForegroundService> stopService(
        context: Context,
        serviceClass: Class<T>
    ) {
        val intent = Intent(context, serviceClass)
        context.stopService(intent)
    }
}