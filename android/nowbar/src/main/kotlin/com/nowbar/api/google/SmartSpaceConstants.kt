package com.nowbar.api.google

/**
 * All discovered Google SmartSpace / Ambient Data constants extracted from
 * the Google APK (com.google.android.googlequicksearchbox).
 *
 * Source: decompiled/google/sources — classes matching SmartSpace, Ambient,
 * RobinKit, and related patterns.
 */
object SmartSpaceConstants {

    // ── Intent Actions ────────────────────────────────────────────────
    object Actions {
        /** Fired when a SmartSpace document is tapped by the user. */
        const val AMBIENT_DOCUMENT_CLICKED =
            "com.google.android.ambient.intent.action.AMBIENT_DOCUMENT_CLICKED"

        /** Fired when a SmartSpace document is dismissed by the user. */
        const val AMBIENT_DOCUMENT_DISMISSED =
            "com.google.android.ambient.intent.action.AMBIENT_DOCUMENT_DISMISSED"

        /** Fired when the user changes ambient data settings. */
        const val AMBIENT_DATA_SETTINGS_CHANGED =
            "com.google.android.ambient.intent.action.AMBIENT_DATA_SETTINGS_CHANGED"

        /** gRPC bind action used by SmartspaceServiceGrpcEndpointService. */
        const val GRPC_BIND = "grpc.io.action.BIND"
    }

    // ── Intent Extras ─────────────────────────────────────────────────
    object Extras {
        const val DOCUMENT_ID = "document_id"
        const val EVENT_ID = "event_id"
        const val GOOGLE_SRP_URL = "google_srp_url"
        const val SURFACE_TYPE =
            "com.google.android.apps.gsa.smartspace.EXTRA_SURFACE_TYPE"
    }

    // ── Permissions ───────────────────────────────────────────────────
    object Permissions {
        /** Required to register a SmartSpace client. signatureOrSystem level. */
        const val REGISTER_SMARTSPACE =
            "com.google.android.googlequicksearchbox.permission.REGISTER_SMARTSPACE"
    }

    // ── Feature Flags (PackageManager.hasSystemFeature) ───────────────
    object Features {
        /** Google Pixel ambient data feature. Checked by brch(0). */
        const val GOOGLE_AMBIENT_DATA = "com.google.android.feature.AMBIENT_DATA"

        /** OnePlus/Oplus ambient alerts feature. Checked by brch(1). */
        const val OPLUS_AMBIENT_ALERTS = "com.oplus.software.feature.ambient_alerts"

        /** Samsung Now Bar feature. Checked by brch(2). */
        const val SAMSUNG_NOWBAR = "com.samsung.feature.nowbar"
    }

    // ── Proto Package References ──────────────────────────────────────
    object Proto {
        const val SMARTSPACE_CARD_TYPE =
            "com.google.assistant.ambient.SmartspaceProto.SmartspaceUpdate.SmartspaceCard.CardType"

        const val CLIENT_TYPE =
            "com.google.assistant.discoverability.zerostate.ClientTypeEnumProto.ClientType"

        const val SMARTSPACE_UPDATE_SOURCE =
            "com.google.common.logging.OpaSmartspaceProto.SmartspaceEvent.SmartspaceUpdateSource"

        const val ROBIN_KIT_SERVICE =
            "com.google.android.apps.search.assistant.surfaces.voice.robin.robinkit.proto.RobinKitService"
    }

    // ── gRPC Services ─────────────────────────────────────────────────
    object GrpcServices {
        const val SMARTSPACE_SERVICE =
            "com.google.android.apps.search.assistant.verticals.ambient.grpc.impl.SmartspaceServiceGrpcEndpointService"

        const val ROBIN_KIT_SERVICE =
            "com.google.android.apps.search.assistant.surfaces.voice.robin.robinkit.service.RobinKitGrpcService"
    }

    // ── Content URIs ──────────────────────────────────────────────────
    object ContentUris {
        /** Now Playing ambient music state provider. */
        const val NOW_PLAYING_STATE =
            "content://com.google.intelligence.sense.ambientmusic.state_provider/now_playing_state"
    }

    // ── Logging/Streamz Paths ─────────────────────────────────────────
    object Streamz {
        const val SMARTSPACE_LOGGER_TASK =
            "com.google.android.apps.gsa.smartspace.task.LoggerTask"

        const val WEATHER_MISSING_LOCATION_PERMISSION =
            "/client_streamz/android_gsa/smartspace/weather/current_weather_update_missing_location_permission"
    }

    // ── Default Google SRP base URL ───────────────────────────────────
    const val GOOGLE_SRP_BASE_URL = "https://www.google.com"
}