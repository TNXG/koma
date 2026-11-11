import { createRouter, createWebHashHistory } from "vue-router";
import { useTimetableStore } from "@/stores/timetable-store";

/**
 * Capacitor 内核的本地服务器对 history 回退支持不稳定，先用 hash 模式。
 * 首次进入顺序：登录导入 → 确认开学日期 → 首页。
 */
export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: "/",
      name: "home",
      component: () => import("@/pages/home-page.vue"),
    },
    {
      path: "/login",
      name: "login",
      component: () => import("@/pages/login-page.vue"),
    },
    {
      path: "/semester",
      name: "semester",
      component: () => import("@/pages/semester-page.vue"),
    },
  ],
});

router.beforeEach((to) => {
  const timetable = useTimetableStore();
  if (to.name === "login")
    return true;
  if (!timetable.snapshot)
    return { name: "login" };
  if (to.name === "home" && !timetable.startDate)
    return { name: "semester" };
  return true;
});
