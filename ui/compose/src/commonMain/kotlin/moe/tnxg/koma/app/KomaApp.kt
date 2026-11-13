package moe.tnxg.koma.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CancellationException
import moe.tnxg.koma.app.state.AppController
import moe.tnxg.koma.app.state.Route
import moe.tnxg.koma.feature.home.HomeScreen
import moe.tnxg.koma.feature.login.LoginScreen
import moe.tnxg.koma.feature.semester.SemesterScreen
import moe.tnxg.koma.platform.PlatformServices
import moe.tnxg.koma.ui.component.button.KomaButton
import moe.tnxg.koma.ui.component.loader.KomaLoader
import moe.tnxg.koma.ui.platform.PlatformBackHandler
import moe.tnxg.koma.ui.theme.KomaTheme

@Composable
fun KomaApp(platform: PlatformServices) {
    KomaTheme {
        val scope = rememberCoroutineScope()
        var controller by remember(platform) { mutableStateOf<AppController?>(null) }
        var startupError by remember { mutableStateOf<String?>(null) }
        var attempt by remember { mutableStateOf(0) }
        LaunchedEffect(platform, attempt) {
            try {
                platform.migrateLegacySettings()
                controller = AppController(platform, scope)
                startupError = null
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { startupError = error.message ?: "本机数据读取失败" }
        }
        val insets = if (controller?.route == Route.Home)
            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal) else WindowInsets.safeDrawing
        Box(Modifier.fillMaxSize().background(KomaTheme.colors.bg).windowInsetsPadding(insets)) {
            val app = controller
            if (app == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (startupError == null) KomaLoader()
                    else KomaButton("读取失败，重试", { attempt++ })
                }
            } else {
                PlatformBackHandler(app.route != Route.Home && app.timetable.configured) {
                    app.confirmStart(app.timetable.startDate)
                }
                when (app.route) {
                    Route.Login -> LoginScreen(app)
                    Route.Semester -> app.timetable.snapshot?.let {
                        SemesterScreen(it.semester, app.timetable.startDate, app::confirmStart)
                    }
                    Route.Home -> app.timetable.snapshot?.let {
                        HomeScreen(it, app.timetable.startDate, app::reimport, app::editStart, app::logout)
                    }
                }
            }
        }
    }
}
