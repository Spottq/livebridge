package com.kakao.taxi.liveupdate

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.os.Process
import android.util.Log

/** Source events and animation frames share one queue, preserving their order off the UI thread. */
internal object NotificationProcessing {
    // Preserve source history across a listener rebind in the same process.
    val sourceLifecycle = SourceNotificationLifecycle()
    val originalRemovalPolicy = OriginalRemovalPolicy()
    val handler: Handler by lazy {
        val thread = HandlerThread("LiveBridge-notifications", Process.THREAD_PRIORITY_BACKGROUND)
        thread.start()
        object : Handler(thread.looper) {
            override fun dispatchMessage(msg: Message) {
                try {
                    super.dispatchMessage(msg)
                } catch (error: Exception) {
                    // A failed OEM action/frame must not kill the shared notification queue.
                    Log.e("NotificationProcessing", "Notification task failed", error)
                }
            }
        }
    }
}
