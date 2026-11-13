package moe.tnxg.koma.data.education.cas

import java.math.BigInteger
import org.jsoup.Jsoup
import moe.tnxg.koma.domain.model.Term

fun extractExecution(html: String): String? = Jsoup.parse(html).selectFirst("input[name=execution]")
    ?.attr("value")?.takeIf(String::isNotEmpty)
fun casErrorText(html: String): String = Jsoup.parse(html).select("[class]")
    .firstOrNull { it.className().contains("error", ignoreCase = true) }?.text()?.trim().orEmpty()
fun isCaptchaError(raw: String): Boolean = raw.contains("FailedAuthcodeException", ignoreCase = true)
fun describeCasError(raw: String): String = when {
    isCaptchaError(raw) -> "验证码不对"
    raw.contains("not recognized", ignoreCase = true) -> "账号或密码不对"
    Regex("flow|execution", RegexOption.IGNORE_CASE).containsMatchIn(raw) -> "登录过期了，请重试"
    Regex("没有响应|超时").containsMatchIn(raw) -> "登录没有响应，请重试"
    raw.trim().length in 1..60 -> raw.trim()
    else -> "登录失败，请重试"
}

/** The CAS security.js protocol is raw modular RSA, not PKCS#1 padded RSA. */
fun encryptCasPassword(password: String, modulusHex: String, exponentHex: String): String {
    val modulus = BigInteger(modulusHex, 16)
    val exponent = BigInteger(exponentHex, 16)
    val chunk = 2 * (modulus.bitLength() / 16 - 1)
    require(chunk > 0 && modulus.signum() > 0 && exponent.signum() > 0) { "公钥不对" }
    // security.js reverses code points, then reads the leading UTF-16 unit of each.
    val chars = password.codePoints().toArray().reversed().map { Character.toChars(it)[0].code }.toMutableList()
    while (chars.size % chunk != 0) chars += 0
    return chars.chunked(chunk).joinToString(" ") { block ->
        var message = BigInteger.ZERO
        for (value in block.asReversed()) message = message.shiftLeft(8).or(BigInteger.valueOf(value.toLong()))
        val hex = message.modPow(exponent, modulus).toString(16)
        hex.padStart((hex.length + 3) / 4 * 4, '0')
    }
}

fun termFromIndex(html: String): Term? {
    val document = Jsoup.parse(html)
    fun value(hidden: String, select: String): String? = document.getElementById(hidden)?.attr("value")?.takeIf(String::isNotEmpty)
        ?: document.getElementById(select)?.selectFirst("option[selected]")?.attr("value")?.takeIf(String::isNotEmpty)
    val year = value("xnm_hide", "xnm") ?: return null
    val semester = value("xqm_hide", "xqm") ?: return null
    return Term(year, semester)
}
