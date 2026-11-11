package moe.tnxg.koma.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/** 应用自注册的通知渠道；已存在时不动（importance 由用户决定，不能改回） */
object NotificationChannels {
    const val CLASS_REMINDERS = "class_reminders_v1"

    fun ensureAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CLASS_REMINDERS) != null)
            return
        val channel = NotificationChannel(
            CLASS_REMINDERS,
            "上课提醒",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "课表与上课提醒"
        }
        manager.createNotificationChannel(channel)
    }
}
