package moe.tnxg.koma.app.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import moe.tnxg.koma.data.education.zhengfang.DEFAULT_TIME_GRID
import moe.tnxg.koma.domain.model.TimetableSnapshot
import moe.tnxg.koma.platform.PlatformServices

class TimetableSession(private val platform: PlatformServices) {
    private val json = Json { ignoreUnknownKeys = true }
    var snapshot by mutableStateOf(platform.readSetting("koma.snapshot")?.let { raw ->
        runCatching { json.decodeFromString<TimetableSnapshot>(raw) }.getOrNull()
    })
        private set
    var startDate by mutableStateOf(platform.readSetting("koma.startDate")?.takeIf {
        runCatching { LocalDate.parse(it) }.isSuccess
    }.orEmpty())
        private set

    val configured: Boolean get() = snapshot != null && startDate.isNotEmpty()

    fun accept(next: TimetableSnapshot) {
        val previous = snapshot
        val sameSchool = previous?.schoolId == next.schoolId
        val sameTerm = sameSchool && previous?.term == next.term
        val grid = next.timeGrid.ifEmpty {
            if (sameSchool) previous?.timeGrid.orEmpty().ifEmpty { DEFAULT_TIME_GRID } else DEFAULT_TIME_GRID
        }
        val accepted = next.copy(timeGrid = grid)
        platform.writeSetting("koma.snapshot", json.encodeToString(accepted))
        if (!sameTerm) {
            platform.writeSetting("koma.startDate", null)
            startDate = ""
        }
        snapshot = accepted
    }

    fun confirm(iso: String) {
        LocalDate.parse(iso)
        platform.writeSetting("koma.startDate", iso)
        startDate = iso
    }

    fun clear() {
        platform.writeSetting("koma.snapshot", null)
        platform.writeSetting("koma.startDate", null)
        snapshot = null
        startDate = ""
    }
}
