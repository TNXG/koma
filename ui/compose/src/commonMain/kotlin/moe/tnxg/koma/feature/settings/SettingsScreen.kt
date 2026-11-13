package moe.tnxg.koma.feature.settings

import androidx.compose.runtime.Composable
import moe.tnxg.koma.app.preferences.PreferenceStore
import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.platform.NotificationService

enum class SettingsPage {
    Main,
    Appearance,
    Display,
    Cards,
    Navigation,
    Reminders,
}

@Composable
fun SettingsScreen(
    page: SettingsPage,
    store: PreferenceStore,
    snapshot: TimetableSnapshot,
    startDate: String,
    notifications: NotificationService?,
    onPage: (SettingsPage) -> Unit,
    onBack: () -> Unit,
) {
    val handleBack = {
        store.clearError()
        onBack()
    }
    when (page) {
        SettingsPage.Main -> SettingsMainPage(
            store = store,
            notifications = notifications,
            onPage = {
                store.clearError()
                onPage(it)
            },
            onBack = handleBack,
        )
        SettingsPage.Appearance -> AppearancePage(store = store, onBack = handleBack)
        SettingsPage.Display -> DisplayPage(
            store = store,
            snapshot = snapshot,
            startDate = startDate,
            onBack = handleBack,
        )
        SettingsPage.Cards -> CardsPage(
            store = store,
            snapshot = snapshot,
            onBack = handleBack,
        )
        SettingsPage.Navigation -> NavigationPage(store = store, onBack = handleBack)
        SettingsPage.Reminders -> RemindersPage(
            store = store,
            notifications = notifications,
            onBack = handleBack,
        )
    }
}
