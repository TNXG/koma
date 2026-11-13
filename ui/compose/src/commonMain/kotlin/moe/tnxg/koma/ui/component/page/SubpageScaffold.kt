package moe.tnxg.koma.ui.component.page

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.icon.KomaIcon
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.platform.PlatformBackHandler
import moe.tnxg.koma.ui.platform.swipeBackToPop
import moe.tnxg.koma.ui.theme.KomaTheme

/**
 * Every pushed page (settings and profile alike): round back button, large title, one column of
 * grouped rows. Back works from the button, the system gesture, and a left-edge swipe.
 */
@Composable
fun SubpageScaffold(
    title: String,
    onBack: () -> Unit,
    topRight: (@Composable () -> Unit)? = null,
    error: String? = null,
    onDismissError: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = KomaTheme.colors
    PlatformBackHandler(enabled = true, onBack = onBack)
    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .swipeBackToPop(onBack)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 20.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackButton(onBack)
                Spacer(Modifier.weight(1f))
                if (topRight != null) topRight()
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.fillMaxWidth().widthIn(max = 540.dp)) {
                    KomaText(
                        text = title,
                        size = 29,
                        weight = FontWeight.ExtraBold,
                        color = c.ink,
                        letterSpacing = -1.1f,
                        lineHeight = 1.35f,
                    )
                    Spacer(Modifier.height(18.dp))
                    if (!error.isNullOrBlank()) {
                        ErrorBanner(error, onDismissError)
                        Spacer(Modifier.height(14.dp))
                    }
                    content()
                }
            }
        }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    val c = KomaTheme.colors
    Box(
        modifier = Modifier
            .size(44.dp)
            .semantics {
                role = Role.Button
                contentDescription = "返回"
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onBack,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(c.surface),
            contentAlignment = Alignment.Center,
        ) {
            KomaIcon(
                glyph = KomaGlyph.Left,
                description = null,
                modifier = Modifier.size(18.dp),
                color = c.ink,
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    val c = KomaTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.danger.copy(alpha = 0.10f), shape)
            .border(1.dp, c.danger.copy(alpha = 0.28f), shape)
            .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KomaText(
            text = message,
            modifier = Modifier.weight(1f),
            size = 12,
            weight = FontWeight.Medium,
            color = c.danger,
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .semantics { role = Role.Button }
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            KomaIcon(KomaGlyph.CloseCircle, description = "关闭提示", modifier = Modifier.size(16.dp), color = c.danger)
        }
    }
}
