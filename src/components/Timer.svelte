<script lang="ts">
    import { onDestroy } from "svelte";
    import type { Timer, Translation } from "../types";
    import { filePickerService } from "../services/filePicker";
    import { timers as timersStore, settings } from "../stores";
    import { getTranslation } from "../utils/translations";
    import type { Language } from "../types";
    import Icon from "./Icon.svelte";

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
            <h3>{t.addTimer}</h3>

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
                <div class="duration-inputs">
                    <div class="duration-input">
                        <input
                            type="number"
                            bind:value={newTimerHours}
                            min="0"
                            max="23"
                            class="duration-number"
                        />
                        <span class="duration-label">h</span>
                    </div>
                    <span class="duration-separator">:</span>
                    <div class="duration-input">
                        <input
                            type="number"
                            bind:value={newTimerMinutes}
                            min="0"
                            max="59"
                            class="duration-number"
                        />
                        <span class="duration-label">m</span>
                    </div>
                    <span class="duration-separator">:</span>
                    <div class="duration-input">
                        <input
                            type="number"
                            bind:value={newTimerSeconds}
                            min="0"
                            max="59"
                            class="duration-number"
                        />
                        <span class="duration-label">s</span>
                    </div>
                </div>
            </div>

            <div class="form-group">
                <label>{t.alarmSound}</label>
                <button class="sound-select-btn" on:click={selectSound}>
                    🔔 {newTimerSoundName}
                </button>
            </div>

            <div class="form-group checkbox-group">
                <label class="checkbox-label">
                    <input type="checkbox" bind:checked={newTimerVibrate} />
                    <span>📳 {t.vibrate}</span>
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

    <button
        class="btn-add-timer"
        on:click={() => (showAddTimer = !showAddTimer)}
    >
        {#if showAddTimer}
            <Icon name="x" size={20} />
        {:else}
            <Icon name="plus" size={20} />
        {/if}
        {t.addTimer}
    </button>
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

    .duration-inputs {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        justify-content: center;
    }

    .duration-input {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.25rem;
    }

    .duration-number {
        width: 70px;
        padding: 0.75rem;
        background: #2a2a2a;
        border: 2px solid #444;
        border-radius: 12px;
        color: #fff;
        font-size: 1.5rem;
        text-align: center;
        font-family: "SF Mono", "Monaco", monospace;
    }

    .duration-number:focus {
        outline: none;
        border-color: #ff9500;
    }

    .duration-label {
        font-size: 0.9rem;
        color: #888;
    }

    .duration-separator {
        font-size: 2rem;
        color: #fff;
        font-weight: bold;
        margin: 0 0.25rem;
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
    }

    .sound-select-btn:hover {
        border-color: #ff9500;
    }

    .checkbox-group {
        margin-bottom: 1rem;
    }

    .checkbox-label {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        cursor: pointer;
        padding: 0.75rem;
        background: #2a2a2a;
        border-radius: 12px;
        transition: all 0.2s;
    }

    .checkbox-label:hover {
        background: #333;
    }

    .checkbox-label input[type="checkbox"] {
        width: 20px;
        height: 20px;
        cursor: pointer;
    }

    .checkbox-label span {
        font-size: 1rem;
        color: #fff;
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

        .duration-number {
            width: 60px;
            font-size: 1.25rem;
        }
    }
</style>
