/**
 * 正方教务系统通用解析（与学校无关）。
 * 学校插件（如 schools/xjvut）负责取数与学期参数，这里负责把正方 JSON 解析成 RuleOutput。
 */

export { guessTerm, termLabel } from "./term";
export { DEFAULT_TIME_GRID, zfTimeGrid } from "./time-grid";
export * from "./types";
export { parseZfWeekly } from "./weekly-parser";
export { type TermWeeksMeta, zfTermWeeks } from "./weeks-table";
