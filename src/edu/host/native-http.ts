/**
 * 原生 HTTP 插件的 TS 侧契约（Kotlin 实现见 docs/architecture.md §3.2/§7）：
 * android/app/src/main/java/com/koma/timetable/plugins/EduNativeHttpPlugin.kt
 * 必须按这些方法名与参数一一对齐；不跟重定向、保留响应头大小写、Set-Cookie 多值成组。
 */

import type { EduCookieJar, EduHttpRequest, EduHttpResult } from "../contracts/http";
import { registerPlugin } from "@capacitor/core";

export interface EduNativeHttpPlugin {
  /** 单次请求；binary=true 时 body 为 base64 */
  request: (options: EduHttpRequest) => Promise<EduHttpResult>;
  /** 读指定 url 的 Cookie 头（"k=v; k2=v2"），无则 null */
  cookiesFor: (options: { url: string }) => Promise<{ value: string | null }>;
  /** 把各主机的会话 Cookie 种进学校独立 Profile */
  storeCookies: (options: { jars: EduCookieJar[] }) => Promise<void>;
  /** 退出登录/清除数据 */
  clearCookies: (options: { urls: string[] }) => Promise<void>;
}

export const EduNativeHttp = registerPlugin<EduNativeHttpPlugin>("EduNativeHttp");
