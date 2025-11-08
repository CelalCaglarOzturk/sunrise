import { writable, derived } from 'svelte/store';
import type { Alarm, AppSettings, Location, SunTimes, Language } from '../types';
import { detectSystemLanguage } from '../utils/translations';

// Alarms store
function createAlarmsStore() {
  const { subscribe, set, update } = writable<Alarm[]>([]);

  return {
    subscribe,
    set,
    add: (alarm: Alarm) => update(alarms => [...alarms, alarm]),
    update: (id: string, updatedAlarm: Partial<Alarm>) =>
      update(alarms => alarms.map(alarm =>
        alarm.id === id ? { ...alarm, ...updatedAlarm } : alarm
      )),
    remove: (id: string) => update(alarms => alarms.filter(alarm => alarm.id !== id)),
    toggle: (id: string) =>
      update(alarms => alarms.map(alarm =>
        alarm.id === id ? { ...alarm, enabled: !alarm.enabled } : alarm
      )),
    load: (alarms: Alarm[]) => set(alarms),
  };
}

// Settings store
function createSettingsStore() {
  const defaultSettings: AppSettings = {
    defaultClockMode: 'normal',
    use24HourFormat: true,
    language: detectSystemLanguage(),
    notificationsEnabled: true,
  };

  const { subscribe, set, update } = writable<AppSettings>(defaultSettings);

  return {
    subscribe,
    set,
    update: (settings: Partial<AppSettings>) =>
      update(current => ({ ...current, ...settings })),
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

// Create store instances
export const alarms = createAlarmsStore();
export const settings = createSettingsStore();
export const location = createLocationStore();
export const sunTimes = createSunTimesStore();

// Derived store for sorted alarms (enabled first, then by time)
export const sortedAlarms = derived(alarms, $alarms => {
  return [...$alarms].sort((a, b) => {
    // Enabled alarms first
    if (a.enabled && !b.enabled) return -1;
    if (!a.enabled && b.enabled) return 1;

    // Then sort by time
    return a.time.localeCompare(b.time);
  });
});

// Derived store for enabled alarms only
export const enabledAlarms = derived(alarms, $alarms =>
  $alarms.filter(alarm => alarm.enabled)
);

// Derived store for next alarm
export const nextAlarm = derived([enabledAlarms, sunTimes, location], ([$alarms, $sunTimes, $location]) => {
  if ($alarms.length === 0) return null;

  const now = new Date();
  const today = now.getDay(); // 0-6 (Sunday-Saturday)

  let nextAlarmTime: Date | null = null;
  let nextAlarmData: Alarm | null = null;

  for (const alarm of $alarms) {
    // Check if alarm should ring today or in upcoming days
    const alarmDays = alarm.repeatDays.length > 0 ? alarm.repeatDays : [today];

    for (let daysAhead = 0; daysAhead < 7; daysAhead++) {
      const checkDay = (today + daysAhead) % 7;

      if (!alarmDays.includes(checkDay) && alarm.repeatDays.length > 0) continue;

      let alarmTime: Date;

      if (alarm.useSunTime && $sunTimes.length > 0 && $location) {
        // Find sun time for the day
        const dayDate = new Date(now);
        dayDate.setDate(dayDate.getDate() + daysAhead);
        dayDate.setHours(0, 0, 0, 0);

        const sunTime = $sunTimes.find(st => {
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
        const [hours, minutes] = alarm.time.split(':').map(Number);
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
});

// Persistence keys
const STORAGE_KEYS = {
  ALARMS: 'sun_clock_alarms',
  SETTINGS: 'sun_clock_settings',
  LOCATION: 'sun_clock_location',
};

// Load from localStorage
export function loadFromStorage() {
  if (typeof window === 'undefined') return;

  try {
    const savedAlarms = localStorage.getItem(STORAGE_KEYS.ALARMS);
    if (savedAlarms) {
      alarms.load(JSON.parse(savedAlarms));
    }

    const savedSettings = localStorage.getItem(STORAGE_KEYS.SETTINGS);
    if (savedSettings) {
      settings.set(JSON.parse(savedSettings));
    }

    const savedLocation = localStorage.getItem(STORAGE_KEYS.LOCATION);
    if (savedLocation) {
      location.set(JSON.parse(savedLocation));
    }
  } catch (error) {
    console.error('Error loading from storage:', error);
  }
}

// Save to localStorage
export function saveToStorage() {
  if (typeof window === 'undefined') return;

  alarms.subscribe(value => {
    localStorage.setItem(STORAGE_KEYS.ALARMS, JSON.stringify(value));
  });

  settings.subscribe(value => {
    localStorage.setItem(STORAGE_KEYS.SETTINGS, JSON.stringify(value));
  });

  location.subscribe(value => {
    if (value) {
      localStorage.setItem(STORAGE_KEYS.LOCATION, JSON.stringify(value));
    }
  });
}
