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

internal class TodayWidgetViews(
    private val context: Context,
    private val today: LocalDate,
    private val agenda: TodayAgenda,
    private val prefs: AppPreferences,
    private val openApp: PendingIntent,
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
    private val variants = mutableMapOf<Pair<Int, Overflow>, RemoteViews>()

    private val headerHeight = 18 + lineHeight(11)
    private val rowHeight = maxOf(32, 4 + lineHeight(13) + lineHeight(10))
    private val footerHeight = 1 + lineHeight(10)

    fun forOptions(options: Bundle, defaultWidth: Int, defaultHeight: Int): RemoteViews {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val sizes = BundleCompat.getParcelableArrayList(options, AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java)
            if (!sizes.isNullOrEmpty() && sizes.size <= 16) {
                return RemoteViews(sizes.associateWith { forSize(it.width.toInt(), it.height.toInt()) })
            }
        }
        val landscape = forSize(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, defaultWidth),
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, defaultHeight))
        val portrait = forSize(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, defaultWidth),
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, defaultHeight))
        return if (landscape === portrait) portrait else RemoteViews(landscape, portrait)
    }

    private fun lineHeight(sp: Int): Int {
        val metrics = context.resources.displayMetrics
        val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp.toFloat(), metrics)
        return ceil(px * 1.2f / metrics.density).toInt()
    }

    private fun forSize(width: Int, height: Int): RemoteViews {
        val total = agenda.courses.size
        val room = height - headerHeight
        val withoutFooter = (room / rowHeight).coerceIn(1, 8)
        val overflow = when {
            total <= withoutFooter -> Overflow.None
            // On a short, wide widget the count fits next to the week without displacing a row.
            width >= maxOf(174, 20 + lineHeight(11) * 11) -> Overflow.Header
            room >= rowHeight + footerHeight -> Overflow.Footer
            else -> Overflow.None
        }
        val rows = if (overflow == Overflow.Footer) ((room - footerHeight) / rowHeight).coerceIn(1, 8) else withoutFooter
        val capacity = minOf(total, rows)
        return variants.getOrPut(capacity to overflow) { render(capacity, overflow) }
    }

    private fun render(capacity: Int, overflow: Overflow): RemoteViews =
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
            val date = agenda.date
            val monthDay = "${date.month.number}/${date.day}"
            text(R.id.widget_date, if (date == addDays(today, 1)) context.getString(R.string.widget_tomorrow, monthDay)
            else context.getString(R.string.widget_date, monthDay, weekdayLabel(date)))
            ink(R.id.widget_date) { it.ink }
            text(R.id.widget_week, agenda.week?.let { context.getString(R.string.widget_week, it) })
            ink(R.id.widget_week) { it.accent }
            val more = if (overflow != Overflow.None) context.getString(R.string.widget_overflow, agenda.courses.size - capacity) else null
            text(R.id.widget_header_overflow, more.takeIf { overflow == Overflow.Header })
            ink(R.id.widget_header_overflow) { it.ink2 }
            setViewVisibility(R.id.widget_rows, if (capacity > 0) View.VISIBLE else View.GONE)
            removeAllViews(R.id.widget_rows)
            for (entry in agenda.visibleCourses(capacity)) addView(R.id.widget_rows, courseRow(entry))
            text(R.id.widget_overflow, more.takeIf { overflow == Overflow.Footer })
            ink(R.id.widget_overflow) { it.ink2 }
            setViewVisibility(R.id.widget_empty, if (capacity == 0) View.VISIBLE else View.GONE)
            if (capacity == 0) {
                val message = when (agenda.state) {
                    AgendaState.NoTimetable -> R.string.widget_no_timetable
                    AgendaState.NeedsStart -> R.string.widget_needs_start
                    AgendaState.BeforeTerm -> R.string.widget_before_term
                    AgendaState.AfterTerm -> R.string.widget_after_term
                    else -> R.string.widget_no_classes
                }
                val hint = when (agenda.state) {
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
