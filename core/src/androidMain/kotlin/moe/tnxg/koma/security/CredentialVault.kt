package moe.tnxg.koma.security

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.security.MessageDigest

/**
 * 登录凭据存储：Keystore 只存不可导出的 AES-256-GCM 密钥，
 * 密文 JSON {"v":1,"iv":...,"ct":...} 存 app 私有 SharedPreferences。
 *
 * - 键 = SHA-256(schoolId) 十六进制，不把学校 id 明文写进文件名/键名；
 * - AAD = schoolId 的 UTF-8 字节，条目不能跨学校混用；
 * - 解密失败一律删除该条并返回 null，不崩溃、不打印敏感内容。
 */
class CredentialVault(
    context: Context,
    private val cipher: KeystoreCipher = KeystoreCipher(KEY_ALIAS, preferStrongBox = true),
    prefsName: String = PREFS_NAME,
) {
    data class Credential(val username: String, val password: String)

    private val prefs = context.applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun isAvailable(): Boolean = cipher.isAvailable()

    /** 覆盖同校旧条目；加密或落盘失败返回 false */
    @Synchronized
    fun save(schoolId: String, username: String, password: String): Boolean {
        val payload = JSONObject()
            .put("username", username)
            .put("password", password)
            .toString()
        val blob = cipher.encrypt(payload, aadOf(schoolId)) ?: return false
        return prefs.edit().putString(keyOf(schoolId), blob).commit()
    }

    @Synchronized
    fun load(schoolId: String): Credential? {
        val key = keyOf(schoolId)
        val blob = prefs.getString(key, null) ?: return null
        val plain = cipher.decrypt(blob, aadOf(schoolId))
        if (plain == null) {
            prefs.edit().remove(key).commit()
            Log.w(TAG, "凭据条目不可解密，已删除：${key.take(12)}…")
            return null
        }
        val credential = try {
            val json = JSONObject(plain)
            Credential(json.getString("username"), json.getString("password"))
        } catch (e: Exception) {
            null
        }
        if (credential == null) {
            prefs.edit().remove(key).commit()
            Log.w(TAG, "凭据条目格式异常，已删除：${key.take(12)}…")
        }
        return credential
    }

    @Synchronized
    fun remove(schoolId: String) {
        // 退出登录/删除凭据必须落盘后再返回：进程随后被杀也不能留下旧密文
        check(prefs.edit().remove(keyOf(schoolId)).commit()) { "安全凭据删除失败" }
    }

    private fun keyOf(schoolId: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(schoolId.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun aadOf(schoolId: String): ByteArray = schoolId.toByteArray(Charsets.UTF_8)

    companion object {
        const val PREFS_NAME = "koma_credentials"
        const val KEY_ALIAS = "koma.credentials.v1"
        private const val TAG = "KomaCredentials"
    }
}
