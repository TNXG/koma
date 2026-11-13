package moe.tnxg.koma.ui.component.media

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import moe.tnxg.koma.app.preferences.LocalAppPreferences
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.icon.KomaIcon
import moe.tnxg.koma.ui.component.loader.KomaLoader
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme

/**
 * Profile cover: a chosen photo, or the random image kept on device for three days at a time.
 * [refreshToken] forces one download when it changes.
 */
@Composable
fun ProfileCover(
    photoPath: String,
    modifier: Modifier = Modifier,
    refreshToken: Int = 0,
    alignment: Alignment = Alignment.Center,
) {
    val c = KomaTheme.colors
    val services = LocalImageServices.current
    val reduceMotion = LocalAppPreferences.current.reduceMotion
    var state by remember { mutableStateOf<CoverState>(CoverState.Loading) }
    var reloadNonce by remember { mutableIntStateOf(0) }

    LaunchedEffect(services, photoPath, refreshToken, reloadNonce) {
        state = CoverState.Loading
        state = try {
            CoverState.Ready(
                resolveCover(
                    services = services,
                    photoPath = photoPath,
                    forceRefresh = refreshToken > 0 || reloadNonce > 0,
                ),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            CoverState.Error(failure.message?.takeIf { it.isNotBlank() } ?: "封面加载失败")
        }
    }

    val fadeAlpha by animateFloatAsState(
        targetValue = if (state is CoverState.Ready) 1f else 0f,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 280),
        label = "coverFade",
    )

    Box(modifier = modifier.background(c.surface2)) {
        when (val current = state) {
            CoverState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                KomaLoader(size = 18.dp, color = c.ink4)
            }

            is CoverState.Ready -> Image(
                bitmap = current.bitmap,
                contentDescription = "封面",
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = fadeAlpha },
                alignment = alignment,
                contentScale = ContentScale.Crop,
            )

            is CoverState.Error -> {
                val pillShape = RoundedCornerShape(999.dp)
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(pillShape)
                        .background(c.surface.copy(alpha = 0.92f))
                        .border(1.dp, c.line, pillShape)
                        .semantics { role = Role.Button }
                        .clickable { reloadNonce++ }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KomaIcon(KomaGlyph.Refresh, description = null, modifier = Modifier.size(13.dp), color = c.ink2)
                    KomaText("加载失败 · 重试", size = 11, weight = FontWeight.SemiBold, color = c.ink2)
                }
            }
        }
    }
}
