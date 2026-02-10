import { BatteryOptimization } from "@capawesome-team/capacitor-android-battery-optimization";

export class BatteryOptimizationService {
  private static instance: BatteryOptimizationService;

  private constructor() {}

  static getInstance(): BatteryOptimizationService {
    if (!BatteryOptimizationService.instance) {
      BatteryOptimizationService.instance = new BatteryOptimizationService();
    }
    return BatteryOptimizationService.instance;
  }

  /**
   * Check if battery optimization is enabled for the app
   * Returns true if the app is being optimized (restricted)
   */
  async isBatteryOptimizationEnabled(): Promise<boolean> {
    try {
      const result = await BatteryOptimization.isBatteryOptimizationEnabled();
      return result.enabled;
    } catch (error) {
      console.error("Error checking battery optimization:", error);
      // If plugin fails, assume optimization is not enabled (safer assumption)
      return false;
    }
  }

  /**
   * Open battery optimization settings
   * Takes user to system settings to disable battery optimization for this app
   */
  async openBatteryOptimizationSettings(): Promise<void> {
    try {
      // Use the capawesome plugin's native method
      await BatteryOptimization.openBatteryOptimizationSettings();
    } catch (error) {
      console.error("Error opening battery optimization settings:", error);
    }
  }

  /**
   * Request to ignore battery optimization (requires special permission)
   * Note: This requires REQUEST_IGNORE_BATTERY_OPTIMIZATIONS permission in AndroidManifest.xml
   */
  async requestIgnoreBatteryOptimization(): Promise<boolean> {
    try {
      await BatteryOptimization.requestIgnoreBatteryOptimization();
      return true;
    } catch (error) {
      console.error("Error requesting battery optimization ignore:", error);
      return false;
    }
  }
}

export const batteryOptimizationService = BatteryOptimizationService.getInstance();
