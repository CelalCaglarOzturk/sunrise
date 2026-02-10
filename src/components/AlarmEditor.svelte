<script lang="ts">
    import type { Alarm, Location, Translation } from "../types";
    import { filePickerService } from "../services/filePicker";
    import { sunCalcService } from "../services/sunCalc";
    import { Toggle, Button, Modal } from "./ui";
    import { Volume2, VolumeX, Bell, Vibrate, TrendingUp, Trash2, Folder, X, Sun, Clock } from "@lucide/svelte";

    interface Props {
        alarm: Alarm | null;
        t: Translation;
        use24Hour?: boolean;
        location: Location | null;
        isOpen: boolean;
        onsave: (alarm: Alarm) => void;
        onclose: () => void;
    }

    let { 
        alarm = null, 
        t, 
        use24Hour = true, 
        location = null, 
        isOpen = false, 
        onsave, 
        onclose 
    }: Props = $props();

    // Form fields
    let time = $state("07:00");
    let label = $state("");
    let useSunTime = $state(false);
    let sunOffset = $state(0);
    let sunOffsetHours = $state(0);
    let sunOffsetMinutes = $state(0);
    let repeatDays = $state<number[]>([]);
    let deleteAfterUse = $state(false);
    let soundUri = $state("");
    let soundName = $state("Default");
    let vibrate = $state(true);
    let volume = $state(50);
    let gradualVolume = $state(false);
    let gradualDuration = $state(30);

    // UI state
    let showSoundPicker = $state(false);

    // Wheel picker refs
    let hoursWheelRef: HTMLDivElement;
    let minutesWheelRef: HTMLDivElement;

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
    $effect(() => {
        if (alarm) {
            time = alarm.time;
            label = alarm.label;
            useSunTime = alarm.useSunTime;
            sunOffset = alarm.sunOffset;
            sunOffsetHours = Math.floor(Math.abs(alarm.sunOffset) / 60) * (alarm.sunOffset < 0 ? -1 : 1);
            sunOffsetMinutes = Math.abs(alarm.sunOffset % 60);
            repeatDays = [...alarm.repeatDays];
            deleteAfterUse = alarm.deleteAfterUse;
            soundUri = alarm.soundUri;
            soundName = alarm.soundName;
            vibrate = alarm.vibrate;
            volume = alarm.volume;
            gradualVolume = alarm.gradualVolume;
            gradualDuration = alarm.gradualDuration;
        } else {
            resetForm();
        }
    });

    function resetForm() {
        time = "07:00";
        label = "";
        useSunTime = false;
        sunOffset = 0;
        sunOffsetHours = 0;
        sunOffsetMinutes = 0;
        repeatDays = [];
        deleteAfterUse = false;
        soundUri = "";
        soundName = "Default";
        vibrate = true;
        volume = 50;
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

    function getHourOptions(): number[] {
        return Array.from({ length: 25 }, (_, i) => i - 12);
    }

    function getMinuteOptions(): number[] {
        return Array.from({ length: 60 }, (_, i) => i);
    }

    function updateSunOffset() {
        if (sunOffsetHours < 0) {
            sunOffset = sunOffsetHours * 60 - sunOffsetMinutes;
        } else {
            sunOffset = sunOffsetHours * 60 + sunOffsetMinutes;
        }
    }

    $effect(() => {
        sunOffsetHours;
        sunOffsetMinutes;
        updateSunOffset();
    });

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
        if (isOpen && hoursWheelRef && minutesWheelRef) {
            setTimeout(() => {
                scrollToValue(hoursWheelRef, sunOffsetHours, getHourOptions());
                scrollToValue(minutesWheelRef, sunOffsetMinutes, getMinuteOptions());
            }, 50);
        }
    });

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
            volume,
            gradualVolume,
            gradualDuration,
        };

        onsave(newAlarm);
        handleClose();
    }

    function handleClose() {
        onclose();
        resetForm();
    }

    function getDayLabel(dayKey: string): string {
        return t[dayKey as keyof Translation] as string;
    }

    function formatSunTime(): string {
        if (!location) return "--:--";
        const now = new Date();
        const sunriseDate = sunCalcService.convertSunTimeToActualTime(sunOffset, location, now);
        return sunCalcService.formatTime(sunriseDate, use24Hour);
    }

    function selectWeekdays() { repeatDays = [1, 2, 3, 4, 5]; }
    function selectWeekends() { repeatDays = [0, 6]; }
    function selectDaily() { repeatDays = [0, 1, 2, 3, 4, 5, 6]; }
    function selectNone() { repeatDays = []; }

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

<Modal open={isOpen} title={alarm ? t.editAlarm : t.addAlarm} onclose={handleClose}>
    <div class="modal-body">
        <!-- Label -->
        <div class="form-group">
            <label for="label">{t.label}</label>
            <input id="label" type="text" bind:value={label} placeholder={t.label} class="form-input" />
        </div>

        <!-- Clock Mode Toggle -->
        <div class="form-group">
            <div class="clock-mode-toggle">
                <button class="mode-btn" class:active={!useSunTime} onclick={() => (useSunTime = false)} type="button">
                    <Clock size={16} aria-hidden="true" />
                    {t.normalClock}
                </button>
                <button class="mode-btn" class:active={useSunTime} onclick={() => (useSunTime = true)} disabled={!location} type="button">
                    <Sun size={16} aria-hidden="true" />
                    {t.sunBasedClock}
                </button>
            </div>
        </div>

        {#if useSunTime}
            <!-- Sun Offset Time Picker -->
            <div class="form-group">
                <label>
                    {t.sunOffset}
                    <span class="sun-time-preview">{formatSunTime()}</span>
                </label>
                <div class="time-picker-container">
                    <div class="time-picker-column">
                        <div class="picker-label">Hours</div>
                        <div class="wheel-picker-wrapper">
                            <div class="wheel-picker-overlay"></div>
                            <div class="wheel-picker-selection"></div>
                            <div class="wheel-picker" bind:this={hoursWheelRef} onscroll={() => handleWheelScroll(hoursWheelRef, getHourOptions(), (v) => (sunOffsetHours = v))}>
                                {#each getHourOptions() as hour}
                                    <div class="wheel-item" class:selected={sunOffsetHours === hour} onclick={() => { sunOffsetHours = hour; scrollToValue(hoursWheelRef, hour, getHourOptions()); }}>
                                        {hour >= 0 ? `+${hour}` : hour}
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
                            <div class="wheel-picker" bind:this={minutesWheelRef} onscroll={() => handleWheelScroll(minutesWheelRef, getMinuteOptions(), (v) => (sunOffsetMinutes = v))}>
                                {#each getMinuteOptions() as minute}
                                    <div class="wheel-item" class:selected={sunOffsetMinutes === minute} onclick={() => { sunOffsetMinutes = minute; scrollToValue(minutesWheelRef, minute, getMinuteOptions()); }}>
                                        {minute.toString().padStart(2, "0")}
                                    </div>
                                {/each}
                            </div>
                        </div>
                    </div>
                </div>
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
                <input id="time" type="time" bind:value={time} class="form-input time-input" />
            </div>
        {/if}

        <!-- Repeat Days -->
        <div class="form-group">
            <label>{t.repeat}</label>
            <div class="quick-select">
                <button class="quick-btn" onclick={selectWeekdays} type="button">{t.weekdays}</button>
                <button class="quick-btn" onclick={selectWeekends} type="button">{t.weekends}</button>
                <button class="quick-btn" onclick={selectDaily} type="button">{t.daily}</button>
                <button class="quick-btn" onclick={selectNone} type="button">{t.once}</button>
            </div>
            <div class="days-selector">
                {#each dayNames as { key, value }}
                    <button class="day-btn" class:selected={repeatDays.includes(value)} onclick={() => toggleDay(value)} type="button">
                        {getDayLabel(key)}
                    </button>
                {/each}
            </div>
        </div>

        <!-- Alarm Sound -->
        <div class="form-group">
            <label>{t.alarmSound}</label>
            <button class="sound-select-btn" onclick={() => (showSoundPicker = !showSoundPicker)} type="button">
                <Bell size={16} aria-hidden="true" />
                {soundName}
            </button>

            {#if showSoundPicker}
                <div class="sound-picker">
                    <button onclick={() => chooseSound("Default", "")} type="button">
                        <Bell size={14} aria-hidden="true" /> Default
                    </button>
                    <button onclick={() => chooseSound("Gentle", "gentle.mp3")} type="button">
                        <Bell size={14} aria-hidden="true" /> Gentle
                    </button>
                    <button onclick={() => chooseSound("Loud", "loud.mp3")} type="button">
                        <Bell size={14} aria-hidden="true" /> Loud
                    </button>
                    <button onclick={() => chooseSound("Nature", "nature.mp3")} type="button">
                        <Bell size={14} aria-hidden="true" /> Nature
                    </button>
                    <button onclick={() => chooseSound("Digital", "digital.mp3")} type="button">
                        <Bell size={14} aria-hidden="true" /> Digital
                    </button>
                    <button class="btn-browse-files" onclick={browseSoundFiles} type="button">
                        <Folder size={14} aria-hidden="true" /> {t.browseFiles}
                    </button>
                </div>
            {/if}

            {#if soundUri && (soundUri.startsWith('file://') || soundUri.startsWith('content://'))}
                <div class="sound-warning">
                    <Bell size={14} aria-hidden="true" />
                    <span>Note: Custom sounds from storage will be played by the app, not by the system notification.</span>
                </div>
            {/if}
        </div>

        <!-- Volume -->
        <div class="form-group">
            <label>{t.volume}: {volume}%</label>
            <div class="volume-control">
                <VolumeX size={20} />
                <input type="range" min="0" max="100" bind:value={volume} class="volume-slider" />
                <Volume2 size={20} />
            </div>
        </div>

        <!-- Vibrate -->
        <div class="form-group">
            <div class="toggle-option-row">
                <span class="toggle-option-text">
                    <Vibrate size={16} /> {t.vibrate}
                </span>
                <Toggle bind:checked={vibrate} />
            </div>
        </div>

        <!-- Gradual Volume -->
        <div class="form-group">
            <div class="toggle-option-row">
                <span class="toggle-option-text">
                    <TrendingUp size={16} /> {t.gradualVolume}
                </span>
                <Toggle bind:checked={gradualVolume} />
            </div>
        </div>

        {#if gradualVolume}
            <div class="form-group">
                <label for="gradualDuration">{t.gradualDuration}: {gradualDuration} {t.seconds}</label>
                <input id="gradualDuration" type="range" bind:value={gradualDuration} min="10" max="120" step="5" class="duration-slider" />
            </div>
        {/if}

        <!-- Delete After Use -->
        <div class="form-group">
            <div class="toggle-option-row">
                <span class="toggle-option-text">
                    <Trash2 size={16} color="var(--color-error)" /> {t.deleteAfterUse}
                </span>
                <Toggle bind:checked={deleteAfterUse} />
            </div>
        </div>
    </div>

    {#snippet footer()}
        <Button variant="secondary" onclick={handleClose} fullWidth>{t.cancel}</Button>
        <Button variant="primary" onclick={handleSave} fullWidth>{t.save}</Button>
    {/snippet}
</Modal>

<style>
    .modal-body {
        padding: var(--space-4);
        overflow-y: auto;
        flex: 1;
    }

    .form-group {
        margin-bottom: var(--space-6);
    }

    .form-group > label:not(.toggle-option-label) {
        display: block;
        margin-bottom: var(--space-2);
        color: var(--color-text-primary);
        font-weight: 500;
        font-size: var(--text-base);
    }

    .form-input {
        width: 100%;
        padding: var(--space-3);
        background: var(--color-bg-elevated);
        border: 1px solid var(--color-border);
        border-radius: var(--radius-md);
        color: var(--color-text-primary);
        font-size: var(--text-base);
        transition: border-color var(--duration-normal);
    }

    .form-input:focus {
        outline: none;
        border-color: var(--color-primary);
    }

    .time-input {
        font-size: var(--text-2xl);
        font-family: var(--font-mono);
        text-align: center;
    }

    .clock-mode-toggle {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: var(--space-2);
    }

    .mode-btn {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: var(--space-2);
        padding: var(--space-3);
        min-height: 48px;
        background: var(--color-bg-elevated);
        border: 1px solid var(--color-border);
        border-radius: var(--radius-md);
        color: var(--color-text-primary);
        cursor: pointer;
        font-size: var(--text-base);
        transition: all var(--duration-normal);
        -webkit-tap-highlight-color: transparent;
    }

    .mode-btn:hover:not(:disabled) {
        border-color: var(--color-primary);
    }

    .mode-btn.active {
        background: var(--color-primary);
        border-color: var(--color-primary);
        color: #000;
        font-weight: bold;
    }

    .mode-btn:disabled {
        opacity: 0.3;
        cursor: not-allowed;
    }

    .sun-time-preview {
        color: var(--color-primary);
        font-family: var(--font-mono);
        font-size: var(--text-lg);
        margin-left: var(--space-2);
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
        margin-bottom: var(--space-2);
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
        width: 90px;
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

    .offset-hint {
        text-align: center;
        font-size: var(--text-sm);
        color: var(--color-text-secondary);
        margin-top: var(--space-2);
    }

    .quick-select {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: var(--space-2);
        margin-bottom: var(--space-3);
        width: 100%;
    }

    .quick-btn {
        width: 100%;
        padding: var(--space-2);
        background: var(--color-bg-elevated);
        border: 1px solid var(--color-border);
        border-radius: var(--radius-md);
        color: var(--color-text-primary);
        cursor: pointer;
        font-size: var(--text-sm);
        transition: all var(--duration-normal);
        -webkit-tap-highlight-color: transparent;
    }

    .quick-btn:hover {
        border-color: var(--color-primary);
    }

    .days-selector {
        display: grid;
        grid-template-columns: repeat(7, 1fr);
        gap: var(--space-2);
    }

    .day-btn {
        aspect-ratio: 1;
        background: var(--color-bg-tertiary);
        border: 2px solid var(--color-border);
        border-radius: 50%;
        color: var(--color-text-primary);
        cursor: pointer;
        font-size: var(--text-sm);
        font-weight: 500;
        transition: all var(--duration-normal);
        display: flex;
        align-items: center;
        justify-content: center;
        -webkit-tap-highlight-color: transparent;
    }

    .day-btn:hover {
        border-color: var(--color-primary);
    }

    .day-btn.selected {
        background: var(--color-primary);
        border-color: var(--color-primary);
        color: #000;
        font-weight: bold;
    }

    .sound-select-btn {
        width: 100%;
        padding: var(--space-3);
        background: var(--color-bg-elevated);
        border: 1px solid var(--color-border);
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

    .sound-picker {
        display: flex;
        flex-direction: column;
        gap: var(--space-2);
        margin-top: var(--space-3);
        padding: var(--space-3);
        background: var(--color-bg-tertiary);
        border-radius: var(--radius-md);
    }

    .sound-picker button {
        padding: var(--space-2);
        background: var(--color-bg-secondary);
        border: 1px solid var(--color-border);
        border-radius: var(--radius-sm);
        color: var(--color-text-primary);
        cursor: pointer;
        transition: all var(--duration-normal);
        -webkit-tap-highlight-color: transparent;
        display: flex;
        align-items: center;
        gap: var(--space-2);
    }

    .sound-picker button:hover {
        border-color: var(--color-primary);
    }

    .btn-browse-files {
        background: var(--color-primary) !important;
        color: #000 !important;
        font-weight: bold;
    }

    .sound-warning {
        display: flex;
        align-items: center;
        gap: var(--space-2);
        margin-top: var(--space-2);
        padding: var(--space-3);
        background: var(--color-primary-light);
        border: 1px solid rgba(255, 149, 0, 0.3);
        border-radius: var(--radius-sm);
        color: var(--color-primary);
        font-size: var(--text-sm);
        line-height: 1.4;
    }

    .volume-control {
        display: flex;
        align-items: center;
        gap: var(--space-4);
        padding: var(--space-2);
        background: var(--color-bg-secondary);
        border-radius: var(--radius-md);
        color: var(--color-text-secondary);
    }

    .volume-slider {
        flex: 1;
        height: 6px;
        background: var(--color-bg-elevated);
        border-radius: 3px;
        outline: none;
        -webkit-appearance: none;
        appearance: none;
        cursor: pointer;
    }

    .volume-slider::-webkit-slider-thumb {
        -webkit-appearance: none;
        appearance: none;
        width: 18px;
        height: 18px;
        background: var(--color-primary);
        border-radius: 50%;
        cursor: pointer;
        transition: all var(--duration-normal);
    }

    .volume-slider::-webkit-slider-thumb:hover {
        transform: scale(1.2);
        background: var(--color-primary-hover);
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

    .duration-slider {
        width: 100%;
        height: 6px;
        background: var(--color-bg-elevated);
        border-radius: 3px;
        outline: none;
        -webkit-appearance: none;
        appearance: none;
    }

    .duration-slider::-webkit-slider-thumb {
        -webkit-appearance: none;
        width: 18px;
        height: 18px;
        background: var(--color-primary);
        border-radius: 50%;
        cursor: pointer;
    }

    @media (max-width: 768px) {
        .quick-select {
            grid-template-columns: repeat(2, 1fr);
        }

        .quick-btn {
            font-size: var(--text-xs);
        }

        .mode-btn {
            font-size: var(--text-sm);
        }
    }
</style>
