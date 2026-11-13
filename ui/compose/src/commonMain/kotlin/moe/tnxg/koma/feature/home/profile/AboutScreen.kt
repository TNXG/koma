package moe.tnxg.koma.feature.home.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import moe.tnxg.koma.resources.Res
import moe.tnxg.koma.resources.koma_icon
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.icon.KomaIcon
import moe.tnxg.koma.ui.component.list.SettingsDivider
import moe.tnxg.koma.ui.component.list.SettingsGroup
import moe.tnxg.koma.ui.component.list.SettingsNavRow
import moe.tnxg.koma.ui.component.list.SettingsSection
import moe.tnxg.koma.ui.component.media.decodePhotoBytes
import moe.tnxg.koma.ui.component.media.downloadCoverBytes
import moe.tnxg.koma.ui.component.page.SubpageScaffold
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme
import org.jetbrains.compose.resources.painterResource

const val APP_VERSION = "0.2.0"
const val PROJECT_URL = "https://github.com/tnxg/koma"
private const val PROJECT_HOST = "github.com/tnxg/koma"
private const val ISSUES_URL = "$PROJECT_URL/issues"
private const val AGREEMENT = "AGPL-3.0"
private const val SLOGAN = "列车终将开往下一站，那舞台呢？我们呢？"
private const val MAINTAINER_URL = "https://github.com/TNXG"
private const val MAINTAINER_AVATAR = "https://api-space.tnxg.top/avatar?s=qq"

/** Decoded once per URL so returning to this page does not refetch the avatars. */
private val developerAvatarCache = mutableMapOf<String, ImageBitmap>()

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onLicenses: () -> Unit,
    onPrivacy: () -> Unit,
) {
    val c = KomaTheme.colors
    val uriHandler = LocalUriHandler.current
    val open = { url: String -> runCatching { uriHandler.openUri(url) } }

    SubpageScaffold(title = "关于", onBack = onBack) {
        AboutHeroCard()

        SettingsSection("支持")
        SettingsGroup {
            SettingsNavRow(
                title = "开源许可",
                glyph = KomaGlyph.Book,
                value = "3 项",
                onClick = onLicenses,
            )
            SettingsDivider()
            SettingsNavRow(title = "隐私与权限", glyph = KomaGlyph.Eye, onClick = onPrivacy)
            SettingsDivider()
            SettingsNavRow(
                title = "反馈问题",
                glyph = KomaGlyph.Message,
                onClick = { open(ISSUES_URL) },
            )
        }

        SettingsSection("关于项目")
        SettingsGroup {
            SettingsNavRow(
                title = "项目主页",
                glyph = KomaGlyph.Code,
                value = PROJECT_HOST,
                onClick = { open(PROJECT_URL) },
            )
            SettingsDivider()
            DeveloperRow(
                name = "TNXG",
                role = "开发者 · GitHub.com",
                initial = "T",
                avatarUrl = MAINTAINER_AVATAR,
                tint = c.accent,
                onClick = { open(MAINTAINER_URL) },
            )
        }

        KomaText(
            text = "Koma 以 $AGREEMENT 发布。课程数据与账号凭据只保存在本机。",
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 18.dp),
            size = 11.5f,
            weight = FontWeight.Medium,
            color = c.ink5,
            lineHeight = 1.75f,
        )
    }
}

@Composable
private fun AboutHeroCard() {
    val c = KomaTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(c.surface)
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(Res.drawable.koma_icon),
            contentDescription = null,
            modifier = Modifier.size(84.dp).clip(RoundedCornerShape(23.dp)),
        )
        KomaText(
            text = "Koma",
            modifier = Modifier.padding(top = 14.dp),
            size = 24,
            weight = FontWeight.ExtraBold,
            color = c.ink,
            letterSpacing = -0.6f,
        )
        KomaText(
            text = "版本 $APP_VERSION",
            modifier = Modifier.padding(top = 5.dp),
            size = 12.5f,
            weight = FontWeight.Medium,
            color = c.ink4,
        )
        KomaText(
            text = SLOGAN,
            modifier = Modifier.padding(top = 10.dp),
            size = 12.5f,
            color = c.ink3,
            lineHeight = 1.6f,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(c.surface2)
                .padding(vertical = 11.dp),
        ) {
            MetaCell(value = APP_VERSION, label = "版本", modifier = Modifier.weight(1f))
            MetaCell(value = "Android", label = "平台", modifier = Modifier.weight(1f))
            MetaCell(value = AGREEMENT, label = "协议", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetaCell(value: String, label: String, modifier: Modifier) {
    val c = KomaTheme.colors
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        KomaText(
            text = value,
            size = 13,
            weight = FontWeight.Bold,
            color = c.ink,
            letterSpacing = -0.2f,
        )
        KomaText(
            text = label,
            modifier = Modifier.padding(top = 2.dp),
            size = 10,
            weight = FontWeight.Medium,
            color = c.ink4,
        )
    }
}

@Composable
private fun DeveloperRow(
    name: String,
    role: String,
    initial: String,
    avatarUrl: String,
    tint: Color,
    onClick: () -> Unit,
) {
    val c = KomaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DeveloperAvatar(url = avatarUrl, initial = initial, tint = tint)
        Column(Modifier.weight(1f)) {
            KomaText(name, size = 14, weight = FontWeight.Bold, color = c.ink, letterSpacing = -0.2f)
            KomaText(
                text = role,
                modifier = Modifier.padding(top = 1.dp),
                size = 11.5f,
                weight = FontWeight.Medium,
                color = c.ink4,
            )
        }
        KomaIcon(KomaGlyph.Right, description = null, modifier = Modifier.size(15.dp), color = c.ink5)
    }
}

/** The real avatar when the host can fetch it; the initial only covers loading and failure. */
@Composable
private fun DeveloperAvatar(url: String, initial: String, tint: Color) {
    var bitmap by remember(url) { mutableStateOf(developerAvatarCache[url]) }
    LaunchedEffect(url) {
        if (bitmap == null) {
            bitmap = try {
                decodePhotoBytes(downloadCoverBytes(url)).also { developerAvatarCache[url] = it }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }
    }
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            KomaText(initial, size = 14, weight = FontWeight.Bold, color = tint)
        }
    }
}
