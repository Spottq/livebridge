package com.kakao.taxi.liveupdate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class GoogleNowBarTestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val builder = GoogleNowBarTestNotificationBuilder(context.applicationContext)
        when (intent.action) {
            ACTION_POST_SPORTS -> builder.postSports()
            ACTION_POST_FINANCE -> builder.postFinance()
            ACTION_CANCEL -> builder.cancelAll()
        }
    }

    companion object {
        const val ACTION_POST_SPORTS = "com.kakao.taxi.POST_GOOGLE_SPORTS_NOWBAR_TEST"
        const val ACTION_POST_FINANCE = "com.kakao.taxi.POST_GOOGLE_FINANCE_NOWBAR_TEST"
        const val ACTION_CANCEL = "com.kakao.taxi.CANCEL_GOOGLE_NOWBAR_TESTS"
    }
}
