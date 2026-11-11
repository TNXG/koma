/**
 * 新疆理工职业大学：统一身份认证（CAS）直登 + 教务（正方新版）原生取课表。
 * CAS 表单登录拿 TGT → jw/sso/zfiotlogin 换 ticket → ticketlogin 签发教务会话 →
 * POST xskbcxMobile_cxXsKb.html 逐周取课表 JSON。全程原生 HTTP 逐跳收 Cookie，不经 WebView。
 * 流程与易错点见 docs/edu-auth/README.md §2–§3。
 */

import type { EduHttp } from "../../contracts/http";
import type { EduCredentials, EduKbFetch, EduLoginBegin, EduLoginFlow, EduLoginOutcome } from "../../contracts/plugin";
import type { ZfKb, ZfTerm, ZfWeeklyTimetable } from "../../systems/zhengfang";
import { casErrorText, describeCasError, encryptCasPassword, extractExecution, isCaptchaError } from "../../cas/cas";
import { guessTerm, parseZfWeekly, termLabel, zfTermWeeks, zfTimeGrid } from "../../systems/zhengfang";
import {
  CAS_LOGIN,
  CAS_ORIGIN,
  hostOf,
  INDEX_RE,
  JW_ORIGIN,
  KB_URL,
  pick,
  RJC_URL,
  SSO_JUMP,
  WEEKS_URL,
  XjvutSession,
} from "./session";

/** 移动端索引页用隐藏字段 xnm_hide/xqm_hide；桌面索引页用下拉 */
function termFromIndex(html: string): ZfTerm | null {
  const hidden = (id: string) => {
    const tag = new RegExp(`<input\\b[^>]*\\bid=["']${id}["'][^>]*>`, "i").exec(html)?.[0];
    return tag
      ? /\bvalue\s*=\s*["']([^"']+)["']/i.exec(tag)?.[1]
      : undefined;
  };
  const selectedIn = (id: string) => {
    const seg = new RegExp(`<select[^>]*id="${id}"[\\s\\S]*?</select>`).exec(html);
    if (!seg)
      return null;
    return /<option[^>]*value="([^"]*)"[^>]*selected/.exec(seg[0])?.[1] ?? null;
  };
  const xnm = hidden("xnm_hide") ?? selectedIn("xnm");
  const xqm = hidden("xqm_hide") ?? selectedIn("xqm");
  return xnm && xqm
    ? { xnm, xqm }
    : null;
}

interface WeeklyFetch extends ZfWeeklyTimetable {
  meta: { startDate: string; totalWeeks: number };
  studentName: string;
}

/** 同学期周次接口：失败时不沿用猜测的开学日 */
async function fetchWeeks(session: XjvutSession, http: EduHttp, term: ZfTerm): Promise<unknown> {
  try {
    const r = await session.call(http, {
      url: WEEKS_URL,
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8", "X-Requested-With": "XMLHttpRequest" },
      body: new URLSearchParams({ xnm: term.xnm, xqm: term.xqm }).toString(),
    });
    return JSON.parse(r.body);
  } catch {
    return null;
  }
}

/** 作息：日课表接口按学期给每节的起止钟点；失败不阻断导入（应用默认作息兜底） */
async function fetchRjc(session: XjvutSession, http: EduHttp, term: ZfTerm): Promise<unknown> {
  try {
    const r = await session.call(http, {
      url: RJC_URL,
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded", "X-Requested-With": "XMLHttpRequest", "Accept": "application/json, text/javascript, */*; q=0.01" },
      body: new URLSearchParams({ xnm: term.xnm, xqm: term.xqm }).toString(),
    });
    return JSON.parse(r.body);
  } catch {
    return undefined;
  }
}

/** 登录后逐周拉取移动端课表；周课表决定最终排课，学期接口不参与覆盖 */
async function fetchWeeklyTimetable(session: XjvutSession, http: EduHttp, term: ZfTerm): Promise<WeeklyFetch | null> {
  const weeks = await fetchWeeks(session, http, term);
  const meta = zfTermWeeks(weeks);
  if (!meta || !Array.isArray(weeks))
    return null;
  const courses: ZfKb[] = [];
  let studentName = "";
  for (const week of weeks) {
    const zs = String((week as { zs?: unknown }).zs ?? "");
    if (!/^\d+$/.test(zs))
      return null;
    const r = await session.call(http, {
      url: KB_URL,
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
        "X-Requested-With": "XMLHttpRequest",
        "Referer": SSO_JUMP,
        "Accept": "application/json, text/javascript, */*; q=0.01",
      },
      body: new URLSearchParams({
        xnm: term.xnm,
        xqm: term.xqm,
        zs,
        kblx: "1",
        doType: "app",
      }).toString(),
    });
    let json: unknown;
    try {
      json = JSON.parse(r.body);
    } catch {
      return null;
    }
    if (!isKbResponse(json))
      return null;
    const student = json.xsxx?.XM;
    if (typeof student === "string" && student.trim())
      studentName = student.trim();
    for (const item of json.kbList) {
      if (!item || typeof item !== "object")
        return null;
      courses.push({ ...(item as ZfKb), zcd: zs });
    }
  }
  return {
    weeks: weeks as ZfWeeklyTimetable["weeks"],
    courses,
    meta,
    studentName,
  };
}

/** kbList 响应的窄化防御：xsxx.XM 与 kbList 都是可选的 */
function isKbResponse(json: unknown): json is { xsxx?: { XM?: unknown }; kbList: unknown[] } {
  return !!json && typeof json === "object" && "kbList" in json && Array.isArray((json as { kbList: unknown }).kbList);
}

/** 登录后立即调用：SSO 落索引页 → 学期 → 逐周课表 → 解析 → 时间格 */
async function fetchTimetable(session: XjvutSession, http: EduHttp): Promise<EduKbFetch | null> {
  const page = await session.follow(http, SSO_JUMP);
  if (hostOf(page.url) !== new URL(JW_ORIGIN).host || !INDEX_RE.test(page.url))
    return null;
  const term = termFromIndex(page.body) ?? guessTerm();
  const weekly = await fetchWeeklyTimetable(session, http, term);
  if (!weekly)
    return null;
  const parsed = parseZfWeekly(weekly);
  if (parsed.courses.length === 0)
    return null;
  const out = {
    ...parsed,
    semester: {
      name: termLabel(term),
      ...weekly.meta,
    },
    timeGrid: zfTimeGrid(await fetchRjc(session, http, term)),
  };
  return {
    out,
    term,
    pageUrl: page.url,
    studentName: weekly.studentName,
  };
}

/** 未登录：拿公钥（种下 _pv0CAS）并按需取验证码 */
async function beginForm(session: XjvutSession, http: EduHttp): Promise<EduLoginBegin> {
  const page = await session.follow(http, CAS_LOGIN);
  const execution = extractExecution(page.body);
  if (!execution)
    throw new Error("登录页没有就绪");
  const kr = await session.call(http, {
    url: `${CAS_ORIGIN}/cas/v1/getPubKey`,
    method: "POST",
    headers: { "Content-Type": "application/json", "X-Requested-With": "XMLHttpRequest" },
    body: "{}",
  });
  const keyJson = JSON.parse(kr.body) as { modulus?: string; exponent?: string };
  if (!keyJson.modulus)
    throw new Error("拿不到登录公钥");
  const status = await session.call(http, { url: `${CAS_ORIGIN}/cas/getKaptchaStatus` });
  const need = /true/i.test(status.body);
  let captcha: string | null = null;
  if (need) {
    const img = await fetchKaptcha(session, http);
    captcha = img;
  }
  session.key = { execution, modulus: keyJson.modulus, exponent: keyJson.exponent || "10001" };
  return { kind: "form", captcha };
}

async function fetchKaptcha(session: XjvutSession, http: EduHttp): Promise<string | null> {
  const img = await session.call(http, { url: `${CAS_ORIGIN}/cas/kaptcha?time=${Date.now()}`, binary: true });
  const mime = (pick(img.headers, "content-type")[0] ?? "image/png").split(";")[0].trim();
  return `data:${mime};base64,${img.body}`;
}

/** flow 工厂：每次 begin 前创建，会话状态封闭在闭包里的实例中 */
export function createXjvutFlow(): EduLoginFlow {
  const session = new XjvutSession();

  return {
    async begin(http, cookie) {
      session.reset();
      /* Profile 里两台主机的会话都拿来试：CAS 的 TGT、教务的 JSESSIONID/route */
      await session.seedFrom(cookie);
      /* TGT 活着：zfiotlogin 一趟直接换到已认证教务会话，落在课表索引页 */
      const ready = await session.follow(http, SSO_JUMP);
      if (hostOf(ready.url) === new URL(JW_ORIGIN).host && INDEX_RE.test(ready.url)) {
        return {
          kind: "ready",
          url: ready.url,
          jars: session.browserJars(),
        };
      }
      session.reset();
      return beginForm(session, http);
    },

    async login(http, c: EduCredentials): Promise<EduLoginOutcome> {
      if (!session.key)
        throw new Error("登录还没准备好");
      const enc = encryptCasPassword(c.password, session.key.modulus, session.key.exponent);
      const form = new URLSearchParams({
        username: c.username,
        password: enc,
        code: "",
        authcode: c.captcha,
        execution: session.key.execution,
        _eventId: "submit",
        type: "zhmm",
      });
      const r = await session.follow(http, CAS_LOGIN, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded", "Referer": CAS_LOGIN },
        body: form.toString(),
      });
      /* 还在登录页 = 失败，服务器已换掉 execution：整轮重来；只是验证码错时页面会换一张自动重试 */
      if (extractExecution(r.body)) {
        const raw = casErrorText(r.body);
        const message = describeCasError(raw);
        session.reset();
        return {
          kind: "fail",
          message,
          captcha: isCaptchaError(raw),
        };
      }
      /* CAS 已登录：zfiotlogin 换已认证教务会话（JSESSIONID/route/rememberMe 都进瓶） */
      const page = await session.follow(http, SSO_JUMP);
      if (hostOf(page.url) !== new URL(JW_ORIGIN).host || !INDEX_RE.test(page.url)) {
        session.reset();
        return { kind: "fail", message: "教务登录没接上，请重试" };
      }
      return {
        kind: "ok",
        url: page.url,
        jars: session.browserJars(),
      };
    },

    /** 换一张验证码：同一会话的公钥/Cookie 不动 */
    async refreshCaptcha(http: EduHttp): Promise<string | null> {
      return fetchKaptcha(session, http);
    },

    fetchTimetable: (http: EduHttp): Promise<EduKbFetch | null> => fetchTimetable(session, http),
  };
}
