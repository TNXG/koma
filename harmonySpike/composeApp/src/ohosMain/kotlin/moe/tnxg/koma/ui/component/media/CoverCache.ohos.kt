package moe.tnxg.koma.ui.component.media

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** OHOS 端封面下载依赖网络层移植（M1 未接入），返回明确失败由 UI 的 fallback 路径处理。 */
internal actual suspend fun downloadCoverBytes(url: String): ByteArray =
    throw IllegalStateException("OHOS 网络层尚未接入")

/** 图片解码走 OHOS skiko 后端（与 iOS/Desktop 同源），行为与 Android 端一致。 */
internal actual suspend fun decodePhotoBytes(bytes: ByteArray): ImageBitmap =
    withContext(Dispatchers.Default) {
        org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
    }
