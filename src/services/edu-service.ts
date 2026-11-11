/**
 * UI 的统一数据入口：登录编排 + 课表导入 → 领域模型。
 * 页面只消费这一层；所有方法返回可判别联合类型，不向 UI 抛业务异常。
 * 会话状态（flow 实例）按学校隔离：begin 创建，登录/导入期间复用，logout 后丢弃。
 */

import type { EduHttp } from "@/edu/contracts/http";
import type { EduCredentials, EduKbFetch, EduPlugin, EduSystemId } from "@/edu/contracts/plugin";
import type { Diagnostic } from "@/edu/contracts/protocol";
import type { EduCookieStore } from "@/edu/host/cookie-store";
import type { TimetableSnapshot } from "@/types/timetable";
import { parseWeeksString } from "@/domain/weeks";
import { createDefaultCookieStore } from "@/edu/host/cookie-store";
import { createEduHttp } from "@/edu/host/http";
import { DEFAULT_EDU_PLUGIN, EDU_PLUGINS, getEduPlugin, hasDirectLogin } from "@/edu/registry";
import { courseIdOf } from "@/types/timetable";

export interface EduSchoolInfo {
  id: string;
  name: string;
  system: EduSystemId | null;
  /** 是否启用应用内直登页（false = 内置浏览器自己登录） */
  directLogin: boolean;
}

export type EduLoginPreparation
  = | { kind: "ready"; url: string }
    | { kind: "form"; captcha: string | null }
    | { kind: "unavailable"; message: string };

export interface EduCredentialsInput {
  username: string;
  password: string;
  captcha: string;
}

export type EduLoginSubmitResult
  = | { kind: "ok"; url: string }
    | { kind: "fail"; message: string; captcha?: boolean };

export interface EduImportOk {
  kind: "ok";
  timetable: TimetableSnapshot;
  diagnostics: Diagnostic[];
}

export type EduImportResult = EduImportOk | { kind: "fail"; message: string };

export interface EduServiceOptions {
  /** 注入自定义 Http / Cookie 存取（测试与平台适配用）；缺省按平台自动选择 */
  http?: EduHttp;
  cookies?: EduCookieStore;
}

export interface EduService {
  listSchools: () => EduSchoolInfo[];
  /** 开始一次登录：先探测已有会话，再决定出表单（可能带验证码）还是直接 ready */
  prepareLogin: (schoolId?: string) => Promise<EduLoginPreparation>;
  /** 同一会话换一张验证码 */
  refreshCaptcha: (schoolId?: string) => Promise<string>;
  login: (schoolId: string, credentials: EduCredentialsInput) => Promise<EduLoginSubmitResult>;
  /** 用当前会话原生拉课表；未登录（无存活实例）时会先走一次 begin 探测 */
  importTimetable: (schoolId?: string) => Promise<EduImportResult>;
  /** 清除该学校的会话 Cookie 并丢弃登录实例 */
  logout: (schoolId: string) => Promise<void>;
}

interface SchoolSession {
  plugin: EduPlugin;
  flow: ReturnType<Extract<EduPlugin["auth"], { kind: "login" }>["flow"]> | null;
}

export function createEduService(options: EduServiceOptions = {}): EduService {
  const http = options.http ?? createEduHttp();
  const cookies = options.cookies ?? createDefaultCookieStore();
  /** schoolId → 进行中的登录/导入会话 */
  const sessions = new Map<string, SchoolSession>();

  const resolve = (schoolId?: string): EduPlugin => getEduPlugin(schoolId ?? DEFAULT_EDU_PLUGIN.id) ?? DEFAULT_EDU_PLUGIN;

  /** 取进行中的 flow 实例；没有则新建（并保留，供后续 login/import 使用） */
  const liveFlow = (session: SchoolSession) => {
    if (!session.flow) {
      session.flow = session.plugin.auth.kind === "login"
        ? session.plugin.auth.flow()
        : null;
    }
    return session.flow;
  };

  return {
    listSchools() {
      return EDU_PLUGINS.map(p => ({
        id: p.id,
        name: p.name,
        system: p.system,
        directLogin: hasDirectLogin(p),
      }));
    },

    async prepareLogin(schoolId) {
      const plugin = resolve(schoolId);
      const session: SchoolSession = { plugin, flow: null };
      sessions.set(plugin.id, session);
      if (!hasDirectLogin(plugin))
        return { kind: "unavailable", message: "该学校暂不支持应用内登录，请使用内置浏览器" };
      const flow = liveFlow(session);
      if (!flow)
        return { kind: "unavailable", message: "该学校暂不支持应用内登录" };
      try {
        const begin = await flow.begin(http, url => cookies.read(url));
        if (begin.kind === "ready") {
          await cookies.write(begin.jars);
          return { kind: "ready", url: begin.url };
        }
        return { kind: "form", captcha: begin.captcha };
      } catch (e) {
        session.flow = null;
        return {
          kind: "unavailable",
          message: e instanceof Error
            ? e.message
            : "登录页没有就绪",
        };
      }
    },

    async refreshCaptcha(schoolId) {
      const plugin = resolve(schoolId);
      const flow = sessions.get(plugin.id)?.flow;
      if (!flow?.refreshCaptcha)
        throw new Error("登录还没准备好");
      const captcha = await flow.refreshCaptcha(http);
      if (!captcha)
        throw new Error("拿不到验证码");
      return captcha;
    },

    async login(schoolId, credentials: EduCredentialsInput) {
      const plugin = resolve(schoolId);
      const flow = sessions.get(plugin.id)?.flow;
      if (!flow)
        return { kind: "fail", message: "请先开始登录" };
      try {
        const outcome = await flow.login(http, credentials satisfies EduCredentials);
        if (outcome.kind === "fail")
          return { kind: "fail", message: outcome.message, captcha: outcome.captcha };
        await cookies.write(outcome.jars);
        return { kind: "ok", url: outcome.url };
      } catch (e) {
        sessions.delete(plugin.id);
        return {
          kind: "fail",
          message: e instanceof Error
            ? e.message
            : "登录失败，请重试",
        };
      }
    },

    async importTimetable(schoolId) {
      const plugin = resolve(schoolId);
      const session = sessions.get(plugin.id);
      const flow = session?.flow ?? null;
      if (!flow?.fetchTimetable)
        return { kind: "fail", message: "需要先登录" };
      try {
        const kb = await flow.fetchTimetable(http);
        if (!kb)
          return { kind: "fail", message: "课表获取失败，请重新登录" };
        sessions.delete(plugin.id);
        return {
          kind: "ok",
          timetable: toSnapshot(plugin, kb),
          diagnostics: kb.out.diagnostics,
        };
      } catch {
        return { kind: "fail", message: "课表获取失败，请重新登录" };
      }
    },

    async logout(schoolId) {
      const plugin = resolve(schoolId);
      sessions.delete(plugin.id);
      await cookies.clear(plugin.sessionUrls);
    },
  };
}

/** 学校插件的解析输出 → 给 UI 的领域模型 */
function toSnapshot(plugin: EduPlugin, kb: EduKbFetch): TimetableSnapshot {
  const { out, term } = kb;
  return {
    schoolId: plugin.id,
    term,
    semester: out.semester,
    timeGrid: out.timeGrid,
    courses: out.courses.map((c) => {
      const course = {
        name: c.name,
        teacher: c.teacher,
        location: c.location,
        weekday: c.weekday,
        startPeriod: c.startPeriod,
        endPeriod: c.endPeriod,
      };
      return {
        ...course,
        id: courseIdOf(course),
        weeks: parseWeeksString(c.weeks).weeks,
        source: { pluginId: plugin.id, ...c.raw },
      };
    }),
    studentName: kb.studentName || undefined,
    importedAt: new Date().toISOString(),
  };
}
