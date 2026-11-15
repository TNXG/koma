package moe.tnxg.koma.app.state

import kotlinx.coroutines.test.runTest
import kotlin.test.*
import moe.tnxg.koma.app.preferences.PreferenceStore
import moe.tnxg.koma.app.session.TimetableSession
import moe.tnxg.koma.data.education.*
import moe.tnxg.koma.domain.model.*
import moe.tnxg.koma.platform.*

class AccountSyncTest {
    @Test fun unrecognizedImagesAreRefreshedBeforeManualFallback() = runTest {
        val platform = SyncPlatform()
        val app = AppController(platform, PreferenceStore(platform::readSetting, platform::writeSetting), this)
        app.openSchools(); app.selectSchool(); app.prepare(); testScheduler.runCurrent()
        app.username = "student"; app.password = "secret"; app.submit(); testScheduler.runCurrent()
        assertEquals(3, platform.imagesSeen.distinct().size)
        assertTrue(app.manualCaptcha)
        assertEquals(LoginPhase.Form, app.phase)
        assertEquals(0, platform.logins)
    }

    @Test fun laterReadableImageCompletesLoginWithoutManualInput() = runTest {
        val platform = SyncPlatform(readableImage = "image-2")
        val app = AppController(platform, PreferenceStore(platform::readSetting, platform::writeSetting), this)
        app.openSchools(); app.selectSchool(); app.prepare(); testScheduler.runCurrent()
        app.username = "student"; app.password = "secret"; app.submit(); testScheduler.runCurrent()
        assertEquals(Route.Semester, app.route)
        assertFalse(app.manualCaptcha)
        assertEquals(1, platform.logins)
    }

    @Test fun automaticRefreshKeepsDateAndHomeAndThrottlesRepeatedForeground() = runTest {
        val platform = SyncPlatform(ready = true)
        TimetableSession(platform).apply { accept(platform.snapshot); confirm("2026-08-31") }
        val app = AppController(platform, PreferenceStore(platform::readSetting, platform::writeSetting), this)
        app.autoUpdateIfDue(1_000_000L); testScheduler.runCurrent()
        assertEquals(1, platform.imports)
        assertEquals(Route.Home, app.route)
        assertEquals("2026-08-31", app.timetable.startDate)
        app.autoUpdateIfDue(1_000_001L); testScheduler.runCurrent()
        assertEquals(1, platform.imports)
    }

    @Test fun backgroundVerificationFailureNeverDiscardsOfflineDataOrNavigates() = runTest {
        val platform = SyncPlatform()
        TimetableSession(platform).apply { accept(platform.snapshot); confirm("2026-08-31") }
        val app = AppController(platform, PreferenceStore(platform::readSetting, platform::writeSetting), this)
        app.autoUpdateIfDue(1_000_000L); testScheduler.runCurrent()
        assertEquals(Route.Home, app.route)
        assertNotNull(app.timetable.snapshot)
        assertTrue(app.syncNeedsLogin)
        assertEquals(0, platform.imports)
        assertEquals(0, platform.logins)
    }

    @Test fun disabledAutoUpdateSurvivesRestartAndDoesNotContactSchool() = runTest {
        val platform = SyncPlatform(ready = true)
        TimetableSession(platform).apply { accept(platform.snapshot); confirm("2026-08-31") }
        val preferences = PreferenceStore(platform::readSetting, platform::writeSetting)
        val app = AppController(platform, preferences, this)
        app.updateAutoUpdate(false)
        val restarted = AppController(platform, preferences, this)
        restarted.autoUpdateIfDue(1_000_000L); testScheduler.runCurrent()
        assertFalse(restarted.autoUpdate)
        assertEquals(0, platform.prepares)
    }
}

private class SyncPlatform(val readableImage: String? = null, val ready: Boolean = false) : PlatformServices {
    val settings = mutableMapOf<String, String>()
    val imagesSeen = mutableListOf<String>()
    var image = 1
    var logins = 0
    var imports = 0
    var prepares = 0
    val snapshot = TimetableSnapshot("xjvut", Term("2026", "3"), Semester("秋季", "2026-08-31", 20),
        emptyList(), emptyList(), importedAt = "2026-11-15T00:00:00Z")
    override val edu = object : EduService {
        override suspend fun prepare(): LoginPreparation { prepares++; return if (ready) LoginPreparation.Ready else LoginPreparation.Form("image-1") }
        override suspend fun refreshCaptcha() = "image-${++image}"
        override suspend fun login(username: String, password: String, captcha: String): LoginResult {
            logins++; assertEquals("2", captcha); return LoginResult(true)
        }
        override suspend fun importTimetable(): TimetableSnapshot { imports++; return snapshot }
        override suspend fun logout() {}
    }
    override val canRememberCredentials = false
    override val notifications = null
    override fun readSetting(key: String) = settings[key]
    override fun writeSetting(key: String, value: String?) { if (value == null) settings.remove(key) else settings[key] = value }
    override suspend fun loadCredentials(): SavedCredentials? = null
    override suspend fun saveCredentials(value: SavedCredentials?) {}
    override suspend fun recognizeCaptcha(dataUrl: String, view: Int): String? {
        imagesSeen += dataUrl
        return if (dataUrl == readableImage) "1+1" else null
    }
}
