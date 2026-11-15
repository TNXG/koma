package moe.tnxg.koma.data.education

import java.nio.file.Files
import javax.crypto.KeyGenerator
import kotlinx.coroutines.runBlocking
import moe.tnxg.koma.data.education.http.CookieVault
import moe.tnxg.koma.data.education.xjvut.Endpoints
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlin.test.*

class LoginSessionTest {
    @Test fun firstPreparationAvoidsUnauthenticatedServiceTicketCycleAndSubmitsFreshExecution() = runBlocking {
        val fixture = SessionBoundaryFixture(CookieVault(null))
        val service = fixture.service()
        assertIs<LoginPreparation.Form>(service.prepare())
        assertTrue(fixture.requests.none { it.url.queryParameter("service") != null })
        assertTrue(fixture.requests.none { it.url.queryParameter("ticket") != null })
        assertTrue(service.login("student", "password", "").ok)
        assertEquals(fixture.execution, fixture.submittedExecution)
    }

    @Test fun persistedAnonymousRoutingCookiesDoNotBecomeEvidenceOfAuthentication() = runBlocking {
        withPersistedVault { file, key ->
            val origin = Endpoints.LOGIN.toHttpUrl()
            CookieVault(file) { key }.saveFromResponse(origin,
                listOf(Cookie.parse(origin, "route=anonymous; Path=/")!!))
            val fixture = SessionBoundaryFixture(CookieVault(file) { key })
            assertIs<LoginPreparation.Form>(fixture.service().prepare())
            assertTrue(fixture.requests.none { it.url.queryParameter("service") != null })
        }
    }

    @Test fun persistedCasSessionStillExchangesServiceTicketWithoutCredentials() = runBlocking {
        withPersistedVault { file, key ->
            val origin = Endpoints.LOGIN.toHttpUrl()
            CookieVault(file) { key }.saveFromResponse(origin,
                listOf(Cookie.parse(origin, "CASTGT=live; Path=/cas; Secure; HttpOnly")!!))
            val fixture = SessionBoundaryFixture(CookieVault(file) { key })
            assertIs<LoginPreparation.Ready>(fixture.service().prepare())
            assertTrue(fixture.requests.any { it.url.queryParameter("service") == Endpoints.SSO })
            assertTrue(fixture.requests.any { it.url.queryParameter("ticket") == "ST-live" })
            assertTrue(fixture.requests.none { it.method == "POST" })
        }
    }

    @Test fun persistedEducationSessionRestoresEvenWhenCasSessionHasExpired() = runBlocking {
        val vault = CookieVault(null)
        val origin = Endpoints.SSO.toHttpUrl()
        vault.saveFromResponse(origin, listOf(Cookie.parse(origin, "JSESSIONID=live; Path=/; Secure; HttpOnly")!!))
        val fixture = SessionBoundaryFixture(vault)
        assertIs<LoginPreparation.Ready>(fixture.service().prepare())
        assertTrue(fixture.requests.none { it.url.host == "qyrz.xjvut.edu.cn" })
    }

    @Test fun restoreStopsOnlyAtTheExactCasLoginOrigin() = runBlocking<Unit> {
        val fixture = SessionBoundaryFixture(CookieVault(null))
        fixture.casTarget = "https://qyrz.xjvut.edu.cn:444/cas/login"
        assertFailsWith<java.io.IOException> { fixture.service().prepare() }
        assertTrue(fixture.requests.any { it.url.port == 444 })
    }

    private suspend fun withPersistedVault(block: suspend (java.io.File, javax.crypto.SecretKey) -> Unit) {
        val directory = Files.createTempDirectory("koma-login-boundary").toFile()
        try { block(directory.resolve("cookies"), KeyGenerator.getInstance("AES").generateKey()) }
        finally { directory.deleteRecursively() }
    }
}

/** Emulates the external CAS/JW boundary; parsing, cookies and login state remain real. */
private class SessionBoundaryFixture(private val vault: CookieVault) {
    val requests = mutableListOf<Request>()
    var execution = ""
        private set
    var submittedExecution: String? = null
        private set
    var casTarget: String? = null
    private val cas = Endpoints.LOGIN.toHttpUrl()
    private val sso = Endpoints.SSO.toHttpUrl()
    private val serviceLogin = cas.newBuilder().addQueryParameter("service", Endpoints.SSO).build()
    private val index = "${Endpoints.JW}/jwglxt/kbcx/xskbcxMobile_cxXskbcxIndex.html"

    fun service(): XjvutEduService = XjvutEduService(vault, OkHttpClient.Builder().addInterceptor { chain ->
        val request = chain.request()
        requests += request
        fun cookie(url: HttpUrl, name: String) = vault.loadForRequest(url).any { it.name == name && it.value == "live" }
        fun save(url: HttpUrl, raw: String) = vault.saveFromResponse(url, listOf(Cookie.parse(url, raw)!!))
        var location: String? = null
        val body = when {
            request.url.port == 444 -> {
                location = "https://qyrz.xjvut.edu.cn:444/cas/login"
                ""
            }
            request.url.encodedPath == "/sso/zfiotlogin" -> {
                location = when {
                    request.url.queryParameter("ticket") == "ST-live" -> {
                        save(sso, "JSESSIONID=live; Path=/; Secure; HttpOnly")
                        index
                    }
                    cookie(sso, "JSESSIONID") -> index
                    else -> casTarget ?: serviceLogin.toString()
                }
                ""
            }
            request.url.encodedPath == "/cas/login" && request.method == "POST" -> {
                val form = request.body as FormBody
                submittedExecution = (0 until form.size).firstOrNull { form.name(it) == "execution" }?.let(form::value)
                assertEquals(execution, submittedExecution)
                save(cas, "CASTGT=live; Path=/cas; Secure; HttpOnly")
                "<html>authenticated</html>"
            }
            request.url.encodedPath == "/cas/login" && request.url.queryParameter("service") != null -> {
                // Without a valid CAS session the service can keep bouncing an unusable ticket.
                // A restore probe must not enter that chain just to discover a login is needed.
                location = sso.newBuilder().addQueryParameter("ticket", if (cookie(cas, "CASTGT")) "ST-live" else "ST-rejected").build().toString()
                ""
            }
            request.url.encodedPath == "/cas/login" -> if (cookie(cas, "CASTGT")) {
                "<html>authenticated</html>"
            } else {
                save(cas, "route=anonymous; Path=/")
                execution = "e${requests.count { it.url == cas }}"
                "<form><input name='execution' value='$execution'></form>"
            }
            request.url.encodedPath == "/cas/v1/getPubKey" -> """{"modulus":"${"f".repeat(128)}","exponent":"10001"}"""
            request.url.encodedPath == "/cas/getKaptchaStatus" -> "false"
            request.url.toString() == index -> "<html>timetable</html>"
            else -> error("Unexpected fixture request: ${request.url}")
        }
        Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
            .code(if (location == null) 200 else 302).message("OK")
            .apply { location?.let { header("Location", it) } }.body(body.toResponseBody()).build()
    }.build())
}
