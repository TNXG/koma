import { createPinia } from "pinia";
import { createApp } from "vue";
import App from "@/App.vue";
import { router } from "./router";
import "./global.scss";

/* Capacitor 阶段引入原生桥；当前仅保证 Web 基础工程可运行 */

createApp(App)
  .use(createPinia())
  .use(router)
  .mount("#app");
