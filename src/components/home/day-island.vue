<script setup lang="ts" vapor>
import { useEventListener } from "@vueuse/core";
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import CalendarLine from "~icons/mingcute/calendar-line";
import { daysBetween, parseLocalDate, sameDay, toIsoDate, weekdayLabel } from "@/domain/calendar";

/**
 * 右侧日期岛：贴边的半透明胶囊。收起时只露一枚把手，展开是纵向日期条。
 * 上下拨动 = 换天（原生滚动吸附 + 轻震）；底部固定「今」与月历入口。
 */
const props = defineProps<{
  view: string;
  today: Date;
  days: Date[];
  counts: Map<string, number>;
  viewIsToday: boolean;
}>();

const emit = defineEmits<{ jump: [day: Date]; calendar: [] }>();

const OPEN_IDLE = 6000;

const open = ref(false);
const box = ref<HTMLElement>();
const viewDate = computed(() => parseLocalDate(props.view));

let idleTimer: ReturnType<typeof setTimeout> | undefined;
let settleTimer: ReturnType<typeof setTimeout> | undefined;
let autoTimer: ReturnType<typeof setTimeout> | undefined;
let followTimer: ReturnType<typeof setTimeout> | undefined;
/** 程序滚动期间不回读位置，否则会把自己滚出来的中间日期当成选择 */
let autoScroll = false;
let touchedAt = 0;
let lastPicked = "";

function vibrate(): void {
  if (!open.value)
    return;
  navigator.vibrate?.(6);
}

function centerView(behavior: ScrollBehavior): void {
  const el = box.value?.querySelector<HTMLElement>(`[data-d="${props.view}"]`);
  if (!box.value || !el)
    return;
  const top = el.offsetTop + el.offsetHeight / 2 - box.value.clientHeight / 2;
  if (Math.abs(box.value.scrollTop - Math.max(0, top)) < 1)
    return;
  autoScroll = true;
  clearTimeout(autoTimer);
  autoTimer = setTimeout(() => (autoScroll = false), 320);
  box.value.scrollTo({ top, behavior });
}

/** 外部换天后归位；用户刚拨过就先等空闲，别抢手势 */
function follow(): void {
  clearTimeout(followTimer);
  const idle = Date.now() - touchedAt;
  if (idle >= 900)
    centerView("smooth");
  else
    followTimer = setTimeout(follow, 900 - idle);
}

function pickFromScroll(): void {
  const el = box.value;
  if (!el)
    return;
  const center = el.scrollTop + el.clientHeight / 2;
  let best: HTMLElement | null = null;
  let bestDist = Infinity;
  for (const chip of el.querySelectorAll<HTMLElement>("[data-d]")) {
    const dist = Math.abs(chip.offsetTop + chip.offsetHeight / 2 - center);
    if (dist < bestDist) {
      bestDist = dist;
      best = chip;
    }
  }
  if (!best || best.dataset.d === lastPicked)
    return;
  lastPicked = best.dataset.d!;
  if (best.dataset.d === props.view)
    return;
  vibrate();
  emit("jump", parseLocalDate(best.dataset.d!));
}

function onScroll(): void {
  if (autoScroll)
    return;
  touchedAt = Date.now();
  clearTimeout(settleTimer);
  settleTimer = setTimeout(pickFromScroll, 110);
}

function pokeIdle(): void {
  clearTimeout(idleTimer);
  idleTimer = setTimeout(() => (open.value = false), OPEN_IDLE);
}

function toggle(): void {
  open.value = !open.value;
  pokeIdle();
}

function pickToday(): void {
  pokeIdle();
  vibrate();
  emit("jump", props.today);
}

function pickCalendar(): void {
  pokeIdle();
  emit("calendar");
}

watch(open, (v) => {
  if (!v) {
    clearTimeout(idleTimer);
    return;
  }
  lastPicked = "";
  nextTick(() => centerView("auto"));
});

watch(() => props.view, () => {
  lastPicked = props.view;
  pokeIdle();
  if (open.value)
    follow();
});

useEventListener(box, "scroll", onScroll, { passive: true });
useEventListener(box, "scrollend", () => {
  clearTimeout(autoTimer);
  autoScroll = false;
  if (open.value)
    pickFromScroll();
});

onMounted(() => {
  centerView("auto");
  pokeIdle();
});

onBeforeUnmount(() => {
  clearTimeout(idleTimer);
  clearTimeout(settleTimer);
  clearTimeout(autoTimer);
  clearTimeout(followTimer);
});

const offsetLabel = computed(() => {
  const diff = daysBetween(props.today, viewDate.value);
  if (diff === 0)
    return "今天";
  return diff > 0
    ? `+${diff} 天`
    : `${diff} 天`;
});
</script>

<template>
  <div class="island" :class="{ 'island--open': open }">
    <button class="island__handle" :aria-expanded="open" aria-label="展开日期" @click="toggle">
      <span class="island__day">{{ viewDate.getDate() }}</span>
      <span class="island__dow">{{ viewIsToday ? '今' : weekdayLabel(viewDate) }}</span>
      <span v-if="!viewIsToday" class="island__dot" aria-hidden="true" />
    </button>

    <div class="island__body" @pointerdown="pokeIdle">
      <div ref="box" class="strip" role="listbox" aria-label="选择日期">
        <button
          v-for="day in days" :key="day.getTime()" :data-d="toIsoDate(day)" class="strip__chip" :class="{
            'strip__chip--on': sameDay(day, viewDate),
            'strip__chip--past': day < today,
          }" role="option" :aria-selected="sameDay(day, viewDate)" @click="emit('jump', day)"
        >
          <span class="strip__num">{{ day.getDate() }}</span>
          <span class="strip__dow">{{ sameDay(day, today) ? '今' : weekdayLabel(day) }}</span>
          <span v-if="counts.get(toIsoDate(day))" class="strip__count" aria-hidden="true" />
        </button>
      </div>

      <div class="island__foot">
        <button class="island__act" :class="{ 'island__act--on': !viewIsToday }" @click="pickToday">
          今
        </button>
        <button class="island__act" aria-label="打开月历" @click="pickCalendar">
          <CalendarLine :width="15" :height="15" class="shrink-0" aria-hidden="true" />
        </button>
      </div>

      <p class="island__meta">
        {{ offsetLabel }}
      </p>
    </div>
  </div>
</template>

<style scoped lang="scss">
$handle: 26px;
$body: 58px;
$chip: 52px;

.island {
  position: absolute;
  right: 0;
  bottom: calc(var(--safe-bottom) + 22%);
  z-index: 9;
  display: flex;
  align-items: stretch;
  height: min(calc(#{$chip} * 5 + 86px), 48vh);
  border-radius: 24px 0 0 24px;
  background: color-mix(in srgb, var(--c-surface) 70%, transparent);
  box-shadow:
    inset 0 0 0 1px color-mix(in srgb, var(--c-surface) 65%, transparent),
    0 10px 30px -14px rgb(0 0 0 / 24%);
  backdrop-filter: blur(22px) saturate(180%);
  -webkit-backdrop-filter: blur(22px) saturate(180%);
  transform: translate3d(calc(100% - #{$handle}), 0, 0);
  transition: transform 0.42s cubic-bezier(0.22, 0.9, 0.24, 1);

  &--open {
    transform: translate3d(0, 0, 0);
  }

  &__handle {
    display: flex;
    flex: none;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 1px;
    width: $handle;
    border-radius: 24px 0 0 24px;
    color: var(--c-ink);
    background: color-mix(in srgb, var(--c-surface) 78%, transparent);

    &:active .island__day {
      transform: scale(0.9);
    }
  }

  &__day {
    font-size: 14px;
    font-weight: 800;
    line-height: 1.15;
    font-variant-numeric: tabular-nums;
    letter-spacing: -0.02em;
    transition: transform 0.15s ease;
  }

  &__dow {
    font-size: 9px;
    font-weight: 700;
    line-height: 1;
    color: var(--c-ink4);
  }

  &__dot {
    position: absolute;
    top: 16px;
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background: var(--c-accent);
  }

  &__body {
    position: relative;
    display: flex;
    flex: none;
    flex-direction: column;
    width: $body;
    padding: 8px 6px 9px;
  }

  &__foot {
    display: flex;
    flex: none;
    gap: 4px;
    margin-top: 5px;
    padding: 0 1px;
  }

  &__act {
    display: grid;
    flex: 1;
    place-items: center;
    height: 29px;
    border-radius: 10px;
    font-size: 12px;
    font-weight: 800;
    color: var(--c-ink4);
    background: color-mix(in srgb, var(--c-surface2) 62%, transparent);
    transition:
      color 0.2s ease,
      background 0.2s ease;

    &:active {
      transform: scale(0.94);
    }

    &--on {
      color: var(--c-accent);
      background: var(--c-accent-soft);
    }
  }

  &__meta {
    flex: none;
    margin-top: 5px;
    font-size: 9px;
    font-weight: 600;
    text-align: center;
    font-variant-numeric: tabular-nums;
    color: var(--c-ink5);
  }
}

/* 纵向日期条：上下各留两枚 chip 的空间，任意一天都能停到正中 */
.strip {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  padding-block: calc(#{$chip} * 2);
  overflow-y: auto;
  mask-image: linear-gradient(to bottom, transparent 0, #000 12%, #000 88%, transparent 100%);
  touch-action: pan-y;
  scroll-snap-type: y mandatory;
  scrollbar-width: none;

  &__chip {
    position: relative;
    display: flex;
    flex: none;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 1px;
    height: $chip;
    border-radius: 13px;
    scroll-snap-align: center;
    opacity: 0.46;
    transition:
      opacity 0.24s ease,
      background 0.24s ease;

    &--on {
      opacity: 1;
      background: var(--c-accent-soft);
    }
  }

  &__num {
    font-size: 16px;
    font-weight: 700;
    line-height: 1.1;
    font-variant-numeric: tabular-nums;
    color: var(--c-ink);

    .strip__chip--on & {
      color: var(--c-accent);
    }

    .strip__chip--past & {
      color: var(--c-ink3);
    }
  }

  &__dow {
    font-size: 9px;
    font-weight: 600;
    line-height: 1;
    color: var(--c-ink4);

    .strip__chip--on & {
      color: var(--c-accent2);
    }
  }

  &__count {
    position: absolute;
    top: 9px;
    right: 8px;
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background: var(--c-ink5);

    .strip__chip--on & {
      background: var(--c-accent);
    }
  }
}

@media (prefers-reduced-motion: reduce) {
  .island {
    transition: opacity 0.2s ease;
  }
}

@media (prefers-reduced-transparency: reduce) {
  .island,
  .island__handle {
    background: var(--c-surface);
    backdrop-filter: none;
    -webkit-backdrop-filter: none;
  }
}
</style>
