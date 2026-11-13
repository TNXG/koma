package moe.tnxg.koma.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Course(
    val id: String,
    val name: String,
    val teacher: String? = null,
    val location: String? = null,
    val weekday: Int,
    val startPeriod: Int,
    val endPeriod: Int,
    val weeks: List<Int>,
    val source: Map<String, String> = emptyMap(),
)

@Serializable
data class Semester(val name: String, val startDate: String, val totalWeeks: Int)

@Serializable
data class TimeSlot(val index: Int, val start: Int, val end: Int)

@Serializable
data class Term(val xnm: String, val xqm: String)

@Serializable
data class TimetableSnapshot(
    val schoolId: String,
    val term: Term,
    val semester: Semester,
    val timeGrid: List<TimeSlot>,
    val courses: List<Course>,
    val studentName: String? = null,
    val importedAt: String,
)

fun courseIdOf(name: String, teacher: String?, location: String?, weekday: Int, startPeriod: Int, endPeriod: Int): String =
    listOf(name, teacher.orEmpty(), location.orEmpty(), weekday.toString(), startPeriod.toString(), endPeriod.toString()).joinToString("|")

fun courseHeldOnWeek(course: Course, week: Int): Boolean = week in course.weeks

fun coursesForWeek(courses: List<Course>, week: Int): List<Course> = courses.filter { courseHeldOnWeek(it, week) }
    .sortedWith(compareBy(Course::weekday, Course::startPeriod))
