package moe.tnxg.koma.data.education.xjvut

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.coroutines.CancellationException
import moe.tnxg.koma.data.education.EduService
import moe.tnxg.koma.data.education.LoginPreparation
import moe.tnxg.koma.data.education.LoginResult
import moe.tnxg.koma.data.education.cas.*
import moe.tnxg.koma.data.education.http.*

internal class LoginSession(private val http: EduHttp, private val vault: CookieVault) {
    private data class PublicKey(val execution: String, val modulus: String, val exponent: String)
    private var key: PublicKey? = null
    var ready: Boolean = false
        private set

    suspend fun restore(): Boolean {
        ready = false
        val entry = http.follow(Endpoints.SSO, stopAt = Endpoints::isLogin)
        if (Endpoints.isIndex(entry.url)) {
            ready = true
            return true
        }
        val cas = entry["Location"]?.let { EduHttp.resolve(entry.url, it) }?.takeIf(Endpoints::isLogin) ?: return false
        // Only inspect CAS here: an authenticated no-service login redirects to
        // the default portal, whose session is unrelated to the education service.
        val probe = http.call(Endpoints.LOGIN)
        if (extractExecution(probe.text) != null) return false
        // Existing CAS-only sessions must still be able to mint a JW service ticket.
        ready = Endpoints.isIndex(http.follow(cas.toString()).url)
        return ready
    }

    suspend fun prepare(): LoginPreparation {
        key = null
        if (restore()) return LoginPreparation.Ready
        vault.clear()
        try {
            val page = http.follow(Endpoints.LOGIN)
            key = loadPublicKey(page.text)
            val status = http.call("${Endpoints.CAS}/cas/getKaptchaStatus")
            return LoginPreparation.Form(if (status.text.contains("true", ignoreCase = true)) refreshCaptcha() else null)
        } catch (error: EduHttpException) {
            throw IoException(explain(error), error)
        }
    }

    private suspend fun loadPublicKey(html: String): PublicKey {
        val execution = extractExecution(html) ?: throw IoException("登录页没有就绪")
        val response = http.call("${Endpoints.CAS}/cas/v1/getPubKey",
            EduBody.Raw("{}", "application/json"),
            mapOf("X-Requested-With" to "XMLHttpRequest"))
        val public = try { Json.parseToJsonElement(response.text).jsonObject } catch (_: IllegalArgumentException) {
            throw IoException("拿不到登录公钥")
        }
        val modulus = (public["modulus"] as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)
            ?: throw IoException("拿不到登录公钥")
        val exponent = (public["exponent"] as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) ?: "10001"
        return PublicKey(execution, modulus, exponent)
    }

    suspend fun refreshCaptcha(): String {
        if (key == null) throw IoException("登录还没准备好")
        val image = http.call("${Endpoints.CAS}/cas/kaptcha?time=${nowMillis()}")
        if (image.bytes.isEmpty()) throw IoException("拿不到验证码")
        val type = image["Content-Type"]?.substringBefore(';')?.trim() ?: "image/png"
        if (!type.startsWith("image/", ignoreCase = true)) throw IoException("拿不到验证码")
        return "data:$type;base64,${kotlin.io.encoding.Base64.Default.encode(image.bytes)}"
    }

    suspend fun login(username: String, password: String, captcha: String): LoginResult {
        val public = key ?: return LoginResult(false, "请先开始登录")
        println("KomaEdu login-submit endpoint=/cas/login modulusBits=${BigInt.fromHex(public.modulus).bitLength()} exponentLength=${public.exponent.length}")
        val body = EduBody.Form(listOf(
            "username" to username,
            "password" to encryptCasPassword(password, public.modulus, public.exponent),
            "code" to "", "authcode" to captcha, "execution" to public.execution,
            "_eventId" to "submit", "type" to "zhmm",
        ))
        key = null
        val response = try {
            http.call(Endpoints.LOGIN, body, mapOf("Referer" to Endpoints.LOGIN))
        } catch (error: EduHttpException) {
            reportLoginResponse(error.status, error.body)
            // CAS also renders form failures with HTTP 401; retain its execution and captcha classification.
            ready = false
            key = try {
                val page = error.body.takeIf { extractExecution(it) != null } ?: http.call(Endpoints.LOGIN).text
                loadPublicKey(page)
            }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { null }
            return LoginResult(false, explain(error), isCaptchaError(error.body))
        }
        reportLoginResponse(response.status, response.text)
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

    // Temporary boundary diagnostics: only public key dimensions and a fixed
    // error category. Never print response text, submitted fields or cookies.
    private fun reportLoginResponse(status: Int, html: String) {
        val reason = casErrorText(html)
        val category = when {
            isCaptchaError(reason) -> "captcha"
            reason.contains("not recognized", ignoreCase = true) -> "account"
            Regex("flow|execution", RegexOption.IGNORE_CASE).containsMatchIn(reason) -> "session"
            reason.isEmpty() -> "none"
            else -> "other"
        }
        println("KomaEdu login-response endpoint=/cas/login status=$status error=$category")
    }

    /** Prefer the reason the auth server rendered; fall back to what the status itself means. */
    private fun explain(error: EduHttpException): String {
        val text = casErrorText(error.body)
        if (text.isNotEmpty()) return describeCasError(text)
        return when (error.status) {
            401, 403 -> "账号或密码不对"
            423 -> "登录被暂时锁定，请稍后重试"
            429 -> "登录太频繁，请稍后重试"
            404 -> "教务登录页已更新，请更新应用后重试"
            in 500..599 -> "教务服务暂时不可用，请稍后重试"
            else -> "登录没有完成，请稍后重试"
        }
    }

    fun invalidate() { ready = false; key = null }
    suspend fun reset() { invalidate(); vault.clear() }
}
