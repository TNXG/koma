package moe.tnxg.koma.data.education

import java.nio.file.Files
import javax.crypto.KeyGenerator
import moe.tnxg.koma.data.education.http.CookieVault
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import kotlin.test.*

class CookieVaultTest {
    @Test fun encryptedSessionsRestoreWithPathDomainAndSecureMatching() {
        val directory = Files.createTempDirectory("koma-cookie-test").toFile()
        try {
            val file = directory.resolve("cookies")
            val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
            val origin = "https://qyrz.xjvut.edu.cn/cas/login".toHttpUrl()
            val jar = CookieVault(file) { key }
            jar.saveFromResponse(origin, listOf(Cookie.parse(origin, "CASTGT=secret-ticket; Path=/cas; HttpOnly; Secure")!!))
            assertFalse(file.readBytes().toString(Charsets.UTF_8).contains("secret-ticket"))
            val restored = CookieVault(file) { key }
            assertEquals("secret-ticket", restored.loadForRequest(origin).single().value)
            assertTrue(restored.loadForRequest("https://qyrz.xjvut.edu.cn/other".toHttpUrl()).isEmpty())
            assertTrue(restored.loadForRequest("http://qyrz.xjvut.edu.cn/cas/login".toHttpUrl()).isEmpty())
            assertTrue(restored.loadForRequest("https://evil.example/cas/login".toHttpUrl()).isEmpty())
            restored.saveFromResponse(origin, listOf(Cookie.parse(origin, "CASTGT=gone; Path=/cas; Max-Age=0")!!))
            assertTrue(CookieVault(file) { key }.loadForRequest(origin).isEmpty())
        } finally { directory.deleteRecursively() }
    }
    @Test fun corruptOrWrongKeyVaultCannotRestoreAuthentication() {
        val directory = Files.createTempDirectory("koma-cookie-test").toFile()
        try {
            val file = directory.resolve("cookies")
            val key = KeyGenerator.getInstance("AES").generateKey()
            val url = "https://jw.xjvut.edu.cn:6082/jwglxt/index".toHttpUrl()
            CookieVault(file) { key }.saveFromResponse(url, listOf(Cookie.parse(url, "JSESSIONID=private; Path=/jwglxt")!!))
            val wrong = KeyGenerator.getInstance("AES").generateKey()
            assertTrue(CookieVault(file) { wrong }.loadForRequest(url).isEmpty())
            CookieVault(file) { key }.clear()
            assertFalse(file.exists())
        } finally { directory.deleteRecursively() }
    }
}
