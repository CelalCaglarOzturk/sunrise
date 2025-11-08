<script lang="ts">
    import { onMount, onDestroy } from "svelte";
    import { Sun } from "@lucide/svelte";

    export let use24Hour: boolean = true;
    export let useSunBased: boolean = false;
    export let sunriseTime: Date | null = null;

    let currentTime = "";
    let currentDate = "";
    let intervalId: number | null = null;

    function updateTime() {
        const now = new Date();

        // Format time
        if (useSunBased && sunriseTime) {
            // Calculate time since sunrise
            const minutesSinceSunrise =
                (now.getTime() - sunriseTime.getTime()) / 60000;

            // Handle time before sunrise (show as negative)
            const isBeforeSunrise = minutesSinceSunrise < 0;
            const absMinutes = Math.abs(minutesSinceSunrise);

            const sunHours = Math.floor(absMinutes / 60);
            const sunMinutes = Math.floor(absMinutes % 60);
            const sunSeconds = Math.floor(((absMinutes % 1) * 60));

            const sign = isBeforeSunrise ? "-" : "";
            currentTime = `${sign}${String(sunHours).padStart(2, "0")}:${String(sunMinutes).padStart(2, "0")}:${String(sunSeconds).padStart(2, "0")}`;
        } else {
            const hours = now.getHours();
            const minutes = now.getMinutes();
            const seconds = now.getSeconds();

            if (use24Hour) {
                currentTime = `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
            } else {
                const displayHours = hours % 12 || 12;
                const period = hours >= 12 ? "PM" : "AM";
                currentTime = `${String(displayHours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")} ${period}`;
            }
        }

        // Format date
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
        padding: 2rem 1rem;
        background: #1a1a1a;
        border-radius: 16px;
        margin: 0 1rem 1rem;
        border: 2px solid #333;
    }

    .time {
        font-size: 3.5rem;
        font-weight: bold;
        color: #fff;
        font-family: "SF Mono", "Monaco", "Inconsolata", monospace;
        letter-spacing: 0.05em;
        text-shadow: 0 2px 10px rgba(255, 149, 0, 0.3);
    }

    .date {
        font-size: 1.1rem;
        color: #888;
        margin-top: 0.5rem;
        font-weight: 500;
    }

    .sun-mode-indicator {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        font-size: 0.9rem;
        color: #ff9500;
        margin-top: 0.5rem;
        padding: 0.3rem 0.8rem;
        background: rgba(255, 149, 0, 0.1);
        border-radius: 12px;
        border: 1px solid rgba(255, 149, 0, 0.3);
        font-weight: 500;
    }

    .digital-clock.sun-based {
        border-color: rgba(255, 149, 0, 0.3);
    }

    @media (max-width: 768px) {
        .digital-clock {
            padding: 1.5rem 1rem;
            margin: 0 0.5rem 0.75rem;
        }

        .time {
            font-size: 2.5rem;
        }

        .date {
            font-size: 0.95rem;
        }
    }
</style>
