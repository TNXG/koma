/**
 * 周次表校验（docs/edu-auth README §3.3）：zs 从 1 连续递增、首个 rq 起始日为周一、
 * 每段正好 7 天、≤60 周。任一不满足即整体放弃（不沿用猜测的开学日）。
 */

import type { ZfWeek } from "./types";

export interface TermWeeksMeta {
  startDate: string;
  totalWeeks: number;
}

const DAY_MS = 24 * 60 * 60 * 1000;

/** "YYYY-MM-DD" → UTC 当日零点；非法返回 null */
function parseDate(s: string): Date | null {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(s);
  if (!m)
    return null;
  const d = new Date(Date.UTC(Number(m[1]), Number(m[2]) - 1, Number(m[3])));
  return Number.isNaN(d.getTime())
    ? null
    : d;
}

export function zfTermWeeks(weeks: unknown): TermWeeksMeta | null {
  if (!Array.isArray(weeks) || weeks.length === 0 || weeks.length > 60)
    return null;
  let firstStart: Date | null = null;
  for (let i = 0; i < weeks.length; i++) {
    const row = weeks[i] as ZfWeek | null;
    if (!row || typeof row !== "object")
      return null;
    const zs = Number(row.zs);
    if (!Number.isInteger(zs) || zs !== i + 1)
      return null;
    const seg = typeof row.rq === "string"
      ? row.rq.split("/")
      : [];
    if (seg.length !== 2)
      return null;
    const start = parseDate(seg[0]);
    const end = parseDate(seg[1]);
    if (!start || !end)
      return null;
    /* 首周必须是周一（UTC day 1）；每段必须正好 7 天且与上一段首尾相接 */
    if (i === 0) {
      if (start.getUTCDay() !== 1)
        return null;
      firstStart = start;
    } else if (start.getTime() - (firstStart as Date).getTime() !== i * 7 * DAY_MS) {
      return null;
    }
    if (end.getTime() - start.getTime() !== 6 * DAY_MS)
      return null;
  }
  if (!firstStart)
    return null;
  return {
    startDate: firstStart.toISOString().slice(0, 10),
    totalWeeks: weeks.length,
  };
}
