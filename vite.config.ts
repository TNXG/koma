import { fileURLToPath, URL } from "node:url";
import tailwindcss from "@tailwindcss/vite";
import vue from "@vitejs/plugin-vue";
import Icons from "unplugin-icons/vite";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [
    tailwindcss(),
    /* Vapor 按文件 opt-in（<script setup vapor>）：shadcn-vue / reka-ui 等
       第三方组件依赖 VDOM 的组件实例，不能被强制编译成 Vapor。 */
    vue(),
    /* ~icons/mingcute/* 在构建时编译成 VDOM 组件；根 app 已装 vaporInteropPlugin，
       Vapor 组件里的这层 VDOM 图标靠 interop 挂载。 */
    Icons({ compiler: "vue3", scale: 1 }),
  ],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  server: { host: "127.0.0.1" },
  test: {
    environment: "node",
    include: ["tests/**/*.spec.ts"],
  },
});
