package moe.tnxg.koma.feature.home.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import moe.tnxg.koma.app.preferences.PreferenceStore
import moe.tnxg.koma.platform.PhotoTarget
import moe.tnxg.koma.ui.component.list.SettingsDivider
import moe.tnxg.koma.ui.component.list.SettingsGroup
import moe.tnxg.koma.ui.component.list.SettingsNavRow
import moe.tnxg.koma.ui.component.list.SettingsSection
import moe.tnxg.koma.ui.component.media.LocalImageServices
import moe.tnxg.koma.ui.component.media.ProfileAvatar
import moe.tnxg.koma.ui.component.media.ProfileCover
import moe.tnxg.koma.ui.component.page.SubpageScaffold
import moe.tnxg.koma.ui.component.sheet.OptionSheet
import moe.tnxg.koma.ui.component.sheet.SheetOption
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme

@Composable
fun ProfileScreen(store: PreferenceStore, onBack: () -> Unit) {
    val c = KomaTheme.colors
    val services = LocalImageServices.current
    val scope = rememberCoroutineScope()
    var draftName by remember(store.profileName) { mutableStateOf(store.profileName) }
    var picker by remember { mutableStateOf<PhotoTarget?>(null) }
    var coverToken by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var pickError by remember { mutableStateOf<String?>(null) }
    val hasCustomAvatar = store.avatarPhoto.isNotBlank()
    val hasCustomCover = store.coverPhoto.isNotBlank()

    fun pickPhoto(target: PhotoTarget) {
        val imageServices = services ?: return
        if (busy) return
        scope.launch {
            busy = true
            pickError = null
            try {
                val path = imageServices.pickPhoto(target)
                if (path != null) {
                    when (target) {
                        PhotoTarget.Avatar -> store.saveAvatarPhoto(path)
                        PhotoTarget.Cover -> store.saveCoverPhoto(path)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                pickError = failure.message ?: "无法读取所选照片"
            }
            busy = false
        }
    }

    fun restoreGeneratedAvatar() {
        services?.deletePhoto(PhotoTarget.Avatar)
        store.saveAvatarPhoto(null)
        store.rerollAvatarSeed()
    }

    fun restoreRandomCover() {
        services?.deletePhoto(PhotoTarget.Cover)
        store.saveCoverPhoto(null)
        coverToken++
    }

    SubpageScaffold(
        title = "个人资料",
        onBack = {
            store.clearError()
            onBack()
        },
        error = store.error ?: pickError,
        onDismissError = {
            store.clearError()
            pickError = null
        },
        topRight = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(c.accent)
                    .semantics { role = Role.Button }
                    .clickable {
                        if (store.saveProfileName(draftName.trim())) onBack()
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                KomaText("保存", size = 12, weight = FontWeight.SemiBold, color = Color.White)
            }
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(c.surface2),
        ) {
            ProfileCover(
                photoPath = store.coverPhoto,
                modifier = Modifier.fillMaxSize(),
                refreshToken = coverToken,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.45f),
                        ),
                    ),
            )
            ProfileAvatar(
                seed = store.avatarSeed,
                photoPath = store.avatarPhoto,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 14.dp)
                    .size(66.dp)
                    .border(3.dp, Color.White, CircleShape),
            )
        }

        SettingsSection("展示", first = false)
        SettingsGroup {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KomaText("称呼", size = 14, weight = FontWeight.SemiBold, color = c.ink, letterSpacing = -0.25f)
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    if (draftName.isEmpty()) {
                        KomaText("同学", size = 12.5f, weight = FontWeight.Medium, color = c.ink3)
                    }
                    BasicTextField(
                        value = draftName,
                        onValueChange = {
                            draftName = it
                            if (store.error != null) store.clearError()
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = c.ink3,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.End,
                        ),
                        cursorBrush = SolidColor(c.accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        SettingsSection("头像与封面")
        SettingsGroup {
            SettingsNavRow(
                title = "头像",
                value = if (hasCustomAvatar) "已自定义" else "自动生成",
                onClick = { picker = PhotoTarget.Avatar },
            )
            SettingsDivider()
            SettingsNavRow(
                title = "封面",
                value = if (hasCustomCover) "已自定义" else "三天更新",
                onClick = { picker = PhotoTarget.Cover },
            )
        }
    }

    if (picker == PhotoTarget.Avatar) {
        val options = buildList {
            add(SheetOption(title = "从相册选择", onClick = { pickPhoto(PhotoTarget.Avatar) }))
            if (hasCustomAvatar) {
                add(SheetOption(title = "恢复自动生成", onClick = ::restoreGeneratedAvatar))
            } else {
                add(SheetOption(title = "换一个自动头像", onClick = { store.rerollAvatarSeed() }))
            }
        }
        OptionSheet(
            open = true,
            title = "头像",
            options = options,
            onDismiss = { picker = null },
            error = store.error ?: pickError,
        )
    }

    if (picker == PhotoTarget.Cover) {
        val options = buildList {
            add(SheetOption(title = "从相册选择", onClick = { pickPhoto(PhotoTarget.Cover) }))
            if (hasCustomCover) {
                add(SheetOption(title = "恢复随机封面", onClick = ::restoreRandomCover))
            } else {
                add(SheetOption(title = "刷新随机封面", onClick = { coverToken++ }))
            }
        }
        OptionSheet(
            open = true,
            title = "封面",
            options = options,
            onDismiss = { picker = null },
            error = store.error ?: pickError,
        )
    }
}
