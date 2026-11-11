package moe.tnxg.koma.plugins

import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import moe.tnxg.koma.http.CookieVault
import moe.tnxg.koma.http.NativeHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

/**
 * 教务 HTTP 能力（契约见 src/edu/host/native-http.ts，字段逐字对齐）。
 * 网络与加解密在专用线程池；CookieManager 操作切主线程。
 */
@CapacitorPlugin(name = "EduNativeHttp")
class EduNativeHttpPlugin : Plugin() {
    private val client = NativeHttpClient()
    private val executor = Executors.newFixedThreadPool(2) { runnable ->
        Thread(runnable, "koma-native-http").apply { isDaemon = true }
    }
    private val main = Handler(Looper.getMainLooper())
    private lateinit var vault: CookieVault

    override fun load() {
        vault = CookieVault(context)
    }

    override fun handleOnDestroy() {
        executor.shutdownNow()
        super.handleOnDestroy()
    }

    @PluginMethod
    fun request(call: PluginCall) {
        val url = call.getString("url")
        if (url.isNullOrBlank())
            return call.reject("缺少 url")

        val spec = NativeHttpClient.RequestSpec(
            url = url,
            method = call.getString("method") ?: "GET",
            headers = stringMapOf(call.getObject("headers")),
            body = call.getString("body"),
            binary = call.getBoolean("binary") ?: false,
        )

        executor.execute {
            try {
                val response = client.execute(spec)
                main.post { resolve(call, response.toJsObject()) }
            } catch (e: IllegalArgumentException) {
                main.post { call.reject(e.message ?: "参数错误") }
            } catch (e: Exception) {
                // 只报异常类型，不带堆栈、URL query、Cookie 或请求体
                main.post { call.reject("网络请求失败：${e.javaClass.simpleName}") }
            }
        }
    }

    @PluginMethod
    fun cookiesFor(call: PluginCall) {
        val url = call.getString("url")
        if (url.isNullOrBlank())
            return call.reject("缺少 url")
        call.resolve(JSObject().put("value", vault.read(url) ?: JSONObject.NULL))
    }

    @PluginMethod
    fun storeCookies(call: PluginCall) {
        val array = call.getArray("jars") ?: return call.reject("缺少 jars")
        val jars = mutableListOf<Pair<String, List<String>>>()
        for (index in 0 until array.length()) {
            val jar = array.optJSONObject(index) ?: continue
            val url = jar.optString("url")
            if (url.isEmpty())
                continue
            val cookies = jar.optJSONArray("cookies") ?: JSONArray()
            jars += url to (0 until cookies.length()).map { cookies.optString(it) }.filter { it.isNotEmpty() }
        }

        executor.execute {
            val stored = vault.store(jars)
            main.post {
                if (!stored) {
                    call.reject("Cookie 存储不可用", "COOKIE_STORE_UNAVAILABLE")
                    return@post
                }
                // 镜像失败不影响插件存储（插件存储才是读源），也不打断登录
                runCatching { mirrorStore(jars) }
                call.resolve()
            }
        }
    }

    @PluginMethod
    fun clearCookies(call: PluginCall) {
        val array = call.getArray("urls") ?: return call.reject("缺少 urls")
        val urls = (0 until array.length()).map { array.optString(it) }.filter { it.isNotEmpty() }

        executor.execute {
            val removed = vault.clear(urls)
            main.post {
                runCatching { mirrorClear(removed) }
                call.resolve()
            }
        }
    }

    private fun resolve(call: PluginCall, payload: JSObject) {
        try {
            call.resolve(payload)
        } catch (e: Exception) {
            // WebView 已销毁时的迟到响应，忽略
        }
    }

    /** CookieManager 只是 WebView 的镜像：按原始 url + Set-Cookie 串种入 */
    private fun mirrorStore(jars: List<Pair<String, List<String>>>) {
        val manager = CookieManager.getInstance()
        manager.setAcceptCookie(true)
        for ((url, cookies) in jars)
            for (cookie in cookies)
                manager.setCookie(url, cookie)
        manager.flush()
    }

    /** 没有按 url 删除的 API：逐条用 Max-Age=0 过期，再 flush */
    private fun mirrorClear(removed: List<Pair<String, List<CookieVault.Cookie>>>) {
        val manager = CookieManager.getInstance()
        for ((host, cookies) in removed) {
            for (cookie in cookies) {
                val path = if (cookie.path == "/") "" else cookie.path
                val url = "https://$host$path/"
                manager.setCookie(url, "${cookie.name}=; Max-Age=0; Path=${cookie.path}")
            }
        }
        manager.flush()
    }

    private fun stringMapOf(obj: JSObject?): Map<String, String> {
        if (obj == null)
            return emptyMap()
        val out = mutableMapOf<String, String>()
        for (key in obj.keys())
            out[key] = obj.optString(key)
        return out
    }

    private fun NativeHttpClient.ResponseSpec.toJsObject(): JSObject {
        val headerJson = JSObject()
        for ((name, values) in headers)
            headerJson.put(name, JSONArray(values))
        return JSObject()
            .put("status", status)
            .put("url", url)
            .put("headers", headerJson)
            .put("body", body)
    }
}
