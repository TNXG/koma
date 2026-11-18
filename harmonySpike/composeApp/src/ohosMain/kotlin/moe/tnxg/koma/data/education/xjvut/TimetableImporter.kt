package moe.tnxg.koma.data.education.xjvut

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import moe.tnxg.koma.data.education.cas.termFromIndex
import moe.tnxg.koma.data.education.http.EduBody
import moe.tnxg.koma.data.education.http.EduHttp
import moe.tnxg.koma.data.education.http.EduIoException
import moe.tnxg.koma.data.education.http.nowMillis
import moe.tnxg.koma.data.education.zhengfang.*
import moe.tnxg.koma.domain.model.*

internal class TimetableImporter(private val http: EduHttp) {
    private class WeekCourses(val rows: List<JsonElement>, val student: String?)
    var diagnostics: List<Diagnostic> = emptyList()
        private set

    suspend fun fetch(): TimetableSnapshot {
        val page = http.follow(Endpoints.SSO)
        if (!Endpoints.isIndex(page.url)) throw EduIoException("课表获取失败，请重新登录")
        val term = termFromIndex(page.text) ?: guessTerm()
        val weeks = optionalJson(Endpoints.WEEKS, term)
        val meta = zfTermWeeks(weeks) ?: throw EduIoException("教务周次表无效，无法确定开学日期，请稍后重新导入")
        val numbers = (weeks as JsonArray).map { week ->
            val number = (week as? JsonObject)?.get("zs")?.jsonPrimitive?.contentOrNull
                ?: throw EduIoException("教务周次表无效")
            if (!Regex("^\\d+$").matches(number)) throw EduIoException("教务周次表无效")
            number
        }
        // Weeks and the optional time grid are independent; fetch them at the same time.
        val fetched = coroutineScope {
            val gate = Semaphore(WEEK_CONCURRENCY)
            val times = async { optionalJson(Endpoints.TIMES, term) }
            val perWeek = numbers.map { number ->
                async { gate.withPermit { fetchWeek(term, number) } }
            }.awaitAll()
            perWeek to times.await()
        }
        val rows = fetched.first.flatMap { it.rows }
        val studentName = fetched.first.firstNotNullOfOrNull { it.student }
        val parsed = parseZfWeekly(JsonArray(rows))
        diagnostics = parsed.diagnostics
        if (parsed.courses.isEmpty()) throw EduIoException("接口没有返回任何课程，请重新登录后导入")
        val times = zfTimeGrid(fetched.second)
        return TimetableSnapshot("xjvut", term, Semester(termLabel(term), meta.startDate, meta.totalWeeks),
            times, parsed.courses, studentName, Clock.System.now().toString())
    }

    private suspend fun fetchWeek(term: Term, number: String): WeekCourses {
        val body = EduBody.form(
            "xnm" to term.xnm, "xqm" to term.xqm, "zs" to number,
            "kblx" to "1", "doType" to "app",
        )
        val result = http.call(Endpoints.COURSES, body, ajaxHeaders + ("Referer" to Endpoints.SSO))
        val response = parseJson(result.text) as? JsonObject
            ?: throw EduIoException("第 $number 周课表获取失败，请重新登录")
        val courses = response["kbList"] as? JsonArray
            ?: throw EduIoException("第 $number 周课表获取失败，请重新登录")
        val student = ((response["xsxx"] as? JsonObject)?.get("XM") as? JsonPrimitive)?.contentOrNull?.trim()
            ?.takeIf(String::isNotEmpty)
        val rows = courses.map { course ->
            val fields = course as? JsonObject ?: throw EduIoException("第 $number 周课表格式无效")
            // Actual weekly placement wins over stale semester-level zcd values.
            JsonObject(fields + ("zcd" to JsonPrimitive(number)))
        }
        return WeekCourses(rows, student)
    }

    private suspend fun optionalJson(url: String, term: Term): JsonElement? {
        return try {
            val body = EduBody.form("xnm" to term.xnm, "xqm" to term.xqm)
            parseJson(http.call(url, body, ajaxHeaders).text)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: EduIoException) { null }
    }
    private fun parseJson(text: String): JsonElement? = try { Json.parseToJsonElement(text) }
        catch (_: IllegalArgumentException) { null }
    private val ajaxHeaders = mapOf("X-Requested-With" to "XMLHttpRequest", "Accept" to "application/json, text/javascript, */*; q=0.01")
    private companion object { const val WEEK_CONCURRENCY = 4 }
}
