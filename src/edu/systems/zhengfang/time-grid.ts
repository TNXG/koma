/**
 * 作息表 → 时间格（docs/edu-auth README §3.5、§4.1）：
 * qssj 常为整段 "10:00 - 10:45"（取首尾时间），jssj 可为空；失败不阻断导入，应用用默认作息兜底。
 */

import type { TimeSlot } from "../../contracts/protocol";
import type { ZfRjcRow } from "./types";

/** "08:00" → 分钟数；非法给 null */
function toMinutes(s: string): number | null {
  const m = /^(\d{1,2}):(\d{2})$/.exec(s.trim());
  if (!m)
    return null;
  const h = Number(m[1]);
  const min = Number(m[2]);
  return h >= 0 && h <= 23 && min <= 59
    ? h * 60 + min
    : null;
}

/** "10:00 - 10:45" → {start, end}；一段里取首尾时间 */
function parseRowTimes(row: ZfRjcRow): { start: number; end: number } | null {
  const times = [row.qssj, row.jssj]
    .filter((s): s is string => typeof s === "string")
    .flatMap(s => s.match(/\d{1,2}:\d{2}/g) ?? [])
    .map(toMinutes)
    .filter((m): m is number => m !== null);
  if (times.length === 0)
    return null;
  return {
    start: Math.min(...times),
    end: Math.max(...times),
  };
}

/** 按行序生成时间格（第 1 节 index = 1）；钟点解析失败的行跳过 */
export function zfTimeGrid(rows: unknown): TimeSlot[] {
  if (!Array.isArray(rows))
    return [];
  const grid: TimeSlot[] = [];
  rows.forEach((raw, i) => {
    if (!raw || typeof raw !== "object")
      return;
    const times = parseRowTimes(raw as ZfRjcRow);
    if (!times || times.end < times.start)
      return;
    grid.push({ index: i + 1, start: times.start, end: times.end });
  });
  return grid;
}

/**
 * 默认作息兜底：作息接口失败且无历史记录时使用（通用五段制，仅保证可展示，
 * 不代表任何学校实际作息；拿到真实作息后会被覆盖）。
 */
export const DEFAULT_TIME_GRID: TimeSlot[] = [
  { index: 1, start: 8 * 60, end: 8 * 60 + 45 },
  { index: 2, start: 8 * 60 + 55, end: 9 * 60 + 40 },
  { index: 3, start: 10 * 60, end: 10 * 60 + 45 },
  { index: 4, start: 10 * 60 + 55, end: 11 * 60 + 40 },
  { index: 5, start: 14 * 60, end: 14 * 60 + 45 },
  { index: 6, start: 14 * 60 + 55, end: 15 * 60 + 40 },
  { index: 7, start: 15 * 60 + 50, end: 16 * 60 + 35 },
  { index: 8, start: 16 * 60 + 45, end: 17 * 60 + 30 },
  { index: 9, start: 19 * 60, end: 19 * 60 + 45 },
  { index: 10, start: 19 * 60 + 55, end: 20 * 60 + 40 },
];
