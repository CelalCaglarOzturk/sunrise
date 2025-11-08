<script lang="ts">
    import { createEventDispatcher } from "svelte";
    import type { Alarm, Location, Translation } from "../types";
    import { filePickerService } from "../services/filePicker";
    import { sunCalcService } from "../services/sunCalc";
    import Icon from "./Icon.svelte";

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
    let sunOffsetHours = 0;
    let sunOffsetMinutes = 0;
    let repeatDays: number[] = [];
    let deleteAfterUse = false;
    let soundUri = "";
    let soundName = "Default";
    let vibrate = true;
    let volume = 50;
    let gradualVolume = false;
    let gradualDuration = 30;

    // UI state
    let showSoundPicker = false;

    // Wheel picker refs
    let hoursWheelRef: HTMLDivElement;
    let minutesWheelRef: HTMLDivElement;
    let isScrollingHours = false;
    let isScrollingMinutes = false;

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
        sunOffsetHours =
            Math.floor(Math.abs(alarm.sunOffset) / 60) *
            (alarm.sunOffset < 0 ? -1 : 1);
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

    // Generate hour options (-12 to +12)
    function getHourOptions(): number[] {
        const options = [];
        for (let i = -12; i <= 12; i++) {
            options.push(i);
        }
        return options;
    }

    // Generate minute options (0 to 59)
    function getMinuteOptions(): number[] {
        const options = [];
        for (let i = 0; i <= 59; i++) {
            options.push(i);
        }
        return options;
    }

    // Update sunOffset when hours or minutes change
    function updateSunOffset() {
        if (sunOffsetHours < 0) {
            sunOffset = sunOffsetHours * 60 - sunOffsetMinutes;
        } else {
            sunOffset = sunOffsetHours * 60 + sunOffsetMinutes;
        }
    }

    $: (sunOffsetHours, sunOffsetMinutes, updateSunOffset());

    // Scroll wheel picker to center item
    function scrollToValue(
        container: HTMLDivElement,
        value: number,
        options: number[],
    ) {
        if (!container) return;
        const index = options.indexOf(value);
        if (index === -1) return;
        const itemHeight = 40; // Height of each item
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

            // Snap to position
            container.scrollTo({
                top: clampedIndex * itemHeight,
                behavior: "smooth",
            });
            setter(options[clampedIndex]);
        }, 100);
    }

    // Initialize wheel positions when opening
    $: if (isOpen && hoursWheelRef && minutesWheelRef) {
        setTimeout(() => {
            scrollToValue(hoursWheelRef, sunOffsetHours, getHourOptions());
            scrollToValue(
                minutesWheelRef,
                sunOffsetMinutes,
                getMinuteOptions(),
            );
        }, 50);
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
            volume,
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
                <button class="btn-close" on:click={handleClose}>
                    <Icon name="x" size={20} />
                </button>
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
                            <Icon name="clock" size={16} />
                            {t.normalClock}
                        </button>
                        <button
                            class="mode-btn"
                            class:active={useSunTime}
                            on:click={() => (useSunTime = true)}
                            disabled={!location}
                        >
                            <Icon name="sun" size={16} />
                            {t.sunBasedClock}
                        </button>
                    </div>
                </div>

                {#if useSunTime}
                    <!-- Sun Offset Time Picker -->
                    <div class="form-group">
                        <label>
                            {t.sunOffset}
                            <span class="sun-time-preview"
                                >{formatSunTime()}</span
                            >
                        </label>
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
                                                (v) => (sunOffsetHours = v),
                                            )}
                                    >
                                        {#each getHourOptions() as hour}
                                            <div
                                                class="wheel-item"
                                                class:selected={sunOffsetHours ===
                                                    hour}
                                                on:click={() => {
                                                    sunOffsetHours = hour;
                                                    scrollToValue(
                                                        hoursWheelRef,
                                                        hour,
                                                        getHourOptions(),
                                                    );
                                                }}
                                            >
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
                                    <div
                                        class="wheel-picker"
                                        bind:this={minutesWheelRef}
                                        on:scroll={() =>
                                            handleWheelScroll(
                                                minutesWheelRef,
                                                getMinuteOptions(),
                                                (v) => (sunOffsetMinutes = v),
                                            )}
                                    >
                                        {#each getMinuteOptions() as minute}
                                            <div
                                                class="wheel-item"
                                                class:selected={sunOffsetMinutes ===
                                                    minute}
                                                on:click={() => {
                                                    sunOffsetMinutes = minute;
                                                    scrollToValue(
                                                        minutesWheelRef,
                                                        minute,
                                                        getMinuteOptions(),
                                                    );
                                                }}
                                            >
                                                {minute
                                                    .toString()
                                                    .padStart(2, "0")}
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
                        <Icon name="bell" size={16} />
                        {soundName}
                    </button>

                    {#if showSoundPicker}
                        <div class="sound-picker">
                            <button on:click={() => chooseSound("Default", "")}>
                                <Icon name="bell" size={14} />
                                Default
                            </button>
                            <button
                                on:click={() =>
                                    chooseSound("Gentle", "gentle.mp3")}
                            >
                                <Icon name="bell" size={14} />
                                Gentle
                            </button>
                            <button
                                on:click={() => chooseSound("Loud", "loud.mp3")}
                            >
                                <Icon name="bell" size={14} />
                                Loud
                            </button>
                            <button
                                on:click={() =>
                                    chooseSound("Nature", "nature.mp3")}
                            >
                                <Icon name="bell" size={14} />
                                Nature
                            </button>
                            <button
                                on:click={() =>
                                    chooseSound("Digital", "digital.mp3")}
                            >
                                <Icon name="bell" size={14} />
                                Digital
                            </button>
                            <button
                                class="btn-browse-files"
                                on:click={browseSoundFiles}
                            >
                                <Icon name="folder" size={14} />
                                {t.browseFiles}
                            </button>
                        </div>
                    {/if}
                </div>

                <!-- Volume -->
                <div class="form-group">
                    <label>
                        {t.volume}: {volume}%
                    </label>
                    <div class="volume-control">
                        <Icon name="volume-x" size={20} />
                        <input
                            type="range"
                            min="0"
                            max="100"
                            bind:value={volume}
                            class="volume-slider"
                        />
                        <Icon name="volume" size={20} />
                    </div>
                </div>

                <!-- Vibrate -->
                <div class="form-group">
                    <label class="toggle-option-label">
                        <span class="toggle-option-text">
                            <Icon name="vibrate" size={16} />
                            {t.vibrate}
                        </span>
                        <label class="toggle-switch">
                            <input type="checkbox" bind:checked={vibrate} />
                            <span class="toggle-slider"></span>
                        </label>
                    </label>
                </div>

                <!-- Gradual Volume -->
                <div class="form-group">
                    <label class="toggle-option-label">
                        <span class="toggle-option-text">
                            <Icon name="trending-up" size={16} />
                            {t.gradualVolume}
                        </span>
                        <label class="toggle-switch">
                            <input
                                type="checkbox"
                                bind:checked={gradualVolume}
                            />
                            <span class="toggle-slider"></span>
                        </label>
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
                <div class="form-group">
                    <label class="toggle-option-label">
                        <span class="toggle-option-text">
                            <Icon name="trash" size={16} />
                            {t.deleteAfterUse}
                        </span>
                        <label class="toggle-switch">
                            <input
                                type="checkbox"
                                bind:checked={deleteAfterUse}
                            />
                            <span class="toggle-slider"></span>
                        </label>
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
        background: rgba(0, 0, 0, 0.85);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 1000;
        padding: 1rem;
        overflow-y: auto;
        backdrop-filter: blur(5px);
    }

    .modal-content {
        background: #1a1a1a;
        border-radius: 20px;
        width: 100%;
        max-width: 500px;
        max-height: 90vh;
        display: flex;
        flex-direction: column;
        box-shadow: 0 10px 40px rgba(0, 0, 0, 0.8);
        border: 1px solid #333;
    }

    .modal-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1.5rem;
        border-bottom: 1px solid #333;
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

    .form-group label:not(.toggle-option-label) {
        display: block;
        margin-bottom: 0.5rem;
        color: var(--text-primary, #fff);
        font-weight: 500;
        font-size: 0.95rem;
    }

    .form-input {
        width: 100%;
        padding: 0.75rem;
        background: #333;
        border: 1px solid #555;
        border-radius: 10px;
        color: #fff;
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
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 0.5rem;
        padding: 0.75rem;
        background: #333;
        border: 1px solid #555;
        border-radius: 10px;
        color: #fff;
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
        width: 90px;
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

    .offset-hint {
        text-align: center;
        font-size: 0.9rem;
        color: #888;
        margin-top: 0.5rem;
    }

    .quick-select {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 0.5rem;
        margin-bottom: 0.75rem;
    }

    .quick-btn {
        width: 80px;
        padding: 0.5rem;
        background: #333;
        border: 1px solid #555;
        border-radius: 10px;
        color: #fff;
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
        padding: 0.875rem;
        background: #333;
        border: 1px solid #555;
        border-radius: 10px;
        color: #fff;
        cursor: pointer;
        font-size: 1rem;
        text-align: left;
        transition: all 0.2s;
    }

    .sound-select-btn:hover {
        border-color: #ff9500;
        background: #444;
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
        background: #333;
    }

    .btn-browse-files {
        background: var(--primary, #e94560) !important;
        font-weight: bold;
    }

    .btn-browse-files:hover {
        background: #ffaa33 !important;
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

    .volume-control {
        display: flex;
        align-items: center;
        gap: 1rem;
        padding: 0.5rem;
        background: #1a1a1a;
        border-radius: 10px;
    }

    .volume-control span {
        font-size: 1.25rem;
        flex-shrink: 0;
    }

    .volume-slider {
        flex: 1;
        height: 6px;
        background: #333;
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
        background: #ff9500;
        border-radius: 50%;
        cursor: pointer;
        transition: all 0.2s ease;
    }

    .volume-slider::-webkit-slider-thumb:hover {
        transform: scale(1.2);
        background: #ffaa33;
    }

    .volume-slider::-moz-range-thumb {
        width: 18px;
        height: 18px;
        background: #ff9500;
        border-radius: 50%;
        border: none;
        cursor: pointer;
        transition: all 0.2s ease;
    }

    .volume-slider::-moz-range-thumb:hover {
        transform: scale(1.2);
        background: #ffaa33;
    }

    .duration-slider {
        width: 100%;
        height: 6px;
        background: #333;
        border-radius: 3px;
        outline: none;
        -webkit-appearance: none;
        appearance: none;
    }

    .duration-slider::-webkit-slider-thumb {
        -webkit-appearance: none;
        width: 18px;
        height: 18px;
        background: #ff9500;
        border-radius: 50%;
        cursor: pointer;
    }

    .duration-slider::-moz-range-thumb {
        width: 18px;
        height: 18px;
        background: #ff9500;
        border-radius: 50%;
        border: none;
        cursor: pointer;
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
