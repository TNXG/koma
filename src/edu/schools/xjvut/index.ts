/**
 * 新疆理工职业大学插件（docs/edu-auth/README.md）：
 * CAS 直登 + 教务（正方新版）原生取课表，登录方式为应用内直登页。
 */

import type { EduPlugin } from "../../contracts/plugin";
import { createXjvutFlow } from "./flow";
import { CAS_LOGIN, CAS_ORIGIN, JW_ORIGIN } from "./session";

export const xjvut: EduPlugin = {
  id: "xjvut",
  name: "新疆理工职业大学 统一身份认证（CAS）",
  url: CAS_LOGIN,
  system: "zhengfang_new",
  auth: { kind: "login", flow: createXjvutFlow },
  sessionUrls: [CAS_ORIGIN, JW_ORIGIN, `${JW_ORIGIN}/jwglxt/`],
};
