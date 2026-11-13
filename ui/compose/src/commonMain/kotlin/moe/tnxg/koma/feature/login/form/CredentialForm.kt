package moe.tnxg.koma.feature.login.form

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.icon.KomaIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.tnxg.koma.app.state.AppController
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.component.text.komaFontFamily
import moe.tnxg.koma.ui.theme.KomaTheme

@Composable
fun CredentialForm(app: AppController) {
    val colors = KomaTheme.colors
    var visible by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surface)) {
        CredentialRow("学号", app.username, { app.username = it }, !app.busy, keyboard = KeyboardType.Number)
        FormDivider()
        CredentialRow("密码", app.password, { app.password = it }, !app.busy,
            transformation = if (visible) VisualTransformation.None else PasswordVisualTransformation()) {
            KomaIcon(if (visible) KomaGlyph.EyeOff else KomaGlyph.Eye,
                if (visible) "隐藏密码" else "显示密码",
                Modifier.size(28.dp).clickable(remember { MutableInteractionSource() }, null) { visible = !visible }
                    .padding(5.5.dp), color = colors.ink4)
        }
        if (app.captcha != null && app.manualCaptcha) {
            FormDivider()
            CredentialRow("验证码", app.captchaInput, { app.captchaInput = it }, !app.busy,
                keyboard = KeyboardType.Number, placeholder = "图中算式得数") {
                CaptchaImage(app.captcha!!, app::refreshCaptcha)
            }
        }
        if (app.platform.canRememberCredentials) {
            FormDivider()
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                KomaText("保存密码", Modifier.weight(1f), 12.5f, FontWeight.Medium, colors.ink4)
                RememberSwitch(app.rememberPassword, !app.busy) { app.rememberPassword = it }
            }
        }
    }
}

@Composable
private fun CredentialRow(
    label: String, value: String, onChange: (String) -> Unit, enabled: Boolean,
    keyboard: KeyboardType = KeyboardType.Text,
    transformation: VisualTransformation = VisualTransformation.None,
    placeholder: String = "", trailing: @Composable () -> Unit = {},
) {
    val colors = KomaTheme.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        KomaText(label, Modifier.width(62.dp), 12.5f, FontWeight.Medium, colors.ink4)
        BasicTextField(value, onChange, Modifier.weight(1f), enabled = enabled, singleLine = true,
            visualTransformation = transformation, keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            textStyle = TextStyle(color = colors.ink, fontFamily = komaFontFamily(), fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, lineHeight = 20.3.sp, fontFeatureSettings = "cv11, ss01"),
            cursorBrush = SolidColor(colors.accent),
            decorationBox = { field ->
                Box { if (value.isEmpty() && placeholder.isNotEmpty()) KomaText(placeholder,
                    size = 14, weight = FontWeight.Medium, color = colors.ink5); field() }
            })
        trailing()
    }
}

@Composable
private fun FormDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(KomaTheme.colors.line2))
}

@Composable
private fun RememberSwitch(checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val colors = KomaTheme.colors
    val background by animateColorAsState(if (checked) colors.accent else colors.line, tween(200))
    val x by animateDpAsState(if (checked) 21.dp else 3.dp,
        tween(240, easing = CubicBezierEasing(.3f, 1.25f, .5f, 1f)))
    Box(Modifier.size(46.dp, 28.dp).clip(CircleShape).background(background)
        .clickable(remember { MutableInteractionSource() }, null, enabled) { onChange(!checked) }) {
        Box(Modifier.offset(x, 3.dp).size(22.dp).shadow(2.dp, CircleShape).background(Color.White, CircleShape))
    }
}
