package moe.tnxg.koma.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationsInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var publisher: NotificationPublisher
    private lateinit var manager: NotificationManager

    @Before
    fun setUp() {
        grantNotificationPermission()
        NotificationChannels.ensureAll(context)
        publisher = NotificationPublisher(context)
        manager = context.getSystemService(NotificationManager::class.java)
        publisher.cancelAll()
    }

    @After
    fun tearDown() {
        publisher.cancelAll()
    }

    @Test
    fun channelIsRegisteredByIdempotently() {
        NotificationChannels.ensureAll(context)
        NotificationChannels.ensureAll(context)

        val channel = manager.getNotificationChannel(NotificationChannels.CLASS_REMINDERS)
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertEquals("上课提醒", channel.name.toString())
    }

    @Test
    fun showPostsThenCancelRemoves() {
        val id = "test-show"
        assertEquals(
            NotificationPublisher.Result.Posted,
            publisher.post(id, "上课提醒", "第 3-4 节 · 高等数学", NotificationChannels.CLASS_REMINDERS),
        )
        assertTrue(await { manager.activeNotifications.any { it.tag == id } })

        publisher.cancel(id)
        assertTrue(await { manager.activeNotifications.none { it.tag == id } })
    }

    @Test
    fun sameIdUpdatesInsteadOfAdding() {
        val id = "test-update"
        publisher.post(id, "旧标题", "旧内容", NotificationChannels.CLASS_REMINDERS)
        publisher.post(id, "新标题", "新内容", NotificationChannels.CLASS_REMINDERS)

        assertTrue(await { manager.activeNotifications.any { it.tag == id } })
        assertEquals(1, manager.activeNotifications.count { it.tag == id })
    }

    @Test
    fun cancelAllRemovesEverything() {
        publisher.post("test-a", "A", "a", NotificationChannels.CLASS_REMINDERS)
        publisher.post("test-b", "B", "b", NotificationChannels.CLASS_REMINDERS)
        assertTrue(await { manager.activeNotifications.size >= 2 })

        publisher.cancelAll()
        assertTrue(await { manager.activeNotifications.isEmpty() })
    }

    @Test
    fun unknownChannelIsRejected() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        assertEquals(
            NotificationPublisher.Result.UnknownChannel,
            publisher.post("test-unknown", "t", "b", "no_such_channel"),
        )
    }

    private fun await(timeoutMs: Long = 3000, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition())
                return true
            Thread.sleep(50)
        }
        return condition()
    }

    private fun grantNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
            return
        shell("pm grant ${context.packageName} ${Manifest.permission.POST_NOTIFICATIONS}")
    }

    /** 仅用于在测试前授予通知权限（adb 等价的 pm grant） */
    private fun shell(command: String): String {
        val fd = instrumentation.uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(fd).bufferedReader().use { it.readText() }
    }
}
