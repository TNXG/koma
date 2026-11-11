import { createPinia } from "pinia";
import { createApp, vaporInteropPlugin } from "vue";
import App from "@/App.vue";
import { router } from "./router";
import "./tailwind.css";
import "./global.scss";

/* 根用 VDOM；自带组件按 <script setup vapor> 编译，interop 插件让两者互嵌 */
createApp(App)
  .use(vaporInteropPlugin)
  .use(createPinia())
  .use(router)
  .mount("#app");
