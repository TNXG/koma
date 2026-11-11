import type { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  appId: "moe.tnxg.koma",
  appName: "Koma Pro",
  webDir: "dist",
  server: {
    androidScheme: "https",
  },
  plugins: {
    CapacitorSQLite: {
      androidIsEncryption: false,
      androidDatabaseLocation: "default",
    },
  },
};

export default config;
