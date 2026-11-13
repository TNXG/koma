package moe.tnxg.koma.data.education.zhengfang

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.*
import moe.tnxg.koma.domain.calendar.*
import moe.tnxg.koma.domain.model.Term
import moe.tnxg.koma.domain.model.TimeSlot

internal fun JsonElement?.field(name: String): String = ((this as? JsonObject)?.get(name) as? JsonPrimitive)?.contentOrNull.orEmpty()

data class TermWeeksMeta(val startDate: String, val totalWeeks: Int)
fun zfTermWeeks(input: JsonElement?): TermWeeksMeta? {
    val weeks = input as? JsonArray ?: return null
    if (weeks.isEmpty() || weeks.size > 60) return null
    var first: LocalDate? = null
    weeks.forEachIndexed { index, row ->
        if (row.field("zs").toDoubleOrNull() != (index + 1).toDouble()) return null
        val range = row.field("rq").split('/')
        if (range.size != 2) return null
        val start = parseDate(range[0]) ?: return null
        val end = parseDate(range[1]) ?: return null
        if (index == 0) {
            if (isoWeekday(start) != 1) return null
            first = start
        } else if (daysBetween(first ?: return null, start) != index * 7) return null
        if (daysBetween(start, end) != 6) return null
    }
    return TermWeeksMeta(first?.toString() ?: return null, weeks.size)
}
private fun parseDate(value: String): LocalDate? {
    if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(value)) return null
    return try { LocalDate.parse(value) } catch (_: IllegalArgumentException) { null }
}

fun termLabel(term: Term): String {
    val index = when (term.xqm) { "3" -> "1"; "12" -> "2"; "16" -> "3"; else -> term.xqm }
    val next = term.xnm.toIntOrNull()?.plus(1)?.toString() ?: "NaN"
    return "${term.xnm}–$next 学年 第 $index 学期"
}
fun guessTerm(now: LocalDate = today()): Term = when {
    now.monthNumber >= 9 -> Term(now.year.toString(), "3")
    now.monthNumber >= 2 -> Term(now.year.toString(), "12")
    else -> Term((now.year - 1).toString(), "3")
}

private val clock = Regex("\\d{1,2}:\\d{2}")
fun zfTimeGrid(input: JsonElement?): List<TimeSlot> = (input as? JsonArray)?.mapIndexedNotNull { index, row ->
    if (row !is JsonObject) return@mapIndexedNotNull null
    val times = listOf(row.field("qssj"), row.field("jssj")).flatMap { clock.findAll(it).map { match -> match.value }.toList() }
        .mapNotNull { time ->
            val pair = time.split(':')
            val hour = pair[0].toInt()
            val minute = pair[1].toInt()
            if (hour in 0..23 && minute in 0..59) hour * 60 + minute else null
        }
    if (times.isEmpty()) null else TimeSlot(index + 1, times.min(), times.max())
}.orEmpty()

val DEFAULT_TIME_GRID: List<TimeSlot> = listOf(
    TimeSlot(1, 480, 525), TimeSlot(2, 535, 580), TimeSlot(3, 600, 645), TimeSlot(4, 655, 700),
    TimeSlot(5, 840, 885), TimeSlot(6, 895, 940), TimeSlot(7, 950, 995), TimeSlot(8, 1005, 1050),
    TimeSlot(9, 1140, 1185), TimeSlot(10, 1195, 1240),
)
