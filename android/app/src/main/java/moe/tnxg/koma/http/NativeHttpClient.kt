package moe.tnxg.koma.http

import android.util.Base64
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * EduNativeHttp 的核心实现：一头一响应、不跟重定向，便于逐跳收 Set-Cookie。
 * 不自动附加 Cookie（xjvut 会话自己拼 Cookie 头），也不自动重试。
 */
class NativeHttpClient(
    private val client: OkHttpClient = defaultClient(),
) {
    data class RequestSpec(
        val url: String,
        val method: String = "GET",
        val headers: Map<String, String> = emptyMap(),
        val body: String? = null,
        val binary: Boolean = false,
    )

    data class ResponseSpec(
        val status: Int,
        val url: String,
        val headers: Map<String, List<String>>,
        val body: String,
    )

    /** 同步执行一次请求；调用方负责放到 IO 线程 */
    fun execute(spec: RequestSpec): ResponseSpec {
        val httpUrl = spec.url.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("无效的请求地址，只支持 http/https")
        val method = spec.method.uppercase(Locale.ROOT)
        if (spec.body != null && (method == "GET" || method == "HEAD"))
            throw IllegalArgumentException("$method 请求不能带 body")

        val builder = Request.Builder().url(httpUrl)
        for ((name, value) in spec.headers)
            builder.addHeader(name, value)
        builder.method(method, spec.body?.toRequestBody(mediaTypeOf(spec.headers)))

        client.newCall(builder.build()).execute().use { response ->
            return toSpec(response, spec.binary)
        }
    }

    private fun toSpec(response: Response, binary: Boolean): ResponseSpec {
        val bytes = response.body?.bytes() ?: ByteArray(0)
        val body = if (binary)
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        else
            String(bytes, Charsets.UTF_8)
        return ResponseSpec(
            status = response.code,
            url = response.request.url.toString(),
            headers = response.headers.toMultimap(),
            body = body,
        )
    }

    /** 请求体的 Content-Type 缺省时传 null（OkHttp 不额外补头） */
    private fun mediaTypeOf(headers: Map<String, String>) =
        headers.entries.firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }
            ?.value
            ?.toMediaTypeOrNull()

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
