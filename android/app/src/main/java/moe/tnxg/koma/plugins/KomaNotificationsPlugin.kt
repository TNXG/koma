package moe.tnxg.koma.plugins

import android.Manifest
import android.content.Context
import android.os.Build
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback
import moe.tnxg.koma.notifications.NotificationChannels
import moe.tnxg.koma.notifications.NotificationPublisher

/**
 * 本地通知（契约见 src/platform/notifications.ts）。
 * 渠道由原生注册，show 是即时发送；不涉及 AlarmManager / WorkManager。
 */
@CapacitorPlugin(
    name = "KomaNotifications",
    permissions = [
        Permission(alias = "notifications", strings = [Manifest.permission.POST_NOTIFICATIONS]),
    ],
)
class KomaNotificationsPlugin : Plugin() {
    private lateinit var publisher: NotificationPublisher

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun load() {
        publisher = NotificationPublisher(context)
        NotificationChannels.ensureAll(context)
    }

    @PluginMethod
    fun getStatus(call: PluginCall) {
        call.resolve(status())
    }

    @PluginMethod
    fun requestPermission(call: PluginCall) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || publisher.isGranted()) {
            call.resolve(status())
            return
        }
        markRequested()
        requestPermissionForAlias(ALIAS, call, "permissionCallback")
    }

    @PermissionCallback
    private fun permissionCallback(call: PluginCall) {
        markRequested()
        call.resolve(status())
    }

    @PluginMethod
    fun show(call: PluginCall) {
        val id = call.getString("id")
        val title = call.getString("title")
        val body = call.getString("body")
        if (id.isNullOrBlank() || title.isNullOrBlank() || body.isNullOrBlank())
            return call.reject("id/title/body 不能为空", "INVALID_ARGUMENT")

        val channelId = call.getString("channelId") ?: NotificationChannels.CLASS_REMINDERS
        val result = try {
            publisher.post(id, title, body, channelId)
        } catch (e: Exception) {
            return call.reject("通知发送失败：${e.javaClass.simpleName}", "POST_FAILED")
        }

        val payload = JSObject().put("posted", result is NotificationPublisher.Result.Posted)
        when (result) {
            NotificationPublisher.Result.Posted -> Unit
            NotificationPublisher.Result.PermissionDenied -> payload.put("reason", "permission_denied")
            NotificationPublisher.Result.UnknownChannel -> payload.put("reason", "unknown_channel")
            NotificationPublisher.Result.ChannelBlocked -> payload.put("reason", "channel_blocked")
        }
        call.resolve(payload)
    }

    @PluginMethod
    fun cancel(call: PluginCall) {
        val id = call.getString("id")
        if (id.isNullOrBlank())
            return call.reject("缺少 id", "INVALID_ARGUMENT")
        publisher.cancel(id)
        call.resolve()
    }

    @PluginMethod
    fun cancelAll(call: PluginCall) {
        publisher.cancelAll()
        call.resolve()
    }

    private fun status(): JSObject {
        val state = when {
            publisher.isGranted() -> "granted"
            prefs.getBoolean(KEY_REQUESTED, false) -> "denied"
            else -> "prompt"
        }
        return JSObject().put("notifications", state)
    }

    private fun markRequested() {
        prefs.edit().putBoolean(KEY_REQUESTED, true).apply()
    }

    private companion object {
        const val ALIAS = "notifications"
        const val PREFS_NAME = "koma_notifications"
        const val KEY_REQUESTED = "requested"
    }
}
