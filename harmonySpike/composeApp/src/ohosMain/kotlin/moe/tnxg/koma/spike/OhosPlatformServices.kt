package moe.tnxg.koma.spike

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import moe.tnxg.koma.data.education.EduService
import moe.tnxg.koma.data.education.XjvutEduService
import moe.tnxg.koma.platform.PlatformServices
import moe.tnxg.koma.platform.SavedCredentials

/** OHOS networking, sandbox settings, and device-local encrypted Asset Store credentials. */
class OhosPlatformServices : PlatformServices {

    private val settingsPath = OhosPaths.filesDir + "/koma_settings.json"
    private val credentialVault = OhosCredentialVault()

    @Serializable
    private data class SettingsMap(val values: Map<String, String> = emptyMap())

    private val settings = mutableMapOf<String, String>().also { map ->
        settingsPath.let { path ->
            OhosPaths.readBytes(path)?.decodeToString()?.let { text ->
                try { map.putAll(Json.decodeFromString<SettingsMap>(text).values) } catch (_: Exception) {}
            }
        }
    }

    private fun persistSettings() {
        settingsPath.let { path ->
            OhosPaths.writeBytes(path, Json.encodeToString(SettingsMap(settings.toMap())).encodeToByteArray())
        }
    }

    override val edu: EduService = XjvutEduService()

    override val canRememberCredentials: Boolean = true

    override val notifications = null

    override fun readSetting(key: String): String? = settings[key]

    override fun writeSetting(key: String, value: String?) {
        if (value == null) settings.remove(key) else settings[key] = value
        persistSettings()
    }

    override suspend fun loadCredentials(): SavedCredentials? = credentialVault.load()

    override suspend fun saveCredentials(value: SavedCredentials?) = credentialVault.save(value)

    override suspend fun recognizeCaptcha(dataUrl: String, view: Int): String? = OhosCaptchaBridge.recognize(dataUrl, view)

    override suspend fun migrateLegacySettings() {}
}
