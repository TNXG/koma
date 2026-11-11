/**
 * 教务插件与宿主之间的解析结果协议（docs/edu-auth README §4）：
 * 学校插件把教务系统的原始 JSON 解析成这个统一形态，宿主再转成领域模型。
 */

export type DiagnosticLevel = "info" | "warn" | "error";

/** 解析诊断：级别 + 稳定码 + 面向开发者的说明 + 来源行号（0 起） */
export interface Diagnostic {
  level: DiagnosticLevel;
  code: string;
  message: string;
  at?: { row: number };
}

/** 教务原始行中保留下来用于溯源的字段 */
export interface ParsedCourseRaw {
  kcmc?: string;
  xqj?: string;
  jcs?: string;
  zcd?: string;
}

/** 解析后的单门排课；weeks 为显式周次列表的逗号串（如 "1,3,5"） */
export interface ParsedCourse {
  name: string;
  teacher?: string;
  location?: string;
  /** 星期 1–7 */
  weekday: number;
  startPeriod: number;
  endPeriod: number;
  weeks: string;
  raw: ParsedCourseRaw;
}

/** 学期元信息；startDate 为该学期第 1 周周一（YYYY-MM-DD） */
export interface SemesterMeta {
  name: string;
  startDate: string;
  totalWeeks: number;
}

/** 作息时间格；start/end 为当日分钟数，第 1 节 index = 1 */
export interface TimeSlot {
  index: number;
  start: number;
  end: number;
}

/** 教务系统学期参数：xnm 学年起始年，xqm 3/12/16 = 第一/二/三学期 */
export interface EduTerm {
  xnm: string;
  xqm: string;
}

/** 学校插件解析课表的统一输出 */
export interface RuleOutput {
  courses: ParsedCourse[];
  diagnostics: Diagnostic[];
  timeGrid: TimeSlot[];
  semester: SemesterMeta;
}
