/**
 * EduHttp 的平台出口：原生走 Kotlin 插件（登录全程可逐跳收 Set-Cookie，生产链路）；
 * Web 走 fetch 适配器（仅开发调试——浏览器拿不到 Set-Cookie、不能手动跟重定向，
 * 登录链路在 Web 端不完整，属于预期行为）。
 */

import type { EduHttp, EduHttpRequest, EduHttpResult } from "../contracts/http";
import { Capacitor } from "@capacitor/core";
import { EduNativeHttp } from "./native-http";

const nativeEduHttp: EduHttp = req => EduNativeHttp.request(req);

const webEduHttp: EduHttp = async (req: EduHttpRequest): Promise<EduHttpResult> => {
  const res = await fetch(req.url, {
    method: req.method ?? "GET",
    headers: req.headers,
    body: req.body,
    /* 浏览器会自动跟重定向；这里只能拿到最终一跳 */
    redirect: "follow",
  });
  const headers: Record<string, string[]> = {};
  res.headers.forEach((value, key) => {
    (headers[key] ??= []).push(value);
  });
  const body = req.binary
    ? arrayBufferToBase64(await res.arrayBuffer())
    : await res.text();
  return {
    status: res.status,
    url: res.url || req.url,
    headers,
    body,
  };
};

function arrayBufferToBase64(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = "";
  for (let i = 0; i < bytes.length; i += 0x8000)
    binary += String.fromCharCode(...bytes.subarray(i, i + 0x8000));
  return btoa(binary);
}

export function createEduHttp(): EduHttp {
  return Capacitor.isNativePlatform()
    ? nativeEduHttp
    : webEduHttp;
}
