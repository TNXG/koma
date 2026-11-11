<script setup lang="ts" vapor>
/**
 * 顶部羽化层：多层 backdrop-filter + 渐变遮罩，越靠上越实、往下连续过渡。
 * 每层 [模糊半径, 遮罩开始消退 %, 完全消退 %]；透明度写在子层上，
 * 父层保持不透明，否则会成为 backdrop root 采不到底下内容。
 */
withDefaults(defineProps<{ opacity?: number; feather?: number }>(), { opacity: 1, feather: 28 });

const BANDS: [number, number, number][] = [
  [1, 70, 100],
  [2, 56, 92],
  [4, 42, 78],
  [8, 26, 62],
  [14, 8, 46],
];

const VEIL = "linear-gradient(to bottom, rgb(var(--c-bg-rgb) / .93) 0%, rgb(var(--c-bg-rgb) / .86) 58%, rgb(var(--c-bg-rgb) / .32) 86%, rgb(var(--c-bg-rgb) / 0) 100%)";
</script>

<template>
  <div class="veil" :style="{ '--feather': `${feather}px` }" aria-hidden="true">
    <div
      v-for="[radius, from, to] in BANDS"
      :key="radius"
      class="veil__band"
      :style="{
        height: `${to}%`,
        opacity,
        backdropFilter: `blur(${radius}px)`,
        WebkitBackdropFilter: `blur(${radius}px)`,
        maskImage: `linear-gradient(to bottom, #000 0%, #000 ${(from / to) * 100}%, rgba(0,0,0,0) 100%)`,
        WebkitMaskImage: `linear-gradient(to bottom, #000 0%, #000 ${(from / to) * 100}%, rgba(0,0,0,0) 100%)`,
      }"
    />
    <div class="veil__tint" :style="{ background: VEIL, opacity }" />
  </div>
</template>

<style scoped lang="scss">
.veil {
  position: absolute;
  inset: 0 0 calc(-1 * var(--feather));
  z-index: -1;
  pointer-events: none;

  &__band,
  &__tint {
    position: absolute;
    inset: 0 0 auto;
  }
}
</style>
