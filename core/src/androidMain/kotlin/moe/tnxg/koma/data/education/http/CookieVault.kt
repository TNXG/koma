package moe.tnxg.koma.data.education.http

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** RFC cookie matching on every hop; encrypted private-file persistence also retains session cookies. */
internal class CookieVault(
    private val file: File?,
    private val key: () -> SecretKey = ::androidCookieKey,
) : CookieJar {
    @Serializable private data class StoredCookie(
        val name: String, val value: String, val domain: String, val path: String,
        val expiresAt: Long, val secure: Boolean, val httpOnly: Boolean, val hostOnly: Boolean,
    ) {
        fun cookie(): Cookie = Cookie.Builder().name(name).value(value).path(path).expiresAt(expiresAt).apply {
            if (hostOnly) hostOnlyDomain(domain) else domain(domain)
            if (secure) secure()
            if (httpOnly) httpOnly()
        }.build()
    }
    private val cookies = mutableListOf<Cookie>()
    private val json = Json { ignoreUnknownKeys = true }
    private val aad = "koma.education.cookies.v1".toByteArray()
    init { load() }

    @Synchronized override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (!allowed(url.host)) return
        for (cookie in cookies) {
            if (!allowed(cookie.domain)) continue
            this.cookies.removeAll { it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path }
            if (cookie.expiresAt > System.currentTimeMillis()) this.cookies += cookie
        }
        persist()
    }
    @Synchronized override fun loadForRequest(url: HttpUrl): List<Cookie> {
        cookies.removeAll { it.expiresAt <= System.currentTimeMillis() }
        if (!allowed(url.host)) return emptyList()
        return cookies.filter { it.matches(url) }.sortedByDescending { it.path.length }
    }
    @Synchronized fun clear() { cookies.clear(); persist() }
    private fun allowed(host: String): Boolean = host == "qyrz.xjvut.edu.cn" || host == "jw.xjvut.edu.cn"

    private fun load() {
        val target = file ?: return
        if (!target.isFile) return
        try {
            val bytes = target.readBytes()
            require(bytes.size > 29 && bytes[0] == 1.toByte())
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
            cipher.updateAAD(aad)
            val decoded = cipher.doFinal(bytes.copyOfRange(13, bytes.size)).toString(Charsets.UTF_8)
            val saved = json.decodeFromString<List<StoredCookie>>(decoded)
            cookies += saved.mapNotNull { entry ->
                try { entry.cookie().takeIf { allowed(it.domain) && it.expiresAt > System.currentTimeMillis() } }
                catch (_: IllegalArgumentException) { null }
            }
        } catch (_: Exception) {
            // Corrupted, restored-to-another-device, or invalidated keys mean a fresh login, never plaintext fallback.
            cookies.clear()
        }
    }
    private fun persist() {
        val target = file ?: return
        val parent = target.absoluteFile.parentFile ?: throw IOException("会话存储目录不可用")
        if (!parent.isDirectory && !parent.mkdirs()) throw IOException("会话存储目录不可用")
        val values = cookies.filter { it.expiresAt > System.currentTimeMillis() }.map {
            StoredCookie(it.name, it.value, it.domain, it.path, it.expiresAt, it.secure, it.httpOnly, it.hostOnly)
        }
        if (values.isEmpty()) {
            if (target.exists() && !target.delete()) throw IOException("无法清除登录会话")
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(aad)
        val encrypted = byteArrayOf(1) + cipher.iv + cipher.doFinal(json.encodeToString(values).toByteArray())
        val temp = File.createTempFile("edu-cookies-", ".tmp", parent)
        try {
            if (!temp.setReadable(false, false) || !temp.setWritable(false, false) ||
                !temp.setReadable(true, true) || !temp.setWritable(true, true)) throw IOException("无法保护登录会话")
            temp.outputStream().use { stream -> stream.write(encrypted); stream.fd.sync() }
            try { Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
            catch (_: AtomicMoveNotSupportedException) { Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING) }
        } finally { temp.delete() }
    }
}

@Synchronized
private fun androidCookieKey(): SecretKey {
    val alias = "koma.education.cookies.v1"
    val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (store.getKey(alias, null) as? SecretKey)?.let { return it }
    return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
        init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
    }.generateKey()
}
