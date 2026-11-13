package moe.tnxg.koma.app.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import moe.tnxg.koma.data.education.EduService
import moe.tnxg.koma.data.education.LoginPreparation
import moe.tnxg.koma.data.education.LoginResult
import moe.tnxg.koma.domain.model.Semester
import moe.tnxg.koma.domain.model.Term
import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.platform.PlatformServices
import moe.tnxg.koma.platform.SavedCredentials

class TimetableSessionTest {
    @Test
    fun reimportPreservesConfirmedDateOnlyForSameSchoolAndTerm() {
        val platform = MemorySettings()
        val session = TimetableSession(platform)
        session.accept(snapshot())
        session.confirm("2026-08-31")
        session.accept(snapshot())
        assertEquals("2026-08-31", session.startDate)
        session.accept(snapshot(school = "other"))
        assertEquals("", session.startDate)
    }

    @Test
    fun persistedSnapshotRestoresAndLogoutRemovesBothValues() {
        val platform = MemorySettings()
        val session = TimetableSession(platform)
        session.accept(snapshot())
        session.confirm("2026-08-31")
        val restored = TimetableSession(platform)
        assertEquals("2026-08-31", restored.startDate)
        assertEquals("xjvut", restored.snapshot?.schoolId)
        restored.clear()
        assertNull(platform.readSetting("koma.snapshot"))
        assertNull(platform.readSetting("koma.startDate"))
    }

    private fun snapshot(school: String = "xjvut") = TimetableSnapshot(
        school, Term("2026", "3"), Semester("2026 学年", "2026-09-07", 20),
        emptyList(), emptyList(), importedAt = "2026-11-13T00:00:00Z",
    )
}

private class MemorySettings : PlatformServices {
    private val values = mutableMapOf<String, String>()
    override val edu: EduService get() = error("No education calls in persistence test")
    override val canRememberCredentials = false
    override val notifications = null
    override fun readSetting(key: String) = values[key]
    override fun writeSetting(key: String, value: String?) { if (value == null) values.remove(key) else values[key] = value }
    override suspend fun loadCredentials(): SavedCredentials? = null
    override suspend fun saveCredentials(value: SavedCredentials?) = error("Not used")
    override suspend fun recognizeCaptcha(dataUrl: String, view: Int): String? = null
}
