package moe.tnxg.koma.platform

import moe.tnxg.koma.data.education.EduService

data class SavedCredentials(val username: String, val password: String)

interface PlatformServices {
    val edu: EduService
    val canRememberCredentials: Boolean
    fun readSetting(key: String): String?
    fun writeSetting(key: String, value: String?)
    suspend fun loadCredentials(): SavedCredentials?
    suspend fun saveCredentials(value: SavedCredentials?)
    suspend fun recognizeCaptcha(dataUrl: String, view: Int): String?
    val notifications: NotificationService?

    /** Null on platforms without a photo picker; the profile then stays on generated artwork. */
    val images: ImageServices? get() = null

    /** Must finish before reading the snapshot or start date on application startup. */
    suspend fun migrateLegacySettings() {}
}
