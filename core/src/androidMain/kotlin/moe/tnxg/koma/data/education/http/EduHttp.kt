package moe.tnxg.koma.data.education.http

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal data class HttpResult(val status: Int, val url: HttpUrl, val headers: Headers, val bytes: ByteArray) {
    val text: String get() = bytes.toString(Charsets.UTF_8)
}

internal class EduHttp(private val client: OkHttpClient) {
    suspend fun call(url: String, body: RequestBody? = null, headers: Map<String, String> = emptyMap()): HttpResult {
        val target = url.toHttpUrl()
        require(target.isHttps) { "登录只允许安全连接" }
        val request = Request.Builder().url(target).header("User-Agent", USER_AGENT)
            .apply { headers.forEach { (key, value) -> header(key, value) } }
            .apply { if (body != null) post(body) }.build()
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        e.printStackTrace()
                        continuation.resumeWithException(IOException("教务连接失败，请检查网络后重试", e))
                    }
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val result = response.use { HttpResult(it.code, it.request.url, it.headers, it.body?.bytes() ?: byteArrayOf()) }
                        if (continuation.isActive) {
                            if (result.status >= 400) continuation.resumeWithException(IOException("教务服务没有响应（${result.status}），请稍后重试"))
                            else continuation.resume(result)
                        }
                    } catch (error: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                }
            })
        }
    }
    suspend fun follow(url: String, body: RequestBody? = null, headers: Map<String, String> = emptyMap()): HttpResult {
        var target = url.toHttpUrl()
        // CAS -> SSO -> ticketlogin -> index chains can legitimately exceed a handful of hops.
        for (hop in 0..MAX_REDIRECTS) {
            val result = if (hop == 0) call(target.toString(), body, headers) else call(target.toString())
            if (result.status !in 300..399) return result
            val location = result.headers["Location"] ?: return result
            if (hop == MAX_REDIRECTS) throw IOException("登录跳转次数过多，请重试")
            target = target.resolve(location) ?: throw IOException("登录跳转地址无效")
        }
        error("Unreachable redirect state")
    }
    companion object {
        private const val MAX_REDIRECTS = 16
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
        fun client(vault: CookieVault): OkHttpClient = OkHttpClient.Builder().cookieJar(vault)
            .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS).build()
    }
}
