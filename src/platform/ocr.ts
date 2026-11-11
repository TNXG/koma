/**
 * 平台 OCR 能力：Android 走 KomaOcr 插件（ONNX Runtime 本地推理，模型随包分发），
 * Web 一律不可用——不伪造识别结果。
 */

import { Capacitor, registerPlugin } from "@capacitor/core";

export interface OcrResult {
  text: string;
  /** 实际生效的后端：nnapi / cpu */
  provider: string;
}

export interface Ocr {
  available: () => Promise<boolean>;
  /** image 接受 data URI 或裸 base64；view 0 = 原图，1 = 裁掉两端噪声 */
  recognize: (image: string, view?: number) => Promise<OcrResult>;
}

interface KomaOcrPlugin {
  isAvailable: () => Promise<{ available: boolean }>;
  recognize: (options: { image: string; view: number }) => Promise<{ text: string; provider: string }>;
}

const KomaOcr = registerPlugin<KomaOcrPlugin>("KomaOcr");

const webOcr: Ocr = {
  async available() {
    return false;
  },
  async recognize() {
    throw new Error("此设备不支持本地识别");
  },
};

const nativeOcr: Ocr = {
  async available() {
    try {
      return (await KomaOcr.isAvailable()).available;
    } catch {
      return false;
    }
  },
  recognize(image, view = 0) {
    return KomaOcr.recognize({ image, view });
  },
};

export function createOcr(): Ocr {
  return Capacitor.isNativePlatform()
    ? nativeOcr
    : webOcr;
}
