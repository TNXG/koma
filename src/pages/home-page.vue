<script setup lang="ts" vapor>
import type { TimetableCourse } from "@/types/timetable";
import { useEventListener, useIntervalFn, useNow } from "@vueuse/core";
import { computed, nextTick, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import CalendarLine from "~icons/mingcute/calendar-line";
import ExitLine from "~icons/mingcute/exit-line";
import Home4Line from "~icons/mingcute/home-4-line";
import MapPinLine from "~icons/mingcute/map-pin-line";
import Refresh2Line from "~icons/mingcute/refresh-2-line";
import Settings3Line from "~icons/mingcute/settings-3-line";
import User3Line from "~icons/mingcute/user-3-line";
import DateSheet from "@/components/common/date-sheet.vue";
import KButton from "@/components/common/k-button.vue";
import TopVeil from "@/components/common/top-veil.vue";
import { Drawer, DrawerContent, DrawerDescription, DrawerTitle } from "@/components/ui/drawer";
import {
  addDays,
  coursesOnDate,
  daysBetween,
  formatDuration,
  formatMinutes,
  formatMonthDay,
  isoWeekday,
  minutesOfDay,
  parseLocalDate,
  sameDay,
  startOfDay,
  toIsoDate,
  weekdayLabel,
} from "@/domain/calendar";
import { courseVisual } from "@/domain/course-visual";
import { useEduStore } from "@/stores/edu-store";
import { useTimetableStore } from "@/stores/timetable-store";

type RowState = "past" | "now" | "next" | "later";

interface Row {
  course: TimetableCourse;
  start: number | null;
  end: number | null;
  state: RowState;
  pct: number;
}

interface DayBlock {
  kind: "day";
  anchor: string;
  date: Date;
  relative: string | null;
  week: number;
  rows: Row[];
}

interface FreeBlock {
  kind: "free";
  anchor: string;
  start: Date;
  end: Date;
  label: string;
  days: number;
}

interface WeekBlock {
  kind: "week";
  anchor: string;
  week: number;
}

type Block = DayBlock | FreeBlock | WeekBlock;

const RELATIVE = ["今天", "明天", "后天"];

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

/* ---------------- 时间线 ---------------- */

const totalDays = computed(() => daysBetween(termStart, termEnd) + 1);

const blocks = computed<Block[]>(() => {
  const out: Block[] = [];
  const rowsByDay = new Map<string, Row[]>();
  let nextMarked = false;

  for (let offset = 0; offset < totalDays.value; offset++) {
    const date = addDays(termStart, offset);
    const iso = toIsoDate(date);
    const isToday = sameDay(date, today.value);
    const items = coursesOnDate(snapshot.courses, snapshot.timeGrid, timetable.startDate, date);
    const rows: Row[] = items.map((item) => {
      const past = !isToday
        ? date < today.value
        : item.end !== null && item.end <= nowMinutes.value;
      if (past)
        return { ...item, state: "past", pct: 100 };
      if (isToday && item.start !== null && item.start <= nowMinutes.value)
        return { ...item, state: "now", pct: (nowMinutes.value - item.start) / Math.max(1, (item.end ?? item.start) - item.start) * 100 };
      if (!nextMarked && isToday) {
        nextMarked = true;
        return { ...item, state: "next", pct: 0 };
      }
      return { ...item, state: "later", pct: 0 };
    });
    rowsByDay.set(iso, rows);
  }

  for (let offset = 0; offset < totalDays.value; offset++) {
    const date = addDays(termStart, offset);
    const iso = toIsoDate(date);
    const rows = rowsByDay.get(iso)!;
    const dayOfWeek = isoWeekday(date);
    const weekNo = Math.floor(offset / 7) + 1;

    if (dayOfWeek === 1)
      out.push({ kind: "week", anchor: iso, week: weekNo });

    if (rows.length > 0 || dayOfWeek === 1) {
      const diff = daysBetween(today.value, date);
      out.push({
        kind: "day",
        anchor: iso,
        date,
        relative: diff >= 0 && diff < RELATIVE.length
          ? RELATIVE[diff]
          : null,
        week: weekNo,
        rows,
      });
      continue;
    }

    const previous = out.at(-1);
    if (previous?.kind === "free" && isoWeekday(previous.end) !== 7) {
      previous.end = date;
      previous.days++;
      continue;
    }
    out.push({
      kind: "free",
      anchor: iso,
      start: date,
      end: date,
      label: dayOfWeek >= 6
        ? "周末"
        : "无课",
      days: 1,
    });
  }
  return out;
});

/** 每个有课日期 → 所在区块的滚动锚点（无课日期落在整块「无课」上） */
const blockAnchorByDay = computed(() => {
  const map = new Map<string, string>();
  for (const block of blocks.value) {
    if (block.kind === "day") {
      map.set(block.anchor, block.anchor);
    } else if (block.kind === "free") {
      for (let d = block.start; d <= block.end; d = addDays(d, 1))
        map.set(toIsoDate(d), block.anchor);
    }
  }
  return map;
});

const courseCount = computed(() => {
  const map = new Map<string, number>();
  for (const block of blocks.value) {
    if (block.kind === "day")
      map.set(block.anchor, block.rows.length);
  }
  return map;
});

/* ---------------- 滚动与选中 ---------------- */

const scroller = ref<HTMLElement>();
const head = ref<HTMLElement>();
const titleStack = ref<HTMLElement>();
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

function headInfo(date: Date): { parts: string[]; empty: boolean } {
  const rows = coursesOnDate(snapshot.courses, snapshot.timeGrid, timetable.startDate, date);
  const week = Math.floor(daysBetween(termStart, date) / 7) + 1;
  if (week < 1) {
    return {
      parts: [`距离开学 ${daysBetween(date, termStart)} 天`],
      empty: false,
    };
  }
  if (week > snapshot.semester.totalWeeks) {
    return {
      parts: ["学期已结束"],
      empty: false,
    };
  }
  const parts = [
    `第 ${week} 周`,
    week % 2 === 1
      ? "单周"
      : "双周",
  ];
  if (rows.length === 0) {
    parts.push("无课");
  } else if (sameDay(date, today.value)) {
    const remaining = rows.filter(r => r.end === null || r.end > nowMinutes.value).length;
    parts.push(`${rows.length} 节课，剩 ${remaining} 节`);
  } else {
    parts.push(`${rows.length} 节课`);
  }
  return { parts, empty: rows.length === 0 };
}

type LayerRole = "cur" | "inc";

const headLayers = computed(() => {
  const layers = [
    {
      iso: view.value,
      role: "cur" as LayerRole,
      date: viewDate.value,
      ...headInfo(viewDate.value),
    },
  ];
  if (incoming.value) {
    const date = parseLocalDate(incoming.value.anchor);
    layers.push({
      iso: incoming.value.anchor,
      role: "inc",
      date,
      ...headInfo(date),
    });
  }
  return layers;
});

function titleStyle(role: LayerRole): Record<string, string | number> {
  const p = morph.value;
  if (role === "cur")
    return { opacity: 1 - smooth(0, 0.55, p), transform: `translateY(${-10 * p}px)` };
  if (incoming.value?.docks) {
    return {
      opacity: smooth(0.72, 1, p),
    };
  }
  return {
    opacity: smooth(0.25, 1, p),
    transform: `translateY(${(1 - p) * 12}px)`,
  };
}

function metaStyle(role: LayerRole): Record<string, string | number> {
  const p = morph.value;
  return role === "cur"
    ? { opacity: 1 - smooth(0, 0.5, p), transform: `translateY(${-6 * p}px)` }
    : {
        opacity: smooth(0.5, 1, p),
        transform: `translateY(${(1 - p) * 6}px)`,
      };
}

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

const strip = ref<HTMLElement>();
const stripDays = computed(() => Array.from({ length: totalDays.value }, (_, i) => addDays(termStart, i)));

const STRIP_IDLE = 1800;
let stripAuto = false;
let stripAutoTimer: ReturnType<typeof setTimeout> | undefined;
let stripTouchedAt = 0;
let stripFollowTimer: ReturnType<typeof setTimeout> | undefined;

function centerStrip(behavior: ScrollBehavior): void {
  const box = strip.value;
  const el = box?.querySelector<HTMLElement>(`[data-d="${view.value}"]`);
  if (!box || !el)
    return;
  const left = el.offsetLeft - (box.clientWidth - el.offsetWidth) / 2;
  if (Math.abs(box.scrollLeft - Math.max(0, left)) < 1)
    return;
  stripAuto = true;
  clearTimeout(stripAutoTimer);
  stripAutoTimer = setTimeout(releaseStrip, 900);
  box.scrollTo({ left, behavior });
}

function releaseStrip(): void {
  stripAuto = false;
}

/* 日期条跟随主列表；用户刚手动拨过日期条时先不抢，空闲 STRIP_IDLE 后再平滑归位 */
function followStrip(): void {
  clearTimeout(stripFollowTimer);
  const idle = Date.now() - stripTouchedAt;
  if (idle >= STRIP_IDLE)
    centerStrip("smooth");
  else
    stripFollowTimer = setTimeout(followStrip, STRIP_IDLE - idle);
}

function onStripScroll(): void {
  if (!stripAuto)
    stripTouchedAt = Date.now();
}

watch(view, followStrip);
useEventListener(strip, "scroll", onStripScroll, { passive: true });
useEventListener(strip, "scrollend", () => {
  clearTimeout(stripAutoTimer);
  releaseStrip();
});

function jump(day: Date): void {
  stripTouchedAt = 0;
  pickDay(day);
  nextTick(followStrip);
}

function backToToday(): void {
  jump(today.value);
}

onMounted(async () => {
  await nextTick();
  const el = elementOf(blockAnchorByDay.value.get(view.value) ?? view.value);
  if (el && scroller.value)
    scroller.value.scrollTop = Math.max(0, dockTop(el));
  update();
  centerStrip("auto");
});

/* ---------------- 课程提示 ---------------- */

function hint(row: Row): string | null {
  if (row.state === "now" && row.end !== null)
    return `上课中，现在 ${formatMinutes(nowMinutes.value)}，还剩 ${formatDuration(row.end - nowMinutes.value)}`;
  if (row.state === "next" && row.start !== null) {
    const gap = row.start - nowMinutes.value;
    if (viewIsToday.value && gap > 0 && gap <= 60)
      return `还有 ${formatDuration(gap)}，${formatMinutes(row.start)} 开始`;
    return `下一节，${formatMinutes(row.start)} 开始`;
  }
  return null;
}

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
      <header ref="head" class="head">
        <TopVeil :opacity="veil" />
        <div ref="titleStack" class="head__stack">
          <div v-for="layer in headLayers" :key="layer.iso" class="head__layer" :aria-hidden="layer.role === 'inc'">
            <h1 class="head__title" :style="titleStyle(layer.role)">
              {{ formatMonthDay(layer.date) }} <span>周{{ weekdayLabel(layer.date) }}</span>
            </h1>
            <p class="head__meta" :style="metaStyle(layer.role)">
              <template v-for="(part, i) in layer.parts" :key="part">
                <span v-if="i" class="head__rule" />
                <span :class="{ 'head__meta--mute': i === layer.parts.length - 1 && layer.empty }">{{ part }}</span>
              </template>
            </p>
          </div>
        </div>
      </header>

      <div class="timeline">
        <template v-for="block in blocks" :key="`${block.kind}-${block.anchor}`">
          <div v-if="block.kind === 'week'" class="week">
            <span class="week__rule" />第 {{ block.week }} 周<span class="week__rule" />
          </div>

          <section v-else-if="block.kind === 'free'" :data-anchor="block.anchor" class="free">
            <p class="free__name">
              {{ block.label }}
            </p>
            <p class="free__sub">
              {{ formatMonthDay(block.start) }}<template v-if="block.days > 1">
                —{{ formatMonthDay(block.end) }}
              </template> · {{ block.days }} 天
            </p>
          </section>

          <section v-else :data-anchor="block.anchor" class="day">
            <div class="day__head">
              <span v-if="block.relative" class="day__label" data-fade>{{ block.relative }}</span>
              <span class="day__date" :class="{ 'day__date--lead': !block.relative }" data-morph>
                <b>{{ formatMonthDay(block.date) }}</b> <span>周{{ weekdayLabel(block.date) }}</span>
              </span>
              <span v-if="block.rows.length" class="day__count" data-fade>
                {{ block.rows.length }} 节课<template v-if="block.rows[0].start !== null">，{{
                  formatMinutes(block.rows[0].start!) }} 开始</template>
              </span>
            </div>

            <div v-if="block.rows.length === 0" class="day__empty">
              无课
            </div>

            <div v-for="(row, ri) in block.rows" :key="row.course.id" class="slot">
              <div class="slot__time" :class="{ 'is-past': row.state === 'past' }">
                <p class="slot__period">
                  {{ row.course.startPeriod === row.course.endPeriod ? `${row.course.startPeriod}节`
                    : `${row.course.startPeriod}–${row.course.endPeriod}节` }}
                </p>
                <p class="slot__start">
                  {{ row.start !== null ? formatMinutes(row.start) : '' }}
                </p>
                <p class="slot__end">
                  {{ row.end !== null ? formatMinutes(row.end) : '' }}
                </p>
              </div>

              <div
                class="slot__rail"
                :class="{ 'slot__rail--first': ri === 0, 'slot__rail--last': ri === block.rows.length - 1 }"
              >
                <div class="slot__line">
                  <i v-if="row.state === 'past'" class="slot__fill" />
                  <template v-else-if="row.state === 'now'">
                    <i class="slot__fill" :style="{ height: `${row.pct}%` }" />
                    <b class="slot__dot" :style="{ top: `${row.pct}%` }" />
                  </template>
                </div>
              </div>

              <div class="slot__main" :class="{ 'is-past': row.state === 'past' }">
                <div
                  class="card" :class="`card--${row.state}`"
                  :style="{ '--tone': `var(--c-tone-${courseVisual(row.course.name).tone})` }"
                >
                  <i v-if="row.state === 'now'" class="card__progress" :style="{ width: `${row.pct}%` }" />
                  <div class="card__top">
                    <div class="card__text">
                      <p class="card__name">
                        {{ row.course.name }}
                      </p>
                      <p v-if="row.course.location" class="card__info">
                        <MapPinLine :width="13" :height="13" class="shrink-0" aria-hidden="true" />
                        <span>{{ row.course.location }}</span>
                      </p>
                      <p v-if="row.course.teacher" class="card__info">
                        <User3Line :width="13" :height="13" class="shrink-0" aria-hidden="true" />
                        <span>{{ row.course.teacher }}</span>
                      </p>
                    </div>
                    <component
                      :is="courseVisual(row.course.name).icon" :width="28" :height="28"
                      class="card__glyph shrink-0" aria-hidden="true"
                    />
                  </div>
                  <p v-if="hint(row)" class="card__hint" :class="{ 'card__hint--mute': row.state === 'next' }">
                    {{ hint(row) }}
                  </p>
                </div>
              </div>
            </div>
          </section>
        </template>
      </div>
    </div>

    <div class="fade" />
    <div class="dock">
      <Transition name="pill">
        <button v-if="!viewIsToday" class="pill" @click="backToToday">
          回到今天
        </button>
      </Transition>

      <div class="strip">
        <div ref="strip" class="strip__box">
          <button
            v-for="day in stripDays" :key="day.getTime()" :data-d="toIsoDate(day)" class="strip__day"
            :class="{ 'strip__day--on': sameDay(day, viewDate), 'strip__day--past': day < today }" @click="jump(day)"
          >
            <span class="strip__num">
              {{ day.getDate() }}
            </span>
            <span class="strip__dow">
              {{ sameDay(day, today) ? '今天' : day.getDate() === 1 ? `${day.getMonth() + 1}月` : `周${weekdayLabel(day)}`
              }}
            </span>
            <span v-if="courseCount.get(toIsoDate(day))" class="strip__count">{{ courseCount.get(toIsoDate(day))
            }}</span>
          </button>
        </div>
        <button class="strip__cal" aria-label="选择日期" @click="cal = true">
          <CalendarLine :width="16" :height="16" class="shrink-0" aria-hidden="true" />
        </button>
      </div>

      <nav class="tabs" aria-label="主导航">
        <button class="tab tab--on" aria-current="page">
          <Home4Line :width="19" :height="19" class="shrink-0" aria-hidden="true" />
          <span class="tab__label">
            今天
          </span>
        </button>
        <button class="tab" @click="notify('周课表正在设计中')">
          <CalendarLine :width="19" :height="19" class="shrink-0" aria-hidden="true" />
          <span class="tab__label">
            课表
          </span>
        </button>
        <button class="tab" @click="sheet = true">
          <Settings3Line :width="19" :height="19" class="shrink-0" aria-hidden="true" />
          <span class="tab__label">
            我的
          </span>
        </button>
      </nav>
    </div>

    <Transition name="toast">
      <p v-if="toast" class="toast">
        {{ toast }}
      </p>
    </Transition>

    <DateSheet :open="cal" :value="viewDate" hint="选一天，跳到那天的课程" @pick="jump" @close="cal = false" />

    <Drawer :open="sheet" @update:open="(v: boolean) => (sheet = v)">
      <DrawerContent class="me mx-auto w-full max-w-[430px] border-none px-3 pb-[calc(var(--safe-bottom)+32px)]">
        <DrawerTitle class="sr-only">
          我的
        </DrawerTitle>
        <DrawerDescription class="sr-only">
          {{ snapshot.studentName ?? '同学' }} · {{ snapshot.semester.name }}
        </DrawerDescription>
        <div class="me__who">
          <p class="me__name">
            {{ snapshot.studentName ?? '同学' }}
          </p>
          <p class="me__sub">
            {{ snapshot.semester.name }}
          </p>
        </div>
        <KButton variant="text" tone="mute" class="me__row" @click="reimport">
          <Refresh2Line :width="17" :height="17" class="shrink-0" aria-hidden="true" />
          重新导入课表
        </KButton>
        <KButton variant="text" tone="mute" class="me__row" @click="editStart">
          <CalendarLine :width="17" :height="17" class="shrink-0" aria-hidden="true" />
          修改开学时间
          <span class="me__value">{{ formatMonthDay(parseLocalDate(timetable.startDate)) }}</span>
        </KButton>
        <KButton variant="text" tone="danger" class="me__row" @click="logout">
          <ExitLine :width="17" :height="17" class="shrink-0" aria-hidden="true" />
          退出登录并清除课表
        </KButton>
      </DrawerContent>
    </Drawer>
  </div>
</template>

<style scoped lang="scss">
$time: 44px;
$rail: 2px;
$gap: 12px;
$pad: 20px;
$slot-gap: 6px;
$day-gap: 28px;

.home {
  position: relative;
  height: 100%;
  overflow: hidden;
}

.scroll {
  position: relative;
  height: 100%;
  padding: 0 $pad 220px;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

/* 顶部标题：吸顶 + 多层羽化；横向撑满到屏幕边，羽化不会在内边距处被截断 */
.head {
  position: sticky;
  top: 0;
  z-index: 5;
  margin: 0 (-$pad);
  padding: calc(var(--safe-top) + 18px) $pad 14px;

  &__stack {
    position: relative;
  }

  &__layer + &__layer {
    position: absolute;
    inset: 0;
  }

  &__title {
    font-size: 26px;
    font-weight: 800;
    line-height: 1.2;
    letter-spacing: -0.02em;
    will-change: opacity, transform;

    span {
      font-weight: 700;
      color: var(--c-ink5);
    }
  }

  &__meta {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px;
    margin-top: 8px;
    font-size: 12.5px;
    font-weight: 600;
    color: var(--c-ink2);
    will-change: opacity, transform;

    &--mute {
      color: var(--c-ink4);
    }
  }

  &__rule {
    height: 12px;
    border-left: 1px solid var(--c-line);
  }
}

.timeline {
  padding-top: 8px;
}

.week {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0 $day-gap;
  font-size: 11px;
  font-weight: 600;
  color: var(--c-ink5);

  &__rule {
    flex: 1;
    height: 1px;
    background: var(--c-line);
  }
}

.day {
  padding-bottom: $day-gap;

  &__head {
    position: relative;
    display: flex;
    align-items: baseline;
    gap: 10px;
    padding-bottom: 10px;
  }

  &__label,
  &__date,
  &__count {
    will-change: opacity, transform;
  }

  &__label {
    font-size: 17px;
    font-weight: 800;
    line-height: 1;
    letter-spacing: -0.02em;
  }

  &__date {
    font-size: 12.5px;
    line-height: 1.2;
    white-space: nowrap;
    color: var(--c-ink3);

    b {
      font-weight: 700;
    }

    span {
      font-weight: 600;
      color: var(--c-ink5);
    }

    &--lead {
      font-size: 17px;
      letter-spacing: -0.02em;
      color: var(--c-ink);

      b {
        font-weight: 800;
      }
    }
  }

  &__count {
    margin-left: auto;
    font-size: 12px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    color: var(--c-ink5);
  }

  &__empty {
    padding-top: 6px;
    font-size: 13.5px;
    font-weight: 600;
    color: var(--c-ink4);
  }
}

.free {
  margin-bottom: $day-gap;
  padding: 18px 20px;
  border-radius: 20px;
  background: linear-gradient(135deg, var(--c-surface), color-mix(in srgb, var(--c-accent) 5%, var(--c-surface)));
  box-shadow: 0 4px 16px rgb(27 28 32 / 4%);

  &__name {
    font-size: 17px;
    font-weight: 800;
    letter-spacing: -0.02em;
  }

  &__sub {
    margin-top: 4px;
    font-size: 13px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    color: var(--c-ink3);
  }
}

/* 一节课：时间列 + 竖线 + 卡片；竖线从首张卡顶到末张卡底，中间连续 */
.slot {
  display: flex;
  padding: $slot-gap 0;

  &__time {
    flex: none;
    width: $time;
    padding-top: 14px;
    text-align: left;

    &.is-past {
      opacity: 0.5;
    }
  }

  &__period {
    font-size: 11px;
    font-weight: 700;
    color: var(--c-ink2);
  }

  &__start,
  &__end {
    font-size: 11px;
    font-weight: 500;
    font-variant-numeric: tabular-nums;
  }

  &__start {
    margin-top: 4px;
    color: var(--c-ink4);
  }

  &__end {
    color: var(--c-ink5);
  }

  &__rail {
    flex: none;
    width: $rail;
    margin: (-$slot-gap) 0 (-$slot-gap) $gap;
    align-self: stretch;

    &--first {
      margin-top: 14px;
    }

    &--last {
      margin-bottom: 14px;
    }
  }

  &__line {
    position: relative;
    height: 100%;
    background: var(--c-line);

    .slot__rail--first & {
      border-top-left-radius: $rail;
      border-top-right-radius: $rail;
    }

    .slot__rail--last & {
      border-bottom-left-radius: $rail;
      border-bottom-right-radius: $rail;
    }
  }

  &__fill {
    position: absolute;
    inset: 0 0 auto;
    display: block;
    height: 100%;
    border-radius: inherit;
    background: var(--c-accent);
  }

  &__dot {
    position: absolute;
    left: 50%;
    width: 9px;
    height: 9px;
    border: 2.5px solid var(--c-accent);
    border-radius: 50%;
    background: var(--c-surface);
    transform: translate(-50%, -50%);
  }

  &__main {
    min-width: 0;
    flex: 1;
    padding-left: 16px;

    &.is-past {
      opacity: 0.5;
    }
  }
}

/* 课程卡：保留原排版，表层取 mikcb 的学科色渐变 + 同色柔光投影 + 进行时进度填充 */
.card {
  position: relative;
  overflow: hidden;
  padding: 14px 14px 14px 16px;
  border-radius: 20px;
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--tone) 3%, var(--c-surface)) 0%,
    color-mix(in srgb, var(--tone) 11%, var(--c-surface)) 100%
  );
  box-shadow: 0 4px 16px color-mix(in srgb, var(--tone) 12%, transparent);

  &--now {
    box-shadow:
      0 0 0 1.2px color-mix(in srgb, var(--tone) 30%, transparent),
      0 4px 18px color-mix(in srgb, var(--tone) 20%, transparent);
  }

  &--past {
    box-shadow: none;
  }

  &__progress {
    position: absolute;
    inset: 0 auto 0 0;
    display: block;
    background: color-mix(in srgb, var(--tone) 12%, transparent);
    transition: width 0.6s ease;
  }

  &__top {
    position: relative;
    display: flex;
    align-items: flex-start;
    gap: 8px;
  }

  &__text {
    min-width: 0;
    flex: 1;
  }

  &__name {
    font-size: 16px;
    font-weight: 700;
    line-height: 1.25;
    letter-spacing: -0.01em;
  }

  &__info {
    display: flex;
    align-items: center;
    gap: 5px;
    margin-top: 5px;
    font-size: 12.5px;
    font-weight: 500;
    color: var(--c-ink3);

    & + & {
      margin-top: 3px;
    }

    svg {
      color: color-mix(in srgb, var(--tone) 55%, var(--c-ink4));
    }

    span {
      min-width: 0;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
    }
  }

  &__glyph {
    flex: none;
    color: var(--tone);
    transform: rotate(-4deg);
  }

  &__hint {
    position: relative;
    display: inline-block;
    margin-top: 8px;
    padding: 3px 8px;
    border-radius: 8px;
    font-size: 12px;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
    color: var(--c-accent);
    background: color-mix(in srgb, var(--c-accent) 10%, transparent);

    &--mute {
      font-weight: 600;
      color: var(--c-ink2);
      background: color-mix(in srgb, var(--tone) 12%, transparent);
    }
  }
}

/* 底部：渐隐 + 日期条 + 导航 */
.fade {
  position: absolute;
  inset: auto 0 0;
  z-index: 6;
  height: 210px;
  background: var(--c-fade);
  pointer-events: none;
}

.dock {
  position: absolute;
  inset: auto 0 0;
  z-index: 7;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 0 16px calc(var(--safe-bottom) + 24px);
  pointer-events: none;

  > * {
    pointer-events: auto;
  }
}

/* 浮在日期条的投影之上，否则投影会盖住按钮 */
.pill {
  position: relative;
  z-index: 1;
  padding: 7px 14px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: var(--c-accent);
  background: var(--c-surface);
}

.strip {
  display: flex;
  align-items: stretch;
  width: 100%;
  padding: 6px 4px;
  border: 1px solid var(--c-dock-line);
  border-radius: 24px;
  background: var(--c-dock);
  box-shadow: var(--c-dock-shadow);

  &__box {
    display: flex;
    flex: 1;
    gap: 4px;
    min-width: 0;
    padding: 0 4px;
    overflow-x: auto;
    scroll-snap-type: x proximity;
    mask-image: linear-gradient(90deg, transparent, #000 10px, #000 calc(100% - 10px), transparent);
    scrollbar-width: none;
  }

  &__day {
    position: relative;
    display: flex;
    flex: none;
    flex-direction: column;
    align-items: center;
    width: 46px;
    padding: 5px 0;
    border-radius: 13px;
    scroll-snap-align: center;
    transition: background 0.2s ease;

    &--on {
      background: var(--c-accent-soft);
    }

    &--past .strip__num,
    &--past .strip__dow {
      color: var(--c-ink5);
    }
  }

  &__num {
    font-size: 17px;
    font-weight: 700;
    line-height: 1.2;
    font-variant-numeric: tabular-nums;
    color: var(--c-ink);

    .strip__day--on & {
      color: var(--c-accent);
    }
  }

  &__dow {
    margin-top: 2px;
    font-size: 10.5px;
    font-weight: 600;
    color: var(--c-ink4);

    .strip__day--on & {
      color: var(--c-accent);
    }
  }

  &__count {
    position: absolute;
    top: 4px;
    right: 5px;
    font-size: 8px;
    line-height: 1;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
    color: var(--c-ink5);

    .strip__day--on & {
      color: var(--c-accent2);
    }
  }

  &__cal {
    display: flex;
    flex: none;
    align-items: center;
    justify-content: center;
    width: 36px;
    color: var(--c-ink2);
  }
}

.tabs {
  display: flex;
  width: 92%;
  padding: 5px;
  border: 1px solid var(--c-dock-line);
  border-radius: 999px;
  background: var(--c-dock);
  box-shadow: var(--c-dock-shadow);
}

.tab {
  position: relative;
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 6px 4px 5px;
  border-radius: 999px;
  color: var(--c-ink);
  transition:
    background 0.2s ease,
    color 0.2s ease,
    transform 0.15s ease;

  &:active {
    transform: scale(0.94);
  }

  &--on {
    color: var(--c-accent);
    background: var(--c-accent-soft);
  }

  &__label {
    font-size: 9.5px;
    font-weight: 700;
  }
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

.me {
  display: flex;
  flex-direction: column;
  gap: 2px;

  &__who {
    padding: 4px 8px 14px;
  }

  &__name {
    font-size: 17px;
    font-weight: 800;
    letter-spacing: -0.02em;
  }

  &__sub {
    margin-top: 2px;
    font-size: 12px;
    font-weight: 500;
    color: var(--c-ink4);
  }

  &__row {
    display: flex;
    width: 100%;
    align-items: center;
    gap: 10px;
    padding: 13px 10px;
    border-radius: 12px;
    font-size: 14px;
    font-weight: 500;
    justify-content: flex-start;

    &:active {
      background: var(--c-bg);
    }
  }

  &__value {
    margin-left: auto;
    font-size: 12px;
    font-weight: 500;
    color: var(--c-ink4);
  }
}

.pill-enter-active,
.pill-leave-active,
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.2s ease;
}

.pill-enter-from,
.pill-leave-to,
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
}
</style>
