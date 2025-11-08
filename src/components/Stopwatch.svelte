<script lang="ts">
    import { onDestroy } from "svelte";
    import type {
        Lap,
        Translation,
        Stopwatch as StopwatchType,
    } from "../types";
    import { stopwatch as stopwatchStore } from "../stores";
    import Icon from "./Icon.svelte";

    export let t: Translation;

    let stopwatchData: StopwatchType;
    let intervalId: number | null = null;
    let startTime: number = 0;
    let pausedTime: number = 0;

    const unsubStopwatch = stopwatchStore.subscribe(
        (value) => (stopwatchData = value),
    );

    $: isRunning = stopwatchData.isRunning;
    $: isPaused = stopwatchData.isPaused;
    $: elapsedTime = stopwatchData.elapsedTime;
    $: laps = stopwatchData.laps;
    $: formattedTime = formatTime(elapsedTime);

    function formatTime(ms: number): string {
        const totalSeconds = Math.floor(ms / 1000);
        const hours = Math.floor(totalSeconds / 3600);
        const minutes = Math.floor((totalSeconds % 3600) / 60);
        const seconds = totalSeconds % 60;
        const milliseconds = Math.floor((ms % 1000) / 10);

        if (hours > 0) {
            return `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}.${String(milliseconds).padStart(2, "0")}`;
        }
        return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}.${String(milliseconds).padStart(2, "0")}`;
    }

    function start() {
        if (isPaused) {
            // Resume from pause
            startTime = Date.now() - pausedTime;
        } else {
            // Fresh start
            startTime = Date.now();
            pausedTime = 0;
        }

        stopwatchStore.update({
            isRunning: true,
            isPaused: false,
        });

        intervalId = window.setInterval(() => {
            const currentTime = Date.now() - startTime;
            stopwatchStore.update({
                elapsedTime: currentTime,
            });
        }, 10);
    }

    function pause() {
        if (intervalId !== null) {
            clearInterval(intervalId);
            intervalId = null;
        }

        pausedTime = elapsedTime;
        stopwatchStore.update({
            isRunning: false,
            isPaused: true,
        });
    }

    function reset() {
        if (intervalId !== null) {
            clearInterval(intervalId);
            intervalId = null;
        }

        pausedTime = 0;
        startTime = 0;
        stopwatchStore.reset();
    }

    function addLap() {
        if (!isRunning) return;

        const previousLapTime =
            laps.length > 0 ? laps[laps.length - 1].splitTime : 0;
        const lapTime = elapsedTime - previousLapTime;

        const lap: Lap = {
            id: `lap_${Date.now()}`,
            lapNumber: laps.length + 1,
            time: lapTime,
            splitTime: elapsedTime,
        };

        stopwatchStore.addLap(lap);
    }

    function clearLaps() {
        stopwatchStore.update({ laps: [] });
    }

    function getFastestLap(): Lap | null {
        if (laps.length === 0) return null;
        return laps.reduce((fastest, lap) =>
            lap.time < fastest.time ? lap : fastest,
        );
    }

    function getSlowestLap(): Lap | null {
        if (laps.length === 0) return null;
        return laps.reduce((slowest, lap) =>
            lap.time > slowest.time ? lap : slowest,
        );
    }

    onDestroy(() => {
        if (intervalId !== null) {
            clearInterval(intervalId);
        }
        unsubStopwatch();
    });
</script>

<div class="stopwatch-container">
    <div class="stopwatch-display">
        <div class="stopwatch-time">{formattedTime}</div>
    </div>

    <div class="stopwatch-controls">
        {#if !isRunning && !isPaused}
            <button class="btn-stopwatch btn-start" on:click={start}>
                <Icon name="play" size={20} />
                {t.start}
            </button>
            <button class="btn-stopwatch btn-lap" disabled style="opacity: 0.3">
                <Icon name="clock" size={20} />
                {t.lap}
            </button>
        {:else if isRunning}
            <button class="btn-stopwatch btn-pause" on:click={pause}>
                <Icon name="pause" size={20} />
                {t.pause}
            </button>
            <button class="btn-stopwatch btn-lap" on:click={addLap}>
                <Icon name="clock" size={20} />
                {t.lap}
            </button>
        {:else if isPaused}
            <button class="btn-stopwatch btn-resume" on:click={start}>
                <Icon name="play" size={20} />
                {t.resume}
            </button>
            <button class="btn-stopwatch btn-reset" on:click={reset}>
                <Icon name="rotate-ccw" size={20} />
                {t.reset}
            </button>
        {/if}
    </div>

    {#if laps.length > 0}
        <div class="laps-section">
            <div class="laps-header">
                <h3>{t.laps} ({laps.length})</h3>
                <button class="btn-clear-laps" on:click={clearLaps}>
                    <Icon name="trash" size={18} />
                    {t.clearLaps}
                </button>
            </div>

            <div class="laps-list">
                {#each [...laps].reverse() as lap (lap.id)}
                    {@const fastest = getFastestLap()}
                    {@const slowest = getSlowestLap()}
                    <div
                        class="lap-item"
                        class:fastest={fastest && lap.id === fastest.id}
                        class:slowest={slowest && lap.id === slowest.id}
                    >
                        <div class="lap-number">#{lap.lapNumber}</div>
                        <div class="lap-times">
                            <div class="lap-time">
                                <span class="lap-label">{t.lapTime}:</span>
                                <span class="lap-value"
                                    >{formatTime(lap.time)}</span
                                >
                            </div>
                            <div class="split-time">
                                <span class="lap-label">{t.splitTime}:</span>
                                <span class="lap-value"
                                    >{formatTime(lap.splitTime)}</span
                                >
                            </div>
                        </div>
                    </div>
                {/each}
            </div>
        </div>
    {/if}
</div>

<style>
    .stopwatch-container {
        padding: 1rem;
        max-width: 600px;
        margin: 0 auto;
    }

    .stopwatch-display {
        display: flex;
        justify-content: center;
        align-items: center;
        padding: 3rem 1rem;
        margin-bottom: 2rem;
    }

    .stopwatch-time {
        font-size: 4rem;
        font-weight: bold;
        color: #fff;
        font-family: "SF Mono", "Monaco", "Inconsolata", monospace;
        text-shadow: 0 2px 10px rgba(255, 149, 0, 0.3);
        letter-spacing: 0.05em;
    }

    .stopwatch-controls {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 1rem;
        margin-bottom: 2rem;
    }

    .btn-stopwatch {
        padding: 1.25rem;
        border: none;
        border-radius: 16px;
        font-size: 1.1rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 0.5rem;
    }

    .btn-stopwatch:disabled {
        cursor: not-allowed;
    }

    .btn-start,
    .btn-resume {
        background: #ff9500;
        color: white;
    }

    .btn-start:hover,
    .btn-resume:hover {
        background: #e68600;
        transform: translateY(-2px);
        box-shadow: 0 4px 15px rgba(255, 149, 0, 0.4);
    }

    .btn-pause {
        background: #555;
        color: white;
    }

    .btn-pause:hover {
        background: #666;
        transform: translateY(-2px);
        box-shadow: 0 4px 15px rgba(85, 85, 85, 0.4);
    }

    .btn-lap {
        background: #333;
        color: white;
    }

    .btn-lap:hover:not(:disabled) {
        background: #444;
        transform: translateY(-2px);
    }

    .btn-reset {
        background: #555;
        color: white;
    }

    .btn-reset:hover {
        background: #666;
        transform: translateY(-2px);
    }

    .laps-section {
        background: #1a1a1a;
        border-radius: 16px;
        padding: 1.5rem;
        border: 2px solid #333;
    }

    .laps-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1rem;
    }

    .laps-header h3 {
        margin: 0;
        color: #ff9500;
        font-size: 1.3rem;
    }

    .btn-clear-laps {
        padding: 0.5rem 1rem;
        background: #555;
        border: none;
        border-radius: 10px;
        color: white;
        font-size: 0.9rem;
        cursor: pointer;
        transition: all 0.2s;
    }

    .btn-clear-laps:hover {
        background: #666;
        transform: translateY(-1px);
    }

    .laps-list {
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
        max-height: 400px;
        overflow-y: auto;
    }

    .lap-item {
        display: flex;
        align-items: center;
        gap: 1rem;
        padding: 1rem;
        background: #1a1a1a;
        border-radius: 12px;
        border: 2px solid #333;
        transition: all 0.2s;
    }

    .lap-item:hover {
        border-color: #555;
    }

    .lap-item.fastest {
        border-color: #ff9500;
        background: rgba(255, 149, 0, 0.05);
    }

    .lap-item.slowest {
        border-color: #666;
        background: rgba(102, 102, 102, 0.05);
    }

    .lap-number {
        flex-shrink: 0;
        width: 50px;
        height: 50px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #333;
        border-radius: 50%;
        font-weight: bold;
        color: #fff;
        font-size: 1.1rem;
    }

    .lap-item.fastest .lap-number {
        background: #ff9500;
        color: #000;
    }

    .lap-item.slowest .lap-number {
        background: #555;
        color: #fff;
    }

    .lap-times {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
    }

    .lap-time,
    .split-time {
        display: flex;
        justify-content: space-between;
        align-items: center;
    }

    .lap-label {
        font-size: 0.85rem;
        color: var(--text-secondary, #888);
    }

    .lap-value {
        font-size: 1.1rem;
        font-weight: 600;
        color: var(--text-primary, #fff);
        font-family: "SF Mono", "Monaco", "Inconsolata", monospace;
    }

    /* Scrollbar styling for laps list */
    .laps-list::-webkit-scrollbar {
        width: 6px;
    }

    .laps-list::-webkit-scrollbar-track {
        background: #1a1a1a;
        border-radius: 3px;
    }

    .laps-list::-webkit-scrollbar-thumb {
        background: #333;
        border-radius: 3px;
    }

    .laps-list::-webkit-scrollbar-thumb:hover {
        background: #555;
    }

    @media (max-width: 768px) {
        .stopwatch-time {
            font-size: 3rem;
        }

        .btn-stopwatch {
            padding: 1rem;
            font-size: 1rem;
        }

        .lap-number {
            width: 40px;
            height: 40px;
            font-size: 0.9rem;
        }

        .lap-value {
            font-size: 0.95rem;
        }
    }
</style>
