import { describe, expect, it } from "vitest";
import { DEFAULT_TIME_GRID, guessTerm, parseZfWeekly, termLabel, zfTermWeeks, zfTimeGrid } from "@/edu/systems/zhengfang";

const validWeek = (zs: number) => {
  const start = Date.UTC(2025, 8, 1) + (zs - 1) * 7 * 86400000;
  const iso = (t: number) => new Date(t).toISOString().slice(0, 10);
  return { zs, rq: `${iso(start)}/${iso(start + 6 * 86400000)}` };
};

describe("zfTermWeeks", () => {
  it("合法周次表给出开学日与周数", () => {
    const meta = zfTermWeeks([1, 2, 3].map(validWeek));
    expect(meta).toEqual({ startDate: "2025-09-01", totalWeeks: 3 });
  });

  it("首周不是周一、不连续、段长不是 7 天都整体放弃", () => {
    expect(zfTermWeeks([
      { zs: 1, rq: "2025-09-02/2025-09-08" },
    ])).toBeNull();
    expect(zfTermWeeks([
      validWeek(1),
      validWeek(3),
    ])).toBeNull();
    expect(zfTermWeeks([
      { zs: 1, rq: "2025-09-01/2025-09-05" },
    ])).toBeNull();
    expect(zfTermWeeks([])).toBeNull();
    expect(zfTermWeeks(Array.from({ length: 61 }, (_, i) => validWeek(i + 1)))).toBeNull();
    expect(zfTermWeeks("nope")).toBeNull();
  });
});

describe("termLabel / guessTerm", () => {
  it("学期标签", () => {
    expect(termLabel({ xnm: "2025", xqm: "3" })).toBe("2025–2026 学年 第 1 学期");
    expect(termLabel({ xnm: "2025", xqm: "12" })).toBe("2025–2026 学年 第 2 学期");
    expect(termLabel({ xnm: "2025", xqm: "16" })).toBe("2025–2026 学年 第 3 学期");
  });

  it("按月份猜学期：9 月起上学期、2 月起下学期、1 月属上一年秋季学期", () => {
    expect(guessTerm(new Date(2025, 8, 15))).toEqual({ xnm: "2025", xqm: "3" });
    expect(guessTerm(new Date(2026, 2, 1))).toEqual({ xnm: "2026", xqm: "12" });
    expect(guessTerm(new Date(2026, 0, 15))).toEqual({ xnm: "2025", xqm: "3" });
  });
});

describe("zfTimeGrid", () => {
  it("qssj 整段取首尾，jssj 可为空；解析失败的行跳过", () => {
    const grid = zfTimeGrid([
      { jcmc: "第1节", qssj: "08:00 - 08:45" },
      { jcmc: "第2节", qssj: "08:55", jssj: "09:40" },
      { jcmc: "第3节", qssj: "不是时间" },
    ]);
    expect(grid).toEqual([
      { index: 1, start: 480, end: 525 },
      { index: 2, start: 535, end: 580 },
    ]);
  });

  it("非数组返回空数组", () => {
    expect(zfTimeGrid(null)).toEqual([]);
    expect(zfTimeGrid("x")).toEqual([]);
  });

  it("默认作息为 10 节兜底", () => {
    expect(DEFAULT_TIME_GRID).toHaveLength(10);
    expect(DEFAULT_TIME_GRID[0]).toEqual({ index: 1, start: 480, end: 525 });
  });
});

describe("parseZfWeekly", () => {
  it("按合并键合并同一门课，weeks 逗号拼接", () => {
    const { courses, diagnostics } = parseZfWeekly({
      courses: [
        {
          kcmc: "高等数学",
          xm: "李四",
          cdmc: "3-201",
          xqj: "1",
          jcs: "3-4",
          zcd: "1,2",
        },
        {
          kcmc: "高等数学",
          xm: "李四",
          cdmc: "3-201",
          xqj: "1",
          jcs: "3-4",
          zcd: "4-6周",
        },
        {
          kcmc: "大学英语",
          xqj: "3",
          jcs: "0304",
          zcd: "1-16周(单)",
        },
      ],
    });
    expect(diagnostics).toEqual([]);
    expect(courses).toHaveLength(2);
    expect(courses[0].weeks).toBe("1,2,4,5,6");
    expect(courses[0].teacher).toBe("李四");
    expect(courses[1].weeks).toBe("1,3,5,7,9,11,13,15");
    expect(courses[1].raw.zcd).toBe("1-16周(单)");
  });

  it("行级问题记诊断并跳过；全空记 EMPTY", () => {
    const { courses, diagnostics } = parseZfWeekly({
      courses: [
        {
          kcmc: " ",
          xqj: "2",
          jcs: "5",
          zcd: "1",
        },
        {
          kcmc: "体育",
          xqj: "9",
          jcs: "5",
          zcd: "1",
        },
        {
          kcmc: "美术",
          xqj: "2",
          jcs: "abc",
          zcd: "1",
        },
        {
          kcmc: "音乐",
          xqj: "2",
          jcs: "5",
          zcd: "某周",
        },
      ],
    });
    expect(courses).toHaveLength(0);
    expect(diagnostics.map(d => d.code)).toEqual([
      "EMPTY_NAME",
      "BAD_WEEKDAY",
      "BAD_PERIOD",
      "BAD_WEEKS",
      "EMPTY",
    ]);
    expect(diagnostics.every(d => d.at?.row !== undefined)).toBe(true);
  });

  it("周次部分段失败记 WEEKS_PARTIAL", () => {
    const { courses, diagnostics } = parseZfWeekly({
      courses: [
        {
          kcmc: "高等数学",
          xqj: "1",
          jcs: "1",
          zcd: "1,看不懂",
        },
      ],
    });
    expect(courses[0].weeks).toBe("1");
    expect(diagnostics.map(d => d.code)).toEqual(["WEEKS_PARTIAL"]);
  });
});
