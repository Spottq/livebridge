package com.nowbar.api.google

/**
 * RobinKit automation types and integration configuration extracted from Google APK.
 *
 * RobinKit is Google's framework for third-party assistant integrations.
 * Samsung Now Bar uses ROBIN_KIT_SAMSUNG_NOW_BAR_AUTOMATION(67) entry point
 * with SAMSUNG_NOW_BAR_AUTOMATION(10) as the RobinKit feature type.
 */
object RobinKitIntegration {

    // ── Entry Point Types (eucd.java / feti.java) ─────────────────────
    /**
     * Assistant entry point types — how the assistant session is triggered.
     * Only RobinKit-related entries are listed; full enum has 70+ values.
     *
     * Source: defpackage/eucd.java (proto enum, also mirrored in feti.java)
     */
    enum class EntryPointType(val id: Int) {
        ROBIN_KIT(33),
        ROBIN_KIT_MA(46),
        ROBIN_KIT_PIXEL_SUBZERO(47),
        ROBIN_KIT_SMART_SUGGESTIONS(50),
        ROBIN_KIT_ACTION_BLOCKS(53),
        ROBIN_KIT_SAMSUNG_IMAGEGEN(56),
        ROBIN_KIT_GOOGLE_MESSAGES(63),
        ROBIN_KIT_MAGIC_ACTIONS_AUTOMATION(66),
        ROBIN_KIT_SAMSUNG_NOW_BAR_AUTOMATION(67),
        ROBIN_KIT_MAGIC_POINTER(69);

        companion object {
            private val byId = entries.associateBy { it.id }
            fun fromId(id: Int): EntryPointType? = byId[id]

            /** The entry point used for Samsung Now Bar automation. */
            val SAMSUNG_NOW_BAR = ROBIN_KIT_SAMSUNG_NOW_BAR_AUTOMATION
        }
    }

    // ── RobinKit Feature Types (eusa.java) ────────────────────────────
    /**
     * RobinKit feature type — identifies the capability/surface using RobinKit.
     *
     * Source: defpackage/eusa.java
     */
    enum class FeatureType(val id: Int) {
        UNSPECIFIED(0),
        USER_GENERATED(1),
        PIXEL_SUBZERO(2),
        PIXEL_MA(3),
        SMART_SUGGESTIONS(4),
        ACTION_BLOCKS(5),
        SAMSUNG_IMAGE_GEN(6),
        PERSONALIZATION_SUGGESTIONS(7),
        GOOGLE_MESSAGES(8),
        MAGIC_ACTIONS_AUTOMATION(9),
        SAMSUNG_NOW_BAR_AUTOMATION(10),
        CLICKABLE_SUGGESTIONS(11),
        MAGIC_POINTER(12);

        companion object {
            private val byId = entries.associateBy { it.id }
            fun fromId(id: Int): FeatureType? = byId[id]

            /** The feature type for Samsung Now Bar. */
            val SAMSUNG_NOW_BAR = SAMSUNG_NOW_BAR_AUTOMATION
        }
    }

    // ── RobinKit API Status (fexh.java) ───────────────────────────────
    /**
     * RobinKit API availability status returned by the availability check.
     *
     * Source: defpackage/fexh.java
     */
    enum class ApiStatus(val id: Int) {
        ROBIN_KIT_API_STATUS_UNKNOWN(0),
        ROBIN_KIT_AVAILABLE(1),
        WORK_PROFILE_NOT_SUPPORTED(2),
        ONBOARDING_NOT_COMPLETED(3),
        MULTIPLE_ACCOUNTS_NOT_SUPPORTED(4),
        ACCOUNT_MISMATCH(5),
        NO_CAPABILITIES_FOUND(6),
        ROBIN_KIT_NOT_ENABLED(7),
        ROBIN_KIT_AVAILABLE_SUMMARIZE_URL_AND_ANALYZE_ATTACHMENT(8),
        ROBIN_KIT_AVAILABLE_SUMMARIZE_URL(9),
        ROBIN_KIT_AVAILABLE_ANALYZE_ATTACHMENT(10),
        ROBIN_KIT_ENTRY_POINT_SERVICE_ERROR(11),
        ROBIN_KIT_LAUNCH_SUCCESS(12),
        ENTRY_POINT_REQUEST_NULL(13),
        ENTRY_POINT_REQUEST_NOT_SUCCESS(14),
        CAPABILITY_PROVIDER_NOT_FOUND(15),
        AGSA_NOT_DEFAULT_ASSISTANT(16),
        ROBIN_MINOR_USER(17);

        val isAvailable: Boolean
            get() = this == ROBIN_KIT_AVAILABLE ||
                this == ROBIN_KIT_AVAILABLE_SUMMARIZE_URL_AND_ANALYZE_ATTACHMENT ||
                this == ROBIN_KIT_AVAILABLE_SUMMARIZE_URL ||
                this == ROBIN_KIT_AVAILABLE_ANALYZE_ATTACHMENT ||
                this == ROBIN_KIT_LAUNCH_SUCCESS

        companion object {
            private val byId = entries.associateBy { it.id }
            fun fromId(id: Int): ApiStatus? = byId[id]
        }
    }

    // ── RobinKit Request Type (fexg.java) ─────────────────────────────
    /**
     * Type of RobinKit request being made.
     *
     * Source: defpackage/fexg.java
     */
    enum class RequestType(val id: Int) {
        UNSPECIFIED(0),
        AVAILABILITY(1),
        ROBIN_KIT_LAUNCH(2);

        companion object {
            private val byId = entries.associateBy { it.id }
            fun fromId(id: Int): RequestType? = byId[id]
        }
    }

    // ── Session Reason (afzv.java / agbs.java) ────────────────────────
    /**
     * Reason for creating an assistant session, RobinKit-related entry.
     *
     * Source: defpackage/afzv.java (value 22), defpackage/agbs.java (value 24)
     * Two different enum versions exist in the APK with different ordinals.
     */
    object SessionReason {
        const val REASON_ROBIN_KIT_REQUEST_V1 = 22 // afzv.java
        const val REASON_ROBIN_KIT_REQUEST_V2 = 24 // agbs.java
    }

    // ── RobinKit Logging Events (dmjw.java) ───────────────────────────
    /**
     * RobinKit-related logging event names for analytics.
     *
     * Source: defpackage/dmjw.java
     */
    object LogEvents {
        const val AVAILABILITY_REQUEST_STARTED = "ROBIN_KIT_AVAILABILITY_REQUEST_STARTED"
        const val LAUNCH_REQUEST_STARTED = "ROBIN_KIT_LAUNCH_REQUEST_STARTED"
        const val AVAILABILITY_REQUEST_END = "ROBIN_KIT_AVAILABILITY_REQUEST_END"
        const val LAUNCH_REQUEST_END = "ROBIN_KIT_LAUNCH_REQUEST_END"
        const val CAPABILITY_PROVIDER_EXECUTION_STATUS = "ROBIN_KIT_CAPABILITY_PROVIDER_EXECUTION_STATUS"
        const val FLOATY_REQUEST_EDITED = "ROBIN_KIT_FLOATY_REQUEST_EDITED"
        const val FLOATY_REQUEST_RECEIVED = "ROBIN_KIT_FLOATY_REQUEST_RECEIVED"
        const val FLOATY_REQUEST_ENDED = "ROBIN_KIT_FLOATY_REQUEST_ENDED"
        const val FLOATY_REQUEST_SUBMITTED = "ROBIN_KIT_FLOATY_REQUEST_SUBMITTED"
        const val FULLSCREEN_REQUEST_EDITED = "ROBIN_KIT_FULLSCREEN_REQUEST_EDITED"
        const val FULLSCREEN_REQUEST_RECEIVED = "ROBIN_KIT_FULLSCREEN_REQUEST_RECEIVED"
        const val FULLSCREEN_REQUEST_SUBMITTED = "ROBIN_KIT_FULLSCREEN_REQUEST_SUBMITTED"
        const val GET_CURRENT_ACCOUNT_REQUEST_STARTED = "ROBIN_KIT_GET_CURRENT_ACCOUNT_REQUEST_STARTED"
        const val GET_CURRENT_ACCOUNT_REQUEST_END = "ROBIN_KIT_GET_CURRENT_ACCOUNT_REQUEST_END"
        const val APPFLOW = "ROBIN_KIT_APPFLOW"
    }

    // ── gRPC Service Configuration ────────────────────────────────────
    object GrpcConfig {
        const val SERVICE_CLASS =
            "com.google.android.apps.search.assistant.surfaces.voice.robin.robinkit.service.RobinKitGrpcService"

        const val PROTO_SERVICE =
            "com.google.android.apps.search.assistant.surfaces.voice.robin.robinkit.proto.RobinKitService"

        const val LOG_TAG = "Ma-Robin-RobinKit"
    }

    // ── Feature Flag Mapping ──────────────────────────────────────────
    /**
     * Maps the RobinKit entry point type to the corresponding feature type.
     */
    val ENTRY_POINT_TO_FEATURE: Map<EntryPointType, FeatureType> = mapOf(
        EntryPointType.ROBIN_KIT to FeatureType.UNSPECIFIED,
        EntryPointType.ROBIN_KIT_MA to FeatureType.PIXEL_MA,
        EntryPointType.ROBIN_KIT_PIXEL_SUBZERO to FeatureType.PIXEL_SUBZERO,
        EntryPointType.ROBIN_KIT_SMART_SUGGESTIONS to FeatureType.SMART_SUGGESTIONS,
        EntryPointType.ROBIN_KIT_ACTION_BLOCKS to FeatureType.ACTION_BLOCKS,
        EntryPointType.ROBIN_KIT_SAMSUNG_IMAGEGEN to FeatureType.SAMSUNG_IMAGE_GEN,
        EntryPointType.ROBIN_KIT_GOOGLE_MESSAGES to FeatureType.GOOGLE_MESSAGES,
        EntryPointType.ROBIN_KIT_MAGIC_ACTIONS_AUTOMATION to FeatureType.MAGIC_ACTIONS_AUTOMATION,
        EntryPointType.ROBIN_KIT_SAMSUNG_NOW_BAR_AUTOMATION to FeatureType.SAMSUNG_NOW_BAR_AUTOMATION,
        EntryPointType.ROBIN_KIT_MAGIC_POINTER to FeatureType.MAGIC_POINTER,
    )

    // ── Appflow Feature (dmjx.java) ───────────────────────────────────
    /** RobinKit Appflow feature ID. Source: defpackage/dmjx.java */
    const val APPFLOW_FEATURE_ID = 164

    // ── Surface Type (tuv.java) ───────────────────────────────────────
    /** RobinKit surface type in the surface registry. Source: defpackage/tuv.java */
    const val SURFACE_TYPE_ID = 191

    // ── Invocation Source (afvl.java) ─────────────────────────────────
    /** Invocation source ordinal for RobinKit. Source: defpackage/afvl.java */
    const val INVOCATION_SOURCE_ORDINAL = 17
}