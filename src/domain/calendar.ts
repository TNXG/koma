/**
 * 日期与学期周次的纯函数：所有日期按本地时区的“日”处理，避免 UTC 偏移。
 */

import type { TimetableCourse, TimetableTimeSlot } from "@/types/timetable";

const DAY_MS = 86_400_000;
const WEEKDAY_LABELS = ["一", "二", "三", "四", "五", "六", "日"];

export function parseLocalDate(iso: string): Date {
  const [y, m, d] = iso.split("-").map(Number);
  return new Date(y, m - 1, d);
}

export function toIsoDate(date: Date): string {
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${m}-${d}`;
}

export function startOfDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

export function addDays(date: Date, days: number): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days);
}

export function sameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

/** 星期 1–7（周一为 1） */
export function isoWeekday(date: Date): number {
  return date.getDay() === 0
    ? 7
    : date.getDay();
}

export function mondayOf(date: Date): Date {
  return addDays(startOfDay(date), 1 - isoWeekday(date));
}

/** 相差的自然日数（b − a），跨夏令时也按日计 */
export function daysBetween(a: Date, b: Date): number {
  return Math.round((startOfDay(b).getTime() - startOfDay(a).getTime()) / DAY_MS);
}

/** 学期第几周（第 1 周 = 1）；开学前 ≤ 0 */
export function termWeekOf(startDate: string, date: Date): number {
  return Math.floor(daysBetween(parseLocalDate(startDate), date) / 7) + 1;
}

export function weekdayLabel(date: Date): string {
  return WEEKDAY_LABELS[isoWeekday(date) - 1];
}

/** 如 "11月11日" */
export function formatMonthDay(date: Date): string {
  return `${date.getMonth() + 1}月${date.getDate()}日`;
}

/** 当日分钟数 → "HH:mm" */
export function formatMinutes(minutes: number): string {
  const h = String(Math.floor(minutes / 60)).padStart(2, "0");
  const m = String(minutes % 60).padStart(2, "0");
  return `${h}:${m}`;
}

export function minutesOfDay(date: Date): number {
  return date.getHours() * 60 + date.getMinutes();
}

export interface DayCourse {
  course: TimetableCourse;
  /** 当日分钟数；作息表缺该节次时为 null */
  start: number | null;
  end: number | null;
}

/** 某天要上的课，按开始节次排序，并解析出上下课时间 */
export function coursesOnDate(
  courses: TimetableCourse[],
  timeGrid: TimetableTimeSlot[],
  startDate: string,
  date: Date,
): DayCourse[] {
  const week = termWeekOf(startDate, date);
  const weekday = isoWeekday(date);
  return courses
    .filter(c => c.weekday === weekday && c.weeks.includes(week))
    .sort((a, b) => a.startPeriod - b.startPeriod)
    .map(course => ({
      course,
      start: timeGrid.find(s => s.index === course.startPeriod)?.start ?? null,
      end: timeGrid.find(s => s.index === course.endPeriod)?.end ?? null,
    }));
}

/** 时长文案：35 → "35 分钟"，95 → "1 小时 35 分钟" */
export function formatDuration(minutes: number): string {
  const total = Math.max(0, Math.round(minutes));
  if (total < 60)
    return `${total} 分钟`;
  const rest = total % 60;
  return rest === 0
    ? `${Math.floor(total / 60)} 小时`
    : `${Math.floor(total / 60)} 小时 ${rest} 分钟`;
}
