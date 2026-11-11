/**
 * 节次解析（docs/edu-auth README §4.2）：
 * "3-4" 区间、"0304" 两位一节（合并为非连续节次的最小–最大跨度）、"5" 单节。
 */

export interface PeriodRange {
  start: number;
  end: number;
}

const MAX_PERIOD = 30;

export function parsePeriodRange(input: string): PeriodRange | null {
  const s = input.trim();
  if (!s)
    return null;
  const range = /^(\d{1,2})\s*-\s*(\d{1,2})$/.exec(s);
  if (range) {
    const start = Number(range[1]);
    const end = Number(range[2]);
    return validate(start <= end
      ? { start, end }
      : { start: end, end: start });
  }
  // 两位一节："0304" → [3, 4]；"030506" → [3, 5, 6]，跨取最小–最大
  if (s.length >= 4 && s.length % 2 === 0 && /^\d+$/.test(s)) {
    const periods: number[] = [];
    for (let i = 0; i < s.length; i += 2)
      periods.push(Number(s.slice(i, i + 2)));
    const start = Math.min(...periods);
    const end = Math.max(...periods);
    return validate({ start, end });
  }
  const single = /^(\d{1,2})$/.exec(s);
  if (single) {
    return validate({
      start: Number(single[1]),
      end: Number(single[1]),
    });
  }
  return null;
}

function validate(r: PeriodRange): PeriodRange | null {
  return r.start >= 1 && r.end >= r.start && r.end <= MAX_PERIOD
    ? r
    : null;
}
