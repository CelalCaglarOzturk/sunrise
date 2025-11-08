<script lang="ts">
  import type { SunTimes, Translation } from '../types';
  import { sunCalcService } from '../services/sunCalc';

  export let sunTimes: SunTimes[] = [];
  export let t: Translation;
  export let use24Hour: boolean = true;

  function formatTime(date: Date): string {
    return sunCalcService.formatTime(date, use24Hour);
  }

  function getDayLabel(date: Date): string {
    if (sunCalcService.isToday(date)) {
      return t.today;
    }
    if (sunCalcService.isTomorrow(date)) {
      return t.tomorrow;
    }
    return sunCalcService.getDayName(date);
  }

  function getFormattedDate(date: Date): string {
    return sunCalcService.getFormattedDate(date);
  }
</script>

<div class="sun-times-display">
  <h3 class="sun-times-title">
    ☀️ {t.upcomingSunTimes}
  </h3>

  <div class="sun-times-list">
    {#each sunTimes as sunTime, index}
      <div class="sun-time-item" class:today={index === 0}>
        <div class="day-info">
          <div class="day-label">
            {getDayLabel(sunTime.date)}
          </div>
          <div class="date-label">
            {getFormattedDate(sunTime.date)}
          </div>
        </div>

        <div class="times">
          <div class="time-block sunrise">
            <div class="time-icon">🌅</div>
            <div class="time-info">
              <div class="time-label">{t.sunrise}</div>
              <div class="time-value">{formatTime(sunTime.sunrise)}</div>
            </div>
          </div>

          <div class="time-block sunset">
            <div class="time-icon">🌇</div>
            <div class="time-info">
              <div class="time-label">{t.sunset}</div>
              <div class="time-value">{formatTime(sunTime.sunset)}</div>
            </div>
          </div>
        </div>
      </div>
    {/each}
  </div>
</div>

<style>
  .sun-times-display {
    padding: 1rem;
    max-width: 600px;
    margin: 0 auto;
    width: 100%;
  }

  .sun-times-title {
    font-size: 1.5rem;
    font-weight: bold;
    color: var(--text-primary, #ffd700);
    margin-bottom: 1rem;
    text-align: center;
  }

  .sun-times-list {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  .sun-time-item {
    background: var(--sun-item-bg, #1a1a2e);
    border-radius: 16px;
    padding: 1rem;
    border: 2px solid var(--sun-item-border, #0f3460);
    transition: all 0.3s ease;
  }

  .sun-time-item:hover {
    border-color: var(--sun-item-hover, #ffd700);
    box-shadow: 0 4px 12px rgba(255, 215, 0, 0.2);
  }

  .sun-time-item.today {
    border-color: var(--today-border, #ffd700);
    background: var(--today-bg, rgba(255, 215, 0, 0.05));
  }

  .day-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 0.75rem;
    padding-bottom: 0.75rem;
    border-bottom: 1px solid var(--divider-color, #0f3460);
  }

  .day-label {
    font-size: 1.1rem;
    font-weight: bold;
    color: var(--text-primary, #fff);
  }

  .date-label {
    font-size: 0.9rem;
    color: var(--text-secondary, #888);
  }

  .times {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
  }

  .time-block {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.5rem;
    background: var(--time-block-bg, #0f3460);
    border-radius: 12px;
  }

  .time-block.sunrise {
    border: 1px solid rgba(255, 215, 0, 0.3);
  }

  .time-block.sunset {
    border: 1px solid rgba(255, 140, 0, 0.3);
  }

  .time-icon {
    font-size: 2rem;
    flex-shrink: 0;
  }

  .time-info {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }

  .time-label {
    font-size: 0.75rem;
    color: var(--text-secondary, #888);
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .time-value {
    font-size: 1.25rem;
    font-weight: bold;
    color: var(--time-color, #ffd700);
    font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  }

  @media (max-width: 768px) {
    .sun-times-display {
      padding: 0.5rem;
    }

    .sun-times-title {
      font-size: 1.25rem;
    }

    .times {
      gap: 0.5rem;
    }

    .time-block {
      flex-direction: column;
      text-align: center;
      gap: 0.5rem;
      padding: 0.75rem 0.5rem;
    }

    .time-icon {
      font-size: 1.5rem;
    }

    .time-value {
      font-size: 1rem;
    }
  }
</style>
