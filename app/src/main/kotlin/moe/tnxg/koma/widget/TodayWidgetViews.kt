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
import moe.tnxg.koma.R
import moe.tnxg.koma.app.preferences.AppPreferences
import moe.tnxg.koma.app.preferences.ThemePreference
import moe.tnxg.koma.domain.calendar.addDays
import moe.tnxg.koma.domain.calendar.formatMonthDay
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
    private val variants = mutableMapOf<Pair<Int, Boolean>, RemoteViews>()

    private val headerHeight = 25 + lineHeight(14)
    private val rowHeight = maxOf(42, 8 + lineHeight(15) + lineHeight(12))
    private val footerHeight = 2 + lineHeight(12)

    fun forOptions(options: Bundle): RemoteViews {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val sizes = BundleCompat.getParcelableArrayList(options, AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java)
            if (!sizes.isNullOrEmpty() && sizes.size <= 16) {
                return RemoteViews(sizes.associateWith { forHeight(it.height.toInt()) })
            }
        }
        val landscape = forHeight(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 108))
        val portrait = forHeight(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 108))
        return if (landscape === portrait) portrait else RemoteViews(landscape, portrait)
    }

    private fun lineHeight(sp: Int): Int {
        val metrics = context.resources.displayMetrics
        val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp.toFloat(), metrics)
        return ceil(px * 1.2f / metrics.density).toInt()
    }

    private fun forHeight(height: Int): RemoteViews {
        val total = agenda.courses.size
        val room = height - headerHeight
        val withoutFooter = (room / rowHeight).coerceIn(1, 8)
        val withFooter = ((room - footerHeight) / rowHeight).coerceIn(1, 8)
        val overflow = total > withoutFooter && room >= rowHeight + footerHeight
        val capacity = minOf(total, if (overflow) withFooter else withoutFooter)
        return variants.getOrPut(capacity to overflow) { render(capacity, overflow) }
    }

    private fun render(capacity: Int, overflow: Boolean): RemoteViews =
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
            val monthDay = formatMonthDay(date)
            text(R.id.widget_date, if (date == addDays(today, 1)) context.getString(R.string.widget_tomorrow, monthDay)
            else context.getString(R.string.widget_date, monthDay, weekdayLabel(date)))
            ink(R.id.widget_date) { it.ink }
            text(R.id.widget_week, agenda.week?.let { context.getString(R.string.widget_week, it) })
            ink(R.id.widget_week) { it.accent }
            setViewVisibility(R.id.widget_rows, if (capacity > 0) View.VISIBLE else View.GONE)
            removeAllViews(R.id.widget_rows)
            for (entry in agenda.visibleCourses(capacity)) addView(R.id.widget_rows, courseRow(entry))
            text(R.id.widget_overflow, if (overflow) context.getString(R.string.widget_overflow, agenda.courses.size - capacity) else null)
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
        val (course, start, end) = time
        val period = if (course.startPeriod == course.endPeriod) context.getString(R.string.widget_period, course.startPeriod)
        else context.getString(R.string.widget_period_range, course.startPeriod, course.endPeriod)
        val clock = if (start != null && end != null) "${formatTime(start)}–${formatTime(end)}" else null
        val status = when (state) {
            ClassState.Current -> R.string.widget_class_now
            ClassState.Next -> R.string.widget_class_next
            ClassState.Unknown -> R.string.widget_class_unknown
            ClassState.Finished -> R.string.widget_class_finished.takeIf { agenda.state == AgendaState.Finished }
            ClassState.Later -> null
        }
        val details = buildString {
            if (prefs.showRoom) course.location?.trim()?.takeIf(String::isNotEmpty)?.let { append(it) }
            if (prefs.showTeacher) course.teacher?.trim()?.takeIf(String::isNotEmpty)?.let {
                if (isNotEmpty()) append(" · ")
                append(it)
            }
        }
        val emphasized = state == ClassState.Current || state == ClassState.Next
        val tone = if (prefs.colorCourses) courseVisual(course.name).tone else null
        return RemoteViews(context.packageName, R.layout.today_widget_course).apply {
            text(R.id.widget_course_time, clock ?: period)
            ink(R.id.widget_course_time) { if (state == ClassState.Finished) it.ink2 else it.ink }
            text(R.id.widget_course_status, status?.let { context.getString(it) } ?: period.takeIf { clock != null })
            ink(R.id.widget_course_status) { if (emphasized) it.accent else it.ink2 }
            paint(R.id.widget_course_mark, "setBackgroundColor") {
                if (state == ClassState.Finished) it.line else if (tone != null) it.courseTone(tone) else it.accent
            }
            text(R.id.widget_course_name, course.name)
            ink(R.id.widget_course_name) { if (state == ClassState.Finished) it.ink2 else it.ink }
            text(R.id.widget_course_details, details.ifEmpty { period.takeIf { status != null && clock != null } })
            ink(R.id.widget_course_details) { it.ink2 }
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
