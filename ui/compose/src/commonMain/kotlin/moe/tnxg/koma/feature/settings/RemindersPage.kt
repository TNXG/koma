package moe.tnxg.koma.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import moe.tnxg.koma.app.preferences.PreferenceStore
import moe.tnxg.koma.platform.NotificationPermission
import moe.tnxg.koma.platform.NotificationService
import moe.tnxg.koma.ui.component.list.SettingsDivider
import moe.tnxg.koma.ui.component.list.SettingsGroup
import moe.tnxg.koma.ui.component.list.SettingsNavRow
import moe.tnxg.koma.ui.component.list.SettingsSection
import moe.tnxg.koma.ui.component.list.SettingsSwitchRow
import moe.tnxg.koma.ui.component.page.SubpageScaffold
import moe.tnxg.koma.ui.component.sheet.OptionSheet
import moe.tnxg.koma.ui.component.sheet.SheetOption

@Composable
internal fun RemindersPage(
    store: PreferenceStore,
    notifications: NotificationService?,
    onBack: () -> Unit,
) {
    val prefs = store.value
    val scope = rememberCoroutineScope()
    var permission by remember(notifications) { mutableStateOf(notifications.safeStatus()) }
    var requesting by remember { mutableStateOf(false) }
    var permissionError by remember { mutableStateOf<String?>(null) }
    var leadPicker by remember { mutableStateOf(false) }

    suspend fun requestSystemPermission() {
        val service = notifications ?: return
        if (requesting) return
        requesting = true
        permissionError = null
        try {
            permission = service.requestPermission()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            permissionError = failure.message ?: "无法请求系统通知权限"
            permission = service.safeStatus()
        } finally {
            requesting = false
        }
    }

    LaunchedEffect(prefs.remindersEnabled) {
        if (prefs.remindersEnabled && notifications != null && permission == NotificationPermission.Prompt) {
            requestSystemPermission()
        }
    }

    SubpageScaffold(
        title = "课前提醒",
        onBack = onBack,
        error = store.error ?: permissionError,
        onDismissError = {
            store.clearError()
            permissionError = null
        },
    ) {
        SettingsGroup {
            SettingsSwitchRow(
                title = "课前提醒",
                checked = prefs.remindersEnabled,
                onToggle = { enabled -> store.update(prefs.copy(remindersEnabled = enabled)) },
            )
            SettingsDivider()
            SettingsNavRow(
                title = "提前时间",
                value = if (prefs.remindersEnabled) reminderLeadLabel(prefs.reminderLeadMinutes) else "已关闭",
                onClick = if (prefs.remindersEnabled) {
                    { leadPicker = true }
                } else {
                    null
                },
            )
        }

        SettingsSection("系统权限")
        SettingsGroup {
            SettingsNavRow(
                title = "通知权限",
                value = when {
                    notifications == null -> "不可用"
                    requesting -> "请求中…"
                    else -> permissionLabel(hasService = true, permission = permission)
                },
                chevron = false,
                onClick = if (notifications == null) {
                    null
                } else {
                    {
                        if (permission == NotificationPermission.Granted) {
                            permission = notifications.safeStatus()
                        } else {
                            scope.launch { requestSystemPermission() }
                        }
                    }
                },
            )
        }
    }

    OptionSheet(
        open = leadPicker,
        title = "提前时间",
        options = REMINDER_LEAD_OPTIONS.map { lead ->
            SheetOption(
                title = reminderLeadLabel(lead),
                selected = prefs.reminderLeadMinutes == lead,
                onClick = { store.update(prefs.copy(reminderLeadMinutes = lead)) },
            )
        },
        onDismiss = { leadPicker = false },
        error = store.error,
    )
}
