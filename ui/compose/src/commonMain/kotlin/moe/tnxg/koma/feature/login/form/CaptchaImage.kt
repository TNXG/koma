package moe.tnxg.koma.feature.login.form

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

@OptIn(ExperimentalEncodingApi::class, ExperimentalResourceApi::class)
@Composable
fun CaptchaImage(dataUrl: String, onRefresh: () -> Unit) {
    val bitmap = remember(dataUrl) { Base64.decode(dataUrl.substringAfter(',')).decodeToImageBitmap() }
    Box(Modifier.size(104.dp, 34.dp).clip(RoundedCornerShape(8.dp)).background(Color.White)
        .clickable(remember { MutableInteractionSource() }, null, onClick = onRefresh)) {
        Image(bitmap, "换一张验证码", Modifier.size(104.dp, 34.dp), contentScale = ContentScale.Fit)
    }
}
