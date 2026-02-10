<script lang="ts">
  import { X } from "@lucide/svelte";

  interface Props {
    open?: boolean;
    title?: string;
    showClose?: boolean;
    children?: import("svelte").Snippet;
    footer?: import("svelte").Snippet;
    onclose?: () => void;
  }

  let {
    open = $bindable(false),
    title = "",
    showClose = true,
    children,
    footer,
    onclose,
  }: Props = $props();

  function handleBackdropClick(e: MouseEvent) {
    if (e.target === e.currentTarget) {
      closeModal();
    }
  }

  function closeModal() {
    open = false;
    onclose?.();
  }

  function handleKeydown(e: KeyboardEvent) {
    if (e.key === "Escape") {
      closeModal();
    }
  }
</script>

{#if open}
  <!-- svelte-ignore a11y_click_events_have_key_events -->
  <div
    class="modal-backdrop"
    onclick={handleBackdropClick}
    role="dialog"
    aria-modal="true"
    aria-labelledby={title ? "modal-title" : undefined}
  >
    <div class="modal-content">
      {#if title || showClose}
        <div class="modal-header">
          {#if title}
            <h2 id="modal-title" class="modal-title">{title}</h2>
          {:else}
            <div></div>
          {/if}
          
          {#if showClose}
            <button
              type="button"
              class="close-btn"
              onclick={closeModal}
              aria-label="Close modal"
            >
              <X size={20} />
            </button>
          {/if}
        </div>
      {/if}
      
      <div class="modal-body">
        {@render children?.()}
      </div>
      
      {#if footer}
        <div class="modal-footer">
          {@render footer()}
        </div>
      {/if}
    </div>
  </div>
{/if}

<style>
  .modal-backdrop {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.8);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: var(--z-modal);
    padding: var(--space-4);
    animation: fadeIn var(--duration-normal);
  }

  @keyframes fadeIn {
    from {
      opacity: 0;
    }
    to {
      opacity: 1;
    }
  }

  .modal-content {
    background: var(--color-bg-secondary);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    width: 100%;
    max-width: 400px;
    max-height: 90vh;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    animation: scaleIn var(--duration-normal);
  }

  @keyframes scaleIn {
    from {
      opacity: 0;
      transform: scale(0.95);
    }
    to {
      opacity: 1;
      transform: scale(1);
    }
  }

  .modal-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--space-4);
    border-bottom: 1px solid var(--color-border);
  }

  .modal-title {
    font-size: var(--text-xl);
    font-weight: 600;
    color: var(--color-primary);
    margin: 0;
  }

  .close-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    background: transparent;
    border: none;
    border-radius: var(--radius-md);
    color: var(--color-text-secondary);
    cursor: pointer;
    transition: all var(--duration-normal);
    -webkit-tap-highlight-color: transparent;
  }

  .close-btn:hover {
    color: var(--color-text-primary);
    background: var(--color-bg-tertiary);
  }

  .modal-body {
    padding: var(--space-4);
    overflow-y: auto;
    flex: 1;
  }

  .modal-footer {
    display: flex;
    gap: var(--space-3);
    padding: var(--space-4);
    border-top: 1px solid var(--color-border);
  }

  .modal-footer :global(> *) {
    flex: 1;
  }
</style>
