package moe.tnxg.koma.domain.period

data class PeriodRange(val start: Int, val end: Int)
fun parsePeriodRange(input: String): PeriodRange? {
    val text = input.trim()
    val interval = Regex("^(\\d{1,2})\\s*-\\s*(\\d{1,2})$").matchEntire(text)
    val values = when {
        interval != null -> listOf(interval.groupValues[1].toInt(), interval.groupValues[2].toInt())
        text.length >= 4 && text.length % 2 == 0 && text.all { it in '0'..'9' } -> text.chunked(2).map(String::toInt)
        Regex("^\\d{1,2}$").matches(text) -> listOf(text.toInt())
        else -> return null
    }
    val start = values.minOrNull() ?: return null
    val end = values.maxOrNull() ?: return null
    return if (start >= 1 && end <= 30) PeriodRange(start, end) else null
}
