package com.kakao.taxi.liveupdate

import org.junit.Assert.*
import org.junit.Test
import java.nio.charset.StandardCharsets.UTF_8
import kotlin.random.Random

class PerformancePolicyTest {
    @Test fun staticSnapshotsSkipButNewPostsAndRecoveryRefresh() {
        val policy = SnapshotRefreshPolicy()
        assertTrue(policy.needsRefresh("a", 10, 0, false))
        policy.record("a", 10, 0)
        for (now in 4_000L until 60_000L step 4_000L) {
            assertFalse(policy.needsRefresh("a", 10, now, false))
        }
        assertTrue(policy.needsRefresh("a", 11, 4_000, false))
        assertTrue(policy.needsRefresh("a", 10, 60_000, false))
    }

    @Test fun mediaStillRefreshesEveryTick() {
        val policy = SnapshotRefreshPolicy()
        for (now in 0L..60_000L step 4_000L) {
            assertTrue(policy.needsRefresh("media", 10, now, true))
            policy.record("media", 10, now)
        }
    }

    @Test fun settingsDndDisconnectAndRemovalsInvalidateStamps() {
        val policy = SnapshotRefreshPolicy()
        policy.record("a", 10, 0)
        policy.invalidate()
        assertTrue(policy.needsRefresh("a", 10, 1, false))
        policy.record("a", 10, 1)
        policy.remove("a")
        assertTrue(policy.needsRefresh("a", 10, 2, false))
        policy.record("a", 10, 2)
        policy.retain(setOf("b"))
        assertTrue(policy.needsRefresh("a", 10, 3, false))
    }

    @Test fun staticWorkDropsFifteenFoldWithoutChangingMediaTicks() {
        val policy = SnapshotRefreshPolicy()
        var staticBuilds = 0
        var mediaBuilds = 0
        for (now in 0L until 240_000L step 4_000L) {
            for (index in 0 until 100) {
                val key = "static-$index"
                if (policy.needsRefresh(key, 10, now, false)) {
                    staticBuilds++
                    policy.record(key, 10, now)
                }
            }
            if (policy.needsRefresh("media", 10, now, true)) {
                mediaBuilds++
                policy.record("media", 10, now)
            }
        }
        assertEquals(400, staticBuilds) // 6,000 before snapshot gating
        assertEquals(60, mediaBuilds)
    }

    @Test fun retentionIncludesUtf8BracketsAndCommas() {
        val records = listOf("{\"text\":\"Привет 🐈\"}", "{\"text\":\"你好\"}", "{}")
        val sizes = records.map { it.toByteArray(UTF_8).size }
        val firstBytes = "[${records.first()}]".toByteArray(UTF_8).size
        assertEquals(0, LogRetention.retainedCount(sizes, firstBytes - 1))
        assertEquals(1, LogRetention.retainedCount(sizes, firstBytes))
        val allBytes = records.joinToString(",", "[", "]").toByteArray(UTF_8).size
        assertEquals(3, LogRetention.retainedCount(sizes, allBytes))
        assertEquals(2, LogRetention.retainedCount(sizes, allBytes - 1))
    }

    @Test fun retentionMatchesReferenceForRandomLogsAndLimits() {
        val random = Random(128129)
        repeat(500) {
            val records = List(random.nextInt(0, 200)) { "x".repeat(random.nextInt(1, 200)) }
            val max = random.nextInt(2, 10_000)
            var expected = records.size
            while (records.take(expected).joinToString(",", "[", "]").toByteArray(UTF_8).size > max) expected--
            assertEquals(expected, LogRetention.retainedCount(records.map { it.toByteArray(UTF_8).size }, max))
        }
    }
}
