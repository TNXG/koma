<script setup lang="ts" vapor>
import { useEventListener } from "@vueuse/core";
import { computed, nextTick, onMounted, ref, watch } from "vue";
import CalendarLine from "~icons/mingcute/calendar-line";
import Home4Line from "~icons/mingcute/home-4-line";
import Settings3Line from "~icons/mingcute/settings-3-line";
import { parseLocalDate, sameDay, toIsoDate, weekdayLabel } from "@/domain/calendar";

const props = defineProps<{
  viewIsToday: boolean;
  days: Date[];
  counts: Map<string, number>;
  view: string;
  today: Date;
  /** 外部跳转（日历选日/回到今天）时递增，强制日期条立即归位 */
  recenter: number;
}>();

const emit = defineEmits<{
  jump: [day: Date];
  calendar: [];
  notify: [text: string];
  me: [];
}>();

const viewDate = computed(() => parseLocalDate(props.view));

const box = ref<HTMLElement>();

const STRIP_IDLE = 1800;
let stripAuto = false;
let stripAutoTimer: ReturnType<typeof setTimeout> | undefined;
let stripTouchedAt = 0;
let stripFollowTimer: ReturnType<typeof setTimeout> | undefined;

function centerStrip(behavior: ScrollBehavior): void {
  const el = box.value?.querySelector<HTMLElement>(`[data-d="${props.view}"]`);
  if (!box.value || !el)
    return;
  const left = el.offsetLeft - (box.value.clientWidth - el.offsetWidth) / 2;
  if (Math.abs(box.value.scrollLeft - Math.max(0, left)) < 1)
    return;
  stripAuto = true;
  clearTimeout(stripAutoTimer);
  stripAutoTimer = setTimeout(releaseStrip, 900);
  box.value.scrollTo({ left, behavior });
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

watch(() => props.view, followStrip);
watch(() => props.recenter, () => {
  stripTouchedAt = 0;
  followStrip();
});

useEventListener(box, "scroll", onStripScroll, { passive: true });
useEventListener(box, "scrollend", () => {
  clearTimeout(stripAutoTimer);
  releaseStrip();
});

onMounted(async () => {
  await nextTick();
  centerStrip("auto");
});

function choose(day: Date): void {
  stripTouchedAt = 0;
  emit("jump", day);
}
</script>

<template>
  <div class="fade" />
  <div class="dock">
    <Transition name="pill">
      <button v-if="!viewIsToday" class="pill" @click="choose(today)">
        回到今天
      </button>
    </Transition>

    <div class="strip">
      <div ref="box" class="strip__box">
        <button
          v-for="day in days" :key="day.getTime()" :data-d="toIsoDate(day)" class="strip__day"
          :class="{ 'strip__day--on': sameDay(day, viewDate), 'strip__day--past': day < today }" @click="choose(day)"
        >
          <span class="strip__num">
            {{ day.getDate() }}
          </span>
          <span class="strip__dow">
            {{ sameDay(day, today) ? '今天' : day.getDate() === 1 ? `${day.getMonth() + 1}月` : `周${weekdayLabel(day)}`
            }}
          </span>
          <span v-if="counts.get(toIsoDate(day))" class="strip__count">{{ counts.get(toIsoDate(day))
          }}</span>
        </button>
      </div>
      <button class="strip__cal" aria-label="选择日期" @click="emit('calendar')">
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
      <button class="tab" @click="emit('notify', '周课表正在设计中')">
        <CalendarLine :width="19" :height="19" class="shrink-0" aria-hidden="true" />
        <span class="tab__label">
          课表
        </span>
      </button>
      <button class="tab" @click="emit('me')">
        <Settings3Line :width="19" :height="19" class="shrink-0" aria-hidden="true" />
        <span class="tab__label">
          我的
        </span>
      </button>
    </nav>
  </div>
</template>

<style scoped lang="scss">
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

.pill-enter-active,
.pill-leave-active {
  transition: opacity 0.2s ease;
}

.pill-enter-from,
.pill-leave-to {
  opacity: 0;
}
</style>
