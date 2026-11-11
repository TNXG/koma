<script setup lang="ts" vapor>
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import RightLine from "~icons/mingcute/right-line";
import DateSheet from "@/components/common/date-sheet.vue";
import KButton from "@/components/common/k-button.vue";
import { daysBetween, formatMonthDay, mondayOf, parseLocalDate, sameDay, startOfDay, toIsoDate } from "@/domain/calendar";
import { useTimetableStore } from "@/stores/timetable-store";

const router = useRouter();
const timetable = useTimetableStore();
const semester = timetable.snapshot!.semester;
const today = startOfDay(new Date());

const detected = parseLocalDate(semester.startDate);
const selected = ref(timetable.startDate
  ? parseLocalDate(timetable.startDate)
  : detected);
const sheetOpen = ref(false);

const isDetected = computed(() => sameDay(selected.value, detected));
const weekOfToday = computed(() => Math.floor(daysBetween(selected.value, today) / 7) + 1);
const todayText = computed(() => {
  const week = weekOfToday.value;
  if (week < 1)
    return `距离开学 ${-daysBetween(today, selected.value)} 天`;
  if (week > semester.totalWeeks)
    return "学期已结束";
  return `第 ${week} 周`;
});

function pick(day: Date): void {
  selected.value = mondayOf(day);
}

async function confirm(): Promise<void> {
  timetable.confirmStartDate(toIsoDate(selected.value));
  await router.replace({ name: "home" });
}
</script>

<template>
  <main class="semester">
    <div class="semester__body">
      <h1 class="semester__title">
        设置开学时间
      </h1>
      <p class="semester__sub">
        第 1 周的周一，决定每天显示哪些课
      </p>

      <div class="card">
        <button class="row row--link" @click="sheetOpen = true">
          <span class="row__k">开学</span>
          <span class="row__v">
            <span class="row__value">{{ toIsoDate(selected).replaceAll("-", "/") }}</span>
            <span class="row__desc">
              第 1 周 · {{ formatMonthDay(selected) }} 周一
              <span v-if="isDetected"> · 来自教务</span>
            </span>
          </span>
          <RightLine :width="14" :height="14" class="row__chev shrink-0" aria-hidden="true" />
        </button>

        <div class="row">
          <span class="row__k">学期</span>
          <span class="row__v">
            <span class="row__value">{{ semester.name }}</span>
          </span>
        </div>

        <div class="row">
          <span class="row__k">总周数</span>
          <span class="row__v">
            <span class="row__value">{{ semester.totalWeeks }} 周</span>
          </span>
        </div>

        <div class="row">
          <span class="row__k">今天</span>
          <span class="row__v">
            <span class="row__value">{{ todayText }}</span>
          </span>
        </div>
      </div>

      <KButton v-if="!isDetected" variant="text" tone="mute" class="semester__reset" @click="pick(detected)">
        恢复教务日期（{{ formatMonthDay(detected) }}）
      </KButton>
    </div>

    <footer class="semester__foot">
      <KButton @click="confirm">
        开始使用
      </KButton>
    </footer>

    <DateSheet :open="sheetOpen" :value="selected" @pick="pick" @close="sheetOpen = false" />
  </main>
</template>

<style scoped lang="scss">
.semester {
  display: flex;
  flex-direction: column;
  height: 100%;

  &__body {
    flex: 1;
    min-height: 0;
    padding: calc(var(--safe-top) + 34px) 20px 0;
    overflow-y: auto;
  }

  &__title {
    font-size: 26px;
    font-weight: 800;
    line-height: 1.2;
    letter-spacing: -0.02em;
  }

  &__sub {
    margin-top: 6px;
    font-size: 12.5px;
    font-weight: 500;
    color: var(--c-ink4);
  }

  &__reset {
    margin-top: 16px;
    margin-left: 2px;
  }

  &__foot {
    flex: none;
    padding: 16px 20px calc(var(--safe-bottom) + 22px);
  }
}

.card {
  margin-top: 22px;
  overflow: hidden;
  border-radius: 20px;
  background: var(--c-surface);
}

.row {
  display: flex;
  align-items: center;
  padding: 13px 16px;
  text-align: left;

  & + & {
    border-top: 1px solid var(--c-line2);
  }

  &--link:active {
    background: var(--c-bg);
  }

  &__k {
    flex: none;
    width: 62px;
    font-size: 12.5px;
    font-weight: 500;
    color: var(--c-ink4);
  }

  &__v {
    flex: 1;
    min-width: 0;
  }

  &__value {
    display: block;
    font-size: 14px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    color: var(--c-ink);
  }

  &__desc {
    display: block;
    margin-top: 2px;
    font-size: 11.5px;
    font-weight: 500;
    color: var(--c-ink4);
  }

  &__chev {
    flex: none;
    margin-left: 8px;
    color: var(--c-ink5);
  }
}
</style>
