<script setup lang="ts" vapor>
withDefaults(defineProps<{ size?: number }>(), { size: 16 });
</script>

<template>
  <svg
    class="loader-spin" :width="size" :height="size" viewBox="0 0 16 16" stroke-linejoin="round"
    style="color: currentcolor" aria-hidden="true"
  >
    <path d="M8 0V4" stroke="currentColor" stroke-width="1.5" opacity="1" />
    <path d="M8 16V12" stroke="currentColor" stroke-width="1.5" opacity=".5" />
    <path d="M3.29773 1.52783L5.64887 4.7639" stroke="currentColor" stroke-width="1.5" opacity=".9" />
    <path d="M12.7023 1.52783L10.3511 4.7639" stroke="currentColor" stroke-width="1.5" opacity=".1" />
    <path d="M12.7023 14.472L10.3511 11.236" stroke="currentColor" stroke-width="1.5" opacity=".4" />
    <path d="M3.29773 14.472L5.64887 11.236" stroke="currentColor" stroke-width="1.5" opacity=".6" />
    <path d="M15.6085 5.52783L11.8043 6.7639" stroke="currentColor" stroke-width="1.5" opacity=".2" />
    <path d="M0.391602 10.472L4.19583 9.23598" stroke="currentColor" stroke-width="1.5" opacity=".7" />
    <path d="M15.6085 10.4722L11.8043 9.2361" stroke="currentColor" stroke-width="1.5" opacity=".3" />
    <path d="M0.391602 5.52783L4.19583 6.7639" stroke="currentColor" stroke-width="1.5" opacity=".8" />
  </svg>
</template>
