package moe.tnxg.koma.data.education

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import moe.tnxg.koma.data.education.http.EduHttp
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlin.test.*

class EduHttpTest {
    @Test fun redirectsResolveRelativeLocationsAndDropPostBody() = runBlocking {
        val requests = mutableListOf<Request>()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            requests += request
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(if (requests.size == 1) 302 else 200)
                .message("OK").apply { if (requests.size == 1) header("Location", "../next") }.body("done".toResponseBody()).build()
        }.build()
        val result = EduHttp(client).follow("https://qyrz.xjvut.edu.cn/cas/login", FormBody.Builder().add("password", "encrypted").build())
        assertEquals("https://qyrz.xjvut.edu.cn/next", result.url.toString())
        assertEquals(listOf("POST", "GET"), requests.map { it.method })
        assertNull(requests[1].body)
    }
    @Test fun permanentAndSeeOtherRedirectsConvertPostToGetWithoutEntityHeaders() = runBlocking {
        for (status in listOf(301, 302, 303)) {
            val requests = mutableListOf<Request>()
            val http = redirectFixture(status, "/next", requests)
            http.follow("https://qyrz.xjvut.edu.cn/cas/login", FormBody.Builder().add("password", "secret").build(),
                mapOf("Content-Type" to "application/custom", "Content-Length" to "15",
                    "Transfer-Encoding" to "chunked", "Authorization" to "Bearer same-origin"))
            assertEquals(listOf("POST", "GET"), requests.map { it.method }, "status $status")
            assertNull(requests[1].body)
            for (name in listOf("Content-Type", "Content-Length", "Transfer-Encoding")) {
                assertNull(requests[1].header(name), "status $status: $name")
            }
            assertEquals("Bearer same-origin", requests[1].header("Authorization"))
        }
    }
    @Test fun temporaryAndPermanentRedirectsRetainPostAndHeadersOnSameOrigin() = runBlocking {
        for (status in listOf(307, 308)) {
            val requests = mutableListOf<Request>()
            val body = FormBody.Builder().add("password", "secret").build()
            redirectFixture(status, "/next", requests).follow("https://qyrz.xjvut.edu.cn/cas/login", body,
                mapOf("Authorization" to "Bearer private"))
            assertEquals(listOf("POST", "POST"), requests.map { it.method }, "status $status")
            assertSame(body, requests[1].body)
            assertEquals("Bearer private", requests[1].header("Authorization"))
        }
    }
    @Test fun crossOriginRedirectsNeverForwardCallerCredentialsOrReferrers() = runBlocking {
        for (target in listOf("https://jw.xjvut.edu.cn:6082/sso", "https://qyrz.xjvut.edu.cn:444/cas")) {
            val requests = mutableListOf<Request>()
            redirectFixture(302, target, requests).follow("https://qyrz.xjvut.edu.cn/cas/login",
                FormBody.Builder().add("password", "secret").build(),
                mapOf("Authorization" to "Bearer private", "Proxy-Authorization" to "Basic private",
                    "Cookie" to "TGC=private", "Referer" to "https://qyrz.xjvut.edu.cn/?ticket=private",
                    "Origin" to "https://qyrz.xjvut.edu.cn", "X-Private-Token" to "private"))
            assertEquals("GET", requests[1].method)
            assertNull(requests[1].body)
            for (name in listOf("Authorization", "Proxy-Authorization", "Cookie", "Referer", "Origin", "X-Private-Token")) {
                assertNull(requests[1].header(name), name)
            }
        }
    }
    @Test fun preservingRedirectsCannotReplayCredentialsToAnotherOrigin() = runBlocking<Unit> {
        for (status in listOf(307, 308)) {
            val requests = mutableListOf<Request>()
            val http = redirectFixture(status, "https://other.example/collect", requests)
            assertFailsWith<java.io.IOException> {
                http.follow("https://qyrz.xjvut.edu.cn/cas/login", FormBody.Builder().add("password", "secret").build())
            }
            assertEquals(1, requests.size)
        }
    }
    @Test fun nonRedirectStatusesIgnoreLocation() = runBlocking {
        for (status in listOf(300, 304, 305, 306)) {
            val requests = mutableListOf<Request>()
            val result = redirectFixture(status, "/ignored", requests).follow("https://qyrz.xjvut.edu.cn/cas/login")
            assertEquals(status, result.status)
            assertEquals(1, requests.size)
        }
    }
    @Test fun redirectsCannotDowngradeTls() = runBlocking<Unit> {
        val requests = mutableListOf<Request>()
        val http = redirectFixture(302, "http://qyrz.xjvut.edu.cn/cas/login", requests)
        assertFailsWith<java.io.IOException> { http.follow("https://qyrz.xjvut.edu.cn/cas/login") }
        assertEquals(1, requests.size)
    }
    @Test fun cancellationEscapesRatherThanReturningBusinessFailure() = runBlocking<Unit> {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            started.countDown()
            check(release.await(5, TimeUnit.SECONDS))
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("done".toResponseBody()).build()
        }.build()
        try {
            val request = async { EduHttp(client).call("https://qyrz.xjvut.edu.cn/cas/login") }
            assertTrue(withContext(Dispatchers.IO) { started.await(5, TimeUnit.SECONDS) })
            request.cancel()
            assertFailsWith<CancellationException> { request.await() }
        } finally { release.countDown() }
    }
    @Test fun redirectChainsLongerThanSixHopsStillResolve() = runBlocking {
        val requests = mutableListOf<Request>()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            requests += request
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(if (requests.size <= 8) 302 else 200)
                .message("OK").apply { if (requests.size <= 8) header("Location", "/hop${requests.size}") }
                .body("done".toResponseBody()).build()
        }.build()
        val result = EduHttp(client).follow("https://qyrz.xjvut.edu.cn/cas/login")
        assertEquals("https://qyrz.xjvut.edu.cn/hop8", result.url.toString())
        assertEquals(9, requests.size)
    }
    @Test fun redirectLoopsFailExplicitly() = runBlocking<Unit> {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(302).message("Loop")
                .header("Location", "/cas/login").body("".toResponseBody()).build()
        }.build()
        assertFailsWith<java.io.IOException> { EduHttp(client).follow("https://qyrz.xjvut.edu.cn/cas/login") }
    }

    private fun redirectFixture(status: Int, location: String, requests: MutableList<Request>): EduHttp =
        EduHttp(OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            requests += request
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                .code(if (requests.size == 1) status else 200).message("OK")
                .apply { if (requests.size == 1) header("Location", location) }
                .body("done".toResponseBody()).build()
        }.build())
}
