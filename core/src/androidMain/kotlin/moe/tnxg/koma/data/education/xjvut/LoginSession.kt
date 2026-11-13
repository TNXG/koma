package moe.tnxg.koma.data.education.xjvut

import java.io.IOException
import java.util.Base64
import kotlinx.serialization.json.*
import moe.tnxg.koma.data.education.*
import moe.tnxg.koma.data.education.cas.*
import moe.tnxg.koma.data.education.http.*
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

internal class LoginSession(private val http: EduHttp, private val vault: CookieVault) {
    private data class PublicKey(val execution: String, val modulus: String, val exponent: String)
    private var key: PublicKey? = null
    var ready: Boolean = false
        private set

    suspend fun restore(): Boolean {
        ready = Endpoints.isIndex(http.follow(Endpoints.SSO).url)
        return ready
    }
    suspend fun prepare(): LoginPreparation {
        key = null
        if (restore()) return LoginPreparation.Ready
        vault.clear()
        val page = http.follow(Endpoints.LOGIN)
        key = loadPublicKey(page.text)
        val status = http.call("${Endpoints.CAS}/cas/getKaptchaStatus")
        return LoginPreparation.Form(if (status.text.contains("true", ignoreCase = true)) refreshCaptcha() else null)
    }

    private suspend fun loadPublicKey(html: String): PublicKey {
        val execution = extractExecution(html) ?: throw IOException("登录页没有就绪")
        val response = http.call("${Endpoints.CAS}/cas/v1/getPubKey", "{}".toRequestBody("application/json".toMediaType()),
            mapOf("X-Requested-With" to "XMLHttpRequest"))
        val public = try { Json.parseToJsonElement(response.text).jsonObject } catch (_: IllegalArgumentException) {
            throw IOException("拿不到登录公钥")
        }
        val modulus = (public["modulus"] as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)
            ?: throw IOException("拿不到登录公钥")
        val exponent = (public["exponent"] as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) ?: "10001"
        return PublicKey(execution, modulus, exponent)
    }
    suspend fun refreshCaptcha(): String {
        if (key == null) throw IOException("登录还没准备好")
        val image = http.call("${Endpoints.CAS}/cas/kaptcha?time=${System.currentTimeMillis()}")
        if (image.bytes.isEmpty()) throw IOException("拿不到验证码")
        val type = image.headers["Content-Type"]?.substringBefore(';')?.trim() ?: "image/png"
        if (!type.startsWith("image/", ignoreCase = true)) throw IOException("拿不到验证码")
        return "data:$type;base64,${Base64.getEncoder().encodeToString(image.bytes)}"
    }
    suspend fun login(username: String, password: String, captcha: String): LoginResult {
        val public = key ?: return LoginResult(false, "请先开始登录")
        val body = FormBody.Builder().add("username", username)
            .add("password", encryptCasPassword(password, public.modulus, public.exponent))
            .add("code", "").add("authcode", captcha).add("execution", public.execution)
            .add("_eventId", "submit").add("type", "zhmm").build()
        key = null
        val response = http.follow(Endpoints.LOGIN, body, mapOf("Referer" to Endpoints.LOGIN))
        if (extractExecution(response.text) != null) {
            val raw = casErrorText(response.text)
            ready = false
            key = loadPublicKey(response.text)
            return LoginResult(false, describeCasError(raw), isCaptchaError(raw))
        }
        if (!restore()) {
            reset()
            return LoginResult(false, "教务登录没接上，请重试")
        }
        return LoginResult(true)
    }
    fun invalidate() { ready = false; key = null }
    fun reset() { invalidate(); vault.clear() }
}
