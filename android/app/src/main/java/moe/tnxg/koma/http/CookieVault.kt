package moe.tnxg.koma.http

import android.content.Context
import android.util.Log
import moe.tnxg.koma.security.KeystoreCipher
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 插件自管的 Cookie 存储：按 host 索引（RFC 6265 的 Cookie 不区分端口），
 * Cookie 名、Path 与值一起用 Keystore AES-GCM 加密后存私有 SharedPreferences。
 *
 * 为什么不用 CookieManager 直接读：CookieManager.getCookie(url) 会按 Path 过滤，
 * CAS 的 TGT 带 Path=/cas、教务的 JSESSIONID 带 Path=/jwglxt，按 origin 读会读不回来；
 * 教务插件是自己拼 Cookie 头的，需要 host 级全量。CookieManager 只作 WebView 镜像。
 */
class CookieVault(
    context: Context,
    private val cipher: KeystoreCipher = KeystoreCipher(KEY_ALIAS),
    prefsName: String = PREFS_NAME,
) {
    data class Cookie(
        val name: String,
        val value: String,
        val path: String,
        val expiresAt: Long = 0L,
    ) {
        fun expired(now: Long): Boolean = expiresAt in 1..now
    }

    private val prefs = context.applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    /** "k=v; k2=v2"；没有可用 Cookie 时 null。只按 host 过滤，不按 Path 过滤 */
    @Synchronized
    fun read(url: String): String? {
        val host = hostOf(url) ?: return null
        val now = System.currentTimeMillis()
        val cookies = loadAll()[host].orEmpty().filter { !it.expired(now) }
        if (cookies.isEmpty())
            return null
        return cookies.joinToString("; ") { "${it.name}=${it.value}" }
    }

    /** 写入各 url 的 Set-Cookie 串；加密或落盘失败返回 false */
    @Synchronized
    fun store(jars: List<Pair<String, List<String>>>): Boolean {
        val hosts = loadAll()
        for ((url, cookies) in jars) {
            val host = hostOf(url) ?: continue
            val bucket = hosts.getOrPut(host) { mutableListOf() }
            for (raw in cookies)
                applySetCookie(bucket, raw, url)
        }
        pruneExpired(hosts)
        return persist(hosts)
    }

    /**
     * 只清这些 url 的 Cookie：path 为 "/" 时清整个 host，否则清该 path 前缀下的条目。
     * 返回被删除的 [host] → 条目列表，供调用方同步清理 CookieManager 镜像。
     */
    @Synchronized
    fun clear(urls: List<String>): List<Pair<String, List<Cookie>>> {
        val hosts = loadAll()
        val removed = mutableListOf<Pair<String, List<Cookie>>>()
        for (url in urls) {
            val host = hostOf(url) ?: continue
            val bucket = hosts[host] ?: continue
            val prefix = pathOf(url)
            val gone = if (prefix == "/")
                bucket.toList()
            else
                bucket.filter { underPath(it.path, prefix) }
            if (gone.isEmpty())
                continue
            val kept = bucket.filterNot { cookie ->
                gone.any { it.name == cookie.name && it.path == cookie.path }
            }
            removed += host to gone
            if (kept.isEmpty())
                hosts.remove(host)
            else
                hosts[host] = kept.toMutableList()
        }
        persist(hosts)
        return removed
    }

    private fun loadAll(): MutableMap<String, MutableList<Cookie>> {
        val blob = prefs.getString(STORE_KEY, null) ?: return mutableMapOf()
        val plain = cipher.decrypt(blob, AAD) ?: run {
            Log.w(TAG, "Cookie 存储不可解密，按空处理")
            return mutableMapOf()
        }
        val hosts = try {
            val root = JSONObject(plain)
            if (root.optInt("v", -1) != FORMAT_VERSION)
                return mutableMapOf()
            val json = root.getJSONObject("hosts")
            val out = mutableMapOf<String, MutableList<Cookie>>()
            for (host in json.keys()) {
                val array = json.getJSONArray(host)
                out[host] = (0 until array.length()).mapNotNull { index ->
                    val item = array.optJSONObject(index) ?: return@mapNotNull null
                    val name = item.optString("name")
                    if (name.isEmpty())
                        null
                    else
                        Cookie(
                            name = name,
                            value = item.optString("value"),
                            path = item.optString("path", "/").ifEmpty { "/" },
                            expiresAt = item.optLong("expires", 0L),
                        )
                }.toMutableList()
            }
            out
        } catch (e: Exception) {
            mutableMapOf()
        }
        return hosts
    }

    private fun persist(hosts: Map<String, List<Cookie>>): Boolean {
        val root = JSONObject().put("v", FORMAT_VERSION)
        val json = JSONObject()
        for ((host, cookies) in hosts) {
            if (cookies.isEmpty())
                continue
            val array = JSONArray()
            for (cookie in cookies) {
                array.put(
                    JSONObject()
                        .put("name", cookie.name)
                        .put("value", cookie.value)
                        .put("path", cookie.path)
                        .put("expires", cookie.expiresAt),
                )
            }
            json.put(host, array)
        }
        root.put("hosts", json)
        val blob = cipher.encrypt(root.toString(), AAD) ?: return false
        return prefs.edit().putString(STORE_KEY, blob).commit()
    }

    private fun applySetCookie(bucket: MutableList<Cookie>, raw: String, url: String) {
        val parts = raw.split(";")
        val pair = parts.firstOrNull()?.trim().orEmpty()
        val at = pair.indexOf('=')
        if (at <= 0)
            return
        val name = pair.substring(0, at).trim()
        val value = pair.substring(at + 1).trim()
        if (name.isEmpty())
            return

        var path = defaultPath(url)
        var expiresAt = 0L
        var expired = false
        for (attr in parts.drop(1)) {
            val trimmed = attr.trim()
            val eq = trimmed.indexOf('=')
            val key = (if (eq >= 0) trimmed.substring(0, eq) else trimmed).trim().lowercase(Locale.ROOT)
            val attrValue = if (eq >= 0) trimmed.substring(eq + 1).trim() else ""
            when (key) {
                "path" -> if (attrValue.startsWith("/"))
                    path = attrValue
                "max-age" -> {
                    val seconds = attrValue.toLongOrNull()
                    if (seconds != null) {
                        if (seconds <= 0)
                            expired = true
                        else
                            expiresAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds)
                    }
                }
                "expires" -> {
                    val instant = parseExpires(attrValue)
                    if (instant != null) {
                        if (instant <= System.currentTimeMillis())
                            expired = true
                        else
                            expiresAt = instant
                    }
                }
            }
        }

        bucket.removeAll { it.name == name && it.path == path }
        if (expired)
            return
        bucket += Cookie(name, value, path, expiresAt)
    }

    private fun pruneExpired(hosts: MutableMap<String, MutableList<Cookie>>) {
        val now = System.currentTimeMillis()
        val empty = mutableListOf<String>()
        for ((host, cookies) in hosts) {
            cookies.removeAll { it.expired(now) }
            if (cookies.isEmpty())
                empty += host
        }
        for (host in empty)
            hosts.remove(host)
    }

    /** RFC 6265 默认路径：去掉最后一段；没有 "/" 时用 "/" */
    private fun defaultPath(url: String): String {
        val path = try {
            URL(url).path
        } catch (e: Exception) {
            ""
        }
        if (path.isEmpty() || !path.startsWith("/"))
            return "/"
        val last = path.lastIndexOf('/')
        return if (last <= 0) "/" else path.substring(0, last)
    }

    /** 清除目录是否覆盖该 Cookie 路径：/jwglxt/ 覆盖 /jwglxt 与 /jwglxt/... */
    private fun underPath(cookiePath: String, prefix: String): Boolean {
        val normalized = prefix.trimEnd('/').ifEmpty { "/" }
        if (normalized == "/")
            return true
        return cookiePath == normalized || cookiePath.startsWith("$normalized/")
    }

    private fun pathOf(url: String): String = try {
        URL(url).path.ifEmpty { "/" }
    } catch (e: Exception) {
        "/"
    }

    private fun hostOf(url: String): String? = try {
        URL(url).host?.lowercase(Locale.ROOT)?.takeIf { it.isNotEmpty() }
    } catch (e: Exception) {
        null
    }

    private fun parseExpires(value: String): Long? = try {
        ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
    } catch (e: Exception) {
        null
    }

    companion object {
        const val PREFS_NAME = "koma_cookies"
        const val KEY_ALIAS = "koma.cookies.v1"
        private const val STORE_KEY = "vault_v1"
        private const val FORMAT_VERSION = 1
        private const val TAG = "KomaCookies"
        private val AAD = "koma.cookies.vault".toByteArray(Charsets.UTF_8)
    }
}
