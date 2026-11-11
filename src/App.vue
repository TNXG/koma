<template>
  <div class="shell">
    <RouterView />
  </div>
</template>

<style scoped lang="scss">
.shell {
  position: relative;
  width: 100%;
  max-width: 430px;
  height: 100dvh;
  margin: 0 auto;
  overflow: hidden;
  background: var(--c-bg);
  color: var(--c-ink);
}
</style>
