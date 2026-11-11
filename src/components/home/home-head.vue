<script setup lang="ts" vapor>
import TopVeil from "@/components/common/top-veil.vue";
import { formatMonthDay, weekdayLabel } from "@/domain/calendar";

interface HeadLayer {
  iso: string;
  role: "cur" | "inc";
  date: Date;
  parts: string[];
  empty: boolean;
}

const props = defineProps<{
  layers: HeadLayer[];
  veil: number;
  morph: number;
  incomingDocks: boolean;
}>();

const clamp01 = (v: number) => Math.min(1, Math.max(0, v));

function smooth(from: number, to: number, v: number): number {
  const t = clamp01((v - from) / (to - from));
  return t * t * (3 - 2 * t);
}

function titleStyle(layer: HeadLayer): Record<string, string | number> {
  const p = props.morph;
  if (layer.role === "cur") {
    return {
      opacity: 1 - smooth(0, 0.55, p),
      transform: `translateY(${-10 * p}px)`,
    };
  }
  if (props.incomingDocks) {
    return {
      opacity: smooth(0.72, 1, p),
    };
  }
  return {
    opacity: smooth(0.25, 1, p),
    transform: `translateY(${(1 - p) * 12}px)`,
  };
}

function metaStyle(layer: HeadLayer): Record<string, string | number> {
  const p = props.morph;
  if (layer.role === "cur") {
    return {
      opacity: 1 - smooth(0, 0.5, p),
      transform: `translateY(${-6 * p}px)`,
    };
  }
  return {
    opacity: smooth(0.5, 1, p),
    transform: `translateY(${(1 - p) * 6}px)`,
  };
}
</script>

<template>
  <header class="head">
    <TopVeil :opacity="veil" />
    <div class="head__stack">
      <div v-for="layer in layers" :key="layer.iso" class="head__layer" :aria-hidden="layer.role === 'inc'">
        <h1 class="head__title" :style="titleStyle(layer)">
          {{ formatMonthDay(layer.date) }} <span>周{{ weekdayLabel(layer.date) }}</span>
        </h1>
        <p class="head__meta" :style="metaStyle(layer)">
          <template v-for="(part, i) in layer.parts" :key="part">
            <span v-if="i" class="head__rule" />
            <span :class="{ 'head__meta--mute': i === layer.parts.length - 1 && layer.empty }">{{ part }}</span>
          </template>
        </p>
      </div>
    </div>
  </header>
</template>

<style scoped lang="scss">
/* 顶部标题：吸顶 + 多层羽化；横向撑满到屏幕边，羽化不会在内边距处被截断 */
.head {
  position: sticky;
  top: 0;
  z-index: 5;
  margin: 0 calc(-1 * var(--home-pad, 20px));
  padding: calc(var(--safe-top) + 18px) var(--home-pad, 20px) 14px;

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
</style>
