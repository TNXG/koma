package moe.tnxg.koma.data.education.xjvut

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*
import moe.tnxg.koma.data.education.cas.termFromIndex
import moe.tnxg.koma.data.education.http.EduHttp
import moe.tnxg.koma.data.education.zhengfang.*
import moe.tnxg.koma.domain.model.*
import okhttp3.FormBody

internal class TimetableImporter(private val http: EduHttp) {
    private class WeekCourses(val rows: List<JsonElement>, val student: String?)
    var diagnostics: List<Diagnostic> = emptyList()
        private set

    suspend fun fetch(): TimetableSnapshot {
        val page = http.follow(Endpoints.SSO)
        if (!Endpoints.isIndex(page.url)) throw IOException("课表获取失败，请重新登录")
        val term = termFromIndex(page.text) ?: guessTerm()
        val weeks = optionalJson(Endpoints.WEEKS, term)
        val meta = zfTermWeeks(weeks) ?: throw IOException("教务周次表无效，无法确定开学日期，请稍后重新导入")
        val numbers = (weeks as JsonArray).map { week ->
            val number = (week as? JsonObject)?.get("zs")?.jsonPrimitive?.contentOrNull
                ?: throw IOException("教务周次表无效")
            if (!Regex("^\\d+$").matches(number)) throw IOException("教务周次表无效")
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
        if (parsed.courses.isEmpty()) throw IOException("接口没有返回任何课程，请重新登录后导入")
        val times = zfTimeGrid(fetched.second)
        return TimetableSnapshot("xjvut", term, Semester(termLabel(term), meta.startDate, meta.totalWeeks),
            times, parsed.courses, studentName, Clock.System.now().toString())
    }

    private suspend fun fetchWeek(term: Term, number: String): WeekCourses {
        val body = FormBody.Builder().add("xnm", term.xnm).add("xqm", term.xqm).add("zs", number)
            .add("kblx", "1").add("doType", "app").build()
        val result = http.call(Endpoints.COURSES, body, ajaxHeaders + ("Referer" to Endpoints.SSO))
        val response = parseJson(result.text) as? JsonObject
            ?: throw IOException("第 $number 周课表获取失败，请重新登录")
        val courses = response["kbList"] as? JsonArray
            ?: throw IOException("第 $number 周课表获取失败，请重新登录")
        val student = ((response["xsxx"] as? JsonObject)?.get("XM") as? JsonPrimitive)?.contentOrNull?.trim()
            ?.takeIf(String::isNotEmpty)
        val rows = courses.map { course ->
            val fields = course as? JsonObject ?: throw IOException("第 $number 周课表格式无效")
            // Actual weekly placement wins over stale semester-level zcd values.
            JsonObject(fields + ("zcd" to JsonPrimitive(number)))
        }
        return WeekCourses(rows, student)
    }

    private suspend fun optionalJson(url: String, term: Term): JsonElement? {
        return try {
            val body = FormBody.Builder().add("xnm", term.xnm).add("xqm", term.xqm).build()
            parseJson(http.call(url, body, ajaxHeaders).text)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: IOException) { null }
    }
    private fun parseJson(text: String): JsonElement? = try { Json.parseToJsonElement(text) }
        catch (_: IllegalArgumentException) { null }
    private val ajaxHeaders = mapOf("X-Requested-With" to "XMLHttpRequest", "Accept" to "application/json, text/javascript, */*; q=0.01")
    private companion object { const val WEEK_CONCURRENCY = 4 }
}
