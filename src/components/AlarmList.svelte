<script lang="ts">
    import type { Alarm } from "../types";
    import type { Translation } from "../types";
    import { sunCalcService } from "../services/sunCalc";
    import Icon from "./Icon.svelte";

    export let alarms: Alarm[] = [];
    export let t: Translation;
    export let use24Hour: boolean = true;
    export let onEdit: (alarm: Alarm) => void;
    export let onDelete: (id: string) => void;
    export let onToggle: (id: string) => void;

    function formatAlarmTime(alarm: Alarm): string {
        if (alarm.useSunTime) {
            const offsetAbs = Math.abs(alarm.sunOffset);
            const hours = Math.floor(offsetAbs / 60);
            const minutes = offsetAbs % 60;

            let text = "";
            if (hours > 0) text += `${hours}h `;
            if (minutes > 0) text += `${minutes}m`;

            return alarm.sunOffset >= 0 ? `☀️ +${text}` : `☀️ -${text}`;
        }

        if (use24Hour) {
            return alarm.time;
        }

        const [hours, minutes] = alarm.time.split(":").map(Number);
        const displayHours = hours % 12 || 12;
        const period = hours >= 12 ? "PM" : "AM";
        return `${displayHours}:${String(minutes).padStart(2, "0")} ${period}`;
    }

    function formatRepeatDays(alarm: Alarm): string {
        if (alarm.repeatDays.length === 0) {
            return t.once;
        }

        if (alarm.repeatDays.length === 7) {
            return t.daily;
        }

        const weekdays = [1, 2, 3, 4, 5];
        const weekends = [0, 6];

        if (
            alarm.repeatDays.length === 5 &&
            weekdays.every((d) => alarm.repeatDays.includes(d))
        ) {
            return t.weekdays;
        }

        if (
            alarm.repeatDays.length === 2 &&
            weekends.every((d) => alarm.repeatDays.includes(d))
        ) {
            return t.weekends;
        }

        const dayNames = [
            t.sunday,
            t.monday,
            t.tuesday,
            t.wednesday,
            t.thursday,
            t.friday,
            t.saturday,
        ];

        return alarm.repeatDays
            .sort()
            .map((day) => dayNames[day])
            .join(", ");
    }

    function handleToggle(alarm: Alarm) {
        onToggle(alarm.id);
    }

    function handleEdit(alarm: Alarm) {
        onEdit(alarm);
    }

    function handleDelete(alarm: Alarm) {
        if (confirm(`${t.confirmDeleteMessage}`)) {
            onDelete(alarm.id);
        }
    }
</script>

<div class="alarm-list">
    {#if alarms.length === 0}
        <div class="empty-state">
            <div class="empty-icon">
                <Icon name="bell" size={64} color="#888" />
            </div>
            <h3>{t.noAlarmsYet}</h3>
            <p>{t.createFirstAlarm}</p>
        </div>
    {:else}
        {#each alarms as alarm (alarm.id)}
            <div class="alarm-item" class:disabled={!alarm.enabled}>
                <div class="alarm-main">
                    <div class="alarm-toggle">
                        <label class="toggle-switch">
                            <input
                                type="checkbox"
                                checked={alarm.enabled}
                                on:change={() => handleToggle(alarm)}
                            />
                            <span class="toggle-slider"></span>
                        </label>
                    </div>

                    <div class="alarm-info" on:click={() => handleEdit(alarm)}>
                        <div class="alarm-time">
                            {formatAlarmTime(alarm)}
                        </div>

                        {#if alarm.label}
                            <div class="alarm-label">
                                {alarm.label}
                            </div>
                        {/if}

                        <div class="alarm-meta">
                            <span class="alarm-repeat">
                                {formatRepeatDays(alarm)}
                            </span>

                            {#if alarm.deleteAfterUse}
                                <span class="alarm-badge">
                                    <Icon name="trash" size={12} />
                                    {t.deleteAfterUse}
                                </span>
                            {/if}

                            {#if alarm.vibrate}
                                <span class="alarm-badge">
                                    <Icon name="vibrate" size={12} />
                                </span>
                            {/if}

                            {#if alarm.gradualVolume}
                                <span class="alarm-badge">
                                    <Icon name="trending-up" size={12} />
                                </span>
                            {/if}
                        </div>
                    </div>

                    <div class="alarm-actions">
                        <button
                            class="btn-icon btn-edit"
                            on:click={() => handleEdit(alarm)}
                            title={t.editAlarm}
                        >
                            <Icon name="edit" size={18} />
                        </button>
                        <button
                            class="btn-icon btn-delete"
                            on:click={() => handleDelete(alarm)}
                            title={t.deleteAlarm}
                        >
                            <Icon name="trash" size={18} />
                        </button>
                    </div>
                </div>
            </div>
        {/each}
    {/if}
</div>

<style>
    .alarm-list {
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
        padding: 1rem;
        max-width: 600px;
        margin: 0 auto;
        width: 100%;
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
        color: #fff;
    }

    .empty-state p {
        margin: 0.25rem 0;
        font-size: 1rem;
    }

    .alarm-item {
        background: #1a1a1a;
        border-radius: 15px;
        padding: 1rem;
        border: 1px solid #333;
        transition: all 0.2s ease;
    }

    .alarm-item:hover {
        border-color: #ff9500;
    }

    .alarm-item.disabled {
        opacity: 0.5;
        border-color: #333;
    }

    .alarm-item.disabled:hover {
        border-color: #333;
    }

    .alarm-main {
        display: flex;
        align-items: center;
        gap: 1rem;
    }

    .alarm-toggle {
        flex-shrink: 0;
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
        transition: 0.3s;
        border-radius: 50%;
    }

    input:checked + .toggle-slider {
        background-color: #ff9500;
    }

    input:checked + .toggle-slider:before {
        transform: translateX(22px);
    }

    .alarm-info {
        flex: 1;
        cursor: pointer;
        min-width: 0;
    }

    .alarm-time {
        font-size: 2rem;
        font-weight: 700;
        color: #fff;
        font-family: "SF Mono", "Monaco", "Inconsolata", monospace;
        margin-bottom: 0.25rem;
    }

    .alarm-label {
        font-size: 1rem;
        color: #fff;
        margin-bottom: 0.5rem;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    .alarm-meta {
        display: flex;
        flex-wrap: wrap;
        gap: 0.5rem;
        font-size: 0.85rem;
        color: #888;
    }

    .alarm-repeat {
        color: #888;
    }

    .alarm-badge {
        display: inline-flex;
        align-items: center;
        gap: 0.25rem;
        padding: 0.15rem 0.5rem;
        background: rgba(255, 149, 0, 0.1);
        border-radius: 8px;
        font-size: 0.75rem;
        color: #ff9500;
    }

    .alarm-actions {
        display: flex;
        gap: 0.5rem;
        flex-shrink: 0;
    }

    .btn-icon {
        width: 40px;
        height: 40px;
        border: none;
        background: #333;
        border-radius: 10px;
        cursor: pointer;
        font-size: 1.2rem;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.2s ease;
    }

    .btn-icon:hover {
        transform: scale(1.1);
    }

    .btn-icon:active {
        transform: scale(0.95);
    }

    .btn-edit:hover {
        background: #444;
    }

    .btn-delete:hover {
        background: #ff3b30;
    }

    @media (max-width: 768px) {
        .alarm-list {
            padding: 0.5rem;
        }

        .alarm-item {
            padding: 0.75rem;
        }

        .alarm-time {
            font-size: 1.5rem;
        }

        .alarm-main {
            gap: 0.75rem;
        }

        .btn-icon {
            width: 36px;
            height: 36px;
            font-size: 1rem;
        }
    }
</style>
