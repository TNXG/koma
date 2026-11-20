package moe.tnxg.koma.app.state

import kotlinx.coroutines.test.runTest
import kotlin.test.*
import moe.tnxg.koma.app.preferences.PreferenceStore
import moe.tnxg.koma.data.education.*
import moe.tnxg.koma.domain.model.*
import moe.tnxg.koma.platform.*

class SavedLoginTest {
    @Test fun savedPasswordSurvivesRecognitionFailureForManualSubmission() = runTest {
        val platform = SavedLoginPlatform(recognizable = false)
        val app = AppController(platform, PreferenceStore(platform::readSetting, platform::writeSetting), this)
        app.selectSchool(); app.prepare(); testScheduler.runCurrent()
        assertTrue(app.manualCaptcha)
        assertEquals("image-4", app.captcha)
        assertEquals(LoginPhase.Form, app.phase)
        assertEquals(3, platform.refreshes)
        assertEquals("student", app.username)
        assertEquals("secret", app.password)
        app.captchaInput = "2"; app.submit(); testScheduler.runCurrent()
        assertEquals(Route.Semester, app.route)
        assertEquals(SavedCredentials("student", "secret"), platform.saved)
    }

    @Test fun savedPasswordAutomaticallyLogsInWhenNoCaptchaIsRequired() = runTest {
        val platform = SavedLoginPlatform(captcha = null)
        val app = AppController(platform, PreferenceStore(platform::readSetting, platform::writeSetting), this)
        app.selectSchool(); app.prepare(); testScheduler.runCurrent()
        assertEquals(Route.Semester, app.route)
        assertEquals(1, platform.logins)
    }

    @Test fun disablingRememberPasswordRemovesPreviouslySavedCredentialsAfterLogin() = runTest {
        val platform = SavedLoginPlatform(recognizable = false)
        val app = AppController(platform, PreferenceStore(platform::readSetting, platform::writeSetting), this)
        app.selectSchool(); app.prepare(); testScheduler.runCurrent()
        app.rememberPassword = false
        app.captchaInput = "2"; app.submit(); testScheduler.runCurrent()
        assertEquals(Route.Semester, app.route)
        assertNull(platform.saved)
    }
}

private class SavedLoginPlatform(private val captcha: String? = "image-1", private val recognizable: Boolean = true) : PlatformServices {
    private val settings = mutableMapOf<String, String>()
    var saved: SavedCredentials? = SavedCredentials("student", "secret")
    var refreshes = 0
    var logins = 0
    override val canRememberCredentials = true
    override val notifications = null
    override fun readSetting(key: String) = settings[key]
    override fun writeSetting(key: String, value: String?) { if (value == null) settings.remove(key) else settings[key] = value }
    override suspend fun loadCredentials() = saved
    override suspend fun saveCredentials(value: SavedCredentials?) { saved = value }
    override suspend fun recognizeCaptcha(dataUrl: String, view: Int): String? = if (recognizable) "1+1" else null
    override val edu = object : EduService {
        override suspend fun prepare() = LoginPreparation.Form(captcha)
        override suspend fun refreshCaptcha(): String { refreshes++; return "image-${refreshes + 1}" }
        override suspend fun login(username: String, password: String, captcha: String): LoginResult {
            assertEquals("student", username)
            assertEquals("secret", password)
            assertEquals(if (this@SavedLoginPlatform.captcha == null) "" else "2", captcha)
            logins++
            return LoginResult(true)
        }
        override suspend fun importTimetable() = TimetableSnapshot("xjvut", Term("2026", "3"),
            Semester("秋季", "2026-08-31", 20), emptyList(), emptyList(), importedAt = "2026-11-15T00:00:00Z")
        override suspend fun logout() {}
    }
}
