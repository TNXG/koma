package moe.tnxg.koma.feature.settings

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.LocalDate
import moe.tnxg.koma.app.preferences.AccentPreference
import moe.tnxg.koma.app.preferences.HomeDestination
import moe.tnxg.koma.app.preferences.ScheduleDensity
import moe.tnxg.koma.app.preferences.ThemePreference
import moe.tnxg.koma.domain.calendar.weekIndex
import moe.tnxg.koma.domain.model.Course
import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.domain.week.WeekCourseGroup
import moe.tnxg.koma.domain.week.WeekSchedule
import moe.tnxg.koma.domain.week.buildWeekSchedule
import moe.tnxg.koma.platform.NotificationPermission
import moe.tnxg.koma.platform.NotificationService

internal data class DisplayWeekPreview(
    val week: Int,
    val schedule: WeekSchedule,
    val visibleDays: Int,
    val periodCount: Int,
    val offWeekByWeekday: Map<Int, List<WeekCourseGroup>>,
)

internal fun ThemePreference.label(): String = when (this) {
    ThemePreference.System -> "跟随系统"
    ThemePreference.Light -> "浅色"
    ThemePreference.Dark -> "深色"
}

internal fun AccentPreference.label(): String = when (this) {
    AccentPreference.Iris -> "鸢尾蓝"
    AccentPreference.Blue -> "晴空蓝"
    AccentPreference.Green -> "松石绿"
    AccentPreference.Rose -> "蔷薇粉"
    AccentPreference.Amber -> "琥珀金"
}

internal fun ScheduleDensity.label(): String = when (this) {
    ScheduleDensity.Compact -> "紧凑"
    ScheduleDensity.Standard -> "标准"
    ScheduleDensity.Relaxed -> "宽松"
}

internal fun HomeDestination.label(): String = when (this) {
    HomeDestination.Today -> "今天"
    HomeDestination.Week -> "课表"
    HomeDestination.Me -> "我的"
}

internal val TEXT_SCALE_OPTIONS = listOf(
    0.9f to "紧凑",
    1.0f to "标准",
    1.12f to "较大",
    1.24f to "大号",
)

internal val REMINDER_LEAD_OPTIONS = listOf(0, 5, 10, 15, 30)

internal fun textScaleLabel(scale: Float): String =
    TEXT_SCALE_OPTIONS.minBy { kotlin.math.abs(it.first - scale) }.second

internal fun reminderLeadLabel(minutes: Int): String =
    if (minutes <= 0) "准时" else "提前 $minutes 分钟"

internal fun accentColorOf(accent: AccentPreference, dark: Boolean): Color = when (accent) {
    AccentPreference.Iris -> if (dark) Color(0xFF7C8FF0) else Color(0xFF4F5BD5)
    AccentPreference.Blue -> if (dark) Color(0xFF8FA4F0) else Color(0xFF5574D8)
    AccentPreference.Green -> if (dark) Color(0xFF6FB894) else Color(0xFF4C9A78)
    AccentPreference.Rose -> if (dark) Color(0xFFE08BA1) else Color(0xFFC95F78)
    AccentPreference.Amber -> if (dark) Color(0xFFD9AA4C) else Color(0xFFB98A2F)
}

internal fun buildDisplayWeekPreview(
    snapshot: TimetableSnapshot,
    startDate: String,
    todayDate: LocalDate,
    showWeekend: Boolean,
    showOtherWeekCourses: Boolean,
): DisplayWeekPreview {
    val totalWeeks = snapshot.semester.totalWeeks.coerceAtLeast(1)
    val currentWeek = runCatching { weekIndex(startDate, todayDate) }
        .getOrDefault(1)
        .coerceIn(1, totalWeeks)
    val schedule = buildWeekSchedule(snapshot, startDate, currentWeek)
    val visibleDays = if (showWeekend) 7 else 5
    val offWeek = if (!showOtherWeekCourses) {
        emptyMap()
    } else {
        (1..visibleDays).associateWith { weekday ->
            val occupied = schedule.days.getOrNull(weekday - 1)?.groups.orEmpty()
            val candidates = snapshot.courses
                .filter { it.weekday == weekday && currentWeek !in it.weeks }
                .sortedWith(compareBy(Course::startPeriod, Course::endPeriod, Course::id))
            val nonOverlapping = mutableListOf<WeekCourseGroup>()
            for (course in candidates) {
                val clashesActive = occupied.any {
                    course.startPeriod <= it.endPeriod && course.endPeriod >= it.startPeriod
                }
                val clashesOff = nonOverlapping.any {
                    course.startPeriod <= it.endPeriod && course.endPeriod >= it.startPeriod
                }
                if (!clashesActive && !clashesOff) {
                    nonOverlapping += WeekCourseGroup(course.startPeriod, course.endPeriod, listOf(course))
                }
            }
            nonOverlapping
        }
    }
    val maxActivePeriod = schedule.days.take(visibleDays)
        .flatMap { it.groups }.maxOfOrNull { it.endPeriod } ?: 0
    val maxOffPeriod = offWeek.values.flatten().maxOfOrNull { it.endPeriod } ?: 0
    val periodCount = maxOf(8, maxActivePeriod, maxOffPeriod).coerceAtMost(schedule.periodCount.coerceAtLeast(8))
    return DisplayWeekPreview(currentWeek, schedule, visibleDays, periodCount, offWeek)
}

internal fun pickCardPreviewCourse(snapshot: TimetableSnapshot): Course? =
    snapshot.courses.firstOrNull { !it.location.isNullOrBlank() && !it.teacher.isNullOrBlank() }
        ?: snapshot.courses.firstOrNull()

internal fun permissionLabel(hasService: Boolean, permission: NotificationPermission?): String = when {
    !hasService || permission == null -> "不可用"
    permission == NotificationPermission.Granted -> "已授权"
    permission == NotificationPermission.Prompt -> "未授权"
    else -> "已拒绝"
}

internal fun NotificationService?.safeStatus(): NotificationPermission? =
    this?.let { runCatching { it.getStatus() }.getOrNull() }
