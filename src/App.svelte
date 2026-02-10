<script lang="ts">
    import { onMount, onDestroy } from "svelte";
    import AlarmList from "./components/AlarmList.svelte";
    import AlarmEditor from "./components/AlarmEditor.svelte";
    import SunTimesDisplay from "./components/SunTimesDisplay.svelte";
    import Timer from "./components/Timer.svelte";
    import Stopwatch from "./components/Stopwatch.svelte";
    import Pomodoro from "./components/Pomodoro.svelte";
    import BulkActions from "./components/BulkActions.svelte";
    import DigitalClock from "./components/DigitalClock.svelte";
    import { 
        MoreVertical, 
        Plus, 
        MapPin, 
        Clock, 
        Sun, 
        Timer as TimerIcon, 
        Brain, 
        Settings, 
        Download, 
        Upload,
        Bell
    } from "@lucide/svelte";

    import {
        alarms,
        settings,
        location,
        sunTimes,
        loadFromStorage,
        saveToStorage,
    } from "./stores";
    import { locationService } from "./services/location";
    import { filePickerService } from "./services/filePicker";
    import { sunCalcService } from "./services/sunCalc";
    import { alarmService } from "./services/alarm";
    import { batteryOptimizationService } from "./services/batteryOptimization";
    import { systemAlarmService } from "./services/systemAlarm";
    import { settingsImportExportService } from "./services/settingsImportExport";
    import { getTranslation } from "./utils/translations";
    import type { Alarm, Language } from "./types";

    let currentTime = new Date();
    let timeInterval: number;
    let locationWatchId: string | null = null;

    let showAlarmEditor = false;
    let editingAlarm: Alarm | null = null;
    let showBulkActions = false;
    let currentView:
        | "alarms"
        | "sun-times"
        | "settings"
        | "timer"
        | "stopwatch"
        | "pomodoro" = "alarms";

    let alarmsData: Alarm[] = [];
    let settingsData: any;
    let locationData: any;
    let sunTimesData: any[] = [];

    // Permission and optimization states
    let showBatteryOptimizationAlert = false;
    let showSystemAlarmAlert = false;
    let hasBatteryOptimization = false;
    let hasSystemAlarmAccess = false;

    // Subscribe to stores
    const unsubAlarms = alarms.subscribe((value) => (alarmsData = value));
    const unsubSettings = settings.subscribe((value) => (settingsData = value));
    const unsubLocation = location.subscribe((value) => (locationData = value));
    const unsubSunTimes = sunTimes.subscribe((value) => (sunTimesData = value));

    // Reactive derived translation
    $: t = getTranslation(settingsData.language as Language);
    $: sortedAlarms = [...alarmsData].sort((a, b) => {
        if (a.enabled && !b.enabled) return -1;
        if (!a.enabled && b.enabled) return 1;
        return a.time.localeCompare(b.time);
    });

    onMount(async () => {
        // Load saved data
        await loadFromStorage();
        saveToStorage();

        // Start clock
        timeInterval = window.setInterval(() => {
            currentTime = new Date();
        }, 1000);

        // Request permissions
        await requestPermissions();

        // Get location
        await updateLocation();

        // Initialize alarm listeners
        alarmService.initializeListeners(handleAlarmTriggered);

        // Check battery optimization and system alarm access
        await checkSystemStatus();
    });

    async function checkSystemStatus() {
        try {
            // Check battery optimization
            hasBatteryOptimization = await batteryOptimizationService.isBatteryOptimizationEnabled();
            showBatteryOptimizationAlert = hasBatteryOptimization;

            // Check system alarm access
            hasSystemAlarmAccess = await systemAlarmService.checkSystemAlarmAccess();
            showSystemAlarmAlert = !hasSystemAlarmAccess;
        } catch (error) {
            console.error("Error checking system status:", error);
        }
    }

    async function handleBatteryOptimization() {
        await batteryOptimizationService.openBatteryOptimizationSettings();
        // Recheck after returning
        setTimeout(async () => {
            hasBatteryOptimization = await batteryOptimizationService.isBatteryOptimizationEnabled();
            showBatteryOptimizationAlert = hasBatteryOptimization;
        }, 1000);
    }

    async function handleSystemAlarmAccess() {
        const granted = await systemAlarmService.requestSystemAlarmAccess();
        hasSystemAlarmAccess = granted;
        showSystemAlarmAlert = !granted;
    }

    async function handleExportSettings() {
        const fileName = await settingsImportExportService.exportSettings();
        if (fileName) {
            // Show success message or notification
            console.log("Settings exported to:", fileName);
        }
    }

    async function handleImportSettings() {
        try {
            // For web, use file picker
            const input = document.createElement("input");
            input.type = "file";
            input.accept = ".json";
            input.onchange = async (e: Event) => {
                const file = (e.target as HTMLInputElement).files?.[0];
                if (file) {
                    const reader = new FileReader();
                    reader.onload = async (event) => {
                        const content = event.target?.result as string;
                        // Save to temp file and import
                        const tempFileName = "temp_import.json";
                        const { Filesystem, Directory, Encoding } = await import("@capacitor/filesystem");
                        await Filesystem.writeFile({
                            path: tempFileName,
                            data: content,
                            directory: Directory.Documents,
                            encoding: Encoding.UTF8,
                        });
                        const success = await settingsImportExportService.importSettings(tempFileName);
                        if (success) {
                            // Reload data
                            await loadFromStorage();
                            console.log("Settings imported successfully");
                        }
                        // Clean up temp file
                        await Filesystem.deleteFile({
                            path: tempFileName,
                            directory: Directory.Documents,
                        });
                    };
                    reader.readAsText(file);
                }
            };
            input.click();
        } catch (error) {
            console.error("Error importing settings:", error);
        }
    }

    onDestroy(() => {
        if (timeInterval) clearInterval(timeInterval);
        if (locationWatchId) locationService.clearWatch(locationWatchId);
        alarmService.removeListeners();
        stopAlarmSound();
        unsubAlarms();
        unsubSettings();
        unsubLocation();
        unsubSunTimes();
    });

    async function requestPermissions() {
        try {
            await locationService.requestPermissions();
            await alarmService.requestPermissions();
            await filePickerService.requestPermissions();
        } catch (error) {
            console.error("Error requesting permissions:", error);
        }
    }

    async function updateLocation() {
        try {
            const currentLocation = await locationService.getCurrentLocation();
            if (currentLocation) {
                location.update(currentLocation);
                updateSunTimes(currentLocation);
            }
        } catch (error) {
            console.error("Error updating location:", error);
        }
    }

    function updateSunTimes(currentLocation: any) {
        const upcoming = sunCalcService.getUpcomingSunTimes(currentLocation, 5);
        sunTimes.update(upcoming);
    }

    function handleAddAlarm() {
        editingAlarm = null;
        showAlarmEditor = true;
    }

    function handleEditAlarm(alarm: Alarm) {
        editingAlarm = alarm;
        showAlarmEditor = true;
    }

    async function handleSaveAlarm(alarm: Alarm) {
        if (editingAlarm) {
            alarms.update(alarm.id, alarm);
        } else {
            alarms.add(alarm);
        }

        // Schedule the alarm
        if (alarm.enabled) {
            await alarmService.scheduleAlarm(alarm, locationData);
        }

        showAlarmEditor = false;
        editingAlarm = null;
    }

    function handleCloseEditor() {
        showAlarmEditor = false;
        editingAlarm = null;
    }

    async function handleDeleteAlarm(id: string) {
        await alarmService.cancelAlarm(id);
        alarms.remove(id);
    }

    async function handleToggleAlarm(id: string) {
        const alarm = alarmsData.find((a) => a.id === id);
        if (!alarm) return;

        alarms.toggle(id);

        if (alarm.enabled) {
            await alarmService.cancelAlarm(id);
        } else {
            await alarmService.scheduleAlarm(
                { ...alarm, enabled: true },
                locationData,
            );
        }
    }

    function handleAlarmTriggered(alarmId: string, extra: any) {
        console.log("Alarm triggered:", alarmId, extra);

        // Play custom sound if available
        if (extra.soundUri && (extra.soundUri.startsWith('file://') || extra.soundUri.startsWith('content://'))) {
            playCustomAlarmSound(extra.soundUri, extra.volume || 100, extra.gradualVolume, extra.gradualDuration);
        }

        // If delete after use, remove the alarm
        if (extra.deleteAfterUse) {
            handleDeleteAlarm(alarmId);
        } else {
            // Reschedule the alarm
            const alarm = alarmsData.find((a) => a.id === alarmId);
            if (alarm) {
                alarmService.scheduleAlarm(alarm, locationData);
            }
        }
    }

    let alarmAudio: HTMLAudioElement | null = null;
    let volumeIntervalId: number | null = null;

    function playCustomAlarmSound(soundUri: string, targetVolume: number, gradualVolume: boolean, gradualDuration: number) {
        try {
            // Stop any existing alarm sound
            stopAlarmSound();

            // Create new audio element
            alarmAudio = new Audio(soundUri);
            alarmAudio.loop = true;

            if (gradualVolume) {
                // Start at low volume
                alarmAudio.volume = 0.1;
                alarmAudio.play();

                // Gradually increase volume
                const steps = gradualDuration * 10; // 10 steps per second
                const volumeIncrement = (targetVolume / 100 - 0.1) / steps;
                let currentStep = 0;

                volumeIntervalId = window.setInterval(() => {
                    if (alarmAudio && currentStep < steps) {
                        currentStep++;
                        const newVolume = Math.min(0.1 + (volumeIncrement * currentStep), targetVolume / 100);
                        alarmAudio.volume = newVolume;
                    } else {
                        if (volumeIntervalId) {
                            clearInterval(volumeIntervalId);
                            volumeIntervalId = null;
                        }
                    }
                }, 100);
            } else {
                // Play at target volume immediately
                alarmAudio.volume = targetVolume / 100;
                alarmAudio.play();
            }

            // Add error handler
            alarmAudio.onerror = (error) => {
                console.error("Error playing custom alarm sound:", error);
                stopAlarmSound();
            };

            // Auto-stop after 10 minutes (safety)
            setTimeout(() => {
                stopAlarmSound();
            }, 600000);
        } catch (error) {
            console.error("Error setting up custom alarm sound:", error);
        }
    }

    function stopAlarmSound() {
        if (alarmAudio) {
            alarmAudio.pause();
            alarmAudio = null;
        }
        if (volumeIntervalId) {
            clearInterval(volumeIntervalId);
            volumeIntervalId = null;
        }
    }

    function toggleDefaultClockMode() {
        settings.update({
            defaultClockMode:
                settingsData.defaultClockMode === "normal" ? "sun" : "normal",
        });
    }

    function toggle24HourFormat() {
        settings.update({
            use24HourFormat: !settingsData.use24HourFormat,
        });
    }

    function changeLanguage(lang: Language) {
        settings.update({ language: lang });
    }

    function getCurrentSunrise(): Date | null {
        if (sunTimesData.length === 0) return null;
        return sunTimesData[0].sunrise;
    }

    async function handleDisableAllAlarms() {
        alarmsData.forEach((alarm) => {
            if (alarm.enabled) {
                alarmService.cancelAlarm(alarm.id);
            }
        });
        alarms.disableAll();
        showBulkActions = false;
    }

    async function handleEnableAllAlarms() {
        alarms.enableAll();
        alarmsData.forEach((alarm) => {
            alarmService.scheduleAlarm(alarm, locationData);
        });
    }

    async function handleDeleteAllAlarms() {
        alarmsData.forEach((alarm) => {
            alarmService.cancelAlarm(alarm.id);
        });
        alarms.deleteAll();
        showBulkActions = false;
    }
</script>

<main style="padding-bottom: 96px;">
    <div class="app-container">
        <!-- Header -->
        <header class="app-header">
            <h1 class="app-title">{t.appName}</h1>
            <div class="header-actions">
                {#if currentView === "alarms"}
                    <button
                        class="btn-icon btn-icon-orange"
                        on:click={() => (showBulkActions = !showBulkActions)}
                        title="Bulk Actions"
                    >
                        <MoreVertical size={20} aria-hidden="true" />
                    </button>
                {/if}
            </div>
        </header>

        <!-- Content Views -->
        <div class="content-container">
            {#if currentView === "alarms"}
                <BulkActions
                    visible={showBulkActions}
                    alarms={alarmsData}
                    {t}
                    onDisableAll={handleDisableAllAlarms}
                    onEnableAll={handleEnableAllAlarms}
                    onDeleteAll={handleDeleteAllAlarms}
                />
                <div class="view-content">
                    <DigitalClock
                        use24Hour={settingsData.use24HourFormat}
                        showSeconds={settingsData.showSeconds}
                        useSunBased={settingsData.defaultClockMode === "sun"}
                        sunriseTime={getCurrentSunrise()}
                    />
                    <AlarmList
                        alarms={sortedAlarms}
                        {t}
                        use24Hour={settingsData.use24HourFormat}
                        onEdit={handleEditAlarm}
                        onDelete={handleDeleteAlarm}
                        onToggle={handleToggleAlarm}
                    />

                    <button class="btn-add-alarm" on:click={handleAddAlarm}>
                        <Plus size={20} aria-hidden="true" />
                        {t.addAlarm}
                    </button>
                </div>
            {:else if currentView === "sun-times"}
                <div class="view-content">
                    {#if locationData}
                        <SunTimesDisplay
                            sunTimes={sunTimesData}
                            {t}
                            use24Hour={settingsData.use24HourFormat}
                            language={settingsData.language}
                        />
                    {:else}
                        <div class="empty-state">
                            <div class="empty-icon">
                                <MapPin size={64} color="#888" aria-hidden="true" />
                            </div>
                            <h3>{t.locationPermission}</h3>
                            <p>{t.locationPermissionDesc}</p>
                            <button
                                class="btn-primary"
                                on:click={updateLocation}
                            >
                                {t.grantPermission}
                            </button>
                        </div>
                    {/if}
                </div>
            {:else if currentView === "settings"}
                <div class="view-content settings-view">
                    <div class="settings-section">
                        <h3>{t.defaultClockMode}</h3>
                        <div class="setting-item">
                            <div class="time-format-toggle">
                                <button
                                    class="format-btn"
                                    class:active={settingsData.defaultClockMode ===
                                        "normal"}
                                    on:click={() =>
                                        settings.update({
                                            defaultClockMode: "normal",
                                        })}
                                >
                                    <Clock size={16} aria-hidden="true" />
                                    {t.normalClock}
                                </button>
                                <button
                                    class="format-btn"
                                    class:active={settingsData.defaultClockMode ===
                                        "sun"}
                                    on:click={() =>
                                        settings.update({
                                            defaultClockMode: "sun",
                                        })}
                                >
                                    <Sun size={16} aria-hidden="true" />
                                    {t.sunBasedClock}
                                </button>
                            </div>
                        </div>
                    </div>

                    <div class="settings-section">
                        <h3>{t.use24Hour}</h3>
                        <div class="setting-item">
                            <div class="time-format-toggle">
                                <button
                                    class="format-btn"
                                    class:active={!settingsData.use24HourFormat}
                                    on:click={() =>
                                        settings.update({
                                            use24HourFormat: false,
                                        })}
                                >
                                    12 Hour Format
                                </button>
                                <button
                                    class="format-btn"
                                    class:active={settingsData.use24HourFormat}
                                    on:click={() =>
                                        settings.update({
                                            use24HourFormat: true,
                                        })}
                                >
                                    24 Hour Format
                                </button>
                            </div>
                        </div>
                    </div>

                    <div class="settings-section">
                        <h3>{t.showSeconds}</h3>
                        <div class="setting-item">
                            <label class="setting-label">
                                <span>{t.showSeconds}</span>
                                <label class="toggle">
                                    <input
                                        type="checkbox"
                                        checked={settingsData.showSeconds}
                                        on:change={() =>
                                            settings.update({
                                                showSeconds: !settingsData.showSeconds,
                                            })}
                                    />
                                    <span class="toggle-slider"></span>
                                </label>
                            </label>
                        </div>
                    </div>

                    <div class="settings-section">
                        <h3>{t.language}</h3>
                        <div class="setting-item">
                            <select
                                class="language-select"
                                value={settingsData.language}
                                on:change={(e) =>
                                    changeLanguage(
                                        e.currentTarget.value as Language,
                                    )}
                            >
                                <option value="en">English</option>
                                <option value="tr">Türkçe</option>
                                <option value="es">Español</option>
                                <option value="de">Deutsch</option>
                                <option value="fr">Français</option>
                                <option value="it">Italiano</option>
                                <option value="pt">Português</option>
                                <option value="ru">Русский</option>
                                <option value="ja">日本語</option>
                                <option value="zh">中文</option>
                                <option value="ar">العربية</option>
                            </select>
                        </div>
                    </div>

                    <div class="settings-section">
                        <h3>{t.permissions}</h3>
                        <button
                            class="btn-secondary"
                            on:click={requestPermissions}
                        >
                            {t.grantPermission}
                        </button>
                    </div>

                    <!-- Battery Optimization Alert -->
                    {#if showBatteryOptimizationAlert}
                        <div class="settings-section alert-section">
                            <h3>{t.batteryOptimization}</h3>
                            <p class="alert-description">{t.batteryOptimizationDesc}</p>
                            <button
                                class="btn-alert"
                                on:click={handleBatteryOptimization}
                            >
                                {t.grantPermission}
                            </button>
                        </div>
                    {/if}

                    <!-- System Alarm Access Alert -->
                    {#if showSystemAlarmAlert}
                        <div class="settings-section alert-section">
                            <h3>{t.systemAlarmAccess}</h3>
                            <p class="alert-description">{t.systemAlarmAccessDesc}</p>
                            <button
                                class="btn-alert"
                                on:click={handleSystemAlarmAccess}
                            >
                                {t.grantPermission}
                            </button>
                        </div>
                    {/if}

                    <!-- Export/Import Settings -->
                    <div class="settings-section">
                        <h3>{t.exportSettings} / {t.importSettings}</h3>
                        <div class="export-import-buttons">
                            <button
                                class="btn-secondary"
                                on:click={handleExportSettings}
                            >
                                <Download size={16} aria-hidden="true" />
                                {t.exportSettings}
                            </button>
                            <button
                                class="btn-secondary"
                                on:click={handleImportSettings}
                            >
                                <Upload size={16} aria-hidden="true" />
                                {t.importSettings}
                            </button>
                        </div>
                    </div>

                    <div class="settings-section">
                        <h3>{t.about}</h3>
                        <div class="about-content">
                            <p class="app-name-full">Sun-Based Alarm Clock</p>
                            <p class="app-version">Version 1.0.0</p>
                        </div>
                    </div>
                </div>
            {:else if currentView === "timer"}
                <div class="view-content">
                    <Timer {t} />
                </div>
            {:else if currentView === "stopwatch"}
                <div class="view-content">
                    <Stopwatch {t} />
                </div>
            {:else if currentView === "pomodoro"}
                <div class="view-content">
                    <Pomodoro {t} />
                </div>
            {/if}
        </div>

        <!-- Bottom Navigation -->
        <nav class="bottom-nav">
            <button
                class="nav-btn"
                class:active={currentView === "alarms"}
                on:click={() => (currentView = "alarms")}
            >
                <Bell size={24} aria-hidden="true" />
                <span>Alarms</span>
            </button>
            <button
                class="nav-btn"
                class:active={currentView === "timer"}
                on:click={() => (currentView = "timer")}
            >
                <TimerIcon size={24} aria-hidden="true" />
                <span>Timer</span>
            </button>
            <button
                class="nav-btn"
                class:active={currentView === "stopwatch"}
                on:click={() => (currentView = "stopwatch")}
            >
                <Clock size={24} aria-hidden="true" />
                <span>Stopwatch</span>
            </button>
            <button
                class="nav-btn"
                class:active={currentView === "sun-times"}
                on:click={() => (currentView = "sun-times")}
            >
                <Sun size={24} aria-hidden="true" />
                <span>Sun</span>
            </button>
            <button
                class="nav-btn"
                class:active={currentView === "pomodoro"}
                on:click={() => (currentView = "pomodoro")}
            >
                <Brain size={24} aria-hidden="true" />
                <span>Focus</span>
            </button>
            <button
                class="nav-btn"
                class:active={currentView === "settings"}
                on:click={() => (currentView = "settings")}
            >
                <Settings size={24} aria-hidden="true" />
                <span>Settings</span>
            </button>
        </nav>
    </div>

    <!-- Alarm Editor Modal -->
    <AlarmEditor
        alarm={editingAlarm}
        {t}
        use24Hour={settingsData.use24HourFormat}
        location={locationData}
        isOpen={showAlarmEditor}
        onsave={handleSaveAlarm}
        onclose={handleCloseEditor}
    />
</main>

<style>
    :global(*) {
        box-sizing: border-box;
        margin: 0;
        padding: 0;
    }

    :global(body) {
        font-family: var(--font-sans);
        background: var(--color-bg-primary);
        color: var(--color-text-primary);
        min-height: 100vh;
        overflow-x: hidden;
    }

    main {
        min-height: 100vh;
        /* Dynamic padding set via inline style based on detected nav bar height */
    }

    .app-container {
        max-width: 800px;
        margin: 0 auto;
        padding: var(--space-4);
        padding-top: calc(var(--space-4) + env(safe-area-inset-top, 0px));
    }

    .app-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: var(--space-6) var(--space-4) var(--space-4);
    }

    .app-title {
        font-size: var(--text-3xl);
        font-weight: 700;
        color: var(--color-text-primary);
    }

    .header-actions {
        display: flex;
        gap: var(--space-2);
    }

    .btn-icon {
        width: 44px;
        height: 44px;
        border: none;
        background: var(--color-bg-secondary);
        border-radius: var(--radius-md);
        cursor: pointer;
        font-size: 1.5rem;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all var(--duration-normal) var(--ease-default);
        -webkit-tap-highlight-color: transparent;
    }

    .btn-icon:hover {
        background: var(--color-primary);
        transform: scale(1.05);
    }

    .btn-icon-orange {
        color: var(--color-primary);
    }

    .btn-icon-orange:hover {
        color: #fff;
    }

    .bottom-nav {
        position: fixed;
        bottom: 0;
        left: 0;
        right: 0;
        display: grid;
        grid-template-columns: repeat(6, 1fr);
        background: var(--color-bg-primary);
        border-top: 2px solid var(--color-primary);
        padding: var(--space-2) var(--space-2) calc(var(--space-2) + var(--safe-area-inset-bottom, env(safe-area-inset-bottom, 0px)));
        gap: var(--space-1);
        z-index: var(--z-sticky);
    }

    .nav-btn {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: var(--space-2);
        background: var(--color-bg-secondary);
        border: none;
        border-radius: var(--radius-md);
        color: var(--color-text-secondary);
        cursor: pointer;
        font-size: var(--text-xs);
        font-weight: 500;
        transition: all var(--duration-normal) var(--ease-default);
        gap: var(--space-1);
        min-height: 64px;
        -webkit-tap-highlight-color: transparent;
    }

    .nav-btn:hover {
        background: var(--color-bg-tertiary);
        color: var(--color-text-primary);
    }

    .nav-btn.active {
        background: var(--color-primary);
        color: #000;
        font-weight: bold;
    }

    .nav-btn span {
        font-size: var(--text-xs);
        text-align: center;
        line-height: 1.2;
        max-width: 100%;
    }

    .content-container {
        min-height: calc(100vh - 160px);
        padding-bottom: calc(96px + var(--safe-area-inset-bottom, env(safe-area-inset-bottom, 0px)));
    }

    .view-content {
        animation: fadeIn var(--duration-normal) var(--ease-default);
    }

    .btn-add-alarm {
        width: 100%;
        max-width: 600px;
        margin: var(--space-4) auto var(--space-8);
        display: flex;
        align-items: center;
        justify-content: center;
        gap: var(--space-2);
        padding: var(--space-4);
        background: var(--color-primary);
        border: none;
        border-radius: var(--radius-md);
        color: #000;
        font-size: var(--text-lg);
        font-weight: 700;
        cursor: pointer;
        transition: all var(--duration-normal) var(--ease-default);
        -webkit-tap-highlight-color: transparent;
    }

    .btn-add-alarm:hover {
        background: var(--color-primary-hover);
        transform: translateY(-2px);
    }

    .btn-add-alarm:active {
        transform: translateY(0);
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
        margin-bottom: var(--space-4);
    }

    .empty-state h3 {
        margin: var(--space-2) 0;
        font-size: var(--text-2xl);
        color: var(--color-text-primary);
    }

    .empty-state p {
        margin: var(--space-1) 0 var(--space-6);
        font-size: var(--text-base);
    }

    .btn-primary {
        padding: var(--space-3) var(--space-8);
        background: var(--color-primary);
        border: none;
        border-radius: var(--radius-md);
        color: #000;
        font-size: var(--text-base);
        font-weight: 600;
        cursor: pointer;
        transition: all var(--duration-normal);
        -webkit-tap-highlight-color: transparent;
    }

    .btn-primary:hover {
        background: var(--color-primary-hover);
        transform: translateY(-2px);
    }

    .settings-view {
        padding: var(--space-4);
        max-width: 600px;
        margin: 0 auto;
        width: 100%;
        box-sizing: border-box;
    }

    .settings-section {
        margin-bottom: var(--space-8);
        background: var(--color-bg-secondary);
        padding: var(--space-6);
        border-radius: var(--radius-lg);
        border: 1px solid var(--color-border);
        width: 100%;
        box-sizing: border-box;
    }

    .settings-section h3 {
        color: var(--color-primary);
        margin-bottom: var(--space-4);
        font-size: var(--text-xl);
        font-weight: 600;
    }

    .setting-item {
        margin-bottom: var(--space-4);
    }

    .setting-item:last-child {
        margin-bottom: 0;
    }

    .time-format-toggle {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: var(--space-2);
        width: 100%;
        box-sizing: border-box;
    }

    .format-btn {
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
        white-space: nowrap;
        -webkit-tap-highlight-color: transparent;
        box-sizing: border-box;
        overflow: hidden;
        text-overflow: ellipsis;
    }

    .format-btn:hover {
        border-color: var(--color-primary);
    }

    .format-btn.active {
        background: var(--color-primary);
        border-color: var(--color-primary);
        font-weight: bold;
    }

    .about-content {
        padding: var(--space-4);
        background: var(--color-bg-tertiary);
        border-radius: var(--radius-md);
        border: 2px solid var(--color-border);
        text-align: center;
    }

    .app-name-full {
        font-size: var(--text-xl);
        font-weight: bold;
        color: var(--color-primary);
        margin-bottom: var(--space-2);
    }

    .app-version {
        font-size: var(--text-sm);
        color: var(--color-text-secondary);
    }

    .language-select {
        width: 100%;
        padding: var(--space-3);
        background: var(--color-bg-elevated);
        border: 1px solid var(--color-border);
        border-radius: var(--radius-md);
        color: var(--color-text-primary);
        font-size: var(--text-base);
        cursor: pointer;
        transition: all var(--duration-normal) var(--ease-default);
        -webkit-tap-highlight-color: transparent;
    }

    .language-select:focus {
        outline: none;
        border-color: var(--color-primary);
    }

    .language-select option {
        background: var(--color-bg-secondary);
        color: var(--color-text-primary);
    }

    .btn-secondary {
        width: 100%;
        padding: var(--space-3);
        background: var(--color-bg-secondary);
        border: 1px solid var(--color-primary);
        border-radius: var(--radius-md);
        color: var(--color-primary);
        font-size: var(--text-base);
        font-weight: 600;
        cursor: pointer;
        transition: all var(--duration-normal) var(--ease-default);
        -webkit-tap-highlight-color: transparent;
        box-sizing: border-box;
    }

    .btn-secondary:hover {
        background: var(--color-primary);
        color: #000;
    }

    /* Alert Section Styles */
    .alert-section {
        background: linear-gradient(135deg, var(--color-primary-light) 0%, var(--color-bg-secondary) 100%);
        border: 2px solid var(--color-primary);
    }

    .alert-section h3 {
        color: var(--color-primary);
    }

    .alert-description {
        color: var(--color-text-secondary);
        font-size: var(--text-sm);
        margin: var(--space-2) 0 var(--space-4) 0;
        line-height: 1.4;
    }

    .btn-alert {
        width: 100%;
        padding: var(--space-3);
        background: var(--color-primary);
        border: none;
        border-radius: var(--radius-md);
        color: #000;
        font-size: var(--text-base);
        font-weight: 600;
        cursor: pointer;
        transition: all var(--duration-normal) var(--ease-default);
        -webkit-tap-highlight-color: transparent;
        box-sizing: border-box;
    }

    .btn-alert:hover {
        background: var(--color-primary-hover);
        transform: translateY(-2px);
    }

    /* Export/Import Buttons */
    .export-import-buttons {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: var(--space-3);
    }

    .export-import-buttons .btn-secondary {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: var(--space-2);
    }

    @media (max-width: 768px) {
        .settings-view {
            padding: var(--space-2);
        }

        .settings-section {
            padding: var(--space-4);
            margin-bottom: var(--space-4);
        }

        .format-btn {
            font-size: var(--text-sm);
            padding: var(--space-2) var(--space-1);
            gap: var(--space-1);
        }

        .settings-section h3 {
            font-size: var(--text-base);
        }

        .app-container {
            padding: var(--space-2);
        }

        .app-title {
            font-size: var(--text-2xl);
        }

        .nav-btn {
            min-height: 56px;
            padding: var(--space-1);
        }

        .nav-btn span {
            font-size: var(--text-xs);
        }
    }
</style>
