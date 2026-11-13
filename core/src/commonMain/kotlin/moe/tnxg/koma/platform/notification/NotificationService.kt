package moe.tnxg.koma.platform

enum class NotificationPermission { Prompt, Granted, Denied }
enum class NotificationPostResult { Posted, PermissionDenied, UnknownChannel, ChannelBlocked }

interface NotificationService {
    fun getStatus(): NotificationPermission
    suspend fun requestPermission(): NotificationPermission
    fun show(
        id: String,
        title: String,
        body: String,
        channelId: String = "class_reminders_v1",
    ): NotificationPostResult
    fun cancel(id: String)
    fun cancelAll()
}
