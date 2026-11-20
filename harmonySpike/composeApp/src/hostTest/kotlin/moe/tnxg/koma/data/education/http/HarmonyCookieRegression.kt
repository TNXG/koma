package moe.tnxg.koma.data.education.http

import io.ktor.http.Url
import io.ktor.http.parseServerSetCookieHeader
import kotlinx.coroutines.runBlocking

/** Compile with the real ohosMain CookieVault.kt and Ktor, using no persistence. */
fun main() = runBlocking {
    val vault = CookieVault(null)
    val cas = Url("https://qyrz.xjvut.edu.cn/cas/login")
    val jw = Url("https://jw.xjvut.edu.cn:6082/sso/zfiotlogin")
    var assertions = 0
    suspend fun expect(url: Url, expected: List<String>) {
        check(vault.get(url).map { it.name } == expected) { "Harmony cookie request-boundary mismatch" }
        assertions++
    }
    suspend fun save(url: Url, raw: String) = vault.addCookie(url, parseServerSetCookieHeader(raw))

    // Public anonymous /cas/login currently emits route at / and a domain-bound
    // iPlanetDirectoryProCAS cookie at /. All values below are synthetic fixtures.
    save(cas, "route=fixture-route; Path=/")
    save(cas, "iPlanetDirectoryProCAS=fixture-cas; Domain=qyrz.xjvut.edu.cn; Path=/; HttpOnly")
    save(cas, "SESSION=fixture-session; Path=/cas; Secure; HttpOnly")
    for (path in listOf("/cas/login", "/cas/v1/getPubKey", "/cas/getKaptchaStatus", "/cas/kaptcha")) {
        expect(Url("https://qyrz.xjvut.edu.cn$path"), listOf("SESSION", "route", "iPlanetDirectoryProCAS"))
    }
    expect(Url("https://qyrz.xjvut.edu.cn/case"), listOf("route", "iPlanetDirectoryProCAS"))
    expect(Url("http://qyrz.xjvut.edu.cn/cas/login"), listOf("route", "iPlanetDirectoryProCAS"))
    expect(jw, emptyList())
    save(jw, "JSESSIONID=fixture-jw; Path=/; Secure; HttpOnly")
    expect(jw, listOf("JSESSIONID"))
    expect(cas, listOf("SESSION", "route", "iPlanetDirectoryProCAS"))
    expect(Url("https://unrelated.example/cas/login"), emptyList())
    save(cas, "SESSION=fixture-replacement; Path=/cas; Secure; HttpOnly")
    check(vault.get(cas).count { it.name == "SESSION" } == 1) { "Harmony cookie replacement mismatch" }
    assertions++
    save(cas, "SESSION=removed; Path=/cas; Max-Age=0; Secure; HttpOnly")
    expect(cas, listOf("route", "iPlanetDirectoryProCAS"))
    vault.clear()
    expect(cas, emptyList())
    expect(jw, emptyList())
    println("Harmony cookie regression: $assertions assertions passed")
}
