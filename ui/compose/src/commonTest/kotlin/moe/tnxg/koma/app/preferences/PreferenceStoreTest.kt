package moe.tnxg.koma.app.preferences

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PreferenceStoreTest {
    @Test fun savedDisplayChoicesSurviveRestart() {
        val data = mutableMapOf<String, String>()
        fun store() = PreferenceStore(data::get) { key, value ->
            if (value == null) data.remove(key) else data[key] = value
        }
        val first = store()
        assertTrue(first.update(first.value.copy(theme = ThemePreference.Dark, showWeekend = false,
            showTeacher = false, density = ScheduleDensity.Compact, defaultHome = HomeDestination.Me)))
        val restored = store().value
        assertEquals(ThemePreference.Dark, restored.theme)
        assertFalse(restored.showWeekend)
        assertFalse(restored.showTeacher)
        assertEquals(ScheduleDensity.Compact, restored.density)
        assertEquals(HomeDestination.Me, restored.defaultHome)
    }

    @Test fun resetPreservesProfileAndImportedData() {
        val data = mutableMapOf("koma.snapshot" to "imported", "koma.startDate" to "2026-08-31")
        val store = PreferenceStore(data::get) { key, value ->
            if (value == null) data.remove(key) else data[key] = value
        }
        store.saveProfileName("我的课表")
        store.update(store.value.copy(theme = ThemePreference.Dark, remindersEnabled = true))
        assertTrue(store.reset())
        assertEquals(ThemePreference.System, store.value.theme)
        assertEquals("我的课表", store.profileName)
        assertEquals("imported", data["koma.snapshot"])
        assertEquals("2026-08-31", data["koma.startDate"])
    }

    @Test fun avatarSeedAndPickedPhotosSurviveRestartAndReset() {
        val data = mutableMapOf<String, String>()
        fun store() = PreferenceStore(data::get) { key, value ->
            if (value == null) data.remove(key) else data[key] = value
        }
        val first = store()
        assertTrue(first.ensureAvatarSeed())
        val seed = first.avatarSeed
        assertEquals(32, seed.length)
        assertTrue(seed.all { it in "0123456789abcdef" })
        first.ensureAvatarSeed()
        assertEquals(seed, first.avatarSeed, "an existing seed is never replaced on its own")
        assertTrue(first.saveAvatarPhoto("/files/photos/avatar.jpg"))
        assertTrue(first.saveCoverPhoto("/files/photos/cover.jpg"))
        assertTrue(first.reset())

        val restored = store()
        assertEquals(seed, restored.avatarSeed)
        assertEquals("/files/photos/avatar.jpg", restored.avatarPhoto)
        assertEquals("/files/photos/cover.jpg", restored.coverPhoto)

        assertTrue(restored.clearSessionAvatar())
        assertEquals("", restored.avatarSeed)
        assertEquals("/files/photos/avatar.jpg", restored.avatarPhoto)
    }

    @Test fun failedWriteNeverPublishesUnsavedChoices() {
        val store = PreferenceStore({ null }) { _, _ -> error("disk full") }
        assertFalse(store.update(store.value.copy(showWeekend = false)))
        assertTrue(store.value.showWeekend)
        assertEquals("disk full", store.error)
        assertFalse(store.saveProfileName("new name"))
        assertEquals("", store.profileName)
    }

    @Test fun invalidStoredValuesDoNotBreakStartup() {
        val data = mapOf("koma.preferences" to "{\"textScale\":-2,\"reminderLeadMinutes\":999,\"theme\":\"FutureTheme\"}")
        val store = PreferenceStore(data::get) { _, _ -> }
        assertEquals(ThemePreference.System, store.value.theme)
        assertEquals(1f, store.value.textScale)
        assertEquals(10, store.value.reminderLeadMinutes)
    }
}
