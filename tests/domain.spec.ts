import { describe, expect, it } from "vitest";
import { parsePeriodRange } from "@/domain/period";
import { parseWeeksString } from "@/domain/weeks";

describe("parseWeeksString", () => {
  it("解析单双周区间", () => {
    expect(parseWeeksString("1-8周(单)").weeks).toEqual([1, 3, 5, 7]);
    expect(parseWeeksString("2-6周(双)").weeks).toEqual([2, 4, 6]);
  });

  it("解析普通区间、单周与多段", () => {
    expect(parseWeeksString("1-8周").weeks).toEqual([1, 2, 3, 4, 5, 6, 7, 8]);
    expect(parseWeeksString("5").weeks).toEqual([5]);
    expect(parseWeeksString("1,3,5").weeks).toEqual([1, 3, 5]);
    expect(parseWeeksString("第1-2周，4;；6").weeks).toEqual([1, 2, 4, 6]);
  });

  it("段间独立应用单双周并去重", () => {
    expect(parseWeeksString("1-4周(单),3-6周(双)").weeks).toEqual([1, 3, 4, 6]);
  });

  it("部分段失败记 partial 并保留其余段", () => {
    const r = parseWeeksString("1-3,看不懂,7");
    expect(r.weeks).toEqual([1, 2, 3, 7]);
    expect(r.partial).toBe(true);
  });

  it("全部失败返回空列表且 partial", () => {
    const r = parseWeeksString("某周");
    expect(r.weeks).toEqual([]);
    expect(r.partial).toBe(true);
  });

  it("拒绝越界区间", () => {
    expect(parseWeeksString("0-5").partial).toBe(true);
    expect(parseWeeksString("61-65").partial).toBe(true);
    expect(parseWeeksString("8-1").partial).toBe(true);
  });
});

describe("parsePeriodRange", () => {
  it("解析区间、两位一节与单节", () => {
    expect(parsePeriodRange("3-4")).toEqual({ start: 3, end: 4 });
    expect(parsePeriodRange("0304")).toEqual({ start: 3, end: 4 });
    expect(parsePeriodRange("5")).toEqual({ start: 5, end: 5 });
    expect(parsePeriodRange("030506")).toEqual({ start: 3, end: 6 });
  });

  it("非法输入返回 null", () => {
    expect(parsePeriodRange("")).toBeNull();
    expect(parsePeriodRange("abc")).toBeNull();
    expect(parsePeriodRange("0")).toBeNull();
    expect(parsePeriodRange("999")).toBeNull();
  });
});
