<script setup lang="ts" vapor>
import KLoader from "./k-loader.vue";

withDefaults(defineProps<{
  variant?: "primary" | "text";
  tone?: "brand" | "mute" | "danger";
  disabled?: boolean;
  busy?: boolean;
}>(), {
  variant: "primary",
  tone: "brand",
  disabled: false,
  busy: false,
});
</script>

<template>
  <button
    v-if="variant === 'text'"
    class="text-btn"
    :class="`text-btn--${tone}`"
    :disabled="disabled || busy"
  >
    <span class="btn__slot" :class="{ 'is-hidden': busy }"><slot /></span>
    <KLoader v-if="busy" :size="14" class="text-btn__loader" />
  </button>
  <button
    v-else
    class="btn"
    :class="{ 'btn--danger': tone === 'danger' }"
    type="button"
    :disabled="disabled || busy"
  >
    <span class="btn__slot" :class="{ 'is-hidden': busy }"><slot /></span>
    <KLoader v-if="busy" :size="18" class="btn__loader" />
  </button>
</template>

<style scoped lang="scss">
.btn {
  position: relative;
  display: grid;
  place-items: center;
  width: 100%;
  padding: 15px;
  border-radius: 18px;
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  background: var(--c-accent);
  transition:
    transform 0.15s ease,
    background 0.2s ease;

  &:active:not(:disabled) {
    transform: scale(0.985);
  }

  &:disabled {
    color: var(--c-ink4);
    background: var(--c-line);
  }

  &--danger:not(:disabled) {
    background: var(--c-danger);
  }

  &__loader,
  &__slot {
    grid-area: 1 / 1;
  }

  &__slot {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
  }

  &__loader {
    color: currentcolor;
  }
}

.text-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
  transition: opacity 0.15s ease;

  &:active:not(:disabled) {
    opacity: 0.6;
  }

  &:disabled {
    opacity: 0.35;
  }

  &--brand {
    color: var(--c-accent);
  }

  &--mute {
    color: var(--c-ink3);
  }

  &--danger {
    color: var(--c-danger);
  }

  .btn__slot {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: 10px;
  }
}

.is-hidden {
  opacity: 0;
}
</style>
