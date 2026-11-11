import { fileURLToPath, URL } from "node:url";
import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [
    vue({
      features: { vapor: true },
    }),
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
