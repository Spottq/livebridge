package com.kakao.taxi.liveupdate

import org.junit.Assert.*
import org.junit.Test

class SourceChannelPolicyTest {
    private val blocked = mapOf("com.example.first" to setOf("Ads", "offers"))

    @Test fun excludesOnlyTheSelectedChannelInTheSelectedApp() {
        assertFalse(channelAllowed(blocked, "com.example.first", "Ads"))
        assertTrue(channelAllowed(blocked, "com.example.first", "messages"))
        assertTrue(channelAllowed(blocked, "com.example.second", "Ads"))
    }

    @Test fun channelIdsAreCaseSensitiveAndUnknownChannelsRemainAllowed() {
        assertTrue(channelAllowed(blocked, "com.example.first", "ads"))
        assertTrue(channelAllowed(blocked, "com.example.first", null))
        assertTrue(channelAllowed(emptyMap(), "com.example.first", "Ads"))
    }
}
