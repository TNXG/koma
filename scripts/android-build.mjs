// 跨平台构建 debug APK：Windows 走 gradlew.bat，其余走 ./gradlew。
// 失败（非零退出或找不到产物）时以非零码退出，成功打印 APK 绝对路径。
import { spawnSync } from "node:child_process";
import { existsSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const androidDir = join(root, "android");
const apkPath = join(androidDir, "app", "build", "outputs", "apk", "debug", "app-debug.apk");

if (!existsSync(androidDir)) {
  console.error("android/ 不存在，请先执行 pnpm exec cap add android");
  process.exit(1);
}

const command = process.platform === "win32"
  ? "gradlew.bat assembleDebug"
  : "./gradlew assembleDebug";
const result = spawnSync(command, {
  cwd: androidDir,
  stdio: "inherit",
  shell: true,
});

if (result.error) {
  console.error(`Gradle 启动失败：${result.error.message}`);
  process.exit(1);
}
if (result.status !== 0) {
  console.error(`Gradle 构建失败，退出码 ${result.status}`);
  process.exit(1);
}
if (!existsSync(apkPath)) {
  console.error(`构建结束但未找到 APK：${apkPath}`);
  process.exit(1);
}
console.log(`APK: ${apkPath}`);
