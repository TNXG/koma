import { createRouter, createWebHashHistory } from "vue-router";

/**
 * Capacitor 内核的本地服务器对 history 回退支持不稳定，先用 hash 模式；
 * 路由表随页面设计逐步补全，页面组件延迟加载。
 */
export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: "/",
      name: "home",
      component: () => import("@/pages/home-page.vue"),
    },
  ],
});
