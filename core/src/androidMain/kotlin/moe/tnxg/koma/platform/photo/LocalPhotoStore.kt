package moe.tnxg.koma.platform.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.tnxg.koma.platform.CachedPhoto
import moe.tnxg.koma.platform.ImageServices
import moe.tnxg.koma.platform.PhotoTarget
import java.io.File
import java.io.IOException

private const val MAX_PHOTO_DIMENSION = 1440
private const val PHOTO_JPEG_QUALITY = 88
private const val MAX_PICKED_BYTES = 32 * 1024 * 1024

/**
 * Photos stay in app-private storage: one JPEG per [PhotoTarget] for user picks, plus a single
 * `cover-cache.bin` whose modification time is the moment the random cover was downloaded.
 */
class LocalPhotoStore(context: Context) : ImageServices {
    private val app = context.applicationContext
    private val picker = context as? PhotoPickerHost
    private val photoDir = File(app.filesDir, "photos")
    private val coverCache = File(app.filesDir, "cover-cache.bin")

    override suspend fun pickPhoto(target: PhotoTarget): String? {
        val uri = picker?.pickPhotoUri() ?: return null
        return withContext(Dispatchers.IO) { importPhoto(target, uri) }
    }

    override fun readPhotoBytes(path: String): ByteArray? {
        val file = File(path)
        return runCatching {
            if (!file.canonicalFile.toPath().startsWith(photoDir.canonicalFile.toPath())) null
            else file.takeIf { it.length() in 1..MAX_PICKED_BYTES.toLong() }?.readBytes()
        }.getOrNull()
    }

    override fun deletePhoto(target: PhotoTarget) {
        runCatching { photoFile(target).delete() }
    }

    override fun readCoverCache(): CachedPhoto? = runCatching {
        if (!coverCache.isFile) return null
        val bytes = coverCache.readBytes()
        if (bytes.isEmpty()) return null
        CachedPhoto(bytes, coverCache.lastModified())
    }.getOrNull()

    override fun writeCoverCache(bytes: ByteArray, savedAtMillis: Long) {
        runCatching {
            coverCache.writeBytes(bytes)
            coverCache.setLastModified(savedAtMillis)
        }
    }

    override fun clearCoverCache() {
        runCatching { coverCache.delete() }
    }

    private fun photoFile(target: PhotoTarget) =
        File(photoDir, "${target.name.lowercase()}.jpg")

    private fun importPhoto(target: PhotoTarget, uri: Uri): String? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = openStream(uri) ?: return null
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, MAX_PHOTO_DIMENSION)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = openStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        return try {
            photoDir.mkdirs()
            val file = photoFile(target)
            file.outputStream().use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, PHOTO_JPEG_QUALITY, output)) {
                    throw IOException("无法保存所选照片")
                }
            }
            file.absolutePath
        } finally {
            bitmap.recycle()
        }
    }

    private fun openStream(uri: Uri) = runCatching { app.contentResolver.openInputStream(uri) }.getOrNull()
}
