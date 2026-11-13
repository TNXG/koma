package moe.tnxg.koma.platform

/** The two places the profile keeps a user-chosen photo. */
enum class PhotoTarget { Avatar, Cover }

/** Bytes of the random cover cache plus the moment they were written. */
class CachedPhoto(val bytes: ByteArray, val savedAtMillis: Long)

/**
 * On-device photo handling for the profile: picking from the system picker, the two user-chosen
 * photos, and the short-lived cache of the random cover. Every file here lives in app storage.
 */
interface ImageServices {
    /** Opens the system photo picker for [target]; copies the result locally and returns its path. */
    suspend fun pickPhoto(target: PhotoTarget): String?

    /** Bytes of a photo previously returned by [pickPhoto]; null when it is missing. */
    fun readPhotoBytes(path: String): ByteArray?

    fun deletePhoto(target: PhotoTarget)

    /** Cached random cover bytes, or null when nothing has been cached yet. */
    fun readCoverCache(): CachedPhoto?

    fun writeCoverCache(bytes: ByteArray, savedAtMillis: Long)

    fun clearCoverCache()
}
