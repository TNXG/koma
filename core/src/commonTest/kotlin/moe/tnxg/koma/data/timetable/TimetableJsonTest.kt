package moe.tnxg.koma.data.timetable

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import moe.tnxg.koma.domain.model.Course
import moe.tnxg.koma.domain.model.Semester
import moe.tnxg.koma.domain.model.Term
import moe.tnxg.koma.domain.model.TimeSlot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimetableJsonTest {
    @Test fun convertsValidatedJsonWithoutDefaultSchoolTimes() {
        val snapshot = parseTimetableJson(validJson, "2026-11-17T10:00:00Z")
        assertEquals("local-json", snapshot.schoolId)
        assertEquals(Term("2026-09-07", "秋季学期"), snapshot.term)
        assertEquals(Semester("秋季学期", "2026-09-07", 18), snapshot.semester)
        assertEquals(listOf(TimeSlot(1, 490, 535), TimeSlot(2, 545, 590)), snapshot.timeGrid)
        assertEquals(listOf(Course(
            id = "高等数学|||2|1|2", name = "高等数学", teacher = null, location = null,
            weekday = 2, startPeriod = 1, endPeriod = 2, weeks = listOf(1, 3, 5),
            source = mapOf("pluginId" to "local-json"), note = "带教材",
        )), snapshot.courses)
        assertEquals("同学", snapshot.studentName)
        assertEquals("2026-11-17T10:00:00Z", snapshot.importedAt)
    }

    @Test fun acceptsOptionalFieldsAndEmptyCourseList() {
        val course = """{"name":"英语","weekday":7,"startPeriod":2,"endPeriod":2,"weeks":[18]}"""
        val snapshot = parseTimetableJson(withCourses(course))
        assertEquals("英语", snapshot.courses.single().name)
        assertEquals("", snapshot.courses.single().note)
        assertEquals(null, snapshot.courses.single().teacher)
        assertEquals(null, parseTimetableJson(withRoot("studentName", "null")).studentName)
        assertTrue(parseTimetableJson(withCourses()).courses.isEmpty())
    }

    @Test fun preservesDistinctSchedulesForSameCourseName() {
        val second = courseJson.replace("\"weekday\":2", "\"weekday\":3")
        val snapshot = parseTimetableJson(withCourses(courseJson, second))
        assertEquals(listOf(2, 3), snapshot.courses.map { it.weekday })
        assertEquals(2, snapshot.courses.map { it.id }.distinct().size)
    }

    @Test fun rejectsMissingRequiredFieldsAndUnsupportedVersions() {
        listOf("version", "semester", "timeGrid", "courses").forEach { field ->
            assertInvalid(withoutRoot(field), field)
        }
        listOf("0", "2", "null", "\"1\"", "1.0", "true").forEach { version ->
            assertInvalid(withRoot("version", version), "version")
        }
        listOf("name", "startDate", "totalWeeks").forEach { field ->
            val semester = Json.parseToJsonElement(semesterJson) as kotlinx.serialization.json.JsonObject
            val missing = buildJsonObject { semester.forEach { (key, value) -> if (key != field) put(key, value) } }
            assertInvalid(withRoot("semester", missing.toString()), "semester.$field")
        }
    }

    @Test fun rejectsInvalidSemesterDatesAndWeekCounts() {
        listOf("2026-02-30", "2025-02-29", "2026-13-01", "2026-9-07", "not-a-date").forEach { date ->
            assertInvalid(validJson.replace("2026-09-07", date), "semester.startDate")
        }
        listOf("0", "61", "\"18\"", "18.5", "null").forEach { weeks ->
            assertInvalid(validJson.replace("\"totalWeeks\":18", "\"totalWeeks\":$weeks"), "semester.totalWeeks")
        }
        assertInvalid(validJson.replace("秋季学期", " "), "semester.name")
        assertInvalid(validJson.replace("2026-09-07", "2026-09-08"), "semester.startDate")
        parseTimetableJson(validJson.replace("2026-09-07", "2016-02-29"))
    }

    @Test fun rejectsInvalidClockTimesAndOverlappingSlots() {
        listOf("24:00", "08:60", "8:10", "-1:10", "08:10:00").forEach { time ->
            assertInvalid(validJson.replace("08:10", time), "timeGrid[0].start")
        }
        assertInvalid(validJson.replace("08:55", "08:10"), "timeGrid[0].end")
        assertInvalid(validJson.replace("09:05", "08:50"), "timeGrid[1].start")
        assertInvalid(validJson.replace("09:50", "08:00"), "timeGrid[1].end")
        assertEquals(2, parseTimetableJson(validJson.replace("09:05", "08:55")).timeGrid.size)
    }

    @Test fun rejectsMissingNoncontiguousAndOversizedGrids() {
        assertInvalid(withRoot("timeGrid", "[]"), "timeGrid")
        assertInvalid(withRoot("timeGrid", "null"), "timeGrid")
        assertInvalid(validJson.replace("\"index\":2", "\"index\":3"), "timeGrid[1].index")
        assertInvalid(validJson.replace("\"index\":1", "\"index\":0"), "timeGrid[0].index")
        val slot = Json.parseToJsonElement("""{"index":1,"start":"08:00","end":"08:45"}""")
        assertInvalid(withRoot("timeGrid", JsonArray(List(31) { slot }).toString()), "timeGrid")
    }

    @Test fun rejectsInvalidCourseFieldsAndWeeks() {
        listOf(
            "\"name\":\" \"" to "name",
            "\"weekday\":0" to "weekday",
            "\"weekday\":8" to "weekday",
            "\"weekday\":\"2\"" to "weekday",
            "\"startPeriod\":0" to "startPeriod",
            "\"startPeriod\":3" to "startPeriod",
            "\"endPeriod\":3" to "endPeriod",
            "\"startPeriod\":2,\"endPeriod\":1" to "endPeriod",
            "\"weeks\":[]" to "weeks",
            "\"weeks\":[0]" to "weeks[0]",
            "\"weeks\":[19]" to "weeks[0]",
            "\"weeks\":[1.5]" to "weeks[0]",
            "\"weeks\":[\"1\"]" to "weeks[0]",
            "\"teacher\":23" to "teacher",
            "\"location\":[]" to "location",
            "\"note\":null" to "note",
        ).forEach { (field, path) ->
            val minimal = """{"name":"课程","weekday":2,"startPeriod":1,"endPeriod":2,"weeks":[1]}"""
            val row = Json.parseToJsonElement(minimal) as kotlinx.serialization.json.JsonObject
            val changes = Json.parseToJsonElement("{$field}") as kotlinx.serialization.json.JsonObject
            val changed = buildJsonObject { row.forEach { (key, value) -> put(key, value) }; changes.forEach { (key, value) -> put(key, value) } }
            assertInvalid(withCourses(changed.toString()), "courses[0].$path")
        }
        val row = Json.parseToJsonElement(courseJson) as kotlinx.serialization.json.JsonObject
        listOf("name", "weekday", "startPeriod", "endPeriod", "weeks").forEach { field ->
            val missing = buildJsonObject { row.forEach { (key, value) -> if (key != field) put(key, value) } }
            assertInvalid(withCourses(missing.toString()), "courses[0].$field")
        }
    }

    @Test fun rejectsUnknownFieldsAtEveryObjectLevel() {
        assertInvalid(withRoot("school", "\"unknown\""), "school")
        assertInvalid(validJson.replace("\"totalWeeks\":18", "\"totalWeeks\":18,\"weeks\":18"), "semester.weeks")
        assertInvalid(validJson.replace("\"index\":1", "\"index\":1,\"label\":\"一\""), "timeGrid[0].label")
        assertInvalid(withCourses(courseJson.dropLast(1) + ",\"week\":[1]}"), "courses[0].week")
    }

    @Test fun rejectsDuplicateSchedulesRatherThanLosingWeeksOrNotes() {
        assertInvalid(withCourses(courseJson, courseJson), "courses[1]")
        assertInvalid(withCourses(courseJson, courseJson.replace("[5,1,3,3]", "[2,4]")), "courses[1]")
        assertInvalid(withCourses(courseJson, courseJson.replace("带教材", "另一条备注")), "courses[1]")
        assertInvalid(withCourses(courseJson, courseJson.replace(" 高等数学 ", "高等数学")), "courses[1]")
    }

    @Test fun rejectsSyntaxWithoutEchoingPrivateInput() {
        val privateInput = "private-student-record-".repeat(200)
        listOf("{\"$privateInput\":", "[]", "null").forEach { raw ->
            val error = assertFailsWith<IllegalArgumentException> { parseTimetableJson(raw) }
            assertTrue(error.message.orEmpty().isNotBlank())
            assertTrue(error.message.orEmpty().length < 300)
            assertFalse(error.message.orEmpty().contains(privateInput))
        }
    }

    @Test fun acceptsJsonInsideAiCodeFence() {
        val direct = parseTimetableJson(validJson)
        val fenced = parseTimetableJson("```json\n$validJson\n```")
        val bare = parseTimetableJson("```\n$validJson\n```")
        assertEquals(direct.semester, fenced.semester)
        assertEquals(direct.courses, fenced.courses)
        assertEquals(direct.semester, bare.semester)
    }

    @Test fun exampleIsImportableAndCarriesExplicitTimes() {
        val example = parseTimetableJson(JSON_TIMETABLE_EXAMPLE)
        assertTrue(example.courses.isNotEmpty())
        assertTrue(example.timeGrid.isNotEmpty())
        assertEquals("local-json", example.schoolId)
        assertTrue(example.courses.all { course -> course.startPeriod in 1..example.timeGrid.size && course.endPeriod in 1..example.timeGrid.size })
    }

    private fun assertInvalid(raw: String, path: String) {
        val error = assertFailsWith<IllegalArgumentException>(path) { parseTimetableJson(raw) }
        assertTrue(error.message.orEmpty().contains(path), "字段路径应包含 $path，实际为 ${error.message}")
    }

    private fun withRoot(field: String, value: String): String = buildJsonObject {
        val root = Json.parseToJsonElement(validJson) as kotlinx.serialization.json.JsonObject
        root.forEach { (key, item) -> put(key, item) }
        put(field, Json.parseToJsonElement(value))
    }.toString()

    private fun withoutRoot(field: String): String = buildJsonObject {
        val root = Json.parseToJsonElement(validJson) as kotlinx.serialization.json.JsonObject
        root.forEach { (key, item) -> if (key != field) put(key, item) }
    }.toString()

    private fun withCourses(vararg rows: String): String = withRoot("courses", rows.joinToString(prefix = "[", postfix = "]"))

    private val semesterJson = """{"name":" 秋季学期 ","startDate":"2026-09-07","totalWeeks":18}"""
    private val courseJson = """{"name":" 高等数学 ","teacher":" ","location":"","weekday":2,"startPeriod":1,"endPeriod":2,"weeks":[5,1,3,3],"note":"带教材"}"""
    private val validJson get() = """{"version":1,"semester":$semesterJson,"timeGrid":[{"index":1,"start":"08:10","end":"08:55"},{"index":2,"start":"09:05","end":"09:50"}],"courses":[$courseJson],"studentName":" 同学 "}"""
}
