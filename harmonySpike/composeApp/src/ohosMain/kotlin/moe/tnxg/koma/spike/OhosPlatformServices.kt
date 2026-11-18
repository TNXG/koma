package moe.tnxg.koma.spike

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import moe.tnxg.koma.data.education.EduService
import moe.tnxg.koma.data.education.LoginPreparation
import moe.tnxg.koma.data.education.LoginResult
import moe.tnxg.koma.data.education.XjvutEduService
import moe.tnxg.koma.domain.model.Semester
import moe.tnxg.koma.domain.model.Term
import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.platform.PlatformServices
import moe.tnxg.koma.platform.SavedCredentials

/**
 * OHOS 平台服务（M2）。
 *
 * 与 Android 端对齐的能力：
 *  - 教务网络层：ktor-OHOS（CPF fork 3.3.3-1.1.0，Curl 引擎 + 静态 libcurl）完整移植 okhttp 栈，
 *    登录/验证码/课表导入行为一致
 *  - 设置持久化：应用沙箱 filesDir JSON（等价 SharedPreferences）
 *  - 会话 Cookie 持久化：沙箱文件（0600）
 *
 * 仍降级：验证码 OCR（无 onnxruntime，手动输入）、通知、图片选择器、凭据保存（无 Keystore）。
 */
class OhosPlatformServices : PlatformServices {

    private val settingsPath = OhosPaths.filesDir?.plus("/koma_settings.json")

    @Serializable
    private data class SettingsMap(val values: Map<String, String> = emptyMap())

    private val settings = mutableMapOf<String, String>().also { map ->
        settingsPath?.let { path ->
            OhosPaths.readBytes(path)?.decodeToString()?.let { text ->
                try { map.putAll(Json.decodeFromString<SettingsMap>(text).values) } catch (_: Exception) {}
            }
        }
    }
    private val settingsMutex = kotlinx.coroutines.sync.Mutex()

    private fun persistSettings() {
        settingsPath?.let { path ->
            OhosPaths.writeBytes(path, Json.encodeToString(SettingsMap(settings.toMap())).encodeToByteArray())
        }
    }

    override val edu: EduService = XjvutEduService()

    override val canRememberCredentials: Boolean = false

    override val notifications = null

    override fun readSetting(key: String): String? = settings[key]

    override fun writeSetting(key: String, value: String?) {
        if (value == null) settings.remove(key) else settings[key] = value
        persistSettings()
    }

    override suspend fun loadCredentials(): SavedCredentials? = null

    override suspend fun saveCredentials(value: SavedCredentials?) {}

    override suspend fun recognizeCaptcha(dataUrl: String, view: Int): String = ""

    override suspend fun migrateLegacySettings() {}
}
