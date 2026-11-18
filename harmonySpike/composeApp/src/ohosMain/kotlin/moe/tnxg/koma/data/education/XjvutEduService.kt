package moe.tnxg.koma.data.education

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import moe.tnxg.koma.data.education.http.CookieVault
import moe.tnxg.koma.data.education.http.EduHttp
import moe.tnxg.koma.data.education.http.IoException
import moe.tnxg.koma.data.education.xjvut.LoginSession
import moe.tnxg.koma.data.education.xjvut.TimetableImporter
import moe.tnxg.koma.data.education.zhengfang.Diagnostic
import moe.tnxg.koma.domain.model.TimetableSnapshot

class XjvutEduService internal constructor(private val vault: CookieVault) : EduService {

    constructor(cookieFile: String? = null) : this(CookieVault(cookieFile))

    private val mutex = Mutex()
    private val http = EduHttp(EduHttp.client(vault))
    private val session = LoginSession(http, vault)
    private val importer = TimetableImporter(http)
    val diagnostics: List<Diagnostic> get() = importer.diagnostics

    override suspend fun prepare(): LoginPreparation = withContext(Dispatchers.Default) {
        mutex.withLock { session.prepare() }
    }
    override suspend fun refreshCaptcha(): String = withContext(Dispatchers.Default) {
        mutex.withLock { session.refreshCaptcha() }
    }
    override suspend fun login(username: String, password: String, captcha: String): LoginResult = withContext(Dispatchers.Default) {
        mutex.withLock {
            try { session.login(username, password, captcha) }
            catch (cancelled: CancellationException) { session.invalidate(); throw cancelled }
            catch (error: Exception) {
                session.invalidate()
                throw error
            }
        }
    }
    override suspend fun importTimetable(): TimetableSnapshot = withContext(Dispatchers.Default) {
        mutex.withLock {
            // A new process can reimport directly using persisted CAS/JW cookies, without credentials.
            if (!session.ready && !session.restore()) throw IoException("需要先登录")
            try { importer.fetch() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { session.invalidate(); throw error }
        }
    }
    override suspend fun logout(): Unit = withContext(Dispatchers.Default) {
        mutex.withLock { session.reset() }
    }
}
