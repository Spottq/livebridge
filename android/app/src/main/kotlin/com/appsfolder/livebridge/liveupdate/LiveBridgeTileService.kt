package com.kakao.taxi.liveupdate

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class LiveBridgeTileService : TileService() {
    private val prefs by lazy { ConverterPrefs(applicationContext) }

    override fun onStartListening() {
        super.onStartListening()
        if (isUnsupportedDevice()) {
            enforceUnsupportedState()
        }
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) {
            unlockAndRun { toggleConverter() }
        } else {
            toggleConverter()
        }
    }

    private fun toggleConverter() {
        if (isUnsupportedDevice()) {
            enforceUnsupportedState()
            updateTile()
            return
        }

        val newValue = !prefs.getConverterEnabled()
        prefs.setConverterEnabled(newValue)
        if (!newValue) {
            LiveUpdateNotifier.cancelAllMirrored(applicationContext)
        } else {
            requestNotificationListenerRebindIfPossible()
        }
        syncKeepAliveForegroundService()
        syncNetworkSpeedService()
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val unsupported = isUnsupportedDevice()
        val enabled = !unsupported && prefs.getConverterEnabled()
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "LiveBridge"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                unsupported -> {
                    if (isRussianLocale()) {
                        "\u0422\u043e\u043b\u044c\u043a\u043e Samsung"
                    } else {
                        "Samsung only"
                    }
                }
                enabled -> if (isRussianLocale()) "\u0412\u043a\u043b\u044e\u0447\u0435\u043d\u043e" else "Enabled"
                else -> if (isRussianLocale()) "\u0412\u044b\u043a\u043b\u044e\u0447\u0435\u043d\u043e" else "Disabled"
            }
        }
        tile.updateTile()
    }

    private fun syncKeepAliveForegroundService() {
        val shouldRun =
            prefs.getConverterEnabled() &&
                    prefs.getKeepAliveForegroundEnabled() &&
                    isNotificationListenerEnabled() &&
                    !DeviceBlocker.isBlockedDevice()
        if (shouldRun) {
            KeepAliveForegroundService.start(applicationContext)
        } else {
            KeepAliveForegroundService.stop(applicationContext)
        }
    }

    private fun syncNetworkSpeedService() {
        if (prefs.getNetworkSpeedEnabled() && !DeviceBlocker.isBlockedDevice()) {
            NetworkSpeedForegroundService.sync(applicationContext)
        } else {
            NetworkSpeedForegroundService.stop(applicationContext)
        }
    }

    private fun enforceUnsupportedState() {
        prefs.setConverterEnabled(false)
        LiveUpdateNotifier.cancelAllMirrored(applicationContext)
        KeepAliveForegroundService.stop(applicationContext)
        NetworkSpeedForegroundService.stop(applicationContext)
    }

    private fun isUnsupportedDevice(): Boolean {
        return DeviceBlocker.isBlockedDevice()
    }

    private fun requestNotificationListenerRebindIfPossible() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return
        }
        if (!isNotificationListenerEnabled()) {
            return
        }
        runCatching {
            NotificationListenerService.requestRebind(
                ComponentName(applicationContext, LiveUpdateNotificationListenerService::class.java)
            )
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val service = ComponentName(applicationContext, LiveUpdateNotificationListenerService::class.java)
        return enabled.split(":")
            .mapNotNull(ComponentName::unflattenFromString)
            .any { it == service }
    }

    private fun isRussianLocale(): Boolean {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale
        }
        return locale?.language?.startsWith("ru", ignoreCase = true) == true
    }

    companion object {
        fun requestStateSync(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                return
            }
            runCatching {
                requestListeningState(
                    context,
                    ComponentName(context, LiveBridgeTileService::class.java)
                )
            }
        }
    }
}
