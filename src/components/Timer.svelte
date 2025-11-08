<script lang="ts">
    import { onDestroy } from "svelte";
    import type { Timer, Translation } from "../types";
    import { filePickerService } from "../services/filePicker";
    import { timers as timersStore, settings } from "../stores";
    import { getTranslation } from "../utils/translations";
    import type { Language } from "../types";
    import Icon from "./Icon.svelte";
    import { Bell, Vibrate } from "@lucide/svelte";

    export let t: Translation;

    let timersData: Timer[] = [];
    let settingsData: any;

    const unsubTimers = timersStore.subscribe((value) => (timersData = value));
    const unsubSettings = settings.subscribe((value) => (settingsData = value));

    onDestroy(() => {
        unsubTimers();
        unsubSettings();
    });

    $: timers = timersData;

    let showAddTimer = false;
    let newTimerLabel = "";
    let newTimerHours = 0;
    let newTimerMinutes = 1;
    let newTimerSeconds = 0;
    let newTimerSound = "";
    let newTimerSoundName = "Default";
    let newTimerVibrate = true;

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

    // Generate options for wheel pickers
    function getHourOptions(): number[] {
        const options = [];
        for (let i = 0; i <= 23; i++) {
            options.push(i);
        }
        return options;
    }

    function getMinuteOptions(): number[] {
        const options = [];
        for (let i = 0; i <= 59; i++) {
            options.push(i);
        }
        return options;
    }

    function getSecondOptions(): number[] {
        const options = [];
        for (let i = 0; i <= 59; i++) {
            options.push(i);
        }
        return options;
    }

    // Scroll wheel picker to center item
    function scrollToValue(
        container: HTMLDivElement,
        value: number,
        options: number[],
    ) {
        if (!container) return;
        const index = options.indexOf(value);
        if (index === -1) return;
        const itemHeight = 40;
        const scrollPosition = index * itemHeight;
        container.scrollTo({
            top: scrollPosition,
            behavior: "smooth",
        });
    }

    // Handle wheel scroll snap
    let scrollTimeout: number;
    function handleWheelScroll(
        container: HTMLDivElement,
        options: number[],
        setter: (value: number) => void,
    ) {
        if (!container) return;

        clearTimeout(scrollTimeout);

        scrollTimeout = window.setTimeout(() => {
            const itemHeight = 40;
            const scrollTop = container.scrollTop;
            const index = Math.round(scrollTop / itemHeight);
            const clampedIndex = Math.max(
                0,
                Math.min(index, options.length - 1),
            );

            container.scrollTo({
                top: clampedIndex * itemHeight,
                behavior: "smooth",
            });
            setter(options[clampedIndex]);
        }, 100);
    }

    // Initialize wheel positions when opening timer editor
    $: if (
        showAddTimer &&
        hoursWheelRef &&
        minutesWheelRef &&
        secondsWheelRef
    ) {
        setTimeout(() => {
            scrollToValue(hoursWheelRef, newTimerHours, getHourOptions());
            scrollToValue(minutesWheelRef, newTimerMinutes, getMinuteOptions());
            scrollToValue(secondsWheelRef, newTimerSeconds, getSecondOptions());
        }, 50);
    }

    function createTimer() {
        const duration =
            newTimerHours * 3600 + newTimerMinutes * 60 + newTimerSeconds;

        if (duration <= 0) {
            alert("Please set a valid duration");
            return;
        }

        const timer: Timer = {
            id: `timer_${Date.now()}`,
            label: newTimerLabel || `Timer ${timers.length + 1}`,
            duration: duration,
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
        const timer = timers.find((t) => t.id === id);
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
                // Timer finished - could trigger notification here
                timersStore.update(id, { isRunning: false, remainingTime: 0 });
                return;
            }

            timersStore.update(id, {
                remainingTime: currentTimer.remainingTime - 1,
            });
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

    function resumeTimer(id: string) {
        startTimer(id);
    }

    function resetTimer(id: string) {
        const interval = timerIntervals.get(id);
        if (interval) {
            clearInterval(interval);
            timerIntervals.delete(id);
        }

        const timer = timers.find((t) => t.id === id);
        if (!timer) return;

        timersStore.update(id, {
            isRunning: false,
            isPaused: false,
            remainingTime: timer.duration,
        });
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
    {#if timers.length === 0 && !showAddTimer}
        <div class="empty-state">
            <div class="empty-icon">
                <Icon name="timer" size={64} color="#888" />
            </div>
            <h3>{t.noTimersYet}</h3>
            <p>{t.createFirstTimer}</p>
        </div>
    {/if}

    <div class="timers-list">
        {#each timers as timer (timer.id)}
            <div
                class="timer-item"
                class:running={timer.isRunning}
                class:finished={timer.remainingTime === 0}
            >
                <div class="timer-header">
                    <div class="timer-label">{timer.label}</div>
                    <button
                        class="btn-delete-timer"
                        on:click={() => deleteTimer(timer.id)}
                    >
                        <Icon name="trash" size={18} />
                    </button>
                </div>

                <div class="timer-display">
                    <div class="timer-time">
                        {formatTime(timer.remainingTime)}
                    </div>
                    <div class="timer-total">
                        / {formatTime(timer.duration)}
                    </div>
                </div>

                <div class="timer-progress">
                    <div
                        class="timer-progress-bar"
                        style="width: {getProgressPercentage(timer)}%"
                    ></div>
                </div>

                <div class="timer-controls">
                    {#if !timer.isRunning && !timer.isPaused}
                        <button
                            class="btn-timer btn-start"
                            on:click={() => startTimer(timer.id)}
                        >
                            <Icon name="play" size={16} />
                            {t.start}
                        </button>
                    {:else if timer.isRunning}
                        <button
                            class="btn-timer btn-pause"
                            on:click={() => pauseTimer(timer.id)}
                        >
                            <Icon name="pause" size={16} />
                            {t.pause}
                        </button>
                    {:else if timer.isPaused}
                        <button
                            class="btn-timer btn-resume"
                            on:click={() => resumeTimer(timer.id)}
                        >
                            <Icon name="play" size={16} />
                            {t.resume}
                        </button>
                    {/if}

                    <button
                        class="btn-timer btn-reset"
                        on:click={() => resetTimer(timer.id)}
                    >
                        <Icon name="rotate-ccw" size={16} />
                        {t.reset}
                    </button>
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
                <button
                    class="btn-close-editor"
                    on:click={() => (showAddTimer = false)}
                >
                    <Icon name="x" size={20} />
                </button>
            </div>

            <div class="form-group">
                <label for="timer-label">{t.label}</label>
                <input
                    id="timer-label"
                    type="text"
                    bind:value={newTimerLabel}
                    placeholder={t.label}
                    class="form-input"
                />
            </div>

            <div class="form-group">
                <label>{t.duration}</label>
                <div class="time-picker-container">
                    <div class="time-picker-column">
                        <div class="picker-label">Hours</div>
                        <div class="wheel-picker-wrapper">
                            <div class="wheel-picker-overlay"></div>
                            <div class="wheel-picker-selection"></div>
                            <div
                                class="wheel-picker"
                                bind:this={hoursWheelRef}
                                on:scroll={() =>
                                    handleWheelScroll(
                                        hoursWheelRef,
                                        getHourOptions(),
                                        (v) => (newTimerHours = v),
                                    )}
                            >
                                {#each getHourOptions() as hour}
                                    <div
                                        class="wheel-item"
                                        class:selected={newTimerHours === hour}
                                        on:click={() => {
                                            newTimerHours = hour;
                                            scrollToValue(
                                                hoursWheelRef,
                                                hour,
                                                getHourOptions(),
                                            );
                                        }}
                                    >
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
                            <div
                                class="wheel-picker"
                                bind:this={minutesWheelRef}
                                on:scroll={() =>
                                    handleWheelScroll(
                                        minutesWheelRef,
                                        getMinuteOptions(),
                                        (v) => (newTimerMinutes = v),
                                    )}
                            >
                                {#each getMinuteOptions() as minute}
                                    <div
                                        class="wheel-item"
                                        class:selected={newTimerMinutes ===
                                            minute}
                                        on:click={() => {
                                            newTimerMinutes = minute;
                                            scrollToValue(
                                                minutesWheelRef,
                                                minute,
                                                getMinuteOptions(),
                                            );
                                        }}
                                    >
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
                            <div
                                class="wheel-picker"
                                bind:this={secondsWheelRef}
                                on:scroll={() =>
                                    handleWheelScroll(
                                        secondsWheelRef,
                                        getSecondOptions(),
                                        (v) => (newTimerSeconds = v),
                                    )}
                            >
                                {#each getSecondOptions() as second}
                                    <div
                                        class="wheel-item"
                                        class:selected={newTimerSeconds ===
                                            second}
                                        on:click={() => {
                                            newTimerSeconds = second;
                                            scrollToValue(
                                                secondsWheelRef,
                                                second,
                                                getSecondOptions(),
                                            );
                                        }}
                                    >
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
                <button class="sound-select-btn" on:click={selectSound}>
                    <Bell size={16} />
                    {newTimerSoundName}
                </button>
            </div>

            <div class="form-group">
                <label class="toggle-option-label">
                    <span class="toggle-option-text">
                        <Vibrate size={16} />
                        {t.vibrate}
                    </span>
                    <label class="toggle-switch">
                        <input type="checkbox" bind:checked={newTimerVibrate} />
                        <span class="toggle-slider"></span>
                    </label>
                </label>
            </div>

            <div class="timer-editor-actions">
                <button
                    class="btn-timer-action btn-cancel"
                    on:click={() => {
                        showAddTimer = false;
                        resetForm();
                    }}
                >
                    {t.cancel}
                </button>
                <button
                    class="btn-timer-action btn-save"
                    on:click={createTimer}
                >
                    {t.save}
                </button>
            </div>
        </div>
    {/if}

    {#if !showAddTimer}
        <button
            class="btn-add-timer"
            on:click={() => (showAddTimer = true)}
        >
            <Icon name="plus" size={20} />
            {t.addTimer}
        </button>
    {/if}
</div>

<style>
    .timer-container {
        padding: 1rem;
        max-width: 600px;
        margin: 0 auto;
    }

    .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 3rem 1rem;
        text-align: center;
        color: var(--text-secondary, #888);
    }

    .empty-icon {
        font-size: 4rem;
        margin-bottom: 1rem;
        opacity: 0.5;
    }

    .empty-state h3 {
        margin: 0.5rem 0;
        font-size: 1.5rem;
        color: #ff9500;
    }

    .timers-list {
        display: flex;
        flex-direction: column;
        gap: 1rem;
        margin-bottom: 1rem;
    }

    .timer-item {
        background: #1a1a1a;
        border-radius: 16px;
        padding: 1.5rem;
        border: 2px solid #333;
        transition: all 0.3s ease;
    }

    .timer-item.running {
        border-color: #ff9500;
        box-shadow: 0 4px 12px rgba(255, 149, 0, 0.3);
    }

    .timer-item.finished {
        border-color: #ff9500;
        animation: pulse 2s infinite;
    }

    @keyframes pulse {
        0%,
        100% {
            box-shadow: 0 4px 12px rgba(255, 149, 0, 0.3);
        }
        50% {
            box-shadow: 0 4px 20px rgba(255, 149, 0, 0.6);
        }
    }

    .timer-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1rem;
    }

    .timer-label {
        font-size: 1.1rem;
        font-weight: 600;
        color: #fff;
    }

    .btn-delete-timer {
        background: none;
        border: none;
        font-size: 1.2rem;
        cursor: pointer;
        padding: 0.25rem 0.5rem;
        border-radius: 8px;
        transition: all 0.2s;
        color: #d97706;
        -webkit-tap-highlight-color: transparent;
    }

    .btn-delete-timer:hover {
        background: #d97706;
        color: #fff;
    }

    .timer-display {
        display: flex;
        align-items: baseline;
        justify-content: center;
        margin-bottom: 1rem;
    }

    .timer-time {
        font-size: 3rem;
        font-weight: bold;
        color: #fff;
        font-family: "SF Mono", "Monaco", monospace;
    }

    .timer-total {
        font-size: 1.2rem;
        color: #888;
        margin-left: 0.5rem;
        font-family: "SF Mono", "Monaco", monospace;
    }

    .timer-progress {
        width: 100%;
        height: 8px;
        background: #333;
        border-radius: 4px;
        overflow: hidden;
        margin-bottom: 1rem;
    }

    .timer-progress-bar {
        height: 100%;
        background: #ff9500;
        transition: width 0.5s linear;
        border-radius: 4px;
    }

    .timer-controls {
        display: flex;
        gap: 0.5rem;
        justify-content: center;
    }

    .btn-timer {
        flex: 1;
        padding: 0.75rem;
        border: none;
        border-radius: 12px;
        font-size: 1rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 0.5rem;
        -webkit-tap-highlight-color: transparent;
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
    }

    .btn-pause {
        background: #555;
        color: white;
    }

    .btn-pause:hover {
        background: #666;
        transform: translateY(-2px);
    }

    .btn-reset {
        background: #555;
        color: white;
    }

    .btn-reset:hover {
        background: #666;
    }

    .timer-finished {
        margin-top: 1rem;
        padding: 0.75rem;
        background: rgba(255, 149, 0, 0.1);
        border-radius: 12px;
        text-align: center;
        font-weight: bold;
        color: #ff9500;
        font-size: 1.1rem;
    }

    .timer-editor {
        background: #1a1a1a;
        border-radius: 16px;
        padding: 1.5rem;
        margin-bottom: 1rem;
        border: 2px solid #333;
    }

    .timer-editor-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1rem;
    }

    .timer-editor-header h3 {
        margin: 0;
        color: #ff9500;
    }

    .btn-close-editor {
        background: none;
        border: none;
        color: #888;
        cursor: pointer;
        width: 36px;
        height: 36px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 8px;
        transition: all 0.2s;
        -webkit-tap-highlight-color: transparent;
    }

    .btn-close-editor:hover {
        background: rgba(255, 149, 0, 0.1);
        color: #ff9500;
    }

    .timer-editor h3 {
        margin: 0 0 1rem 0;
        color: #ff9500;
    }

    .form-group {
        margin-bottom: 1rem;
    }

    .form-group label {
        display: block;
        margin-bottom: 0.5rem;
        color: #fff;
        font-weight: 500;
    }

    .form-input {
        width: 100%;
        padding: 0.75rem;
        background: #2a2a2a;
        border: 2px solid #444;
        border-radius: 12px;
        color: #fff;
        font-size: 1rem;
    }

    .form-input:focus {
        outline: none;
        border-color: #ff9500;
    }

    .time-picker-container {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 0.5rem;
        padding: 1rem;
        background: #1a1a1a;
        border-radius: 12px;
        border: 1px solid #333;
        margin-bottom: 0.5rem;
    }

    .time-picker-column {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.5rem;
    }

    .picker-label {
        font-size: 0.85rem;
        color: #888;
        text-transform: uppercase;
        letter-spacing: 0.5px;
        font-weight: 500;
    }

    .wheel-picker-wrapper {
        position: relative;
        width: 80px;
        height: 160px;
        overflow: hidden;
        border-radius: 12px;
        background: #1a1a1a;
        border: 2px solid #333;
    }

    .wheel-picker-overlay {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: linear-gradient(
            to bottom,
            rgba(26, 26, 26, 0.95) 0%,
            transparent 30%,
            transparent 70%,
            rgba(26, 26, 26, 0.95) 100%
        );
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
        border-top: 2px solid #ff9500;
        border-bottom: 2px solid #ff9500;
        background: rgba(255, 149, 0, 0.05);
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
        font-size: 1rem;
        font-weight: bold;
        font-family: "SF Mono", "Monaco", "Inconsolata", monospace;
        color: #555;
        cursor: pointer;
        scroll-snap-align: center;
        transition: all 0.3s ease;
        user-select: none;
        -webkit-tap-highlight-color: transparent;
    }

    .wheel-item:hover {
        color: #777;
    }

    .wheel-item.selected {
        color: #ff9500;
        font-size: 1.75rem;
        text-shadow: 0 0 10px rgba(255, 149, 0, 0.3);
    }

    .time-picker-separator {
        font-size: 2rem;
        font-weight: bold;
        color: #ff9500;
        margin: 0 0.25rem;
        padding-top: 1.5rem;
    }

    .sound-select-btn {
        width: 100%;
        padding: 0.75rem;
        background: #2a2a2a;
        border: 2px solid #444;
        border-radius: 12px;
        color: #fff;
        cursor: pointer;
        font-size: 1rem;
        text-align: left;
        transition: all 0.2s;
        -webkit-tap-highlight-color: transparent;
    }

    .sound-select-btn:hover {
        border-color: #ff9500;
    }

    .toggle-option-label {
        display: flex !important;
        align-items: center;
        justify-content: space-between;
        gap: 1rem;
        width: 100%;
        padding: 1rem;
        margin-bottom: 0 !important;
        background: #2a2a2a;
        border-radius: 12px;
        border: 2px solid #333;
        cursor: pointer;
        transition: all 0.2s;
        -webkit-tap-highlight-color: transparent;
    }

    .toggle-option-label:hover {
        border-color: #444;
        background: #333;
    }

    .toggle-option-text {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        font-size: 1rem;
        color: #fff;
        font-weight: 500;
        flex: 1;
    }

    .toggle-switch {
        position: relative;
        display: inline-block;
        width: 50px;
        height: 26px;
        flex-shrink: 0;
    }

    .toggle-switch input {
        opacity: 0;
        width: 0;
        height: 0;
    }

    .toggle-slider {
        position: absolute;
        cursor: pointer;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background-color: #444;
        transition: 0.3s;
        border-radius: 26px;
        -webkit-tap-highlight-color: transparent;
    }

    .toggle-slider:before {
        position: absolute;
        content: "";
        height: 20px;
        width: 20px;
        left: 3px;
        bottom: 3px;
        background-color: white;
        transition: 0.3s;
        border-radius: 50%;
    }

    input:checked + .toggle-slider {
        background-color: #ff9500;
    }

    input:checked + .toggle-slider:before {
        transform: translateX(24px);
    }

    .timer-editor-actions {
        display: flex;
        gap: 0.5rem;
    }

    .btn-timer-action {
        flex: 1;
        padding: 0.875rem;
        border: none;
        border-radius: 12px;
        font-size: 1rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;
        -webkit-tap-highlight-color: transparent;
    }

    .btn-cancel {
        background: #555;
        color: #fff;
    }

    .btn-cancel:hover {
        background: #666;
    }

    .btn-save {
        background: #ff9500;
        color: white;
    }

    .btn-save:hover {
        background: #e68600;
    }

    .btn-add-timer {
        width: 100%;
        padding: 1rem;
        background: #ff9500;
        border: none;
        border-radius: 16px;
        color: white;
        font-size: 1.1rem;
        font-weight: bold;
        cursor: pointer;
        transition: all 0.3s ease;
        -webkit-tap-highlight-color: transparent;
        box-shadow: 0 4px 15px rgba(255, 149, 0, 0.3);
    }

    .btn-add-timer:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(255, 149, 0, 0.4);
    }

    @media (max-width: 768px) {
        .timer-time {
            font-size: 2.5rem;
        }

        .timer-total {
            font-size: 1rem;
        }
    }
</style>
