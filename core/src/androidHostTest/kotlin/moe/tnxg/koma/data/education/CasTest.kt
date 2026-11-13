package moe.tnxg.koma.data.education

import moe.tnxg.koma.data.education.cas.*
import kotlin.test.*

class CasTest {
    @Test fun rsaUsesUnpaddedLegacyLittleEndianBlocks() {
        val modulus = "f".repeat(128)
        assertEquals("7465737431323334", encryptCasPassword("test1234", modulus, "1"))
        assertEquals(2, encryptCasPassword("x".repeat(63), modulus, "10001").split(' ').size)
        assertFailsWith<IllegalArgumentException> { encryptCasPassword("pw", "ff", "10001") }
    }
    @Test fun parsesReorderedHtmlAttributesAndNestedErrorText() {
        assertEquals("e1&x", extractExecution("<input value='e1&amp;x' name='execution'>"))
        assertNull(extractExecution("<form></form>"))
        assertEquals("FailedAuthcodeException: 验证码错误", casErrorText("<span class='errors auth_error'><b>FailedAuthcodeException:</b> 验证码错误</span>"))
        assertEquals("验证码不对", describeCasError("FailedAuthcodeException"))
        assertEquals("账号或密码不对", describeCasError("password not recognized"))
        assertEquals("登录过期了，请重试", describeCasError("Invalid execution flow"))
    }
    @Test fun discoversBothMobileAndDesktopTerms() {
        assertEquals("2025", termFromIndex("<input value='2025' id='xnm_hide'><input id='xqm_hide' value='3'>")?.xnm)
        assertEquals("12", termFromIndex("<select id='xnm'><option selected value='2026'>Y</select><select id='xqm'><option selected value='12'>S</select>")?.xqm)
    }
}
