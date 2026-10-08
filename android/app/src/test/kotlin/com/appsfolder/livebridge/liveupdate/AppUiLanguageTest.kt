package com.kakao.taxi.liveupdate

import org.junit.Assert.assertEquals
import org.junit.Test

class AppUiLanguageTest {
    @Test fun manualLanguageOverridesSystemForServicesAndActions() {
        assertEquals("es", AppUiLanguage.resolve("es", "en"))
        assertEquals("de", AppUiLanguage.resolve("de", "ru"))
    }
    @Test fun automaticLanguageUsesSystemAndRegionalTagsResolveToBaseLanguage() {
        assertEquals("es", AppUiLanguage.resolve("system", "es-MX"))
        assertEquals("de", AppUiLanguage.resolve("", "de-AT"))
        assertEquals("es", AppUiLanguage.resolve("ES_mx", "en"))
        assertEquals("ru", AppUiLanguage.resolve("ru", "de"))
    }
}
