/**
 * 周次表达式解析（docs/edu-auth README §4.2）：
 * 按逗号/分号分段，每段剥掉前导「第」，交给段内表达式解析；单双周只作用于本段。
 * 例："第1-8周(单),10,13-16周(双)" → [1,3,5,7,10,14,16]
 */

export interface WeeksParseResult {
  /** 去重升序后的显式周次列表 */
  weeks: number[];
  /** 是否存在解析不了的段（其余段仍有效） */
  partial: boolean;
}

/** 单段：区间（可选单/双周）或单个周次；不匹配返回 null */
function parseWeekSegment(segment: string): number[] | null {
  const s = segment.replace(/^第/, "").trim();
  const range = /^(\d+)\s*-\s*(\d+)\s*(?:周\s*)?(?:\(\s*(单|双)\s*\))?$/.exec(s);
  if (range) {
    const from = Number(range[1]);
    const to = Number(range[2]);
    if (from < 1 || to < from || to > 60)
      return null;
    const parity = range[3];
    const weeks: number[] = [];
    for (let w = from; w <= to; w++) {
      if (parity === "单" && w % 2 === 0)
        continue;
      if (parity === "双" && w % 2 === 1)
        continue;
      weeks.push(w);
    }
    return weeks;
  }
  const single = /^(\d+)\s*周?$/.exec(s);
  if (single) {
    const w = Number(single[1]);
    return w >= 1 && w <= 60
      ? [w]
      : null;
  }
  return null;
}

export function parseWeeksString(input: string): WeeksParseResult {
  const weeks = new Set<number>();
  let partial = false;
  for (const raw of input.split(/[,，;；]/)) {
    const segment = raw.trim();
    if (!segment)
      continue;
    const parsed = parseWeekSegment(segment);
    if (!parsed) {
      partial = true;
      continue;
    }
    for (const w of parsed)
      weeks.add(w);
  }
  return {
    weeks: [...weeks].sort((a, b) => a - b),
    partial,
  };
}

/** 显式周次列表 → 逗号串（诊断与导出用） */
export function weeksToString(weeks: number[]): string {
  return [...weeks].sort((a, b) => a - b).join(",");
}
