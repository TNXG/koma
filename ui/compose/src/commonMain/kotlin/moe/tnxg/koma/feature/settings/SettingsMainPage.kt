package moe.tnxg.koma.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import moe.tnxg.koma.app.preferences.PreferenceStore
import moe.tnxg.koma.platform.NotificationPermission
import moe.tnxg.koma.platform.NotificationService
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.list.SettingsDivider
import moe.tnxg.koma.ui.component.list.SettingsGroup
import moe.tnxg.koma.ui.component.list.SettingsNavRow
import moe.tnxg.koma.ui.component.list.SettingsSection
import moe.tnxg.koma.ui.component.page.SubpageScaffold
import moe.tnxg.koma.ui.component.sheet.ConfirmSheet
import moe.tnxg.koma.ui.theme.KomaTheme

@Composable
internal fun SettingsMainPage(
    store: PreferenceStore,
    notifications: NotificationService?,
    onPage: (SettingsPage) -> Unit,
    onBack: () -> Unit,
) {
    val c = KomaTheme.colors
    val prefs = store.value
    val permission = remember(notifications) { notifications.safeStatus() }
    var confirmReset by remember { mutableStateOf(false) }

    val reminderValue = when {
        notifications == null -> "不可用"
        !prefs.remindersEnabled -> "已关闭"
        permission == NotificationPermission.Granted -> reminderLeadLabel(prefs.reminderLeadMinutes)
        else -> "未授权"
    }

    SubpageScaffold(
        title = "设置",
        onBack = onBack,
        error = store.error,
        onDismissError = store::clearError,
    ) {
        SettingsSection("课表", first = true)
        SettingsGroup {
            SettingsNavRow(
                title = "课表显示",
                value = prefs.density.label(),
                onClick = { onPage(SettingsPage.Display) },
            )
            SettingsDivider()
            SettingsNavRow(
                title = "课程卡片",
                value = if (prefs.colorCourses) "彩色标识" else "中性卡片",
                onClick = { onPage(SettingsPage.Cards) },
            )
        }

        SettingsSection("外观")
        SettingsGroup {
            SettingsNavRow(
                title = "主题与强调色",
                value = prefs.theme.label(),
                onClick = { onPage(SettingsPage.Appearance) },
            )
            SettingsDivider()
            SettingsNavRow(
                title = "首页与导航",
                value = prefs.defaultHome.label(),
                onClick = { onPage(SettingsPage.Navigation) },
            )
        }

        SettingsSection("提醒")
        SettingsGroup {
            SettingsNavRow(
                title = "课前提醒",
                value = reminderValue,
                onClick = { onPage(SettingsPage.Reminders) },
            )
        }

        SettingsSection("重置")
        SettingsGroup {
            SettingsNavRow(
                title = "恢复默认设置",
                tint = c.danger,
                onClick = {
                    store.clearError()
                    confirmReset = true
                },
            )
        }
    }

    ConfirmSheet(
        open = confirmReset,
        icon = KomaGlyph.Refresh,
        title = "恢复默认设置？",
        message = "只还原应用偏好，账号与课表不受影响。",
        details = listOf(
            "重置：主题、字号、显示、卡片与提醒",
            "保留：账号、课表、开学日期、头像与封面",
        ),
        confirmLabel = "恢复默认",
        cancelLabel = "取消",
        destructive = true,
        error = store.error,
        onConfirm = { if (store.reset()) confirmReset = false },
        onDismiss = {
            store.clearError()
            confirmReset = false
        },
    )
}
