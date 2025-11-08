import { LocalNotifications } from "@capacitor/local-notifications";
import { Haptics, ImpactStyle } from "@capacitor/haptics";
import type { Alarm, Location } from "../types";
import { sunCalcService } from "./sunCalc";

export class AlarmService {
  private static instance: AlarmService;
  private scheduledAlarms: Set<string> = new Set();

  private constructor() {}

  static getInstance(): AlarmService {
    if (!AlarmService.instance) {
      AlarmService.instance = new AlarmService();
    }
    return AlarmService.instance;
  }

  async requestPermissions(): Promise<boolean> {
    try {
      const result = await LocalNotifications.requestPermissions();
      return result.display === "granted";
    } catch (error) {
      console.error("Error requesting notification permissions:", error);
      return false;
    }
  }

  async checkPermissions(): Promise<boolean> {
    try {
      const result = await LocalNotifications.checkPermissions();
      return result.display === "granted";
    } catch (error) {
      console.error("Error checking notification permissions:", error);
      return false;
    }
  }

  /**
   * Schedule an alarm
   */
  async scheduleAlarm(alarm: Alarm, location: Location | null): Promise<void> {
    try {
      const hasPermission = await this.checkPermissions();
      if (!hasPermission) {
        const granted = await this.requestPermissions();
        if (!granted) {
          throw new Error("Notification permission denied");
        }
      }

      // Cancel existing notification for this alarm
      await this.cancelAlarm(alarm.id);

      const notifications = this.calculateAlarmTimes(alarm, location);

      if (notifications.length === 0) {
        console.warn("No valid alarm times calculated");
        return;
      }

      await LocalNotifications.schedule({
        notifications: notifications.map((notification, index) => ({
          id: parseInt(alarm.id.replace(/\D/g, "").slice(0, 9)) + index,
          title: alarm.label || "Alarm",
          body: this.formatAlarmBody(alarm),
          schedule: { at: notification },
          // Note: Android notifications can only use sounds from res/raw folder
          // Custom sounds from storage cannot be used directly
          // Default notification sound will be used if soundUri is from external storage
          sound: this.getNotificationSound(alarm.soundUri),
          actionTypeId: alarm.id,
          extra: {
            alarmId: alarm.id,
            vibrate: alarm.vibrate,
            gradualVolume: alarm.gradualVolume,
            gradualDuration: alarm.gradualDuration,
            deleteAfterUse: alarm.deleteAfterUse,
            soundUri: alarm.soundUri,
            soundName: alarm.soundName,
          },
        })),
      });

      this.scheduledAlarms.add(alarm.id);
    } catch (error) {
      console.error("Error scheduling alarm:", error);
      throw error;
    }
  }

  /**
   * Cancel an alarm
   */
  async cancelAlarm(alarmId: string): Promise<void> {
    try {
      // Get all pending notifications
      const pending = await LocalNotifications.getPending();

      // Find notifications for this alarm
      const notificationsToCancel = pending.notifications.filter(
        (notification) => notification.extra?.alarmId === alarmId
      );

      if (notificationsToCancel.length > 0) {
        await LocalNotifications.cancel({
          notifications: notificationsToCancel,
        });
      }

      this.scheduledAlarms.delete(alarmId);
    } catch (error) {
      console.error("Error canceling alarm:", error);
      throw error;
    }
  }

  /**
   * Reschedule all alarms
   */
  async rescheduleAllAlarms(
    alarms: Alarm[],
    location: Location | null
  ): Promise<void> {
    try {
      // Cancel all existing notifications
      await LocalNotifications.cancel({
        notifications: (await LocalNotifications.getPending()).notifications,
      });

      this.scheduledAlarms.clear();

      // Schedule all enabled alarms
      for (const alarm of alarms.filter((a) => a.enabled)) {
        await this.scheduleAlarm(alarm, location);
      }
    } catch (error) {
      console.error("Error rescheduling alarms:", error);
      throw error;
    }
  }

  /**
   * Calculate alarm times for the next 7 days
   */
  private calculateAlarmTimes(alarm: Alarm, location: Location | null): Date[] {
    const times: Date[] = [];
    const now = new Date();
    const today = now.getDay(); // 0-6 (Sunday-Saturday)

    // If no repeat days, alarm should ring once
    const repeatDays = alarm.repeatDays.length > 0 ? alarm.repeatDays : [today];

    for (let daysAhead = 0; daysAhead < 7; daysAhead++) {
      const checkDay = (today + daysAhead) % 7;

      // Skip if not in repeat days
      if (!repeatDays.includes(checkDay) && alarm.repeatDays.length > 0) {
        continue;
      }

      let alarmTime: Date;

      if (alarm.useSunTime && location) {
        // Sun-based time
        const targetDate = new Date(now);
        targetDate.setDate(targetDate.getDate() + daysAhead);
        targetDate.setHours(0, 0, 0, 0);

        alarmTime = sunCalcService.convertSunTimeToActualTime(
          alarm.sunOffset,
          location,
          targetDate
        );
      } else {
        // Normal time
        const [hours, minutes] = alarm.time.split(":").map(Number);
        alarmTime = new Date(now);
        alarmTime.setDate(alarmTime.getDate() + daysAhead);
        alarmTime.setHours(hours, minutes, 0, 0);
      }

      // Only add if in the future
      if (alarmTime > now) {
        times.push(alarmTime);

        // If delete after use or no repeat days, only schedule once
        if (alarm.deleteAfterUse || alarm.repeatDays.length === 0) {
          break;
        }
      }
    }

    return times;
  }

  /**
   * Format alarm body text
   */
  private formatAlarmBody(alarm: Alarm): string {
    if (alarm.useSunTime) {
      const offsetAbs = Math.abs(alarm.sunOffset);
      const offsetHours = Math.floor(offsetAbs / 60);
      const offsetMinutes = offsetAbs % 60;

      let text = "";
      if (offsetHours > 0) {
        text += `${offsetHours}h `;
      }
      if (offsetMinutes > 0 || offsetHours === 0) {
        text += `${offsetMinutes}m `;
      }

      if (alarm.sunOffset >= 0) {
        text += "after sunrise";
      } else {
        text += "before sunrise";
      }

      return text;
    }

    return alarm.time;
  }

  /**
   * Trigger vibration
   */
  async triggerVibration(): Promise<void> {
    try {
      await Haptics.impact({ style: ImpactStyle.Heavy });
    } catch (error) {
      console.error("Error triggering vibration:", error);
    }
  }

  /**
   * Handle alarm notification click
   */
  async handleAlarmNotification(notificationId: number): Promise<void> {
    try {
      // Dismiss the notification
      await LocalNotifications.cancel({
        notifications: [{ id: notificationId }],
      });
    } catch (error) {
      console.error("Error handling alarm notification:", error);
    }
  }

  /**
   * Get all pending notifications
   */
  async getPendingNotifications(): Promise<any[]> {
    try {
      const result = await LocalNotifications.getPending();
      return result.notifications;
    } catch (error) {
      console.error("Error getting pending notifications:", error);
      return [];
    }
  }

  /**
   * Check if alarm is scheduled
   */
  isAlarmScheduled(alarmId: string): boolean {
    return this.scheduledAlarms.has(alarmId);
  }

  /**
   * Get notification sound (only built-in sounds work on Android)
   * Custom sounds from external storage need to be handled by the app itself
   */
  private getNotificationSound(soundUri: string): string | undefined {
    // If soundUri is empty or looks like an external file, use default
    if (
      !soundUri ||
      soundUri.startsWith("file://") ||
      soundUri.startsWith("content://")
    ) {
      // Return undefined to use system default notification sound
      return undefined;
    }
    // Only return sound if it's a resource sound (res/raw)
    return soundUri;
  }

  /**
   * Initialize notification listeners
   */
  initializeListeners(
    onAlarmTriggered: (alarmId: string, extra: any) => void
  ): void {
    LocalNotifications.addListener(
      "localNotificationReceived",
      (notification) => {
        console.log("Notification received:", notification);

        if (notification.extra) {
          // Trigger vibration if enabled
          if (notification.extra.vibrate) {
            this.triggerVibration();
          }

          // Call the alarm triggered callback
          onAlarmTriggered(notification.extra.alarmId, notification.extra);
        }
      }
    );

    LocalNotifications.addListener(
      "localNotificationActionPerformed",
      (notification) => {
        console.log("Notification action performed:", notification);

        if (notification.notification.extra) {
          // Handle notification dismissal
          onAlarmTriggered(
            notification.notification.extra.alarmId,
            notification.notification.extra
          );
        }
      }
    );
  }

  /**
   * Remove notification listeners
   */
  removeListeners(): void {
    LocalNotifications.removeAllListeners();
  }
}

export const alarmService = AlarmService.getInstance();
