package moe.tnxg.koma.spike

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import moe.tnxg.koma.platform.CachedPhoto
import moe.tnxg.koma.platform.ImageServices
import moe.tnxg.koma.platform.PhotoTarget
import platform.posix.stat
import platform.posix.utimbuf

private const val MAX_PICKED_BYTES = 32 * 1024 * 1024

/**
 * 照片全部落在应用沙箱（`filesDir/photos/`）：每个 [PhotoTarget] 一张 JPEG 原图，
 * 随机封面缓存为单文件 `cover-cache.bin`（Android 端同目录约定）。
 * 相册选图经 [OhosPhotoPickerBridge] 走 ArkTS `photoAccessHelper`，选回后原样拷贝入沙箱。
 */
class OhosImageServices : ImageServices {

    private val photoDir get() = OhosPaths.filesDir + "/photos"
    private val coverCachePath get() = OhosPaths.filesDir + "/cover-cache.bin"

    override suspend fun pickPhoto(target: PhotoTarget): String? {
        val bytes = OhosPhotoPickerBridge.pickPhotoBytes() ?: return null
        if (bytes.size > MAX_PICKED_BYTES) return null
        val dir = photoDir
        platform.posix.mkdir(dir, 448u) // 0700
        val path = "$dir/${target.name.lowercase()}.jpg"
        return if (OhosPaths.writeBytes(path, bytes)) path else null
    }

    override fun readPhotoBytes(path: String): ByteArray? {
        // 与 Android 端一致：只读沙箱 photos 目录内的路径，拒绝越界读取。
        if (!path.startsWith("$photoDir/")) return null
        return OhosPaths.readBytes(path)
    }

    override fun deletePhoto(target: PhotoTarget) {
        OhosPaths.delete("$photoDir/${target.name.lowercase()}.jpg")
    }

    override fun readCoverCache(): CachedPhoto? {
        val bytes = OhosPaths.readBytes(coverCachePath) ?: return null
        if (bytes.isEmpty()) return null
        val savedAt = fileModifiedMillis(coverCachePath) ?: return null
        return CachedPhoto(bytes, savedAt)
    }

    override fun writeCoverCache(bytes: ByteArray, savedAtMillis: Long) {
        if (!OhosPaths.writeBytes(coverCachePath, bytes)) return
        setFileModifiedMillis(coverCachePath, savedAtMillis)
    }

    override fun clearCoverCache() {
        OhosPaths.delete(coverCachePath)
    }

    /** 文件 mtime（`st_mtim.tv_sec` 秒 → 毫秒）； stat 失败返回 null，缓存条目随之视为无效。 */
    @OptIn(ExperimentalForeignApi::class)
    private fun fileModifiedMillis(path: String): Long? = memScoped {
        val info = alloc<stat>()
        if (platform.posix.stat(path, info.ptr) != 0) null
        else info.st_mtim.tv_sec.toLong() * 1000L
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun setFileModifiedMillis(path: String, millis: Long): Boolean = memScoped {
        val times = alloc<utimbuf>()
        times.actime = (millis / 1000).convert()
        times.modtime = (millis / 1000).convert()
        platform.posix.utime(path, times.ptr) == 0
    }
}
