/**
 * 学校登录插件契约（源自 docs/edu-auth/plugin.ts，有两处调整，见 docs/architecture.md §3.1）：
 * 1. flow 是工厂 () => EduLoginFlow —— 会话状态（Cookie 瓶、CAS 公钥）封闭在实例内，
 *    不再用模块级可变状态，支持并发登录与失败重试。
 * 2. RuleOutput 定义在本层 contracts/protocol.ts，供所有学校插件共享。
 */

import type { EduCookieJar, EduHttp } from "./http";
import type { RuleOutput } from "./protocol";

/** 教务系统标识；页面识别与抓取脚本按系统区分，与学校无关 */
export type EduSystemId = "zhengfang" | "zhengfang_new" | "custom";

/** 教务站入口与学校标识；绑定与自动更新记录也按这三项存 */
export interface EduSchool {
  name: string;
  url: string;
  system: EduSystemId | null;
}

export interface EduCredentials {
  username: string;
  password: string;
  /** 验证码答案；begin 说不需要时传空串 */
  captcha: string;
}

/** begin：先拿已有会话试一遍，活着直接 ready（带落点与 Cookie）；没活回 form，captcha=null 表示这次不用验证码 */
export type EduLoginBegin
  = | { kind: "form"; captcha: string | null }
    | { kind: "ready"; url: string; jars: EduCookieJar[] };

/**
 * login：ok 带落点与 Cookie；fail 带给用户看的话（验证码/密码错、会话过期等），页面重新 begin。
 * captcha=true 表示这次只是验证码不对：页面可以换一张自动重试，凭据本身没问题。
 */
export type EduLoginOutcome
  = | { kind: "ok"; url: string; jars: EduCookieJar[] }
    | { kind: "fail"; message: string; captcha?: boolean };

/** 登录后原生拉到的课表：解析好的规则输出 + 当时选中的学期 + 落点页 */
export interface EduKbFetch {
  out: RuleOutput;
  term: { xnm: string; xqm: string };
  pageUrl: string;
  /** 学生姓名，登录后替换界面上的默认称呼 */
  studentName: string;
}

/** 学校专属的登录执行；一个实例承载一次登录会话的全部状态 */
export interface EduLoginFlow {
  begin: (http: EduHttp, cookie: (url: string) => Promise<string | null>) => Promise<EduLoginBegin>;
  login: (http: EduHttp, c: EduCredentials) => Promise<EduLoginOutcome>;
  /** 换一张验证码（同一会话的公钥/Cookie 不动）；没实现就整轮 begin */
  refreshCaptcha?: (http: EduHttp) => Promise<string | null>;
  /** 登录成功后用会话原生拉课表（正方学校实现）；没接上给 null，页面退回内置浏览器兜底 */
  fetchTimetable?: (http: EduHttp) => Promise<EduKbFetch | null>;
}

/** 登录方式：login = 应用内直登页（插件给 flow 工厂）；webview = 内置浏览器里用户自己登录 */
export type EduAuth = { kind: "webview" } | { kind: "login"; flow: () => EduLoginFlow };

export interface EduPlugin extends EduSchool {
  id: string;
  auth: EduAuth;
  /** 该学校可能写入会话 Cookie 的 url 列表（logout / 清数据时逐个清除） */
  sessionUrls: string[];
}

/** 这家学校是否启用应用内直登页（否则仍走内置浏览器自己登录） */
export const hasDirectLogin = (p: EduPlugin): boolean => p.auth.kind === "login";
