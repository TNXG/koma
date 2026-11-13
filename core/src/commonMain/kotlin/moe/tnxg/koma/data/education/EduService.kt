package moe.tnxg.koma.data.education

import moe.tnxg.koma.domain.model.TimetableSnapshot

interface EduService {
    suspend fun prepare(): LoginPreparation
    suspend fun refreshCaptcha(): String
    suspend fun login(username: String, password: String, captcha: String): LoginResult
    suspend fun importTimetable(): TimetableSnapshot
    suspend fun logout()
}

sealed interface LoginPreparation {
    data class Form(val captcha: String?) : LoginPreparation
    data object Ready : LoginPreparation
}

data class LoginResult(val ok: Boolean, val message: String = "", val captcha: Boolean = false)
