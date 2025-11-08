// Types for the Sun-based Alarm Clock App

export interface Alarm {
  id: string;
  time: string; // HH:mm format
  enabled: boolean;
  label: string;
  useSunTime: boolean; // true = sun-based, false = normal clock
  sunOffset: number; // minutes offset from sunrise (can be negative)
  repeatDays: number[]; // 0-6 (Sunday-Saturday)
  deleteAfterUse: boolean;
  soundUri: string;
  soundName: string;
  vibrate: boolean;
  gradualVolume: boolean;
  gradualDuration: number; // seconds for volume increase
}

export interface AppSettings {
  defaultClockMode: "normal" | "sun"; // default clock mode for new alarms
  use24HourFormat: boolean;
  language: string;
  notificationsEnabled: boolean;
}

export interface SunTimes {
  sunrise: Date;
  sunset: Date;
  date: Date;
}

export interface Location {
  latitude: number;
  longitude: number;
  timestamp: number;
}

export interface Translation {
  appName: string;
  alarms: string;
  addAlarm: string;
  editAlarm: string;
  deleteAlarm: string;
  time: string;
  label: string;
  normalClock: string;
  sunBasedClock: string;
  sunOffset: string;
  minutesAfterSunrise: string;
  minutesBeforeSunrise: string;
  repeat: string;
  sunday: string;
  monday: string;
  tuesday: string;
  wednesday: string;
  thursday: string;
  friday: string;
  saturday: string;
  deleteAfterUse: string;
  alarmSound: string;
  selectSound: string;
  vibrate: string;
  gradualVolume: string;
  gradualDuration: string;
  seconds: string;
  save: string;
  cancel: string;
  delete: string;
  enabled: string;
  disabled: string;
  upcomingSunTimes: string;
  sunrise: string;
  sunset: string;
  today: string;
  tomorrow: string;
  settings: string;
  defaultClockMode: string;
  use24Hour: string;
  language: string;
  locationPermission: string;
  locationPermissionDesc: string;
  notificationPermission: string;
  notificationPermissionDesc: string;
  alarmPermission: string;
  alarmPermissionDesc: string;
  grantPermission: string;
  permissions: string;
  noAlarmsYet: string;
  createFirstAlarm: string;
  alarmSetFor: string;
  in: string;
  hours: string;
  minutes: string;
  at: string;
  every: string;
  once: string;
  weekdays: string;
  weekends: string;
  daily: string;
  custom: string;
  confirmDelete: string;
  confirmDeleteMessage: string;
  yes: string;
  no: string;
  timer: string;
  stopwatch: string;
  addTimer: string;
  start: string;
  pause: string;
  resume: string;
  reset: string;
  stop: string;
  lap: string;
  duration: string;
  setDuration: string;
  timerFinished: string;
  noTimersYet: string;
  createFirstTimer: string;
  selectSoundFromStorage: string;
  browseFiles: string;
  laps: string;
  lapTime: string;
  splitTime: string;
  totalTime: string;
  clearLaps: string;
}

export type Language =
  | "en"
  | "tr"
  | "es"
  | "de"
  | "fr"
  | "it"
  | "pt"
  | "ru"
  | "ja"
  | "zh"
  | "ar";

export interface Timer {
  id: string;
  label: string;
  duration: number; // seconds
  remainingTime: number; // seconds
  isRunning: boolean;
  isPaused: boolean;
  soundUri: string;
  soundName: string;
  vibrate: boolean;
}

export interface Lap {
  id: string;
  lapNumber: number;
  time: number; // milliseconds
  splitTime: number; // milliseconds from start
}

export interface Stopwatch {
  isRunning: boolean;
  isPaused: boolean;
  elapsedTime: number; // milliseconds
  laps: Lap[];
}
