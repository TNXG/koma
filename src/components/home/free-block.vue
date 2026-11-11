<script setup lang="ts" vapor>
import type { FreeBlock } from "@/domain/home-timeline";
import { formatMonthDay } from "@/domain/calendar";

defineProps<{ block: FreeBlock }>();
</script>

<template>
  <section :data-anchor="block.anchor" class="free">
    <p class="free__name">
      {{ block.label }}
    </p>
    <p class="free__sub">
      {{ formatMonthDay(block.start) }}<template v-if="block.days > 1">
        —{{ formatMonthDay(block.end) }}
      </template> · {{ block.days }} 天
    </p>
  </section>
</template>

<style scoped lang="scss">
.free {
  margin-bottom: 28px;
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
</style>
