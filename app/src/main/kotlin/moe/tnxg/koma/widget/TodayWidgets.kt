package moe.tnxg.koma.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import moe.tnxg.koma.MainActivity
import moe.tnxg.koma.app.preferences.APP_PREFERENCES_KEY
import moe.tnxg.koma.app.preferences.loadAppPreferences
import moe.tnxg.koma.domain.timeline.todayAgenda
import moe.tnxg.koma.domain.timeline.upcomingAgendas
import moe.tnxg.koma.platform.TimetableSettings
import moe.tnxg.koma.platform.komaSettings
import java.time.ZonedDateTime

internal class TodayWidgets(private val context: Context) : SharedPreferences.OnSharedPreferenceChangeListener {
    private val settings = context.komaSettings()
    private val wideProvider = ComponentName(context, TodayWidgetProvider::class.java)
    private val tallProvider = ComponentName(context, TallTodayWidgetProvider::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshLock = Mutex()

    init {
        settings.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (key == null || key == TimetableSettings.SNAPSHOT || key == TimetableSettings.START_DATE || key == APP_PREFERENCES_KEY) {
            refresh()
        }
    }

    fun refresh(pending: BroadcastReceiver.PendingResult? = null) {
        scope.launch {
            try {
                refreshLock.withLock { update() }
            } catch (error: RuntimeException) {
                Log.w("KomaWidget", "无法刷新今日课表：${error.javaClass.simpleName}")
            } finally {
                pending?.finish()
            }
        }
    }

    private fun update() {
        val manager = AppWidgetManager.getInstance(context)
        val wideIds = manager.getAppWidgetIds(wideProvider)
        val tallIds = manager.getAppWidgetIds(tallProvider)
        val alarm = context.getSystemService(AlarmManager::class.java)
        val refreshIntent = Intent(TodayWidgetProvider.REFRESH_ACTION).setComponent(wideProvider)
        if (wideIds.isEmpty() && tallIds.isEmpty()) {
            PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
                ?.let { alarm.cancel(it); it.cancel() }
            return
        }

        val now = ZonedDateTime.now()
        val date = LocalDate(now.year, now.monthValue, now.dayOfMonth)
        val snapshot = TimetableSettings.decodeSnapshot(settings.getString(TimetableSettings.SNAPSHOT, null))
        val start = settings.getString(TimetableSettings.START_DATE, null)
        val agenda = todayAgenda(snapshot, start, date, now.hour * 60 + now.minute)
        val nextDay = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        val next = agenda.nextChangeMinute?.let { minute ->
            ZonedDateTime.ofLocal(now.toLocalDate().atStartOfDay().plusMinutes(minute.toLong()), now.zone, now.offset)
        } ?: nextDay
        val refresh = PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_IMMUTABLE)
        // No exact-alarm permission or extra wakeup; the system may defer updates while idle.
        alarm.setAndAllowWhileIdle(AlarmManager.RTC, next.toInstant().toEpochMilli(), refresh)

        val launch = Intent(Intent.ACTION_MAIN).setClass(context, MainActivity::class.java)
            .addCategory(Intent.CATEGORY_LAUNCHER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val open = PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE)
        val views = TodayWidgetViews(context, date, agenda, loadAppPreferences { settings.getString(it, null) }, open) { limit ->
            upcomingAgendas(snapshot, start, agenda.date, limit)
        }
        for (id in wideIds) manager.updateAppWidget(id, views.forOptions(WidgetMode.Today, manager.getAppWidgetOptions(id), 174, 108))
        for (id in tallIds) manager.updateAppWidget(id, views.forOptions(WidgetMode.Agenda, manager.getAppWidgetOptions(id), 110, 160))
    }
}
