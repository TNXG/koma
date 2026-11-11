<script setup lang="ts" vapor>
import { useEventListener, useIntervalFn, useNow } from "@vueuse/core";
import { computed, nextTick, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import DateSheet from "@/components/common/date-sheet.vue";
import DaySection from "@/components/home/day-section.vue";
import FreeBlock from "@/components/home/free-block.vue";
import HomeDock from "@/components/home/home-dock.vue";
import HomeHead from "@/components/home/home-head.vue";
import MeDrawer from "@/components/home/me-drawer.vue";
import WeekDivider from "@/components/home/week-divider.vue";
import {
  addDays,
  daysBetween,
  minutesOfDay,
  parseLocalDate,
  sameDay,
  startOfDay,
  toIsoDate,
} from "@/domain/calendar";
import {
  blockAnchors,
  buildTimeline,
  countCoursesByDay,
  describeDay,
} from "@/domain/home-timeline";
import { useEduStore } from "@/stores/edu-store";
import { useTimetableStore } from "@/stores/timetable-store";

type LayerRole = "cur" | "inc";

const router = useRouter();
const timetable = useTimetableStore();
const edu = useEduStore();
const snapshot = timetable.snapshot!;
const now = useNow({
  scheduler: cb => useIntervalFn(cb, 30_000),
});

const today = computed(() => startOfDay(now.value));
const nowMinutes = computed(() => minutesOfDay(now.value));
const termStart = parseLocalDate(timetable.startDate);
const termEnd = addDays(termStart, snapshot.semester.totalWeeks * 7 - 1);
const totalDays = computed(() => daysBetween(termStart, termEnd) + 1);

const blocks = computed(() => buildTimeline(snapshot, timetable.startDate, today.value, nowMinutes.value));
const blockAnchorByDay = computed(() => blockAnchors(blocks.value));
const courseCount = computed(() => countCoursesByDay(blocks.value));

/* ---------------- 滚动与选中 ---------------- */

const scroller = ref<HTMLElement>();
const head = computed(() => scroller.value?.querySelector<HTMLElement>(".head") ?? null);
const titleStack = computed(() => scroller.value?.querySelector<HTMLElement>(".head__stack") ?? null);
const headHeight = () => head.value?.offsetHeight ?? 92;
/** 区块的停靠位置：日期行正好并入顶部标题，课程卡紧贴标题下方 */
const dockTop = (el: HTMLElement) => el.offsetTop - headHeight() + (el.querySelector<HTMLElement>(".day__head")?.offsetHeight ?? 0);
const view = ref(toIsoDate(today.value));
const veil = ref(0);
/** 下一个区块并入顶部标题的进度 0→1；docks 表示它带日期标题，会飞进大标题 */
const morph = ref(0);
const incoming = ref<{ anchor: string; docks: boolean } | null>(null);
const gliding = ref(false);
let glideTimer: ReturnType<typeof setTimeout> | undefined;

const MORPH_DISTANCE = 72;

const clamp01 = (v: number) => Math.min(1, Math.max(0, v));

function smooth(from: number, to: number, v: number): number {
  const t = clamp01((v - from) / (to - from));
  return t * t * (3 - 2 * t);
}

const viewDate = computed(() => parseLocalDate(view.value));
const viewIsToday = computed(() => sameDay(viewDate.value, today.value));

const headLayers = computed(() => {
  const layers = [
    {
      iso: view.value,
      role: "cur" as LayerRole,
      date: viewDate.value,
      ...describeDay(snapshot, timetable.startDate, viewDate.value, today.value, nowMinutes.value),
    },
  ];
  if (incoming.value) {
    const date = parseLocalDate(incoming.value.anchor);
    layers.push({
      iso: incoming.value.anchor,
      role: "inc" as LayerRole,
      date,
      ...describeDay(snapshot, timetable.startDate, date, today.value, nowMinutes.value),
    });
  }
  return layers;
});

function elementOf(anchor: string): HTMLElement | null {
  return scroller.value?.querySelector<HTMLElement>(`[data-anchor="${anchor}"]`) ?? null;
}

function scrollToBlock(anchor: string): void {
  const el = elementOf(anchor);
  const box = scroller.value;
  if (!el || !box)
    return;
  gliding.value = true;
  update();
  clearTimeout(glideTimer);
  glideTimer = setTimeout(endGlide, 700);
  box.scrollTo({
    top: dockTop(el),
    behavior: "smooth",
  });
}

function endGlide(): void {
  if (!gliding.value)
    return;
  gliding.value = false;
  update();
}

/* 日期标题飞入顶部：每帧直接写 style，不走响应式 */
const painted = new Set<HTMLElement>();

function resetRow(row: HTMLElement): void {
  row.style.cssText = "";
  for (const el of row.querySelectorAll<HTMLElement>("[data-morph], [data-fade]"))
    el.style.cssText = "";
}

function morphRow(row: HTMLElement, stack: HTMLElement, p: number): void {
  const sub = row.querySelector<HTMLElement>("[data-morph]")!;
  const title = stack.querySelector<HTMLElement>(".head__title")!;
  const ratio = Number.parseFloat(getComputedStyle(title).fontSize) / Number.parseFloat(getComputedStyle(sub).fontSize);
  const e = p < 0.5
    ? 2 * p * p
    : 1 - (-2 * p + 2) ** 2 / 2;
  const rowBox = row.getBoundingClientRect();
  const stackBox = stack.getBoundingClientRect();
  const dx = stackBox.left - (rowBox.left + sub.offsetLeft);
  const dy = stackBox.top + title.offsetHeight / 2 - (rowBox.top + sub.offsetTop + sub.offsetHeight / 2);
  row.style.visibility = "";
  row.style.zIndex = "6";
  sub.style.transformOrigin = "0 50%";
  sub.style.transform = `translate3d(${dx * e}px, ${dy * e}px, 0) scale(${1 + (ratio - 1) * e})`;
  sub.style.opacity = String(1 - smooth(0.82, 1, p));
  if (!sub.classList.contains("day__date--lead"))
    sub.style.color = `color-mix(in srgb, var(--c-ink) ${Math.round(e * 100)}%, var(--c-ink3))`;
  for (const el of row.querySelectorAll<HTMLElement>("[data-fade]")) {
    el.style.opacity = String(1 - clamp01(p * 2.4));
    el.style.transform = `translateY(${-8 * e}px)`;
  }
}

let frame = 0;

function update(): void {
  frame = 0;
  const box = scroller.value;
  const headEl = head.value;
  const stack = titleStack.value;
  if (!box || !headEl || !stack)
    return;
  const scrollTop = box.scrollTop;
  const t = clamp01(scrollTop / 24);
  veil.value = 1 - (1 - t) * (1 - t);

  const dock = headEl.offsetHeight + 1;
  const keep = new Set<HTMLElement>();
  let current: HTMLElement | null = null;
  let next: HTMLElement | null = null;
  for (const el of box.querySelectorAll<HTMLElement>("[data-anchor]")) {
    const top = el.offsetTop - scrollTop;
    if (top > dock) {
      next = el;
      break;
    }
    current = el;
    /* 已经并入顶部的日期行保持隐藏，直到区块整个离开屏幕，否则会在羽化层下面闪回原位 */
    const row = top + el.offsetHeight > -dock
      ? el.querySelector<HTMLElement>(".day__head")
      : null;
    if (row && !gliding.value) {
      if (!painted.has(row))
        resetRow(row);
      row.style.visibility = "hidden";
      keep.add(row);
    }
  }

  const settled = !gliding.value;
  if (settled && current && blockAnchorByDay.value.get(view.value) !== current.dataset.anchor)
    view.value = current.dataset.anchor!;
  const p = settled && current && next
    ? clamp01((dock + MORPH_DISTANCE - (next.offsetTop - scrollTop)) / MORPH_DISTANCE)
    : 0;
  const nextRow = next?.querySelector<HTMLElement>(".day__head") ?? null;
  morph.value = p;
  incoming.value = p > 0
    ? { anchor: next!.dataset.anchor!, docks: nextRow !== null }
    : null;

  if (p > 0 && nextRow) {
    morphRow(nextRow, stack, p);
    keep.add(nextRow);
  }
  for (const el of painted) {
    if (!keep.has(el))
      resetRow(el);
  }
  painted.clear();
  keep.forEach(el => painted.add(el));
}

function onScroll(): void {
  frame ||= requestAnimationFrame(update);
}

useEventListener(() => scroller.value, "scroll", onScroll, { passive: true });
useEventListener(() => scroller.value, "scrollend", endGlide);
useEventListener(window, "resize", onScroll);

function pickDay(day: Date): void {
  const clamped = day < termStart
    ? termStart
    : day > termEnd
      ? termEnd
      : day;
  const iso = toIsoDate(clamped);
  if (iso !== view.value) {
    titleStack.value?.animate(
      [
        { opacity: 0, transform: "translateY(10px)", filter: "blur(4px)" },
        { opacity: 1, transform: "none", filter: "none" },
      ],
      { duration: 320, easing: "cubic-bezier(0.22, 0.9, 0.24, 1)" },
    );
  }
  view.value = iso;
  scrollToBlock(blockAnchorByDay.value.get(iso) ?? iso);
}

const stripDays = computed(() => Array.from({ length: totalDays.value }, (_, i) => addDays(termStart, i)));
const recenter = ref(0);

function jump(day: Date): void {
  pickDay(day);
  recenter.value++;
}

onMounted(async () => {
  await nextTick();
  const el = elementOf(blockAnchorByDay.value.get(view.value) ?? view.value);
  if (el && scroller.value)
    scroller.value.scrollTop = Math.max(0, dockTop(el));
  update();
});

/* ---------------- 我的 / 底部浮层 ---------------- */

const sheet = ref(false);
const cal = ref(false);
const toast = ref("");
let toastTimer: ReturnType<typeof setTimeout> | undefined;

function notify(text: string): void {
  toast.value = text;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (toast.value = ""), 1600);
}

async function reimport(): Promise<void> {
  sheet.value = false;
  await router.push({ name: "login" });
}

async function editStart(): Promise<void> {
  sheet.value = false;
  await router.push({ name: "semester" });
}

async function logout(): Promise<void> {
  sheet.value = false;
  await edu.logout();
  await router.replace({ name: "login" });
}
</script>

<template>
  <div class="home">
    <div ref="scroller" class="scroll">
      <HomeHead :layers="headLayers" :veil="veil" :morph="morph" :incoming-docks="incoming?.docks ?? false" />

      <div class="timeline">
        <template v-for="block in blocks" :key="`${block.kind}-${block.anchor}`">
          <WeekDivider v-if="block.kind === 'week'" :week="block.week" />
          <FreeBlock v-else-if="block.kind === 'free'" :block="block" />
          <DaySection
            v-else
            :block="block"
            :now-minutes="nowMinutes"
            :view-is-today="viewIsToday"
          />
        </template>
      </div>
    </div>

    <HomeDock
      :view-is-today="viewIsToday"
      :days="stripDays"
      :counts="courseCount"
      :view="view"
      :today="today"
      :recenter="recenter"
      @jump="jump"
      @calendar="cal = true"
      @notify="notify"
      @me="sheet = true"
    />

    <Transition name="toast">
      <p v-if="toast" class="toast">
        {{ toast }}
      </p>
    </Transition>

    <DateSheet :open="cal" :value="viewDate" hint="选一天，跳到那天的课程" @pick="jump" @close="cal = false" />

    <MeDrawer
      :open="sheet"
      :student-name="snapshot.studentName"
      :semester-name="snapshot.semester.name"
      :start-date="timetable.startDate"
      @update:open="(v: boolean) => (sheet = v)"
      @reimport="reimport"
      @edit-start="editStart"
      @logout="logout"
    />
  </div>
</template>

<style scoped lang="scss">
.home {
  --home-pad: 20px;

  position: relative;
  height: 100%;
  overflow: hidden;
}

.scroll {
  position: relative;
  height: 100%;
  padding: 0 var(--home-pad) 220px;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.timeline {
  padding-top: 8px;
}

.toast {
  position: absolute;
  bottom: calc(var(--safe-bottom) + 178px);
  left: 50%;
  z-index: 30;
  padding: 7px 14px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  background: rgb(27 28 32 / 82%);
  transform: translateX(-50%);
}

.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.2s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
}
</style>
