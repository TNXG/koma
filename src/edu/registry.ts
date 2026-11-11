/**
 * 已接入的学校注册表：当前只为新疆理工职业大学负责。
 * 新学校加一个 schools/<id>/ 并在这里登记即可，业务与 UI 无需改动。
 */

import type { EduPlugin } from "./contracts/plugin";
import { hasDirectLogin } from "./contracts/plugin";
import { xjvut } from "./schools/xjvut";

export const EDU_PLUGINS: EduPlugin[] = [xjvut];

/** 默认学校：首页「导入课表」直达它的登录页 */
export const DEFAULT_EDU_PLUGIN: EduPlugin = EDU_PLUGINS[0];

export function getEduPlugin(id: string): EduPlugin | null {
  return EDU_PLUGINS.find(p => p.id === id) ?? null;
}

export { hasDirectLogin };
