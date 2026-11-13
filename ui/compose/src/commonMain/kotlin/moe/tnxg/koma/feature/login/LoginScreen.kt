package moe.tnxg.koma.feature.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.tnxg.koma.app.state.AppController
import moe.tnxg.koma.app.state.LoginPhase
import moe.tnxg.koma.feature.login.form.CredentialForm
import moe.tnxg.koma.ui.component.button.KomaButton
import moe.tnxg.koma.ui.component.button.KomaTextButton
import moe.tnxg.koma.ui.component.loader.KomaLoader
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme

@Composable
fun LoginScreen(app: AppController) {
    val colors = KomaTheme.colors
    LaunchedEffect(app) { app.prepare() }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 34.dp)) {
            KomaText("登录教务", size = 26, weight = FontWeight.ExtraBold, letterSpacing = -.52f, lineHeight = 1.2f)
            Spacer(Modifier.height(6.dp))
            KomaText("新疆理工职业大学 · 统一身份认证", size = 12.5f, weight = FontWeight.Medium, color = colors.ink4)
            when (app.phase) {
                LoginPhase.Idle, LoginPhase.Preparing, LoginPhase.Importing -> {
                    Column(Modifier.fillMaxWidth().padding(vertical = 72.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        KomaLoader(20.dp)
                        KomaText(if (app.phase == LoginPhase.Importing) "正在读取课表" else "正在连接统一身份认证",
                            size = 12.5f, weight = FontWeight.Medium, color = colors.ink4)
                    }
                }
                LoginPhase.Error -> {
                    Spacer(Modifier.height(24.dp))
                    Column(Modifier.fillMaxWidth().background(colors.surface, RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        KomaText("暂时无法登录", size = 13.5f, weight = FontWeight.Bold)
                        KomaText(app.message, size = 12.5f, weight = FontWeight.Medium, color = colors.ink4)
                        KomaTextButton("重试", app::prepare, Modifier.padding(top = 8.dp))
                    }
                }
                else -> {
                    Spacer(Modifier.height(22.dp))
                    CredentialForm(app)
                    if (app.message.isNotEmpty()) KomaText(app.message,
                        Modifier.padding(start = 2.dp, top = 10.dp), 12.5f, FontWeight.Medium, colors.danger)
                }
            }
        }
        KomaButton(
            text = when (app.phase) {
                LoginPhase.Error -> "重试"
                LoginPhase.Importing -> "读取课表"
                LoginPhase.LoggingIn -> "登录中"
                else -> "登录"
            },
            onClick = if (app.phase == LoginPhase.Error) app::prepare else app::submit,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 22.dp),
            enabled = app.phase == LoginPhase.Error || app.canSubmit,
            loading = app.busy,
        )
    }
}
