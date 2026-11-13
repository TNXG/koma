package moe.tnxg.koma.domain

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import moe.tnxg.koma.domain.calendar.*
import moe.tnxg.koma.domain.captcha.captchaAnswer
import moe.tnxg.koma.domain.model.*
import moe.tnxg.koma.domain.period.*
import moe.tnxg.koma.domain.timeline.*
import moe.tnxg.koma.domain.weeks.*
import moe.tnxg.koma.domain.visual.*
import moe.tnxg.koma.domain.text.cleanText
import kotlin.test.*

class DomainTest {
    @Test fun courseIdentityUsesOrderedKeywordsAndStableFallbackHash() {
        assertEquals(CourseVisual(CourseIcon.Translate, CourseTone.Blue), courseVisual("英语程序设计"))
        assertEquals(CourseVisual(CourseIcon.Code, CourseTone.Teal), courseVisual(" JAVA "))
        assertEquals(CourseVisual(CourseIcon.Book, CourseTone.Amber), courseVisual("A"))
        assertEquals("课程 名称", cleanText(" \n课程\u00a0 名称\t"))
    }
    @Test fun independentWeekSegmentsRetainValidParts() {
        assertEquals(listOf(1, 3, 4, 6, 10), parseWeeksString("第1-4周(单),3-6周(双);10").weeks)
        assertEquals(WeeksParseResult(listOf(1, 2, 3, 7), true), parseWeeksString("1-3,看不懂,7"))
        listOf("0-5", "61-65", "8-1").forEach { assertTrue(parseWeeksString(it).partial) }
    }
    @Test fun periodsHandleEncodedListsAndRejectOutOfRange() {
        assertEquals(PeriodRange(3, 6), parsePeriodRange("030506"))
        assertEquals(PeriodRange(3, 4), parsePeriodRange("4-3"))
        listOf("", "abc", "0", "999", "31").forEach { assertNull(parsePeriodRange(it)) }
    }
    @Test fun captchaRejectsDecorationAndOutOfDomainOperands() {
        val cases = mapOf("13x192" to "32", "14+194" to "33", "L16-2-2" to "14",
            "一4-3->" to "1", "19-10-2" to "9", "11-11-2" to "0", "2+10=1+1" to "12",
            "49x5=2" to null, "14-0=2" to null, "=2+10" to null, "158=2" to null, "" to null)
        cases.forEach { (input, answer) -> assertEquals(answer, captchaAnswer(input), input) }
    }
    @Test fun calendarUsesNaturalDaysAndNegativeWeeks() {
        assertEquals(-1, weekIndex("2025-09-01", LocalDate.parse("2025-08-18")))
        assertEquals(LocalDate.parse("2025-09-01"), mondayOf(LocalDate.parse("2025-09-07")))
        assertEquals(2, daysBetween(LocalDate.parse("2025-03-29"), LocalDate.parse("2025-03-31")))
    }
    @Test fun timelineMarksBoundariesAndMapsFreeDates() {
        val date = LocalDate.parse("2025-09-01")
        val snapshot = TimetableSnapshot("xjvut", Term("2025", "3"), Semester("秋", date.toString(), 1),
            listOf(TimeSlot(1, 480, 525), TimeSlot(2, 600, 645)), listOf(
                Course("a", "数学", weekday = 1, startPeriod = 1, endPeriod = 1, weeks = listOf(1)),
                Course("b", "英语", weekday = 1, startPeriod = 2, endPeriod = 2, weeks = listOf(1))), importedAt = "")
        val blocks = buildTimeline(snapshot, date.toString(), date, 525)
        val monday = blocks.filterIsInstance<DayBlock>().first()
        assertEquals(listOf(RowState.Past, RowState.Next), monday.courses.map { it.state })
        assertEquals(GapStop(525, 600, 75, RowState.Now), monday.stops[1])
        assertEquals("2025-09-02", blockAnchors(blocks)["2025-09-07"])
        assertEquals(listOf("第 1 周", "单周", "2 节课，剩 1 节"), describeDay(snapshot, date.toString(), date, date, 525).parts)
        assertEquals(snapshot, Json.decodeFromString<TimetableSnapshot>(Json.encodeToString(TimetableSnapshot.serializer(), snapshot)))
    }
}
