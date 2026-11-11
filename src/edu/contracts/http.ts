/**
 * 教务插件对宿主 HTTP 能力的要求：一头一响应、不自动跟重定向，
 * 才能逐跳收到每一跳的 Set-Cookie。实现方：Android 原生（Kotlin EduNativeHttpPlugin）
 * 与 Web fetch 适配器（仅开发调试）。
 */

/** 单次 HTTP 请求；binary=true 时结果 body 返回 base64（验证码图片用） */
export interface EduHttpRequest {
  url: string;
  method?: string;
  headers?: Record<string, string>;
  body?: string;
  binary?: boolean;
}

/** 一头一响应：headers 保留服务端大小写与多值（Set-Cookie 一组多个） */
export interface EduHttpResult {
  status: number;
  /** 最终落点 URL（未跟重定向时即请求 URL） */
  url: string;
  headers: Record<string, string[]>;
  body: string;
}

export type EduHttp = (req: EduHttpRequest) => Promise<EduHttpResult>;

/** 一组同源 Cookie（name=value 串），宿主按 url 种进学校独立 Profile */
export interface EduCookieJar {
  url: string;
  cookies: string[];
}
