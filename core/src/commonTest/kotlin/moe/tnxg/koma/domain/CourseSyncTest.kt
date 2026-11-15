package moe.tnxg.koma.domain

import kotlinx.serialization.json.Json
import moe.tnxg.koma.domain.course.*
import moe.tnxg.koma.domain.model.*
import kotlin.test.*

class CourseSyncTest {
    @Test fun unchangedImportsPreserveEditsWithoutConflictsAndAdvanceMetadata() {
        val edited = updateCourseNote(synchronizeCourses(null, fixture()), "a", "手动备注")
        val refreshed = synchronizeCourses(edited, fixture().copy(importedAt = "second"))
        assertEquals("手动备注", refreshed.snapshot.courses.single().note)
        assertEquals("second", refreshed.snapshot.importedAt)
        assertTrue(refreshed.conflicts.isEmpty())
        val metadataOnly = fixture().courses.single().copy(source = mapOf("raw" to "changed"))
        assertTrue(synchronizeCourses(refreshed, fixture(listOf(metadataOnly))).conflicts.isEmpty())
    }

    @Test fun changedRemoteCreatesPersistentConflictAndLocalChoiceDoesNotReappear() {
        val local = updateCourseNote(synchronizeCourses(null, fixture()), "a", "保留")
        val remote = fixture(listOf(fixture().courses.single().copy(location = "教务教室")))
        val conflicted = synchronizeCourses(local, remote)
        assertEquals("旧教室", conflicted.snapshot.courses.single().location)
        assertEquals(CourseSyncConflictKind.RemoteChanged, conflicted.conflicts.single().kind)
        val reloaded = Json.decodeFromString<CourseSyncState>(Json.encodeToString(conflicted))
        assertEquals(1, synchronizeCourses(reloaded, remote).conflicts.size)
        val kept = resolveCourseConflict(reloaded, reloaded.conflicts.single().id, false)
        assertTrue(synchronizeCourses(kept, remote).conflicts.isEmpty())
        assertEquals("保留", synchronizeCourses(kept, remote).snapshot.courses.single().note)
        val changedAgain = remote.copy(courses = remote.courses.map { it.copy(weekday = 2) })
        assertEquals(1, synchronizeCourses(kept, changedAgain).conflicts.size)
    }

    @Test fun remoteDeletionRequiresChoiceAndRemoteChoiceAlsoDeletesReschedule() {
        val local = rescheduleCourse(synchronizeCourses(null, fixture()), "a", 1, 3, 2, 1, 2, "新")
        val deleted = synchronizeCourses(local, fixture(emptyList()))
        assertEquals(2, deleted.snapshot.courses.size)
        assertEquals(CourseSyncConflictKind.RemoteDeleted, deleted.conflicts.single().kind)
        val accepted = resolveCourseConflict(deleted, deleted.conflicts.single().id, true)
        assertTrue(accepted.snapshot.courses.isEmpty())
        assertTrue(synchronizeCourses(accepted, fixture(emptyList())).conflicts.isEmpty())
    }

    @Test fun remoteChoiceReplacesScheduleButKeepsLocalNotesAndRemovesDerivedCourses() {
        val local = rescheduleCourse(updateCourseNote(synchronizeCourses(null, fixture()), "a", "手动"),
            "a", 1, 3, 2, 1, 2, null)
        val remote = fixture(listOf(fixture().courses.single().copy(weekday = 5)))
        val conflict = synchronizeCourses(local, remote)
        val accepted = resolveCourseConflict(conflict, conflict.conflicts.single().id, true)
        assertEquals(listOf(5), accepted.snapshot.courses.map { it.weekday })
        // Notes are local-only data and must survive adopting the school schedule.
        assertEquals("手动", accepted.snapshot.courses.single().note)
        assertTrue(synchronizeCourses(accepted, remote).conflicts.isEmpty())
    }

    @Test fun differentIdOverlapCannotSilentlyReplaceLocalEdit() {
        val local = updateCourseNote(synchronizeCourses(null, fixture()), "a", "手动")
        val other = fixture().courses.single().copy(id = "b", name = "英语")
        val remote = fixture(listOf(fixture().courses.single(), other))
        val conflict = synchronizeCourses(local, remote)
        assertEquals(listOf("a"), conflict.snapshot.courses.map { it.id })
        assertEquals(CourseSyncConflictKind.Overlap, conflict.conflicts.single().kind)
        assertEquals(listOf("a"), conflict.conflicts.single().relatedCourseIds)
        val kept = resolveCourseConflict(conflict, conflict.conflicts.single().id, false)
        assertEquals(listOf("a"), synchronizeCourses(kept, remote).snapshot.courses.map { it.id })
        assertTrue(synchronizeCourses(kept, remote).conflicts.isEmpty())
        val accepted = resolveCourseConflict(conflict, conflict.conflicts.single().id, true)
        assertEquals(listOf("b"), coursesForWeek(accepted.snapshot.courses, 1).map { it.id })
        assertEquals("手动", accepted.snapshot.courses.first { it.id == "a" }.note)
    }

    @Test fun remoteNewIdentityCannotBypassManualSuspension() {
        val original = fixture().courses.single()
        val local = setCourseSuspended(synchronizeCourses(null, fixture()), "a", 1, true)
        val remote = fixture(listOf(original, original.copy(id = "new", weeks = listOf(1))))
        val conflicted = synchronizeCourses(local, remote)
        assertTrue(coursesForWeek(conflicted.snapshot.courses, 1).isEmpty())
        assertEquals(CourseSyncConflictKind.Overlap, conflicted.conflicts.single().kind)
        val accepted = resolveCourseConflict(conflicted, conflicted.conflicts.single().id, true)
        assertEquals(listOf("new"), coursesForWeek(accepted.snapshot.courses, 1).map { it.id })
        assertEquals(listOf("a"), coursesForWeek(accepted.snapshot.courses, 2).map { it.id })
    }

    @Test fun ordinaryRemoteChangesDoNotRequireManualResolution() {
        val initial = synchronizeCourses(null, fixture())
        val remote = fixture(listOf(fixture().courses.single().copy(location = "新")))
        val updated = synchronizeCourses(initial, remote)
        assertEquals("新", updated.snapshot.courses.single().location)
        assertTrue(updated.conflicts.isEmpty())
        assertTrue(synchronizeCourses(updated, fixture(emptyList())).snapshot.courses.isEmpty())
        val otherTerm = fixture().copy(term = Term("2027", "1"))
        assertEquals("", synchronizeCourses(updateCourseNote(initial, "a", "旧"), otherTerm).snapshot.courses.single().note)
    }

    @Test fun remoteChangeOfEditedCourseAlsoDetectsOverlapWithAnotherEditedCourse() {
        val a = fixture().courses.single()
        val b = a.copy(id = "b", weekday = 2)
        val initial = synchronizeCourses(null, fixture(listOf(a, b)))
        val local = updateCourseNote(updateCourseNote(initial, "a", "A备注"), "b", "B备注")
        val conflicted = synchronizeCourses(local, fixture(listOf(a.copy(weekday = 2), b)))
        assertEquals(CourseSyncConflictKind.Overlap, conflicted.conflicts.single().kind)
        assertEquals(listOf("b"), conflicted.conflicts.single().relatedCourseIds)
        val accepted = resolveCourseConflict(conflicted, conflicted.conflicts.single().id, true)
        assertEquals(listOf("a"), coursesForWeek(accepted.snapshot.courses, 1).map { it.id })
        assertEquals("B备注", accepted.snapshot.courses.first { it.id == "b" }.note)
    }

    @Test fun editsWhileConflictIsPendingRefreshTheAffectedLocalOccurrences() {
        val a = fixture().courses.single()
        val c = a.copy(id = "c", weekday = 3, weeks = listOf(1))
        val initial = updateCourseNote(synchronizeCourses(null, fixture(listOf(a, c))), "a", "手动")
        val pending = synchronizeCourses(initial, fixture(listOf(a, c, a.copy(id = "b", weeks = listOf(1)))))
        val movedA = rescheduleCourse(pending, "a", 1, 2, 2, 1, 2, null)
        val movedC = rescheduleCourse(movedA, "c", 1, 1, 1, 1, 2, null)
        val replacement = movedC.snapshot.courses.single { it.rescheduledFromCourseId == "c" }
        assertTrue(replacement.id in movedC.conflicts.single().relatedCourseIds)
        val accepted = resolveCourseConflict(movedC, movedC.conflicts.single().id, true)
        assertEquals(listOf("b"), coursesForWeek(accepted.snapshot.courses, 1).map { it.id })
    }
}
