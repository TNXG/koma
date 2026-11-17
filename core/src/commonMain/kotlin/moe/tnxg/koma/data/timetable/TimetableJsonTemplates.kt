package moe.tnxg.koma.data.timetable

const val LOCAL_JSON_SCHOOL_ID = "local-json"

const val JSON_TIMETABLE_EXAMPLE = """{
  "version": 1,
  "semester": {
    "name": "2026 年秋季学期",
    "startDate": "2026-09-07",
    "totalWeeks": 18
  },
  "timeGrid": [
    {"index": 1, "start": "08:10", "end": "08:55"},
    {"index": 2, "start": "09:05", "end": "09:50"}
  ],
  "courses": [
    {
      "name": "高等数学",
      "teacher": null,
      "location": "教学楼 201",
      "weekday": 1,
      "startPeriod": 1,
      "endPeriod": 2,
      "weeks": [1, 3, 5, 7, 9, 11, 13, 15, 17],
      "note": "单周上课"
    }
  ]
}"""

const val JSON_TIMETABLE_GUIDE = """课表 JSON 格式说明

只接受单个 JSON 对象。字段名区分大小写；所有层级的未知字段均拒绝，不会静默忽略。不要输出 Markdown 代码围栏、注释、前言或结尾说明。数字必须是整数，不能用字符串或小数替代。

完整结构与字段类型（“可省略”以外全部必填）：
• version：整数，必须为 1。
• semester：对象，只能包含 name、startDate、totalWeeks。
  - name：非空字符串，学期名称。
  - startDate：字符串，严格为 YYYY-MM-DD，必须是真实存在的日期，且是第 1 教学周的周一，不是任意开学日或截图日期。
  - totalWeeks：整数，1–60，总教学周数。
• timeGrid：非空数组，1–30 个对象，每个对象只能包含 index、start、end。
  - index：整数，按数组顺序从 1 开始连续递增（1、2、3……），不能跳号。
  - start、end：字符串，严格为 HH:mm（00:00–23:59），结束时间必须晚于开始时间，不能跨日。节次按时间排列，不能重叠；相邻节次可以首尾相接。
  - 必须填写该学校真实的完整作息节次表；不会补齐节次或套用教务默认时间。示例钟点仅用于演示，不能据此猜测实际钟点。
• courses：数组，可以为空；每个对象只能包含 name、teacher、location、weekday、startPeriod、endPeriod、weeks、note。
  - name：非空字符串，课程名称；导入时去除首尾空白。
  - teacher：字符串或 null，可省略；去除首尾空白，空串视为 null。
  - location：字符串或 null，可省略；去除首尾空白，空串视为 null。
  - weekday：整数，1–7（周一为 1，周日为 7）。
  - startPeriod、endPeriod：整数，含首尾的起止节次，必须对应 timeGrid 中的节次，且 startPeriod ≤ endPeriod。
  - weeks：非空整数数组，每项为 1–totalWeeks 的教学周次；导入时去重并升序排列。不能写“1-16周”“单周”“双周”等文本，必须展开为具体周数，例如 [1,3,5]。
  - note：字符串，可省略；省略时为空字符串，不接受 null。备注按原文保留。
• studentName：字符串或 null，可省略；去除首尾空白，空串视为 null。建议省略，避免暴露身份。

同名课程在不同星期或不同节次上课，必须拆为不同课程对象。课程名、教师、地点、星期和起止节次全部相同的排课项，规范化空白后视为重复，即使 weeks 或 note 不同也拒绝导入：请先手动合并周次，完整确认并保留所有备注；有冲突时先核实，不能直接丢弃其中一条。

空 courses 不代表识别成功或自动导入；导入前需要检查学期、第 1 周周一、总周数、作息、课程数量和周次概况，再明确确认。

隐私：发送截图给 AI 前先遮挡学生姓名、学号、联系方式、账号及其他无关个人信息。不要把学号或隐私信息放进任何 JSON 字段。"""

const val JSON_TIMETABLE_AI_PROMPT = """请将我提供的课表截图整理为 Koma 可导入的课表 JSON。

先检查截图是否包含学期名称、第 1 教学周的周一日期、总教学周数、每一节真实起止钟点、每门课程的名称/星期/起止节次/具体周次。
如果任何关键字段缺失、模糊、无法识别或相互矛盾，先逐项向我询问并等待答复，不要猜测日期、星期、周次、节次、作息时间，也不要拿示例中的值补缺。截图只有本周课表时，必须询问每门课程完整周次，不能推断为整个学期。
教师、地点、备注不清楚时也不要猜测；先询问是否补充或允许省略。studentName 建议直接省略，不要识别或输出学生姓名、学号等个人身份信息。发送截图前请先遮挡姓名、学号及其他无关隐私信息。
确认所有必填信息后，仅输出一个原始 JSON 对象，不要 Markdown 代码围栏、解释文字、注释或多份备选结果。没有足够信息时可以先提问，但不要生成带占位符、null 必填字段、猜测值或不完整数据的 JSON。
单周、双周或周次范围必须在核实总周数后展开为整数列表。同名但不同星期或不同时间的课程拆成多个课程对象；完全相同的排课身份先合并周次，备注冲突先询问，不要丢失信息。除非已确认课表确实没有任何课程，否则不要因为识别失败而输出空 courses。
严格遵循下面完整字段定义、校验规则和示例；示例仅展示格式，不代表我的学期、课程或作息。

""" + JSON_TIMETABLE_GUIDE + "\n\n完整有效示例：\n" + JSON_TIMETABLE_EXAMPLE
