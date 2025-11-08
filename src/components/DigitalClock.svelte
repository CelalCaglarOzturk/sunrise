<script lang="ts">
    import { onMount, onDestroy } from "svelte";
    import type { Translation } from "../types";

    export let t: Translation;
    export let use24Hour: boolean = true;

    let currentTime = "";
    let currentDate = "";
    let intervalId: number | null = null;

    function updateTime() {
        const now = new Date();

        // Format time
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

<div class="digital-clock">
    <div class="time">{currentTime}</div>
    <div class="date">{currentDate}</div>
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
