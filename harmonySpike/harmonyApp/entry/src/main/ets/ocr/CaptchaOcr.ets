import { worker, util, MessageEvents } from '@kit.ArkTS';
import { resourceManager } from '@kit.LocalizationKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import nativeApi from 'libentry.so';
import { CaptchaRequest, CaptchaResponse } from './CaptchaMessages';

interface CaptchaAssets {
  model: Uint8Array;
  charset: Uint8Array;
}

/** One worker/session per page lifetime. Model loading and inference never block the UI thread. */
export class CaptchaOcr {
  private thread: worker.ThreadWorker | undefined;
  private assets: Promise<CaptchaAssets> | undefined;
  private closed = false;
  private failed = false;
  private pending = new Set<number>();

  private resources: resourceManager.ResourceManager;

  constructor(resources: resourceManager.ResourceManager) {
    this.resources = resources;
  }

  private async loadAssets(): Promise<CaptchaAssets> {
    const model = await this.resources.getRawFileContent('ocr/captcha.onnx');
    const charset = await this.resources.getRawFileContent('ocr/charset.txt');
    return { model: model, charset: charset };
  }

  async recognize(id: number, tensor: ArrayBuffer, width: number): Promise<void> {
    this.pending.add(id);
    try {
      if (this.closed || this.failed) {
        this.pending.delete(id);
        nativeApi.completeCaptcha(id, '');
        return;
      }
      this.assets ??= this.loadAssets();
      const assets = await this.assets;
      if (this.closed) {
        return;
      }
      const request: CaptchaRequest = { id: id, tensor: tensor, width: width };
      if (!this.thread) {
        this.thread = new worker.ThreadWorker('entry/ets/workers/CaptchaWorker.ets', { name: 'koma-ocr' });
        this.thread.onmessage = (event: MessageEvents) => {
          const response = event.data as CaptchaResponse;
          this.pending.delete(response.id);
          if (response.error) {
            hilog.warn(0, 'KomaOcr', 'Captcha inference failed: %{public}s', response.error);
          }
          nativeApi.completeCaptcha(response.id, response.text);
        };
        this.thread.onerror = () => { this.failed = true; this.failPending(); };
        this.thread.onmessageerror = () => { this.failed = true; this.failPending(); };
        this.thread.onexit = () => { this.failed = true; this.thread = undefined; this.failPending(); };
        request.model = assets.model.buffer as ArrayBuffer;
        request.charset = util.TextDecoder.create('utf-8').decodeToString(assets.charset);
      }
      this.thread.postMessage(request, request.model ? [tensor, request.model] : [tensor]);
    } catch (error) {
      hilog.warn(0, 'KomaOcr', 'Captcha setup failed: %{public}s', (error as Error).message);
      this.pending.delete(id);
      nativeApi.completeCaptcha(id, '');
    }
  }

  private failPending(): void {
    this.pending.forEach((id: number) => { nativeApi.completeCaptcha(id, ''); });
    this.pending.clear();
  }

  close(): void {
    this.closed = true;
    this.failPending();
    const thread = this.thread;
    this.thread = undefined;
    if (thread) {
      try { thread.postMessage('close'); }
      catch (error) { thread.terminate(); }
    }
  }
}
