package moe.tnxg.koma.feature.calendar

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DateSheetMonthPageTest {
    @Test
    fun consecutiveMonthsDifferByOnePageAcrossYearBoundary() {
        val december = LocalDate(2025, 12, 1)
        val january = LocalDate(2026, 1, 1)
        assertEquals(1, calendarPageOf(january) - calendarPageOf(december))
        assertEquals(january, calendarMonthAt(calendarPageOf(january)))
    }

    @Test
    fun anyDayMapsBackToTheSameMonthPage() {
        assertEquals(LocalDate(2026, 11, 1), calendarMonthAt(calendarPageOf(LocalDate(2026, 11, 14))))
    }

    @Test
    fun pagesOutsideTheSupportedRangeClamp() {
        assertEquals(0, calendarPageOf(LocalDate(1899, 12, 31)))
        assertEquals(CALENDAR_MONTH_PAGES - 1, calendarPageOf(LocalDate(2100, 1, 1)))
        assertEquals(CALENDAR_MONTH_BASE, calendarMonthAt(-3))
        assertEquals(calendarMonthAt(CALENDAR_MONTH_PAGES - 1), calendarMonthAt(CALENDAR_MONTH_PAGES + 5))
    }
}
