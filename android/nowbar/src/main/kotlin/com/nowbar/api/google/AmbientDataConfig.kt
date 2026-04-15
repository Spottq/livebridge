package com.nowbar.api.google

/**
 * Configuration for Google Ambient Data integration.
 *
 * Covers AmbientDataDocument types, their built-in type candidates,
 * and the feature detection logic extracted from the Google APK.
 *
 * Source: com.google.android.ambient.app.backend.documents.*
 */
object AmbientDataConfig {

    // ── AmbientDataDocument Interface ─────────────────────────────────
    // All implementations share these common fields:
    //   a() -> creationTimestamp: Long
    //   b() -> documentTtlMillis: Long
    //   c() -> dismissAction: PotentialAction
    //   d() -> seenAction: PotentialAction (nullable)
    //   e() -> tapAction: PotentialAction
    //   f() -> ambientRankingMetaData: AmbientRankingMetaDataDocument
    //   g() -> builtInType: ThingDocument
    //   h() -> id: String
    //   i() -> namespace: String
    //   j() -> notificationDedupeId: String

    /**
     * All AmbientDataDocument implementations discovered in the APK.
     *
     * Source: com.google.android.ambient.app.backend.documents/
     */
    enum class DocumentType(val className: String) {
        COMMUTE("CommuteAmbientDataDocument"),
        EVENT("EventAmbientDataDocument"),
        INVESTMENT_RECAP("InvestmentRecapAmbientDataDocument"),
        MY_PIXEL("MyPixelAmbientDataDocument"),
        SEMANTIC_LOCATION("SemanticLocationAmbientDataDocument"),
        SPORTS_SCORE("SportsScoreAmbientDataDocument"),
        THING("ThingAmbientDataDocument"),
        TYPED_THING("TypedThingAmbientDataDocument");

        val fullClassName: String
            get() = "com.google.android.ambient.app.backend.documents.$className"
    }

    /**
     * Built-in type candidate documents used as payload inside AmbientDataDocuments.
     *
     * Source: com.google.android.ambient.app.backend.documents.builtintypecandidates/
     */
    enum class BuiltInTypeCandidate(val className: String) {
        COMMUTE("CommuteDocument"),
        EVENT("EventDocument"),
        IMPORTANT_DURATION("ImportantDurationDocument"),
        INVESTMENT("InvestmentDocument"),
        INVESTMENT_MARKET("InvestmentMarketDocument"),
        INVESTMENT_PRICE_CHANGE("InvestmentPriceChangeDocument"),
        INVESTMENT_RECAP("InvestmentRecapDocument"),
        MY_PIXEL("MyPixelDocument"),
        PLACE("PlaceDocument"),
        SEMANTIC_LOCATION("SemanticLocationDocument"),
        SPORTS_EVENT("SportsEventDocument"),
        SPORTS_TEAM("SportsTeamDocument"),
        THING("ThingDocument"),
        TRIP("TripDocument"),
        TRIP_CONDITION("TripConditionDocument"),
        TYPED_THING("TypedThingDocument");

        val fullClassName: String
            get() = "com.google.android.ambient.app.backend.documents.builtintypecandidates.$className"
    }

    // ── Feature Detection ─────────────────────────────────────────────
    /**
     * OEM platform type detected via PackageManager.hasSystemFeature().
     *
     * Source: defpackage/brch.java
     *   brch(0) — Google Pixel (AMBIENT_DATA)
     *   brch(1) — OnePlus/Oplus (ambient_alerts)
     *   brch(2) — Samsung (nowbar)
     *
     * Additional check for Google Pixel: SDK_INT >= 35 (Android 15+)
     */
    enum class AmbientPlatform(
        val featureFlag: String,
        val internalId: Int
    ) {
        GOOGLE_PIXEL(
            featureFlag = "com.google.android.feature.AMBIENT_DATA",
            internalId = 0
        ),
        OPLUS(
            featureFlag = "com.oplus.software.feature.ambient_alerts",
            internalId = 1
        ),
        SAMSUNG(
            featureFlag = "com.samsung.feature.nowbar",
            internalId = 2
        );
    }

    // ── Now Playing Integration ───────────────────────────────────────
    object NowPlaying {
        /** Content URI for ambient music "Now Playing" state. */
        const val STATE_CONTENT_URI =
            "content://com.google.intelligence.sense.ambientmusic.state_provider/now_playing_state"

        /** Internal event type for Now Playing queries. Source: defpackage/dmkf.java */
        const val QUERY_EVENT_ID = 103

        /** Lockscreen Now Playing surface. Source: defpackage/tuv.java */
        const val LOCKSCREEN_SURFACE_ID = 55

        /** Now Playing feature ID. Source: defpackage/fics.java */
        const val FEATURE_ID = 281

        /** Sound search tag for Now Playing. Source: defpackage/ebgw.java */
        const val SOUND_SEARCH_TAG_ID = 990
    }

    // ── YTM (YouTube Music) Integration ───────────────────────────────
    object YouTubeMusic {
        /** Widget type ID for YTM Now Playing. Source: defpackage/ckkt.java */
        const val NOW_PLAYING_WIDGET_ID = 56

        /** Widget type ID for YTM Now Playing Legacy. Source: defpackage/ckkt.java */
        const val NOW_PLAYING_LEGACY_WIDGET_ID = 57

        /** Widget type ID for YTM Flip. */
        const val FLIP_WIDGET_ID = 54

        /** Widget type ID for YTM Freeform. */
        const val FREEFORM_WIDGET_ID = 55
    }

    // ── Cross-Profile SmartSpace ──────────────────────────────────────
    object CrossProfile {
        /** Bundler class for cross-profile SmartSpace data. */
        const val BUNDLER_CLASS =
            "com.google.android.apps.search.assistant.verticals.ambient.crossprofile.SmartspaceCrossProfileManager_Bundler"

        /** Enum types serialized through the cross-profile bundler. */
        val SERIALIZED_ENUM_TYPES = listOf(
            "com.google.assistant.discoverability.zerostate.ClientTypeEnumProto.ClientType",
            "com.google.assistant.ambient.SmartspaceProto.SmartspaceUpdate.SmartspaceCard.CardType",
            "com.google.common.logging.OpaSmartspaceProto.SmartspaceEvent.SmartspaceUpdateSource"
        )
    }

    // ── Sensitive Notification Handling ────────────────────────────────
    object SensitiveNotification {
        /** Internal card display type. Source: defpackage/duwd.java */
        const val DISPLAY_TYPE_NAME = "SENSITIVE_NOTIFICATION"
        const val DISPLAY_TYPE_ID = 12
        const val PRIORITY = 4

        /** Feature ID. Source: defpackage/fics.java */
        const val FEATURE_ID = 304
    }
}