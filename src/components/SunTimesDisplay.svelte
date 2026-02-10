<script lang="ts">
    import type { SunTimes, Translation } from "../types";
    import { sunCalcService } from "../services/sunCalc";
    import { Sunrise, Sunset, Clock } from "@lucide/svelte";

    interface Props {
        sunTimes: SunTimes[];
        t: Translation;
        use24Hour?: boolean;
        language?: string;
    }

    let { 
        sunTimes = [], 
        t, 
        use24Hour = true, 
        language = "en" 
    }: Props = $props();

    function formatTime(date: Date): string {
        return sunCalcService.formatTime(date, use24Hour);
    }

    function getDaylightDuration(sunrise: Date, sunset: Date): string {
        const durationMs = sunset.getTime() - sunrise.getTime();
        const hours = Math.floor(durationMs / (1000 * 60 * 60));
        const minutes = Math.floor((durationMs % (1000 * 60 * 60)) / (1000 * 60));
        return `${hours}h ${minutes}m`;
    }

    function getDayLabel(date: Date): string {
        if (sunCalcService.isToday(date)) {
            return t.today;
        }
        if (sunCalcService.isTomorrow(date)) {
            return t.tomorrow;
        }
        return sunCalcService.getDayName(date, language);
    }

    function getFormattedDate(date: Date): string {
        return sunCalcService.getFormattedDate(date, language);
    }
</script>

<div class="sun-times-display">
    <h3 class="sun-times-title">{t.upcomingSunTimes}</h3>

    <div class="sun-times-list">
        {#each sunTimes as sunTime, index}
            <div class="sun-time-item" class:today={index === 0}>
                <div class="day-info">
                    <div class="day-label">{getDayLabel(sunTime.date)}</div>
                    <div class="date-label">{getFormattedDate(sunTime.date)}</div>
                </div>

                <div class="times">
                    <div class="time-block sunrise">
                        <div class="time-icon">
                            <Sunrise size={32} />
                        </div>
                        <div class="time-info">
                            <div class="time-label">{t.sunrise}</div>
                            <div class="time-value">{formatTime(sunTime.sunrise)}</div>
                        </div>
                    </div>

                    <div class="time-block sunset">
                        <div class="time-icon">
                            <Sunset size={32} />
                        </div>
                        <div class="time-info">
                            <div class="time-label">{t.sunset}</div>
                            <div class="time-value">{formatTime(sunTime.sunset)}</div>
                        </div>
                    </div>
                </div>

                <div class="daylight-duration">
                    <Clock size={16} />
                    <span>Daylight: {getDaylightDuration(sunTime.sunrise, sunTime.sunset)}</span>
                </div>
            </div>
        {/each}
    </div>
</div>

<style>
    .sun-times-display {
        padding: var(--space-4);
        max-width: 600px;
        margin: 0 auto;
        width: 100%;
    }

    .sun-times-title {
        font-size: var(--text-2xl);
        font-weight: bold;
        color: var(--color-primary);
        margin-bottom: var(--space-4);
        text-align: center;
    }

    .sun-times-list {
        display: flex;
        flex-direction: column;
        gap: var(--space-3);
    }

    .sun-time-item {
        background: var(--color-bg-secondary);
        border-radius: var(--radius-lg);
        padding: var(--space-4);
        border: 2px solid var(--color-border);
        transition: all var(--duration-normal) var(--ease-default);
    }

    .sun-time-item:hover {
        border-color: var(--color-primary);
        box-shadow: 0 4px 12px rgba(255, 149, 0, 0.2);
    }

    .sun-time-item.today {
        border-color: var(--color-primary);
        background: var(--color-primary-light);
    }

    .day-info {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: var(--space-3);
        padding-bottom: var(--space-3);
        border-bottom: 1px solid var(--color-border);
    }

    .day-label {
        font-size: var(--text-lg);
        font-weight: bold;
        color: var(--color-text-primary);
    }

    .date-label {
        font-size: var(--text-base);
        color: var(--color-text-secondary);
    }

    .times {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: var(--space-4);
    }

    .time-block {
        display: flex;
        align-items: center;
        gap: var(--space-3);
        padding: var(--space-2);
        background: var(--color-bg-tertiary);
        border-radius: var(--radius-md);
        border: 1px solid rgba(255, 149, 0, 0.3);
    }

    .time-icon {
        flex-shrink: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        color: var(--color-primary);
    }

    .time-info {
        display: flex;
        flex-direction: column;
        gap: var(--space-1);
    }

    .time-label {
        font-size: var(--text-xs);
        color: var(--color-text-secondary);
        text-transform: uppercase;
        letter-spacing: 0.5px;
    }

    .time-value {
        font-size: var(--text-xl);
        font-weight: bold;
        color: var(--color-primary);
        font-family: var(--font-mono);
    }

    .daylight-duration {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: var(--space-2);
        margin-top: var(--space-3);
        padding: var(--space-2);
        background: var(--color-bg-tertiary);
        border-radius: var(--radius-md);
        color: var(--color-primary);
        font-size: var(--text-base);
        font-weight: 600;
        border: 1px solid rgba(255, 149, 0, 0.3);
    }

    @media (max-width: 768px) {
        .sun-times-display {
            padding: var(--space-2);
        }

        .sun-times-title {
            font-size: var(--text-xl);
        }

        .times {
            gap: var(--space-2);
        }

        .time-block {
            flex-direction: column;
            text-align: center;
            gap: var(--space-2);
            padding: var(--space-3) var(--space-2);
        }

        .time-value {
            font-size: var(--text-base);
        }
    }
</style>
