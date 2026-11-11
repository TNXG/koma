import { describe, expect, it } from "vitest";
import {
  applySetCookie,
  casErrorText,
  cookieHeaderValue,
  describeCasError,
  encryptCasPassword,
  extractExecution,
  isCaptchaError,
  parseCookieHeader,
} from "@/edu/cas/cas";

describe("cAS 页面解析", () => {
  it("提取 execution 隐藏域", () => {
    const html = `<form><input type="hidden" name="execution" value="e1s3_abc"/></form>`;
    expect(extractExecution(html)).toBe("e1s3_abc");
    expect(extractExecution("<form></form>")).toBeNull();
  });

  it("从 error 元素取失败原文", () => {
    const html = `<span class="errors auth_error">FailedAuthcodeException: 验证码错误</span>`;
    expect(casErrorText(html)).toBe("FailedAuthcodeException: 验证码错误");
    expect(casErrorText("<div>ok</div>")).toBe("");
  });

  it("错误文案翻译", () => {
    expect(isCaptchaError("FailedAuthcodeException")).toBe(true);
    expect(describeCasError("FailedAuthcodeException")).toBe("验证码不对");
    expect(describeCasError("password not recognized")).toBe("账号或密码不对");
    expect(describeCasError("Invalid execution flow")).toBe("登录过期了，请重试");
    expect(describeCasError("")).toBe("登录失败，请重试");
    expect(describeCasError("x".repeat(80))).toBe("登录失败，请重试");
  });
});

describe("cookie 瓶", () => {
  it("收 Set-Cookie 只取 name=value", () => {
    const jar = new Map<string, string>();
    applySetCookie(jar, ["TGT=abc; Path=/cas; HttpOnly", "非法", "JSESSIONID=js1; Path=/jwglxt"]);
    expect(Object.fromEntries(jar)).toEqual({ TGT: "abc", JSESSIONID: "js1" });
  });

  it("拼请求头；空瓶给 undefined", () => {
    const jar = new Map([
      ["a", "1"],
      ["b", "2"],
    ]);
    expect(cookieHeaderValue(jar)).toBe("a=1; b=2");
    expect(cookieHeaderValue(new Map())).toBeUndefined();
  });

  it("拆回 Cookie 头", () => {
    const jar = parseCookieHeader("a=1; b=2");
    expect(Object.fromEntries(jar)).toEqual({ a: "1", b: "2" });
  });
});

describe("encryptCasPassword", () => {
  const modulus = "f".repeat(128); // 512 位密钥 → 每块 62 字节

  it("输出为空格分隔、长度对齐 4 位边界的 hex 块", () => {
    const out = encryptCasPassword("test1234", modulus, "10001");
    const blocks = out.split(" ");
    expect(blocks.length).toBeGreaterThan(0);
    for (const b of blocks) {
      expect(b).toMatch(/^[0-9a-f]+$/);
      expect(b.length % 4).toBe(0);
    }
  });

  it("同输入输出一致；块数随密码长度按 62 字节进位", () => {
    expect(encryptCasPassword("test1234", modulus, "10001"))
      .toBe(encryptCasPassword("test1234", modulus, "10001"));
    const oneBlock = encryptCasPassword("a", modulus, "10001").split(" ").length;
    const twoBlocks = encryptCasPassword("x".repeat(63), modulus, "10001").split(" ").length;
    expect(oneBlock).toBe(1);
    expect(twoBlocks).toBe(2);
  });

  it("公钥过短时抛错", () => {
    expect(() => encryptCasPassword("pw", "ff", "10001")).toThrow();
  });
});
