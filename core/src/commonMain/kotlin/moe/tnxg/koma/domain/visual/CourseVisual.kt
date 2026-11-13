package moe.tnxg.koma.domain.visual

enum class CourseTone { Blue, Amber, Rose, Green, Violet, Orange, Teal }
enum class CourseIcon { Translate, Formula, Fitness, Code, Flask, Bank, Palette, Briefcase, Chip, Heart, Book }
data class CourseVisual(val icon: CourseIcon, val tone: CourseTone)
private data class Rule(val words: List<String>, val icon: CourseIcon, val tone: CourseTone)
private val rules = listOf(
    Rule(listOf("英语", "english", "外语", "日语", "俄语"), CourseIcon.Translate, CourseTone.Blue),
    Rule(listOf("数学", "高数", "代数", "统计", "概率", "微积分"), CourseIcon.Formula, CourseTone.Amber),
    Rule(listOf("体育", "健康", "运动"), CourseIcon.Fitness, CourseTone.Rose),
    Rule(listOf("程序", "编程", "计算机", "软件", "数据", "网络", "python", "java", "web", "前端"), CourseIcon.Code, CourseTone.Teal),
    Rule(listOf("物理", "化学", "生物", "实验"), CourseIcon.Flask, CourseTone.Green),
    Rule(listOf("思想", "政治", "马克思", "毛泽东", "形势", "历史", "法治", "习近平"), CourseIcon.Bank, CourseTone.Orange),
    Rule(listOf("设计", "美术", "艺术", "音乐", "绘画"), CourseIcon.Palette, CourseTone.Violet),
    Rule(listOf("就业", "创业", "职业", "管理", "经济"), CourseIcon.Briefcase, CourseTone.Orange),
    Rule(listOf("电子", "电路", "机械", "自动化", "嵌入式", "单片机"), CourseIcon.Chip, CourseTone.Teal),
    Rule(listOf("心理"), CourseIcon.Heart, CourseTone.Rose),
)
private val fallback = listOf(CourseTone.Blue, CourseTone.Violet, CourseTone.Green, CourseTone.Orange, CourseTone.Teal, CourseTone.Amber)
fun courseVisual(courseName: String): CourseVisual {
    val name = courseName.trim().lowercase()
    rules.firstOrNull { rule -> rule.words.any(name::contains) }?.let { return CourseVisual(it.icon, it.tone) }
    var hash = 0u
    var index = 0
    while (index < courseName.length) {
        val first = courseName[index++].code
        val codePoint = if (first in 0xD800..0xDBFF && index < courseName.length && courseName[index].code in 0xDC00..0xDFFF) {
            0x10000 + ((first - 0xD800) shl 10) + (courseName[index++].code - 0xDC00)
        } else first
        hash = hash * 31u + codePoint.toUInt()
    }
    return CourseVisual(CourseIcon.Book, fallback[(hash % fallback.size.toUInt()).toInt()])
}
