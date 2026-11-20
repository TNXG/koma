import { ArkUIViewController } from "compose/src/main/cpp/types/libcompose_arkui_utils";

export const MainArkUIViewController: (
  filesDir: string, recognize: (id: number, tensor: ArrayBuffer, width: number) => void
) => ArkUIViewController;
export const completeCaptcha: (id: number, text: string) => void;
export const releaseCaptcha: () => void;
