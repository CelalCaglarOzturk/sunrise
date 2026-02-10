import { writable, derived } from "svelte/store";
import type {
  Alarm,
  AppSettings,
  Location,
  SunTimes,
  Language,
  Timer,
  Stopwatch,
} from "../types";
import {
  detectSystemLanguage,
  detectSystem24HourFormat,
} from "../utils/translations";
import { dbService } from "../services/database";

// Alarms store
function createAlarmsStore() {
  const { subscribe, set, update } = writable<Alarm[]>([]);

  return {
    subscribe,
    set,
    add: (alarm: Alarm) => update((alarms) => [...alarms, alarm]),
    update: (id: string, updatedAlarm: Partial<Alarm>) =>
      update((alarms) =>
        alarms.map((alarm) =>
          alarm.id === id ? { ...alarm, ...updatedAlarm } : alarm
        )
      ),
    remove: (id: string) =>
      update((alarms) => alarms.filter((alarm) => alarm.id !== id)),
    toggle: (id: string) =>
      update((alarms) =>
        alarms.map((alarm) =>
          alarm.id === id ? { ...alarm, enabled: !alarm.enabled } : alarm
        )
      ),
    disableAll: () =>
      update((alarms) => alarms.map((alarm) => ({ ...alarm, enabled: false }))),
    enableAll: () =>
      update((alarms) => alarms.map((alarm) => ({ ...alarm, enabled: true }))),
    deleteAll: () => set([]),
    load: (alarms: Alarm[]) => set(alarms),
  };
}

// Settings store
function createSettingsStore() {
  const defaultSettings: AppSettings = {
    defaultClockMode: "normal",
    use24HourFormat: detectSystem24HourFormat(),
    showSeconds: true,
    language: detectSystemLanguage(),
    notificationsEnabled: true,
  };

  const { subscribe, set, update } = writable<AppSettings>(defaultSettings);

  return {
    subscribe,
    set,
    update: (settings: Partial<AppSettings>) =>
      update((current) => ({ ...current, ...settings })),
    reset: () => set(defaultSettings),
  };
}

// Location store
function createLocationStore() {
  const { subscribe, set } = writable<Location | null>(null);

  return {
    subscribe,
    set,
    update: (location: Location) => set(location),
  };
}

// Sun times store (for upcoming 5 days)
function createSunTimesStore() {
  const { subscribe, set } = writable<SunTimes[]>([]);

  return {
    subscribe,
    set,
    update: (sunTimes: SunTimes[]) => set(sunTimes),
  };
}

// Timer store
function createTimersStore() {
  const { subscribe, set, update } = writable<Timer[]>([]);

  return {
    subscribe,
    set,
    add: (timer: Timer) => update((timers) => [...timers, timer]),
    update: (id: string, updatedTimer: Partial<Timer>) =>
      update((timers) =>
        timers.map((timer) =>
          timer.id === id ? { ...timer, ...updatedTimer } : timer
        )
      ),
    remove: (id: string) =>
      update((timers) => timers.filter((timer) => timer.id !== id)),
    load: (timers: Timer[]) => set(timers),
  };
}

// Stopwatch store
function createStopwatchStore() {
  const defaultStopwatch: Stopwatch = {
    isRunning: false,
    isPaused: false,
    elapsedTime: 0,
    laps: [],
  };

  const { subscribe, set, update } = writable<Stopwatch>(defaultStopwatch);

  return {
    subscribe,
    set,
    update: (data: Partial<Stopwatch>) =>
      update((current) => ({ ...current, ...data })),
    reset: () => set(defaultStopwatch),
    addLap: (lap: any) =>
      update((current) => ({
        ...current,
        laps: [...current.laps, lap],
      })),
    clearLaps: () =>
      update((current) => ({
        ...current,
        laps: [],
      })),
  };
}

// Create store instances
export const alarms = createAlarmsStore();
export const settings = createSettingsStore();
export const location = createLocationStore();
export const sunTimes = createSunTimesStore();
export const timers = createTimersStore();
export const stopwatch = createStopwatchStore();

// Derived store for sorted alarms (enabled first, then by time)
export const sortedAlarms = derived(alarms, ($alarms) => {
  return [...$alarms].sort((a, b) => {
    // Enabled alarms first
    if (a.enabled && !b.enabled) return -1;
    if (!a.enabled && b.enabled) return 1;

    // Then sort by time
    return a.time.localeCompare(b.time);
  });
});

// Derived store for enabled alarms only
export const enabledAlarms = derived(alarms, ($alarms) =>
  $alarms.filter((alarm) => alarm.enabled)
);

// Derived store for next alarm
export const nextAlarm = derived(
  [enabledAlarms, sunTimes, location],
  ([$alarms, $sunTimes, $location]) => {
    if ($alarms.length === 0) return null;

    const now = new Date();
    const today = now.getDay(); // 0-6 (Sunday-Saturday)

    let nextAlarmTime: Date | null = null;
    let nextAlarmData: Alarm | null = null;

    for (const alarm of $alarms) {
      // Check if alarm should ring today or in upcoming days
      const alarmDays =
        alarm.repeatDays.length > 0 ? alarm.repeatDays : [today];

      for (let daysAhead = 0; daysAhead < 7; daysAhead++) {
        const checkDay = (today + daysAhead) % 7;

        if (!alarmDays.includes(checkDay) && alarm.repeatDays.length > 0)
          continue;

        let alarmTime: Date;

        if (alarm.useSunTime && $sunTimes.length > 0 && $location) {
          // Find sun time for the day
          const dayDate = new Date(now);
          dayDate.setDate(dayDate.getDate() + daysAhead);
          dayDate.setHours(0, 0, 0, 0);

          const sunTime = $sunTimes.find((st) => {
            const stDate = new Date(st.date);
            stDate.setHours(0, 0, 0, 0);
            return stDate.getTime() === dayDate.getTime();
          });

          if (sunTime) {
            alarmTime = new Date(sunTime.sunrise);
            alarmTime.setMinutes(alarmTime.getMinutes() + alarm.sunOffset);
          } else {
            continue;
          }
        } else {
          // Normal time
          const [hours, minutes] = alarm.time.split(":").map(Number);
          alarmTime = new Date(now);
          alarmTime.setDate(alarmTime.getDate() + daysAhead);
          alarmTime.setHours(hours, minutes, 0, 0);
        }

        // Check if this alarm time is in the future
        if (alarmTime > now) {
          if (!nextAlarmTime || alarmTime < nextAlarmTime) {
            nextAlarmTime = alarmTime;
            nextAlarmData = alarm;
          }
          break; // Found next occurrence for this alarm
        }
      }
    }

    return nextAlarmData ? { alarm: nextAlarmData, time: nextAlarmTime } : null;
  }
);

// Load from SQLite
export async function loadFromStorage(): Promise<void> {
  try {
    // Initialize database
    await dbService.initialize();

    // Load from SQLite
    const [savedAlarms, savedSettings, savedLocation, savedTimers, savedStopwatch] = await Promise.all([
      dbService.getAlarms(),
      dbService.getSettings(),
      dbService.getLocation(),
      dbService.getTimers(),
      dbService.getStopwatch(),
    ]);

    if (savedAlarms.length > 0) {
      alarms.load(savedAlarms);
    }

    if (savedSettings) {
      settings.set(savedSettings);
    }

    if (savedLocation) {
      location.set(savedLocation);
    }

    if (savedTimers.length > 0) {
      timers.load(savedTimers);
    }

    if (savedStopwatch) {
      stopwatch.set(savedStopwatch);
    }
  } catch (error) {
    console.error("Error loading from storage:", error);
  }
}

// Save to SQLite
export function saveToStorage(): void {
  // Subscribe to alarms
  alarms.subscribe(($alarms) => {
    // Delete all and re-insert
    dbService.deleteAllAlarms().then(() => {
      $alarms.forEach((alarm) => dbService.saveAlarm(alarm));
    });
  });

  // Subscribe to settings
  settings.subscribe(($settings) => {
    dbService.saveSettings($settings);
  });

  // Subscribe to location
  location.subscribe(($location) => {
    if ($location) {
      dbService.saveLocation($location);
    }
  });

  // Subscribe to timers
  timers.subscribe(($timers) => {
    // Delete all and re-insert
    dbService.deleteAllTimers().then(() => {
      $timers.forEach((timer) => dbService.saveTimer(timer));
    });
  });

  // Subscribe to stopwatch
  stopwatch.subscribe(($stopwatch) => {
    dbService.saveStopwatch($stopwatch);
  });
}
