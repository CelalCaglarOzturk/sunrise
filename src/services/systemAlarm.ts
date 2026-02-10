import { LocalNotifications } from "@capacitor/local-notifications";

export class SystemAlarmService {
  private static instance: SystemAlarmService;

  private constructor() {}

  static getInstance(): SystemAlarmService {
    if (!SystemAlarmService.instance) {
      SystemAlarmService.instance = new SystemAlarmService();
    }
    return SystemAlarmService.instance;
  }

  /**
   * Check if system alarm access is granted
   * Uses Capacitor LocalNotifications to verify we can schedule notifications
   */
  async checkSystemAlarmAccess(): Promise<boolean> {
    try {
      // Check notification permissions - this is required for alarms
      const permissionStatus = await LocalNotifications.checkPermissions();
      
      // If we have display permission, we likely have alarm access
      return permissionStatus.display === "granted";
    } catch (error) {
      console.error("Error checking system alarm access:", error);
      return false;
    }
  }

  /**
   * Request system alarm access
   * On Android, this requests notification permissions which are required for alarms
   */
  async requestSystemAlarmAccess(): Promise<boolean> {
    try {
      // Request notification permissions through Capacitor
      const result = await LocalNotifications.requestPermissions();
      
      if (result.display === "granted") {
        // Try to schedule a test notification to verify exact alarm permission
        const testId = Math.floor(Math.random() * 1000000);
        const testTime = new Date(Date.now() + 60000); // 1 minute from now
        
        await LocalNotifications.schedule({
          notifications: [{
            id: testId,
            title: "Alarm Test",
            body: "Testing alarm permission",
            schedule: { at: testTime },
          }]
        });

        // Cancel test notification immediately
        await LocalNotifications.cancel({ 
          notifications: [{ id: testId }] 
        });
        
        return true;
      }
      
      return false;
    } catch (error) {
      console.error("Error requesting system alarm access:", error);
      return false;
    }
  }

  /**
   * Check if the app can schedule exact alarms
   * This checks the SCHEDULE_EXACT_ALARM permission status
   */
  async canScheduleExactAlarms(): Promise<boolean> {
    try {
      // Try scheduling a notification 1 second in the future
      const testId = Math.floor(Math.random() * 1000000);
      const testTime = new Date(Date.now() + 1000);
      
      await LocalNotifications.schedule({
        notifications: [{
          id: testId,
          title: "Test",
          body: "Test",
          schedule: { at: testTime },
        }]
      });

      // Cancel immediately
      await LocalNotifications.cancel({ 
        notifications: [{ id: testId }] 
      });
      
      return true;
    } catch (error) {
      console.error("Cannot schedule exact alarms:", error);
      return false;
    }
  }
}

export const systemAlarmService = SystemAlarmService.getInstance();
