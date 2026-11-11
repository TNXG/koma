/**
 * 给 UI 的课表领域模型：与教务系统原始字段彻底解耦，
 * 由 EduService 把学校插件的 RuleOutput 转换而来（见 services/edu-service.ts）。
 */

/** 一门排课；weeks 为显式周次列表，source 保留教务原始串用于溯源与排错 */
export interface TimetableCourse {
  /** 稳定标识：由 name|teacher|location|weekday|period 派生，跨周合并后唯一 */
  id: string;
  name: string;
  teacher?: string;
  location?: string;
  /** 星期 1–7 */
  weekday: number;
  startPeriod: number;
  endPeriod: number;
  weeks: number[];
  source: {
    pluginId: string;
    kcmc?: string;
    xqj?: string;
    jcs?: string;
    zcd?: string;
  };
}

export interface TimetableSemester {
  /** 如 "2025–2026 学年 第 1 学期" */
  name: string;
  /** 第 1 周周一，YYYY-MM-DD */
  startDate: string;
  totalWeeks: number;
}

/** 作息时间格；start/end 为当日分钟数，第 1 节 index = 1 */
export interface TimetableTimeSlot {
  index: number;
  start: number;
  end: number;
}

export interface TimetableTerm {
  xnm: string;
  xqm: string;
}

/** 一次导入得到的完整课表快照，UI 与持久化的唯一数据形态 */
export interface TimetableSnapshot {
  schoolId: string;
  term: TimetableTerm;
  semester: TimetableSemester;
  timeGrid: TimetableTimeSlot[];
  courses: TimetableCourse[];
  studentName?: string;
  importedAt: string;
}

/** 课程稳定标识：同一门课（同合并键）跨周合并后共享一个 id */
export function courseIdOf(c: Pick<TimetableCourse, "name" | "teacher" | "location" | "weekday" | "startPeriod" | "endPeriod">): string {
  return [
    c.name,
    c.teacher ?? "",
    c.location ?? "",
    c.weekday,
    c.startPeriod,
    c.endPeriod,
  ].join("|");
}

/** 课程是否在指定周上课（week 从 1 起） */
export function courseHeldOnWeek(course: TimetableCourse, week: number): boolean {
  return course.weeks.includes(week);
}

/** 指定周有课的课程，按 星期 → 开始节次 排序（连续日程流与周视图共用） */
export function coursesForWeek(courses: TimetableCourse[], week: number): TimetableCourse[] {
  return courses
    .filter(c => courseHeldOnWeek(c, week))
    .sort((a, b) => a.weekday - b.weekday || a.startPeriod - b.startPeriod);
}
