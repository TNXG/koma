package moe.tnxg.koma.domain.weeks

data class WeeksParseResult(val weeks: List<Int>, val partial: Boolean)
private val range = Regex("^(\\d+)\\s*-\\s*(\\d+)\\s*(?:周\\s*)?(?:\\(\\s*(单|双)\\s*\\))?$")
private val single = Regex("^(\\d+)\\s*周?$")

fun parseWeeksString(input: String): WeeksParseResult {
    val weeks = mutableSetOf<Int>()
    var partial = false
    input.split(',', '，', ';', '；').forEach { raw ->
        val segment = raw.trim().removePrefix("第").trim()
        if (segment.isEmpty()) return@forEach
        val interval = range.matchEntire(segment)
        val lone = single.matchEntire(segment)
        if (interval != null) {
            val from = interval.groupValues[1].toIntOrNull()
            val to = interval.groupValues[2].toIntOrNull()
            if (from == null || to == null || from < 1 || to < from || to > 60) partial = true
            else weeks += (from..to).filter { when (interval.groupValues[3]) { "单" -> it % 2 == 1; "双" -> it % 2 == 0; else -> true } }
        } else if (lone != null) {
            val value = lone.groupValues[1].toIntOrNull()
            if (value != null && value in 1..60) weeks += value else partial = true
        } else partial = true
    }
    return WeeksParseResult(weeks.sorted(), partial)
}

fun weeksToString(weeks: List<Int>): String = weeks.sorted().joinToString(",")
