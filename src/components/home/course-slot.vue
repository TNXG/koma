<script setup lang="ts" vapor>
import type { Row } from "@/domain/home-timeline";
import { computed } from "vue";
import MapPinLine from "~icons/mingcute/map-pin-line";
import User3Line from "~icons/mingcute/user-3-line";
import { formatDuration, formatMinutes } from "@/domain/calendar";
import { courseVisual } from "@/domain/course-visual";

const props = defineProps<{
  row: Row;
  first: boolean;
  last: boolean;
  nowMinutes: number;
  viewIsToday: boolean;
}>();

const visual = computed(() => courseVisual(props.row.course.name));

const hint = computed<string | null>(() => {
  const { row } = props;
  if (row.state === "now" && row.end !== null)
    return `上课中，现在 ${formatMinutes(props.nowMinutes)}，还剩 ${formatDuration(row.end - props.nowMinutes)}`;
  if (row.state === "next" && row.start !== null) {
    const gap = row.start - props.nowMinutes;
    if (props.viewIsToday && gap > 0 && gap <= 60)
      return `还有 ${formatDuration(gap)}，${formatMinutes(row.start)} 开始`;
    return `下一节，${formatMinutes(row.start)} 开始`;
  }
  return null;
});
</script>

<template>
  <div class="slot">
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

    <div class="slot__rail" :class="{ 'slot__rail--first': first, 'slot__rail--last': last }">
      <div class="slot__line">
        <i v-if="row.state === 'past'" class="slot__fill" />
        <template v-else-if="row.state === 'now'">
          <i class="slot__fill" :style="{ height: `${row.pct}%` }" />
          <b class="slot__dot" :style="{ top: `${row.pct}%` }" />
        </template>
      </div>
    </div>

    <div class="slot__main" :class="{ 'is-past': row.state === 'past' }">
      <div class="card" :class="`card--${row.state}`" :style="{ '--tone': `var(--c-tone-${visual.tone})` }">
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
            :is="visual.icon"
            :width="28"
            :height="28"
            class="card__glyph shrink-0"
            aria-hidden="true"
          />
        </div>
        <p v-if="hint" class="card__hint" :class="{ 'card__hint--mute': row.state === 'next' }">
          {{ hint }}
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
$time: 44px;
$rail: 2px;
$gap: 12px;
$slot-gap: 6px;
/* 首尾两段再向卡片外探出的距离：竖线要略长于卡片，不能和卡边齐平显得被裁掉 */
$rail-overhang: 2px;

/* 一节课：时间列 + 竖线 + 卡片；竖线首尾各探出卡片 $slot-gap + $rail-overhang，中间连续 */
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
      margin-top: -($slot-gap + $rail-overhang);
    }

    &--last {
      margin-bottom: -($slot-gap + $rail-overhang);
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

/* 课程卡：学科色渐变 + 同色柔光投影 + 进行时进度填充 */
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
</style>
