<script lang="ts">
    import type { Alarm } from "../types";
    import type { Translation } from "../types";
    import { sunCalcService } from "../services/sunCalc";
    import Icon from "./Icon.svelte";
    import { Sun } from "@lucide/svelte";
    import { Toggle } from "./ui";

    interface Props {
        alarms: Alarm[];
        t: Translation;
        use24Hour?: boolean;
        onEdit: (alarm: Alarm) => void;
        onDelete: (id: string) => void;
        onToggle: (id: string) => void;
    }

    let { 
        alarms = [], 
        t, 
        use24Hour = true, 
        onEdit, 
        onDelete, 
        onToggle 
    }: Props = $props();

    function formatAlarmTime(alarm: Alarm): string {
        if (alarm.useSunTime) {
            const offsetAbs = Math.abs(alarm.sunOffset);
            const hours = Math.floor(offsetAbs / 60);
            const minutes = offsetAbs % 60;

            let text = "";
            if (hours > 0) text += `${hours}h `;
            if (minutes > 0) text += `${minutes}m`;

            return alarm.sunOffset >= 0 ? `+${text}` : `-${text}`;
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
                        <Toggle 
                            checked={alarm.enabled} 
                            onchange={() => handleToggle(alarm)} 
                        />
                    </div>

                    <div class="alarm-info" onclick={() => handleEdit(alarm)} role="button" tabindex="0">
                        <div class="alarm-time">
                            {#if alarm.useSunTime}
                                <Sun size={16} class="sun-icon" />
                            {/if}
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
                            onclick={() => handleEdit(alarm)}
                            title={t.editAlarm}
                            type="button"
                        >
                            <Icon name="edit" size={18} />
                        </button>
                        <button
                            class="btn-icon btn-delete"
                            onclick={() => handleDelete(alarm)}
                            title={t.deleteAlarm}
                            type="button"
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
        gap: var(--space-3);
        padding: var(--space-4);
        max-width: 600px;
        margin: 0 auto;
        width: 100%;
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
        color: var(--color-text-primary);
    }

    .empty-state p {
        margin: var(--space-1) 0;
        font-size: var(--text-base);
    }

    .alarm-item {
        background: var(--color-bg-secondary);
        border-radius: var(--radius-md);
        padding: var(--space-4);
        border: 1px solid var(--color-border);
        transition: all var(--duration-normal) var(--ease-default);
    }

    .alarm-item:hover {
        border-color: var(--color-primary);
    }

    .alarm-item.disabled {
        opacity: 0.5;
        border-color: var(--color-border);
    }

    .alarm-item.disabled:hover {
        border-color: var(--color-border);
    }

    .alarm-main {
        display: flex;
        align-items: center;
        gap: var(--space-4);
    }

    .alarm-toggle {
        flex-shrink: 0;
    }

    .alarm-info {
        flex: 1;
        cursor: pointer;
        min-width: 0;
        -webkit-tap-highlight-color: transparent;
    }

    .alarm-time {
        display: flex;
        align-items: center;
        gap: var(--space-2);
        font-size: var(--text-4xl);
        font-weight: 700;
        color: var(--color-text-primary);
        font-family: var(--font-mono);
        letter-spacing: 0.05em;
        margin-bottom: var(--space-1);
    }

    .alarm-time :global(.sun-icon) {
        color: var(--color-primary);
        flex-shrink: 0;
    }

    .alarm-label {
        font-size: var(--text-base);
        color: var(--color-text-primary);
        margin-bottom: var(--space-2);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    .alarm-meta {
        display: flex;
        flex-wrap: wrap;
        gap: var(--space-2);
        font-size: var(--text-sm);
        color: var(--color-text-secondary);
    }

    .alarm-repeat {
        color: var(--color-text-secondary);
    }

    .alarm-badge {
        display: inline-flex;
        align-items: center;
        gap: var(--space-1);
        padding: 0.15rem var(--space-2);
        background: var(--color-primary-light);
        border-radius: var(--radius-sm);
        font-size: var(--text-xs);
        color: var(--color-primary);
    }

    .alarm-actions {
        display: flex;
        gap: var(--space-2);
        flex-shrink: 0;
    }

    .btn-icon {
        width: 40px;
        height: 40px;
        border: none;
        background: var(--color-bg-elevated);
        border-radius: var(--radius-md);
        cursor: pointer;
        font-size: 1.2rem;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all var(--duration-normal) var(--ease-default);
        -webkit-tap-highlight-color: transparent;
        color: var(--color-text-primary);
    }

    .btn-icon:hover {
        transform: scale(1.1);
    }

    .btn-icon:active {
        transform: scale(0.95);
    }

    .btn-edit:hover {
        background: var(--color-bg-tertiary);
        color: var(--color-primary);
    }

    .btn-delete {
        color: var(--color-error);
    }

    .btn-delete:hover {
        background: var(--color-error);
        color: #fff;
    }

    @media (max-width: 768px) {
        .alarm-list {
            padding: var(--space-2);
        }

        .alarm-item {
            padding: var(--space-3);
        }

        .alarm-time {
            font-size: var(--text-2xl);
        }

        .alarm-main {
            gap: var(--space-3);
        }

        .btn-icon {
            width: 36px;
            height: 36px;
            font-size: 1rem;
        }
    }
</style>
