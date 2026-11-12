<script setup lang="ts" vapor>
import type { DayBlock } from "@/domain/home-timeline";
import CourseSlot from "@/components/home/course-slot.vue";
import GapStopRow from "@/components/home/gap-stop.vue";
import { formatMinutes, formatMonthDay, weekdayLabel } from "@/domain/calendar";

defineProps<{
  block: DayBlock;
  nowMinutes: number;
  viewIsToday: boolean;
}>();
</script>

<template>
  <section :data-anchor="block.anchor" class="day">
    <div class="day__head">
      <span v-if="block.relative" class="day__label" data-fade>{{ block.relative }}</span>
      <span class="day__date" :class="{ 'day__date--lead': !block.relative }" data-morph>
        <b>{{ formatMonthDay(block.date) }}</b> <span>周{{ weekdayLabel(block.date) }}</span>
      </span>
      <span v-if="block.courses.length" class="day__count" data-fade>
        {{ block.courses.length }} 站<template v-if="block.courses[0].start !== null">，{{
          formatMinutes(block.courses[0].start!) }} 发车</template>
      </span>
    </div>

    <div v-if="block.stops.length === 0" class="day__empty">
      今天没有课，线路休整中
    </div>

    <template v-for="(stop, i) in block.stops" :key="stop.kind === 'course' ? stop.course.id : `gap-${stop.start}`">
      <GapStopRow
        v-if="stop.kind === 'gap'"
        :stop="stop"
        :first="i === 0"
        :last="i === block.stops.length - 1"
      />
      <CourseSlot
        v-else
        :row="stop"
        :first="i === 0"
        :last="i === block.stops.length - 1"
        :now-minutes="nowMinutes"
        :view-is-today="viewIsToday"
      />
    </template>
  </section>
</template>

<style scoped lang="scss">
$day-gap: 28px;

.day {
  padding-bottom: $day-gap;

  &__head {
    position: relative;
    display: flex;
    align-items: baseline;
    gap: 10px;
    padding-bottom: 10px;
  }

  &__label,
  &__date,
  &__count {
    will-change: opacity, transform;
  }

  &__label {
    font-size: 17px;
    font-weight: 800;
    line-height: 1;
    letter-spacing: -0.02em;
  }

  &__date {
    font-size: 12.5px;
    line-height: 1.2;
    white-space: nowrap;
    color: var(--c-ink3);

    b {
      font-weight: 700;
    }

    span {
      font-weight: 600;
      color: var(--c-ink5);
    }

    &--lead {
      font-size: 17px;
      letter-spacing: -0.02em;
      color: var(--c-ink);

      b {
        font-weight: 800;
      }
    }
  }

  &__count {
    margin-left: auto;
    font-size: 12px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    color: var(--c-ink5);
  }

  &__empty {
    padding: 2px 0 6px;
    font-size: 13.5px;
    font-weight: 600;
    color: var(--c-ink4);
  }
}
</style>
