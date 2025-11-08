<script lang="ts">
    import { onMount, onDestroy } from "svelte";
    import CircularClock from "./components/CircularClock.svelte";
    import AlarmList from "./components/AlarmList.svelte";
    import AlarmEditor from "./components/AlarmEditor.svelte";
    import SunTimesDisplay from "./components/SunTimesDisplay.svelte";
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

    let showAlarmEditor = false;
    let editingAlarm: Alarm | null = null;
    let currentView: "alarms" | "sun-times" | "settings" = "alarms";

    let alarmsData: Alarm[] = [];
    let settingsData: any;
    let locationData: any;
    let sunTimesData: any[] = [];

    // Subscribe to stores
    const unsubAlarms = alarms.subscribe((value) => (alarmsData = value));
    const unsubSettings = settings.subscribe((value) => (settingsData = value));
    const unsubLocation = location.subscribe((value) => (locationData = value));
    const unsubSunTimes = sunTimes.subscribe((value) => (sunTimesData = value));

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
    });

    onDestroy(() => {
        if (timeInterval) clearInterval(timeInterval);
        if (locationWatchId) locationService.clearWatch(locationWatchId);
        alarmService.removeListeners();
        unsubAlarms();
        unsubSettings();
        unsubLocation();
        unsubSunTimes();
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
</script>

<main>
    <div class="app-container">
        <!-- Header -->
        <header class="app-header">
            <h1 class="app-title">☀️ {t.appName}</h1>
            <div class="header-actions">
                <button
                    class="btn-icon"
                    on:click={updateLocation}
                    title="Refresh Location"
                >
                    📍
                </button>
            </div>
        </header>

        <!-- Clock Display -->
        <section class="clock-section">
            <CircularClock
                alarms={sortedAlarms}
                {currentTime}
                use24Hour={settingsData.use24HourFormat}
                useSunBased={settingsData.defaultClockMode === "sun"}
                sunriseTime={getCurrentSunrise()}
            />
        </section>

        <!-- Navigation -->
        <nav class="tab-nav">
            <button
                class="tab-btn"
                class:active={currentView === "alarms"}
                on:click={() => (currentView = "alarms")}
            >
                ⏰ {t.alarms}
            </button>
            <button
                class="tab-btn"
                class:active={currentView === "sun-times"}
                on:click={() => (currentView = "sun-times")}
            >
                ☀️ {t.upcomingSunTimes}
            </button>
            <button
                class="tab-btn"
                class:active={currentView === "settings"}
                on:click={() => (currentView = "settings")}
            >
                ⚙️ {t.settings}
            </button>
        </nav>

        <!-- Content Views -->
        <div class="content-container">
            {#if currentView === "alarms"}
                <div class="view-content">
                    <AlarmList
                        alarms={sortedAlarms}
                        {t}
                        use24Hour={settingsData.use24HourFormat}
                        onEdit={handleEditAlarm}
                        onDelete={handleDeleteAlarm}
                        onToggle={handleToggleAlarm}
                    />

                    <button class="btn-add-alarm" on:click={handleAddAlarm}>
                        ➕ {t.addAlarm}
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
                            <div class="empty-icon">📍</div>
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
                                <span
                                    >🕐 {t.normalClock} / ☀️ {t.sunBasedClock}</span
                                >
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
                </div>
            {/if}
        </div>
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
        background: linear-gradient(135deg, #0f0c29, #302b63, #24243e);
        color: #fff;
        min-height: 100vh;
        overflow-x: hidden;
    }

    main {
        min-height: 100vh;
        padding-bottom: 2rem;
    }

    .app-container {
        max-width: 800px;
        margin: 0 auto;
        padding: 1rem;
    }

    .app-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1rem 0;
        margin-bottom: 1rem;
    }

    .app-title {
        font-size: 2rem;
        font-weight: bold;
        color: #e94560;
        text-shadow: 0 2px 10px rgba(233, 69, 96, 0.3);
    }

    .header-actions {
        display: flex;
        gap: 0.5rem;
    }

    .btn-icon {
        width: 44px;
        height: 44px;
        border: none;
        background: rgba(15, 52, 96, 0.8);
        border-radius: 12px;
        cursor: pointer;
        font-size: 1.5rem;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.2s ease;
        backdrop-filter: blur(10px);
    }

    .btn-icon:hover {
        background: rgba(233, 69, 96, 0.8);
        transform: scale(1.05);
    }

    .clock-section {
        margin-bottom: 2rem;
    }

    .tab-nav {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 0.5rem;
        margin-bottom: 1.5rem;
        background: rgba(15, 52, 96, 0.3);
        padding: 0.5rem;
        border-radius: 16px;
        backdrop-filter: blur(10px);
    }

    .tab-btn {
        padding: 0.75rem 1rem;
        background: transparent;
        border: none;
        border-radius: 12px;
        color: #fff;
        cursor: pointer;
        font-size: 0.9rem;
        font-weight: 500;
        transition: all 0.3s ease;
        white-space: nowrap;
    }

    .tab-btn:hover {
        background: rgba(233, 69, 96, 0.2);
    }

    .tab-btn.active {
        background: #e94560;
        font-weight: bold;
    }

    .content-container {
        min-height: 300px;
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
        margin: 1rem auto;
        display: block;
        padding: 1rem;
        background: linear-gradient(135deg, #e94560, #d32f2f);
        border: none;
        border-radius: 16px;
        color: white;
        font-size: 1.1rem;
        font-weight: bold;
        cursor: pointer;
        transition: all 0.3s ease;
        box-shadow: 0 4px 15px rgba(233, 69, 96, 0.3);
    }

    .btn-add-alarm:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(233, 69, 96, 0.4);
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
        font-size: 4rem;
        margin-bottom: 1rem;
        opacity: 0.5;
    }

    .empty-state h3 {
        margin: 0.5rem 0;
        font-size: 1.5rem;
        color: #e94560;
    }

    .empty-state p {
        margin: 0.25rem 0 1.5rem;
        font-size: 1rem;
    }

    .btn-primary {
        padding: 0.875rem 2rem;
        background: #e94560;
        border: none;
        border-radius: 12px;
        color: white;
        font-size: 1rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;
    }

    .btn-primary:hover {
        background: #d32f2f;
        transform: translateY(-2px);
    }

    .settings-view {
        padding: 1rem;
        max-width: 600px;
        margin: 0 auto;
    }

    .settings-section {
        margin-bottom: 2rem;
        background: rgba(26, 26, 46, 0.8);
        padding: 1.5rem;
        border-radius: 16px;
        border: 2px solid rgba(15, 52, 96, 0.5);
    }

    .settings-section h3 {
        color: #e94560;
        margin-bottom: 1rem;
        font-size: 1.2rem;
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
        background: rgba(15, 52, 96, 0.5);
        border-radius: 12px;
        cursor: pointer;
        transition: all 0.2s;
    }

    .setting-label:hover {
        background: rgba(15, 52, 96, 0.7);
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
        background-color: #2a2a3e;
        transition: 0.3s;
        border-radius: 28px;
    }

    .toggle-slider:before {
        position: absolute;
        content: "";
        height: 20px;
        width: 20px;
        left: 4px;
        bottom: 4px;
        background-color: white;
        transition: 0.3s;
        border-radius: 50%;
    }

    input:checked + .toggle-slider {
        background-color: #e94560;
    }

    input:checked + .toggle-slider:before {
        transform: translateX(22px);
    }

    .language-select {
        width: 100%;
        padding: 0.75rem;
        background: rgba(15, 52, 96, 0.5);
        border: 2px solid rgba(26, 84, 144, 0.5);
        border-radius: 12px;
        color: #fff;
        font-size: 1rem;
        cursor: pointer;
        transition: all 0.2s;
    }

    .language-select:focus {
        outline: none;
        border-color: #e94560;
    }

    .language-select option {
        background: #16213e;
        color: #fff;
    }

    .btn-secondary {
        width: 100%;
        padding: 0.875rem;
        background: rgba(15, 52, 96, 0.8);
        border: 2px solid rgba(26, 84, 144, 0.5);
        border-radius: 12px;
        color: white;
        font-size: 1rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;
    }

    .btn-secondary:hover {
        background: rgba(26, 84, 144, 0.8);
        border-color: #e94560;
    }

    @media (max-width: 768px) {
        .app-container {
            padding: 0.5rem;
        }

        .app-title {
            font-size: 1.5rem;
        }

        .tab-nav {
            grid-template-columns: repeat(3, 1fr);
            gap: 0.25rem;
            padding: 0.25rem;
        }

        .tab-btn {
            padding: 0.5rem 0.25rem;
            font-size: 0.75rem;
        }

        .settings-section {
            padding: 1rem;
        }
    }
</style>
