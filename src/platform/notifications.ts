/**
 * 平台通知能力：Android 走 KomaNotifications 插件（渠道由原生注册、即时 notify）。
 * Web 返回 null，调用方据此禁用通知入口，不造假成功。
 */

import { Capacitor, registerPlugin } from "@capacitor/core";

export interface NotificationPayload {
  id: string;
  title: string;
  body: string;
  channelId?: string;
}

export type NotificationPermissionState = "granted" | "denied" | "prompt";

export interface NotificationStatus {
  notifications: NotificationPermissionState;
}

export type NotificationReason = "permission_denied" | "unknown_channel" | "channel_blocked";

export interface NotificationPlugin {
  getStatus: () => Promise<NotificationStatus>;
  requestPermission: () => Promise<NotificationStatus>;
  show: (options: NotificationPayload) => Promise<{
    posted: boolean;
    reason?: NotificationReason;
  }>;
  cancel: (options: { id: string }) => Promise<void>;
  cancelAll: () => Promise<void>;
}

const KomaNotifications = registerPlugin<NotificationPlugin>("KomaNotifications");

/** 原生返回插件实例；Web 返回 null（无通知能力） */
export function createNotifications(): NotificationPlugin | null {
  return Capacitor.isNativePlatform()
    ? KomaNotifications
    : null;
}
