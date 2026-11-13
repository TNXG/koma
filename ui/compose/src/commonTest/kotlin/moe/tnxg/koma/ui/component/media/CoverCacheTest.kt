package moe.tnxg.koma.ui.component.media

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoverCacheTest {
    private val savedAt = 1_000_000L

    @Test
    fun cachedCoverIsServedForExactlyThreeDays() {
        assertTrue(coverCacheIsFresh(savedAt, savedAt))
        assertTrue(coverCacheIsFresh(savedAt, savedAt + COVER_CACHE_TTL_MS - 1))
        assertFalse(coverCacheIsFresh(savedAt, savedAt + COVER_CACHE_TTL_MS))
        assertFalse(coverCacheIsFresh(savedAt, savedAt + COVER_CACHE_TTL_MS * 2))
    }

    @Test
    fun missingOrFutureTimestampsNeverCountAsFresh() {
        assertFalse(coverCacheIsFresh(0L, savedAt))
        assertFalse(coverCacheIsFresh(-1L, savedAt))
        assertFalse(coverCacheIsFresh(savedAt, savedAt - 1))
    }
}
