package moe.tnxg.koma.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import moe.tnxg.koma.KomaApplication

open class TodayWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED,
            AppWidgetManager.ACTION_APPWIDGET_DISABLED,
            AppWidgetManager.ACTION_APPWIDGET_RESTORED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            REFRESH_ACTION -> (context.applicationContext as KomaApplication).todayWidgets.refresh(goAsync())
            else -> super.onReceive(context, intent)
        }
    }

    internal companion object {
        const val REFRESH_ACTION = "moe.tnxg.koma.widget.REFRESH_TODAY"
    }
}

class TallTodayWidgetProvider : TodayWidgetProvider()
