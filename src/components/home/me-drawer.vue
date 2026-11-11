<script setup lang="ts" vapor>
import { computed } from "vue";
import CalendarLine from "~icons/mingcute/calendar-line";
import ExitLine from "~icons/mingcute/exit-line";
import Refresh2Line from "~icons/mingcute/refresh-2-line";
import KButton from "@/components/common/k-button.vue";
import { Drawer, DrawerContent, DrawerDescription, DrawerTitle } from "@/components/ui/drawer";
import { formatMonthDay, parseLocalDate } from "@/domain/calendar";

const props = defineProps<{
  open: boolean;
  studentName?: string;
  semesterName: string;
  startDate: string;
}>();

const emit = defineEmits<{
  "update:open": [open: boolean];
  "reimport": [];
  "editStart": [];
  "logout": [];
}>();

const startLabel = computed(() => formatMonthDay(parseLocalDate(props.startDate)));
</script>

<template>
  <Drawer :open="open" @update:open="(v: boolean) => emit('update:open', v)">
    <DrawerContent class="me mx-auto w-full max-w-[430px] border-none px-3 pb-[calc(var(--safe-bottom)+32px)]">
      <DrawerTitle class="sr-only">
        我的
      </DrawerTitle>
      <DrawerDescription class="sr-only">
        {{ studentName ?? '同学' }} · {{ semesterName }}
      </DrawerDescription>
      <div class="me__who">
        <p class="me__name">
          {{ studentName ?? '同学' }}
        </p>
        <p class="me__sub">
          {{ semesterName }}
        </p>
      </div>
      <KButton variant="text" tone="mute" class="me__row" @click="emit('reimport')">
        <Refresh2Line :width="17" :height="17" class="shrink-0" aria-hidden="true" />
        重新导入课表
      </KButton>
      <KButton variant="text" tone="mute" class="me__row" @click="emit('editStart')">
        <CalendarLine :width="17" :height="17" class="shrink-0" aria-hidden="true" />
        修改开学时间
        <span class="me__value">{{ startLabel }}</span>
      </KButton>
      <KButton variant="text" tone="danger" class="me__row" @click="emit('logout')">
        <ExitLine :width="17" :height="17" class="shrink-0" aria-hidden="true" />
        退出登录并清除课表
      </KButton>
    </DrawerContent>
  </Drawer>
</template>

<style scoped lang="scss">
.me {
  display: flex;
  flex-direction: column;
  gap: 2px;

  &__who {
    padding: 4px 8px 14px;
  }

  &__name {
    font-size: 17px;
    font-weight: 800;
    letter-spacing: -0.02em;
  }

  &__sub {
    margin-top: 2px;
    font-size: 12px;
    font-weight: 500;
    color: var(--c-ink4);
  }

  &__row {
    display: flex;
    width: 100%;
    align-items: center;
    gap: 10px;
    padding: 13px 10px;
    border-radius: 12px;
    font-size: 14px;
    font-weight: 500;
    justify-content: flex-start;

    &:active {
      background: var(--c-bg);
    }
  }

  &__value {
    margin-left: auto;
    font-size: 12px;
    font-weight: 500;
    color: var(--c-ink4);
  }
}
</style>
