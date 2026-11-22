package moe.tnxg.koma.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.os.BundleCompat
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import moe.tnxg.koma.R
import moe.tnxg.koma.app.preferences.AppPreferences
import moe.tnxg.koma.app.preferences.ThemePreference
import moe.tnxg.koma.domain.calendar.addDays
import moe.tnxg.koma.domain.calendar.formatTime
import moe.tnxg.koma.domain.calendar.weekdayLabel
import moe.tnxg.koma.domain.timeline.AgendaCourse
import moe.tnxg.koma.domain.timeline.AgendaState
import moe.tnxg.koma.domain.timeline.ClassState
import moe.tnxg.koma.domain.timeline.TodayAgenda
import moe.tnxg.koma.domain.visual.courseVisual
import moe.tnxg.koma.ui.theme.KomaColors
import moe.tnxg.koma.ui.theme.courseTone
import moe.tnxg.koma.ui.theme.komaColors
import kotlin.math.ceil
import kotlin.math.roundToInt

internal class TodayWidgetViews(
    private val context: Context,
    private val today: LocalDate,
    private val agenda: TodayAgenda,
    private val prefs: AppPreferences,
    private val openApp: PendingIntent,
    private val loadMoreDays: (Int) -> List<TodayAgenda>,
) {
    private val dualTheme = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && prefs.theme == ThemePreference.System
    private val dark = when (prefs.theme) {
        ThemePreference.Dark -> true
        ThemePreference.Light -> false
        ThemePreference.System -> !dualTheme && context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
    private val colors = komaColors(dark, prefs.accent)
    private val nightColors = if (dualTheme) komaColors(true, prefs.accent) else null
    private enum class Overflow { None, Header, Footer }
    private data class DayCell(val index: Int, val courses: Int, val overflow: Overflow)

    private val variants = mutableMapOf<List<List<DayCell>>, RemoteViews>()
    private var extraDays: List<TodayAgenda> = emptyList()
    private var requestedDays = 0
    private val horizontalInsets = dimensionDp(R.dimen.widget_inset_start) + dimensionDp(R.dimen.widget_inset_end)
    private val verticalInsets = 2 * dimensionDp(R.dimen.widget_inset_vertical)
    private val dayGap = dimensionDp(R.dimen.widget_day_gap)
    private val dayHeaderHeight = dimensionDp(R.dimen.widget_day_header_gap) + lineHeight(11)
    private val rowHeight = maxOf(35, dimensionDp(R.dimen.widget_course_details_gap) + lineHeight(13) + lineHeight(10))
    private val footerHeight = 1 + lineHeight(10)

    fun forOptions(options: Bundle, defaultWidth: Int, defaultHeight: Int): RemoteViews {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val sizes = BundleCompat.getParcelableArrayList(options, AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java)
            if (!sizes.isNullOrEmpty() && sizes.size <= 16) {
                return RemoteViews(sizes.associateWith { forSize(it.width.toInt(), it.height.toInt(), defaultHeight) })
            }
        }
        val landscape = forSize(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, defaultWidth),
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, defaultHeight), defaultHeight)
        val portrait = forSize(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, defaultWidth),
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, defaultHeight), defaultHeight)
        return if (landscape === portrait) portrait else RemoteViews(landscape, portrait)
    }

    private fun lineHeight(sp: Int): Int {
        val metrics = context.resources.displayMetrics
        val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp.toFloat(), metrics)
        // CJK fallback fonts can produce taller lines than Inter's Latin metrics.
        return ceil(px * 1.45f / metrics.density).toInt()
    }

    private fun dimensionDp(id: Int): Int =
        (context.resources.getDimension(id) / context.resources.displayMetrics.density).roundToInt()

    private fun dayAt(index: Int): TodayAgenda = if (index == 0) agenda else extraDays[index - 1]

    private fun forSize(width: Int, height: Int, defaultHeight: Int): RemoteViews {
        val contentWidth = width - horizontalInsets
        val contentHeight = height - verticalInsets
        val minCellWidth = maxOf(104, lineHeight(11) * 6)
        val columns = ((contentWidth + dayGap) / (minCellWidth + dayGap)).coerceIn(1, 4)
        val minBandHeight = dayHeaderHeight + rowHeight
        val defaultRows = ((defaultHeight - verticalInsets - dayHeaderHeight) / rowHeight).coerceAtLeast(1)
        val firstBandHeight = dayHeaderHeight + rowHeight * minOf(defaultRows, agenda.courses.size.coerceAtLeast(1))
        val bands = if (height < defaultHeight + rowHeight + dayGap) 1
        else (1 + (contentHeight - firstBandHeight).coerceAtLeast(0) / (minBandHeight + dayGap)).coerceAtMost(6)
        val limit = if (agenda.courses.isEmpty()) 0 else columns * bands - 1
        if (limit > requestedDays && (requestedDays == 0 || extraDays.size == requestedDays)) {
            extraDays = loadMoreDays(limit)
            requestedDays = limit
        }
        val dayCount = 1 + minOf(limit, extraDays.size)
        val groups = (0 until dayCount).chunked(columns)
        var remaining = contentHeight - dayGap * (groups.size - 1)
        val plan = groups.mapIndexed { groupIndex, group ->
            // Keep one row for every later date before spending extra space on busier days.
            val later = (groups.lastIndex - groupIndex) * minBandHeight
            val demand = dayHeaderHeight + rowHeight * group.maxOf { dayAt(it).courses.size.coerceAtMost(8) }
            val bandHeight = minOf(demand, (remaining - later).coerceAtLeast(minBandHeight))
            val budget = if (groups.size == 1) contentHeight else bandHeight
            remaining -= budget
            val cellWidth = (contentWidth - dayGap * (group.size - 1)) / group.size
            group.map { dayCell(it, cellWidth, budget) }
        }
        return variants.getOrPut(plan) { render(plan) }
    }

    private fun dayCell(index: Int, width: Int, height: Int): DayCell {
        val total = dayAt(index).courses.size
        val room = height - dayHeaderHeight
        val withoutFooter = (room / rowHeight).coerceIn(1, 8)
        val overflow = when {
            total <= withoutFooter -> Overflow.None
            width >= maxOf(150, lineHeight(11) * 9) -> Overflow.Header
            room >= rowHeight + footerHeight -> Overflow.Footer
            else -> Overflow.None
        }
        val rows = if (overflow == Overflow.Footer) ((room - footerHeight) / rowHeight).coerceIn(1, 8) else withoutFooter
        return DayCell(index, minOf(total, rows), overflow)
    }

    private fun render(plan: List<List<DayCell>>): RemoteViews =
        RemoteViews(context.packageName, R.layout.today_widget).apply {
            setOnClickPendingIntent(android.R.id.background, openApp)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val surface = ColorStateList.valueOf(colors.surface.toArgb())
                val night = nightColors
                if (night == null) setColorStateList(android.R.id.background, "setBackgroundTintList", surface)
                else setColorStateList(android.R.id.background, "setBackgroundTintList", surface,
                    ColorStateList.valueOf(night.surface.toArgb()))
            } else {
                paint(R.id.widget_panel, "setBackgroundColor") { it.surface }
            }
            val single = plan.size == 1 && plan.first().size == 1
            setViewVisibility(R.id.widget_single_day, if (single) View.VISIBLE else View.GONE)
            setViewVisibility(R.id.widget_day_grid, if (single) View.GONE else View.VISIBLE)
            removeAllViews(R.id.widget_day_grid)
            if (single) {
                bindDay(plan.first().first())
            } else {
                for (band in plan) {
                    val row = RemoteViews(context.packageName, R.layout.today_widget_day_band)
                    for (cell in band) {
                        val day = RemoteViews(context.packageName, R.layout.today_widget_day).apply { bindDay(cell) }
                        row.addView(R.id.widget_day_band, day)
                    }
                    addView(R.id.widget_day_grid, row)
                }
            }
        }

    private fun RemoteViews.bindDay(cell: DayCell) {
        val day = dayAt(cell.index)
        val (_, capacity, overflow) = cell
        val date = day.date
        val monthDay = "${date.month.number}/${date.day}"
        text(R.id.widget_date, if (date == addDays(today, 1)) context.getString(R.string.widget_tomorrow, monthDay)
        else context.getString(R.string.widget_date, monthDay, weekdayLabel(date)))
        ink(R.id.widget_date) { it.ink }
        text(R.id.widget_week, day.week?.let { context.getString(R.string.widget_week, it) })
        ink(R.id.widget_week) { it.accent }
        val more = if (overflow != Overflow.None) context.getString(R.string.widget_overflow, day.courses.size - capacity) else null
        text(R.id.widget_header_overflow, more.takeIf { overflow == Overflow.Header })
        ink(R.id.widget_header_overflow) { it.ink2 }
        setViewVisibility(R.id.widget_rows, if (capacity > 0) View.VISIBLE else View.GONE)
        removeAllViews(R.id.widget_rows)
        for (entry in day.visibleCourses(capacity)) addView(R.id.widget_rows, courseRow(entry))
        text(R.id.widget_overflow, more.takeIf { overflow == Overflow.Footer })
        ink(R.id.widget_overflow) { it.ink2 }
        setViewVisibility(R.id.widget_empty, if (capacity == 0) View.VISIBLE else View.GONE)
        if (capacity == 0) {
            val message = when (day.state) {
                AgendaState.NoTimetable -> R.string.widget_no_timetable
                AgendaState.NeedsStart -> R.string.widget_needs_start
                AgendaState.BeforeTerm -> R.string.widget_before_term
                AgendaState.AfterTerm -> R.string.widget_after_term
                else -> R.string.widget_no_classes
            }
            val hint = when (day.state) {
                AgendaState.NoTimetable -> R.string.widget_import_hint
                AgendaState.NeedsStart -> R.string.widget_start_hint
                else -> null
            }
            text(R.id.widget_message, context.getString(message))
            ink(R.id.widget_message) { it.ink }
            text(R.id.widget_hint, hint?.let { context.getString(it) })
            ink(R.id.widget_hint) { it.ink2 }
        }
    }

    private fun courseRow(entry: AgendaCourse): RemoteViews {
        val (time, state) = entry
        val (course, start) = time
        val clock = when {
            state == ClassState.Current -> context.getString(R.string.widget_class_now)
            state == ClassState.Finished -> context.getString(R.string.widget_class_finished)
            start != null -> formatTime(start)
            course.startPeriod == course.endPeriod -> context.getString(R.string.widget_period, course.startPeriod)
            else -> context.getString(R.string.widget_period_range, course.startPeriod, course.endPeriod)
        }
        val room = if (prefs.showRoom) course.location?.trim()?.takeIf(String::isNotEmpty) else null
        val details = if (room != null) "$clock · $room" else clock
        val tone = if (prefs.colorCourses) courseVisual(course.name).tone else null
        return RemoteViews(context.packageName, R.layout.today_widget_course).apply {
            paint(R.id.widget_course_mark, "setBackgroundColor") {
                if (state == ClassState.Finished) it.line else if (tone != null) it.courseTone(tone) else it.accent
            }
            text(R.id.widget_course_name, course.name)
            ink(R.id.widget_course_name) { if (state == ClassState.Finished) it.ink2 else it.ink }
            text(R.id.widget_course_details, details)
            ink(R.id.widget_course_details) { if (state == ClassState.Current) it.accent else it.ink2 }
        }
    }

    private fun RemoteViews.text(id: Int, value: CharSequence?) {
        setTextViewText(id, value)
        setViewVisibility(id, if (value != null) View.VISIBLE else View.GONE)
    }

    private inline fun RemoteViews.ink(id: Int, pick: (KomaColors) -> Color) = paint(id, "setTextColor", pick)

    private inline fun RemoteViews.paint(id: Int, method: String, pick: (KomaColors) -> Color) {
        val normal = pick(colors).toArgb()
        val night = nightColors
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && night != null) {
            setColorInt(id, method, normal, pick(night).toArgb())
        } else {
            setInt(id, method, normal)
        }
    }
}
