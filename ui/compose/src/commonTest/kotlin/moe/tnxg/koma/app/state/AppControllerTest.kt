package moe.tnxg.koma.app.state

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import moe.tnxg.koma.app.preferences.PreferenceStore
import moe.tnxg.koma.app.session.TimetableSession
import moe.tnxg.koma.data.education.EduService
import moe.tnxg.koma.data.education.LoginPreparation
import moe.tnxg.koma.data.education.LoginResult
import moe.tnxg.koma.domain.model.Semester
import moe.tnxg.koma.domain.model.Term
import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.platform.PlatformServices
import moe.tnxg.koma.platform.SavedCredentials

private fun controller(platform: PlatformServices, scope: CoroutineScope) =
    AppController(platform, PreferenceStore(platform::readSetting, platform::writeSetting), scope)

class AppControllerTest {
    @Test
    fun newInstallStartsAtWelcomeWithoutImporting() = runTest {
        val app = controller(OnboardingPlatform(), backgroundScope)
        assertEquals("Welcome", app.route.name)
        assertEquals(LoginPhase.Idle, app.phase)
        assertNull(app.timetable.snapshot)
    }

    @Test
    fun reimportShowsSchoolSelectionWithoutLosingOfflineTimetable() = runTest {
        val platform = OnboardingPlatform()
        TimetableSession(platform).apply { accept(snapshot()); confirm("2026-08-31") }
        val app = controller(platform, backgroundScope)
        assertEquals(Route.Home, app.route)
        app.openSchools()
        assertEquals("Schools", app.route.name)
        assertEquals("2026-08-31", app.timetable.startDate)
        assertEquals("xjvut", app.timetable.snapshot?.schoolId)
    }

    @Test
    fun loginCanReturnToSchoolSelectionAndWelcome() = runTest {
        val app = controller(OnboardingPlatform(), this)
        app.openSchools()
        app.selectSchool()
        app.prepare()
        testScheduler.runCurrent()
        assertEquals(Route.Login, app.route)
        assertEquals(LoginPhase.Form, app.phase)
        app.back()
        assertEquals(Route.Schools, app.route)
        app.back()
        assertEquals(Route.Welcome, app.route)
    }

    @Test
    fun loginImportRequiresDateConfirmationBeforeHomeAndSurvivesRestart() = runTest {
        val platform = OnboardingPlatform()
        val app = controller(platform, this)
        app.openSchools()
        app.selectSchool()
        app.prepare()
        testScheduler.runCurrent()
        app.username = "student"
        app.password = "password"
        app.submit()
        testScheduler.runCurrent()
        assertEquals(Route.Semester, app.route)
        assertEquals("xjvut", app.timetable.snapshot?.schoolId)
        app.confirmStart("2026-09-07")
        assertEquals(Route.Home, app.route)
        val restored = controller(platform, backgroundScope)
        assertEquals(Route.Home, restored.route)
        assertEquals("2026-09-07", restored.timetable.startDate)
    }

    @Test
    fun avatarSeedArrivesWithTheLoginAndLogoutForgetsIt() = runTest {
        val platform = OnboardingPlatform()
        val store = PreferenceStore(platform::readSetting, platform::writeSetting)
        val app = AppController(platform, store, this)
        app.openSchools()
        app.selectSchool()
        app.prepare()
        testScheduler.runCurrent()
        app.username = "student"
        app.password = "password"
        app.submit()
        testScheduler.runCurrent()
        assertTrue(store.avatarSeed.isNotBlank(), "a fresh login must own an avatar")

        app.logout()
        testScheduler.runCurrent()
        assertEquals("", store.avatarSeed)
        assertTrue(store.ensureAvatarSeed())
        assertNotEquals("", store.avatarSeed)
    }

    @Test
    fun cancellingSchoolSelectionReturnsToOfflineTimetable() = runTest {
        val platform = OnboardingPlatform()
        TimetableSession(platform).apply { accept(snapshot()); confirm("2026-08-31") }
        val app = controller(platform, backgroundScope)
        app.openSchools()
        app.back()
        assertEquals(Route.Home, app.route)
        assertEquals("2026-08-31", app.timetable.startDate)
    }

    @Test
    fun leavingLoginCancelsPendingSessionRestore() = runTest {
        val preparation = CompletableDeferred<LoginPreparation>()
        val app = controller(OnboardingPlatform(preparation), this)
        app.openSchools()
        app.selectSchool()
        app.prepare()
        testScheduler.runCurrent()
        assertEquals(LoginPhase.Preparing, app.phase)
        app.back()
        preparation.complete(LoginPreparation.Ready)
        testScheduler.runCurrent()
        assertEquals(Route.Schools, app.route)
        assertNull(app.timetable.snapshot)
    }

    @Test
    fun importedButUnconfirmedTimetableResumesDateSetup() = runTest {
        val platform = OnboardingPlatform()
        TimetableSession(platform).accept(snapshot())
        assertEquals(Route.Semester, controller(platform, backgroundScope).route)
    }

    @Test
    fun logoutClearsLocalTimetableAndRestartsWelcome() = runTest {
        val platform = OnboardingPlatform()
        TimetableSession(platform).apply { accept(snapshot()); confirm("2026-08-31") }
        val app = controller(platform, this)
        app.logout()
        testScheduler.runCurrent()
        assertEquals("Welcome", app.route.name)
        assertNull(controller(platform, backgroundScope).timetable.snapshot)
        assertNull(platform.readSetting("koma.startDate"))
    }
}

private fun snapshot() = TimetableSnapshot(
    "xjvut", Term("2026", "3"), Semester("2026 学年", "2026-09-07", 20),
    emptyList(), emptyList(), importedAt = "2026-11-13T00:00:00Z",
)

private class OnboardingPlatform(private val preparation: CompletableDeferred<LoginPreparation>? = null) : PlatformServices {
    private val values = mutableMapOf<String, String>()
    override val edu = object : EduService {
        override suspend fun prepare() = preparation?.await() ?: LoginPreparation.Form(null)
        override suspend fun refreshCaptcha(): String = error("Not used")
        override suspend fun login(username: String, password: String, captcha: String) = LoginResult(true)
        override suspend fun importTimetable() = snapshot()
        override suspend fun logout() {}
    }
    override val canRememberCredentials = false
    override val notifications = null
    override fun readSetting(key: String) = values[key]
    override fun writeSetting(key: String, value: String?) { if (value == null) values.remove(key) else values[key] = value }
    override suspend fun loadCredentials(): SavedCredentials? = null
    override suspend fun saveCredentials(value: SavedCredentials?) {}
    override suspend fun recognizeCaptcha(dataUrl: String, view: Int): String? = null
}
