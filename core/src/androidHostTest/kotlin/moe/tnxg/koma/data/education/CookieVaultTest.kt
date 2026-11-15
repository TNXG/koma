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
    @Test fun cookiesWithTheSameNameReplaceOnlyTheirOwnHostAndPath() {
        val vault = CookieVault(null)
        val cas = "https://qyrz.xjvut.edu.cn/cas/login".toHttpUrl()
        val jw = "https://jw.xjvut.edu.cn:6082/jwglxt/index".toHttpUrl()
        fun save(url: okhttp3.HttpUrl, raw: String) = vault.saveFromResponse(url, listOf(Cookie.parse(url, raw)!!))
        save(cas, "session=root; Path=/; Secure")
        save(cas, "session=cas-old; Path=/cas; Secure")
        save(jw, "session=jw; Path=/; Secure")
        save(cas, "session=cas-new; Path=/cas; Secure")
        assertEquals(listOf("cas-new", "root"), vault.loadForRequest(cas).map { it.value })
        assertEquals(listOf("jw"), vault.loadForRequest(jw).map { it.value })
        save(cas, "session=deleted; Path=/cas; Max-Age=0")
        assertEquals(listOf("root"), vault.loadForRequest(cas).map { it.value })
        assertEquals(listOf("jw"), vault.loadForRequest(jw).map { it.value })
    }
    @Test fun expiresAndMaxAgeRemovalSurviveEncryptedPersistence() {
        val directory = Files.createTempDirectory("koma-cookie-test").toFile()
        try {
            val file = directory.resolve("cookies")
            val key = KeyGenerator.getInstance("AES").generateKey()
            val origin = "https://qyrz.xjvut.edu.cn/cas/login".toHttpUrl()
            val vault = CookieVault(file) { key }
            fun save(raw: String) = vault.saveFromResponse(origin, listOf(Cookie.parse(origin, raw)!!))
            save("expired=gone; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/")
            save("session=retained; Path=/; HttpOnly")
            save("persistent=retained; Path=/; Max-Age=3600")
            save("deleted=gone; Path=/; Max-Age=0; Expires=Wed, 01 Jan 2098 00:00:00 GMT")
            assertEquals(setOf("session", "persistent"), CookieVault(file) { key }.loadForRequest(origin).map { it.name }.toSet())
            save("persistent=gone; Path=/; Max-Age=0")
            assertEquals(listOf("session"), CookieVault(file) { key }.loadForRequest(origin).map { it.name })
        } finally { directory.deleteRecursively() }
    }
    @Test fun routingCookiesAreHttpsOnlyAndCannotBeInjectedFromAnotherOrigin() {
        val vault = CookieVault(null)
        val cas = "https://qyrz.xjvut.edu.cn/cas/login".toHttpUrl()
        val insecure = "http://qyrz.xjvut.edu.cn/cas/login".toHttpUrl()
        val jw = "https://jw.xjvut.edu.cn:6082/sso".toHttpUrl()
        val route = Cookie.parse(cas, "route=private; Path=/")!!
        vault.saveFromResponse(cas, listOf(route))
        assertTrue(vault.loadForRequest(insecure).isEmpty())
        vault.saveFromResponse(insecure, listOf(Cookie.parse(insecure, "route=injected; Path=/")!!))
        vault.saveFromResponse(jw, listOf(Cookie.parse(cas, "route=foreign; Path=/")!!))
        assertEquals("private", vault.loadForRequest(cas).single().value)
        vault.saveFromResponse("https://evil.example/".toHttpUrl(), listOf(route))
        assertEquals("private", vault.loadForRequest(cas).single().value)
    }
}
