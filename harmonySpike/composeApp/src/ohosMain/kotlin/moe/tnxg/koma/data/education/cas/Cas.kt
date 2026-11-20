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
    "<(\\w+)\\b[^>]*\\bclass\\s*=\\s*([\"'])[^\"']*error[^\"']*\\2[^>]*>(.*?)</\\1\\s*>",
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
    .map { it.groupValues[3].replace(TAGS, "").trim() }
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
    val chunk = 2 * ((modulus.bitLength() - 1) / 16)
    require(chunk > 0 && !modulus.isZero && !exponent.isZero) { "公钥不对" }
    // cas.js reverses UTF-16 units before security.js packs little-endian blocks.
    val chars = password.map { it.code }.asReversed().toMutableList()
    while (chars.size % chunk != 0) chars += 0
    return chars.chunked(chunk).joinToString(" ") { block ->
        val hex = encryptCasBlock(block, exponent, modulus).toHexString()
        hex.padStart((hex.length + 3) / 4 * 4, '0')
    }
}

private fun fromCasDigits(digits: IntArray, carry: Int = 0): BigInt {
    var value = BigInt.valueOf(carry.toLong())
    for (i in digits.indices.reversed()) {
        value = value.shl(16) or BigInt.valueOf(digits[i].toLong())
    }
    return value
}

private fun encryptCasBlock(block: List<Int>, exponent: BigInt, modulus: BigInt): BigInt {
    // security.js adds two UTF-16 units into each nominally 16-bit digit.
    val digits = IntArray(block.size / 2) { block[2 * it] + (block[2 * it + 1] shl 8) }
    if (digits.all { it <= 0xffff }) return fromCasDigits(digits).modPow(exponent, modulus)

    // Non-byte characters can overflow those digits. The deployed JS multiplies
    // them once before they are canonical, truncating each carry with >>> 16.
    // Ordinary integer packing/modPow (including bytewise OR) is not equivalent.
    var count = digits.size
    while (count > 0 && digits[count - 1] == 0) count--
    val square = IntArray(2 * count)
    if (exponent.bitLength() > 1) {
        for (i in 0 until count) {
            var carry = 0L
            for (j in 0 until count) {
                val product = square[i + j].toLong() + digits[i].toLong() * digits[j] + carry
                square[i + j] = (product and 0xffff).toInt()
                carry = (product ushr 16) and 0xffff
            }
            square[i + count] = carry.toInt()
        }
    }
    var carry = 0
    for (i in digits.indices) {
        val digit = digits[i] + carry
        digits[i] = digit and 0xffff
        carry = digit ushr 16
    }
    var result = if (exponent.testBit(0)) fromCasDigits(digits, carry).mod(modulus) else BigInt.ONE
    if (exponent.bitLength() == 1) return result
    var base = fromCasDigits(square).mod(modulus)
    for (bit in 1 until exponent.bitLength()) {
        if (exponent.testBit(bit)) result = (result * base).mod(modulus)
        if (bit + 1 < exponent.bitLength()) base = (base * base).mod(modulus)
    }
    return result
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
