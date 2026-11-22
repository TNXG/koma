package moe.tnxg.koma.ui.component.media

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import moe.tnxg.koma.platform.ImageServices

/**
 * Decoded profile artwork kept between screens, so opening 我的 paints its cover and avatar on
 * the first frame instead of after a disk read and decode. Keyed by photo path; the random cover
 * uses its empty path as the key.
 */
internal object ProfilePhotoCache {
    private val decoded = mutableMapOf<String, ImageBitmap>()

    /** Home preload and the profile cover can ask for the same image; resolve it once. */
    private val coverLock = Mutex()

    fun get(path: String): ImageBitmap? = decoded[path]

    /** Drops [path]'s entry after its file was replaced or deleted. */
    fun invalidate(path: String) {
        decoded.remove(path)
    }

    fun clear() {
        decoded.clear()
    }

    /**
     * The cover for [photoPath]: a chosen photo, or the random cover once it is at most three days
     * old. [forceRefresh] skips cached bytes and downloads a new random cover.
     */
    suspend fun cover(services: ImageServices?, photoPath: String, forceRefresh: Boolean = false): ImageBitmap {
        if (!forceRefresh) decoded[photoPath]?.let { return it }
        return coverLock.withLock {
            if (!forceRefresh) decoded[photoPath]?.let { return@withLock it }
            val bitmap = withContext(Dispatchers.Default) { resolveCover(services, photoPath, forceRefresh) }
            decoded[photoPath] = bitmap
            bitmap
        }
    }

    /** A user-chosen photo decoded from local storage, or null when there is none. */
    suspend fun photo(services: ImageServices?, path: String): ImageBitmap? {
        if (path.isBlank()) return null
        decoded[path]?.let { return it }
        val bitmap = withContext(Dispatchers.Default) {
            services?.readPhotoBytes(path)?.let { decodePhotoBytes(it) }
        }
        if (bitmap != null) decoded[path] = bitmap
        return bitmap
    }

    /** Warms the profile's artwork before it opens; a failure stays silent until 我的 shows it. */
    suspend fun preload(services: ImageServices, photoPath: String, avatarPath: String) {
        try {
            cover(services, photoPath)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
        }
        try {
            photo(services, avatarPath)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
        }
    }
}
