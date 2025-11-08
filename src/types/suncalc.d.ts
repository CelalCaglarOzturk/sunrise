declare module 'suncalc' {
  export interface SunTimes {
    sunrise: Date;
    sunriseEnd: Date;
    goldenHourEnd: Date;
    solarNoon: Date;
    goldenHour: Date;
    sunsetStart: Date;
    sunset: Date;
    dusk: Date;
    nauticalDusk: Date;
    night: Date;
    nadir: Date;
    nightEnd: Date;
    nauticalDawn: Date;
    dawn: Date;
  }

  export interface SunPosition {
    azimuth: number;
    altitude: number;
  }

  export interface MoonPosition {
    azimuth: number;
    altitude: number;
    distance: number;
    parallacticAngle: number;
  }

  export interface MoonIllumination {
    fraction: number;
    phase: number;
    angle: number;
  }

  export interface MoonTimes {
    rise?: Date;
    set?: Date;
    alwaysUp?: boolean;
    alwaysDown?: boolean;
  }

  /**
   * Calculates sun position for a given date and latitude/longitude
   */
  export function getPosition(date: Date | number, latitude: number, longitude: number): SunPosition;

  /**
   * Calculates sun times for a given date and latitude/longitude
   */
  export function getTimes(date: Date | number, latitude: number, longitude: number, height?: number): SunTimes;

  /**
   * Calculates moon position for a given date and latitude/longitude
   */
  export function getMoonPosition(date: Date | number, latitude: number, longitude: number): MoonPosition;

  /**
   * Calculates moon illumination for a given date
   */
  export function getMoonIllumination(date: Date | number): MoonIllumination;

  /**
   * Calculates moon rise and set times for a given date and latitude/longitude
   */
  export function getMoonTimes(date: Date | number, latitude: number, longitude: number, inUTC?: boolean): MoonTimes;

  /**
   * Add a custom time to the times config
   */
  export function addTime(angle: number, riseName: string, setName: string): void;
}
