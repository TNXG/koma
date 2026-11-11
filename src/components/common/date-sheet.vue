<script setup lang="ts" vapor>
import { computed, ref, watch } from "vue";
import LeftLine from "~icons/mingcute/left-line";
import RightLine from "~icons/mingcute/right-line";
import {
  Drawer,
  DrawerContent,
  DrawerDescription,
  DrawerTitle,
} from "@/components/ui/drawer";
import { addDays, mondayOf, sameDay } from "@/domain/calendar";

/** 底部月历：选一天，落到所在周的周一（开学日 = 第 1 周周一） */
const props = defineProps<{ open: boolean; value: Date; hint?: string }>();
const emit = defineEmits<{ pick: [date: Date]; close: [] }>();

const month = ref(new Date(props.value.getFullYear(), props.value.getMonth(), 1));
watch(() => props.open, (open) => {
  if (open)
    month.value = new Date(props.value.getFullYear(), props.value.getMonth(), 1);
});

const cells = computed(() => Array.from({ length: 42 }, (_, i) => addDays(mondayOf(month.value), i)));

function shift(delta: number): void {
  month.value = new Date(month.value.getFullYear(), month.value.getMonth() + delta, 1);
}

function pick(day: Date): void {
  emit("pick", day);
  emit("close");
}
</script>

<template>
  <Drawer :open="open" @update:open="(v: boolean) => !v && emit('close')">
    <DrawerContent class="mx-auto w-full max-w-107.5 rounded-t-xl border-none px-4 pb-[calc(var(--safe-bottom)+32px)]">
      <DrawerTitle class="sr-only">
        选择日期
      </DrawerTitle>
      <DrawerDescription class="sr-only">
        {{ hint ?? '点任意一天，自动取那一周的周一' }}
      </DrawerDescription>

      <div class="cal__bar">
        <button class="cal__nav" aria-label="上个月" @click="shift(-1)">
          <LeftLine :width="16" :height="16" class="shrink-0" aria-hidden="true" />
        </button>
        <p class="cal__month">
          {{ month.getFullYear() }}年{{ month.getMonth() + 1 }}月
        </p>
        <button class="cal__nav" aria-label="下个月" @click="shift(1)">
          <RightLine :width="16" :height="16" class="shrink-0" aria-hidden="true" />
        </button>
      </div>

      <div class="cal__grid">
        <span v-for="d in ['一', '二', '三', '四', '五', '六', '日']" :key="d" class="cal__dow">{{ d }}</span>
        <button
          v-for="day in cells" :key="day.getTime()" class="cal__day" :class="{
            'cal__day--out': day.getMonth() !== month.getMonth(),
            'cal__day--week': sameDay(mondayOf(day), props.value),
            'cal__day--on': sameDay(day, props.value),
            'cal__day--today': sameDay(day, new Date()),
          }" @click="pick(day)"
        >
          {{ day.getDate() }}
        </button>
      </div>

      <p class="cal__hint">
        {{ hint ?? '点任意一天，自动取那一周的周一' }}
      </p>
    </DrawerContent>
  </Drawer>
</template>

<style scoped lang="scss">
.cal {
  &__bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 4px 4px;
  }

  &__month {
    font-size: 15px;
    font-weight: 700;
    letter-spacing: -0.01em;
  }

  &__nav {
    display: grid;
    place-items: center;
    width: 36px;
    height: 36px;
    border-radius: 999px;
    color: var(--c-ink2);

    &:active {
      background: var(--c-surface2);
    }
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    row-gap: 2px;
    margin-top: 4px;
  }

  &__dow {
    padding: 6px 0;
    font-size: 11px;
    font-weight: 600;
    text-align: center;
    color: var(--c-ink5);
  }

  &__day {
    position: relative;
    height: 42px;
    border-radius: 13px;
    font-size: 15px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    color: var(--c-ink);

    &--out {
      font-weight: 500;
      color: var(--c-ink5);
    }

    &--week:not(.cal__day--on) {
      border-radius: 0;
      background: var(--c-surface2);
    }

    &--on {
      color: var(--c-accent);
      background: var(--c-accent-soft);
      box-shadow: inset 0 0 0 1.5px var(--c-accent2);
    }

    &--today::after {
      content: "";
      position: absolute;
      bottom: 6px;
      left: 50%;
      width: 4px;
      height: 4px;
      margin-left: -2px;
      border-radius: 50%;
      background: var(--c-accent);
    }
  }

  &__hint {
    margin-top: 8px;
    font-size: 11.5px;
    font-weight: 500;
    text-align: center;
    color: var(--c-ink4);
  }
}
</style>
