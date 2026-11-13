package moe.tnxg.koma.ui.component.media

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import moe.tnxg.koma.platform.CachedPhoto
import moe.tnxg.koma.platform.ImageServices
import kotlinx.datetime.Clock

internal const val COVER_URL = "https://api-space.tnxg.top/images/wallpaper_height?mode=redirect&source=cdcn"

/** The random cover is reused for three days; a user-chosen cover never expires. */
internal const val COVER_CACHE_TTL_MS = 3L * 24 * 60 * 60 * 1000

internal fun coverCacheIsFresh(savedAtMillis: Long, nowMillis: Long): Boolean =
    savedAtMillis > 0L && nowMillis >= savedAtMillis && nowMillis - savedAtMillis < COVER_CACHE_TTL_MS

@Immutable
internal sealed interface CoverState {
    data object Loading : CoverState
    data class Ready(val bitmap: ImageBitmap) : CoverState
    data class Error(val message: String) : CoverState
}

internal expect suspend fun downloadCoverBytes(url: String): ByteArray

internal expect suspend fun decodePhotoBytes(bytes: ByteArray): ImageBitmap

/**
 * A user-chosen cover is decoded straight from local storage. Otherwise the random cover is
 * served from the on-device cache while it is younger than [COVER_CACHE_TTL_MS]; a forced
 * refresh, a stale cache, or an offline failure all fall back to whatever is available.
 */
internal suspend fun resolveCover(
    services: ImageServices?,
    photoPath: String,
    forceRefresh: Boolean,
    nowMillis: Long = Clock.System.now().toEpochMilliseconds(),
): ImageBitmap {
    if (photoPath.isNotBlank()) {
        val bytes = services?.readPhotoBytes(photoPath) ?: throw IllegalStateException("封面照片已丢失")
        return decodePhotoBytes(bytes)
    }
    val cached = services?.readCoverCache()
    if (!forceRefresh && cached != null && coverCacheIsFresh(cached.savedAtMillis, nowMillis)) {
        return decodePhotoBytes(cached.bytes)
    }
    return try {
        val bytes = downloadCoverBytes(COVER_URL)
        services?.writeCoverCache(bytes, nowMillis)
        decodePhotoBytes(bytes)
    } catch (failure: Exception) {
        val stale: CachedPhoto = cached ?: throw failure
        decodePhotoBytes(stale.bytes)
    }
}
