package moe.tnxg.koma.data.education.zhengfang

import kotlinx.serialization.json.*
import moe.tnxg.koma.domain.model.Course
import moe.tnxg.koma.domain.model.courseIdOf
import moe.tnxg.koma.domain.period.parsePeriodRange
import moe.tnxg.koma.domain.text.cleanText
import moe.tnxg.koma.domain.weeks.parseWeeksString

data class Diagnostic(val code: String, val level: String, val message: String, val row: Int)
data class WeeklyParseResult(val courses: List<Course>, val diagnostics: List<Diagnostic>)

fun parseZfWeekly(input: JsonElement?): WeeklyParseResult {
    val diagnostics = mutableListOf<Diagnostic>()
    val merged = linkedMapOf<String, Course>()
    (input as? JsonArray).orEmpty().forEachIndexed { row, item ->
        fun warn(code: String, message: String) { diagnostics += Diagnostic(code, "warn", message, row) }
        fun clean(key: String): String = cleanText(item.field(key))
        val name = clean("kcmc")
        if (name.isEmpty()) { warn("EMPTY_NAME", "课程名为空，已跳过"); return@forEachIndexed }
        val weekdayNumber = clean("xqj").toDoubleOrNull()
        val weekday = weekdayNumber?.toInt()
        if (weekday == null || weekday !in 1..7 || weekdayNumber != weekday.toDouble()) {
            warn("BAD_WEEKDAY", "星期「${clean("xqj")}」不在 1–7，已跳过"); return@forEachIndexed
        }
        val period = parsePeriodRange(clean("jcs"))
        if (period == null) { warn("BAD_PERIOD", "节次「${clean("jcs")}」无法解析，已跳过"); return@forEachIndexed }
        val weeks = parseWeeksString(clean("zcd"))
        if (weeks.weeks.isEmpty()) { warn("BAD_WEEKS", "周次「${clean("zcd")}」无法解析，已跳过"); return@forEachIndexed }
        if (weeks.partial) diagnostics += Diagnostic("WEEKS_PARTIAL", "info", "周次「${clean("zcd")}」部分段无法解析，已保留其余周", row)
        val teacher = clean("xm").ifEmpty { null }
        val location = clean("cdmc").ifEmpty { null }
        val id = courseIdOf(name, teacher, location, weekday, period.start, period.end)
        val previous = merged[id]
        merged[id] = if (previous != null) previous.copy(weeks = (previous.weeks + weeks.weeks).distinct().sorted()) else Course(
            id, name, teacher, location, weekday, period.start, period.end, weeks.weeks,
            buildMap {
                put("pluginId", "xjvut")
                listOf("kcmc", "xqj", "jcs", "zcd").forEach { key ->
                    if ((item as? JsonObject)?.get(key) is JsonPrimitive) put(key, item.field(key))
                }
            },
        )
    }
    if (merged.isEmpty()) diagnostics += Diagnostic("EMPTY", "error", "接口没有返回任何课程", 0)
    return WeeklyParseResult(merged.values.toList(), diagnostics)
}
