import type { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  appId: "dev.mudrock.sunrise",
  appName: "Sun Clock",
  webDir: "dist",
  server: {
    androidScheme: "https",
  },
  android: {
    webContentsDebuggingEnabled: true,
    allowMixedContent: false,
    // Enable edge-to-edge display to handle 3-button navigation properly
    adjustMarginsForEdgeToEdge: "auto",
  },
  plugins: {
    LocalNotifications: {
      smallIcon: "ic_stat_icon_config_sample",
      iconColor: "#ff9500",
      sound: "beep.wav",
    },
    SplashScreen: {
      launchShowDuration: 2000,
      backgroundColor: "#000000",
      showSpinner: false,
    },
    StatusBar: {
      style: "DARK",
      backgroundColor: "#000000",
    },
    // SafeArea plugin configuration for Android navigation bar handling
    SafeArea: {
      statusBarStyle: "DARK",
      navigationBarStyle: "DARK",
    },
  },
};

export default config;
