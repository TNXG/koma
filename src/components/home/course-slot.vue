<script setup lang="ts" vapor>
import type { Row } from "@/domain/home-timeline";
import { computed } from "vue";
import MapPinLine from "~icons/mingcute/map-pin-line";
import User3Line from "~icons/mingcute/user-3-line";
import { formatDuration, formatMinutes } from "@/domain/calendar";
import { courseVisual } from "@/domain/course-visual";

/** 一站：左侧时间、中间轨道上的上下两枚环形节点、右侧车票卡 */
const props = defineProps<{
  row: Row;
  first: boolean;
  last: boolean;
  nowMinutes: number;
  viewIsToday: boolean;
}>();

const visual = computed(() => courseVisual(props.row.course.name));

const period = computed(() => {
  const { startPeriod, endPeriod } = props.row.course;
  return startPeriod === endPeriod
    ? `${startPeriod} 节`
    : `${startPeriod}–${endPeriod} 节`;
});

const hint = computed<string | null>(() => {
  const { row } = props;
  if (row.state === "now" && row.end !== null)
    return `现在 ${formatMinutes(props.nowMinutes)}，还剩 ${formatDuration(row.end - props.nowMinutes)}`;
  if (row.state === "next" && row.start !== null) {
    const gap = row.start - props.nowMinutes;
    if (props.viewIsToday && gap > 0 && gap <= 60)
      return `还有 ${formatDuration(gap)}，${formatMinutes(row.start)} 开始`;
    return `下一站，${formatMinutes(row.start)} 开出`;
  }
  return null;
});
</script>

<template>
  <div
    class="stop"
    :class="[`stop--${row.state}`, { 'stop--first': first, 'stop--last': last }]"
    :style="{ '--tone': `var(--c-tone-${visual.tone})` }"
  >
    <div class="stop__time">
      <span class="stop__at stop__at--top">{{ row.start !== null ? formatMinutes(row.start) : '' }}</span>
      <span class="stop__period">{{ period }}</span>
      <span class="stop__at stop__at--bottom">{{ row.end !== null ? formatMinutes(row.end) : '' }}</span>
    </div>

    <div class="stop__rail">
      <span class="rail" />
      <span v-if="row.state === 'now'" class="rail__body">
        <i class="rail__fill" :style="{ height: `${row.pct}%` }" />
        <b class="rail__train" :style="{ top: `${row.pct}%` }" />
      </span>
      <span class="node node--top" :class="{ 'node--hot': row.state === 'now' }" />
      <span class="node node--bottom" :class="{ 'node--hot': row.state === 'now' }" />
      <span class="tie tie--top" />
      <span class="tie tie--bottom" />
    </div>

    <div class="stop__card" :style="{ '--tone': `var(--c-tone-${visual.tone})` }">
      <div class="card">
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
        <p v-if="hint" class="card__hint" :class="{ 'card__hint--mute': row.state === 'next' }">
          {{ hint }}
        </p>
        <component
          :is="visual.icon"
          :width="24"
          :height="24"
          class="card__glyph shrink-0"
          aria-hidden="true"
        />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
$time: 46px;
$rail: 18px;
$beat: 2px;

/* 站点行：时间列 | 轨道列 | 卡片列；轨道贯穿整行（含上下留白）保证线不断 */
.stop {
  --pad: 7px;

  display: grid;
  grid-template-columns: $time #{$rail} minmax(0, 1fr);
  align-items: stretch;
  padding: var(--pad) 0;

  &__time {
    position: relative;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    padding: calc(var(--pad) - 5.5px) 7px calc(var(--pad) - 5.5px) 0;
    text-align: right;
  }

  &__at {
    font-size: 11px;
    font-weight: 600;
    line-height: 1;
    font-variant-numeric: tabular-nums;
    color: var(--c-ink3);
    transition: color 0.2s ease;
  }

  &__period {
    font-size: 10px;
    font-weight: 600;
    line-height: 1;
    font-variant-numeric: tabular-nums;
    color: var(--c-ink5);
  }

  &__rail {
    position: relative;
  }

  &__card {
    position: relative;
    min-width: 0;
  }

  &--now {
    .stop__at {
      color: var(--tone);
    }

    .stop__period {
      color: color-mix(in srgb, var(--tone) 60%, var(--c-ink4));
    }
  }

  &--past {
    .stop__at,
    .stop__period {
      color: var(--c-ink5);
    }
  }
}

/* ---------------- 轨道：线、虚线、行进段 ---------------- */

.rail {
  position: absolute;
  inset: 0 auto 0 50%;
  width: $beat;
  margin-left: -($beat * 0.5);
  background: var(--c-line);

  .stop--later &,
  .stop--next & {
    background: repeating-linear-gradient(
      to bottom,
      color-mix(in srgb, var(--tone) 42%, var(--c-line)) 0 4px,
      transparent 4px 9px
    );
  }

  .stop--now & {
    background: color-mix(in srgb, var(--tone) 22%, var(--c-line));
  }

  .stop--past & {
    background: var(--c-line);
  }

  /* 首末站：线向两端淡出，像一段仍在延伸的线路 */
  .stop--first & {
    mask-image: linear-gradient(to bottom, transparent 0, #000 var(--pad));
  }

  .stop--last & {
    mask-image: linear-gradient(to top, transparent 0, #000 var(--pad));
  }
}

.rail__body {
  position: absolute;
  inset: var(--pad) auto var(--pad) 50%;
  width: $beat;
  margin-left: -($beat * 0.5);
}

.rail__fill {
  position: absolute;
  inset: 0 0 auto;
  display: block;
  background: var(--tone);
  transition: height 0.6s ease;
}

/* 当前站点上驶过的列车 */
.rail__train {
  position: absolute;
  left: 50%;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--tone);
  box-shadow: 0 0 0 2.5px var(--c-bg), 0 0 10px 2px color-mix(in srgb, var(--tone) 55%, transparent);
  transform: translate(-50%, -50%);
  transition: top 0.6s ease;
}

/* ---------------- 环形节点 + 横向挂载短杆 ---------------- */

.node {
  position: absolute;
  left: 50%;
  width: 12px;
  height: 12px;
  border: 2.5px solid var(--tone);
  border-radius: 50%;
  background: var(--c-surface);
  box-shadow: 0 0 0 2.5px var(--c-bg);
  transform: translate(-50%, -50%);

  &--top {
    top: var(--pad);
  }

  &--bottom {
    top: calc(100% - var(--pad));
  }

  .stop--past & {
    border-color: var(--c-ink5);
    background: var(--c-bg);
  }

  .stop--later &,
  .stop--next & {
    border-style: dashed;
    border-width: 2px;
    border-color: color-mix(in srgb, var(--tone) 55%, var(--c-ink5));
  }

  /* 当前站：呼吸光晕 */
  &--hot::after {
    content: "";
    position: absolute;
    inset: -3px;
    border: 1.5px solid color-mix(in srgb, var(--tone) 70%, transparent);
    border-radius: 50%;
    animation: nodePulse 2.2s cubic-bezier(0.22, 0.9, 0.24, 1) infinite;
  }
}

@keyframes nodePulse {
  0% {
    opacity: 0.85;
    transform: scale(0.72);
  }

  70%,
  100% {
    opacity: 0;
    transform: scale(2.1);
  }
}

.tie {
  position: absolute;
  left: 50%;
  width: 20px;
  height: 1.5px;
  margin-top: -0.75px;
  background: color-mix(in srgb, var(--tone) 55%, var(--c-line));

  &--top {
    top: var(--pad);
  }

  &--bottom {
    top: calc(100% - var(--pad));
  }

  .stop--past & {
    background: var(--c-line);
  }

  .stop--now & {
    background: var(--tone);
  }
}

/* ---------------- 车票卡：平铺底色 + 两侧打孔 ---------------- */

.card {
  position: relative;
  padding: 12px 14px 12px 16px;
  border-radius: 16px;
  background:
    linear-gradient(
      100deg,
      color-mix(in srgb, var(--tone) 9%, var(--c-surface)) 0%,
      color-mix(in srgb, var(--tone) 3%, var(--c-surface)) 62%
    );

  /* 左右中间的半月打孔，露出页面底色 */
  &::before,
  &::after {
    content: "";
    position: absolute;
    top: 50%;
    width: 12px;
    height: 12px;
    margin-top: -6px;
    border-radius: 50%;
    background: var(--c-bg);
  }

  &::before {
    left: -6px;
  }

  &::after {
    right: -6px;
  }

  &__name {
    padding-right: 26px;
    font-size: 15.5px;
    font-weight: 700;
    line-height: 1.28;
    letter-spacing: -0.01em;
  }

  &__info {
    display: flex;
    align-items: center;
    gap: 5px;
    margin-top: 4px;
    padding-right: 24px;
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
    position: absolute;
    top: 12px;
    right: 12px;
    color: color-mix(in srgb, var(--tone) 72%, transparent);
    transform: rotate(-4deg);
  }

  &__hint {
    display: inline-block;
    margin-top: 9px;
    padding: 3px 8px;
    border-radius: 8px;
    font-size: 11.5px;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
    color: var(--c-surface);
    background: var(--tone);

    &--mute {
      font-weight: 600;
      color: color-mix(in srgb, var(--tone) 75%, var(--c-ink));
      background: color-mix(in srgb, var(--tone) 12%, transparent);
    }
  }

  /* 已驶过：整站褪成浅灰 */
  .stop--past & {
    background: var(--c-surface2);
    box-shadow: none;

    .card__name {
      color: var(--c-ink4);
    }

    .card__info,
    .card__info svg,
    .card__glyph {
      color: var(--c-ink5);
    }
  }

  /* 当前站：一圈主题色细描边 */
  .stop--now & {
    box-shadow: inset 0 0 0 1.5px color-mix(in srgb, var(--tone) 32%, transparent);
  }
}
</style>
