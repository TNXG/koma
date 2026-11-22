package moe.tnxg.koma.platform

import kotlinx.serialization.json.Json
import moe.tnxg.koma.domain.model.TimetableSnapshot

object TimetableSettings {
    const val SNAPSHOT = "koma.snapshot"
    const val START_DATE = "koma.startDate"

    private val json = Json { ignoreUnknownKeys = true }

    fun decodeSnapshot(raw: String?): TimetableSnapshot? = raw?.let {
        try {
            json.decodeFromString<TimetableSnapshot>(it)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
