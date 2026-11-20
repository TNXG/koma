import { worker, MessageEvents } from '@kit.ArkTS';
import * as ort from '@ohos/onnxruntime';
import { CaptchaRequest, CaptchaResponse } from '../ocr/CaptchaMessages';
import { decodeCaptcha } from '../ocr/CaptchaOutput';

let session: ort.OnnxSession | undefined;
let inputName = '';
let outputName = '';
const charset = new Map<number, string>();
const port = worker.workerPort;

port.onmessage = (event: MessageEvents) => {
  if (event.data === 'close') {
    session?.Close();
    session = undefined;
    port.close();
    return;
  }
  const request = event.data as CaptchaRequest;
  const response: CaptchaResponse = { id: request.id, text: '' };
  try {
    if (!session) {
      if (!request.model || !request.charset) {
        throw new Error('Captcha model is unavailable');
      }
      ort.OnnxEnvironment.InitEnvironmentWithNameAndLogLevel('KomaOcr', ort.OnnxLoggingLevel.ERROR);
      ort.OnnxEnvironment.SetTelemetryEnabled(false);
      const options = new ort.OnnxSessionOptions();
      try {
        options.SetOptimizationLevel(ort.OnnxOptimizationLevel.ALL);
        options.SetIntraOpNumThreads(2);
        session = ort.OnnxEnvironment.CreateSessionFromBufferWithOptions(new Int8Array(request.model), options);
      } finally {
        options.Close();
      }
      inputName = session.GetInputNames()[0];
      outputName = session.GetOutputNames()[0];
      request.charset.split('\n').forEach((line: string) => {
        const separator = line.indexOf('\t');
        if (separator > 0) {
          charset.set(Number(line.substring(0, separator)), line.substring(separator + 1).trim());
        }
      });
    }
    const input = new ort.OnnxTensor(request.tensor, [1, 1, 64, request.width], ort.OnnxArkTSType.FLOAT);
    try {
      const inputs: Record<string, ort.OnnxValue> = {};
      inputs[inputName] = input;
      const result = session.Run(inputs);
      try {
        const output = result.GetValueByName(outputName) as ort.OnnxTensor;
        const shape = output.GetInfo().GetShape();
        response.text = decodeCaptcha(new Float32Array(output.GetDataAsArrayBuffer()), shape[shape.length - 1], charset);
      } finally {
        result.Close();
      }
    } finally {
      input.Close();
    }
  } catch (error) {
    response.error = (error as Error).message;
  }
  port.postMessage(response);
};

