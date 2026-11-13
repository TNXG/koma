package moe.tnxg.koma.app.preferences

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.platform.notification.AndroidNotificationService

private const val ReminderAction = "moe.tnxg.koma.COURSE_REMINDER"
private val reminderJson = Json { ignoreUnknownKeys = true; coerceInputValues = true }

@Composable
internal actual fun SyncCourseReminders(snapshot: TimetableSnapshot?, startDate: String, preferences: AppPreferences) {
    val context = LocalContext.current.applicationContext
    LaunchedEffect(snapshot, startDate, preferences.remindersEnabled, preferences.reminderLeadMinutes) {
        scheduleReminder(context, snapshot, startDate, preferences)
    }
}

private fun pending(context: Context, reminder: CourseReminder? = null): PendingIntent {
    val intent = Intent(context, CourseReminderReceiver::class.java).setAction(ReminderAction)
    if (reminder != null) {
        intent.putExtra("title", reminder.course.name)
        intent.putExtra("body", reminder.course.location?.takeIf { it.isNotBlank() } ?: "课程即将开始")
    }
    return PendingIntent.getBroadcast(context, 1701, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

private fun scheduleReminder(context: Context, snapshot: TimetableSnapshot?, startDate: String, preferences: AppPreferences) {
    val alarms = context.getSystemService(AlarmManager::class.java)
    alarms.cancel(pending(context))
    if (!preferences.remindersEnabled || snapshot == null || startDate.isBlank()) {
        AndroidNotificationService(context, null).cancel("course-reminder")
        return
    }
    val next = nextCourseReminder(snapshot, startDate, Clock.System.now(), preferences.reminderLeadMinutes,
        TimeZone.currentSystemDefault()) ?: return
    // Inexact alarms require no special exact-alarm permission; the setting explains possible OS delay.
    alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.at.toEpochMilliseconds(), pending(context, next))
}

class CourseReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settings = context.getSharedPreferences("koma_settings", Context.MODE_PRIVATE)
        val preferences = runCatching {
            settings.getString("koma.preferences", null)?.let { reminderJson.decodeFromString<AppPreferences>(it) }
        }.getOrNull() ?: AppPreferences()
        val snapshot = runCatching {
            settings.getString("koma.snapshot", null)?.let { reminderJson.decodeFromString<TimetableSnapshot>(it) }
        }.getOrNull()
        val start = settings.getString("koma.startDate", null).orEmpty()
        if (preferences.remindersEnabled && snapshot != null && start.isNotBlank() && intent.action == ReminderAction) {
            val title = intent.getStringExtra("title")
            val body = intent.getStringExtra("body")
            if (!title.isNullOrBlank() && !body.isNullOrBlank()) {
                AndroidNotificationService(context, null).show("course-reminder", title, body)
            }
        }
        scheduleReminder(context, snapshot, start, preferences)
    }
}
