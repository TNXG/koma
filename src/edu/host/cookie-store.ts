/**
 * 会话 Cookie 存取：插件 begin 时读取（探测已有会话）、ok 时写回（下次免密）。
 * 原生平台由 EduNativeHttp 插件持久化（Keystore 加密存私有目录，按 host 索引）；
 * Web/测试用内存实现，接口不变。
 */

import type { EduCookieJar } from "../contracts/http";
import { Capacitor } from "@capacitor/core";
import { EduNativeHttp } from "./native-http";

export interface EduCookieStore {
  /** "k=v; k2=v2" 或 null */
  read: (url: string) => Promise<string | null>;
  write: (jars: EduCookieJar[]) => Promise<void>;
  clear: (urls: string[]) => Promise<void>;
}

export function createMemoryCookieStore(): EduCookieStore {
  const jars = new Map<string, Map<string, string>>();
  return {
    async read(url) {
      return jars.get(url)
        ? [
            ...jars.get(url)!.entries(),
          ].map(([k, v]) => `${k}=${v}`).join("; ")
        : null;
    },
    async write(incoming) {
      for (const jar of incoming) {
        const m = jars.get(jar.url) ?? new Map<string, string>();
        for (const cookie of jar.cookies) {
          const at = cookie.indexOf("=");
          if (at <= 0)
            continue;
          m.set(cookie.slice(0, at).trim(), cookie.slice(at + 1).trim());
        }
        jars.set(jar.url, m);
      }
    },
    async clear(urls) {
      for (const url of urls)
        jars.delete(url);
    },
  };
}

/** Android 原生实现：Capacitor 插件持有持久 Cookie，WebView 侧只是镜像 */
export function createNativeCookieStore(): EduCookieStore {
  return {
    async read(url) {
      return (await EduNativeHttp.cookiesFor({ url })).value;
    },
    async write(jars) {
      await EduNativeHttp.storeCookies({ jars });
    },
    async clear(urls) {
      await EduNativeHttp.clearCookies({ urls });
    },
  };
}

/** 平台自动选择：原生持久化，Web/单测用内存 */
export function createDefaultCookieStore(): EduCookieStore {
  return Capacitor.isNativePlatform()
    ? createNativeCookieStore()
    : createMemoryCookieStore();
}
