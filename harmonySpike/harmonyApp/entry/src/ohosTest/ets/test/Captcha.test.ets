import { util } from '@kit.ArkTS';
import { abilityDelegatorRegistry } from '@kit.TestKit';
import { describe, expect, it } from '@ohos/hypium';
import * as ort from '@ohos/onnxruntime';
import { decodeCaptcha } from '../../../main/ets/ocr/CaptchaOutput';

export default function captchaTest() {
  describe('CaptchaModelTest', () => {
    // This CAS fixture reads "1+8=?"; exercise the shipped native ABI without an account or network.
    it('loadsBundledModelAndRecognizesArithmeticTwice', 0, async () => {
      const context = abilityDelegatorRegistry.getAbilityDelegator().getAppContext();
      const resources = context.resourceManager;
      const testResources = context.createModuleContext('entry_test').resourceManager;
      const model = await resources.getRawFileContent('ocr/captcha.onnx');
      const labels = await resources.getRawFileContent('ocr/charset.txt');
      const tensor = await testResources.getRawFileContent('captcha-1-plus-8.f32');
      const charset = new Map<number, string>();
      util.TextDecoder.create('utf-8').decodeToString(labels).split('\n').forEach((line: string) => {
        const separator = line.indexOf('\t');
        if (separator > 0) {
          charset.set(Number(line.substring(0, separator)), line.substring(separator + 1).trim());
        }
      });
      ort.OnnxEnvironment.InitEnvironmentWithNameAndLogLevel('KomaOcrTest', ort.OnnxLoggingLevel.ERROR);
      ort.OnnxEnvironment.SetTelemetryEnabled(false);
      const options = new ort.OnnxSessionOptions();
      let session: ort.OnnxSession;
      try {
        options.SetOptimizationLevel(ort.OnnxOptimizationLevel.ALL);
        options.SetIntraOpNumThreads(2);
        session = ort.OnnxEnvironment.CreateSessionFromBufferWithOptions(new Int8Array(model.buffer), options);
      } finally {
        options.Close();
      }
      try {
        for (let round = 0; round < 2; round++) {
          const input = new ort.OnnxTensor(tensor.buffer as ArrayBuffer, [1, 1, 64, 320], ort.OnnxArkTSType.FLOAT);
          try {
            const inputs: Record<string, ort.OnnxValue> = {};
            inputs[session.GetInputNames()[0]] = input;
            const result = session.Run(inputs);
            try {
              const output = result.GetValueByName(session.GetOutputNames()[0]) as ort.OnnxTensor;
              const shape = output.GetInfo().GetShape();
              const text = decodeCaptcha(new Float32Array(output.GetDataAsArrayBuffer()), shape[shape.length - 1], charset);
              expect(text).assertContain('1+8');
            } finally {
              result.Close();
            }
          } finally {
            input.Close();
          }
        }
      } finally {
        session.Close();
      }
    });
  });
}
