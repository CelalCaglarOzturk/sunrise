import { CapacitorSQLite, SQLiteConnection, SQLiteDBConnection } from "@capacitor-community/sqlite";
import type { Alarm, AppSettings, Location, Timer, Stopwatch, Pomodoro, PomodoroSession } from "../types";

const DB_NAME = "sun_clock_db";

class DatabaseService {
  private sqlite: SQLiteConnection;
  private db: SQLiteDBConnection | null = null;
  private isInitialized = false;

  constructor() {
    this.sqlite = new SQLiteConnection(CapacitorSQLite);
  }

  async initialize(): Promise<void> {
    if (this.isInitialized) return;

    try {
      // Check if connection exists and close it if needed
      if (this.db) {
        await this.db.close();
      }

      // Create database connection
      const ret = await this.sqlite.checkConnectionsConsistency();
      const isConn = (await this.sqlite.isConnection(DB_NAME, false)).result;

      if (ret.result && isConn) {
        this.db = await this.sqlite.retrieveConnection(DB_NAME, false);
      } else {
        this.db = await this.sqlite.createConnection(
          DB_NAME,
          false,
          "no-encryption",
          1,
          false
        );
      }

      await this.db.open();
      await this.createTables();
      this.isInitialized = true;
    } catch (error) {
      console.error("Error initializing database:", error);
      // Fallback to localStorage if SQLite fails
      this.isInitialized = false;
    }
  }

  private async createTables(): Promise<void> {
    if (!this.db) return;

    // Create alarms table
    await this.db.execute(`
      CREATE TABLE IF NOT EXISTS alarms (
        id TEXT PRIMARY KEY,
        time TEXT NOT NULL,
        enabled INTEGER DEFAULT 1,
        label TEXT,
        useSunTime INTEGER DEFAULT 0,
        sunOffset INTEGER DEFAULT 0,
        repeatDays TEXT DEFAULT '[]',
        deleteAfterUse INTEGER DEFAULT 0,
        soundUri TEXT,
        soundName TEXT,
        vibrate INTEGER DEFAULT 1,
        volume INTEGER DEFAULT 100,
        gradualVolume INTEGER DEFAULT 0,
        gradualDuration INTEGER DEFAULT 30
      )
    `);

    // Create settings table
    await this.db.execute(`
      CREATE TABLE IF NOT EXISTS settings (
        id INTEGER PRIMARY KEY CHECK (id = 1),
        defaultClockMode TEXT DEFAULT 'normal',
        use24HourFormat INTEGER DEFAULT 1,
        showSeconds INTEGER DEFAULT 1,
        language TEXT DEFAULT 'en',
        notificationsEnabled INTEGER DEFAULT 1
      )
    `);

    // Create location table
    await this.db.execute(`
      CREATE TABLE IF NOT EXISTS location (
        id INTEGER PRIMARY KEY CHECK (id = 1),
        latitude REAL,
        longitude REAL,
        timestamp INTEGER
      )
    `);

    // Create timers table
    await this.db.execute(`
      CREATE TABLE IF NOT EXISTS timers (
        id TEXT PRIMARY KEY,
        label TEXT,
        duration INTEGER DEFAULT 0,
        remainingTime INTEGER DEFAULT 0,
        isRunning INTEGER DEFAULT 0,
        isPaused INTEGER DEFAULT 0,
        soundUri TEXT,
        soundName TEXT,
        vibrate INTEGER DEFAULT 1
      )
    `);

    // Create stopwatch table
    await this.db.execute(`
      CREATE TABLE IF NOT EXISTS stopwatch (
        id INTEGER PRIMARY KEY CHECK (id = 1),
        isRunning INTEGER DEFAULT 0,
        isPaused INTEGER DEFAULT 0,
        elapsedTime INTEGER DEFAULT 0,
        laps TEXT DEFAULT '[]'
      )
    `);

    // Insert default settings if not exists
    await this.db.execute(`
      INSERT OR IGNORE INTO settings (id) VALUES (1)
    `);

    // Insert default stopwatch if not exists
    await this.db.execute(`
      INSERT OR IGNORE INTO stopwatch (id) VALUES (1)
    `);

    // Create pomodoro table
    await this.db.execute(`
      CREATE TABLE IF NOT EXISTS pomodoro (
        id INTEGER PRIMARY KEY CHECK (id = 1),
        isRunning INTEGER DEFAULT 0,
        isPaused INTEGER DEFAULT 0,
        remainingTime INTEGER DEFAULT 1500,
        currentPhase TEXT DEFAULT 'work',
        workDuration INTEGER DEFAULT 1500,
        shortBreakDuration INTEGER DEFAULT 300,
        longBreakDuration INTEGER DEFAULT 900,
        workSessionsBeforeLongBreak INTEGER DEFAULT 4,
        currentSessionCount INTEGER DEFAULT 0,
        autoStartBreaks INTEGER DEFAULT 0,
        autoStartWork INTEGER DEFAULT 0
      )
    `);

    // Create pomodoro_sessions table
    await this.db.execute(`
      CREATE TABLE IF NOT EXISTS pomodoro_sessions (
        id TEXT PRIMARY KEY,
        startTime INTEGER NOT NULL,
        endTime INTEGER,
        duration INTEGER,
        completed INTEGER DEFAULT 0,
        type TEXT DEFAULT 'work'
      )
    `);

    // Insert default pomodoro if not exists
    await this.db.execute(`
      INSERT OR IGNORE INTO pomodoro (id) VALUES (1)
    `);
  }

  // Alarms
  async getAlarms(): Promise<Alarm[]> {
    if (!this.db || !this.isInitialized) return [];

    try {
      const result = await this.db.query("SELECT * FROM alarms");
      return result.values?.map((row) => ({
        id: row.id,
        time: row.time,
        enabled: Boolean(row.enabled),
        label: row.label || "",
        useSunTime: Boolean(row.useSunTime),
        sunOffset: row.sunOffset,
        repeatDays: JSON.parse(row.repeatDays || "[]"),
        deleteAfterUse: Boolean(row.deleteAfterUse),
        soundUri: row.soundUri || "",
        soundName: row.soundName || "",
        vibrate: Boolean(row.vibrate),
        volume: row.volume,
        gradualVolume: Boolean(row.gradualVolume),
        gradualDuration: row.gradualDuration,
      })) || [];
    } catch (error) {
      console.error("Error getting alarms:", error);
      return [];
    }
  }

  async saveAlarm(alarm: Alarm): Promise<void> {
    if (!this.db || !this.isInitialized) return;

    try {
      await this.db.run(
        `INSERT OR REPLACE INTO alarms 
        (id, time, enabled, label, useSunTime, sunOffset, repeatDays, 
         deleteAfterUse, soundUri, soundName, vibrate, volume, 
         gradualVolume, gradualDuration) 
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        [
          alarm.id,
          alarm.time,
          alarm.enabled ? 1 : 0,
          alarm.label,
          alarm.useSunTime ? 1 : 0,
          alarm.sunOffset,
          JSON.stringify(alarm.repeatDays),
          alarm.deleteAfterUse ? 1 : 0,
          alarm.soundUri,
          alarm.soundName,
          alarm.vibrate ? 1 : 0,
          alarm.volume,
          alarm.gradualVolume ? 1 : 0,
          alarm.gradualDuration,
        ]
      );
    } catch (error) {
      console.error("Error saving alarm:", error);
    }
  }

  async deleteAlarm(id: string): Promise<void> {
    if (!this.db || !this.isInitialized) return;

    try {
      await this.db.run("DELETE FROM alarms WHERE id = ?", [id]);
    } catch (error) {
      console.error("Error deleting alarm:", error);
    }
  }

  async deleteAllAlarms(): Promise<void> {
    if (!this.db || !this.isInitialized) return;

    try {
      await this.db.run("DELETE FROM alarms");
    } catch (error) {
      console.error("Error deleting all alarms:", error);
    }
  }

  // Settings
  async getSettings(): Promise<AppSettings | null> {
    if (!this.db || !this.isInitialized) return null;

    try {
      const result = await this.db.query("SELECT * FROM settings WHERE id = 1");
      if (result.values && result.values.length > 0) {
        const row = result.values[0];
        return {
          defaultClockMode: row.defaultClockMode,
          use24HourFormat: Boolean(row.use24HourFormat),
          showSeconds: Boolean(row.showSeconds),
          language: row.language,
          notificationsEnabled: Boolean(row.notificationsEnabled),
        };
      }
      return null;
    } catch (error) {
      console.error("Error getting settings:", error);
      return null;
    }
  }

  async saveSettings(settings: AppSettings): Promise<void> {
    if (!this.db || !this.isInitialized) return;

    try {
      await this.db.run(
        `INSERT OR REPLACE INTO settings 
        (id, defaultClockMode, use24HourFormat, showSeconds, language, notificationsEnabled) 
        VALUES (?, ?, ?, ?, ?, ?)`,
        [
          1,
          settings.defaultClockMode,
          settings.use24HourFormat ? 1 : 0,
          settings.showSeconds ? 1 : 0,
          settings.language,
          settings.notificationsEnabled ? 1 : 0,
        ]
      );
    } catch (error) {
      console.error("Error saving settings:", error);
    }
  }

  // Location
  async getLocation(): Promise<Location | null> {
    if (!this.db || !this.isInitialized) return null;

    try {
      const result = await this.db.query("SELECT * FROM location WHERE id = 1");
      if (result.values && result.values.length > 0) {
        const row = result.values[0];
        return {
          latitude: row.latitude,
          longitude: row.longitude,
          timestamp: row.timestamp,
        };
      }
      return null;
    } catch (error) {
      console.error("Error getting location:", error);
      return null;
    }
  }

  async saveLocation(location: Location): Promise<void> {
    if (!this.db || !this.isInitialized) return;

    try {
      await this.db.run(
        `INSERT OR REPLACE INTO location 
        (id, latitude, longitude, timestamp) 
        VALUES (?, ?, ?, ?)`,
        [1, location.latitude, location.longitude, location.timestamp]
      );
    } catch (error) {
      console.error("Error saving location:", error);
    }
  }

  // Timers
  async getTimers(): Promise<Timer[]> {
    if (!this.db || !this.isInitialized) return [];

    try {
      const result = await this.db.query("SELECT * FROM timers");
      return result.values?.map((row) => ({
        id: row.id,
        label: row.label || "",
        duration: row.duration,
        remainingTime: row.remainingTime,
        isRunning: Boolean(row.isRunning),
        isPaused: Boolean(row.isPaused),
        soundUri: row.soundUri || "",
        soundName: row.soundName || "",
        vibrate: Boolean(row.vibrate),
      })) || [];
    } catch (error) {
      console.error("Error getting timers:", error);
      return [];
    }
  }

  async saveTimer(timer: Timer): Promise<void> {
    if (!this.db || !this.isInitialized) return;

    try {
      await this.db.run(
        `INSERT OR REPLACE INTO timers 
        (id, label, duration, remainingTime, isRunning, isPaused, soundUri, soundName, vibrate) 
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        [
          timer.id,
          timer.label,
          timer.duration,
          timer.remainingTime,
          timer.isRunning ? 1 : 0,
          timer.isPaused ? 1 : 0,
          timer.soundUri,
          timer.soundName,
          timer.vibrate ? 1 : 0,
        ]
      );
    } catch (error) {
      console.error("Error saving timer:", error);
    }
  }

  async deleteTimer(id: string): Promise<void> {
    if (!this.db || !this.isInitialized) return;

    try {
      await this.db.run("DELETE FROM timers WHERE id = ?", [id]);
    } catch (error) {
      console.error("Error deleting timer:", error);
    }
  }

  async deleteAllTimers(): Promise<void> {
    if (!this.db || !this.isInitialized) return;

    try {
      await this.db.run("DELETE FROM timers");
    } catch (error) {
      console.error("Error deleting all timers:", error);
    }
  }

  // Stopwatch
  async getStopwatch(): Promise<Stopwatch | null> {
    if (!this.db || !this.isInitialized) return null;

    try {
      const result = await this.db.query("SELECT * FROM stopwatch WHERE id = 1");
      if (result.values && result.values.length > 0) {
        const row = result.values[0];
        return {
          isRunning: Boolean(row.isRunning),
          isPaused: Boolean(row.isPaused),
          elapsedTime: row.elapsedTime,
          laps: JSON.parse(row.laps || "[]"),
        };
      }
      return null;
    } catch (error) {
      console.error("Error getting stopwatch:", error);
      return null;
    }
  }

  async saveStopwatch(stopwatch: Stopwatch): Promise<void> {
    if (!this.db || !this.isInitialized) return;

    try {
      await this.db.run(
        `INSERT OR REPLACE INTO stopwatch 
        (id, isRunning, isPaused, elapsedTime, laps) 
        VALUES (?, ?, ?, ?, ?)`,
        [
          1,
          stopwatch.isRunning ? 1 : 0,
          stopwatch.isPaused ? 1 : 0,
          stopwatch.elapsedTime,
          JSON.stringify(stopwatch.laps),
        ]
      );
    } catch (error) {
      console.error("Error saving stopwatch:", error);
    }
  }

  // Pomodoro
  async getPomodoro(): Promise<Pomodoro | null> {
    if (!this.db || !this.isInitialized) return null;

    try {
      const result = await this.db.query("SELECT * FROM pomodoro WHERE id = 1");
      const sessionsResult = await this.db.query("SELECT * FROM pomodoro_sessions ORDER BY startTime DESC");
      
      if (result.values && result.values.length > 0) {
        const row = result.values[0];
        const sessions: PomodoroSession[] = sessionsResult.values?.map((s: any) => ({
          id: s.id,
          startTime: s.startTime,
          endTime: s.endTime,
          duration: s.duration,
          completed: Boolean(s.completed),
          type: s.type as "work" | "shortBreak" | "longBreak",
        })) || [];
        
        return {
          isRunning: Boolean(row.isRunning),
          isPaused: Boolean(row.isPaused),
          remainingTime: row.remainingTime,
          currentPhase: row.currentPhase as "work" | "shortBreak" | "longBreak",
          workDuration: row.workDuration,
          shortBreakDuration: row.shortBreakDuration,
          longBreakDuration: row.longBreakDuration,
          workSessionsBeforeLongBreak: row.workSessionsBeforeLongBreak,
          currentSessionCount: row.currentSessionCount,
          sessions,
          autoStartBreaks: Boolean(row.autoStartBreaks),
          autoStartWork: Boolean(row.autoStartWork),
        };
      }
      return null;
    } catch (error) {
      console.error("Error getting pomodoro:", error);
      return null;
    }
  }

  async savePomodoro(pomodoro: Pomodoro): Promise<void> {
    if (!this.db || !this.isInitialized) return;

    try {
      await this.db.run(
        `INSERT OR REPLACE INTO pomodoro 
        (id, isRunning, isPaused, remainingTime, currentPhase, workDuration, 
         shortBreakDuration, longBreakDuration, workSessionsBeforeLongBreak, 
         currentSessionCount, autoStartBreaks, autoStartWork) 
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        [
          1,
          pomodoro.isRunning ? 1 : 0,
          pomodoro.isPaused ? 1 : 0,
          pomodoro.remainingTime,
          pomodoro.currentPhase,
          pomodoro.workDuration,
          pomodoro.shortBreakDuration,
          pomodoro.longBreakDuration,
          pomodoro.workSessionsBeforeLongBreak,
          pomodoro.currentSessionCount,
          pomodoro.autoStartBreaks ? 1 : 0,
          pomodoro.autoStartWork ? 1 : 0,
        ]
      );

      // Save sessions
      await this.db.run("DELETE FROM pomodoro_sessions");
      for (const session of pomodoro.sessions) {
        await this.db.run(
          `INSERT INTO pomodoro_sessions 
          (id, startTime, endTime, duration, completed, type) 
          VALUES (?, ?, ?, ?, ?, ?)`,
          [
            session.id,
            session.startTime,
            session.endTime,
            session.duration,
            session.completed ? 1 : 0,
            session.type,
          ]
        );
      }
    } catch (error) {
      console.error("Error saving pomodoro:", error);
    }
  }

  // Check if database is available
  isAvailable(): boolean {
    return this.isInitialized && this.db !== null;
  }

  // Close database
  async close(): Promise<void> {
    if (this.db) {
      try {
        await this.db.close();
        this.isInitialized = false;
        this.db = null;
      } catch (error) {
        console.error("Error closing database:", error);
      }
    }
  }
}

export const dbService = new DatabaseService();
