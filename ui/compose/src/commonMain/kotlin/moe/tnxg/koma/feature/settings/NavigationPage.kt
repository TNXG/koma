package moe.tnxg.koma.feature.settings

import androidx.compose.runtime.Composable
import moe.tnxg.koma.app.preferences.HomeDestination
import moe.tnxg.koma.app.preferences.PreferenceStore
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.list.SettingsDivider
import moe.tnxg.koma.ui.component.list.SettingsGroup
import moe.tnxg.koma.ui.component.list.SettingsRadioRow
import moe.tnxg.koma.ui.component.list.SettingsSection
import moe.tnxg.koma.ui.component.page.SubpageScaffold

@Composable
internal fun NavigationPage(
    store: PreferenceStore,
    onBack: () -> Unit,
) {
    val prefs = store.value
    SubpageScaffold(
        title = "首页与导航",
        onBack = onBack,
        error = store.error,
        onDismissError = store::clearError,
    ) {
        SettingsSection("启动后打开", first = true)
        SettingsGroup {
            HomeDestination.entries.forEachIndexed { index, destination ->
                if (index > 0) SettingsDivider()
                SettingsRadioRow(
                    title = destination.label(),
                    selected = prefs.defaultHome == destination,
                    glyph = destination.glyph(),
                    onClick = { store.update(prefs.copy(defaultHome = destination)) },
                )
            }
        }
    }
}

private fun HomeDestination.glyph(): KomaGlyph = when (this) {
    HomeDestination.Today -> KomaGlyph.Home
    HomeDestination.Week -> KomaGlyph.Calendar
    HomeDestination.Me -> KomaGlyph.Person
}
