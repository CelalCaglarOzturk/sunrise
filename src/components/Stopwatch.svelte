<script lang="ts">
    import { onDestroy } from "svelte";
    import type { Lap, Translation, Stopwatch as StopwatchType } from "../types";
    import { stopwatch as stopwatchStore } from "../stores";
    import Icon from "./Icon.svelte";
    import { Button } from "./ui";

    interface Props {
        t: Translation;
    }

    let { t }: Props = $props();

    let stopwatchData: StopwatchType = $state({ isRunning: false, isPaused: false, elapsedTime: 0, laps: [] });
    let intervalId: number | null = null;
    let startTime: number = 0;
    let pausedTime: number = 0;

    const unsubStopwatch = stopwatchStore.subscribe((value) => (stopwatchData = value));

    let isRunning = $derived(stopwatchData.isRunning);
    let isPaused = $derived(stopwatchData.isPaused);
    let elapsedTime = $derived(stopwatchData.elapsedTime);
    let laps = $derived(stopwatchData.laps);
    let formattedTime = $derived(formatTime(elapsedTime));

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
            startTime = Date.now() - pausedTime;
        } else {
            startTime = Date.now();
            pausedTime = 0;
        }

        stopwatchStore.update({ isRunning: true, isPaused: false });

        intervalId = window.setInterval(() => {
            const currentTime = Date.now() - startTime;
            stopwatchStore.update({ elapsedTime: currentTime });
        }, 10);
    }

    function pause() {
        if (intervalId !== null) {
            clearInterval(intervalId);
            intervalId = null;
        }

        pausedTime = elapsedTime;
        stopwatchStore.update({ isRunning: false, isPaused: true });
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

        const previousLapTime = laps.length > 0 ? laps[laps.length - 1].splitTime : 0;
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
        return laps.reduce((fastest, lap) => (lap.time < fastest.time ? lap : fastest));
    }

    function getSlowestLap(): Lap | null {
        if (laps.length === 0) return null;
        return laps.reduce((slowest, lap) => (lap.time > slowest.time ? lap : slowest));
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
            <Button variant="primary" onclick={start} size="lg">
                <Icon name="play" size={20} /> {t.start}
            </Button>
            <Button variant="ghost" onclick={addLap} disabled size="lg">
                <Icon name="clock" size={20} /> {t.lap}
            </Button>
        {:else if isRunning}
            <Button variant="secondary" onclick={pause} size="lg">
                <Icon name="pause" size={20} /> {t.pause}
            </Button>
            <Button variant="ghost" onclick={addLap} size="lg">
                <Icon name="clock" size={20} /> {t.lap}
            </Button>
        {:else if isPaused}
            <Button variant="primary" onclick={start} size="lg">
                <Icon name="play" size={20} /> {t.resume}
            </Button>
            <Button variant="secondary" onclick={reset} size="lg">
                <Icon name="rotate-ccw" size={20} /> {t.reset}
            </Button>
        {/if}
    </div>

    {#if laps.length > 0}
        <div class="laps-section">
            <div class="laps-header">
                <h3>{t.laps} ({laps.length})</h3>
                <Button variant="danger" size="sm" onclick={clearLaps}>
                    <Icon name="trash" size={18} /> {t.clearLaps}
                </Button>
            </div>

            <div class="laps-list">
                {#each [...laps].reverse() as lap (lap.id)}
                    {@const fastest = getFastestLap()}
                    {@const slowest = getSlowestLap()}
                    <div class="lap-item" class:fastest={fastest && lap.id === fastest.id} class:slowest={slowest && lap.id === slowest.id}>
                        <div class="lap-number">#{lap.lapNumber}</div>
                        <div class="lap-times">
                            <div class="lap-time-row">
                                <span class="lap-label">{t.lapTime}:</span>
                                <span class="lap-value">{formatTime(lap.time)}</span>
                            </div>
                            <div class="split-time-row">
                                <span class="lap-label">{t.splitTime}:</span>
                                <span class="lap-value">{formatTime(lap.splitTime)}</span>
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
        padding: var(--space-4);
        max-width: 600px;
        margin: 0 auto;
    }

    .stopwatch-display {
        display: flex;
        justify-content: center;
        align-items: center;
        padding: 3rem var(--space-4);
        margin-bottom: var(--space-8);
    }

    .stopwatch-time {
        font-size: 4rem;
        font-weight: bold;
        color: var(--color-text-primary);
        font-family: var(--font-mono);
        text-shadow: 0 2px 10px rgba(255, 149, 0, 0.3);
        letter-spacing: 0.05em;
    }

    .stopwatch-controls {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: var(--space-4);
        margin-bottom: var(--space-8);
    }

    .laps-section {
        background: var(--color-bg-secondary);
        border-radius: var(--radius-lg);
        padding: var(--space-6);
        border: 2px solid var(--color-border);
    }

    .laps-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: var(--space-4);
    }

    .laps-header h3 {
        margin: 0;
        color: var(--color-primary);
        font-size: var(--text-xl);
    }

    .laps-list {
        display: flex;
        flex-direction: column;
        gap: var(--space-3);
        max-height: 400px;
        overflow-y: auto;
    }

    .lap-item {
        display: flex;
        align-items: center;
        gap: var(--space-4);
        padding: var(--space-4);
        background: var(--color-bg-secondary);
        border-radius: var(--radius-md);
        border: 2px solid var(--color-border);
        transition: all var(--duration-normal);
    }

    .lap-item:hover {
        border-color: var(--color-border-hover);
    }

    .lap-item.fastest {
        border-color: var(--color-primary);
        background: var(--color-primary-light);
    }

    .lap-item.slowest {
        border-color: var(--color-border);
        background: rgba(102, 102, 102, 0.05);
    }

    .lap-number {
        flex-shrink: 0;
        width: 50px;
        height: 50px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: var(--color-bg-elevated);
        border-radius: 50%;
        font-weight: bold;
        color: var(--color-text-primary);
        font-size: var(--text-lg);
    }

    .lap-item.fastest .lap-number {
        background: var(--color-primary);
        color: #000;
    }

    .lap-item.slowest .lap-number {
        background: var(--color-bg-elevated);
        color: var(--color-text-secondary);
    }

    .lap-times {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: var(--space-2);
    }

    .lap-time-row,
    .split-time-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
    }

    .lap-label {
        font-size: var(--text-sm);
        color: var(--color-text-secondary);
    }

    .lap-value {
        font-size: var(--text-lg);
        font-weight: 600;
        color: var(--color-text-primary);
        font-family: var(--font-mono);
    }

    .laps-list::-webkit-scrollbar {
        width: 6px;
    }

    .laps-list::-webkit-scrollbar-track {
        background: var(--color-bg-secondary);
        border-radius: 3px;
    }

    .laps-list::-webkit-scrollbar-thumb {
        background: var(--color-bg-elevated);
        border-radius: 3px;
    }

    .laps-list::-webkit-scrollbar-thumb:hover {
        background: var(--color-border-hover);
    }

    @media (max-width: 768px) {
        .stopwatch-time {
            font-size: 3rem;
        }

        .lap-number {
            width: 40px;
            height: 40px;
            font-size: var(--text-base);
        }

        .lap-value {
            font-size: var(--text-base);
        }
    }
</style>
