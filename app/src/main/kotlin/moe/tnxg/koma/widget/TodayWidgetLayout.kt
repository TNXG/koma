package moe.tnxg.koma.widget

internal enum class WidgetMode { Today, Agenda }

internal enum class OverflowSlot { None, Footer, Header }

internal data class WidgetMetrics(
    val horizontalInsets: Int,
    val verticalInsets: Int,
    val dayGap: Int,
    val colGap: Int,
    val dayHeaderHeight: Int,
    val courseGap: Int,
    val compactRowHeight: Int,
    val wideRowHeight: Int,
    val footerHeight: Int,
)

internal data class DayCell(
    val index: Int,
    val capacity: Int,
    val overflow: OverflowSlot,
    val wideRow: Boolean,
    val showWeek: Boolean,
    val fullDate: Boolean,
    val highlightFocus: Boolean,
)

internal object TodayWidgetLayout {
    private const val WIDE_ROW_MIN_WIDTH_DP = 185
    private const val FULL_DATE_MIN_WIDTH_DP = 165
    private const val MIN_AGENDA_COL_WIDTH_DP = 112
    private const val MAX_COURSES_PER_CELL = 8
    private const val MAX_AGENDA_COLUMNS = 3
    private const val MAX_AGENDA_BANDS = 3
    private const val MIN_TODAY_ROWS_IN_AGENDA = 3

    fun maxExtraDays(mode: WidgetMode, width: Int, metrics: WidgetMetrics): Int {
        if (mode == WidgetMode.Today) return 0
        val columns = agendaColumns(width - metrics.horizontalInsets, metrics)
        return columns * MAX_AGENDA_BANDS - 1
    }

    fun plan(
        mode: WidgetMode,
        width: Int,
        height: Int,
        metrics: WidgetMetrics,
        dayCount: Int,
        courseCount: (Int) -> Int,
        weekOf: (Int) -> Int?,
    ): List<List<DayCell>> {
        val contentWidth = (width - metrics.horizontalInsets).coerceAtLeast(80)
        val contentHeight = (height - metrics.verticalInsets).coerceAtLeast(60)
        if (mode == WidgetMode.Today || dayCount <= 1 || courseCount(0) == 0) {
            return planSingleDay(contentWidth, contentHeight, metrics, courseCount(0), weekOf(0))
        }
        return planAgenda(contentWidth, contentHeight, metrics, dayCount, courseCount, weekOf)
    }

    private fun planSingleDay(
        contentWidth: Int,
        contentHeight: Int,
        metrics: WidgetMetrics,
        totalCourses: Int,
        week: Int?,
    ): List<List<DayCell>> {
        val wideRow = contentWidth >= WIDE_ROW_MIN_WIDTH_DP
        val rowHeight = if (wideRow) metrics.wideRowHeight else metrics.compactRowHeight
        val (capacity, overflow) = fitCourses(totalCourses, contentHeight - metrics.dayHeaderHeight, rowHeight, metrics)
        return listOf(
            listOf(
                DayCell(
                    index = 0,
                    capacity = capacity,
                    overflow = overflow,
                    wideRow = wideRow,
                    showWeek = week != null,
                    fullDate = contentWidth >= FULL_DATE_MIN_WIDTH_DP,
                    highlightFocus = true,
                ),
            ),
        )
    }

    private fun planAgenda(
        contentWidth: Int,
        contentHeight: Int,
        metrics: WidgetMetrics,
        dayCount: Int,
        courseCount: (Int) -> Int,
        weekOf: (Int) -> Int?,
    ): List<List<DayCell>> {
        val columns = agendaColumns(contentWidth, metrics)
        val cellWidth = (contentWidth - metrics.colGap * (columns - 1)) / columns
        val wideRow = columns == 1 && cellWidth >= WIDE_ROW_MIN_WIDTH_DP
        val fullDate = cellWidth >= FULL_DATE_MIN_WIDTH_DP
        val rowHeight = if (wideRow) metrics.wideRowHeight else metrics.compactRowHeight
        val groups = (0 until dayCount).chunked(columns).take(MAX_AGENDA_BANDS)

        val bands = mutableListOf<List<DayCell>>()
        var remaining = contentHeight
        var lastWeek: Int? = null

        for (groupIndex in groups.indices) {
            val group = groups[groupIndex]
            val demand = group.maxOf { courseCount(it).coerceIn(1, MAX_COURSES_PER_CELL) }
            val nextGroup = groups.getOrNull(groupIndex + 1)
            val bandBudget = allocateBandBudget(
                isFirstBand = groupIndex == 0,
                demand = demand,
                remaining = remaining,
                nextDemand = nextGroup?.maxOf { courseCount(it).coerceIn(1, MAX_COURSES_PER_CELL) },
                metrics = metrics,
                rowHeight = rowHeight,
            )

            val room = bandBudget - metrics.dayHeaderHeight
            val cells = group.map { dayIndex ->
                val dayWeek = weekOf(dayIndex)
                val showWeek = dayWeek != null && dayWeek != lastWeek
                if (dayWeek != null) lastWeek = dayWeek
                val (capacity, overflow) = fitCourses(courseCount(dayIndex), room, rowHeight, metrics)
                DayCell(
                    index = dayIndex,
                    capacity = capacity,
                    overflow = overflow,
                    wideRow = wideRow,
                    showWeek = showWeek,
                    fullDate = fullDate,
                    highlightFocus = groupIndex == 0 && dayIndex == 0,
                )
            }
            bands += cells

            remaining -= bandBudget + metrics.dayGap
            if (nextGroup == null || remaining < minBandHeight(nextGroup, courseCount, metrics, rowHeight)) break
        }
        return bands
    }

    private fun allocateBandBudget(
        isFirstBand: Boolean,
        demand: Int,
        remaining: Int,
        nextDemand: Int?,
        metrics: WidgetMetrics,
        rowHeight: Int,
    ): Int {
        if (nextDemand == null) return remaining
        val minNextRows = minOf(2, nextDemand)
        val minNextHeight = metrics.dayGap + metrics.dayHeaderHeight + coursesHeight(minNextRows, rowHeight, metrics.courseGap)
        val idealHeight = metrics.dayHeaderHeight + coursesHeight(demand, rowHeight, metrics.courseGap)
        if (remaining - idealHeight >= minNextHeight) return idealHeight

        val minSelfRows = if (isFirstBand) minOf(demand, MIN_TODAY_ROWS_IN_AGENDA) else minOf(demand, 2)
        val availableForSelf = remaining - minNextHeight
        val minSelfHeight = metrics.dayHeaderHeight + coursesHeight(minSelfRows, rowHeight, metrics.courseGap)
        if (availableForSelf >= minSelfHeight) return availableForSelf
        return remaining
    }

    private fun fitCourses(
        totalCourses: Int,
        room: Int,
        rowHeight: Int,
        metrics: WidgetMetrics,
    ): Pair<Int, OverflowSlot> {
        if (totalCourses <= 0) return 0 to OverflowSlot.None
        val fullFit = rowsForHeight(room, rowHeight, metrics.courseGap)
        if (totalCourses <= fullFit) return totalCourses to OverflowSlot.None
        return if (room >= rowHeight + metrics.footerHeight) {
            rowsForHeight(room - metrics.footerHeight, rowHeight, metrics.courseGap) to OverflowSlot.Footer
        } else {
            1 to OverflowSlot.Header
        }
    }

    private fun minBandHeight(
        group: List<Int>,
        courseCount: (Int) -> Int,
        metrics: WidgetMetrics,
        rowHeight: Int,
    ): Int {
        val nextDemand = group.maxOf { courseCount(it).coerceIn(1, MAX_COURSES_PER_CELL) }
        return metrics.dayHeaderHeight + coursesHeight(minOf(2, nextDemand), rowHeight, metrics.courseGap)
    }

    private fun agendaColumns(contentWidth: Int, metrics: WidgetMetrics): Int {
        val minWidth = maxOf(MIN_AGENDA_COL_WIDTH_DP, metrics.dayHeaderHeight * 5)
        return ((contentWidth + metrics.colGap) / (minWidth + metrics.colGap)).coerceIn(1, MAX_AGENDA_COLUMNS)
    }

    private fun coursesHeight(rows: Int, rowHeight: Int, courseGap: Int): Int =
        (rows * (rowHeight + courseGap) - courseGap).coerceAtLeast(0)

    private fun rowsForHeight(height: Int, rowHeight: Int, courseGap: Int): Int =
        ((height + courseGap) / (rowHeight + courseGap)).coerceIn(1, MAX_COURSES_PER_CELL)
}
