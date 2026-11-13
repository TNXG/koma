package moe.tnxg.koma.platform.migration

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Reads the old origin in the existing WebView profile without launching a web application. */
internal class LegacyTimetableMigration(
    private val context: Context,
    private val preferences: SharedPreferences,
) {
    suspend fun migrate() = migrationLock.withLock {
        if (preferences.getBoolean(MARKER, false)) return@withLock
        val legacy = readLegacyTimetable()
        val editor = preferences.edit()
        if (!preferences.contains("koma.snapshot") && legacy.first != null) {
            editor.putString("koma.snapshot", legacy.first)
        }
        if (!preferences.contains("koma.startDate") && legacy.second != null) {
            editor.putString("koma.startDate", legacy.second)
        }
        // A crash or storage failure must retry extraction, never mark before the data is durable.
        check(editor.putBoolean(MARKER, true).commit()) { "旧课表迁移保存失败" }
    }

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun readLegacyTimetable(): Pair<String?, String?> = withContext(Dispatchers.Main.immediate) {
        val webView = WebView(context)
        try {
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.allowFileAccess = false
            webView.settings.allowContentAccess = false
            withTimeout(10_000) {
                suspendCancellableCoroutine { continuation ->
                    webView.webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                            // Serve only a local empty document. No network, bridge, or legacy JavaScript.
                            return WebResourceResponse("text/html", "UTF-8", ByteArrayInputStream(HTML))
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            if (!continuation.isActive || url != ORIGIN) return
                            view.evaluateJavascript(SCRIPT) { encoded ->
                                if (!continuation.isActive) return@evaluateJavascript
                                try {
                                    val serialized = JSONTokener(encoded).nextValue() as? String
                                        ?: error("无法读取旧课表存储")
                                    val json = JSONObject(serialized)
                                    check(!json.has("error")) { "旧课表存储不可读取" }
                                    fun value(key: String): String? =
                                        if (json.isNull(key)) null else json.getString(key).takeUnless { it == "null" }
                                    continuation.resume(value("snapshot") to value("startDate"))
                                } catch (error: Exception) {
                                    continuation.resumeWithException(error)
                                }
                            }
                        }
                    }
                    webView.loadUrl(ORIGIN)
                }
            }
        } finally {
            webView.stopLoading()
            webView.webViewClient = WebViewClient()
            webView.destroy()
        }
    }

    companion object {
        const val MARKER = "migration.capacitor.localStorage.v1"
        const val ORIGIN = "https://localhost/"
        private val migrationLock = Mutex()
        private val HTML = "<!doctype html><html><head><meta charset=\"utf-8\"></head><body></body></html>".toByteArray()
        private const val SCRIPT = """
            (function() {
              try { return JSON.stringify({snapshot: localStorage.getItem('koma.snapshot'), startDate: localStorage.getItem('koma.startDate')}); }
              catch(e) { return JSON.stringify({error: true}); }
            })()
        """
    }
}
