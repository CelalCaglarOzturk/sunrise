<script lang="ts">
  interface Props {
    variant?: "primary" | "secondary" | "ghost" | "danger";
    size?: "sm" | "md" | "lg";
    type?: "button" | "submit" | "reset";
    disabled?: boolean;
    loading?: boolean;
    fullWidth?: boolean;
    ariaLabel?: string;
    children?: import("svelte").Snippet;
    onclick?: (e: MouseEvent) => void;
  }

  let {
    variant = "primary",
    size = "md",
    type = "button",
    disabled = false,
    loading = false,
    fullWidth = false,
    ariaLabel = undefined,
    children,
    onclick,
  }: Props = $props();

  const variantClasses = {
    primary: "btn-primary",
    secondary: "btn-secondary",
    ghost: "btn-ghost",
    danger: "btn-danger",
  };

  const sizeClasses = {
    sm: "btn-sm",
    md: "btn-md",
    lg: "btn-lg",
  };
</script>

<button
  {type}
  class="btn {variantClasses[variant]} {sizeClasses[size]}"
  class:full-width={fullWidth}
  class:loading
  {disabled}
  aria-label={ariaLabel}
  aria-disabled={disabled}
  aria-busy={loading}
  onclick={onclick}
>
  {#if loading}
    <span class="spinner" aria-hidden="true"></span>
  {/if}
  
  {#if children}
    <span class="btn-content">{@render children()}</span>
  {/if}
</button>

<style>
  .btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: var(--space-2);
    font-weight: 600;
    border: none;
    border-radius: var(--radius-md);
    transition: all var(--duration-normal) var(--ease-default);
    -webkit-tap-highlight-color: transparent;
    cursor: pointer;
    position: relative;
    overflow: hidden;
  }

  .btn:active:not(:disabled) {
    transform: scale(0.98);
  }

  .btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  /* Sizes */
  .btn-sm {
    padding: var(--space-2) var(--space-3);
    font-size: var(--text-sm);
    min-height: 36px;
  }

  .btn-md {
    padding: var(--space-3) var(--space-4);
    font-size: var(--text-base);
    min-height: 44px;
  }

  .btn-lg {
    padding: var(--space-4) var(--space-5);
    font-size: var(--text-lg);
    min-height: 52px;
  }

  /* Variants */
  .btn-primary {
    background: var(--color-primary);
    color: #000;
  }

  .btn-primary:hover:not(:disabled) {
    background: var(--color-primary-hover);
  }

  .btn-secondary {
    background: var(--color-bg-secondary);
    color: var(--color-text-primary);
    border: 1px solid var(--color-border);
  }

  .btn-secondary:hover:not(:disabled) {
    border-color: var(--color-primary);
    background: var(--color-bg-tertiary);
  }

  .btn-ghost {
    background: transparent;
    color: var(--color-text-secondary);
  }

  .btn-ghost:hover:not(:disabled) {
    color: var(--color-primary);
    background: var(--color-primary-light);
  }

  .btn-danger {
    background: var(--color-error);
    color: #fff;
  }

  .btn-danger:hover:not(:disabled) {
    background: var(--color-bg-tertiary);
  }

  /* Full width */
  .full-width {
    width: 100%;
  }

  /* Loading state */
  .loading {
    cursor: wait;
  }

  .spinner {
    width: 20px;
    height: 20px;
    border: 2px solid transparent;
    border-top-color: currentColor;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }

  .btn-content {
    flex: 1;
  }
</style>
