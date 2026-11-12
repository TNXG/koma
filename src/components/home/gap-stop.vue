<script setup lang="ts" vapor>
import type { GapStop } from "@/domain/home-timeline";
import { computed } from "vue";
import { formatDuration } from "@/domain/calendar";

/** 课间空闲站：轨道画成波浪线，旁边一行小字标时长 */
const props = defineProps<{ stop: GapStop; first: boolean; last: boolean }>();

/** 高度随时长微涨，但不让长空档撑满一屏 */
const height = computed(() => Math.round(Math.min(92, Math.max(40, 22 + props.stop.minutes * 0.16))));
</script>

<template>
  <div class="gap" :class="`gap--${stop.state}`" :style="{ '--h': `${height}px` }">
    <span class="gap__time" />
    <span class="gap__rail" :class="{ 'gap__rail--first': first, 'gap__rail--last': last }">
      <svg class="wave" viewBox="0 0 18 100" preserveAspectRatio="none" aria-hidden="true">
        <path
          d="M9 0 C2 12 16 26 9 40 C2 54 16 68 9 80 C5 88 7 96 9 100"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          vector-effect="non-scaling-stroke"
        />
      </svg>
    </span>
    <p class="gap__label">
      空闲 {{ formatDuration(stop.minutes) }}
    </p>
  </div>
</template>

<style scoped lang="scss">
$time: 46px;
$rail: 18px;

.gap {
  display: grid;
  grid-template-columns: $time #{$rail} minmax(0, 1fr);
  align-items: center;
  height: var(--h);

  &__rail {
    position: relative;
    height: 100%;
    color: var(--c-ink5);

    &--first {
      mask-image: linear-gradient(to bottom, transparent 0, #000 12px);
    }

    &--last {
      mask-image: linear-gradient(to top, transparent 0, #000 12px);
    }
  }

  &__label {
    padding-left: 10px;
    font-size: 11.5px;
    font-weight: 600;
    letter-spacing: 0.01em;
    color: var(--c-ink4);
  }

  &--past {
    .gap__rail {
      color: var(--c-line);
    }

    .gap__label {
      color: var(--c-ink5);
    }
  }

  &--now {
    .gap__rail {
      color: var(--c-accent2);
    }

    .gap__label {
      color: var(--c-accent);
    }
  }
}

.wave {
  position: absolute;
  inset: 0 auto 0 50%;
  width: 18px;
  height: 100%;
  transform: translateX(-50%);
}
</style>
