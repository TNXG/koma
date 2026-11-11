<script setup lang="ts" vapor>
defineProps<{ week: number }>();
</script>

<template>
  <div class="week">
    <span class="week__rule" />第 {{ week }} 周<span class="week__rule" />
  </div>
</template>

<style scoped lang="scss">
.week {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0 28px;
  font-size: 11px;
  font-weight: 600;
  color: var(--c-ink5);

  &__rule {
    flex: 1;
    height: 1px;
    background: var(--c-line);
  }
}
</style>
