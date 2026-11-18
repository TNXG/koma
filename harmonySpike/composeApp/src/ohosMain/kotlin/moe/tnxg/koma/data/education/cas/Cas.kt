package moe.tnxg.koma.data.education.cas

import moe.tnxg.koma.domain.model.Term

// jsoup 的窄用法以正则移植（CAS 登录页/教务页结构稳定）。

private val INPUT_EXECUTION = Regex(
    "<input[^>]*name=[\"']execution[\"'][^>]*>", RegexOption.IGNORE_CASE
)
private val VALUE_ATTR = Regex("value=[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
private val ID_INPUT_VALUE = Regex(
    "<input[^>]*id=[\"']%s[\"'][^>]*>", RegexOption.IGNORE_CASE
)
private val ERROR_ELEMENT = Regex(
    "<\\w+[^>]*class=[\"'][^\"']*error[^\"']*\"[^>]*>(.*?)</\\w+>",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)
private val SELECT_BLOCK = Regex("<select[^>]*id=[\"']%s[\"'][^>]*>(.*?)</select>",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val SELECTED_OPTION_VALUE = Regex(
    "<option[^>]*(selected[^>]*value=[\"']([^\"']*)[\"']|value=[\"']([^\"']*)[\"'][^>]*selected)[^>]*>",
    RegexOption.IGNORE_CASE
)
private val TAGS = Regex("<[^>]*>")

fun extractExecution(html: String): String? = INPUT_EXECUTION.findAll(html)
    .map { VALUE_ATTR.find(it.value)?.groupValues?.get(1).orEmpty() }
    .firstOrNull(String::isNotEmpty)

fun casErrorText(html: String): String = ERROR_ELEMENT.findAll(html)
    .map { it.groupValues[1].replace(TAGS, "").trim() }
    .firstOrNull(String::isNotEmpty).orEmpty()

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
    val modulus = BigInt.fromHex(modulusHex)
    val exponent = BigInt.fromHex(exponentHex)
    val chunk = 2 * (modulus.bitLength() / 16 - 1)
    require(chunk > 0 && !modulus.isZero && !exponent.isZero) { "公钥不对" }
    // security.js reverses code points, then reads the leading UTF-16 unit of each.
    val chars = password.map { it.code }.toMutableList()
    while (chars.size % chunk != 0) chars += 0
    return chars.chunked(chunk).joinToString(" ") { block ->
        var message = BigInt.ZERO
        for (value in block.asReversed()) message = (message.shl(8)) or BigInt.valueOf(value.toLong())
        val hex = message.modPow(exponent, modulus).toHexString()
        hex.padStart((hex.length + 3) / 4 * 4, '0')
    }
}

private fun inputValueById(html: String, id: String): String? {
    val tag = Regex("<input[^>]*id=[\"']" + Regex.escape(id) + "[\"'][^>]*>", RegexOption.IGNORE_CASE)
        .find(html)?.value ?: return null
    return VALUE_ATTR.find(tag)?.groupValues?.get(1)?.takeIf(String::isNotEmpty)
}

private fun selectedOptionValue(html: String, id: String): String? {
    val block = Regex("<select[^>]*id=[\"']" + Regex.escape(id) + "[\"'][^>]*>(.*?)</select>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(html)?.groupValues?.get(1) ?: return null
    val option = Regex("<option[^>]*>", RegexOption.IGNORE_CASE).findAll(block)
        .firstOrNull { it.value.contains("selected", ignoreCase = true) }?.value ?: return null
    return VALUE_ATTR.find(option)?.groupValues?.get(1)?.takeIf(String::isNotEmpty)
}

fun termFromIndex(html: String): Term? {
    fun value(hidden: String, select: String): String? = inputValueById(html, hidden)
        ?: selectedOptionValue(html, select)
    val year = value("xnm_hide", "xnm") ?: return null
    val semester = value("xqm_hide", "xqm") ?: return null
    return Term(year, semester)
}
