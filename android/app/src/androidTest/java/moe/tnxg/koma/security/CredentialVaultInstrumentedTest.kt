package moe.tnxg.koma.security

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
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CredentialVaultInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var alias: String
    private lateinit var prefsName: String
    private lateinit var vault: CredentialVault

    @Before
    fun setUp() {
        val id = UUID.randomUUID().toString()
        alias = "koma.test.credentials.$id"
        prefsName = "koma_credentials_test_$id"
        vault = CredentialVault(context, KeystoreCipher(alias), prefsName)
    }

    @After
    fun tearDown() {
        context.deleteSharedPreferences(prefsName)
        KeystoreCipher(alias).deleteKey()
    }

    @Test
    fun saveLoadRemoveRoundTrip() {
        assertTrue(vault.isAvailable())
        assertTrue(vault.save("xjvut", "2023001", "p@ss-中文"))

        val loaded = vault.load("xjvut")
        assertEquals("2023001", loaded?.username)
        assertEquals("p@ss-中文", loaded?.password)

        vault.remove("xjvut")
        assertNull(vault.load("xjvut"))
    }

    @Test
    fun schoolIdsAreIsolatedAndAadMismatchClearsEntry() {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        vault.save("school-a", "ua", "pa")
        val keyA = prefs.all.keys.single()
        vault.save("school-b", "ub", "pb")
        val keyB = prefs.all.keys.first { it != keyA }

        // 把 B 的密文挪到 A 的键下：AAD 不匹配，必须解不开并删除
        prefs.edit().putString(keyA, prefs.getString(keyB, "")).commit()

        assertNull(vault.load("school-a"))
        assertFalse(prefs.contains(keyA))
        assertEquals("ub", vault.load("school-b")?.username)
    }

    @Test
    fun tamperedCiphertextIsDropped() {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        vault.save("xjvut", "u", "p")
        val key = prefs.all.keys.single()
        prefs.edit().putString(key, "{\"v\":1,\"iv\":\"AAAA\",\"ct\":\"AAAA\"}").commit()

        assertNull(vault.load("xjvut"))
        assertFalse(prefs.contains(key))
    }

    @Test
    fun missingKeystoreEntryDoesNotCrash() {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        vault.save("xjvut", "u", "p")
        val key = prefs.all.keys.single()

        KeystoreCipher(alias).deleteKey()

        assertNull(vault.load("xjvut"))
        assertFalse(prefs.contains(key))
    }

    @Test
    fun prefsFileContainsNoPlaintext() {
        val username = "plain-username-42"
        val password = "plain-password-42"
        vault.save("xjvut", username, password)

        val xml = File(context.applicationInfo.dataDir, "shared_prefs/$prefsName.xml").readText()
        assertFalse(xml.contains(username))
        assertFalse(xml.contains(password))
        assertTrue(xml.contains("<string name="))
    }

    @Test
    fun unavailableKeystoreFailsSafely() {
        val unavailable = object : KeystoreCipher(alias) {
            override fun isAvailable(): Boolean = false
            override fun encrypt(plaintext: String, aad: ByteArray): String? = null
            override fun decrypt(blob: String, aad: ByteArray): String? = null
        }
        val stub = CredentialVault(context, unavailable, prefsName)

        assertFalse(stub.isAvailable())
        assertFalse(stub.save("xjvut", "u", "p"))
        assertNull(stub.load("xjvut"))
    }
}
