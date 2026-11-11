import type { EduHttp, EduHttpRequest, EduHttpResult } from "@/edu/contracts/http";

/**
 * Mock 的 CAS + 正方教务服务器（docs/edu-auth/README.md 端点表）：
 * 维护 CAS TGT 与教务 JSESSIONID 两瓶 Cookie、可轮换的 execution、
 * 20 周的周次表与逐周课表 JSON，用于插件全链路集成测试。
 */

export const MOCK_USERNAME = "20250001";
export const MOCK_CAPTCHA = "8888";
export const MOCK_STUDENT_NAME = "张三";
const CAS = "https://qyrz.xjvut.edu.cn";
const JW = "https://jw.xjvut.edu.cn:6082";
const INDEX_URL = `${JW}/kbcx/xskbcxMobile_cxXskbcxIndex.html?gnmkdm=Y253510&layout=default`;
const MODULUS = "f".repeat(128);
const DAY_MS = 24 * 60 * 60 * 1000;

export interface MockEduServer {
  http: EduHttp;
  requests: { url: string; method: string; cookie?: string }[];
}

export interface MockEduOptions {
  /** 本次登录是否需要验证码（默认需要） */
  captchaRequired?: boolean;
  /** 正确的验证码答案（默认 8888） */
  correctCaptcha?: string;
  /** 模拟服务端拒绝该密码（账号密码错误） */
  passwordRejected?: boolean;
}

export function createMockEduServer(options: MockEduOptions = {}): MockEduServer {
  const captchaRequired = options.captchaRequired ?? true;
  const correctCaptcha = options.correctCaptcha ?? MOCK_CAPTCHA;
  let execution = "e-1";
  let executionSeq = 1;
  const requests: MockEduServer["requests"] = [];

  const newExecution = () => `e-${++executionSeq}`;

  const loginPage = (errorText?: string) => `
<html><body><form>
<input type="hidden" name="execution" value="${execution}"/>
${errorText
  ? `<span class="errors auth_error">${errorText}</span>`
  : ""}
</form></body></html>`;

  const indexPage = () => `
<html><body>
<input type="hidden" id="xnm_hide" value="2025"/>
<input type="hidden" id="xqm_hide" value="3"/>
</body></html>`;

  const redirect = (url: string, location: string, setCookies: string[] = []): EduHttpResult => ({
    status: 302,
    url,
    headers: {
      "Location": [location],
      "Set-Cookie": setCookies,
    },
    body: "",
  });

  const ok = (url: string, body: string, headers: Record<string, string[]> = {}): EduHttpResult => ({
    status: 200,
    url,
    headers,
    body,
  });

  const cookiesOf = (req: EduHttpRequest): Map<string, string> => {
    const m = new Map<string, string>();
    for (const pair of (req.headers?.Cookie ?? "").split(";")) {
      const at = pair.indexOf("=");
      if (at > 0)
        m.set(pair.slice(0, at).trim(), pair.slice(at + 1).trim());
    }
    return m;
  };

  const bodyOf = (req: EduHttpRequest): URLSearchParams => new URLSearchParams(req.body ?? "");

  /** 学校公布的周次表：2025-09-01（周一）起 20 周 */
  const weekRows = () => {
    const start = Date.UTC(2025, 8, 1);
    const iso = (t: number) => new Date(t).toISOString().slice(0, 10);
    return Array.from({ length: 20 }, (_, i) => ({
      zs: i + 1,
      rq: `${iso(start + i * 7 * DAY_MS)}/${iso(start + (i * 7 + 6) * DAY_MS)}`,
    }));
  };

  /** 单周课表。zcd 故意给 "1"：正确实现必须用查询周号覆盖它 */
  const kbList = (zs: number) => {
    const list: Record<string, unknown>[] = [
      {
        kcmc: "高等数学",
        xm: "李四",
        cdmc: "3-201",
        xqj: "1",
        jcs: "3-4",
        zcd: "1",
      },
    ];
    if (zs <= 16) {
      list.push({
        kcmc: "大学英语",
        xm: "王五",
        cdmc: "2-101",
        xqj: "3",
        jcs: "0304",
        zcd: "1",
      });
    }
    if (zs === 1) {
      list.push({
        kcmc: " ",
        xm: "赵六",
        cdmc: "1-1",
        xqj: "2",
        jcs: "5",
        zcd: "1",
      });
      list.push({
        kcmc: "体育",
        xm: "钱七",
        cdmc: "操场",
        xqj: "9",
        jcs: "5",
        zcd: "1",
      });
    }
    return list;
  };

  const rjcRows = () => [
    { jcmc: "第1节", qssj: "08:00 - 08:45" },
    { jcmc: "第2节", qssj: "08:55 - 09:40" },
    { jcmc: "第3节", qssj: "10:00", jssj: "10:45" },
    { jcmc: "第4节", qssj: "不是时间" },
  ];

  const handle = (req: EduHttpRequest): EduHttpResult => {
    const u = new URL(req.url);
    const method = req.method ?? "GET";

    if (u.host === new URL(CAS).host) {
      const casTgt = cookiesOf(req).get("CASTGT");
      if (u.pathname === "/cas/login") {
        if (method === "GET") {
          /* 标准CAS：带 service 且 TGT 有效 → 回跳 service 附 ticket；否则渲染登录页 */
          const service = u.searchParams.get("service");
          if (service && casTgt === "TGT-1") {
            return redirect(req.url, `${service}${service.includes("?")
              ? "&"
              : "?"}ticket=ST-2`);
          }
          if (u.searchParams.has("ticket") && casTgt === "TGT-1")
            return ok(req.url, "<html><body><div>登录成功</div></body></html>");
          execution = newExecution();
          return ok(req.url, loginPage());
        }
        const form = bodyOf(req);
        if (options.passwordRejected) {
          execution = newExecution();
          return ok(req.url, loginPage("账号或密码不正确, not recognized"));
        }
        const valid = form.get("username") === MOCK_USERNAME
          && form.get("execution") === execution
          && /^[0-9a-f]+(?: [0-9a-f]+)*$/.test(form.get("password") ?? "")
          && (!captchaRequired || form.get("authcode") === correctCaptcha);
        execution = newExecution();
        if (!valid) {
          const wrongCaptcha = captchaRequired && form.get("authcode") !== correctCaptcha;
          return ok(req.url, loginPage(wrongCaptcha ? "FailedAuthcodeException: 验证码错误" : "账号或密码不正确, not recognized"));
        }
        return redirect(req.url, `${CAS}/cas/login?ticket=ST-1`, ["CASTGT=TGT-1; Path=/cas"]);
      }
      if (u.pathname === "/cas/v1/getPubKey")
        return ok(req.url, JSON.stringify({ modulus: MODULUS, exponent: "10001" }));
      if (u.pathname === "/cas/getKaptchaStatus")
        return ok(req.url, JSON.stringify({ isNeedImgCode: captchaRequired }));
      if (u.pathname === "/cas/kaptcha") {
        return ok(req.url, "a2FwdGNoYQ==", {
          "Content-Type": ["image/png"],
        });
      }
    }

    if (u.host === new URL(JW).host) {
      /* 教务接口的会话以请求头里的 JSESSIONID 为准 */
      const jsid = cookiesOf(req).get("JSESSIONID");
      if (u.pathname === "/sso/zfiotlogin") {
        /* 教务 SSO：无票 → 跳 CAS 质询（service 是 sso 自己，CAS 凭 TGT 回跳带票）；
           有票 → 换教务会话（ticketlogin 签发 JSESSIONID） */
        if (u.searchParams.has("ticket"))
          return redirect(req.url, `${JW}/jwglxt/ticketlogin?ticket=ST-2`, ["route=route-1; Path=/"]);
        const service = `${JW}/sso/zfiotlogin?url=${encodeURIComponent("kbcx/xskbcxMobile_cxXskbcxIndex.html?gnmkdm=Y253510&layout=default")}`;
        return redirect(req.url, `${CAS}/cas/login?service=${encodeURIComponent(service)}`);
      }
      if (u.pathname === "/jwglxt/ticketlogin") {
        if (!u.searchParams.has("ticket"))
          return redirect(req.url, `${CAS}/cas/login`);
        return redirect(req.url, INDEX_URL, ["JSESSIONID=js-42; Path=/jwglxt"]);
      }
      if (u.pathname === "/kbcx/xskbcxMobile_cxXskbcxIndex.html") {
        return jsid
          ? ok(req.url, indexPage())
          : redirect(req.url, `${CAS}/cas/login`);
      }
      if (!jsid)
        return redirect(req.url, `${CAS}/cas/login`);
      if (u.pathname === "/jwglxt/kbcx/xskbcxMobile_cxZc.html")
        return ok(req.url, JSON.stringify(weekRows()));
      if (u.pathname === "/jwglxt/kbcx/xskbcxMobile_cxXsKb.html") {
        const zs = Number(bodyOf(req).get("zs"));
        return ok(req.url, JSON.stringify({
          xsxx: { XM: MOCK_STUDENT_NAME },
          kbList: kbList(zs),
        }));
      }
      if (u.pathname === "/jwglxt/kbcx/xskbcx_cxRjc.html")
        return ok(req.url, JSON.stringify(rjcRows()));
    }

    return ok(req.url, "<html><body>404</body></html>");
  };

  const http: EduHttp = async (req) => {
    const cookies = cookiesOf(req);
    requests.push({
      url: req.url,
      method: req.method ?? "GET",
      cookie: cookies.size
        ? [
            ...cookies.entries(),
          ].map(([k, v]) => `${k}=${v}`).join("; ")
        : undefined,
    });
    return handle(req);
  };

  return { http, requests };
}
