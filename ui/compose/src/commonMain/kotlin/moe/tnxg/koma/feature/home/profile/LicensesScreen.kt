package moe.tnxg.koma.feature.home.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.tnxg.koma.resources.Res
import moe.tnxg.koma.ui.component.list.SettingsDivider
import moe.tnxg.koma.ui.component.list.SettingsGroup
import moe.tnxg.koma.ui.component.list.SettingsNavRow
import moe.tnxg.koma.ui.component.loader.KomaLoader
import moe.tnxg.koma.ui.component.page.SubpageScaffold
import moe.tnxg.koma.ui.component.sheet.KomaSheet
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme
import org.jetbrains.compose.resources.ExperimentalResourceApi

private class LicenseEntry(val name: String, val file: String, val license: String)

private val LICENSE_ENTRIES = listOf(
    LicenseEntry(name = "Koma", file = "Koma.txt", license = "AGPL-3.0"),
    LicenseEntry(name = "Kotlin", file = "Kotlin.txt", license = "Apache-2.0"),
    LicenseEntry(name = "kotlinx.coroutines", file = "KotlinxCoroutines.txt", license = "Apache-2.0"),
    LicenseEntry(name = "kotlinx.serialization", file = "KotlinxSerialization.txt", license = "Apache-2.0"),
    LicenseEntry(name = "kotlinx-datetime", file = "KotlinxDatetime.txt", license = "Apache-2.0"),
    LicenseEntry(name = "Compose Multiplatform", file = "ComposeMultiplatform.txt", license = "Apache-2.0"),
    LicenseEntry(name = "AndroidX", file = "AndroidX.txt", license = "Apache-2.0"),
    LicenseEntry(name = "Miuix", file = "Miuix.txt", license = "Apache-2.0"),
    LicenseEntry(name = "MingCute Icons", file = "MingCute.txt", license = "Apache-2.0"),
    LicenseEntry(name = "Inter", file = "Inter.txt", license = "SIL OFL 1.1"),
    LicenseEntry(name = "Kodama", file = "Kodama.txt", license = "MIT"),
    LicenseEntry(name = "OkHttp", file = "OkHttp.txt", license = "Apache-2.0"),
    LicenseEntry(name = "Jsoup", file = "Jsoup.txt", license = "MIT"),
    LicenseEntry(name = "ONNX Runtime", file = "OnnxRuntime.txt", license = "MIT"),
    LicenseEntry(name = "ddddocr", file = "Ddddocr.txt", license = "MIT"),
)

/** 关于页显示条目数，避免新增条目后计数失配。 */
internal val LICENSE_ENTRY_COUNT = LICENSE_ENTRIES.size

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    var opened by remember { mutableStateOf<LicenseEntry?>(null) }

    SubpageScaffold(title = "开源许可", onBack = onBack) {
        SettingsGroup {
            LICENSE_ENTRIES.forEachIndexed { index, entry ->
                if (index > 0) SettingsDivider()
                SettingsNavRow(
                    title = entry.name,
                    value = entry.license,
                    onClick = { opened = entry },
                )
            }
        }
    }

    LicenseTextSheet(entry = opened, onDismiss = { opened = null })
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun LicenseTextSheet(entry: LicenseEntry?, onDismiss: () -> Unit) {
    val c = KomaTheme.colors
    var shown by remember { mutableStateOf<LicenseEntry?>(null) }
    var body by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(entry) {
        if (entry != null) {
            shown = entry
            body = runCatching {
                Res.readBytes("files/licenses/${entry.file}").decodeToString()
            }.getOrNull()
        }
    }

    KomaSheet(open = entry != null, onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth()) {
            KomaText(
                text = shown?.name.orEmpty(),
                modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 8.dp),
                size = 17,
                weight = FontWeight.Bold,
                color = c.ink,
                letterSpacing = -0.4f,
            )
            Box(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                val text = body
                if (text == null) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                        KomaLoader(size = 18.dp, color = c.ink4)
                    }
                } else {
                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                        KomaText(
                            text = text,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                            size = 11,
                            color = c.ink2,
                            lineHeight = 1.6f,
                        )
                    }
                }
            }
        }
    }
}
