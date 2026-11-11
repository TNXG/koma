/**
 * xjvut 登录会话的 HTTP 管道：主机 Cookie 瓶、单请求、逐跳跟重定向。
 * 状态（瓶 + CAS 公钥）封闭在实例里，一个实例承载一次登录会话（docs/edu-auth README §2.5）。
 * 瓶里只收 qyrz 与 jw:6082 两台主机的 Set-Cookie，其余主机（SSO 跳过去的业务页）由浏览器 SSO 自建。
 */

import type { EduCookieJar, EduHttp, EduHttpRequest, EduHttpResult } from "../../contracts/http";
import { applySetCookie, cookieHeaderValue, parseCookieHeader } from "../../cas/cas";

export const CAS_ORIGIN = "https://qyrz.xjvut.edu.cn";
export const CAS_LOGIN = `${CAS_ORIGIN}/cas/login`;
export const JW_ORIGIN = "https://jw.xjvut.edu.cn:6082";
/** 教务 SSO 入口：移动端学生课表页；Y253510 = 移动端课表模块 */
export const SSO_JUMP = `${JW_ORIGIN}/sso/zfiotlogin?url=kbcx/xskbcxMobile_cxXskbcxIndex.html?gnmkdm=Y253510&layout=default`;
/** 移动端个人课表数据接口：POST 返回 JSON */
export const KB_URL = `${JW_ORIGIN}/jwglxt/kbcx/xskbcxMobile_cxXsKb.html`;
/** 学校公布的每周起止日期；首周周一和周数用于正确映射 zcd */
export const WEEKS_URL = `${JW_ORIGIN}/jwglxt/kbcx/xskbcxMobile_cxZc.html`;
/** SSO 成功的落点：移动端课表索引页 */
export const INDEX_RE = /xskbcxMobile_cxXskbcxIndex\.html/;
/** 作息接口：POST xnm/xqm → 每节 jcmc/qssj/jssj（"10:00 - 10:45"） */
export const RJC_URL = `${JW_ORIGIN}/jwglxt/kbcx/xskbcx_cxRjc.html?gnmkdm=N2151`;

const UA = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36";
const HOST_ORIGIN: Record<string, string> = {
  [new URL(CAS_ORIGIN).host]: CAS_ORIGIN,
  [new URL(JW_ORIGIN).host]: JW_ORIGIN,
};

export const hostOf = (url: string): string => new URL(url).host;

/** 大小写无关地取响应头（Set-Cookie、Location 等） */
export function pick(headers: Record<string, string[]>, name: string): string[] {
  const key = Object.keys(headers).find(k => k.toLowerCase() === name);
  return key
    ? headers[key]
    : [];
}

export interface CasPubKey {
  execution: string;
  modulus: string;
  exponent: string;
}

export class XjvutSession {
  /** 同一次登录的瓶：CAS 放 TGT，教务放 JSESSIONID/route */
  private jars = new Map<string, Map<string, string>>();
  /** CAS 登录页的 execution + 公钥；begin 阶段种下，login 时消费 */
  key: CasPubKey | null = null;

  reset(): void {
    this.jars.clear();
    this.key = null;
  }

  private jarFor(host: string): Map<string, string> {
    let m = this.jars.get(host);
    if (!m) {
      m = new Map();
      this.jars.set(host, m);
    }
    return m;
  }

  private ownHost(url: string): boolean {
    return hostOf(url) in HOST_ORIGIN;
  }

  /** begin 探测已有会话：把宿主 CookieManager 里的两台主机 Cookie 拆回瓶 */
  async seedFrom(cookie: (url: string) => Promise<string | null>): Promise<void> {
    for (const [origin, host] of [
      [CAS_ORIGIN, new URL(CAS_ORIGIN).host],
      [JW_ORIGIN, new URL(JW_ORIGIN).host],
    ] as const) {
      const had = parseCookieHeader((await cookie(origin)) ?? "");
      applySetCookie(this.jarFor(host), [...had].map(([k, v]) => `${k}=${v}`));
    }
  }

  /** 单请求：带上瓶里这台主机的 Cookie，收 Set-Cookie（只收自己两台主机的）；UA 对齐手机浏览器 */
  async call(http: EduHttp, req: EduHttpRequest): Promise<EduHttpResult> {
    const jar = this.jarFor(hostOf(req.url));
    const cookieHeader = cookieHeaderValue(jar);
    const r = await http({
      ...req,
      headers: {
        "User-Agent": UA,
        ...(cookieHeader
          ? { Cookie: cookieHeader }
          : {}),
        ...req.headers,
      },
    });
    if (this.ownHost(r.url))
      applySetCookie(this.jarFor(hostOf(r.url)), pick(r.headers, "set-cookie"));
    return r;
  }

  /** 逐跳跟随重定向：第一跳带 init（可能 POST），之后一律 GET；一头一响应才收得到每跳的 Cookie */
  async follow(http: EduHttp, url: string, init?: Omit<EduHttpRequest, "url">): Promise<EduHttpResult> {
    let cur = url;
    for (let hop = 0; ; hop++) {
      const r = await this.call(http, hop === 0 && init
        ? { ...init, url: cur }
        : { url: cur });
      if (r.status < 300 || r.status >= 400)
        return r;
      const loc = pick(r.headers, "location")[0];
      if (!loc || hop >= 6)
        return r;
      cur = new URL(loc, cur).toString();
    }
  }

  /** 把瓶交成宿主要求的形态：每台主机一组 */
  jarsOut(): EduCookieJar[] {
    const out: EduCookieJar[] = [];
    for (const [host, m] of this.jars) {
      if (m.size === 0)
        continue;
      out.push({
        url: HOST_ORIGIN[host],
        cookies: [...m].map(([k, v]) => `${k}=${v}`),
      });
    }
    return out;
  }

  /** 供 WebView 的路径 Cookie：JW 的 JSESSIONID 由 /jwglxt/ticketlogin 签发，只对 /jwglxt 有效 */
  browserJars(): EduCookieJar[] {
    const out = this.jarsOut();
    const jw = out.find(j => j.url === JW_ORIGIN);
    if (!jw)
      return out;
    const jsid = jw.cookies.find(c => /^JSESSIONID=/i.test(c));
    if (!jsid)
      return out;
    jw.cookies = jw.cookies.filter(c => !/^JSESSIONID=/i.test(c));
    out.push({
      url: `${JW_ORIGIN}/jwglxt/`,
      cookies: [`${jsid}; Path=/jwglxt`],
    });
    return out;
  }
}
