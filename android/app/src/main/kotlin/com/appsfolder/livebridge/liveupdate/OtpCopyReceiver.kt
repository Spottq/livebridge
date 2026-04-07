package com.kakao.taxi.liveupdate

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import java.util.Locale

class OtpCopyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_COPY_OTP) {
            return
        }

        val rawCode = intent.getStringExtra(EXTRA_OTP_CODE).orEmpty()
        val code = rawCode.filter(Char::isDigit)
        if (code.isBlank()) {
            return
        }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("OTP", code))
        Toast.makeText(context, copiedToastText(context), Toast.LENGTH_SHORT).show()
    }

    private fun copiedToastText(context: Context): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        val language = locale?.language?.lowercase(Locale.ROOT).orEmpty()
        return if (language.startsWith("ru")) "Код скопирован" else "Code copied"
    }

    companion object {
        const val ACTION_COPY_OTP = "com.kakao.taxi.action.COPY_OTP"
        const val EXTRA_OTP_CODE = "otp_code"
    }
}
