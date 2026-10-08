package com.kakao.taxi.liveupdate

internal fun channelAllowed(blocked: Map<String, Set<String>>, packageName: String, channelId: String?): Boolean =
    channelId == null || channelId !in blocked[packageName].orEmpty()
