package moe.tnxg.koma.platform.migration

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayInputStream
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LegacyTimetableMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun importsSnapshotAndDateOnceWithoutRemovingOldStorage() = withLegacyStorage { view ->
        val name = "koma_migration_test_${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        try {
            seed(view, "{\"courses\":[]}", "2026-09-07")
            val migration = LegacyTimetableMigration(context, prefs)
            migration.migrate()
            assertEquals("{\"courses\":[]}", prefs.getString("koma.snapshot", null))
            assertEquals("2026-09-07", prefs.getString("koma.startDate", null))
            assertTrue(prefs.getBoolean(LegacyTimetableMigration.MARKER, false))
            assertEquals("2026-09-07", evaluate(view, "localStorage.getItem('koma.startDate')"))
            seed(view, "{\"changed\":true}", "2026-10-05")
            migration.migrate()
            assertEquals("2026-09-07", prefs.getString("koma.startDate", null))
        } finally {
            context.deleteSharedPreferences(name)
        }
    }

    @Test
    fun neverOverwritesSettingsAlreadySavedByNativeApp() = withLegacyStorage { view ->
        val name = "koma_migration_test_${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        try {
            seed(view, "old", "2025-09-01")
            prefs.edit().putString("koma.snapshot", "native").putString("koma.startDate", "2026-09-07").commit()
            LegacyTimetableMigration(context, prefs).migrate()
            assertEquals("native", prefs.getString("koma.snapshot", null))
            assertEquals("2026-09-07", prefs.getString("koma.startDate", null))
        } finally {
            context.deleteSharedPreferences(name)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun withLegacyStorage(block: suspend (WebView) -> Unit) = runBlocking {
        withContext(Dispatchers.Main.immediate) {
            val view = WebView(context)
            var original: JSONObject? = null
            try {
                view.settings.javaScriptEnabled = true
                view.settings.domStorageEnabled = true
                withTimeout(10_000) {
                    suspendCancellableCoroutine<Unit> { continuation ->
                        view.webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest) =
                                WebResourceResponse("text/html", "UTF-8", ByteArrayInputStream("<html></html>".toByteArray()))
                            override fun onPageFinished(view: WebView, url: String) {
                                if (continuation.isActive) continuation.resume(Unit)
                            }
                        }
                        view.loadUrl(LegacyTimetableMigration.ORIGIN)
                    }
                }
                original = JSONObject(evaluate(view, "JSON.stringify({snapshot:localStorage.getItem('koma.snapshot'),date:localStorage.getItem('koma.startDate')})")!!)
                block(view)
            } finally {
                original?.let { old ->
                    for ((field, key) in listOf("snapshot" to "koma.snapshot", "date" to "koma.startDate")) {
                        val action = if (old.isNull(field)) "localStorage.removeItem('$key')"
                            else "localStorage.setItem('$key',${JSONObject.quote(old.getString(field))})"
                        evaluate(view, action)
                    }
                }
                view.destroy()
            }
        }
    }

    private suspend fun seed(view: WebView, snapshot: String, date: String) {
        evaluate(view, "localStorage.setItem('koma.snapshot',${JSONObject.quote(snapshot)});localStorage.setItem('koma.startDate',${JSONObject.quote(date)})")
    }

    private suspend fun evaluate(view: WebView, script: String): String? = withTimeout(10_000) {
        suspendCancellableCoroutine { continuation ->
            view.evaluateJavascript(script) { encoded ->
                if (continuation.isActive) continuation.resume(JSONTokener(encoded).nextValue() as? String)
            }
        }
    }
}
