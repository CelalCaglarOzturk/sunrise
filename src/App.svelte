<script lang="ts">
    import { onMount, onDestroy } from "svelte";
    import AlarmList from "./components/AlarmList.svelte";
    import AlarmEditor from "./components/AlarmEditor.svelte";
    import SunTimesDisplay from "./components/SunTimesDisplay.svelte";
    import Timer from "./components/Timer.svelte";
    import Stopwatch from "./components/Stopwatch.svelte";
    import BulkActions from "./components/BulkActions.svelte";
    import DigitalClock from "./components/DigitalClock.svelte";
    import Icon from "./components/Icon.svelte";

    import {
        alarms,
        settings,
        location,
        sunTimes,
        loadFromStorage,
        saveToStorage,
    } from "./stores";
    import { locationService } from "./services/location";
    import { sunCalcService } from "./services/sunCalc";
    import { alarmService } from "./services/alarm";
    import { getTranslation } from "./utils/translations";
    import type { Alarm, Language } from "./types";

    let currentTime = new Date();
    let timeInterval: number;
    let locationWatchId: string | null = null;
    let navBarHeight = 0;

    let showAlarmEditor = false;
    let editingAlarm: Alarm | null = null;
    let showBulkActions = false;
    let currentView:
        | "alarms"
        | "sun-times"
        | "settings"
        | "timer"
        | "stopwatch" = "alarms";

    let alarmsData: Alarm[] = [];
    let settingsData: any;
    let locationData: any;
    let sunTimesData: any[] = [];

    // Subscribe to stores
    const unsubAlarms = alarms.subscribe((value) => (alarmsData = value));
    const unsubSettings = settings.subscribe((value) => (settingsData = value));
    const unsubLocation = location.subscribe((value) => (locationData = value));
    const unsubSunTimes = sunTimes.subscribe((value) => (sunTimesData = value));

    // Detect Android navigation bar height
    function detectNavBarHeight() {
        if (typeof window !== "undefined") {
            // Calculate the difference between window height and visual viewport height
            const windowHeight = window.innerHeight;
            const visualHeight = window.visualViewport?.height || windowHeight;
            const detectedHeight = windowHeight - visualHeight;

            // If we detect a navigation bar OR if we're on Android and detection failed,
            // use detected height or fallback to typical Android 3-button nav height (72px)
            if (detectedHeight > 0) {
                navBarHeight = detectedHeight;
            } else if (
                typeof navigator !== "undefined" &&
                /Android/i.test(navigator.userAgent)
            ) {
                // Fallback for Android devices where visualViewport doesn't work
                // Typical Android 3-button navigation is 48dp which is ~72px on most devices
                navBarHeight = 72;
            } else {
                navBarHeight = 0;
            }
        }
    }

    // Reactive derived translation
    $: t = getTranslation(settingsData.language as Language);
    $: sortedAlarms = [...alarmsData].sort((a, b) => {
        if (a.enabled && !b.enabled) return -1;
        if (!a.enabled && b.enabled) return 1;
        return a.time.localeCompare(b.time);
    });

    onMount(async () => {
        // Load saved data
        loadFromStorage();
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

        // Detect navigation bar height
        detectNavBarHeight();

        // Update on resize (keyboard, orientation change, etc.)
        if (typeof window !== "undefined") {
            window.addEventListener("resize", detectNavBarHeight);
            if (window.visualViewport) {
                window.visualViewport.addEventListener(
                    "resize",
                    detectNavBarHeight,
                );
            }
        }
    });

    onDestroy(() => {
        if (timeInterval) clearInterval(timeInterval);
        if (locationWatchId) locationService.clearWatch(locationWatchId);
        alarmService.removeListeners();
        unsubAlarms();
        unsubSettings();
        unsubLocation();
        unsubSunTimes();

        // Clean up resize listeners
        if (typeof window !== "undefined") {
            window.removeEventListener("resize", detectNavBarHeight);
            if (window.visualViewport) {
                window.visualViewport.removeEventListener(
                    "resize",
                    detectNavBarHeight,
                );
            }
        }
    });

    async function requestPermissions() {
        try {
            await locationService.requestPermissions();
            await alarmService.requestPermissions();
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

    async function handleSaveAlarm(event: CustomEvent<Alarm>) {
        const alarm = event.detail;

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

    function exportSettings() {
        const data = {
            alarms: alarmsData,
            settings: settingsData,
            timers: [],
            version: "1.0.0",
            exportDate: new Date().toISOString(),
        };

        const jsonStr = JSON.stringify(data, null, 2);
        const blob = new Blob([jsonStr], { type: "application/json" });
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = `gunes-backup-${new Date().toISOString().split("T")[0]}.json`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }

    function importSettings() {
        const input = document.createElement("input");
        input.type = "file";
        input.accept = "application/json";
        input.onchange = async (e) => {
            const file = (e.target as HTMLInputElement).files?.[0];
            if (!file) return;

            try {
                const text = await file.text();
                const data = JSON.parse(text);

                if (data.alarms) {
                    alarms.set(data.alarms);
                    data.alarms.forEach((alarm: Alarm) => {
                        if (alarm.enabled) {
                            alarmService.scheduleAlarm(alarm, locationData);
                        }
                    });
                }

                if (data.settings) {
                    settings.set(data.settings);
                }

                alert("Settings imported successfully!");
            } catch (error) {
                console.error("Import error:", error);
                alert("Failed to import settings. Please check the file.");
            }
        };
        input.click();
    }
</script>

<main style="padding-bottom: {navBarHeight + 96}px;">
    <div class="app-container">
        <!-- Header -->
        <header class="app-header">
            <h1 class="app-title">{t.appName}</h1>
            <div class="header-actions">
                {#if currentView === "alarms"}
                    <button
                        class="btn-icon"
                        on:click={() => (showBulkActions = !showBulkActions)}
                        title="Bulk Actions"
                    >
                        <Icon name="more-vertical" size={20} />
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
                        {t}
                        use24Hour={settingsData.use24HourFormat}
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
                        <Icon name="plus" size={20} />
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
                        />
                    {:else}
                        <div class="empty-state">
                            <div class="empty-icon">
                                <Icon name="map-pin" size={64} color="#888" />
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
                            <label class="setting-label">
                                <span>
                                    <Icon name="clock" size={16} />
                                    {t.normalClock} /
                                    <Icon name="sun" size={16} />
                                    {t.sunBasedClock}
                                </span>
                                <label class="toggle-switch">
                                    <input
                                        type="checkbox"
                                        checked={settingsData.defaultClockMode ===
                                            "sun"}
                                        on:change={toggleDefaultClockMode}
                                    />
                                    <span class="toggle-slider"></span>
                                </label>
                            </label>
                        </div>
                    </div>

                    <div class="settings-section">
                        <h3>{t.use24Hour}</h3>
                        <div class="setting-item">
                            <label class="setting-label">
                                <span>24-hour format</span>
                                <label class="toggle-switch">
                                    <input
                                        type="checkbox"
                                        checked={settingsData.use24HourFormat}
                                        on:change={toggle24HourFormat}
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

                    <div class="settings-section">
                        <h3>{t.about}</h3>
                        <div class="setting-item">
                            <button
                                class="btn-secondary"
                                on:click={exportSettings}
                            >
                                <Icon name="download" size={16} />
                                {t.exportSettings}
                            </button>
                        </div>
                        <div class="setting-item">
                            <button
                                class="btn-secondary"
                                on:click={importSettings}
                            >
                                <Icon name="upload" size={16} />
                                {t.importSettings}
                            </button>
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
            {/if}
        </div>

        <!-- Bottom Navigation -->
        <nav class="bottom-nav" style="bottom: {navBarHeight}px;">
            <button
                class="nav-btn"
                class:active={currentView === "alarms"}
                on:click={() => (currentView = "alarms")}
            >
                <Icon name="bell" size={24} />
                <span>Alarms</span>
            </button>
            <button
                class="nav-btn"
                class:active={currentView === "sun-times"}
                on:click={() => (currentView = "sun-times")}
            >
                <Icon name="sun" size={24} />
                <span>Sun</span>
            </button>
            <button
                class="nav-btn"
                class:active={currentView === "timer"}
                on:click={() => (currentView = "timer")}
            >
                <Icon name="timer" size={24} />
                <span>Timer</span>
            </button>
            <button
                class="nav-btn"
                class:active={currentView === "stopwatch"}
                on:click={() => (currentView = "stopwatch")}
            >
                <Icon name="clock" size={24} />
                <span>Watch</span>
            </button>
            <button
                class="nav-btn"
                class:active={currentView === "settings"}
                on:click={() => (currentView = "settings")}
            >
                <Icon name="settings" size={24} />
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
        on:save={handleSaveAlarm}
        on:close={handleCloseEditor}
    />
</main>

<style>
    :global(*) {
        box-sizing: border-box;
        margin: 0;
        padding: 0;
    }

    :global(body) {
        font-family:
            -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen,
            Ubuntu, Cantarell, sans-serif;
        background: #000;
        color: #fff;
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
        padding: 1rem;
        padding-top: calc(1rem + env(safe-area-inset-top, 0px));
    }

    .app-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1.5rem 1rem 1rem;
    }

    .app-title {
        font-size: 2rem;
        font-weight: 700;
        color: #fff;
    }

    .header-actions {
        display: flex;
        gap: 0.5rem;
    }

    .btn-icon {
        width: 44px;
        height: 44px;
        border: none;
        background: #1a1a1a;
        border-radius: 12px;
        cursor: pointer;
        font-size: 1.5rem;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.2s ease;
    }

    .btn-icon:hover {
        background: #ff9500;
        transform: scale(1.05);
    }

    .bottom-nav {
        position: fixed;
        bottom: 0;
        left: 0;
        right: 0;
        display: grid;
        grid-template-columns: repeat(5, 1fr);
        background: #1a1a1a;
        border-top: 1px solid #333;
        padding: 0.5rem 0.5rem calc(0.5rem + env(safe-area-inset-bottom, 0px));
        gap: 0.25rem;
        z-index: 1000;
    }

    .nav-btn {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 0.5rem;
        background: #1a1a1a;
        border: none;
        border-radius: 15px;
        color: #888;
        cursor: pointer;
        font-size: 0.7rem;
        font-weight: 500;
        transition: all 0.2s ease;
        gap: 0.25rem;
        min-height: 64px;
    }

    .nav-btn:hover {
        background: #2a2a2a;
        color: #fff;
    }

    .nav-btn.active {
        background: #ff9500;
        color: #000;
        font-weight: bold;
    }

    .nav-btn svg {
        flex-shrink: 0;
    }

    .nav-btn span {
        font-size: 0.7rem;
        text-align: center;
        line-height: 1.2;
        max-width: 100%;
    }

    .content-container {
        min-height: calc(100vh - 200px);
        padding-bottom: calc(100px + env(safe-area-inset-bottom, 0px));
    }

    .view-content {
        animation: fadeIn 0.3s ease;
    }

    @keyframes fadeIn {
        from {
            opacity: 0;
            transform: translateY(10px);
        }
        to {
            opacity: 1;
            transform: translateY(0);
        }
    }

    .btn-add-alarm {
        width: 100%;
        max-width: 600px;
        margin: 1rem auto 2rem;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 0.5rem;
        padding: 1rem;
        background: #ff9500;
        border: none;
        border-radius: 15px;
        color: #000;
        font-size: 1.1rem;
        font-weight: 700;
        cursor: pointer;
        transition: all 0.2s ease;
    }

    .btn-add-alarm:hover {
        background: #ffaa33;
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
        padding: 3rem 1rem;
        text-align: center;
        color: #888;
    }

    .empty-icon {
        margin-bottom: 1rem;
    }

    .empty-state h3 {
        margin: 0.5rem 0;
        font-size: 1.5rem;
        color: #fff;
    }

    .empty-state p {
        margin: 0.25rem 0 1.5rem;
        font-size: 1rem;
    }

    .btn-primary {
        padding: 0.875rem 2rem;
        background: #ff9500;
        border: none;
        border-radius: 10px;
        color: #000;
        font-size: 1rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;
    }

    .btn-primary:hover {
        background: #ffaa33;
        transform: translateY(-2px);
    }

    .settings-view {
        padding: 1rem;
        max-width: 600px;
        margin: 0 auto;
    }

    .settings-section {
        margin-bottom: 2rem;
        background: #1a1a1a;
        padding: 1.5rem;
        border-radius: 15px;
        border: 1px solid #333;
    }

    .settings-section h3 {
        color: #ff9500;
        margin-bottom: 1rem;
        font-size: 1.2rem;
        font-weight: 600;
    }

    .setting-item {
        margin-bottom: 1rem;
    }

    .setting-item:last-child {
        margin-bottom: 0;
    }

    .setting-label {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0.75rem;
        background: #333;
        border-radius: 10px;
        cursor: pointer;
        transition: all 0.2s;
    }

    .setting-label:hover {
        background: #444;
    }

    .toggle-switch {
        position: relative;
        display: inline-block;
        width: 50px;
        height: 28px;
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
        background-color: #333;
        transition: 0.4s;
        border-radius: 34px;
    }

    .toggle-slider:before {
        position: absolute;
        content: "";
        height: 20px;
        width: 20px;
        left: 4px;
        bottom: 4px;
        background-color: white;
        transition: 0.4s;
        border-radius: 50%;
    }

    input:checked + .toggle-slider {
        background-color: #ff9500;
    }

    input:checked + .toggle-slider:before {
        transform: translateX(26px);
    }

    .language-select {
        width: 100%;
        padding: 0.75rem;
        background: #333;
        border: 1px solid #555;
        border-radius: 10px;
        color: #fff;
        font-size: 1rem;
        cursor: pointer;
        transition: all 0.2s ease;
    }

    .language-select:focus {
        outline: none;
        border-color: #ff9500;
    }

    .language-select option {
        background: #16213e;
        color: #fff;
    }

    .btn-secondary {
        width: 100%;
        padding: 0.75rem;
        background: #1a1a1a;
        border: 1px solid #ff9500;
        border-radius: 10px;
        color: #ff9500;
        font-size: 1rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.3s ease;
    }

    .btn-secondary:hover {
        background: #ff9500;
        color: #000;
    }

    @media (max-width: 768px) {
        .app-container {
            padding: 0.5rem;
        }

        .app-title {
            font-size: 1.5rem;
        }

        .settings-section {
            padding: 1rem;
        }

        .nav-btn {
            min-height: 56px;
            padding: 0.4rem;
        }

        .nav-btn span {
            font-size: 0.6rem;
        }
    }
</style>
