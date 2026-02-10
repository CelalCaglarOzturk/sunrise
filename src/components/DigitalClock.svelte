<script lang="ts">
    import { onMount, onDestroy } from "svelte";
    import { Sun } from "@lucide/svelte";

    interface Props {
        use24Hour?: boolean;
        showSeconds?: boolean;
        useSunBased?: boolean;
        sunriseTime?: Date | null;
    }

    let { 
        use24Hour = true, 
        showSeconds = true, 
        useSunBased = false, 
        sunriseTime = null 
    }: Props = $props();

    let currentTime = $state("");
    let currentDate = $state("");
    let intervalId: number | null = null;

    function updateTime() {
        const now = new Date();

        if (useSunBased && sunriseTime) {
            const minutesSinceSunrise = (now.getTime() - sunriseTime.getTime()) / 60000;
            const isBeforeSunrise = minutesSinceSunrise < 0;
            const absMinutes = Math.abs(minutesSinceSunrise);

            const sunHours = Math.floor(absMinutes / 60);
            const sunMinutes = Math.floor(absMinutes % 60);
            const sunSeconds = Math.floor(((absMinutes % 1) * 60));

            const sign = isBeforeSunrise ? "-" : "";
            if (showSeconds) {
                currentTime = `${sign}${String(sunHours).padStart(2, "0")}:${String(sunMinutes).padStart(2, "0")}:${String(sunSeconds).padStart(2, "0")}`;
            } else {
                currentTime = `${sign}${String(sunHours).padStart(2, "0")}:${String(sunMinutes).padStart(2, "0")}`;
            }
        } else {
            const hours = now.getHours();
            const minutes = now.getMinutes();
            const seconds = now.getSeconds();

            if (use24Hour) {
                if (showSeconds) {
                    currentTime = `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
                } else {
                    currentTime = `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}`;
                }
            } else {
                const displayHours = hours % 12 || 12;
                const period = hours >= 12 ? "PM" : "AM";
                if (showSeconds) {
                    currentTime = `${String(displayHours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")} ${period}`;
                } else {
                    currentTime = `${String(displayHours).padStart(2, "0")}:${String(minutes).padStart(2, "0")} ${period}`;
                }
            }
        }

        const options: Intl.DateTimeFormatOptions = {
            weekday: "long",
            year: "numeric",
            month: "long",
            day: "numeric",
        };
        currentDate = now.toLocaleDateString(undefined, options);
    }

    onMount(() => {
        updateTime();
        intervalId = window.setInterval(updateTime, 1000);
    });

    onDestroy(() => {
        if (intervalId !== null) {
            clearInterval(intervalId);
        }
    });
</script>

<div class="digital-clock" class:sun-based={useSunBased}>
    <div class="time">{currentTime}</div>
    <div class="date">{currentDate}</div>
    {#if useSunBased && sunriseTime}
        <div class="sun-mode-indicator">
            <Sun size={16} />
            Sun-Based Time
        </div>
    {/if}
</div>

<style>
    .digital-clock {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: var(--space-8) var(--space-4);
        background: var(--color-bg-secondary);
        border-radius: var(--radius-lg);
        margin: 0 var(--space-4) var(--space-4);
        border: 2px solid var(--color-border);
    }

    .time {
        font-size: 3.5rem;
        font-weight: bold;
        color: var(--color-text-primary);
        font-family: var(--font-mono);
        letter-spacing: 0.05em;
        text-shadow: 0 2px 10px rgba(255, 149, 0, 0.3);
    }

    .date {
        font-size: var(--text-lg);
        color: var(--color-text-secondary);
        margin-top: var(--space-2);
        font-weight: 500;
    }

    .sun-mode-indicator {
        display: flex;
        align-items: center;
        gap: var(--space-2);
        font-size: var(--text-sm);
        color: var(--color-primary);
        margin-top: var(--space-2);
        padding: var(--space-1) var(--space-3);
        background: var(--color-primary-light);
        border-radius: var(--radius-md);
        border: 1px solid rgba(255, 149, 0, 0.3);
        font-weight: 500;
    }

    .digital-clock.sun-based {
        border-color: rgba(255, 149, 0, 0.3);
    }

    @media (max-width: 768px) {
        .digital-clock {
            padding: var(--space-6) var(--space-4);
            margin: 0 var(--space-2) var(--space-3);
        }

        .time {
            font-size: 2.5rem;
        }

        .date {
            font-size: var(--text-base);
        }
    }
</style>
