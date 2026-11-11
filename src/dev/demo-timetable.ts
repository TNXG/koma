/**
 * 仅开发构建使用的示例课表：Web 端拿不到教务 Cookie，用它预览 UI。
 * 生产构建中入口按钮被 import.meta.env.DEV 剔除。
 */

import type { TimetableCourse, TimetableSnapshot } from "@/types/timetable";
import { courseIdOf } from "@/types/timetable";

const range = (from: number, to: number) => Array.from({ length: to - from + 1 }, (_, i) => from + i);

const RAW: Omit<TimetableCourse, "id" | "source">[] = [
  {
    name: "大学英语I",
    teacher: "李静",
    location: "A 栋 302",
    weekday: 1,
    startPeriod: 1,
    endPeriod: 2,
    weeks: range(1, 16),
  },
  {
    name: "高等数学",
    teacher: "王建国",
    location: "B 栋 105",
    weekday: 1,
    startPeriod: 3,
    endPeriod: 4,
    weeks: range(1, 18),
  },
  {
    name: "Python 程序设计",
    teacher: "陈晓",
    location: "实训楼 401",
    weekday: 1,
    startPeriod: 7,
    endPeriod: 8,
    weeks: range(3, 18),
  },
  {
    name: "思想道德与法治",
    teacher: "赵敏",
    location: "C 栋 201",
    weekday: 2,
    startPeriod: 1,
    endPeriod: 2,
    weeks: range(1, 12),
  },
  {
    name: "体育",
    teacher: "刘强",
    location: "东区操场",
    weekday: 2,
    startPeriod: 5,
    endPeriod: 6,
    weeks: range(1, 18),
  },
  {
    name: "高等数学",
    teacher: "王建国",
    location: "B 栋 105",
    weekday: 3,
    startPeriod: 1,
    endPeriod: 2,
    weeks: range(1, 18),
  },
  {
    name: "大学英语I",
    teacher: "李静",
    location: "A 栋 302",
    weekday: 3,
    startPeriod: 3,
    endPeriod: 4,
    weeks: range(1, 16),
  },
  {
    name: "电工电子技术",
    teacher: "孙浩",
    location: "实训楼 203",
    weekday: 3,
    startPeriod: 7,
    endPeriod: 8,
    weeks: range(1, 18).filter(w => w % 2 === 1),
  },
  {
    name: "Python 程序设计",
    teacher: "陈晓",
    location: "实训楼 401",
    weekday: 4,
    startPeriod: 1,
    endPeriod: 2,
    weeks: range(3, 18),
  },
  {
    name: "心理健康教育",
    teacher: "周琳",
    location: "C 栋 105",
    weekday: 4,
    startPeriod: 5,
    endPeriod: 6,
    weeks: range(1, 8),
  },
  {
    name: "形势与政策",
    teacher: "吴涛",
    location: "报告厅",
    weekday: 4,
    startPeriod: 9,
    endPeriod: 10,
    weeks: [2, 4, 6, 8, 10, 12, 14],
  },
  {
    name: "职业生涯规划",
    teacher: "郑云",
    location: "C 栋 301",
    weekday: 5,
    startPeriod: 3,
    endPeriod: 4,
    weeks: range(1, 18),
  },
  {
    name: "体育",
    teacher: "刘强",
    location: "东区操场",
    weekday: 5,
    startPeriod: 7,
    endPeriod: 8,
    weeks: range(1, 18),
  },
];

export function demoSnapshot(): TimetableSnapshot {
  return {
    schoolId: "xjvut",
    term: { xnm: "2026", xqm: "3" },
    semester: { name: "2026–2027 学年 第 1 学期", startDate: "2026-08-31", totalWeeks: 20 },
    timeGrid: [
      [1, 600, 645],
      [2, 655, 740],
      [3, 760, 845],
      [4, 855, 940],
      [5, 1020, 1065],
      [6, 1075, 1120],
      [7, 1140, 1185],
      [8, 1195, 1240],
      [9, 1290, 1335],
      [10, 1345, 1390],
    ].map(([index, start, end]) => ({ index, start, end })),
    courses: RAW.map(c => ({
      ...c,
      id: courseIdOf(c),
      source: { pluginId: "demo" },
    })),
    studentName: "同学",
    importedAt: new Date().toISOString(),
  };
}
