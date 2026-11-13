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
}
