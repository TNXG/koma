#!/usr/bin/env node
/**
 * 首次构建拉取验证码 OCR 模型（13MB，不入库）。
 *
 * 模型是 ddddocr 的默认 OCR 模型（common_old.onnx，MIT），随 APK 分发；
 * 这里按 sha256 校验后原子落盘，上游文件被替换时构建会失败而不是静默换模型。
 * 已存在且校验通过时直接跳过，不产生网络请求。
 */
import { createHash } from "node:crypto";
import { mkdir, readFile, rename, rm, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const SOURCE = "https://raw.githubusercontent.com/sml2h3/ddddocr/master/ddddocr/common_old.onnx";
const SHA256 = "b8f2ad9cbc1f2e3922a6cb9459e30824e7e2467f3fb4fd61420640e34ea0bf68";
const SIZE = 13_606_051;

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const target = resolve(root, "android/app/src/main/assets/ocr/captcha.onnx");
const temp = `${target}.tmp`;

async function digest(path) {
  return createHash("sha256").update(await readFile(path)).digest("hex");
}

async function existing() {
  try {
    return await digest(target);
  } catch {
    return null;
  }
}

const hash = await existing();
if (hash === SHA256) {
  console.log(`OCR 模型已就绪：${target}`);
  process.exit(0);
}
if (hash !== null) {
  console.log("已有的 OCR 模型校验不符，重新下载");
  await rm(target, { force: true });
}

console.log(`正在下载 OCR 模型（${(SIZE / 1e6).toFixed(1)} MB）…`);
let body;
try {
  const res = await fetch(SOURCE, { signal: AbortSignal.timeout(180_000) });
  if (!res.ok)
    throw new Error(`HTTP ${res.status}`);
  body = new Uint8Array(await res.arrayBuffer());
} catch (cause) {
  console.error(`拉取 OCR 模型失败：${cause instanceof Error ? cause.message : String(cause)}`);
  console.error("首次构建需要网络；离线时可以先在联网环境执行 pnpm run fetch:ocr");
  process.exit(1);
}

if (body.byteLength !== SIZE) {
  console.error(`模型大小不符：期望 ${SIZE} 字节，实际 ${body.byteLength}`);
  process.exit(1);
}

const actual = createHash("sha256").update(body).digest("hex");
if (actual !== SHA256) {
  console.error(`模型 sha256 不符：期望 ${SHA256}，实际 ${actual}`);
  process.exit(1);
}

await mkdir(dirname(target), { recursive: true });
await writeFile(temp, body);
await rename(temp, target);
console.log(`OCR 模型就绪：${target}`);
