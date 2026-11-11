/** 学期参数的展示与推断（docs/edu-auth README §1、§3）。 */

import type { EduTerm } from "../../contracts/protocol";

/** xqm 值 → 学期序号 */
const TERM_INDEX: Record<string, string> = { 3: "1", 12: "2", 16: "3" };

/** "2025–2026 学年 第 1 学期"；未知 xqm 用原值兜底 */
export function termLabel(term: EduTerm): string {
  const index = TERM_INDEX[term.xqm] ?? term.xqm;
  return `${term.xnm}–${Number(term.xnm) + 1} 学年 第 ${index} 学期`;
}

/**
 * 索引页读不到学期时按当前月份猜：9–1 月是上学期（学年起始年为 9 月所在年，
 * 1 月仍属上一年秋季学期），2–8 月是下学期。
 */
export function guessTerm(now: Date = new Date()): EduTerm {
  const y = now.getFullYear();
  const m = now.getMonth() + 1;
  if (m >= 9) {
    return {
      xnm: String(y),
      xqm: "3",
    };
  }
  if (m >= 2) {
    return {
      xnm: String(y),
      xqm: "12",
    };
  }
  return {
    xnm: String(y - 1),
    xqm: "3",
  };
}
