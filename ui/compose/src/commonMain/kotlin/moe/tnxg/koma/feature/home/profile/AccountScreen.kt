package moe.tnxg.koma.feature.home.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.list.SettingsDivider
import moe.tnxg.koma.ui.component.list.SettingsGroup
import moe.tnxg.koma.ui.component.list.SettingsNavRow
import moe.tnxg.koma.ui.component.list.SettingsSection
import moe.tnxg.koma.ui.component.page.SubpageScaffold
import moe.tnxg.koma.ui.component.sheet.ConfirmSheet
import moe.tnxg.koma.ui.theme.KomaTheme
import moe.tnxg.koma.app.state.LocalAppController
import moe.tnxg.koma.ui.component.list.SettingsSwitchRow

private fun schoolNameOf(schoolId: String): String = when (schoolId) {
    "xjvut" -> "新疆理工职业大学"
    else -> schoolId
}

@Composable
fun AccountScreen(
    snapshot: TimetableSnapshot,
    onBack: () -> Unit,
    onReimport: () -> Unit,
    onLogout: () -> Unit,
) {
    val c = KomaTheme.colors
    val app = LocalAppController.current
    var confirmLogout by remember { mutableStateOf(false) }

    SubpageScaffold(title = "教务账号", onBack = onBack) {
        SettingsGroup {
            SettingsNavRow(
                title = "学校",
                value = schoolNameOf(snapshot.schoolId),
                chevron = false,
            )
            SettingsDivider()
            SettingsNavRow(
                title = "登录状态",
                value = if (app.syncNeedsLogin) "需要验证" else "本机课表可用",
                tag = snapshot.semester.name,
                chevron = false,
            )
            SettingsDivider()
            SettingsNavRow(
                title = "重新导入课表",
                value = "保留手动调整",
                onClick = onReimport,
            )
            SettingsDivider()
            SettingsNavRow(
                title = "登录凭据",
                value = "系统安全存储",
                chevron = false,
            )
        }
        SettingsSection("课表更新")
        SettingsGroup {
            SettingsSwitchRow("自动更新", app.autoUpdate, onToggle = app::updateAutoUpdate)
            SettingsDivider()
            SettingsNavRow("更新频率", value = "每天首次打开时", chevron = false)
            SettingsDivider()
            SettingsNavRow("立即更新", value = if (app.syncing) "更新中…" else "同步教务课表",
                onClick = { app.syncNow() })
            SettingsDivider()
            SettingsNavRow("上次导入", value = snapshot.importedAt.substringBefore('T'), chevron = false)
            if (app.syncStatus.isNotBlank()) {
                SettingsDivider()
                SettingsNavRow("更新状态", value = app.syncStatus, chevron = false)
            }
            if (app.timetable.conflicts.isNotEmpty()) {
                SettingsDivider()
                SettingsNavRow("处理同步冲突", value = "${app.timetable.conflicts.size} 项待处理",
                    tint = c.danger, onClick = { app.showSyncConflicts = true })
            }
        }

        SettingsSection("退出")
        SettingsGroup {
            SettingsNavRow(
                title = "退出登录并清除课表",
                tint = c.danger,
                iconTint = c.danger,
                glyph = KomaGlyph.Exit,
                onClick = { confirmLogout = true },
            )
        }
    }

    ConfirmSheet(
        open = confirmLogout,
        icon = KomaGlyph.Exit,
        title = "退出并清除本机课表？",
        message = "清除后回到欢迎页，需要重新登录并导入课表。",
        details = listOf(
            "清除：本机登录信息与已导入课表",
            "不变：学校教务系统中的数据",
        ),
        confirmLabel = "退出并清除",
        cancelLabel = "取消",
        destructive = true,
        onConfirm = {
            confirmLogout = false
            onLogout()
        },
        onDismiss = { confirmLogout = false },
    )
}
