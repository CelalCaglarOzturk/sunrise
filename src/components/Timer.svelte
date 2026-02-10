<script lang="ts">
    import { onDestroy } from "svelte";
    import type { Timer, Translation } from "../types";
    import { filePickerService } from "../services/filePicker";
    import { timers as timersStore, settings } from "../stores";
    import Icon from "./Icon.svelte";
    import { Bell, Vibrate } from "@lucide/svelte";
    import { Button, Toggle } from "./ui";

    interface Props {
        t: Translation;
    }

    let { t }: Props = $props();

    let timersData: Timer[] = $state([]);
    let settingsData = $state<any>({});

    const unsubTimers = timersStore.subscribe((value) => (timersData = value));
    const unsubSettings = settings.subscribe((value) => (settingsData = value));

    onDestroy(() => {
        unsubTimers();
        unsubSettings();
    });

    let showAddTimer = $state(false);
    let newTimerLabel = $state("");
    let newTimerHours = $state(0);
    let newTimerMinutes = $state(1);
    let newTimerSeconds = $state(0);
    let newTimerSound = $state("");
    let newTimerSoundName = $state("Default");
    let newTimerVibrate = $state(true);

    // Wheel picker refs
    let hoursWheelRef: HTMLDivElement;
    let minutesWheelRef: HTMLDivElement;
    let secondsWheelRef: HTMLDivElement;

    let timerIntervals: Map<string, number> = new Map();

    function formatTime(seconds: number): string {
        const hrs = Math.floor(seconds / 3600);
        const mins = Math.floor((seconds % 3600) / 60);
        const secs = seconds % 60;

        if (hrs > 0) {
            return `${String(hrs).padStart(2, "0")}:${String(mins).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
        }
        return `${String(mins).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
    }

    function getHourOptions(): number[] { return Array.from({ length: 24 }, (_, i) => i); }
    function getMinuteOptions(): number[] { return Array.from({ length: 60 }, (_, i) => i); }
    function getSecondOptions(): number[] { return Array.from({ length: 60 }, (_, i) => i); }

    function scrollToValue(container: HTMLDivElement, value: number, options: number[]) {
        if (!container) return;
        const index = options.indexOf(value);
        if (index === -1) return;
        const itemHeight = 40;
        const scrollPosition = index * itemHeight;
        container.scrollTo({ top: scrollPosition, behavior: "smooth" });
    }

    let scrollTimeout: number;
    function handleWheelScroll(container: HTMLDivElement, options: number[], setter: (value: number) => void) {
        if (!container) return;
        clearTimeout(scrollTimeout);
        scrollTimeout = window.setTimeout(() => {
            const itemHeight = 40;
            const scrollTop = container.scrollTop;
            const index = Math.round(scrollTop / itemHeight);
            const clampedIndex = Math.max(0, Math.min(index, options.length - 1));
            container.scrollTo({ top: clampedIndex * itemHeight, behavior: "smooth" });
            setter(options[clampedIndex]);
        }, 100);
    }

    $effect(() => {
        if (showAddTimer && hoursWheelRef && minutesWheelRef && secondsWheelRef) {
            setTimeout(() => {
                scrollToValue(hoursWheelRef, newTimerHours, getHourOptions());
                scrollToValue(minutesWheelRef, newTimerMinutes, getMinuteOptions());
                scrollToValue(secondsWheelRef, newTimerSeconds, getSecondOptions());
            }, 50);
        }
    });

    function createTimer() {
        const duration = newTimerHours * 3600 + newTimerMinutes * 60 + newTimerSeconds;
        if (duration <= 0) {
            alert("Please set a valid duration");
            return;
        }

        const timer: Timer = {
            id: `timer_${Date.now()}`,
            label: newTimerLabel || `Timer ${timersData.length + 1}`,
            duration,
            remainingTime: duration,
            isRunning: false,
            isPaused: false,
            soundUri: newTimerSound,
            soundName: newTimerSoundName,
            vibrate: newTimerVibrate,
        };

        timersStore.add(timer);
        resetForm();
        showAddTimer = false;
    }

    function resetForm() {
        newTimerLabel = "";
        newTimerHours = 0;
        newTimerMinutes = 1;
        newTimerSeconds = 0;
        newTimerSound = "";
        newTimerSoundName = "Default";
        newTimerVibrate = true;
    }

    function startTimer(id: string) {
        const timer = timersData.find((t) => t.id === id);
        if (!timer) return;

        timersStore.update(id, { isRunning: true, isPaused: false });

        const interval = window.setInterval(() => {
            const currentTimer = timersData.find((t) => t.id === id);
            if (!currentTimer || !currentTimer.isRunning) {
                clearInterval(interval);
                timerIntervals.delete(id);
                return;
            }

            if (currentTimer.remainingTime <= 0) {
                clearInterval(interval);
                timerIntervals.delete(id);
                timersStore.update(id, { isRunning: false, remainingTime: 0 });
                return;
            }

            timersStore.update(id, { remainingTime: currentTimer.remainingTime - 1 });
        }, 1000);

        timerIntervals.set(id, interval);
    }

    function pauseTimer(id: string) {
        const interval = timerIntervals.get(id);
        if (interval) {
            clearInterval(interval);
            timerIntervals.delete(id);
        }
        timersStore.update(id, { isRunning: false, isPaused: true });
    }

    function resumeTimer(id: string) { startTimer(id); }

    function resetTimer(id: string) {
        const interval = timerIntervals.get(id);
        if (interval) {
            clearInterval(interval);
            timerIntervals.delete(id);
        }

        const timer = timersData.find((t) => t.id === id);
        if (!timer) return;

        timersStore.update(id, { isRunning: false, isPaused: false, remainingTime: timer.duration });
    }

    function deleteTimer(timerId: string) {
        const interval = timerIntervals.get(timerId);
        if (interval) {
            clearInterval(interval);
            timerIntervals.delete(timerId);
        }
        timersStore.remove(timerId);
    }

    async function selectSound() {
        const file = await filePickerService.pickAudioFile();
        if (file) {
            newTimerSound = file.uri;
            newTimerSoundName = file.name;
        }
    }

    function getProgressPercentage(timer: Timer): number {
        return ((timer.duration - timer.remainingTime) / timer.duration) * 100;
    }

    onDestroy(() => {
        timerIntervals.forEach((interval) => clearInterval(interval));
        timerIntervals.clear();
    });
</script>

<div class="timer-container">
    {#if timersData.length === 0 && !showAddTimer}
        <div class="empty-state">
            <div class="empty-icon">
                <Icon name="timer" size={64} color="#888" />
            </div>
            <h3>{t.noTimersYet}</h3>
            <p>{t.createFirstTimer}</p>
        </div>
    {/if}

    <div class="timers-list">
        {#each timersData as timer (timer.id)}
            <div class="timer-item" class:running={timer.isRunning} class:finished={timer.remainingTime === 0}>
                <div class="timer-header">
                    <div class="timer-label">{timer.label}</div>
                    <button class="btn-delete-timer" onclick={() => deleteTimer(timer.id)} type="button">
                        <Icon name="trash" size={18} />
                    </button>
                </div>

                <div class="timer-display">
                    <div class="timer-time">{formatTime(timer.remainingTime)}</div>
                    <div class="timer-total">/ {formatTime(timer.duration)}</div>
                </div>

                <div class="timer-progress">
                    <div class="timer-progress-bar" style="width: {getProgressPercentage(timer)}%"></div>
                </div>

                <div class="timer-controls">
                    {#if !timer.isRunning && !timer.isPaused}
                        <Button variant="primary" onclick={() => startTimer(timer.id)} fullWidth>
                            <Icon name="play" size={16} /> {t.start}
                        </Button>
                    {:else if timer.isRunning}
                        <Button variant="secondary" onclick={() => pauseTimer(timer.id)} fullWidth>
                            <Icon name="pause" size={16} /> {t.pause}
                        </Button>
                    {:else if timer.isPaused}
                        <Button variant="primary" onclick={() => resumeTimer(timer.id)} fullWidth>
                            <Icon name="play" size={16} /> {t.resume}
                        </Button>
                    {/if}
                    <Button variant="ghost" onclick={() => resetTimer(timer.id)} fullWidth>
                        <Icon name="rotate-ccw" size={16} /> {t.reset}
                    </Button>
                </div>

                {#if timer.remainingTime === 0}
                    <div class="timer-finished">{t.timerFinished}</div>
                {/if}
            </div>
        {/each}
    </div>

    {#if showAddTimer}
        <div class="timer-editor">
            <div class="timer-editor-header">
                <h3>{t.addTimer}</h3>
                <button class="btn-close-editor" onclick={() => (showAddTimer = false)} type="button">
                    <Icon name="x" size={20} />
                </button>
            </div>

            <div class="form-group">
                <label for="timer-label">{t.label}</label>
                <input id="timer-label" type="text" bind:value={newTimerLabel} placeholder={t.label} class="form-input" />
            </div>

            <div class="form-group">
                <label>{t.duration}</label>
                <div class="time-picker-container">
                    <div class="time-picker-column">
                        <div class="picker-label">Hours</div>
                        <div class="wheel-picker-wrapper">
                            <div class="wheel-picker-overlay"></div>
                            <div class="wheel-picker-selection"></div>
                            <div class="wheel-picker" bind:this={hoursWheelRef} onscroll={() => handleWheelScroll(hoursWheelRef, getHourOptions(), (v) => (newTimerHours = v))}>
                                {#each getHourOptions() as hour}
                                    <div class="wheel-item" class:selected={newTimerHours === hour} onclick={() => { newTimerHours = hour; scrollToValue(hoursWheelRef, hour, getHourOptions()); }}>
                                        {hour.toString().padStart(2, "0")}
                                    </div>
                                {/each}
                            </div>
                        </div>
                    </div>
                    <div class="time-picker-separator">:</div>
                    <div class="time-picker-column">
                        <div class="picker-label">Minutes</div>
                        <div class="wheel-picker-wrapper">
                            <div class="wheel-picker-overlay"></div>
                            <div class="wheel-picker-selection"></div>
                            <div class="wheel-picker" bind:this={minutesWheelRef} onscroll={() => handleWheelScroll(minutesWheelRef, getMinuteOptions(), (v) => (newTimerMinutes = v))}>
                                {#each getMinuteOptions() as minute}
                                    <div class="wheel-item" class:selected={newTimerMinutes === minute} onclick={() => { newTimerMinutes = minute; scrollToValue(minutesWheelRef, minute, getMinuteOptions()); }}>
                                        {minute.toString().padStart(2, "0")}
                                    </div>
                                {/each}
                            </div>
                        </div>
                    </div>
                    <div class="time-picker-separator">:</div>
                    <div class="time-picker-column">
                        <div class="picker-label">Seconds</div>
                        <div class="wheel-picker-wrapper">
                            <div class="wheel-picker-overlay"></div>
                            <div class="wheel-picker-selection"></div>
                            <div class="wheel-picker" bind:this={secondsWheelRef} onscroll={() => handleWheelScroll(secondsWheelRef, getSecondOptions(), (v) => (newTimerSeconds = v))}>
                                {#each getSecondOptions() as second}
                                    <div class="wheel-item" class:selected={newTimerSeconds === second} onclick={() => { newTimerSeconds = second; scrollToValue(secondsWheelRef, second, getSecondOptions()); }}>
                                        {second.toString().padStart(2, "0")}
                                    </div>
                                {/each}
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="form-group">
                <label>{t.alarmSound}</label>
                <button class="sound-select-btn" onclick={selectSound} type="button">
                    <Bell size={16} /> {newTimerSoundName}
                </button>
            </div>

            <div class="form-group">
                <div class="toggle-option-row">
                    <span class="toggle-option-text">
                        <Vibrate size={16} /> {t.vibrate}
                    </span>
                    <Toggle bind:checked={newTimerVibrate} />
                </div>
            </div>

            <div class="timer-editor-actions">
                <Button variant="secondary" onclick={() => { showAddTimer = false; resetForm(); }} fullWidth>{t.cancel}</Button>
                <Button variant="primary" onclick={createTimer} fullWidth>{t.save}</Button>
            </div>
        </div>
    {/if}

    {#if !showAddTimer}
        <Button variant="primary" onclick={() => (showAddTimer = true)} fullWidth size="lg">
            <Icon name="plus" size={20} /> {t.addTimer}
        </Button>
    {/if}
</div>

<style>
    .timer-container {
        padding: var(--space-4);
        max-width: 600px;
        margin: 0 auto;
    }

    .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 3rem var(--space-4);
        text-align: center;
        color: var(--color-text-secondary);
    }

    .empty-icon {
        font-size: 4rem;
        margin-bottom: var(--space-4);
        opacity: 0.5;
    }

    .empty-state h3 {
        margin: var(--space-2) 0;
        font-size: var(--text-2xl);
        color: var(--color-primary);
    }

    .timers-list {
        display: flex;
        flex-direction: column;
        gap: var(--space-4);
        margin-bottom: var(--space-4);
    }

    .timer-item {
        background: var(--color-bg-secondary);
        border-radius: var(--radius-lg);
        padding: var(--space-6);
        border: 2px solid var(--color-border);
        transition: all var(--duration-normal) var(--ease-default);
    }

    .timer-item.running {
        border-color: var(--color-primary);
        box-shadow: 0 4px 12px rgba(255, 149, 0, 0.3);
    }

    .timer-item.finished {
        border-color: var(--color-primary);
        animation: pulse 2s infinite;
    }

    @keyframes pulse {
        0%, 100% { box-shadow: 0 4px 12px rgba(255, 149, 0, 0.3); }
        50% { box-shadow: 0 4px 20px rgba(255, 149, 0, 0.6); }
    }

    .timer-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: var(--space-4);
    }

    .timer-label {
        font-size: var(--text-lg);
        font-weight: 600;
        color: var(--color-text-primary);
    }

    .btn-delete-timer {
        background: none;
        border: none;
        font-size: 1.2rem;
        cursor: pointer;
        padding: var(--space-1) var(--space-2);
        border-radius: var(--radius-md);
        transition: all var(--duration-normal);
        color: var(--color-error);
        -webkit-tap-highlight-color: transparent;
    }

    .btn-delete-timer:hover {
        background: var(--color-error);
        color: #fff;
    }

    .timer-display {
        display: flex;
        align-items: baseline;
        justify-content: center;
        margin-bottom: var(--space-4);
    }

    .timer-time {
        font-size: 3rem;
        font-weight: bold;
        color: var(--color-text-primary);
        font-family: var(--font-mono);
    }

    .timer-total {
        font-size: var(--text-xl);
        color: var(--color-text-secondary);
        margin-left: var(--space-2);
        font-family: var(--font-mono);
    }

    .timer-progress {
        width: 100%;
        height: 8px;
        background: var(--color-bg-elevated);
        border-radius: 4px;
        overflow: hidden;
        margin-bottom: var(--space-4);
    }

    .timer-progress-bar {
        height: 100%;
        background: var(--color-primary);
        transition: width 0.5s linear;
        border-radius: 4px;
    }

    .timer-controls {
        display: flex;
        gap: var(--space-2);
        justify-content: center;
    }

    .timer-finished {
        margin-top: var(--space-4);
        padding: var(--space-3);
        background: var(--color-primary-light);
        border-radius: var(--radius-md);
        text-align: center;
        font-weight: bold;
        color: var(--color-primary);
        font-size: var(--text-lg);
    }

    .timer-editor {
        background: var(--color-bg-secondary);
        border-radius: var(--radius-lg);
        padding: var(--space-6);
        margin-bottom: var(--space-4);
        border: 2px solid var(--color-border);
    }

    .timer-editor-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: var(--space-4);
    }

    .timer-editor-header h3 {
        margin: 0;
        color: var(--color-primary);
    }

    .btn-close-editor {
        background: none;
        border: none;
        color: var(--color-text-secondary);
        cursor: pointer;
        width: 36px;
        height: 36px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: var(--radius-md);
        transition: all var(--duration-normal);
        -webkit-tap-highlight-color: transparent;
    }

    .btn-close-editor:hover {
        background: var(--color-primary-light);
        color: var(--color-primary);
    }

    .form-group {
        margin-bottom: var(--space-4);
    }

    .form-group > label {
        display: block;
        margin-bottom: var(--space-2);
        color: var(--color-text-primary);
        font-weight: 500;
    }

    .form-input {
        width: 100%;
        padding: var(--space-3);
        background: var(--color-bg-tertiary);
        border: 2px solid var(--color-border);
        border-radius: var(--radius-md);
        color: var(--color-text-primary);
        font-size: var(--text-base);
    }

    .form-input:focus {
        outline: none;
        border-color: var(--color-primary);
    }

    .time-picker-container {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: var(--space-2);
        padding: var(--space-4);
        background: var(--color-bg-secondary);
        border-radius: var(--radius-md);
        border: 1px solid var(--color-border);
    }

    .time-picker-column {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: var(--space-2);
    }

    .picker-label {
        font-size: var(--text-sm);
        color: var(--color-text-secondary);
        text-transform: uppercase;
        letter-spacing: 0.5px;
        font-weight: 500;
    }

    .wheel-picker-wrapper {
        position: relative;
        width: 80px;
        height: 160px;
        overflow: hidden;
        border-radius: var(--radius-md);
        background: var(--color-bg-secondary);
        border: 2px solid var(--color-border);
    }

    .wheel-picker-overlay {
        position: absolute;
        inset: 0;
        background: linear-gradient(to bottom, rgba(26, 26, 26, 0.95) 0%, transparent 30%, transparent 70%, rgba(26, 26, 26, 0.95) 100%);
        pointer-events: none;
        z-index: 2;
    }

    .wheel-picker-selection {
        position: absolute;
        top: 50%;
        left: 0;
        right: 0;
        height: 40px;
        transform: translateY(-50%);
        border-top: 2px solid var(--color-primary);
        border-bottom: 2px solid var(--color-primary);
        background: var(--color-primary-light);
        pointer-events: none;
        z-index: 1;
    }

    .wheel-picker {
        height: 100%;
        overflow-y: scroll;
        scroll-snap-type: y mandatory;
        scrollbar-width: none;
        -ms-overflow-style: none;
        padding: 60px 0;
        -webkit-overflow-scrolling: touch;
    }

    .wheel-picker::-webkit-scrollbar {
        display: none;
    }

    .wheel-item {
        height: 40px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: var(--text-base);
        font-weight: bold;
        font-family: var(--font-mono);
        color: var(--color-text-secondary);
        cursor: pointer;
        scroll-snap-align: center;
        transition: all var(--duration-normal);
        user-select: none;
        -webkit-tap-highlight-color: transparent;
    }

    .wheel-item:hover {
        color: var(--color-text-primary);
    }

    .wheel-item.selected {
        color: var(--color-primary);
        font-size: 1.75rem;
        text-shadow: 0 0 10px rgba(255, 149, 0, 0.3);
    }

    .time-picker-separator {
        font-size: 2rem;
        font-weight: bold;
        color: var(--color-primary);
        margin: 0 var(--space-1);
        padding-top: var(--space-6);
    }

    .sound-select-btn {
        width: 100%;
        padding: var(--space-3);
        background: var(--color-bg-tertiary);
        border: 2px solid var(--color-border);
        border-radius: var(--radius-md);
        color: var(--color-text-primary);
        cursor: pointer;
        font-size: var(--text-base);
        text-align: left;
        transition: all var(--duration-normal);
        -webkit-tap-highlight-color: transparent;
        display: flex;
        align-items: center;
        gap: var(--space-2);
    }

    .sound-select-btn:hover {
        border-color: var(--color-primary);
    }

    .toggle-option-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: var(--space-4);
        width: 100%;
        padding: var(--space-4);
        background: var(--color-bg-tertiary);
        border-radius: var(--radius-md);
        border: 1px solid var(--color-border);
    }

    .toggle-option-text {
        display: flex;
        align-items: center;
        gap: var(--space-3);
        font-size: var(--text-base);
        color: var(--color-text-primary);
        font-weight: 500;
    }

    .timer-editor-actions {
        display: flex;
        gap: var(--space-2);
    }

    @media (max-width: 768px) {
        .timer-time {
            font-size: 2.5rem;
        }

        .timer-total {
            font-size: var(--text-base);
        }
    }
</style>
