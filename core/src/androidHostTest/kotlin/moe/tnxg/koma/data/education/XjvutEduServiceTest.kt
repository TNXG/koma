package moe.tnxg.koma.data.education

import kotlinx.coroutines.runBlocking
import moe.tnxg.koma.data.education.http.CookieVault
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import java.nio.file.Files
import javax.crypto.KeyGenerator
import kotlin.test.*

class XjvutEduServiceTest {
    @Test fun loginFailureRefreshAndWeeklyImportPreserveServerPlacement() = runBlocking<Unit> {
        val server = EducationFixture()
        val service = server.service()
        val prepared = assertIs<LoginPreparation.Form>(service.prepare())
        assertTrue(prepared.captcha!!.startsWith("data:image/png;base64,"))
        assertEquals(prepared.captcha, service.refreshCaptcha())
        assertEquals(LoginResult(false, "验证码不对", true), service.login("student", "pw123", "wrong"))
        service.prepare()
        assertTrue(service.login("student", "pw123", "24").ok)
        val snapshot = service.importTimetable()
        assertEquals("测试同学", snapshot.studentName)
        assertEquals("2025-09-01", snapshot.semester.startDate)
        assertEquals(2, snapshot.semester.totalWeeks)
        assertEquals(listOf(1, 2), snapshot.courses.single().weeks)
        assertEquals("1", snapshot.courses.single().source["zcd"])
        assertEquals(600, snapshot.timeGrid.single().start)
        assertEquals(1, snapshot.timeGrid.single().index)
        assertTrue(server.encryptedPassword!!.matches(Regex("[0-9a-f]+(?: [0-9a-f]+)*")))
        assertNotEquals("pw123", server.encryptedPassword)
        service.logout()
        assertIs<LoginPreparation.Form>(service.prepare())
    }
    @Test fun captchaFailureCanRefreshAndRetryWithoutStartingLoginAgain() = runBlocking<Unit> {
        val service = EducationFixture().service()
        val prepared = assertIs<LoginPreparation.Form>(service.prepare())
        assertEquals(LoginResult(false, "验证码不对", true), service.login("student", "pw123", "wrong"))
        assertEquals(prepared.captcha, service.refreshCaptcha())
        assertTrue(service.login("student", "pw123", "24").ok)
    }

    @Test fun reimportRestoresEncryptedPersistedSessionWithoutCredentials() = runBlocking<Unit> {
        val directory = Files.createTempDirectory("koma-education-test").toFile()
        try {
            val file = directory.resolve("cookies")
            val key = KeyGenerator.getInstance("AES").generateKey()
            val firstVault = CookieVault(file) { key }
            val fixture = EducationFixture(firstVault)
            val first = fixture.service()
            first.prepare()
            assertTrue(first.login("student", "pw123", "24").ok)
            first.importTimetable()
            val restored = EducationFixture(CookieVault(file) { key }).service()
            assertEquals(listOf(1, 2), restored.importTimetable().courses.single().weeks)
            assertIs<LoginPreparation.Ready>(restored.prepare())
        } finally { directory.deleteRecursively() }
    }
    @Test fun weeklyRequestsAreFetchedConcurrently() = runBlocking<Unit> {
        val fixture = EducationFixture()
        val service = fixture.service()
        service.prepare()
        assertTrue(service.login("student", "pw123", "24").ok)
        fixture.parallelWeeks = true
        assertEquals(listOf(1, 2), service.importTimetable().courses.single().weeks)
    }
    @Test fun invalidWeekTableAndUnauthenticatedImportNeverSucceed() = runBlocking<Unit> {
        val fixture = EducationFixture()
        val service = fixture.service()
        assertFailsWith<java.io.IOException> { service.importTimetable() }
        service.prepare()
        assertTrue(service.login("student", "pw123", "24").ok)
        fixture.invalidWeeks = true
        assertFailsWith<java.io.IOException> { service.importTimetable() }
    }
    @Test fun lockedLoginShowsTheServerReasonAndKeepsTheFormUsable() = runBlocking<Unit> {
        val fixture = EducationFixture()
        fixture.loginStatus = 423
        fixture.loginStatusBody = "<span class='errors'>密码错误次数过多，账号已锁定</span>"
        val service = fixture.service()
        assertIs<LoginPreparation.Form>(service.prepare())
        assertEquals(LoginResult(false, "密码错误次数过多，账号已锁定"),
            service.login("student", "pw123", "24"))
        fixture.loginStatus = null
        assertTrue(service.login("student", "pw123", "24").ok)
    }
    @Test fun rejectedLoginWithoutServerTextExplainsTheStatusInsteadOfShowingIt() = runBlocking<Unit> {
        val fixture = EducationFixture()
        fixture.loginStatus = 423
        val service = fixture.service()
        service.prepare()
        assertEquals("登录被暂时锁定，请稍后重试", service.login("student", "pw123", "24").message)
    }
    @Test fun blockedLoginPageFailsWithAReasonNotAStatusCode() = runBlocking<Unit> {
        val fixture = EducationFixture()
        fixture.prepareStatus = 423
        val error = assertFailsWith<java.io.IOException> { fixture.service().prepare() }
        assertEquals("登录被暂时锁定，请稍后重试", error.message)
    }
}

/** External HTTP boundary only: all parsing, encryption, snapshot and vault behavior remains production code. */
private class EducationFixture(private val vault: CookieVault = CookieVault(null)) {
    var invalidWeeks = false
    var parallelWeeks = false
    var encryptedPassword: String? = null
    var prepareStatus: Int? = null
    var loginStatus: Int? = null
    var loginStatusBody = ""
    private val weekArrivals = java.util.concurrent.CountDownLatch(2)
    private var execution = "e1"
    private val cas = "https://qyrz.xjvut.edu.cn/cas/login".toHttpUrl()
    private val index = "https://jw.xjvut.edu.cn:6082/jwglxt/kbcx/xskbcxMobile_cxXskbcxIndex.html"
    fun service(): XjvutEduService {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val form = request.body as? FormBody
            fun field(name: String): String? = form?.let { (0 until it.size).firstOrNull { i -> it.name(i) == name }?.let(it::value) }
            var location: String? = null
            var status = 200
            val body = when {
                request.url.encodedPath == "/sso/zfiotlogin" -> {
                    location = if (vault.loadForRequest(cas).any { it.name == "CASTGT" }) index else cas.toString()
                    ""
                }
                request.url.encodedPath.endsWith("xskbcxMobile_cxXskbcxIndex.html") ->
                    "<input id='xnm_hide' value='2025'><input id='xqm_hide' value='3'>"
                request.url.encodedPath == "/cas/login" && request.method == "POST" -> {
                    val reject = loginStatus
                    if (reject != null) {
                        status = reject
                        loginStatusBody
                    } else {
                        assertEquals(execution, field("execution"))
                        assertEquals("student", field("username"))
                        assertEquals("zhmm", field("type"))
                        assertEquals("submit", field("_eventId"))
                        encryptedPassword = field("password")
                        if (field("authcode") != "24") {
                            execution = "e2"
                            "<input name='execution' value='e2'><span class='errors'>FailedAuthcodeException</span>"
                        }
                        else {
                            // Application interceptors stand outside OkHttp's bridge; seed the real vault at the remote boundary.
                            vault.saveFromResponse(cas, listOf(Cookie.parse(cas, "CASTGT=TGT-live; Path=/cas; Secure; HttpOnly")!!))
                            "<html>authenticated</html>"
                        }
                    }
                }
                request.url.encodedPath == "/cas/login" -> {
                    val reject = prepareStatus
                    if (reject != null) {
                        status = reject
                        ""
                    } else {
                        execution = "e1"
                        "<input name='execution' value='e1'>"
                    }
                }
                request.url.encodedPath == "/cas/v1/getPubKey" -> """{"modulus":"${"f".repeat(128)}","exponent":"10001"}"""
                request.url.encodedPath == "/cas/getKaptchaStatus" -> "true"
                request.url.encodedPath == "/cas/kaptcha" -> "image-data"
                request.url.encodedPath.endsWith("cxZc.html") -> if (invalidWeeks) "[]" else
                    """[{"zs":1,"rq":"2025-09-01/2025-09-07"},{"zs":2,"rq":"2025-09-08/2025-09-14"}]"""
                request.url.encodedPath.endsWith("cxXsKb.html") -> {
                    if (parallelWeeks) {
                        weekArrivals.countDown()
                        if (!weekArrivals.await(5, java.util.concurrent.TimeUnit.SECONDS))
                            throw java.io.IOException("周课表请求没有并发")
                    }
                    assertEquals("2025", field("xnm")); assertEquals("3", field("xqm"))
                    assertTrue(field("zs") in listOf("1", "2")); assertEquals("1", field("kblx")); assertEquals("app", field("doType"))
                    """{"xsxx":{"XM":"测试同学"},"kbList":[{"kcmc":"高等数学","xqj":"1","jcs":"0304","zcd":"1"}]}"""
                }
                request.url.encodedPath.endsWith("cxRjc.html") -> """[{"qssj":"10:00 - 10:45"}]"""
                else -> error("Unexpected request: ${request.url}")
            }
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(if (location == null) status else 302)
                .message("OK").apply { location?.let { header("Location", it) } }
                .header("Content-Type", if (request.url.encodedPath == "/cas/kaptcha") "image/png" else "text/plain")
                .body(body.toResponseBody("text/plain".toMediaType())).build()
        }.build()
        return XjvutEduService(vault, client)
    }
}
