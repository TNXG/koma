/**
 * 兜底声明：仅供不加载 Vue 语言服务的 TypeScript（plain tsc、部分编辑器的内置 TS server）解析 SFC。
 * 类型检查请以 `pnpm typecheck`（vue-tsc）为准，它会按真实 SFC 出类型。
 */
declare module "*.vue" {
  import type { DefineComponent } from "vue";

  const component: DefineComponent;
  export default component;
}
