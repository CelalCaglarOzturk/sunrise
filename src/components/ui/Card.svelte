<script lang="ts">
  interface Props {
    padding?: "sm" | "md" | "lg";
    hoverable?: boolean;
    clickable?: boolean;
    selected?: boolean;
    disabled?: boolean;
    children?: import("svelte").Snippet;
    onclick?: (e: MouseEvent) => void;
  }

  let {
    padding = "md",
    hoverable = false,
    clickable = false,
    selected = false,
    disabled = false,
    children,
    onclick,
  }: Props = $props();

  const paddingClasses = {
    sm: "card-sm",
    md: "card-md",
    lg: "card-lg",
  };
</script>

{#if clickable}
  <button
    type="button"
    class="card {paddingClasses[padding]}"
    class:hoverable
    class:selected
    class:disabled
    {disabled}
    onclick={onclick}
  >
    {@render children?.()}
  </button>
{:else}
  <div
    class="card {paddingClasses[padding]}"
    class:hoverable
    class:selected
    class:disabled
  >
    {@render children?.()}
  </div>
{/if}

<style>
  .card {
    background: var(--color-bg-secondary);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    transition: all var(--duration-normal) var(--ease-default);
  }

  /* Padding variants */
  .card-sm {
    padding: var(--space-3);
  }

  .card-md {
    padding: var(--space-4);
  }

  .card-lg {
    padding: var(--space-6);
  }

  /* Hoverable state */
  .hoverable:hover:not(.disabled) {
    border-color: var(--color-border-hover);
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }

  /* Selected state */
  .selected {
    border-color: var(--color-primary);
    background: var(--color-primary-light);
  }

  /* Disabled state */
  .disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  /* Clickable card as button */
  button.card {
    width: 100%;
    text-align: left;
    cursor: pointer;
    -webkit-tap-highlight-color: transparent;
  }

  button.card:active:not(.disabled) {
    transform: scale(0.99);
  }
</style>
