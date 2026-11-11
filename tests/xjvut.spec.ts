import { describe, expect, it } from "vitest";
import { createMemoryCookieStore } from "@/edu/host/cookie-store";
import { createEduService } from "@/services/edu-service";
import { coursesForWeek } from "@/types/timetable";
import { createMockEduServer, MOCK_CAPTCHA, MOCK_STUDENT_NAME, MOCK_USERNAME } from "./mock-edu";

const setup = (options?: Parameters<typeof createMockEduServer>[0]) => {
  const server = createMockEduServer(options);
  const cookieStore = createMemoryCookieStore();
  const service = createEduService({ http: server.http, cookies: cookieStore });
  return { server, cookieStore, service };
};

describe("eduService × xjvut 全链路", () => {
  it("listSchools 暴露默认学校", () => {
    const { service } = setup();
    const schools = service.listSchools();
    expect(schools).toEqual([
      {
        id: "xjvut",
        name: "新疆理工职业大学 统一身份认证（CAS）",
        system: "zhengfang_new",
        directLogin: true,
      },
    ]);
  });

  it("prepareLogin → form + 验证码；错误验证码 fail(captcha)；重新 begin 后正确登录 ok", async () => {
    const { service } = setup();

    const prep = await service.prepareLogin();
    expect(prep.kind).toBe("form");
    if (prep.kind !== "form")
      return;
    expect(prep.captcha).toMatch(/^data:image\/png;base64,/);

    const wrong = await service.login("xjvut", { username: MOCK_USERNAME, password: "pw123", captcha: "0000" });
    expect(wrong).toMatchObject({ kind: "fail", message: "验证码不对", captcha: true });

    /* 服务端已换掉 execution：失败后必须整轮重来 */
    await service.prepareLogin();
    const ok = await service.login("xjvut", { username: MOCK_USERNAME, password: "pw123", captcha: MOCK_CAPTCHA });
    expect(ok).toMatchObject({ kind: "ok" });
  });

  it("不需要验证码时 captcha 为 null", async () => {
    const { service } = setup({ captchaRequired: false });
    const prep = await service.prepareLogin();
    expect(prep).toEqual({ kind: "form", captcha: null });
  });

  it("密码错不自动重试", async () => {
    const { service } = setup({ passwordRejected: true });
    await service.prepareLogin();
    const r = await service.login("xjvut", { username: MOCK_USERNAME, password: "bad", captcha: MOCK_CAPTCHA });
    expect(r).toMatchObject({ kind: "fail", message: "账号或密码不对", captcha: false });
  });

  it("登录 → importTimetable 产出领域快照（周次以查询周为准）", async () => {
    const { service } = setup();
    await service.prepareLogin();
    await service.login("xjvut", { username: MOCK_USERNAME, password: "pw123", captcha: MOCK_CAPTCHA });

    const r = await service.importTimetable("xjvut");
    expect(r.kind).toBe("ok");
    if (r.kind !== "ok")
      return;

    const { timetable, diagnostics } = r;
    expect(timetable.schoolId).toBe("xjvut");
    expect(timetable.term).toEqual({ xnm: "2025", xqm: "3" });
    expect(timetable.semester).toEqual({ name: "2025–2026 学年 第 1 学期", startDate: "2025-09-01", totalWeeks: 20 });
    expect(timetable.studentName).toBe(MOCK_STUDENT_NAME);
    expect(timetable.importedAt).toBeTruthy();

    /* mock 的 zcd 全是 "1"，周次必须来自逐周查询的 zs */
    const math = timetable.courses.find(c => c.name === "高等数学");
    expect(math?.weeks).toHaveLength(20);
    expect(math).toMatchObject({
      weekday: 1,
      startPeriod: 3,
      endPeriod: 4,
      teacher: "李四",
      location: "3-201",
    });
    const english = timetable.courses.find(c => c.name === "大学英语");
    expect(english?.weeks).toHaveLength(16);
    expect(english?.startPeriod).toBe(3);

    /* 时间格来自 cxRjc：第 4 行钟点解析失败被跳过 */
    expect(timetable.timeGrid).toEqual([
      { index: 1, start: 480, end: 525 },
      { index: 2, start: 535, end: 580 },
      { index: 3, start: 600, end: 645 },
    ]);

    expect(diagnostics.map(d => d.code)).toEqual(expect.arrayContaining(["EMPTY_NAME", "BAD_WEEKDAY"]));
    expect(diagnostics.every(d => d.level !== "error")).toBe(true);
  });

  it("第 N 周取课：体育坏行被剔除、大学英语只在 1–16 周", async () => {
    const { service } = setup();
    await service.prepareLogin();
    await service.login("xjvut", { username: MOCK_USERNAME, password: "pw123", captcha: MOCK_CAPTCHA });
    const r = await service.importTimetable("xjvut");
    if (r.kind !== "ok")
      throw new Error("import should succeed");

    const week1 = coursesForWeek(r.timetable.courses, 1);
    expect(week1.map(c => c.name)).toEqual(["高等数学", "大学英语"]);
    const week20 = coursesForWeek(r.timetable.courses, 20);
    expect(week20.map(c => c.name)).toEqual(["高等数学"]);
  });

  it("导入后宿主已存 Cookie，再次 prepareLogin 直接 ready", async () => {
    const { service, cookieStore } = setup();
    await service.prepareLogin();
    await service.login("xjvut", { username: MOCK_USERNAME, password: "pw123", captcha: MOCK_CAPTCHA });
    await service.importTimetable("xjvut");

    expect(await cookieStore.read("https://qyrz.xjvut.edu.cn")).toContain("CASTGT=TGT-1");

    const prep = await service.prepareLogin();
    expect(prep).toMatchObject({ kind: "ready" });
  });

  it("未登录导入给出明确失败；logout 清掉会话后 ready 不再成立", async () => {
    const { service, cookieStore } = setup();
    const before = await service.importTimetable("xjvut");
    expect(before).toMatchObject({ kind: "fail" });

    await service.prepareLogin();
    await service.login("xjvut", { username: MOCK_USERNAME, password: "pw123", captcha: MOCK_CAPTCHA });
    await service.logout("xjvut");
    expect(await cookieStore.read("https://qyrz.xjvut.edu.cn")).toBeNull();

    const prep = await service.prepareLogin();
    expect(prep.kind).toBe("form");
  });

  it("会话失效（教务拒绝无票访问）时导入失败并提示重新登录", async () => {
    /* 用从未登录过的干净服务直接走 begin 后导入：无 JSESSIONID，被 mock 踢回 CAS */
    const { service } = setup();
    await service.prepareLogin();
    const r = await service.importTimetable("xjvut");
    expect(r).toMatchObject({ kind: "fail", message: "课表获取失败，请重新登录" });
  });
});
