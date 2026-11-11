package moe.tnxg.koma.http

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CookieVaultInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val prefsName = "koma_cookies_instr_test"

    @Before
    fun setUp() {
        context.deleteSharedPreferences(prefsName)
    }

    @After
    fun tearDown() {
        context.deleteSharedPreferences(prefsName)
    }

    @Test
    fun pathScopedCookieIsReadableFromOrigin() {
        val vault = CookieVault(context, prefsName = prefsName)

        assertTrue(vault.store(listOf("https://qjyz.example" to listOf("CASTGC=abc; Path=/cas"))))

        // 按 origin 读（CookieManager 会因 Path=/cas 过滤掉，这里必须读得到）
        assertEquals("CASTGC=abc", vault.read("https://qjyz.example"))
        assertEquals("CASTGC=abc", vault.read("https://qjyz.example/cas/login"))
    }

    @Test
    fun clearOnlyTouchesGivenOrigin() {
        val vault = CookieVault(context, prefsName = prefsName)
        vault.store(
            listOf(
                "https://a.example" to listOf("A=1; Path=/"),
                "https://b.example" to listOf("B=2; Path=/"),
            ),
        )

        vault.clear(listOf("https://a.example"))

        assertNull(vault.read("https://a.example"))
        assertEquals("B=2", vault.read("https://b.example"))
    }

    @Test
    fun clearWithPathPrefixKeepsOtherPaths() {
        val vault = CookieVault(context, prefsName = prefsName)
        vault.store(
            listOf(
                "https://jw.example:6082" to listOf("ROOT=1; Path=/"),
                "https://jw.example:6082/jwglxt/ticketlogin" to listOf("JSESSIONID=abc"),
            ),
        )
        // 无 Path 的 Cookie 按 RFC 6265 从 URL 推导：/jwglxt/ticketlogin → /jwglxt
        assertEquals("ROOT=1; JSESSIONID=abc", vault.read("https://jw.example:6082"))

        vault.clear(listOf("https://jw.example:6082/jwglxt/"))

        assertEquals("ROOT=1", vault.read("https://jw.example:6082"))
    }

    @Test
    fun cookiesSurviveNewVaultInstance() {
        CookieVault(context, prefsName = prefsName)
            .store(listOf("https://qjyz.example" to listOf("CASTGC=persisted; Path=/cas")))

        // 新实例等价于进程重启后重新构造
        val reopened = CookieVault(context, prefsName = prefsName)
        assertEquals("CASTGC=persisted", reopened.read("https://qjyz.example"))
    }

    @Test
    fun prefsFileDoesNotContainCookieValue() {
        val vault = CookieVault(context, prefsName = prefsName)
        vault.store(listOf("https://qjyz.example" to listOf("CASTGC=plain-secret-value; Path=/cas")))

        val xml = File(context.applicationInfo.dataDir, "shared_prefs/$prefsName.xml").readText()
        assertFalse(xml.contains("plain-secret-value"))
        assertFalse(xml.contains("CASTGC"))
    }

    @Test
    fun maxAgeZeroRemovesCookie() {
        val vault = CookieVault(context, prefsName = prefsName)
        vault.store(listOf("https://a.example" to listOf("S=1; Path=/")))
        vault.store(listOf("https://a.example" to listOf("S=; Path=/; Max-Age=0")))
        assertNull(vault.read("https://a.example"))
    }
}
