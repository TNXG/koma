/**
 * kbList → 课程列表（docs/edu-auth README §3.6、§4.2）：
 * 按 name|teacher|location|weekday|startPeriod|endPeriod 合并同一门课，weeks 逗号拼接；
 * 行级问题记诊断并跳过，不阻断其余行。
 */

import type { Diagnostic, ParsedCourse } from "../../contracts/protocol";
import type { ZfWeeklyTimetable } from "./types";
import { parsePeriodRange } from "@/domain/period";
import { cleanText } from "@/domain/text";
import { parseWeeksString, weeksToString } from "@/domain/weeks";

interface CourseKeyParts {
  name: string;
  teacher?: string;
  location?: string;
  weekday: number;
  startPeriod: number;
  endPeriod: number;
}

const mergeKey = (p: CourseKeyParts): string =>
  `${p.name}|${p.teacher ?? ""}|${p.location ?? ""}|${p.weekday}|${p.startPeriod}|${p.endPeriod}`;

function diagnostic(code: string, level: Diagnostic["level"], message: string, row: number): Diagnostic {
  return {
    level,
    code,
    message,
    at: { row },
  };
}

/**
 * 逐周拉取汇总（zcd 已被查询周号覆盖）→ 合并后的课程与诊断。
 * 学期元信息与时间格由调用方（学校插件）组装，这里只负责课程行。
 */
export function parseZfWeekly(weekly: Pick<ZfWeeklyTimetable, "courses">): { courses: ParsedCourse[]; diagnostics: Diagnostic[] } {
  const diagnostics: Diagnostic[] = [];
  const merged = new Map<string, ParsedCourse>();

  weekly.courses.forEach((item, row) => {
    const name = cleanText(item.kcmc);
    if (!name) {
      diagnostics.push(diagnostic("EMPTY_NAME", "warn", "课程名为空，已跳过", row));
      return;
    }
    const weekday = Number(cleanText(item.xqj));
    if (!Number.isInteger(weekday) || weekday < 1 || weekday > 7) {
      diagnostics.push(diagnostic("BAD_WEEKDAY", "warn", `星期「${cleanText(item.xqj)}」不在 1–7，已跳过`, row));
      return;
    }
    const period = parsePeriodRange(cleanText(item.jcs));
    if (!period) {
      diagnostics.push(diagnostic("BAD_PERIOD", "warn", `节次「${cleanText(item.jcs)}」无法解析，已跳过`, row));
      return;
    }
    const weeks = parseWeeksString(cleanText(item.zcd));
    if (weeks.weeks.length === 0) {
      diagnostics.push(diagnostic("BAD_WEEKS", "warn", `周次「${cleanText(item.zcd)}」无法解析，已跳过`, row));
      return;
    }
    if (weeks.partial)
      diagnostics.push(diagnostic("WEEKS_PARTIAL", "info", `周次「${cleanText(item.zcd)}」部分段无法解析，已保留其余周`, row));

    const teacher = cleanText(item.xm) || undefined;
    const location = cleanText(item.cdmc) || undefined;
    const key = mergeKey({
      name,
      teacher,
      location,
      weekday,
      startPeriod: period.start,
      endPeriod: period.end,
    });
    const existing = merged.get(key);
    if (existing) {
      const all = new Set([
        ...existing.weeks.split(",").filter(Boolean).map(Number),
        ...weeks.weeks,
      ]);
      existing.weeks = weeksToString([...all]);
      return;
    }
    merged.set(key, {
      name,
      teacher,
      location,
      weekday,
      startPeriod: period.start,
      endPeriod: period.end,
      weeks: weeksToString(weeks.weeks),
      raw: {
        kcmc: item.kcmc,
        xqj: item.xqj,
        jcs: item.jcs,
        zcd: item.zcd,
      },
    });
  });

  const courses = [
    ...merged.values(),
  ];
  if (courses.length === 0)
    diagnostics.push(diagnostic("EMPTY", "error", "接口没有返回任何课程", 0));
  return { courses, diagnostics };
}
