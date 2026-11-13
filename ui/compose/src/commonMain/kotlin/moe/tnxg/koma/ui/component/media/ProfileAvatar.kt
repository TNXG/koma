package moe.tnxg.koma.ui.component.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import moe.tnxg.koma.ui.component.loader.KomaLoader
import moe.tnxg.koma.ui.theme.KomaTheme

/** The user's own photo when one was chosen, otherwise the avatar generated from [seed]. */
@Composable
fun ProfileAvatar(seed: String, photoPath: String, modifier: Modifier = Modifier) {
    if (photoPath.isNotBlank()) {
        LocalPhoto(path = photoPath, modifier = modifier)
    } else {
        KodamaAvatar(seed = seed, modifier = modifier)
    }
}

@Composable
internal fun LocalPhoto(path: String, modifier: Modifier = Modifier) {
    val services = LocalImageServices.current
    val c = KomaTheme.colors
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(services, path) {
        bitmap = try {
            services?.readPhotoBytes(path)?.let { decodePhotoBytes(it) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    Box(
        modifier = modifier.clip(CircleShape).background(c.surface2),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "头像",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            KomaLoader(size = 16.dp, color = c.ink4)
        }
    }
}
