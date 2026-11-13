package moe.tnxg.koma.app

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import moe.tnxg.koma.app.preferences.LocalAppPreferences
import moe.tnxg.koma.app.preferences.PreferenceStore
import moe.tnxg.koma.app.preferences.SyncCourseReminders
import moe.tnxg.koma.app.preferences.ThemePreference
import moe.tnxg.koma.app.state.AppController
import moe.tnxg.koma.app.state.LoginPhase
import moe.tnxg.koma.app.state.Route
import moe.tnxg.koma.feature.home.HomeScreen
import moe.tnxg.koma.feature.login.LoginScreen
import moe.tnxg.koma.feature.login.SchoolScreen
import moe.tnxg.koma.feature.onboarding.WelcomeScreen
import moe.tnxg.koma.feature.semester.SemesterScreen
import moe.tnxg.koma.platform.PlatformServices
import moe.tnxg.koma.ui.component.button.KomaButton
import moe.tnxg.koma.ui.component.loader.KomaLoader
import moe.tnxg.koma.ui.component.media.LocalImageServices
import moe.tnxg.koma.ui.component.sheet.KomaSheet
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.platform.PlatformBackHandler
import moe.tnxg.koma.ui.theme.KomaTheme

@Composable
fun KomaApp(platform: PlatformServices) {
    val preferences = remember(platform) { PreferenceStore(platform::readSetting, platform::writeSetting) }
    val density = LocalDensity.current
    val dark = when (preferences.value.theme) {
        ThemePreference.System -> isSystemInDarkTheme()
        ThemePreference.Light -> false
        ThemePreference.Dark -> true
    }
    CompositionLocalProvider(
        LocalAppPreferences provides preferences.value,
        LocalImageServices provides platform.images,
        LocalDensity provides Density(density.density, density.fontScale * preferences.value.textScale),
    ) {
        KomaTheme(dark) {
            val savedPages = rememberSaveableStateHolder()
            val scope = rememberCoroutineScope()
            var controller by remember(platform) { mutableStateOf<AppController?>(null) }
            var startupError by remember { mutableStateOf<String?>(null) }
            var attempt by remember { mutableStateOf(0) }
            LaunchedEffect(platform, attempt) {
                try {
                    platform.migrateLegacySettings()
                    val app = AppController(platform, preferences, scope)
                    if (app.timetable.configured) preferences.ensureAvatarSeed()
                    controller = app
                    startupError = null
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    startupError = error.message ?: "本机数据读取失败"
                }
            }
            val insets = if (controller?.route == Route.Home) {
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
            } else {
                WindowInsets.safeDrawing
            }
            Box(Modifier.fillMaxSize().background(KomaTheme.colors.bg).windowInsetsPadding(insets)) {
                val app = controller
                if (app == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (startupError == null) KomaLoader()
                        else KomaButton("读取失败，重试", { attempt++ })
                    }
                } else {
                    PlatformBackHandler(app.canGoBack, app::back)
                    SyncCourseReminders(app.timetable.snapshot, app.timetable.startDate, preferences.value)
                    LaunchedEffect(app.route) {
                        if (app.route == Route.Welcome) savedPages.removeState(Route.Home.name)
                    }
                    savedPages.SaveableStateProvider(app.route.name) {
                        when (app.route) {
                            Route.Welcome -> WelcomeScreen(app::openSchools)
                            Route.Schools -> SchoolScreen(app::selectSchool, app::back)
                            Route.Login -> LoginScreen(app)
                            Route.Semester -> app.timetable.snapshot?.let {
                                SemesterScreen(it.semester, app.timetable.startDate, app::confirmStart)
                            }
                            Route.Home -> app.timetable.snapshot?.let {
                                HomeScreen(it, app.timetable.startDate, preferences, platform.notifications,
                                    app::openSchools, app::editStart, app::logout)
                            }
                        }
                    }
                    var dismissed by remember(app.message) { mutableStateOf(false) }
                    KomaSheet(
                        open = app.route == Route.Home && app.phase == LoginPhase.Error && !dismissed,
                        onDismiss = { dismissed = true },
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            KomaText(app.message)
                            KomaButton("关闭", { dismissed = true })
                        }
                    }
                }
            }
        }
    }
}
