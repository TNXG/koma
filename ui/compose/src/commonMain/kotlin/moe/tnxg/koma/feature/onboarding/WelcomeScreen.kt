package moe.tnxg.koma.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.tnxg.koma.resources.Res
import moe.tnxg.koma.resources.koma_icon
import moe.tnxg.koma.ui.component.button.KomaButton
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.icon.KomaIcon
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme
import org.jetbrains.compose.resources.painterResource

private val features = listOf(
    KomaGlyph.Calendar to "今天上什么课，一眼看清",
    KomaGlyph.Location to "几点上下课，下一节去哪",
    KomaGlyph.Refresh to "导入一次，离线也能看",
)

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    val colors = KomaTheme.colors
    val iconShape = RoundedCornerShape(24.dp)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 430.dp).fillMaxSize()) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 48.dp)) {
                Image(painterResource(Res.drawable.koma_icon), "Koma Pro",
                    Modifier.size(84.dp).shadow(14.dp, iconShape, ambientColor = colors.accent,
                        spotColor = colors.accent).clip(iconShape).border(1.dp, colors.line2, iconShape))
                Spacer(Modifier.height(40.dp))
                KomaText("欢迎使用 Koma Pro", size = 30, weight = FontWeight.ExtraBold,
                    letterSpacing = -.6f, lineHeight = 1.25f)
                Spacer(Modifier.height(10.dp))
                KomaText("登录学校教务导入课表，之后的每一天交给它。", size = 14, color = colors.ink2)
                Spacer(Modifier.height(36.dp))
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    features.forEach { (glyph, text) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            KomaIcon(glyph, null, Modifier.size(20.dp), color = colors.accent)
                            KomaText(text, Modifier.padding(start = 14.dp), size = 14,
                                weight = FontWeight.Medium)
                        }
                    }
                }
            }
            KomaText("列车终将开往下一站，那舞台呢？我们呢？",
                Modifier.align(Alignment.CenterHorizontally).padding(horizontal = 28.dp),
                size = 12, color = colors.ink4)
            KomaButton("开始", onStart,
                Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 22.dp))
        }
    }
}
