package com.nowbar.api

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.nowbar.api.cards.NowBarCard
import com.nowbar.api.fallback.FallbackStrategyResolver
import com.nowbar.api.notification.NowBarNotificationBuilder
import com.nowbar.api.notification.SafeNotificationPoster
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

internal class NowBarSessionImpl(
    private val context: Context,
    private val config: NowBarConfig
) : NowBarSession {

    override val id: String = UUID.randomUUID().toString()

    private val _state = MutableStateFlow(SessionState.IDLE)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    private val notificationManager = NotificationManagerCompat.from(context)
    private val notificationBuilder = NowBarNotificationBuilder(context, config)
    private val lock = Any()

    private var lastCard: NowBarCard? = null

    override fun start(card: NowBarCard) = synchronized(lock) {
        check(_state.value != SessionState.STOPPED) {
            "Session is already stopped."
        }

        lastCard = card
        post(card, config.samsungStyle, config.requestPromotedOngoing)
        _state.value = SessionState.ACTIVE
    }

    override fun update(card: NowBarCard) = synchronized(lock) {
        check(_state.value == SessionState.ACTIVE || _state.value == SessionState.PAUSED) {
            "Session must be ACTIVE or PAUSED before update()."
        }

        lastCard = card
        post(
            card = card,
            samsungStyle = currentSamsungStyle(),
            requestPromotedOngoing = currentPromotedState()
        )
    }

    override fun dismiss() = synchronized(lock) {
        check(_state.value == SessionState.ACTIVE || _state.value == SessionState.PAUSED) {
            "Session must be ACTIVE or PAUSED before dismiss()."
        }

        val card = lastCard ?: return
        post(
            card = card,
            samsungStyle = NowBarConfig.STYLE_NOTIFICATION_ONLY,
            requestPromotedOngoing = false
        )
        _state.value = SessionState.PAUSED
    }

    override fun stop() = synchronized(lock) {
        if (_state.value == SessionState.STOPPED) return

        notificationManager.cancel(config.notificationId)
        _state.value = SessionState.STOPPED
    }

    private fun currentSamsungStyle(): Int {
        return if (_state.value == SessionState.PAUSED) {
            NowBarConfig.STYLE_NOTIFICATION_ONLY
        } else {
            config.samsungStyle
        }
    }

    private fun currentPromotedState(): Boolean =
        _state.value != SessionState.PAUSED && config.requestPromotedOngoing

    private fun post(
        card: NowBarCard,
        samsungStyle: Int,
        requestPromotedOngoing: Boolean
    ) {
        val fallback = FallbackStrategyResolver.resolve(
            strategy = config.fallbackStrategy,
            nativeSurfaceSupported = FeatureDetector.isNativeSurfaceSupported(context)
        )

        if (!fallback.shouldPost) {
            return
        }

        SafeNotificationPoster.notify(
            context = context,
            notificationManager = notificationManager,
            notificationId = config.notificationId,
            notification = notificationBuilder.build(card, samsungStyle, requestPromotedOngoing)
        )
    }
}
