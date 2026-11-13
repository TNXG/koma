package moe.tnxg.koma.feature.home.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import moe.tnxg.koma.app.preferences.PreferenceStore
import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.feature.settings.SettingsPage
import moe.tnxg.koma.feature.settings.SettingsScreen
import moe.tnxg.koma.platform.NotificationService

internal enum class MePage {
    Main,
    Profile,
    Semester,
    Account,
    About,
    Licenses,
    Privacy,
    Settings,
    Appearance,
    Display,
    Cards,
    Navigation,
    Reminders,
}

internal fun MePage.parent(): MePage = when (this) {
    MePage.Appearance, MePage.Display, MePage.Cards, MePage.Navigation, MePage.Reminders -> MePage.Settings
    MePage.Licenses, MePage.Privacy -> MePage.About
    else -> MePage.Main
}

@Composable
internal fun ProfileDestinationHost(
    page: MePage,
    store: PreferenceStore,
    snapshot: TimetableSnapshot,
    startDate: String,
    notifications: NotificationService?,
    onPage: (MePage) -> Unit,
    onEditStart: () -> Unit,
    onReimport: () -> Unit,
    onLogout: () -> Unit,
) {
    LaunchedEffect(store) { store.ensureAvatarSeed() }
    val back = { onPage(page.parent()) }
    val pages = rememberSaveableStateHolder()
    pages.SaveableStateProvider(page.name) {
        when (page) {
            MePage.Main -> MeScreen(
                snapshot = snapshot,
                startDate = startDate,
                store = store,
                onProfile = { onPage(MePage.Profile) },
                onSemester = { onPage(MePage.Semester) },
                onAccount = { onPage(MePage.Account) },
                onSettings = { onPage(MePage.Settings) },
                onAbout = { onPage(MePage.About) },
            )
            MePage.Profile -> ProfileScreen(store, back)
            MePage.Semester -> SemesterInfoScreen(snapshot, startDate, back, onEditStart)
            MePage.Account -> AccountScreen(snapshot, back, onReimport, onLogout)
            MePage.About -> AboutScreen(
                onBack = back,
                onLicenses = { onPage(MePage.Licenses) },
                onPrivacy = { onPage(MePage.Privacy) },
            )
            MePage.Licenses -> LicensesScreen(back)
            MePage.Privacy -> PrivacyScreen(notifications, back)
            else -> SettingsScreen(
                page = if (page == MePage.Settings) SettingsPage.Main else SettingsPage.valueOf(page.name),
                store = store,
                snapshot = snapshot,
                startDate = startDate,
                notifications = notifications,
                onPage = { onPage(if (it == SettingsPage.Main) MePage.Settings else MePage.valueOf(it.name)) },
                onBack = back,
            )
        }
    }
}
