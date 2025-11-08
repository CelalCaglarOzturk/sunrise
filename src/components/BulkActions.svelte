<script lang="ts">
    import type { Translation, Alarm } from "../types";
    import Icon from "./Icon.svelte";

    export let visible: boolean = false;
    export let alarms: Alarm[] = [];
    export let t: Translation;
    export let onDisableAll: () => void;
    export let onEnableAll: () => void;
    export let onDeleteAll: () => void;

    $: allDisabled =
        alarms.length > 0 && alarms.every((alarm) => !alarm.enabled);

    function handleDisableAll() {
        if (confirm(t.disableAllConfirm)) {
            onDisableAll();
        }
    }

    function handleEnableAll() {
        onEnableAll();
    }

    function handleDeleteAll() {
        if (confirm(t.deleteAllConfirm)) {
            onDeleteAll();
        }
    }
</script>

{#if visible}
    <div class="bulk-actions">
        {#if allDisabled}
            <button class="bulk-btn enable-btn" on:click={handleEnableAll}>
                <Icon name="chevron-down" size={20} />
                {t.enableAll || "Enable All"}
            </button>
        {:else}
            <button class="bulk-btn disable-btn" on:click={handleDisableAll}>
                <Icon name="chevron-up" size={20} />
                {t.disableAll}
            </button>
        {/if}
        <button class="bulk-btn delete-btn" on:click={handleDeleteAll}>
            <Icon name="trash" size={20} />
            {t.deleteAll}
        </button>
    </div>
{/if}

<style>
    .bulk-actions {
        display: flex;
        gap: 0.5rem;
        padding: 0 1rem 1rem;
        animation: slideDown 0.3s ease;
    }

    @keyframes slideDown {
        from {
            opacity: 0;
            transform: translateY(-10px);
        }
        to {
            opacity: 1;
            transform: translateY(0);
        }
    }

    .bulk-btn {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 0.5rem;
        padding: 0.75rem;
        border: none;
        border-radius: 10px;
        font-size: 0.9rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s ease;
    }

    .disable-btn {
        background: #333;
        color: #fff;
    }

    .disable-btn:hover {
        background: #444;
        transform: translateY(-2px);
    }

    .enable-btn {
        background: #ff9500;
        color: #fff;
    }

    .enable-btn:hover {
        background: #e68600;
        transform: translateY(-2px);
    }

    .delete-btn {
        background: #d97706;
        color: #fff;
    }

    .delete-btn:hover {
        background: #b45309;
        transform: translateY(-2px);
    }

    .bulk-btn:active {
        transform: translateY(0);
    }

    @media (max-width: 768px) {
        .bulk-actions {
            padding: 0 0.5rem 0.75rem;
        }

        .bulk-btn {
            padding: 0.6rem;
            font-size: 0.85rem;
        }
    }
</style>
