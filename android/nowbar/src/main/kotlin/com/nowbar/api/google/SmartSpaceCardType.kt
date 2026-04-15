package com.nowbar.api.google

/**
 * Complete SmartSpace card type enum extracted from Google APK.
 *
 * Source: defpackage/wpf.java — SmartspaceProto.SmartspaceUpdate.SmartspaceCard.CardType
 * Proto: com.google.assistant.ambient.SmartspaceProto.SmartspaceUpdate.SmartspaceCard.CardType
 */
enum class SmartSpaceCardType(val id: Int) {
    DEFAULT(0),
    WEATHER(1),
    CALENDAR(2),
    COMMUTE_TIME(3),
    FLIGHT(4),
    TIPS(5),
    REMINDER(6),
    ALARM(7),
    ONBOARDING(8),
    SPORTS(9),
    WEATHER_ALERT(10),
    CONSENT(11),
    STOCK_PRICE_CHANGE(12),
    SHOPPING_LIST(13),
    LOYALTY_CARD(14),
    MEDIA_RECOMMENDATION(15),
    BEDTIME_ROUTINE(16),
    FITNESS_TRACKING(17),
    ETA_MONITORING(18),
    MISSED_CALL(19),
    PACKAGE_DELIVERY(20),
    TIMER(21),
    STOPWATCH(22),
    UPCOMING_ALARM(23),
    GAS_STATION_PAYMENT(24),
    PAIRED_DEVICE_STATUS(25),
    DRIVING_MODE(26),
    SLEEP_SUMMARY(27),
    FLASHLIGHT(28),
    TIME_TO_LEAVE(29),
    DOORBELL(30),
    MEDIA_RESUME(31),
    CROSS_DEVICE_TIMER(32),
    SEVERE_WEATHER_ALERT(33),
    HOLIDAY_ALARMS(34),
    SAFETY_CHECK(35),
    MEDIA_HEADS_UP(36),
    STEP_COUNTING(37),
    EARTHQUAKE_ALERT(38),
    DATE(39),
    BLAZE_BUILD_PROGRESS(40),
    MEDIA_CURRENT_PLAYING(41),
    EARTHQUAKE_OCCURRED(42),
    MEDIA_RESUME_SS_ACTIVATED(43),
    GROCERY_DELIVERY(44),
    GROCERY_PICKUP(45),
    AIR_QUALITY(46),
    PAIRED_DEVICE_LOW_BATTERY(47),
    FLIGHT_LANDING(48),
    HOTEL_CHECK_IN(49),
    HOTEL_CHECK_OUT(50),
    CALENDAR_NOTIFICATION(51),
    RIDESHARING_ETA(52),
    FOOD_DELIVERY_ETA(53),
    WEATHER_DAILY_FORECAST(54),
    WEATHER_TIMELY_REMINDER(55),
    WEATHER_DEFAULT(56),
    SHOPPING_MALL(57),
    MEDIA_RECS_DRIVING(58),
    RING(59),
    WELLBEING_BEDTIME(60),
    WEATHER_TOMORROW_FORECAST(61),
    CROSS_DEVICE_ALARM(62),
    INTERCITY_TRAIN(63),
    TRAIN_SEAT(64),
    TRAIN_DESTINATION_ALERT(65),
    LOUD_SOUND_ALERT(66),
    EVENT_RESERVATION(67),
    AIRPORT(68),
    TRANSIT_STATION(69),
    WALLET_SUGGESTIONS(70),
    WALLET_BOARDING_PASS(71),
    COMMUTE_TIME_AMBIENT(72),
    SUBZERO(73),
    MY_PIXEL(74),
    SEMANTIC_LOCATION_RESTAURANT(75),
    SEMANTIC_LOCATION_WALLET_PASS(76);

    companion object {
        private val byId = entries.associateBy { it.id }
        fun fromId(id: Int): SmartSpaceCardType? = byId[id]
    }
}

/**
 * SmartSpace surface type — where the card is being displayed.
 *
 * Source: defpackage/eztb.java
 */
enum class SmartSpaceSurfaceType(val id: Int) {
    UNKNOWN_SURFACE(0),
    SYSTEM_UI(1),
    AMBIENT(2),
    OEM_SYSTEM_UI(3),
    WIDGET(4),
    OEM_PREINSTALLED_WIDGET(5),
    BLUECHIP_SMARTSPACE(6);

    companion object {
        private val byId = entries.associateBy { it.id }
        fun fromId(id: Int): SmartSpaceSurfaceType? = byId[id]
    }
}

/**
 * Widget types used in Google's SmartSpace widget registry.
 *
 * Source: defpackage/ckkt.java — full enum of registered widget types.
 */
enum class SmartSpaceWidgetType(val ordinalId: Int, val displayName: String) {
    ARTIST_OF_THE_DAY(0, "GAC: Artist of the day"),
    BATTERY(1, "Battery"),
    BARD_ON_PIXEL(2, "Bard on Pixel"),
    BETTERBUG_BUG_REPORT_SHORTCUT(3, "BetterBug - Bug report shortcut"),
    CALENDAR_MONTH(4, "Calendar - Month"),
    CALENDAR_MONTH_GM3(5, "Calendar - Month GM3"),
    CALENDAR_MONTH_V3_2025(6, "Calendar - Month v3_2025"),
    CALENDAR_SCHEDULE(7, "Calendar - Schedule"),
    CALENDAR_SCHEDULE_GM3(8, "Calendar - Schedule GM3"),
    CALENDAR_SCHEDULE_V3_2025(9, "Calendar - Schedule v3_2025"),
    CONTACTS_BESTIES(10, "Contacts - Besties"),
    CONTACTS_FAVORITE_CONTACTS_GRID(11, "Contacts - Favorite Contacts Grid"),
    CONTACTS_SINGLE_CONTACT(12, "Contacts - Single Contact"),
    DEMO_SIMPLE_LIST(13, "Demo Simple List"),
    DEMO_ANALOG_CLOCK(14, "Demo Analog Clock"),
    DEMO_TOOLBAR(15, "Demo Toolbar"),
    DRIVE_QUICK_ACTION_TOOLBAR(16, "Drive Quick Action Toolbar"),
    DRIVE_SUGGESTIONS(17, "Drive Suggestions"),
    FI_USAGE(18, "Fi - Usage"),
    FI_USAGE_LEGACY(19, "Fi - Usage (Legacy)"),
    GEMINI(20, "Gemini"),
    GMAIL_INBOX_MATERIAL_YOU(21, "Gmail Inbox: Material You"),
    GMAIL_INBOX_LEGACY(22, "Gmail Inbox: Legacy"),
    GOOGLE_APP_SEARCH_WIDGET(23, "AGA Search Widget"),
    GOOGLE_TV_TOP_PICKS(24, "Google TV - Top Picks"),
    KEEP_NOTES_GM3(25, "Keep Notes GM3"),
    KEEP_NOTES_LEGACY(26, "Keep Notes Legacy"),
    KEEP_SINGLE_NOTE(27, "Keep Single Note"),
    KEEP_TOOLBAR_GM3(28, "Keep Toolbar GM3"),
    KEEP_TOOLBAR_LEGACY(29, "Keep Toolbar Legacy"),
    MAPS_SEARCH_WIDGET(30, "Maps Search Widget"),
    MAPS_TRAFFIC_WIDGET(31, "Maps Nearby Traffic"),
    NEWS_LEGACY_WIDGET(32, "News - Legacy"),
    NEWS_GLANCE_WIDGET(33, "News - Glance"),
    PHOTOS_MEMORIES(34, "Photos - Memories"),
    PHOTOS_PEOPLE_AND_PETS(35, "Photos - People & Pets"),
    PIXEL_WEATHER_CURRENT_FORECAST(36, "Pixel Weather - Current Forecast"),
    PIXEL_WEATHER_FULL_FORECAST(37, "Pixel Weather - Full Forecast"),
    PLAY_BOOKS_LEGACY(38, "PlayBooks: Legacy"),
    PLAY_BOOKS_MY_BOOKS(39, "PlayBooks: MyBooks"),
    PLAY_BOOKS_READING_PROGRESS(40, "PlayBooks: Reading Progress"),
    SMARTSPACE(41, "Ambient Assistant: At a Glance"),
    STOCKS(42, "Stocks"),
    TRANSLATE_CLIPBOARD(43, "Translate Clipboard"),
    TRANSLATE_DICTATION(44, "Translate Dictation"),
    TRANSLATE_LENS(45, "Translate Lens"),
    TRANSLATE_OPEN_MIC(46, "Translate Open Mic"),
    TRANSLATE_SPEAK_EASY(47, "Translate Speak Easy"),
    TRANSLATE_QUICK_ACTIONS(48, "Translate Quick Actions"),
    TRANSLATE_SAVED_HISTORY(49, "Translate Saved History"),
    TRANSLATE_TEXT_TRANSLATION(50, "Translate Text Translation"),
    TEST(51, "Test widget"),
    WEATHER(52, "Weather"),
    WEATHER_FREEFORM(53, "Weather - Freeform"),
    YTM_FLIP(54, "YTM Flip"),
    YTM_FREEFORM(55, "YTM Freeform"),
    YTM_NOW_PLAYING(56, "YTM Now Playing"),
    YTM_NOW_PLAYING_LEGACY(57, "YTM Now Playing Legacy"),
    FL_LOCK_UNLOCK(58, "Family Link Lock Unlock"),
    YT_MAIN_QUICK_ACTIONS(59, "YouTube - Quick Actions"),
    YT_MAIN_SEARCH(60, "YouTube - Search");

    companion object {
        private val byId = entries.associateBy { it.ordinalId }
        fun fromId(id: Int): SmartSpaceWidgetType? = byId[id]
    }
}