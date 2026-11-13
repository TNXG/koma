package moe.tnxg.koma.platform.notification

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.tnxg.koma.notifications.NotificationChannels
import moe.tnxg.koma.notifications.NotificationPublisher
import moe.tnxg.koma.platform.NotificationPermission
import moe.tnxg.koma.platform.NotificationPostResult
import moe.tnxg.koma.platform.NotificationService

class AndroidNotificationService(
    context: Context,
    private val permissionHost: NotificationPermissionHost?,
) : NotificationService {
    private val prefs = context.getSharedPreferences("koma_notifications", Context.MODE_PRIVATE)
    private val publisher = NotificationPublisher(context)

    init {
        NotificationChannels.ensureAll(context)
    }

    override fun getStatus(): NotificationPermission = when {
        publisher.isGranted() -> NotificationPermission.Granted
        prefs.getBoolean("requested", false) -> NotificationPermission.Denied
        else -> NotificationPermission.Prompt
    }

    override suspend fun requestPermission(): NotificationPermission = withContext(Dispatchers.Main.immediate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !publisher.isGranted()) {
            val host = checkNotNull(permissionHost) { "通知授权需要前台 Activity" }
            check(prefs.edit().putBoolean("requested", true).commit()) { "通知授权状态保存失败" }
            host.requestNotificationPermission()
        }
        getStatus()
    }

    override fun show(id: String, title: String, body: String, channelId: String): NotificationPostResult {
        require(id.isNotBlank() && title.isNotBlank() && body.isNotBlank()) { "id/title/body 不能为空" }
        return when (publisher.post(id, title, body, channelId)) {
            NotificationPublisher.Result.Posted -> NotificationPostResult.Posted
            NotificationPublisher.Result.PermissionDenied -> NotificationPostResult.PermissionDenied
            NotificationPublisher.Result.UnknownChannel -> NotificationPostResult.UnknownChannel
            NotificationPublisher.Result.ChannelBlocked -> NotificationPostResult.ChannelBlocked
        }
    }

    override fun cancel(id: String) {
        require(id.isNotBlank()) { "缺少 id" }
        publisher.cancel(id)
    }

    override fun cancelAll() = publisher.cancelAll()
}
