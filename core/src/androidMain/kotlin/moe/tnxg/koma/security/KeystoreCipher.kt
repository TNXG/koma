package moe.tnxg.koma.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore 中不可导出的 AES-256-GCM 密钥，以及配套的密文封装。
 *
 * Keystore 只保存密钥本身；调用方拿到 {"v":1,"iv":...,"ct":...} 后自行落到私有存储。
 * 任何加解密失败都返回 null，由调用方决定删除坏条目，绝不崩溃、不打印明文。
 */
open class KeystoreCipher(
    private val alias: String,
    private val preferStrongBox: Boolean = false,
) {
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    /** Keystore 与 AES/GCM 是否可用；不可用时加密/解密返回 null */
    @Synchronized
    open fun isAvailable(): Boolean = try {
        obtainKey()
        true
    } catch (e: Exception) {
        false
    }

    /** 明文 → 密文 JSON（随机 IV + 128-bit tag）；失败返回 null */
    @Synchronized
    open fun encrypt(plaintext: String, aad: ByteArray): String? {
        return try {
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.ENCRYPT_MODE, obtainKey())
            cipher.updateAAD(aad)
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            JSONObject()
                .put("v", FORMAT_VERSION)
                .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .put("ct", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                .toString()
        } catch (e: Exception) {
            null
        }
    }

    /** 密钥失效、密文被篡改、AAD 不匹配或版本未知都返回 null */
    @Synchronized
    open fun decrypt(blob: String, aad: ByteArray): String? {
        return try {
            val json = JSONObject(blob)
            if (json.optInt("v", -1) != FORMAT_VERSION)
                return null
            val iv = Base64.decode(json.getString("iv"), Base64.NO_WRAP)
            val ciphertext = Base64.decode(json.getString("ct"), Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.DECRYPT_MODE, obtainKey(), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(aad)
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /** 仅测试与「清空全部本地数据」使用；删除后旧密文永久不可解 */
    @Synchronized
    fun deleteKey() {
        try {
            keyStore.deleteEntry(alias)
        } catch (e: Exception) {
            // Keystore 不可用时无可删除，忽略
        }
    }

    private fun obtainKey(): SecretKey {
        (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        return try {
            generator.init(keySpec(strongBox = preferStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P))
            generator.generateKey()
        } catch (e: Exception) {
            if (!preferStrongBox)
                throw e
            // StrongBox 不可用（多数设备）：回退到 TEE，不作为硬要求
            generator.init(keySpec(strongBox = false))
            generator.generateKey()
        }
    }

    private fun keySpec(strongBox: Boolean): KeyGenParameterSpec {
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_BITS)
            .setRandomizedEncryptionRequired(true)
        if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            builder.setIsStrongBoxBacked(true)
        return builder.build()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORM = "AES/GCM/NoPadding"
        const val KEY_BITS = 256
        const val TAG_BITS = 128
        const val FORMAT_VERSION = 1
    }
}
