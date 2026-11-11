/**
 * 首页连续时间线的纯计算：课表快照 + 今天/当前分钟 → 可渲染区块，
 * 以及日期 → 锚点、锚点 → 课程数的索引。滚动与 DOM 动画留在页面里。
 */

import type { TimetableCourse, TimetableSnapshot } from "@/types/timetable";
import {
  addDays,
  coursesOnDate,
  daysBetween,
  isoWeekday,
  parseLocalDate,
  sameDay,
  toIsoDate,
} from "@/domain/calendar";

export type RowState = "past" | "now" | "next" | "later";

export interface Row {
  course: TimetableCourse;
  start: number | null;
  end: number | null;
  state: RowState;
  pct: number;
}

export interface DayBlock {
  kind: "day";
  anchor: string;
  date: Date;
  relative: string | null;
  week: number;
  rows: Row[];
}

export interface FreeBlock {
  kind: "free";
  anchor: string;
  start: Date;
  end: Date;
  label: string;
  days: number;
}

export interface WeekBlock {
  kind: "week";
  anchor: string;
  week: number;
}

export type Block = DayBlock | FreeBlock | WeekBlock;

export interface DaySummary {
  parts: string[];
  empty: boolean;
}

const RELATIVE = ["今天", "明天", "后天"];

/** 整学期逐日展开；有课或每周一生成日期区块，连续无课合并成一块 */
export function buildTimeline(
  snapshot: TimetableSnapshot,
  startDate: string,
  today: Date,
  nowMinutes: number,
): Block[] {
  const termStart = parseLocalDate(startDate);
  const totalDays = daysBetween(termStart, addDays(termStart, snapshot.semester.totalWeeks * 7 - 1)) + 1;
  const out: Block[] = [];
  const rowsByDay = new Map<string, Row[]>();
  let nextMarked = false;

  for (let offset = 0; offset < totalDays; offset++) {
    const date = addDays(termStart, offset);
    const iso = toIsoDate(date);
    const isToday = sameDay(date, today);
    const items = coursesOnDate(snapshot.courses, snapshot.timeGrid, startDate, date);
    const rows: Row[] = items.map((item) => {
      const past = !isToday
        ? date < today
        : item.end !== null && item.end <= nowMinutes;
      if (past)
        return { ...item, state: "past", pct: 100 };
      if (isToday && item.start !== null && item.start <= nowMinutes)
        return { ...item, state: "now", pct: (nowMinutes - item.start) / Math.max(1, (item.end ?? item.start) - item.start) * 100 };
      if (!nextMarked && isToday) {
        nextMarked = true;
        return { ...item, state: "next", pct: 0 };
      }
      return { ...item, state: "later", pct: 0 };
    });
    rowsByDay.set(iso, rows);
  }

  for (let offset = 0; offset < totalDays; offset++) {
    const date = addDays(termStart, offset);
    const iso = toIsoDate(date);
    const rows = rowsByDay.get(iso)!;
    const dayOfWeek = isoWeekday(date);
    const weekNo = Math.floor(offset / 7) + 1;

    if (dayOfWeek === 1)
      out.push({ kind: "week", anchor: iso, week: weekNo });

    if (rows.length > 0 || dayOfWeek === 1) {
      const diff = daysBetween(today, date);
      out.push({
        kind: "day",
        anchor: iso,
        date,
        relative: diff >= 0 && diff < RELATIVE.length
          ? RELATIVE[diff]
          : null,
        week: weekNo,
        rows,
      });
      continue;
    }

    const previous = out.at(-1);
    if (previous?.kind === "free" && isoWeekday(previous.end) !== 7) {
      previous.end = date;
      previous.days++;
      continue;
    }
    out.push({
      kind: "free",
      anchor: iso,
      start: date,
      end: date,
      label: dayOfWeek >= 6
        ? "周末"
        : "无课",
      days: 1,
    });
  }
  return out;
}

/** 每个日期 → 所在区块的滚动锚点（无课日期落在整块「无课」上） */
export function blockAnchors(blocks: Block[]): Map<string, string> {
  const map = new Map<string, string>();
  for (const block of blocks) {
    if (block.kind === "day") {
      map.set(block.anchor, block.anchor);
    } else if (block.kind === "free") {
      for (let d = block.start; d <= block.end; d = addDays(d, 1))
        map.set(toIsoDate(d), block.anchor);
    }
  }
  return map;
}

/** 有课日期 → 课程数，供日期条角标使用 */
export function countCoursesByDay(blocks: Block[]): Map<string, number> {
  const map = new Map<string, number>();
  for (const block of blocks) {
    if (block.kind === "day")
      map.set(block.anchor, block.rows.length);
  }
  return map;
}

/** 顶部大标题下的副信息：周次、单双周、当天课程数/剩余节数 */
export function describeDay(
  snapshot: TimetableSnapshot,
  startDate: string,
  date: Date,
  today: Date,
  nowMinutes: number,
): DaySummary {
  const termStart = parseLocalDate(startDate);
  const rows = coursesOnDate(snapshot.courses, snapshot.timeGrid, startDate, date);
  const week = Math.floor(daysBetween(termStart, date) / 7) + 1;
  if (week < 1) {
    return {
      parts: [`距离开学 ${daysBetween(date, termStart)} 天`],
      empty: false,
    };
  }
  if (week > snapshot.semester.totalWeeks) {
    return {
      parts: ["学期已结束"],
      empty: false,
    };
  }
  const parts = [
    `第 ${week} 周`,
    week % 2 === 1
      ? "单周"
      : "双周",
  ];
  if (rows.length === 0) {
    parts.push("无课");
  } else if (sameDay(date, today)) {
    const remaining = rows.filter(r => r.end === null || r.end > nowMinutes).length;
    parts.push(`${rows.length} 节课，剩 ${remaining} 节`);
  } else {
    parts.push(`${rows.length} 节课`);
  }
  return { parts, empty: rows.length === 0 };
}
