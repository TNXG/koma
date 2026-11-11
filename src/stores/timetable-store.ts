import type { TimetableSnapshot } from "@/types/timetable";
import { StorageSerializers, useStorage } from "@vueuse/core";
import { defineStore } from "pinia";
import { computed } from "vue";

/**
 * 当前学期课表 + 用户确认的开学日期。
 * SQLite 仓储接入前先放 localStorage（快照不含任何凭据）。
 */
export const useTimetableStore = defineStore("timetable", () => {
  const snapshot = useStorage<TimetableSnapshot | null>("koma.snapshot", null, undefined, {
    serializer: StorageSerializers.object,
  });
  /** 用户确认过的第 1 周周一；空串 = 尚未确认 */
  const startDate = useStorage("koma.startDate", "");

  const configured = computed(() => snapshot.value !== null && startDate.value !== "");

  function setSnapshot(next: TimetableSnapshot): void {
    const sameTerm = snapshot.value?.term.xnm === next.term.xnm && snapshot.value?.term.xqm === next.term.xqm;
    snapshot.value = next;
    if (!sameTerm)
      startDate.value = "";
  }

  function confirmStartDate(iso: string): void {
    startDate.value = iso;
  }

  function clear(): void {
    snapshot.value = null;
    startDate.value = "";
  }

  return {
    snapshot,
    startDate,
    configured,
    setSnapshot,
    confirmStartDate,
    clear,
  };
});
