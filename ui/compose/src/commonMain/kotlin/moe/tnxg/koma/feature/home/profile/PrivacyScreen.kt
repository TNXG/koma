package moe.tnxg.koma.feature.home.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import moe.tnxg.koma.feature.settings.permissionLabel
import moe.tnxg.koma.feature.settings.safeStatus
import moe.tnxg.koma.platform.NotificationService
import moe.tnxg.koma.ui.component.list.SettingsDivider
import moe.tnxg.koma.ui.component.list.SettingsGroup
import moe.tnxg.koma.ui.component.list.SettingsNavRow
import moe.tnxg.koma.ui.component.list.SettingsSection
import moe.tnxg.koma.ui.component.page.SubpageScaffold

@Composable
fun PrivacyScreen(notifications: NotificationService?, onBack: () -> Unit) {
    val permission = remember(notifications) { notifications.safeStatus() }

    SubpageScaffold(title = "隐私与权限", onBack = onBack) {
        SettingsSection("网络请求", first = true)
        SettingsGroup {
            SettingsNavRow(title = "随机封面", value = "api-space.tnxg.top", chevron = false)
            SettingsDivider()
            SettingsNavRow(title = "教务系统", value = "xjvut.edu.cn", chevron = false)
        }

        SettingsSection("本机数据")
        SettingsGroup {
            SettingsNavRow(title = "登录凭据", value = "系统 Keystore", chevron = false)
            SettingsDivider()
            SettingsNavRow(title = "课表与偏好", value = "仅本机", chevron = false)
        }

        SettingsSection("系统权限")
        SettingsGroup {
            SettingsNavRow(title = "网络访问", value = "必需", chevron = false)
            SettingsDivider()
            SettingsNavRow(
                title = "通知",
                value = permissionLabel(notifications != null, permission),
                chevron = false,
            )
        }
    }
}
