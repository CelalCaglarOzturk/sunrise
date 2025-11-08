<script lang="ts">
    import { onMount } from "svelte";
    import type { Alarm } from "../types";
    import { sunCalcService } from "../services/sunCalc";

    export let alarms: Alarm[] = [];
    export let currentTime: Date = new Date();
    export let use24Hour: boolean = true;
    export let useSunBased: boolean = false;
    export let sunriseTime: Date | null = null;

    let clockSize = 300;
    let centerX = 150;
    let centerY = 150;
    let radius = 120;

    $: hours = currentTime.getHours();
    $: minutes = currentTime.getMinutes();
    $: seconds = currentTime.getSeconds();

    // Calculate hour hand angle (30 degrees per hour, 0.5 degrees per minute)
    $: hourAngle =
        useSunBased && sunriseTime
            ? ((hours * 60 + minutes) / 1440) * 360 - 90
            : (hours % 12) * 30 + minutes * 0.5 - 90;

    // Calculate minute hand angle (6 degrees per minute)
    $: minuteAngle = minutes * 6 - 90;

    // Calculate second hand angle (6 degrees per second)
    $: secondAngle = seconds * 6 - 90;

    // Calculate hour markers
    function getHourMarkers() {
        const markers = [];
        const count = useSunBased ? 24 : 12;
        for (let i = 0; i < count; i++) {
            const angle = (i * (360 / count) - 90) * (Math.PI / 180);
            const outerRadius = radius - 5;
            const innerRadius =
                i % (useSunBased ? 6 : 3) === 0 ? radius - 15 : radius - 10;

            const x1 = centerX + Math.cos(angle) * innerRadius;
            const y1 = centerY + Math.sin(angle) * innerRadius;
            const x2 = centerX + Math.cos(angle) * outerRadius;
            const y2 = centerY + Math.sin(angle) * outerRadius;

            markers.push({
                x1,
                y1,
                x2,
                y2,
                major: i % (useSunBased ? 6 : 3) === 0,
            });
        }
        return markers;
    }

    // Calculate alarm positions on clock
    function getAlarmMarkers() {
        return alarms
            .filter((a) => a.enabled)
            .map((alarm) => {
                let angle: number;

                if (alarm.useSunTime && sunriseTime) {
                    // Sun-based alarm
                    const alarmDate = new Date(sunriseTime);
                    alarmDate.setMinutes(
                        alarmDate.getMinutes() + alarm.sunOffset,
                    );
                    const alarmMinutes =
                        alarmDate.getHours() * 60 + alarmDate.getMinutes();
                    angle = (alarmMinutes / 1440) * 360 - 90;
                } else {
                    // Normal alarm
                    const [h, m] = alarm.time.split(":").map(Number);
                    const totalMinutes = (h % 12) * 60 + m;
                    angle = (totalMinutes / 720) * 360 - 90;
                }

                const rad = angle * (Math.PI / 180);
                const x = centerX + Math.cos(rad) * (radius - 25);
                const y = centerY + Math.sin(rad) * (radius - 25);

                return { x, y, alarm, angle };
            });
    }

    // Get time display
    function getTimeDisplay(): string {
        if (useSunBased && sunriseTime) {
            const minutesSinceSunrise =
                (currentTime.getTime() - sunriseTime.getTime()) / 60000;
            const sunHours = Math.floor(minutesSinceSunrise / 60);
            const sunMinutes = Math.floor(minutesSinceSunrise % 60);
            return `${String(sunHours).padStart(2, "0")}:${String(sunMinutes).padStart(2, "0")}`;
        }

        if (use24Hour) {
            return `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}`;
        }

        const displayHours = hours % 12 || 12;
        const period = hours >= 12 ? "PM" : "AM";
        return `${displayHours}:${String(minutes).padStart(2, "0")} ${period}`;
    }

    $: hourMarkers = getHourMarkers();
    $: alarmMarkers = getAlarmMarkers();
    $: timeDisplay = getTimeDisplay();
</script>

<div class="circular-clock">
    <svg
        width={clockSize}
        height={clockSize}
        viewBox="0 0 {clockSize} {clockSize}"
    >
        <!-- Clock face -->
        <circle cx={centerX} cy={centerY} r={radius} class="clock-face" />

        <!-- Hour markers -->
        {#each hourMarkers as marker}
            <line
                x1={marker.x1}
                y1={marker.y1}
                x2={marker.x2}
                y2={marker.y2}
                class="hour-marker"
                class:major={marker.major}
            />
        {/each}

        <!-- Alarm markers -->
        {#each alarmMarkers as marker}
            <circle cx={marker.x} cy={marker.y} r="8" class="alarm-marker" />
            <circle
                cx={marker.x}
                cy={marker.y}
                r="4"
                class="alarm-marker-inner"
            />
        {/each}

        <!-- Hour hand -->
        <line
            x1={centerX}
            y1={centerY}
            x2={centerX +
                Math.cos((hourAngle * Math.PI) / 180) * (radius * 0.5)}
            y2={centerY +
                Math.sin((hourAngle * Math.PI) / 180) * (radius * 0.5)}
            class="hour-hand"
        />

        <!-- Minute hand -->
        <line
            x1={centerX}
            y1={centerY}
            x2={centerX +
                Math.cos((minuteAngle * Math.PI) / 180) * (radius * 0.7)}
            y2={centerY +
                Math.sin((minuteAngle * Math.PI) / 180) * (radius * 0.7)}
            class="minute-hand"
        />

        <!-- Second hand -->
        <line
            x1={centerX}
            y1={centerY}
            x2={centerX +
                Math.cos((secondAngle * Math.PI) / 180) * (radius * 0.8)}
            y2={centerY +
                Math.sin((secondAngle * Math.PI) / 180) * (radius * 0.8)}
            class="second-hand"
        />

        <!-- Center dot -->
        <circle cx={centerX} cy={centerY} r="6" class="center-dot" />
    </svg>

    <div class="time-display">
        {timeDisplay}
    </div>

    {#if useSunBased && sunriseTime}
        <div class="sun-mode-label">☀️ Sun-Based Time</div>
    {/if}
</div>

<style>
    .circular-clock {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 1rem;
        padding: 1rem;
    }

    svg {
        max-width: 100%;
        height: auto;
    }

    .clock-face {
        fill: var(--clock-face-bg, #1a1a2e);
        stroke: var(--clock-border, #0f3460);
        stroke-width: 3;
    }

    .hour-marker {
        stroke: var(--marker-color, #16213e);
        stroke-width: 2;
    }

    .hour-marker.major {
        stroke: var(--marker-major-color, #0f3460);
        stroke-width: 3;
    }

    .hour-hand {
        stroke: var(--hour-hand-color, #e94560);
        stroke-width: 6;
        stroke-linecap: round;
    }

    .minute-hand {
        stroke: var(--minute-hand-color, #e94560);
        stroke-width: 4;
        stroke-linecap: round;
    }

    .second-hand {
        stroke: var(--second-hand-color, #ff6b9d);
        stroke-width: 2;
        stroke-linecap: round;
    }

    .center-dot {
        fill: var(--center-dot-color, #e94560);
    }

    .alarm-marker {
        fill: var(--alarm-marker-bg, #ffd700);
        stroke: var(--alarm-marker-border, #ff8c00);
        stroke-width: 2;
        cursor: pointer;
        transition: all 0.2s ease;
    }

    .alarm-marker:hover {
        r: 10;
    }

    .alarm-marker-inner {
        fill: var(--alarm-inner-color, #ff8c00);
        pointer-events: none;
    }

    .time-display {
        font-size: 2.5rem;
        font-weight: bold;
        color: var(--time-color, #e94560);
        font-family: "SF Mono", "Monaco", "Inconsolata", "Fira Code", monospace;
        text-shadow: 0 2px 10px rgba(233, 69, 96, 0.3);
    }

    .sun-mode-label {
        font-size: 0.9rem;
        color: var(--sun-label-color, #ffd700);
        font-weight: 500;
        padding: 0.3rem 0.8rem;
        background: rgba(255, 215, 0, 0.1);
        border-radius: 12px;
        border: 1px solid rgba(255, 215, 0, 0.3);
    }

    @media (max-width: 768px) {
        .time-display {
            font-size: 2rem;
        }
    }
</style>
