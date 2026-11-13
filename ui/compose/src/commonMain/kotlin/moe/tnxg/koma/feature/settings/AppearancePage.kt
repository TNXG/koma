package moe.tnxg.koma.feature.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import moe.tnxg.koma.app.preferences.AccentPreference
import moe.tnxg.koma.app.preferences.PreferenceStore
import moe.tnxg.koma.app.preferences.ThemePreference
import moe.tnxg.koma.ui.component.list.SettingsDivider
import moe.tnxg.koma.ui.component.list.SettingsGroup
import moe.tnxg.koma.ui.component.list.SettingsNavRow
import moe.tnxg.koma.ui.component.list.SettingsSection
import moe.tnxg.koma.ui.component.list.SettingsSwitchRow
import moe.tnxg.koma.ui.component.page.SubpageScaffold
import moe.tnxg.koma.ui.component.sheet.OptionSheet
import moe.tnxg.koma.ui.component.sheet.SheetOption

private enum class AppearancePicker { Accent, Scale }

@Composable
internal fun AppearancePage(store: PreferenceStore, onBack: () -> Unit) {
    val prefs = store.value
    var picker by remember { mutableStateOf<AppearancePicker?>(null) }
    val systemDark = isSystemInDarkTheme()
    val dark = when (prefs.theme) {
        ThemePreference.System -> systemDark
        ThemePreference.Light -> false
        ThemePreference.Dark -> true
    }

    SubpageScaffold(
        title = "主题与强调色",
        onBack = onBack,
        error = store.error,
        onDismissError = store::clearError,
    ) {
        SettingsGroup {
            ThemeModePicker(
                selected = prefs.theme,
                accent = prefs.accent,
                onSelect = { theme -> store.update(store.value.copy(theme = theme)) },
            )
        }

        SettingsSection("配色")
        SettingsGroup {
            SettingsNavRow(
                title = "强调色",
                value = prefs.accent.label(),
                onClick = { picker = AppearancePicker.Accent },
            )
            SettingsDivider()
            SettingsNavRow(
                title = "界面字号",
                value = textScaleLabel(prefs.textScale),
                onClick = { picker = AppearancePicker.Scale },
            )
        }

        SettingsSection("动效")
        SettingsGroup {
            SettingsSwitchRow(
                title = "减少动态效果",
                checked = prefs.reduceMotion,
                onToggle = { enabled -> store.update(prefs.copy(reduceMotion = enabled)) },
            )
        }
    }

    OptionSheet(
        open = picker == AppearancePicker.Accent,
        title = "强调色",
        options = AccentPreference.entries.map { accent ->
            SheetOption(
                title = accent.label(),
                selected = accent == prefs.accent,
                swatch = accentColorOf(accent, dark),
                onClick = { store.update(prefs.copy(accent = accent)) },
            )
        },
        onDismiss = { picker = null },
        error = store.error,
    )

    OptionSheet(
        open = picker == AppearancePicker.Scale,
        title = "界面字号",
        options = TEXT_SCALE_OPTIONS.map { (scale, label) ->
            SheetOption(
                title = label,
                selected = textScaleLabel(prefs.textScale) == label,
                onClick = { store.update(prefs.copy(textScale = scale)) },
            )
        },
        onDismiss = { picker = null },
        error = store.error,
    )
}
