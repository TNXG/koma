package moe.tnxg.koma.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 无权限路径由 adb 撤销权限后单独运行：
 *   adb shell pm revoke moe.tnxg.koma android.permission.POST_NOTIFICATIONS
 *   adb shell am instrument -w -e class moe.tnxg.koma.notifications.NotificationDeniedInstrumentedTest \
 *     moe.tnxg.koma.test/androidx.test.runner.AndroidJUnitRunner
 * 权限存在时 assumeFalse 跳过（不是失败），保证全量测试可重复。
 */
@RunWith(AndroidJUnit4::class)
class NotificationDeniedInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun showReturnsPermissionDenied() {
        assumeFalse(NotificationManagerCompat.from(context).areNotificationsEnabled())
        val publisher = NotificationPublisher(context)

        assertFalse(publisher.isGranted())
        assertEquals(
            NotificationPublisher.Result.PermissionDenied,
            publisher.post("test-denied", "上课提醒", "无通知权限", NotificationChannels.CLASS_REMINDERS),
        )
    }
}
