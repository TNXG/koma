import { ArkUIViewController, Compose } from '@cpf-kmp-cmp/compose';
import { hilog } from '@kit.PerformanceAnalysisKit';
import nativeApi from 'libentry.so';
import { CaptchaOcr } from '../ocr/CaptchaOcr';

const DOMAIN = 0x0000;

@Entry
@Component
struct Index {
  private controller: ArkUIViewController | undefined = undefined;
  private ocr: CaptchaOcr | undefined;

  @State errorMessage: string = 'Native module not ready';

  aboutToAppear() {
    if (nativeApi === undefined) {
      hilog.error(DOMAIN, 'Compose', 'nativeApi is undefined, cannot create controller');
      this.errorMessage = 'nativeApi is undefined';
      return;
    }
    try {
      const context = this.getUIContext().getHostContext();
      const filesDir = context?.filesDir;
      if (!filesDir || !context) {
        throw new Error('Application filesDir is unavailable');
      }
      this.ocr = new CaptchaOcr(context.resourceManager);
      this.controller = nativeApi.MainArkUIViewController(filesDir, (id: number, tensor: ArrayBuffer, width: number) => {
        this.ocr?.recognize(id, tensor, width);
      });
      const state = this.controller ? 'created' : 'not_created';
      hilog.info(DOMAIN, 'Compose', 'controller init state: %{public}s', state);
      if (!this.controller) {
        this.errorMessage = 'Controller creation failed (returned null)';
      }
    } catch (e) {
      hilog.error(DOMAIN, 'Compose', 'Exception creating controller: %{public}s', JSON.stringify(e));
      this.errorMessage = 'Exception creating controller: ' + JSON.stringify(e);
    }
  }

  aboutToDisappear(): void {
    nativeApi.releaseCaptcha();
    this.ocr?.close();
  }

  build() {
    Column() {
      if (this.controller) {
        Compose({
          controller: this.controller,
          libraryName: 'entry',
          // This is the dispatcher's fallback, not a second back-event entry point.
          onBackPressed: () => false
        })
        .width('100%')
        .height('100%')
        .expandSafeArea([SafeAreaType.SYSTEM, SafeAreaType.CUTOUT], [SafeAreaEdge.TOP, SafeAreaEdge.BOTTOM,
          SafeAreaEdge.START, SafeAreaEdge.END])
      } else {
        Text(this.errorMessage)
      }
    }
    .width('100%')
    .height('100%')
    .expandSafeArea([SafeAreaType.SYSTEM, SafeAreaType.CUTOUT], [SafeAreaEdge.TOP, SafeAreaEdge.BOTTOM,
      SafeAreaEdge.START, SafeAreaEdge.END])
  }

  onPageHide() {
    if (this.controller) {
      this.controller.onPageHide();
    }
  }

  onPageShow() {
    if (this.controller) {
      this.controller.onPageShow();
    }
  }

  onBackPress(): boolean {
    return this.controller ? this.controller.onBackPress() : false;
  }
}
