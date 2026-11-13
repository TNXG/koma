package moe.tnxg.koma.app.preferences

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable enum class ThemePreference { System, Light, Dark }
@Serializable enum class AccentPreference { Iris, Blue, Green, Rose, Amber }
@Serializable enum class ScheduleDensity { Compact, Standard, Relaxed }
@Serializable enum class HomeDestination { Today, Week, Me }

@Serializable
data class AppPreferences(
    val theme: ThemePreference = ThemePreference.System,
    val accent: AccentPreference = AccentPreference.Iris,
    val textScale: Float = 1f,
    val reduceMotion: Boolean = false,
    val showWeekend: Boolean = true,
    val showOtherWeekCourses: Boolean = false,
    val density: ScheduleDensity = ScheduleDensity.Standard,
    val showRoom: Boolean = true,
    val showTeacher: Boolean = true,
    val colorCourses: Boolean = true,
    val defaultHome: HomeDestination = HomeDestination.Today,
    val remindersEnabled: Boolean = false,
    val reminderLeadMinutes: Int = 10,
)

val LocalAppPreferences = staticCompositionLocalOf { AppPreferences() }

class PreferenceStore(read: (String) -> String?, private val write: (String, String?) -> Unit) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    var value by mutableStateOf(runCatching {
        read("koma.preferences")?.let { json.decodeFromString<AppPreferences>(it) }?.normalized()
    }.getOrNull() ?: AppPreferences())
        private set
    var profileName by mutableStateOf(read("koma.profileName").orEmpty())
        private set
    var avatarSeed by mutableStateOf(read("koma.avatarSeed").orEmpty())
        private set
    var avatarPhoto by mutableStateOf(read("koma.avatarPhoto").orEmpty())
        private set
    var coverPhoto by mutableStateOf(read("koma.coverPhoto").orEmpty())
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun update(next: AppPreferences): Boolean = persist {
        val accepted = next.normalized()
        write("koma.preferences", json.encodeToString(accepted))
        value = accepted
    }

    fun saveProfileName(name: String): Boolean = persist {
        val accepted = name.trim().take(20)
        write("koma.profileName", accepted.ifEmpty { null })
        profileName = accepted
    }

    /** A fresh login gets its own generated avatar; an existing seed or chosen photo is kept. */
    fun ensureAvatarSeed(): Boolean =
        if (avatarSeed.isNotBlank() || avatarPhoto.isNotBlank()) true else rerollAvatarSeed()

    fun rerollAvatarSeed(): Boolean = persist {
        val seed = newAvatarSeed()
        write("koma.avatarSeed", seed)
        avatarSeed = seed
    }

    fun saveAvatarPhoto(path: String?): Boolean = persist {
        val accepted = path?.takeIf { it.isNotBlank() }.orEmpty()
        write("koma.avatarPhoto", accepted.ifEmpty { null })
        avatarPhoto = accepted
    }

    fun saveCoverPhoto(path: String?): Boolean = persist {
        val accepted = path?.takeIf { it.isNotBlank() }.orEmpty()
        write("koma.coverPhoto", accepted.ifEmpty { null })
        coverPhoto = accepted
    }

    /** Logging out forgets the generated avatar; a photo the user picked stays theirs. */
    fun clearSessionAvatar(): Boolean = persist {
        if (avatarSeed.isNotBlank()) {
            write("koma.avatarSeed", null)
            avatarSeed = ""
        }
    }

    fun reset(): Boolean = persist {
        write("koma.preferences", null)
        value = AppPreferences()
    }

    fun clearError() { error = null }

    private inline fun persist(block: () -> Unit): Boolean = try {
        block()
        error = null
        true
    } catch (failure: Exception) {
        error = failure.message ?: "设置保存失败，请重试"
        false
    }
}

private fun AppPreferences.normalized() = copy(
    textScale = textScale.takeIf { it.isFinite() && it in .85f..1.3f } ?: 1f,
    reminderLeadMinutes = reminderLeadMinutes.takeIf { it in listOf(0, 5, 10, 15, 30) } ?: 10,
)
