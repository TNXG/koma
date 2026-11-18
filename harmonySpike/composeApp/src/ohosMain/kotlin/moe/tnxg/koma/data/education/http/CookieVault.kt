package moe.tnxg.koma.data.education.http

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.ktor.http.Cookie
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Url
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import moe.tnxg.koma.spike.OhosPaths

/**
 * RFC cookie 匹配（okhttp CookieJar 语义移植），登录会话持久化到应用沙箱
 * （0600 权限明文 JSON —— debug spike 权衡；Android 端为 KeyStore 加密）。
 */
internal class CookieVault(private val path: String? = OhosPaths.filesDir?.plus("/edu_cookies.json")) : CookiesStorage {

    @Serializable
    private data class StoredCookie(
        val name: String, val value: String, val domain: String, val path: String,
        val expiresAt: Long, val secure: Boolean, val httpOnly: Boolean, val hostOnly: Boolean,
    )

    private class VaultCookie(
        val name: String, val value: String, val domain: String, val path: String,
        val expiresAt: Long, val secure: Boolean, val httpOnly: Boolean, val hostOnly: Boolean,
    )

    private val cookies = mutableListOf<VaultCookie>()
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = kotlinx.coroutines.sync.Mutex()
    private val aad = "koma.education.cookies.v1"

    init {
        path?.let { load(it) }
    }

    private fun matches(cookie: VaultCookie, url: Url): Boolean {
        if (cookie.secure && url.protocol != io.ktor.http.URLProtocol.HTTPS) return false
        val host = url.host
        val domainOk = if (cookie.hostOnly) host == cookie.domain
        else host == cookie.domain || host.endsWith("." + cookie.domain.removePrefix("."))
        if (!domainOk) return false
        val requestPath = "/" + url.pathSegments.joinToString("/")
        val cookiePath = cookie.path.ifEmpty { "/" }
        val pathOk = if (cookiePath == "/") true
        else requestPath.startsWith(cookiePath) &&
            (cookiePath.endsWith("/") || requestPath.getOrNull(cookiePath.length) == '/' ||
                requestPath.length == cookiePath.length)
        return pathOk
    }

    private fun allowed(host: String): Boolean = host == "qyrz.xjvut.edu.cn" || host == "jw.xjvut.edu.cn"

    private fun nowMillis(): Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

    override fun close() {}

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        mutex.withLock {
            val url = requestUrl
            if (!allowed(url.host)) return
            val domain = cookie.domain ?: url.host
            if (!allowed(domain)) return
            val expiresAt = cookie.expires?.timestamp?.let { ts ->
                if (ts in 1..10_000_000_000L) ts * 1000 else ts // GMTDate 秒 → 毫秒归一
            } ?: cookie.maxAge?.let { nowMillis() + it * 1000L } ?: Long.MAX_VALUE / 2
            cookies.removeAll {
                it.name == cookie.name && it.domain == domain && it.path == cookie.path
            }
            if (expiresAt > nowMillis()) {
                cookies += VaultCookie(
                    cookie.name, cookie.value, domain, cookie.path.orEmpty().ifEmpty { "/" },
                    expiresAt, cookie.secure, cookie.httpOnly, cookie.domain == null,
                )
            }
            persist()
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> = mutex.withLock {
        val url = requestUrl
        cookies.removeAll { it.expiresAt <= nowMillis() }
        if (!allowed(url.host)) return emptyList()
        cookies.filter { matches(it, url) }
            .sortedByDescending { it.path.length }
            .map {
                Cookie(
                    name = it.name, value = it.value,
                    domain = it.domain, path = it.path,
                    secure = it.secure, httpOnly = it.httpOnly,
                )
            }
    }

    suspend fun clear() = mutex.withLock {
        cookies.clear(); persist()
    }

    private fun load(file: String) {
        val bytes = OhosPaths.readBytes(file) ?: return
        try {
            val text = bytes.decodeToString()
            val saved = json.decodeFromString<List<StoredCookie>>(text)
            val now = nowMillis()
            cookies += saved.mapNotNull { entry ->
                if (!allowed(entry.domain) || entry.expiresAt <= now) null
                else VaultCookie(
                    entry.name, entry.value, entry.domain, entry.path,
                    entry.expiresAt, entry.secure, entry.httpOnly, entry.hostOnly,
                )
            }
        } catch (_: Exception) {
            // 损坏/换机/失效 → 重新登录，绝不回退明文
            cookies.clear()
        }
    }

    private fun persist() {
        val file = path ?: return
        val values = cookies.filter { it.expiresAt > nowMillis() }.map {
            StoredCookie(it.name, it.value, it.domain, it.path, it.expiresAt, it.secure, it.httpOnly, it.hostOnly)
        }
        if (values.isEmpty()) {
            OhosPaths.delete(file)
            return
        }
        OhosPaths.writeBytes(file, json.encodeToString(values).encodeToByteArray())
    }
}
