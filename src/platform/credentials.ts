/**
 * 平台凭据能力（非教务专用）：Android 走 KomaCredentials 插件
 * （Keystore 密钥 + AES-256-GCM 密文存私有目录），Web 一律返回 unavailable，不落任何浏览器存储。
 */

import { Capacitor, registerPlugin } from "@capacitor/core";

export interface StoredCredential {
  username: string;
  password: string;
}

export type CredentialLoad
  = | { kind: "found"; username: string; password: string }
    | { kind: "empty" }
    | { kind: "unavailable" };

export type CredentialSave
  = | { kind: "ok" }
    | { kind: "unavailable" }
    | { kind: "failed"; message: string };

export interface CredentialStore {
  available: () => Promise<boolean>;
  save: (input: { schoolId: string; username: string; password: string }) => Promise<CredentialSave>;
  load: (schoolId: string) => Promise<CredentialLoad>;
  remove: (schoolId: string) => Promise<void>;
}

interface KomaCredentialsPlugin {
  isAvailable: () => Promise<{ available: boolean }>;
  save: (options: { schoolId: string; username: string; password: string }) => Promise<void>;
  load: (options: { schoolId: string }) => Promise<{
    found: boolean;
    username?: string;
    password?: string;
  }>;
  remove: (options: { schoolId: string }) => Promise<void>;
}

const KomaCredentials = registerPlugin<KomaCredentialsPlugin>("KomaCredentials");

const webCredentialStore: CredentialStore = {
  async available() {
    return false;
  },
  async save() {
    return { kind: "unavailable" };
  },
  async load() {
    return { kind: "unavailable" };
  },
  async remove() {
    // Web 不落凭据，无需清理
  },
};

const nativeCredentialStore: CredentialStore = {
  async available() {
    try {
      return (await KomaCredentials.isAvailable()).available;
    } catch {
      return false;
    }
  },

  async save(input) {
    try {
      await KomaCredentials.save(input);
      return { kind: "ok" };
    } catch (error) {
      return errorCode(error) === "UNAVAILABLE"
        ? { kind: "unavailable" }
        : {
            kind: "failed",
            message: messageOf(error),
          };
    }
  },

  async load(schoolId) {
    try {
      const result = await KomaCredentials.load({ schoolId });
      if (!result.found || typeof result.username !== "string" || typeof result.password !== "string")
        return { kind: "empty" };
      return { kind: "found", username: result.username, password: result.password };
    } catch (error) {
      return errorCode(error) === "INVALID_ARGUMENT"
        ? { kind: "empty" }
        : { kind: "unavailable" };
    }
  },

  async remove(schoolId) {
    await KomaCredentials.remove({ schoolId });
  },
};

/** 原生返回 Capacitor 插件实例；Web 返回不可用实现 */
export function createCredentialStore(): CredentialStore {
  return Capacitor.isNativePlatform()
    ? nativeCredentialStore
    : webCredentialStore;
}

function errorCode(error: unknown): string | undefined {
  if (typeof error !== "object" || error === null || !("code" in error))
    return undefined;
  const code: unknown = error.code;
  return typeof code === "string"
    ? code
    : undefined;
}

function messageOf(error: unknown): string {
  return error instanceof Error
    ? error.message
    : "凭据保存失败";
}
