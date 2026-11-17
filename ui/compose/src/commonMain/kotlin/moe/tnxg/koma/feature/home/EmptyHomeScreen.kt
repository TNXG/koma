package moe.tnxg.koma.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.tnxg.koma.app.preferences.HomeDestination
import moe.tnxg.koma.app.preferences.PreferenceStore
import moe.tnxg.koma.feature.home.dock.HomeDock
import moe.tnxg.koma.feature.home.profile.MePage
import moe.tnxg.koma.feature.home.profile.ProfileDestinationHost
import moe.tnxg.koma.feature.home.profile.parent
import moe.tnxg.koma.platform.NotificationService
import moe.tnxg.koma.ui.component.button.KomaButton
import moe.tnxg.koma.ui.component.button.KomaTextButton
import moe.tnxg.koma.ui.component.page.LocalPageActive
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.platform.PlatformBackHandler
import moe.tnxg.koma.ui.theme.KomaTheme
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/** An unconfigured timetable is absent, not a fabricated semester or demo schedule. */
@Composable
fun EmptyHomeScreen(
    store: PreferenceStore,
    notifications: NotificationService?,
    onLogin: () -> Unit,
    onJsonImport: () -> Unit,
    onLogout: () -> Unit,
) {
    val colors = KomaTheme.colors
    var selected by rememberSaveable { mutableIntStateOf(when (store.value.defaultHome) {
        HomeDestination.Today -> 0
        HomeDestination.Week -> 1
        HomeDestination.Me -> 2
    }) }
    var profilePage by rememberSaveable { mutableStateOf(MePage.Main) }
    val backdrop = rememberLayerBackdrop { drawRect(colors.bg); drawContent() }
    PlatformBackHandler(selected != 0) {
        if (selected == 2 && profilePage != MePage.Main) profilePage = profilePage.parent()
        else selected = 0
    }
    Box(Modifier.fillMaxSize().background(colors.bg)) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            if (selected == 2) {
                CompositionLocalProvider(LocalPageActive provides true) {
                    ProfileDestinationHost(profilePage, store, null, "", notifications,
                        { profilePage = it }, onLogin, onLogin, onLogout)
                }
            } else {
                Column(Modifier.fillMaxSize().statusBarsPadding()
                    .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 110.dp)) {
                    KomaText(if (selected == 0) "今天" else "课表", size = 29, weight = FontWeight.ExtraBold)
                    Column(Modifier.weight(1f).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
                        KomaText("还没有课表", size = 22, weight = FontWeight.Bold)
                        KomaText("无需登录也能使用。导入课表后，这里会显示你的课程。",
                            size = 13, color = colors.ink3)
                        KomaButton("导入课表", onJsonImport)
                        KomaTextButton("登录学校教务导入", onLogin, Modifier.padding(12.dp))
                    }
                }
            }
        }
        if (selected != 2 || profilePage == MePage.Main) {
            HomeDock(selected, { selected = 0 }, { selected = 1 },
                { selected = 2; profilePage = MePage.Main }, Modifier.align(Alignment.BottomCenter), backdrop)
        }
    }
}
