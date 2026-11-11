<script setup lang="ts">
// 占位首页：仅验证基础工程（入口/路由/样式）可运行，正式页面按 UI 设计实现
</script>

<template>
  <main class="home">
    <h1 class="home__title">
      Koma Pro
    </h1>
    <p class="home__hint">
      基础工程已就绪，页面等待 UI 设计。
    </p>
  </main>
</template>

<style scoped lang="scss">
.home {
  display: grid;
  place-content: center;
  min-height: 100dvh;
  gap: 8px;
  text-align: center;

  &__title {
    margin: 0;
    font-size: 24px;
  }

  &__hint {
    margin: 0;
    color: var(--koma-text-secondary);
    font-size: 14px;
  }
}
</style>
