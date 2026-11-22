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

    private val variants = mutableMapOf<List<List<DayCell>>, RemoteViews>()
    private var extraDays: List<TodayAgenda> = emptyList()
    private var requestedDays = 0

    private val metrics = WidgetMetrics(
        horizontalInsets = 2 * dimensionDp(R.dimen.widget_inset_horizontal),
        verticalInsets = 2 * dimensionDp(R.dimen.widget_inset_vertical),
        dayGap = dimensionDp(R.dimen.widget_day_gap),
        colGap = dimensionDp(R.dimen.widget_col_gap),
        dayHeaderHeight = dimensionDp(R.dimen.widget_day_header_gap) + lineHeight(11.5f),
        courseGap = dimensionDp(R.dimen.widget_course_gap),
        compactRowHeight = maxOf(30, 3 + dimensionDp(R.dimen.widget_course_details_gap) + lineHeight(12.5f) + lineHeight(10.5f)),
        wideRowHeight = maxOf(32, 3 + dimensionDp(R.dimen.widget_course_details_gap) + lineHeight(13.5f) + lineHeight(10.5f)),
        footerHeight = 3 + lineHeight(10.5f),
    )

    fun forOptions(mode: WidgetMode, options: Bundle, defaultWidth: Int, defaultHeight: Int): RemoteViews {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val sizes = BundleCompat.getParcelableArrayList(options, AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java)
            if (!sizes.isNullOrEmpty() && sizes.size <= 16) {
                return RemoteViews(sizes.associateWith { forSize(mode, it.width.toInt(), it.height.toInt()) })
            }
        }
        val landscape = forSize(
            mode,
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, defaultWidth),
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, defaultHeight),
        )
        val portrait = forSize(
            mode,
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, defaultWidth),
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, defaultHeight),
        )
        return if (landscape === portrait) portrait else RemoteViews(landscape, portrait)
    }

    private fun lineHeight(sp: Float): Int {
        val displayMetrics = context.resources.displayMetrics
        val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, displayMetrics)
        return ceil(px * 1.15f / displayMetrics.density).toInt()
    }

    private fun dimensionDp(id: Int): Int =
        (context.resources.getDimension(id) / context.resources.displayMetrics.density).roundToInt()

    private fun dayAt(index: Int): TodayAgenda = if (index == 0) agenda else extraDays[index - 1]

    private fun forSize(mode: WidgetMode, width: Int, height: Int): RemoteViews {
        val limit = if (agenda.courses.isEmpty()) 0 else TodayWidgetLayout.maxExtraDays(mode, width, metrics)
        if (limit > requestedDays && (requestedDays == 0 || extraDays.size == requestedDays)) {
            extraDays = loadMoreDays(limit)
            requestedDays = limit
        }
        val dayCount = 1 + minOf(limit, extraDays.size)
        val plan = TodayWidgetLayout.plan(
            mode = mode,
            width = width,
            height = height,
            metrics = metrics,
            dayCount = dayCount,
            courseCount = { dayAt(it).courses.size },
            weekOf = { dayAt(it).week },
        )
        return variants.getOrPut(plan) { render(plan) }
    }

    private fun render(plan: List<List<DayCell>>): RemoteViews =
        RemoteViews(context.packageName, R.layout.today_widget).apply {
            setOnClickPendingIntent(android.R.id.background, openApp)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                paintTint(android.R.id.background, "setBackgroundTintList") { it.surface }
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
                plan.forEachIndexed { bandIndex, band ->
                    if (bandIndex > 0) addView(R.id.widget_day_grid, dividerView(R.layout.today_widget_divider_h))
                    val row = RemoteViews(context.packageName, R.layout.today_widget_day_band)
                    band.forEachIndexed { colIndex, cell ->
                        if (colIndex > 0) row.addView(R.id.widget_day_band, dividerView(R.layout.today_widget_divider_v))
                        val day = RemoteViews(context.packageName, R.layout.today_widget_day).apply { bindDay(cell) }
                        row.addView(R.id.widget_day_band, day)
                    }
                    addView(R.id.widget_day_grid, row)
                }
            }
        }

    private fun dividerView(layoutId: Int): RemoteViews =
        RemoteViews(context.packageName, layoutId).apply { paintImage(R.id.widget_divider) { it.line } }

    private fun RemoteViews.bindDay(cell: DayCell) {
        val day = dayAt(cell.index)
        val isToday = day.date == today
        val isTomorrow = day.date == addDays(today, 1)
        val monthDay = "${day.date.month.number}/${day.date.day}"
        val dateText = when {
            isToday && cell.fullDate -> context.getString(R.string.widget_today_full, monthDay, weekdayLabel(day.date))
            isToday -> context.getString(R.string.widget_today, monthDay)
            isTomorrow && cell.fullDate -> context.getString(R.string.widget_tomorrow_full, monthDay, weekdayLabel(day.date))
            isTomorrow -> context.getString(R.string.widget_tomorrow, monthDay)
            else -> context.getString(R.string.widget_date, monthDay, weekdayLabel(day.date))
        }
        text(R.id.widget_date, dateText)
        ink(R.id.widget_date) { if (isToday) it.accent else it.ink }

        text(R.id.widget_week, day.week.takeIf { cell.showWeek }?.let { context.getString(R.string.widget_week, it) })
        ink(R.id.widget_week) { it.ink3 }

        val hidden = (day.courses.size - cell.capacity).coerceAtLeast(0)
        val overflowText = if (hidden > 0) context.getString(R.string.widget_overflow, hidden) else null
        text(R.id.widget_header_overflow, overflowText.takeIf { cell.overflow == OverflowSlot.Header })
        ink(R.id.widget_header_overflow) { it.ink2 }

        setViewVisibility(R.id.widget_rows, if (cell.capacity > 0) View.VISIBLE else View.GONE)
        removeAllViews(R.id.widget_rows)
        val visible = day.visibleCourses(cell.capacity)
        val focusTarget = if (cell.highlightFocus && isToday) {
            visible.firstOrNull { it.state == ClassState.Current } ?: visible.firstOrNull { it.state == ClassState.Next }
        } else null
        for (entry in visible) {
            addView(R.id.widget_rows, courseRow(entry, cell.wideRow, entry === focusTarget))
        }
        text(R.id.widget_overflow, overflowText.takeIf { cell.overflow == OverflowSlot.Footer })
        ink(R.id.widget_overflow) { it.ink3 }

        setViewVisibility(R.id.widget_empty, if (cell.capacity == 0) View.VISIBLE else View.GONE)
        if (cell.capacity == 0) bindEmpty(day.state)
    }

    private fun RemoteViews.bindEmpty(state: AgendaState) {
        val message = when (state) {
            AgendaState.NoTimetable -> R.string.widget_no_timetable
            AgendaState.NeedsStart -> R.string.widget_needs_start
            AgendaState.BeforeTerm -> R.string.widget_before_term
            AgendaState.AfterTerm -> R.string.widget_after_term
            else -> R.string.widget_no_classes
        }
        val hint = when (state) {
            AgendaState.NoTimetable -> R.string.widget_import_hint
            AgendaState.NeedsStart -> R.string.widget_start_hint
            else -> null
        }
        text(R.id.widget_message, context.getString(message))
        ink(R.id.widget_message) { it.ink }
        text(R.id.widget_hint, hint?.let { context.getString(it) })
        ink(R.id.widget_hint) { it.ink2 }
    }

    private fun courseRow(entry: AgendaCourse, wideRow: Boolean, highlighted: Boolean): RemoteViews {
        val (time, state) = entry
        val (course, start, end) = time
        val periodLabel = if (course.startPeriod == course.endPeriod) {
            context.getString(R.string.widget_period, course.startPeriod)
        } else {
            context.getString(R.string.widget_period_range, course.startPeriod, course.endPeriod)
        }
        val clock = when {
            state == ClassState.Current -> context.getString(R.string.widget_class_now)
            state == ClassState.Finished -> context.getString(R.string.widget_class_finished)
            start != null -> formatTime(start)
            else -> periodLabel
        }
        val room = if (prefs.showRoom) course.location?.trim()?.takeIf(String::isNotEmpty) else null
        val tone = if (prefs.colorCourses) courseVisual(course.name).tone else null
        val layoutId = if (wideRow) R.layout.today_widget_course_wide else R.layout.today_widget_course

        return RemoteViews(context.packageName, layoutId).apply {
            if (highlighted) {
                val activeBg = when {
                    dualTheme -> R.drawable.today_widget_row_active
                    dark -> R.drawable.today_widget_row_active_dark
                    else -> R.drawable.today_widget_row_active_light
                }
                setInt(R.id.widget_course_root, "setBackgroundResource", activeBg)
            }
            paintImage(R.id.widget_course_mark) {
                if (state == ClassState.Finished) it.line else if (tone != null) it.courseTone(tone) else it.accent
            }
            text(R.id.widget_course_name, course.name)
            ink(R.id.widget_course_name) { if (state == ClassState.Finished) it.ink3 else it.ink }

            if (wideRow) {
                text(R.id.widget_course_details, room ?: periodLabel)
                ink(R.id.widget_course_details) { it.ink2 }
                text(R.id.widget_course_time, clock)
                ink(R.id.widget_course_time) {
                    when (state) {
                        ClassState.Current -> it.accent
                        ClassState.Finished -> it.ink3
                        else -> it.ink
                    }
                }
                val subtime = when {
                    state == ClassState.Current && end != null -> formatTime(end)
                    start != null && end != null -> formatTime(end)
                    room != null && start != null -> periodLabel
                    else -> null
                }
                text(R.id.widget_course_subtime, subtime)
                ink(R.id.widget_course_subtime) { it.ink3 }
            } else {
                val details = if (room != null) "$clock · $room" else clock
                text(R.id.widget_course_details, details)
                ink(R.id.widget_course_details) { if (state == ClassState.Current) it.accent else it.ink2 }
            }
        }
    }

    private fun RemoteViews.text(id: Int, value: CharSequence?) {
        setTextViewText(id, value)
        setViewVisibility(id, if (value != null) View.VISIBLE else View.GONE)
    }

    private inline fun RemoteViews.ink(id: Int, pick: (KomaColors) -> Color) = paint(id, "setTextColor", pick)

    private inline fun RemoteViews.paintImage(id: Int, pick: (KomaColors) -> Color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            paintTint(id, "setImageTintList", pick)
        } else {
            setInt(id, "setColorFilter", pick(colors).toArgb())
        }
    }

    private inline fun RemoteViews.paintTint(id: Int, method: String, pick: (KomaColors) -> Color) {
        val normal = ColorStateList.valueOf(pick(colors).toArgb())
        val night = nightColors
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && night != null) {
            setColorStateList(id, method, normal, ColorStateList.valueOf(pick(night).toArgb()))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setColorStateList(id, method, normal)
        }
    }

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
