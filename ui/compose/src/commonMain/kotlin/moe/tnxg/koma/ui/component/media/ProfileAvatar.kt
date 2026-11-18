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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import moe.tnxg.koma.ui.component.loader.KomaLoader
import moe.tnxg.koma.ui.theme.KomaTheme

/** Stable identity image: the user's chosen photo, otherwise the generated avatar for [seed]. */
@Composable
fun ProfileAvatar(seed: String, photoPath: String, modifier: Modifier = Modifier) {
    if (photoPath.isNotBlank()) {
        LocalPhoto(path = photoPath, modifier = modifier)
    } else {
        KodamaAvatar(seed = seed, modifier = modifier, animate = false)
    }
}

@Composable
internal fun LocalPhoto(path: String, modifier: Modifier = Modifier) {
    val services = LocalImageServices.current
    val c = KomaTheme.colors
    var bitmap by remember(path) { mutableStateOf(ProfilePhotoCache.get(path)) }

    LaunchedEffect(services, path) {
        if (bitmap != null) return@LaunchedEffect
        bitmap = try {
            ProfilePhotoCache.photo(services, path)
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
