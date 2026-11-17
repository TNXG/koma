package moe.tnxg.koma.data.timetable

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import moe.tnxg.koma.domain.model.Course
import moe.tnxg.koma.domain.model.Semester
import moe.tnxg.koma.domain.model.Term
import moe.tnxg.koma.domain.model.TimeSlot
import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.domain.model.courseIdOf
import kotlin.time.Clock

/** Imports a timetable JSON atomically; invalid input throws a Chinese, field-addressed error. */
fun parseTimetableJson(raw: String, importedAt: String = Clock.System.now().toString()): TimetableSnapshot {
    val element = try {
        Json.parseToJsonElement(unwrapCodeFence(raw))
    } catch (_: IllegalArgumentException) {
        invalid("$", "内容不完整，请检查引号、逗号和括号是否齐全")
    }
    val root = record(element, "$", rootFields)
    if (integer(root["version"], "$.version") != 1) invalid("$.version", "只支持版本 1")
    val semester = parseSemester(root["semester"])
    val grid = parseGrid(root["timeGrid"])
    val rows = array(root["courses"], "$.courses")
    val identities = mutableSetOf<String>()
    val courses = rows.mapIndexed { index, row ->
        val path = "$.courses[$index]"
        val course = parseCourse(row, path, semester.totalWeeks, grid.size)
        if (!identities.add(course.id)) {
            invalid(path, "存在重复排课（课程名、教师、地点、星期和节次相同），请先合并周次并确认备注，不能直接重复导入")
        }
        course
    }
    return TimetableSnapshot(
        schoolId = LOCAL_JSON_SCHOOL_ID,
        term = Term(semester.startDate, semester.name),
        semester = semester,
        timeGrid = grid,
        courses = courses,
        studentName = root.optionalString("studentName", "$")?.trim()?.ifEmpty { null },
        importedAt = importedAt,
    )
}

private fun parseSemester(element: JsonElement?): Semester {
    val path = "$.semester"
    val row = record(element, path, semesterFields)
    val name = row.requiredString("name", path).trim()
    if (name.isEmpty()) invalid("$path.name", "学期名称不能为空")
    val startDate = row.requiredString("startDate", path)
    if (!datePattern.matches(startDate)) invalid("$path.startDate", "必须为 YYYY-MM-DD 格式的有效日期")
    val date = try {
        LocalDate.parse(startDate)
    } catch (_: IllegalArgumentException) {
        invalid("$path.startDate", "日期不存在，请检查年份、月份和日期")
    }
    if (date.dayOfWeek != DayOfWeek.MONDAY) {
        invalid("$path.startDate", "必须是第 1 教学周的周一日期，避免星期课程偏移")
    }
    val weeks = integer(row["totalWeeks"], "$path.totalWeeks")
    if (weeks !in 1..60) invalid("$path.totalWeeks", "总周数必须在 1–60 之间")
    return Semester(name, startDate, weeks)
}

private fun parseGrid(element: JsonElement?): List<TimeSlot> {
    val rows = array(element, "$.timeGrid")
    if (rows.isEmpty() || rows.size > 30) invalid("$.timeGrid", "必须提供 1–30 节真实作息时间，不能省略或使用默认钟点")
    var previousEnd = -1
    return rows.mapIndexed { position, item ->
        val path = "$.timeGrid[$position]"
        val row = record(item, path, gridFields)
        val index = integer(row["index"], "$path.index")
        if (index != position + 1) invalid("$path.index", "节次必须按数组顺序从 1 开始连续递增")
        val start = minutes(row.requiredString("start", path), "$path.start")
        val end = minutes(row.requiredString("end", path), "$path.end")
        if (end <= start) invalid("$path.end", "结束时间必须晚于开始时间，不能跨日")
        if (start < previousEnd) invalid("$path.start", "节次时间必须依次排列且不能与上一节重叠")
        previousEnd = end
        TimeSlot(index, start, end)
    }
}

private fun parseCourse(element: JsonElement, path: String, totalWeeks: Int, periodCount: Int): Course {
    val row = record(element, path, courseFields)
    val name = row.requiredString("name", path).trim()
    if (name.isEmpty()) invalid("$path.name", "课程名称不能为空")
    val teacher = row.optionalString("teacher", path)?.trim()?.ifEmpty { null }
    val location = row.optionalString("location", path)?.trim()?.ifEmpty { null }
    val weekday = integer(row["weekday"], "$path.weekday")
    if (weekday !in 1..7) invalid("$path.weekday", "星期必须在 1–7 之间（周一为 1，周日为 7）")
    val start = integer(row["startPeriod"], "$path.startPeriod")
    if (start !in 1..periodCount) invalid("$path.startPeriod", "开始节次必须落在 timeGrid 的 1–$periodCount 节内")
    val end = integer(row["endPeriod"], "$path.endPeriod")
    if (end !in start..periodCount) invalid("$path.endPeriod", "结束节次必须不早于开始节次，且落在 timeGrid 内")
    val weekRows = array(row["weeks"], "$path.weeks")
    if (weekRows.isEmpty()) invalid("$path.weeks", "周次不能为空，必须展开为整数数组")
    val weeks = weekRows.mapIndexed { index, value ->
        val weekPath = "$path.weeks[$index]"
        integer(value, weekPath).also { if (it !in 1..totalWeeks) invalid(weekPath, "周次必须在 1–$totalWeeks 之间") }
    }.distinct().sorted()
    val note = if ("note" in row) row.requiredString("note", path) else ""
    return Course(
        id = courseIdOf(name, teacher, location, weekday, start, end),
        name = name, teacher = teacher, location = location, weekday = weekday,
        startPeriod = start, endPeriod = end, weeks = weeks,
        source = localJsonSource, note = note,
    )
}

/** AI replies are commonly wrapped in a ```json fence; accept that instead of failing. */
private fun unwrapCodeFence(raw: String): String {
    val text = raw.trim()
    if (!text.startsWith("```")) return text
    val bodyStart = text.indexOf('\n') + 1
    if (bodyStart <= 0) return text
    val body = text.substring(bodyStart)
    val close = body.lastIndexOf("```")
    return if (close >= 0) body.substring(0, close).trim() else text
}

private fun record(element: JsonElement?, path: String, fields: Set<String>): JsonObject {
    val row = element as? JsonObject ?: invalid(path, "必填字段，必须是 JSON 对象")
    row.keys.firstOrNull { it !in fields }?.let { key ->
        invalid("$path.${key.take(60)}", "不支持此字段，请核对字段名；字段名区分大小写")
    }
    return row
}

private fun array(element: JsonElement?, path: String): JsonArray =
    element as? JsonArray ?: invalid(path, "必填字段，必须是 JSON 数组")

private fun JsonObject.requiredString(key: String, path: String): String {
    val value = get(key) as? JsonPrimitive
    if (value == null || !value.isString) invalid("$path.$key", "必填字段，必须是字符串")
    return value.content
}

private fun JsonObject.optionalString(key: String, path: String): String? {
    val element = get(key) ?: return null
    if (element == JsonNull) return null
    val value = element as? JsonPrimitive
    if (value == null || !value.isString) invalid("$path.$key", "必须是字符串或 null，也可以省略")
    return value.content
}

private fun integer(element: JsonElement?, path: String): Int {
    val value = element as? JsonPrimitive
    if (value == null || value.isString || !integerPattern.matches(value.content)) {
        invalid(path, "必填字段，必须是整数，不能使用字符串、小数或 null")
    }
    return value.intOrNull ?: invalid(path, "整数超出允许范围")
}

private fun minutes(value: String, path: String): Int {
    if (!timePattern.matches(value)) invalid(path, "必须为 HH:mm 格式的有效时间（00:00–23:59）")
    val hour = (value[0] - '0') * 10 + (value[1] - '0')
    val minute = (value[3] - '0') * 10 + (value[4] - '0')
    return hour * 60 + minute
}

private fun invalid(path: String, reason: String): Nothing = throw IllegalArgumentException("$path：$reason")

private val rootFields = setOf("version", "semester", "timeGrid", "courses", "studentName")
private val semesterFields = setOf("name", "startDate", "totalWeeks")
private val gridFields = setOf("index", "start", "end")
private val courseFields = setOf("name", "teacher", "location", "weekday", "startPeriod", "endPeriod", "weeks", "note")
private val datePattern = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")
private val timePattern = Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]")
private val integerPattern = Regex("-?(?:0|[1-9][0-9]*)")
private val localJsonSource = mapOf("pluginId" to "local-json")
