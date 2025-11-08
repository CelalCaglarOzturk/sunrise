<script lang="ts">
    import { createEventDispatcher } from "svelte";
    import type { Alarm, Translation, Location } from "../types";
    import { sunCalcService } from "../services/sunCalc";
    import { filePickerService } from "../services/filePicker";

    export let alarm: Alarm | null = null;
    export let t: Translation;
    export let use24Hour: boolean = true;
    export let location: Location | null = null;
    export let isOpen: boolean = false;

    const dispatch = createEventDispatcher<{
        save: Alarm;
        close: void;
    }>();

    // Form fields
    let time = "07:00";
    let label = "";
    let useSunTime = false;
    let sunOffset = 0;
    let repeatDays: number[] = [];
    let deleteAfterUse = false;
    let soundUri = "";
    let soundName = "Default";
    let vibrate = true;
    let gradualVolume = false;
    let gradualDuration = 30;

    // UI state
    let showSoundPicker = false;

    const dayNames = [
        { key: "sunday", value: 0 },
        { key: "monday", value: 1 },
        { key: "tuesday", value: 2 },
        { key: "wednesday", value: 3 },
        { key: "thursday", value: 4 },
        { key: "friday", value: 5 },
        { key: "saturday", value: 6 },
    ];

    // Initialize form when alarm changes
    $: if (alarm) {
        time = alarm.time;
        label = alarm.label;
        useSunTime = alarm.useSunTime;
        sunOffset = alarm.sunOffset;
        repeatDays = [...alarm.repeatDays];
        deleteAfterUse = alarm.deleteAfterUse;
        soundUri = alarm.soundUri;
        soundName = alarm.soundName;
        vibrate = alarm.vibrate;
        gradualVolume = alarm.gradualVolume;
        gradualDuration = alarm.gradualDuration;
    } else {
        resetForm();
    }

    function resetForm() {
        time = "07:00";
        label = "";
        useSunTime = false;
        sunOffset = 0;
        repeatDays = [];
        deleteAfterUse = false;
        soundUri = "";
        soundName = "Default";
        vibrate = true;
        gradualVolume = false;
        gradualDuration = 30;
    }

    function toggleDay(day: number) {
        if (repeatDays.includes(day)) {
            repeatDays = repeatDays.filter((d) => d !== day);
        } else {
            repeatDays = [...repeatDays, day].sort();
        }
    }

    function handleSave() {
        const newAlarm: Alarm = {
            id: alarm?.id || `alarm_${Date.now()}`,
            time,
            enabled: alarm?.enabled ?? true,
            label,
            useSunTime,
            sunOffset,
            repeatDays,
            deleteAfterUse,
            soundUri,
            soundName,
            vibrate,
            gradualVolume,
            gradualDuration,
        };

        dispatch("save", newAlarm);
        handleClose();
    }

    function handleClose() {
        dispatch("close");
        resetForm();
    }

    function handleBackdropClick(e: MouseEvent) {
        if (e.target === e.currentTarget) {
            handleClose();
        }
    }

    function getDayLabel(dayKey: string): string {
        return t[dayKey as keyof Translation] as string;
    }

    function formatSunTime(): string {
        if (!location) return "--:--";

        const now = new Date();
        const sunriseDate = sunCalcService.convertSunTimeToActualTime(
            sunOffset,
            location,
            now,
        );
        return sunCalcService.formatTime(sunriseDate, use24Hour);
    }

    // Quick select for repeat days
    function selectWeekdays() {
        repeatDays = [1, 2, 3, 4, 5];
    }

    function selectWeekends() {
        repeatDays = [0, 6];
    }

    function selectDaily() {
        repeatDays = [0, 1, 2, 3, 4, 5, 6];
    }

    function selectNone() {
        repeatDays = [];
    }

    // Sound selection using file picker
    async function selectSound() {
        showSoundPicker = true;
    }

    async function browseSoundFiles() {
        const file = await filePickerService.pickAudioFile();
        if (file) {
            soundName = file.name;
            soundUri = file.uri;
            showSoundPicker = false;
        }
    }

    function chooseSound(name: string, uri: string) {
        soundName = name;
        soundUri = uri;
        showSoundPicker = false;
    }
</script>

{#if isOpen}
    <div class="modal-backdrop" on:click={handleBackdropClick}>
        <div class="modal-content">
            <div class="modal-header">
                <h2>{alarm ? t.editAlarm : t.addAlarm}</h2>
                <button class="btn-close" on:click={handleClose}>✕</button>
            </div>

            <div class="modal-body">
                <!-- Label -->
                <div class="form-group">
                    <label for="label">{t.label}</label>
                    <input
                        id="label"
                        type="text"
                        bind:value={label}
                        placeholder={t.label}
                        class="form-input"
                    />
                </div>

                <!-- Clock Mode Toggle -->
                <div class="form-group">
                    <div class="clock-mode-toggle">
                        <button
                            class="mode-btn"
                            class:active={!useSunTime}
                            on:click={() => (useSunTime = false)}
                        >
                            🕐 {t.normalClock}
                        </button>
                        <button
                            class="mode-btn"
                            class:active={useSunTime}
                            on:click={() => (useSunTime = true)}
                            disabled={!location}
                        >
                            ☀️ {t.sunBasedClock}
                        </button>
                    </div>
                </div>

                {#if useSunTime}
                    <!-- Sun Offset -->
                    <div class="form-group">
                        <label for="sunOffset">
                            {t.sunOffset}
                            <span class="sun-time-preview"
                                >{formatSunTime()}</span
                            >
                        </label>
                        <div class="offset-control">
                            <button
                                class="offset-btn"
                                on:click={() =>
                                    (sunOffset = Math.max(
                                        sunOffset - 15,
                                        -480,
                                    ))}
                            >
                                −15
                            </button>
                            <div class="offset-display">
                                {#if sunOffset >= 0}
                                    +{Math.floor(sunOffset / 60)}h {sunOffset %
                                        60}m
                                {:else}
                                    {Math.floor(sunOffset / 60)}h {Math.abs(
                                        sunOffset % 60,
                                    )}m
                                {/if}
                            </div>
                            <button
                                class="offset-btn"
                                on:click={() =>
                                    (sunOffset = Math.min(sunOffset + 15, 480))}
                            >
                                +15
                            </button>
                        </div>
                        <input
                            id="sunOffset"
                            type="range"
                            bind:value={sunOffset}
                            min="-480"
                            max="480"
                            step="15"
                            class="offset-slider"
                        />
                        <div class="offset-hint">
                            {#if sunOffset >= 0}
                                {sunOffset} {t.minutesAfterSunrise}
                            {:else}
                                {Math.abs(sunOffset)} {t.minutesBeforeSunrise}
                            {/if}
                        </div>
                    </div>
                {:else}
                    <!-- Normal Time -->
                    <div class="form-group">
                        <label for="time">{t.time}</label>
                        <input
                            id="time"
                            type="time"
                            bind:value={time}
                            class="form-input time-input"
                        />
                    </div>
                {/if}

                <!-- Repeat Days -->
                <div class="form-group">
                    <label>{t.repeat}</label>

                    <div class="quick-select">
                        <button class="quick-btn" on:click={selectWeekdays}>
                            {t.weekdays}
                        </button>
                        <button class="quick-btn" on:click={selectWeekends}>
                            {t.weekends}
                        </button>
                        <button class="quick-btn" on:click={selectDaily}>
                            {t.daily}
                        </button>
                        <button class="quick-btn" on:click={selectNone}>
                            {t.once}
                        </button>
                    </div>

                    <div class="days-selector">
                        {#each dayNames as { key, value }}
                            <button
                                class="day-btn"
                                class:selected={repeatDays.includes(value)}
                                on:click={() => toggleDay(value)}
                            >
                                {getDayLabel(key)}
                            </button>
                        {/each}
                    </div>
                </div>

                <!-- Alarm Sound -->
                <div class="form-group">
                    <label>{t.alarmSound}</label>
                    <button class="sound-select-btn" on:click={selectSound}>
                        🔔 {soundName}
                    </button>

                    {#if showSoundPicker}
                        <div class="sound-picker">
                            <button on:click={() => chooseSound("Default", "")}>
                                🔔 Default
                            </button>
                            <button
                                on:click={() =>
                                    chooseSound("Gentle", "gentle.mp3")}
                            >
                                🔔 Gentle
                            </button>
                            <button
                                on:click={() => chooseSound("Loud", "loud.mp3")}
                            >
                                🔔 Loud
                            </button>
                            <button
                                on:click={() =>
                                    chooseSound("Nature", "nature.mp3")}
                            >
                                🔔 Nature
                            </button>
                            <button
                                on:click={() =>
                                    chooseSound("Digital", "digital.mp3")}
                            >
                                🔔 Digital
                            </button>
                            <button
                                class="btn-browse-files"
                                on:click={browseSoundFiles}
                            >
                                📁 {t.browseFiles}
                            </button>
                        </div>
                    {/if}
                </div>

                <!-- Vibrate -->
                <div class="form-group checkbox-group">
                    <label class="checkbox-label">
                        <input type="checkbox" bind:checked={vibrate} />
                        <span>📳 {t.vibrate}</span>
                    </label>
                </div>

                <!-- Gradual Volume -->
                <div class="form-group checkbox-group">
                    <label class="checkbox-label">
                        <input type="checkbox" bind:checked={gradualVolume} />
                        <span>🔊↗️ {t.gradualVolume}</span>
                    </label>
                </div>

                {#if gradualVolume}
                    <div class="form-group">
                        <label for="gradualDuration">
                            {t.gradualDuration}: {gradualDuration}
                            {t.seconds}
                        </label>
                        <input
                            id="gradualDuration"
                            type="range"
                            bind:value={gradualDuration}
                            min="10"
                            max="120"
                            step="5"
                            class="duration-slider"
                        />
                    </div>
                {/if}

                <!-- Delete After Use -->
                <div class="form-group checkbox-group">
                    <label class="checkbox-label">
                        <input type="checkbox" bind:checked={deleteAfterUse} />
                        <span>🗑️ {t.deleteAfterUse}</span>
                    </label>
                </div>
            </div>

            <div class="modal-footer">
                <button class="btn btn-cancel" on:click={handleClose}>
                    {t.cancel}
                </button>
                <button class="btn btn-save" on:click={handleSave}>
                    {t.save}
                </button>
            </div>
        </div>
    </div>
{/if}

<style>
    .modal-backdrop {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.8);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 1000;
        padding: 1rem;
        overflow-y: auto;
    }

    .modal-content {
        background: var(--modal-bg, #16213e);
        border-radius: 20px;
        width: 100%;
        max-width: 500px;
        max-height: 90vh;
        display: flex;
        flex-direction: column;
        box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
    }

    .modal-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1.5rem;
        border-bottom: 2px solid var(--border-color, #0f3460);
    }

    .modal-header h2 {
        margin: 0;
        font-size: 1.5rem;
        color: var(--text-primary, #e94560);
    }

    .btn-close {
        background: none;
        border: none;
        font-size: 1.5rem;
        cursor: pointer;
        color: var(--text-secondary, #888);
        width: 36px;
        height: 36px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 8px;
        transition: all 0.2s;
    }

    .btn-close:hover {
        background: var(--hover-bg, rgba(233, 69, 96, 0.1));
        color: var(--text-primary, #e94560);
    }

    .modal-body {
        padding: 1.5rem;
        overflow-y: auto;
        flex: 1;
    }

    .form-group {
        margin-bottom: 1.5rem;
    }

    .form-group label {
        display: block;
        margin-bottom: 0.5rem;
        color: var(--text-primary, #fff);
        font-weight: 500;
        font-size: 0.95rem;
    }

    .form-input {
        width: 100%;
        padding: 0.75rem;
        background: var(--input-bg, #0f3460);
        border: 2px solid var(--input-border, #1a5490);
        border-radius: 12px;
        color: var(--text-primary, #fff);
        font-size: 1rem;
        transition: all 0.2s;
    }

    .form-input:focus {
        outline: none;
        border-color: var(--input-focus, #e94560);
    }

    .time-input {
        font-size: 1.5rem;
        font-family: "SF Mono", monospace;
        text-align: center;
    }

    .clock-mode-toggle {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 0.5rem;
    }

    .mode-btn {
        padding: 0.75rem;
        background: var(--btn-bg, #0f3460);
        border: 2px solid var(--btn-border, #1a5490);
        border-radius: 12px;
        color: var(--text-primary, #fff);
        cursor: pointer;
        font-size: 0.95rem;
        transition: all 0.2s;
    }

    .mode-btn:hover:not(:disabled) {
        border-color: var(--btn-hover, #e94560);
    }

    .mode-btn.active {
        background: var(--btn-active-bg, #e94560);
        border-color: var(--btn-active-border, #e94560);
        font-weight: bold;
    }

    .mode-btn:disabled {
        opacity: 0.3;
        cursor: not-allowed;
    }

    .sun-time-preview {
        color: var(--time-color, #ffd700);
        font-family: "SF Mono", monospace;
        font-size: 1.1rem;
        margin-left: 0.5rem;
    }

    .offset-control {
        display: flex;
        align-items: center;
        gap: 1rem;
        margin-bottom: 0.75rem;
    }

    .offset-btn {
        padding: 0.5rem 1rem;
        background: var(--btn-bg, #0f3460);
        border: 2px solid var(--btn-border, #1a5490);
        border-radius: 10px;
        color: var(--text-primary, #fff);
        cursor: pointer;
        font-size: 1rem;
        font-weight: bold;
        transition: all 0.2s;
    }

    .offset-btn:hover {
        background: var(--btn-hover-bg, #1a5490);
    }

    .offset-display {
        flex: 1;
        text-align: center;
        font-size: 1.5rem;
        font-weight: bold;
        color: var(--time-color, #ffd700);
        font-family: "SF Mono", monospace;
    }

    .offset-slider {
        width: 100%;
        height: 6px;
        background: var(--slider-bg, #0f3460);
        border-radius: 3px;
        outline: none;
        -webkit-appearance: none;
    }

    .offset-slider::-webkit-slider-thumb {
        -webkit-appearance: none;
        appearance: none;
        width: 20px;
        height: 20px;
        background: var(--slider-thumb, #e94560);
        cursor: pointer;
        border-radius: 50%;
    }

    .offset-slider::-moz-range-thumb {
        width: 20px;
        height: 20px;
        background: var(--slider-thumb, #e94560);
        cursor: pointer;
        border-radius: 50%;
        border: none;
    }

    .offset-hint {
        text-align: center;
        font-size: 0.85rem;
        color: var(--text-secondary, #888);
        margin-top: 0.5rem;
    }

    .quick-select {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 0.5rem;
        margin-bottom: 0.75rem;
    }

    .quick-btn {
        padding: 0.5rem;
        background: var(--btn-bg, #0f3460);
        border: 1px solid var(--btn-border, #1a5490);
        border-radius: 8px;
        color: var(--text-primary, #fff);
        cursor: pointer;
        font-size: 0.8rem;
        transition: all 0.2s;
    }

    .quick-btn:hover {
        background: var(--btn-hover-bg, #1a5490);
    }

    .days-selector {
        display: grid;
        grid-template-columns: repeat(7, 1fr);
        gap: 0.5rem;
    }

    .day-btn {
        aspect-ratio: 1;
        background: var(--day-bg, #0f3460);
        border: 2px solid var(--day-border, #1a5490);
        border-radius: 50%;
        color: var(--text-primary, #fff);
        cursor: pointer;
        font-size: 0.85rem;
        font-weight: 500;
        transition: all 0.2s;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .day-btn:hover {
        border-color: var(--day-hover, #e94560);
    }

    .day-btn.selected {
        background: var(--day-selected-bg, #e94560);
        border-color: var(--day-selected-border, #e94560);
        font-weight: bold;
    }

    .sound-select-btn {
        width: 100%;
        padding: 0.75rem;
        background: var(--btn-bg, #0f3460);
        border: 2px solid var(--btn-border, #1a5490);
        border-radius: 12px;
        color: var(--text-primary, #fff);
        cursor: pointer;
        font-size: 1rem;
        text-align: left;
        transition: all 0.2s;
    }

    .sound-select-btn:hover {
        border-color: var(--btn-hover, #e94560);
    }

    .sound-picker {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
        margin-top: 0.75rem;
        padding: 0.75rem;
        background: var(--picker-bg, #0a2540);
        border-radius: 12px;
    }

    .sound-picker button {
        padding: 0.5rem;
        background: var(--picker-btn-bg, #0f3460);
        border: 1px solid var(--picker-btn-border, #1a5490);
        border-radius: 8px;
        color: var(--text-primary, #fff);
        cursor: pointer;
        transition: all 0.2s;
    }

    .sound-picker button:hover {
        background: var(--picker-btn-hover, #1a5490);
    }

    .btn-browse-files {
        background: var(--primary, #e94560) !important;
        font-weight: bold;
    }

    .btn-browse-files:hover {
        background: var(--primary-dark, #d32f2f) !important;
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
        background: var(--checkbox-bg, #0f3460);
        border-radius: 12px;
        transition: all 0.2s;
    }

    .checkbox-label:hover {
        background: var(--checkbox-hover, #1a5490);
    }

    .checkbox-label input[type="checkbox"] {
        width: 20px;
        height: 20px;
        cursor: pointer;
    }

    .checkbox-label span {
        font-size: 1rem;
        color: var(--text-primary, #fff);
    }

    .duration-slider {
        width: 100%;
        height: 6px;
        background: var(--slider-bg, #0f3460);
        border-radius: 3px;
        outline: none;
        -webkit-appearance: none;
    }

    .duration-slider::-webkit-slider-thumb {
        -webkit-appearance: none;
        appearance: none;
        width: 20px;
        height: 20px;
        background: var(--slider-thumb, #e94560);
        cursor: pointer;
        border-radius: 50%;
    }

    .duration-slider::-moz-range-thumb {
        width: 20px;
        height: 20px;
        background: var(--slider-thumb, #e94560);
        cursor: pointer;
        border-radius: 50%;
        border: none;
    }

    .modal-footer {
        display: flex;
        gap: 1rem;
        padding: 1.5rem;
        border-top: 2px solid var(--border-color, #0f3460);
    }

    .btn {
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
        background: var(--btn-cancel-bg, #2a2a3e);
        color: var(--text-primary, #fff);
    }

    .btn-cancel:hover {
        background: var(--btn-cancel-hover, #3a3a4e);
    }

    .btn-save {
        background: var(--btn-save-bg, #e94560);
        color: white;
    }

    .btn-save:hover {
        background: var(--btn-save-hover, #d63850);
    }

    @media (max-width: 768px) {
        .modal-content {
            max-height: 95vh;
        }

        .modal-header h2 {
            font-size: 1.25rem;
        }

        .modal-body,
        .modal-header,
        .modal-footer {
            padding: 1rem;
        }

        .days-selector {
            gap: 0.4rem;
        }

        .day-btn {
            font-size: 0.75rem;
        }
    }
</style>
