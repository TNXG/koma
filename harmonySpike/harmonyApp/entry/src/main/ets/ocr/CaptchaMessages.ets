export interface CaptchaRequest {
  id: number;
  tensor: ArrayBuffer;
  width: number;
  model?: ArrayBuffer;
  charset?: string;
}

export interface CaptchaResponse {
  id: number;
  text: string;
  error?: string;
}
