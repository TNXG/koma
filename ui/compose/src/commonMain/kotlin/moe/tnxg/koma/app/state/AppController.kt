package moe.tnxg.koma.app.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import moe.tnxg.koma.app.session.TimetableSession
import moe.tnxg.koma.data.education.LoginPreparation
import moe.tnxg.koma.domain.captcha.captchaAnswer
import moe.tnxg.koma.platform.PlatformServices
import moe.tnxg.koma.platform.SavedCredentials

enum class Route { Welcome, Schools, Login, Semester, Home }
enum class LoginPhase { Idle, Preparing, Form, LoggingIn, Importing, Error }

class AppController(val platform: PlatformServices, private val scope: CoroutineScope) {
    val timetable = TimetableSession(platform)
    var route by mutableStateOf(when {
        timetable.configured -> Route.Home
        timetable.snapshot != null -> Route.Semester
        else -> Route.Welcome
    })
        private set
    var phase by mutableStateOf(LoginPhase.Idle)
        private set
    var message by mutableStateOf("")
        private set
    var captcha by mutableStateOf<String?>(null)
        private set
    var manualCaptcha by mutableStateOf(false)
        private set
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var captchaInput by mutableStateOf("")
    var rememberPassword by mutableStateOf(true)
    private var operation: Job? = null
    val busy: Boolean get() = phase == LoginPhase.LoggingIn || phase == LoginPhase.Importing
    val canSubmit: Boolean get() = phase == LoginPhase.Form && username.isNotBlank() && password.isNotEmpty() &&
        (captcha == null || !manualCaptcha || captchaInput.isNotBlank())

    fun prepare() = runOperation {
        phase = LoginPhase.Preparing
        message = ""
        when (val result = platform.edu.prepare()) {
            LoginPreparation.Ready -> importTimetable()
            is LoginPreparation.Form -> {
                captcha = result.captcha
                manualCaptcha = false
                phase = LoginPhase.Form
                val saved = if (platform.canRememberCredentials) platform.loadCredentials() else null
                if (saved != null) {
                    username = saved.username
                    password = saved.password
                    login()
                }
            }
        }
    }

    fun submit() {
        if (canSubmit) runOperation { login() }
    }

    fun refreshCaptcha() = runOperation {
        captcha = platform.edu.refreshCaptcha()
        captchaInput = ""
    }

    fun openSchools() { route = Route.Schools; phase = LoginPhase.Idle }

    fun selectSchool() {
        message = ""
        captcha = null
        captchaInput = ""
        manualCaptcha = false
        phase = LoginPhase.Idle
        route = Route.Login
    }

    val canGoBack: Boolean get() = route == Route.Schools || route == Route.Login ||
        (route == Route.Semester && timetable.configured)

    fun back() {
        val destination = when (route) {
            Route.Login -> Route.Schools
            Route.Schools -> if (timetable.configured) Route.Home else Route.Welcome
            Route.Semester -> if (timetable.configured) Route.Home else return
            else -> return
        }
        operation?.cancel()
        phase = LoginPhase.Idle
        route = destination
    }
    fun editStart() { route = Route.Semester }
    fun confirmStart(iso: String) { timetable.confirm(iso); route = Route.Home }

    fun logout() = runOperation {
        platform.edu.logout()
        platform.saveCredentials(null)
        timetable.clear()
        username = ""
        password = ""
        captcha = null
        captchaInput = ""
        message = ""
        phase = LoginPhase.Idle
        route = Route.Welcome
    }

    private suspend fun login() {
        phase = LoginPhase.LoggingIn
        message = ""
        var code: String? = if (captcha == null) "" else if (manualCaptcha) captchaInput.trim() else null
        var rounds = 0
        while (true) {
            if (code == null) {
                code = recognizeCaptcha()
                if (code == null) { manual("验证码没认出来，请手动输入"); return }
            }
            val result = platform.edu.login(username.trim(), password, code)
            if (result.ok) break
            if (!result.captcha) { message = result.message; phase = LoginPhase.Form; return }
            rounds++
            captcha = platform.edu.refreshCaptcha()
            if (manualCaptcha || rounds >= 3) { manual("验证码不对，请手动输入"); return }
            code = null
        }
        captchaInput = ""
        if (platform.canRememberCredentials) {
            platform.saveCredentials(if (rememberPassword) SavedCredentials(username.trim(), password) else null)
        }
        importTimetable()
    }

    private suspend fun recognizeCaptcha(): String? {
        val image = captcha ?: return ""
        repeat(2) { view ->
            val text = try { platform.recognizeCaptcha(image, view) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { null }
            text?.let { captchaAnswer(it) }?.let { return it }
        }
        return null
    }

    private fun manual(text: String) {
        manualCaptcha = true
        captchaInput = ""
        message = text
        phase = LoginPhase.Form
    }

    private suspend fun importTimetable() {
        phase = LoginPhase.Importing
        timetable.accept(platform.edu.importTimetable())
        route = Route.Semester
        phase = LoginPhase.Idle
    }

    private fun runOperation(block: suspend () -> Unit) {
        operation?.cancel()
        operation = scope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                message = error.message ?: "操作失败，请重试"
                phase = LoginPhase.Error
            }
        }
    }
}
