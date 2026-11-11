package moe.tnxg.koma.http

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeHttpClientInstrumentedTest {
    private lateinit var server: MockWebServer
    private val client = NativeHttpClient()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun redirectIsNotFollowedAndSetCookieIsReturned() {
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .setHeader("Location", server.url("/next").toString())
                .addHeader("Set-Cookie", "CASTGC=tgt-abc; Path=/cas"),
        )

        val response = client.execute(
            NativeHttpClient.RequestSpec(url = server.url("/cas/login").toString()),
        )

        assertEquals(302, response.status)
        val setCookie = response.headers.entries
            .firstOrNull { it.key.equals("Set-Cookie", ignoreCase = true) }
            ?.value
        assertNotNull(setCookie)
        assertEquals(listOf("CASTGC=tgt-abc; Path=/cas"), setCookie)
        // 没跟第二跳
        assertEquals(1, server.requestCount)
    }

    @Test
    fun multipleSetCookieHeadersStayGrouped() {
        server.enqueue(
            MockResponse()
                .addHeader("Set-Cookie", "a=1; Path=/")
                .addHeader("Set-Cookie", "b=2; Path=/jwglxt"),
        )

        val response = client.execute(NativeHttpClient.RequestSpec(url = server.url("/").toString()))

        val setCookie = response.headers.entries
            .firstOrNull { it.key.equals("Set-Cookie", ignoreCase = true) }
            ?.value
        assertEquals(listOf("a=1; Path=/", "b=2; Path=/jwglxt"), setCookie)
    }

    @Test
    fun binaryResponseIsBase64() {
        val payload = byteArrayOf(0x00, 0xFF.toByte(), 0x10, 0x7F)
        server.enqueue(MockResponse().setBody(Buffer().write(payload)))

        val response = client.execute(
            NativeHttpClient.RequestSpec(url = server.url("/captcha").toString(), binary = true),
        )

        assertEquals(Base64.encodeToString(payload, Base64.NO_WRAP), response.body)
    }

    @Test
    fun getWithBodyIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            client.execute(
                NativeHttpClient.RequestSpec(
                    url = server.url("/").toString(),
                    method = "GET",
                    body = "x=1",
                ),
            )
        }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun nonHttpUrlIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            client.execute(NativeHttpClient.RequestSpec(url = "ftp://example.com/file"))
        }
        assertTrue(true)
    }
}
