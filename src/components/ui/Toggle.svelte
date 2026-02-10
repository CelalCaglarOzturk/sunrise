<script lang="ts">
  interface Props {
    checked?: boolean;
    disabled?: boolean;
    id?: string;
    onchange?: (checked: boolean) => void;
  }

  let {
    checked = $bindable(false),
    disabled = false,
    id = undefined,
    onchange,
  }: Props = $props();

  function handleChange(e: Event) {
    const target = e.target as HTMLInputElement;
    checked = target.checked;
    onchange?.(checked);
  }
</script>

<label class="toggle-wrapper" for={id}>
  <input
    type="checkbox"
    {id}
    class="toggle-input"
    bind:checked
    {disabled}
    onchange={handleChange}
  />
  <span class="toggle-slider" aria-hidden="true"></span>
</label>

<style>
  .toggle-wrapper {
    position: relative;
    display: inline-block;
    width: 50px;
    height: 28px;
    cursor: pointer;
  }

  .toggle-input {
    position: absolute;
    opacity: 0;
    width: 0;
    height: 0;
  }

  .toggle-slider {
    position: absolute;
    inset: 0;
    background-color: var(--color-bg-elevated);
    border-radius: var(--radius-full);
    transition: background-color var(--duration-normal);
  }

  .toggle-slider::before {
    position: absolute;
    content: "";
    height: 20px;
    width: 20px;
    left: 4px;
    bottom: 4px;
    background-color: white;
    border-radius: 50%;
    transition: transform var(--duration-normal);
  }

  .toggle-input:checked + .toggle-slider {
    background-color: var(--color-primary);
  }

  .toggle-input:checked + .toggle-slider::before {
    transform: translateX(22px);
  }

  .toggle-input:focus-visible + .toggle-slider {
    outline: 2px solid var(--color-primary);
    outline-offset: 2px;
  }

  .toggle-input:disabled + .toggle-slider {
    opacity: 0.5;
    cursor: not-allowed;
  }
</style>
