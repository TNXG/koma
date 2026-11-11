import type { Diagnostic } from "@/edu/contracts/protocol";
import { defineStore } from "pinia";
import { ref } from "vue";
import { captchaAnswer } from "@/domain/captcha";
import { createCredentialStore } from "@/platform/credentials";
import { createOcr } from "@/platform/ocr";
import { edu } from "@/services/edu";
import { useTimetableStore } from "./timetable-store";

export type EduPhase = "idle" | "preparing" | "form" | "logging-in" | "importing" | "done" | "error";
/** auto：本机识别验证码并自动登记；manual：识别不出来或连续失败，改人工输入 */
export type CaptchaMode = "auto" | "manual";

/** 自动换验证码的轮数上限：一次 begin 就是对教务的一次请求，次数要压住 */
const MAX_CAPTCHA_ROUNDS = 3;
/** 预处理视图数，与原生 OcrEngine.VIEWS 一致：0 原图、1 裁掉两端噪声 */
const OCR_VIEWS = 2;

export const useEduStore = defineStore("edu", () => {
  const school = edu.listSchools()[0];
  const credentials = createCredentialStore();
  const ocr = createOcr();

  const phase = ref<EduPhase>("idle");
  const captcha = ref<string | null>(null);
  const captchaMode = ref<CaptchaMode>("auto");
  const message = ref("");
  const diagnostics = ref<Diagnostic[]>([]);
  const canRemember = ref(false);

  async function prepare(): Promise<void> {
    phase.value = "preparing";
    message.value = "";
    canRemember.value = await credentials.available();
    const result = await edu.prepareLogin(school.id);
    if (result.kind === "unavailable") {
      phase.value = "error";
      message.value = result.message;
      return;
    }
    if (result.kind === "ready") {
      await importTimetable();
      return;
    }
    captcha.value = result.captcha;
    captchaMode.value = result.captcha && await ocr.available()
      ? "auto"
      : "manual";
    phase.value = "form";
  }

  async function loadSaved(): Promise<{ username: string; password: string } | null> {
    if (!canRemember.value)
      return null;
    const saved = await credentials.load(school.id);
    return saved.kind === "found"
      ? { username: saved.username, password: saved.password }
      : null;
  }

  async function refreshCaptcha(): Promise<void> {
    try {
      captcha.value = await edu.refreshCaptcha(school.id);
    } catch (e) {
      message.value = e instanceof Error
        ? e.message
        : "验证码加载失败";
    }
  }

  /** 本机识别当前验证码 → 算式答案；两个视图都读不出合法算式才返回 null */
  async function readCaptcha(): Promise<string | null> {
    const image = captcha.value;
    if (!image)
      return "";
    for (let view = 0; view < OCR_VIEWS; view++) {
      try {
        const answer = captchaAnswer((await ocr.recognize(image, view)).text);
        if (answer !== null)
          return answer;
      } catch {
        /* 单视图失败换下一个；全失败交给人工 */
      }
    }
    return null;
  }

  function toManual(text: string): void {
    captchaMode.value = "manual";
    message.value = text;
    phase.value = "form";
  }

  async function login(input: { username: string; password: string; captcha: string; remember: boolean }): Promise<void> {
    if (phase.value !== "form")
      return;
    phase.value = "logging-in";
    message.value = "";

    let code: string | null = captcha.value
      ? captchaMode.value === "manual"
        ? input.captcha
        : null
      : "";
    let rounds = 0;

    for (;;) {
      if (code === null) {
        code = await readCaptcha();
        if (code === null) {
          toManual("验证码没认出来，请手动输入");
          return;
        }
      }
      const result = await edu.login(school.id, { username: input.username, password: input.password, captcha: code });
      if (result.kind === "ok")
        break;
      if (!result.captcha) {
        message.value = result.message;
        phase.value = "form";
        return;
      }
      rounds++;
      if (captchaMode.value !== "auto" || rounds >= MAX_CAPTCHA_ROUNDS) {
        await refreshCaptcha();
        toManual("验证码不对，请手动输入");
        return;
      }
      /* 只是验证码错：换一张再来，账号密码不用重输 */
      await refreshCaptcha();
      code = null;
    }

    if (canRemember.value) {
      if (input.remember)
        await credentials.save({ schoolId: school.id, username: input.username, password: input.password });
      else
        await credentials.remove(school.id);
    }
    await importTimetable();
  }

  async function importTimetable(): Promise<void> {
    phase.value = "importing";
    const result = await edu.importTimetable(school.id);
    if (result.kind === "fail") {
      phase.value = "error";
      message.value = result.message;
      return;
    }
    diagnostics.value = result.diagnostics;
    useTimetableStore().setSnapshot(result.timetable);
    phase.value = "done";
  }

  async function logout(): Promise<void> {
    await edu.logout(school.id);
    await credentials.remove(school.id);
    useTimetableStore().clear();
    phase.value = "idle";
    captcha.value = null;
    captchaMode.value = "auto";
    message.value = "";
  }

  return {
    school,
    phase,
    captcha,
    captchaMode,
    message,
    diagnostics,
    canRemember,
    prepare,
    loadSaved,
    refreshCaptcha,
    login,
    importTimetable,
    logout,
  };
});
