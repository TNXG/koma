package moe.tnxg.koma.platform.notification

/** Implemented by the resumed Android Activity; never substitutes a guessed permission result. */
interface NotificationPermissionHost {
    suspend fun requestNotificationPermission()
}
