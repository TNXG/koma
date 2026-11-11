<script setup lang="ts" vapor>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import EyeLine from "~icons/mingcute/eye-line";
import EyeOffLine from "~icons/mingcute/eye-off-line";
import KButton from "@/components/common/k-button.vue";
import KLoader from "@/components/common/k-loader.vue";
import { useEduStore } from "@/stores/edu-store";
import { useTimetableStore } from "@/stores/timetable-store";

const router = useRouter();
const edu = useEduStore();
const timetable = useTimetableStore();

const username = ref("");
const password = ref("");
const captchaInput = ref("");
const remember = ref(true);
const passwordVisible = ref(false);
const isDev = import.meta.env.DEV;

const schoolName = edu.school.name.replace(/\s*统一身份认证.*$/, "");
const connecting = computed(() => edu.phase === "preparing");
const busy = computed(() => edu.phase === "logging-in" || edu.phase === "importing");
const canSubmit = computed(() =>
  edu.phase === "form"
  && username.value.trim() !== ""
  && password.value !== ""
  /* 自动识别时不需要手填验证码；识别不出来会切到 manual 再要求输入 */
  && (!edu.captcha || edu.captchaMode === "auto" || captchaInput.value.trim() !== ""),
);

onMounted(async () => {
  await edu.prepare();
  const saved = await edu.loadSaved();
  if (saved) {
    username.value = saved.username;
    password.value = saved.password;
  }
  if (edu.phase === "done") {
    await router.replace({ name: "semester" });
    return;
  }
  /* 存过密码且验证码能自动识别：直接续上，等于「一键更新课表」 */
  if (saved && edu.phase === "form" && edu.captchaMode === "auto")
    await submit();
});

async function submit(): Promise<void> {
  if (!canSubmit.value)
    return;
  await edu.login({
    username: username.value.trim(),
    password: password.value,
    captcha: captchaInput.value.trim(),
    remember: remember.value,
  });
  captchaInput.value = "";
  if (edu.phase === "done")
    await router.replace({ name: "semester" });
}

async function useDemo(): Promise<void> {
  const { demoSnapshot } = await import("@/dev/demo-timetable");
  timetable.setSnapshot(demoSnapshot());
  await router.replace({ name: "semester" });
}
</script>

<template>
  <main class="login">
    <div class="login__body">
      <h1 class="login__title">
        登录教务
      </h1>
      <p class="login__sub">
        {{ schoolName }} · 统一身份认证
      </p>

      <div v-if="connecting || edu.phase === 'importing'" class="state">
        <KLoader :size="20" />
        <p class="state__text">
          {{ edu.phase === 'importing' ? '正在读取课表' : '正在连接统一身份认证' }}
        </p>
      </div>

      <div v-else-if="edu.phase === 'error'" class="state state--card">
        <p class="state__title">
          暂时无法登录
        </p>
        <p class="state__text">
          {{ edu.message }}
        </p>
        <KButton variant="text" class="state__retry" @click="edu.prepare()">
          重试
        </KButton>
      </div>

      <form v-else class="card" @submit.prevent="submit">
        <label class="row">
          <span class="row__k">学号</span>
          <span class="row__v">
            <input
              v-model="username"
              class="input"
              inputmode="numeric"
              autocomplete="username"
              autocapitalize="off"
              spellcheck="false"
              :disabled="busy"
            >
          </span>
        </label>

        <label class="row">
          <span class="row__k">密码</span>
          <span class="row__v row__v--inline">
            <input
              v-model="password"
              class="input"
              :type="passwordVisible ? 'text' : 'password'"
              autocomplete="current-password"
              :disabled="busy"
            >
            <button
              type="button"
              class="peek"
              :aria-label="passwordVisible ? '隐藏密码' : '显示密码'"
              @click="passwordVisible = !passwordVisible"
            >
              <component
                :is="passwordVisible ? EyeOffLine : EyeLine"
                :width="17"
                :height="17"
                class="shrink-0"
                aria-hidden="true"
              />
            </button>
          </span>
        </label>

        <label v-if="edu.captcha && edu.captchaMode === 'manual'" class="row">
          <span class="row__k">验证码</span>
          <span class="row__v row__v--inline">
            <input
              v-model="captchaInput"
              class="input"
              inputmode="numeric"
              autocomplete="off"
              autocapitalize="off"
              spellcheck="false"
              placeholder="图中算式得数"
              :disabled="busy"
            >
            <button type="button" class="captcha" aria-label="换一张验证码" @click="edu.refreshCaptcha()">
              <img :src="edu.captcha" alt="验证码" class="captcha__img">
            </button>
          </span>
        </label>

        <div v-if="edu.canRemember" class="row row--plain">
          <span class="row__k row__k--wide">保存密码</span>
          <label class="switch">
            <input v-model="remember" type="checkbox" :disabled="busy">
            <span class="switch__track" />
          </label>
        </div>
      </form>

      <p v-if="edu.message && edu.phase === 'form'" class="error">
        {{ edu.message }}
      </p>

      <KButton v-if="isDev && !connecting && edu.phase !== 'importing'" variant="text" tone="mute" class="login__demo" @click="useDemo">
        用示例课表预览
      </KButton>
    </div>

    <footer class="login__foot">
      <KButton
        v-if="edu.phase === 'error'"
        @click="edu.prepare()"
      >
        重试
      </KButton>
      <KButton
        v-else
        :disabled="!canSubmit"
        :busy="busy"
        @click="submit"
      >
        {{ edu.phase === 'importing' ? '读取课表' : edu.phase === 'logging-in' ? '登录中' : '登录' }}
      </KButton>
    </footer>
  </main>
</template>

<style scoped lang="scss">
.login {
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

  &__demo {
    margin-top: 18px;
    margin-left: 2px;
  }

  &__foot {
    flex: none;
    padding: 16px 20px calc(var(--safe-bottom) + 22px);
  }
}

.state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 72px 0;
  color: var(--c-ink2);

  &--card {
    gap: 6px;
    margin-top: 24px;
    padding: 28px 20px;
    border-radius: 20px;
    background: var(--c-surface);
  }

  &__title {
    font-size: 13.5px;
    font-weight: 700;
    color: var(--c-ink);
  }

  &__text {
    font-size: 12.5px;
    font-weight: 500;
    color: var(--c-ink4);
  }

  &__retry {
    margin-top: 8px;
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
  align-items: baseline;
  padding: 12px 16px;

  & + & {
    border-top: 1px solid var(--c-line2);
  }

  &--plain {
    align-items: center;
  }

  &__k {
    flex: none;
    width: 62px;
    font-size: 12.5px;
    font-weight: 500;
    color: var(--c-ink4);

    &--wide {
      flex: 1;
      width: auto;
    }
  }

  &__v {
    flex: 1;
    min-width: 0;

    &--inline {
      display: flex;
      align-items: center;
      gap: 10px;
    }
  }
}

.input {
  width: 100%;
  border: none;
  outline: none;
  font-size: 14px;
  font-weight: 600;
  color: var(--c-ink);
  background: transparent;

  &::placeholder {
    font-weight: 500;
    color: var(--c-ink5);
  }
}

.peek {
  display: grid;
  flex: none;
  place-items: center;
  width: 28px;
  height: 28px;
  margin: -4px 0;
  color: var(--c-ink4);
}

.captcha {
  display: grid;
  flex: none;
  place-items: center;
  width: 104px;
  height: 34px;
  overflow: hidden;
  border-radius: 8px;
  background: #fff;
  transition: transform 0.15s ease;

  &:active {
    transform: scale(0.96);
  }

  &__img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
}

.switch {
  position: relative;
  flex: none;
  width: 46px;
  height: 28px;

  input {
    position: absolute;
    inset: 0;
    z-index: 1;
    width: 100%;
    height: 100%;
    margin: 0;
    opacity: 0;
  }

  &__track {
    display: block;
    width: 100%;
    height: 100%;
    border-radius: 999px;
    background: var(--c-line);
    transition: background 0.2s ease;

    &::after {
      content: "";
      position: absolute;
      top: 3px;
      left: 3px;
      width: 22px;
      height: 22px;
      border-radius: 50%;
      background: #fff;
      box-shadow: 0 1px 3px rgb(27 28 32 / 18%);
      transition: transform 0.24s cubic-bezier(0.3, 1.25, 0.5, 1);
    }
  }

  input:checked + .switch__track {
    background: var(--c-accent);

    &::after {
      transform: translateX(18px);
    }
  }
}

.error {
  margin-top: 10px;
  padding-left: 2px;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--c-danger);
}
</style>
