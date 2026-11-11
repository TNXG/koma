/** 正方教务移动端接口的原始类型（docs/edu-auth README §4.1）。字段都可能缺省，一律按 unknown 防御。 */

import type { EduTerm } from "../../contracts/protocol";

/** kbList 单行：课程名/教师/教室/星期/节次/周次 */
export interface ZfKb {
  kcmc?: string;
  xm?: string;
  cdmc?: string;
  xqj?: string;
  jcs?: string;
  zcd?: string;
  [key: string]: unknown;
}

/** cxZc 周次表单行：zs 周号，rq "2025-09-01/2025-09-07" */
export interface ZfWeek {
  zs?: number | string;
  rq?: string;
}

/** cxRjc 作息表单行：jcmc 节名，qssj/jssj 常为整段 "10:00 - 10:45" */
export interface ZfRjcRow {
  jcmc?: string;
  qssj?: string;
  jssj?: string;
}

export type ZfTerm = EduTerm;

/** 逐周拉取汇总：courses 的 zcd 已被查询周号覆盖 */
export interface ZfWeeklyTimetable {
  weeks: ZfWeek[];
  courses: ZfKb[];
  meta?: { startDate: string; totalWeeks: number };
  studentName?: string;
}
