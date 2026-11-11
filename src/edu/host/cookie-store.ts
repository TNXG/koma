/**
 * 会话 Cookie 存取：插件 begin 时读取（探测已有会话）、ok 时写回（下次免密）。
 * 当前是内存实现（进程内有效）；Android 阶段由 EduNativeHttpPlugin/CookieManager 提供持久实现，
 * 本接口不变。
 */

import type { EduCookieJar } from "../contracts/http";

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
