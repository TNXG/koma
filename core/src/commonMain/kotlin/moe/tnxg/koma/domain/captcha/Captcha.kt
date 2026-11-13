package moe.tnxg.koma.domain.captcha

private val arithmetic = Regex("(\\d{1,2})([+-])(\\d{1,2})")
fun captchaAnswer(text: String): String? {
    val clean = buildString {
        text.forEach { char ->
            val mapped = when (char) {
                'x', 'X', '×', '＊', '*', '十', '＋' -> '+'
                '一', '－', '—' -> '-'
                'q', 'Q', '﹖', '？' -> '?'
                else -> char
            }
            if (mapped in '0'..'9' || mapped in "+-=?") append(mapped)
        }
    }.substringBefore('=')
    var answer: String? = null
    var length = 0
    arithmetic.findAll(clean).forEach { match ->
        val a = match.groupValues[1].toInt()
        val b = match.groupValues[3].toInt()
        val value = if (match.groupValues[2] == "+") a + b else a - b
        if (a in 1..19 && b in 1..19 && value in 0..38 && match.value.length > length) {
            length = match.value.length
            answer = value.toString()
        }
    }
    return answer
}
