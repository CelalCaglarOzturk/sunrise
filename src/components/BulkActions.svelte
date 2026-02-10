<script lang="ts">
    import type { Translation, Alarm } from "../types";
    import Icon from "./Icon.svelte";
    import { Button } from "./ui";

    interface Props {
        visible: boolean;
        alarms: Alarm[];
        t: Translation;
        onDisableAll: () => void;
        onEnableAll: () => void;
        onDeleteAll: () => void;
    }

    let { visible, alarms, t, onDisableAll, onEnableAll, onDeleteAll }: Props = $props();

    let allDisabled = $derived(alarms.length > 0 && alarms.every((alarm) => !alarm.enabled));

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
            <Button variant="primary" onclick={handleEnableAll}>
                <Icon name="chevron-down" size={20} />
                {t.enableAll || "Enable All"}
            </Button>
        {:else}
            <Button variant="secondary" onclick={handleDisableAll}>
                <Icon name="chevron-up" size={20} />
                {t.disableAll}
            </Button>
        {/if}
        <Button variant="danger" onclick={handleDeleteAll}>
            <Icon name="trash" size={20} />
            {t.deleteAll}
        </Button>
    </div>
{/if}

<style>
    .bulk-actions {
        display: flex;
        gap: var(--space-2);
        padding: 0 var(--space-4) var(--space-4);
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

    @media (max-width: 768px) {
        .bulk-actions {
            padding: 0 var(--space-2) var(--space-3);
        }
    }
</style>
