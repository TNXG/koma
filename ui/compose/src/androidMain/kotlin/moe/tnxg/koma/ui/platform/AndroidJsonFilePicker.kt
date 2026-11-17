package moe.tnxg.koma.ui.platform

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

@Composable
actual fun rememberJsonFilePicker(onResult: (Result<String>) -> Unit): () -> Unit {
    val resolver = LocalContext.current.applicationContext.contentResolver
    val callback = rememberUpdatedState(onResult)
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val result = try {
                    Result.success(withContext(Dispatchers.IO) { readJsonDocument(resolver, uri) })
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    val message = when (failure) {
                        is SecurityException -> "无法访问这个文件，请重新选择或将文件保存到本机后重试。"
                        is CharacterCodingException -> "文件不是有效的 UTF-8 文本，请转换为 UTF-8 编码后重试。"
                        is IllegalArgumentException -> failure.message ?: "无法读取这个文件，请选择 JSON 文本文件。"
                        is IOException -> "文件读取失败，请确认文件可用，或下载到本机后重试。"
                        else -> "无法读取这个文件，请重新选择，或直接粘贴 JSON 内容。"
                    }
                    Result.failure(IllegalArgumentException(message, failure))
                }
                callback.value(result)
            }
        }
    }
    return remember(launcher) {
        {
            try {
                // Some document providers report JSON as plain text or binary data.
                launcher.launch(arrayOf("application/json", "text/*", "application/octet-stream", "*/*"))
            } catch (failure: Exception) {
                callback.value(Result.failure(IllegalStateException(
                    "无法打开文件选择器，请直接粘贴 JSON 内容。", failure,
                )))
            }
        }
    }
}

private suspend fun readJsonDocument(resolver: ContentResolver, uri: Uri): String {
    val bytes = resolver.openInputStream(uri)?.use { input ->
        ByteArrayOutputStream().use { output ->
            val buffer = ByteArray(8192)
            while (true) {
                coroutineContext.ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
    } ?: throw IOException("Document provider did not open the selected file")
    return StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes))
        .toString()
        .removePrefix("\uFEFF")
}
