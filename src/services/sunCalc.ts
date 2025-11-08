import * as SunCalc from "suncalc";
import type { SunTimes, Location } from "../types";

export class SunCalcService {
  private static instance: SunCalcService;

  private constructor() {}

  static getInstance(): SunCalcService {
    if (!SunCalcService.instance) {
      SunCalcService.instance = new SunCalcService();
    }
    return SunCalcService.instance;
  }

  /**
   * Get sunrise and sunset times for a specific date and location
   */
  getSunTimes(date: Date, location: Location): SunTimes {
    const times = SunCalc.getTimes(date, location.latitude, location.longitude);

    return {
      sunrise: times.sunrise,
      sunset: times.sunset,
      date: date,
    };
  }

  /**
   * Get sun times for the next N days
   */
  getUpcomingSunTimes(location: Location, days: number = 5): SunTimes[] {
    const sunTimes: SunTimes[] = [];
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    for (let i = 0; i < days; i++) {
      const date = new Date(today);
      date.setDate(date.getDate() + i);

      sunTimes.push(this.getSunTimes(date, location));
    }

    return sunTimes;
  }

  /**
   * Convert sun-based time to actual time
   * @param sunOffset Minutes offset from sunrise (can be negative)
   * @param location User location
   * @param date Target date
   * @returns Actual time
   */
  convertSunTimeToActualTime(
    sunOffset: number,
    location: Location,
    date: Date = new Date(),
  ): Date {
    const times = SunCalc.getTimes(date, location.latitude, location.longitude);
    const actualTime = new Date(times.sunrise);
    actualTime.setMinutes(actualTime.getMinutes() + sunOffset);

    return actualTime;
  }

  /**
   * Convert actual time to sun-based offset
   * @param time Actual time (HH:mm format)
   * @param location User location
   * @param date Target date
   * @returns Minutes offset from sunrise
   */
  convertActualTimeToSunOffset(
    time: string,
    location: Location,
    date: Date = new Date(),
  ): number {
    const times = SunCalc.getTimes(date, location.latitude, location.longitude);
    const [hours, minutes] = time.split(":").map(Number);

    const actualTime = new Date(date);
    actualTime.setHours(hours, minutes, 0, 0);

    const sunriseTime = times.sunrise.getTime();
    const actualTimeMs = actualTime.getTime();

    const offsetMs = actualTimeMs - sunriseTime;
    const offsetMinutes = Math.round(offsetMs / 60000);

    return offsetMinutes;
  }

  /**
   * Format time for display
   */
  formatTime(date: Date, use24Hour: boolean = true): string {
    if (use24Hour) {
      return date.toLocaleTimeString("en-US", {
        hour: "2-digit",
        minute: "2-digit",
        hour12: false,
      });
    } else {
      return date.toLocaleTimeString("en-US", {
        hour: "numeric",
        minute: "2-digit",
        hour12: true,
      });
    }
  }

  /**
   * Get time until next alarm
   */
  getTimeUntil(targetTime: Date): { hours: number; minutes: number } {
    const now = new Date();
    const diff = targetTime.getTime() - now.getTime();

    if (diff <= 0) {
      return { hours: 0, minutes: 0 };
    }

    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));

    return { hours, minutes };
  }

  /**
   * Check if a date is today
   */
  isToday(date: Date): boolean {
    const today = new Date();
    return (
      date.getDate() === today.getDate() &&
      date.getMonth() === today.getMonth() &&
      date.getFullYear() === today.getFullYear()
    );
  }

  /**
   * Check if a date is tomorrow
   */
  isTomorrow(date: Date): boolean {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    return (
      date.getDate() === tomorrow.getDate() &&
      date.getMonth() === tomorrow.getMonth() &&
      date.getFullYear() === tomorrow.getFullYear()
    );
  }

  /**
   * Get day name
   */
  getDayName(date: Date, locale: string = "en-US"): string {
    return date.toLocaleDateString(locale, { weekday: "long" });
  }

  /**
   * Get formatted date
   */
  getFormattedDate(date: Date, locale: string = "en-US"): string {
    return date.toLocaleDateString(locale, {
      month: "short",
      day: "numeric",
    });
  }
}

export const sunCalcService = SunCalcService.getInstance();
