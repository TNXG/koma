package moe.tnxg.koma.data.education.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.ParametersBuilder
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.decodeURLPart
import io.ktor.http.encodeURLParameter

/** OHOS 端 IO 异常基类（Kotlin/Native 无 java.io.IOException）。 */
internal open class EduIoException(message: String, cause: Throwable? = null) : Exception(message, cause)

internal typealias IoException = EduIoException

internal fun nowMillis(): Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

internal data class HttpResult(val status: Int, val url: Url, val headers: Map<String, String>, val bytes: ByteArray) {
    val text: String get() = bytes.decodeToString()
    operator fun get(name: String): String? = headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
}

/** Carries the rejected response so the login layer can show the server's own reason. */
internal class EduHttpException(val status: Int, val url: Url, val body: String) :
    EduIoException(httpStatusText(status))

private fun httpStatusText(status: Int): String = when (status) {
    401, 403 -> "登录已失效，请重新登录"
    404 -> "教务接口不存在，请更新应用后重试"
    423 -> "登录被暂时锁定，请稍后重试"
    429 -> "请求太频繁，请稍后重试"
    in 500..599 -> "教务服务暂时不可用，请稍后重试"
    else -> "教务请求没有完成，请稍后重试"
}

/** 请求体：表单（替代 okhttp FormBody）或原始文本（替代 toRequestBody）。 */
internal sealed interface EduBody {
    data class Form(val fields: List<Pair<String, String>>) : EduBody
    data class Raw(val text: String, val contentType: String) : EduBody

    companion object {
        fun form(vararg fields: Pair<String, String>): Form = Form(fields.toList())
    }
}

internal class EduHttp(private val client: HttpClient) {

    suspend fun call(url: String, body: EduBody? = null, headers: Map<String, String> = emptyMap()): HttpResult {
        val target = Url(url)
        require(target.protocol == URLProtocol.HTTPS) { "登录只允许安全连接" }
        val response: HttpResponse = client.request(target) {
            method = if (body != null) HttpMethod.Post else HttpMethod.Get
            headers.forEach { (key, value) -> this@request.header(key, value) }
            if (body != null) {
                when (body) {
                    is EduBody.Form -> {
                        setBody(body.fields.joinToString("&") { (k, v) ->
                            k.encodeURLParameter() + "=" + v.encodeURLParameter()
                        })
                        contentType(ContentType.Application.FormUrlEncoded)
                    }
                    is EduBody.Raw -> {
                        setBody(body.text)
                        contentType(ContentType.parse(body.contentType))
                    }
                }
            }
        }
        val status = response.status.value
        val bytes = response.readBytes()
        val flat = response.headers.entries().associate { (name, values) -> name to values.firstOrNull().orEmpty() }
        val result = HttpResult(status, response.call.request.url, flat, bytes)
        if (result.status >= 400) throw EduHttpException(result.status, result.url, result.text)
        return result
    }

    /** A stopped redirect returns its response without requesting the destination. */
    suspend fun follow(
        url: String,
        body: EduBody? = null,
        headers: Map<String, String> = emptyMap(),
        stopAt: (Url) -> Boolean = { false },
    ): HttpResult {
        var target = Url(url)
        var nextBody = body
        var nextHeaders = headers
        for (hop in 0..MAX_REDIRECTS) {
            val result = call(target.toString(), nextBody, nextHeaders)
            if (result.status !in REDIRECT_STATUSES) return result
            val location = result["Location"] ?: return result
            val destination = resolve(result.url, location) ?: throw EduIoException("登录跳转地址无效")
            if (destination.protocol != URLProtocol.HTTPS) throw EduIoException("登录只允许安全连接")
            val sameOrigin = result.url.protocol == destination.protocol && result.url.host == destination.host &&
                result.url.port == destination.port
            // 307/308 preserve the method and payload. Never replay a credential-bearing
            // payload on another origin; silently changing it to GET is not equivalent.
            if (!sameOrigin && nextBody != null && result.status in 307..308) {
                throw EduIoException("登录跳转不能向其他站点发送表单")
            }
            if (stopAt(destination)) return result
            if (hop == MAX_REDIRECTS) throw EduIoException("登录跳转次数过多，请重试")
            if (result.status in 301..303) {
                nextBody = null
                nextHeaders = nextHeaders.filterKeys { name ->
                    !name.equals("Content-Type", true) && !name.equals("Content-Length", true) &&
                        !name.equals("Transfer-Encoding", true)
                }
            }
            // Headers are caller-supplied and can contain credentials under any name.
            // Destination cookies are selected independently by CookieVault on each call.
            if (!sameOrigin) nextHeaders = emptyMap()
            target = destination
        }
        error("Unreachable redirect state")
    }

    companion object {
        private const val MAX_REDIRECTS = 16
        private val REDIRECT_STATUSES = setOf(301, 302, 303, 307, 308)
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

        fun client(vault: CookieVault): HttpClient = HttpClient(Curl) {
            followRedirects = false
            expectSuccess = false
            // curl 引擎不带默认 UA；CAS/WAF 对无 UA 请求直接 500，Android 端每请求都带同一 UA。
            install(UserAgent) { agent = USER_AGENT }
            install(HttpCookies) { storage = vault }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
                requestTimeoutMillis = 60_000
            }
        }

        /** 手动解析 Location（okhttp HttpUrl.resolve 等价），支持绝对地址、协议相对与相对路径。 */
        internal fun resolve(base: Url, location: String): Url? = try {
            val href = location.trim()
            when {
                href.startsWith("https://", true) || href.startsWith("http://", true) -> Url(href)
                href.startsWith("//") -> Url("https:$href")
                else -> {
                    val pathOnly = href.substringBefore('?')
                    val query = if (href.contains('?')) href.substringAfter('?', "") else null
                    val baseWithoutQuery = base.toString().substringBefore('?')
                    val newPath = if (pathOnly.startsWith("/")) pathOnly else {
                        val dir = baseWithoutQuery.substringBeforeLast('/', missingDelimiterValue = baseWithoutQuery)
                        "$dir/$pathOnly"
                    }
                    Url(if (query.isNullOrEmpty()) newPath else "$newPath?$query")
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
