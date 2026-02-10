import { Filesystem, Directory, Encoding } from "@capacitor/filesystem";
import type { AppSettings, Alarm, Timer, Stopwatch, Pomodoro } from "../types";
import { dbService } from "./database";

export interface ExportedData {
  version: string;
  exportDate: string;
  settings: AppSettings;
  alarms: Alarm[];
  timers: Timer[];
  stopwatch: Stopwatch;
  pomodoro: Pomodoro;
}

export class SettingsImportExportService {
  private static instance: SettingsImportExportService;

  private constructor() {}

  static getInstance(): SettingsImportExportService {
    if (!SettingsImportExportService.instance) {
      SettingsImportExportService.instance = new SettingsImportExportService();
    }
    return SettingsImportExportService.instance;
  }

  /**
   * Export all settings and data to a JSON file
   */
  async exportSettings(): Promise<string | null> {
    try {
      // Gather all data
      const [settings, alarms, timers, stopwatch, pomodoro] = await Promise.all([
        dbService.getSettings(),
        dbService.getAlarms(),
        dbService.getTimers(),
        dbService.getStopwatch(),
        dbService.getPomodoro(),
      ]);

      const exportData: ExportedData = {
        version: "1.0.0",
        exportDate: new Date().toISOString(),
        settings: settings || {
          defaultClockMode: "normal",
          use24HourFormat: true,
          showSeconds: true,
          language: "en",
          notificationsEnabled: true,
        },
        alarms: alarms || [],
        timers: timers || [],
        stopwatch: stopwatch || {
          isRunning: false,
          isPaused: false,
          elapsedTime: 0,
          laps: [],
        },
        pomodoro: pomodoro || {
          isRunning: false,
          isPaused: false,
          remainingTime: 1500,
          currentPhase: "work",
          workDuration: 1500,
          shortBreakDuration: 300,
          longBreakDuration: 900,
          workSessionsBeforeLongBreak: 4,
          currentSessionCount: 0,
          sessions: [],
          autoStartBreaks: false,
          autoStartWork: false,
        },
      };

      const jsonData = JSON.stringify(exportData, null, 2);
      const fileName = `sun_clock_backup_${new Date().toISOString().split("T")[0]}.json`;

      // Save to file
      await Filesystem.writeFile({
        path: fileName,
        data: jsonData,
        directory: Directory.Documents,
        encoding: Encoding.UTF8,
      });

      return fileName;
    } catch (error) {
      console.error("Error exporting settings:", error);
      return null;
    }
  }

  /**
   * Import settings from a JSON file
   */
  async importSettings(filePath: string): Promise<boolean> {
    try {
      // Read file
      const result = await Filesystem.readFile({
        path: filePath,
        directory: Directory.Documents,
        encoding: Encoding.UTF8,
      });

      const data: ExportedData = JSON.parse(result.data as string);

      // Validate version
      if (!data.version || !data.settings) {
        throw new Error("Invalid backup file format");
      }

      // Import all data
      if (data.settings) {
        await dbService.saveSettings(data.settings);
      }

      if (data.alarms && Array.isArray(data.alarms)) {
        await dbService.deleteAllAlarms();
        for (const alarm of data.alarms) {
          await dbService.saveAlarm(alarm);
        }
      }

      if (data.timers && Array.isArray(data.timers)) {
        await dbService.deleteAllTimers();
        for (const timer of data.timers) {
          await dbService.saveTimer(timer);
        }
      }

      if (data.stopwatch) {
        await dbService.saveStopwatch(data.stopwatch);
      }

      if (data.pomodoro) {
        await dbService.savePomodoro(data.pomodoro);
      }

      return true;
    } catch (error) {
      console.error("Error importing settings:", error);
      return false;
    }
  }

  /**
   * Get list of backup files
   */
  async getBackupFiles(): Promise<string[]> {
    try {
      const result = await Filesystem.readdir({
        path: "",
        directory: Directory.Documents,
      });

      return result.files
        .filter((file) => file.name.startsWith("sun_clock_backup_") && file.name.endsWith(".json"))
        .map((file) => file.name);
    } catch (error) {
      console.error("Error getting backup files:", error);
      return [];
    }
  }

  /**
   * Share exported file
   */
  async shareExport(fileName: string): Promise<void> {
    try {
      // Read the file
      const result = await Filesystem.readFile({
        path: fileName,
        directory: Directory.Documents,
        encoding: Encoding.UTF8,
      });

      // Create a blob and download link for web
      const blob = new Blob([result.data as string], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = fileName;
      link.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      console.error("Error sharing export:", error);
    }
  }
}

export const settingsImportExportService = SettingsImportExportService.getInstance();
