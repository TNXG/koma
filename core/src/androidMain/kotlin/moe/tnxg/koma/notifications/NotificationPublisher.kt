package moe.tnxg.koma.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import moe.tnxg.koma.core.R

class NotificationPublisher(private val context: Context) {
    sealed class Result {
        data object Posted : Result()
        data object PermissionDenied : Result()
        data object UnknownChannel : Result()
        data object ChannelBlocked : Result()
    }

    fun isGranted(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED)

    fun post(id: String, title: String, body: String, channelId: String): Result {
        if (!isGranted()) return Result.PermissionDenied
        val manager = context.getSystemService(NotificationManager::class.java) ?: return Result.UnknownChannel
        val channel = manager.getNotificationChannel(channelId) ?: return Result.UnknownChannel
        if (channel.importance == NotificationManager.IMPORTANCE_NONE) return Result.ChannelBlocked
        try {
            NotificationManagerCompat.from(context).notify(id, 0, build(channelId, title, body))
        } catch (error: SecurityException) {
            if (!isGranted()) return Result.PermissionDenied
            throw error
        }
        return Result.Posted
    }

    fun cancel(id: String) = NotificationManagerCompat.from(context).cancel(id, 0)
    fun cancelAll() = NotificationManagerCompat.from(context).cancelAll()

    private fun build(channelId: String, title: String, body: String): Notification {
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_koma)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            builder.setContentIntent(PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ))
        }
        return builder.build()
    }
}
