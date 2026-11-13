package moe.tnxg.koma.data.education

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import moe.tnxg.koma.data.education.zhengfang.*
import moe.tnxg.koma.domain.model.*
import kotlin.test.*

class ZhengfangTest {
    @Test fun weeksRequireMondayContiguousSevenDayRanges() {
        val valid = Json.parseToJsonElement("""[{"zs":1,"rq":"2025-09-01/2025-09-07"},{"zs":"2","rq":"2025-09-08/2025-09-14"}]""")
        assertEquals(TermWeeksMeta("2025-09-01", 2), zfTermWeeks(valid))
        for (bad in listOf("[]", "null", """[{"zs":1,"rq":"2025-09-02/2025-09-08"}]""",
            """[{"zs":1,"rq":"2025-09-01/2025-09-05"}]""")) assertNull(zfTermWeeks(Json.parseToJsonElement(bad)))
    }
    @Test fun mergeUsesQueryWeekAndPreservesRawSource() {
        val rows = Json.parseToJsonElement("""[
            {"kcmc":"高等数学","xm":"李四","cdmc":"3-201","xqj":"1","jcs":"0304","zcd":"1,2"},
            {"kcmc":"高等数学","xm":"李四","cdmc":"3-201","xqj":"1","jcs":"3-4","zcd":"4-6周"},
            {"kcmc":"体育","xqj":"9","jcs":"5","zcd":"1"},
            {"kcmc":"英语","xqj":"3","jcs":"1","zcd":"1,看不懂"}]
        """)
        val result = parseZfWeekly(rows)
        assertEquals(listOf(1, 2, 4, 5, 6), result.courses.first().weeks)
        assertEquals("1,2", result.courses.first().source["zcd"])
        assertEquals(listOf("BAD_WEEKDAY", "WEEKS_PARTIAL"), result.diagnostics.map { it.code })
    }
    @Test fun invalidRowsProduceDiagnosticRatherThanCourse() {
        val result = parseZfWeekly(Json.parseToJsonElement("""[{"kcmc":" "},{"kcmc":"A","xqj":2,"jcs":"abc"},{"kcmc":"B","xqj":2,"jcs":"5","zcd":"某周"}]"""))
        assertTrue(result.courses.isEmpty())
        assertEquals(listOf("EMPTY_NAME", "BAD_PERIOD", "BAD_WEEKS", "EMPTY"), result.diagnostics.map { it.code })
    }
    @Test fun timeGridSkipsBadRowsWithoutRenumbering() {
        val rows = Json.parseToJsonElement("""[{"qssj":"08:00 - 08:45"},{"qssj":"oops"},{"qssj":"08:55","jssj":"09:40"}]""")
        assertEquals(listOf(TimeSlot(1, 480, 525), TimeSlot(3, 535, 580)), zfTimeGrid(rows))
        assertEquals(Term("2025", "3"), guessTerm(LocalDate.parse("2026-01-15")))
        assertEquals(Term("2026", "12"), guessTerm(LocalDate.parse("2026-03-01")))
        assertEquals("2025–2026 学年 第 1 学期", termLabel(Term("2025", "3")))
    }
}
