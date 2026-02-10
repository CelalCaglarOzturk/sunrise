<script lang="ts">
  import { onMount, onDestroy } from "svelte";
  import { Play, Pause, RotateCcw, Settings, CheckCircle, Coffee, Brain, SkipForward } from "@lucide/svelte";
  import Icon from "./Icon.svelte";
  import type { Pomodoro, PomodoroSession } from "../types";

  export let t: any;

  let pomodoro: Pomodoro = {
    isRunning: false,
    isPaused: false,
    remainingTime: 1500, // 25 minutes in seconds
    currentPhase: "work",
    workDuration: 1500, // 25 minutes
    shortBreakDuration: 300, // 5 minutes
    longBreakDuration: 900, // 15 minutes
    workSessionsBeforeLongBreak: 4,
    currentSessionCount: 0,
    sessions: [],
    autoStartBreaks: false,
    autoStartWork: false,
  };

  let intervalId: number | null = null;
  let showSettings = false;
  let audioContext: AudioContext | null = null;

  // Temporary settings for modal
  let tempWorkDuration = 25;
  let tempShortBreakDuration = 5;
  let tempLongBreakDuration = 15;
  let tempSessionsBeforeLongBreak = 4;
  let tempAutoStartBreaks = false;
  let tempAutoStartWork = false;

  onMount(() => {
    // Request notification permission
    if ("Notification" in window && Notification.permission === "default") {
      Notification.requestPermission();
    }
  });

  onDestroy(() => {
    if (intervalId) {
      clearInterval(intervalId);
    }
  });

  function formatTime(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
  }

  function getCurrentDuration(): number {
    switch (pomodoro.currentPhase) {
      case "work":
        return pomodoro.workDuration;
      case "shortBreak":
        return pomodoro.shortBreakDuration;
      case "longBreak":
        return pomodoro.longBreakDuration;
      default:
        return pomodoro.workDuration;
    }
  }

  function startTimer() {
    if (pomodoro.isPaused) {
      // Resume
      pomodoro.isPaused = false;
      pomodoro.isRunning = true;
    } else {
      // Start new session
      pomodoro.isRunning = true;
      pomodoro.isPaused = false;
      
      // Create session record for work phase
      if (pomodoro.currentPhase === "work") {
        const session: PomodoroSession = {
          id: Date.now().toString(),
          startTime: Date.now(),
          endTime: 0,
          duration: 0,
          completed: false,
          type: "work",
        };
        pomodoro.sessions = [session, ...pomodoro.sessions];
      }
    }

    intervalId = window.setInterval(() => {
      if (pomodoro.remainingTime > 0) {
        pomodoro.remainingTime--;
      } else {
        completePhase();
      }
    }, 1000);
  }

  function pauseTimer() {
    if (intervalId) {
      clearInterval(intervalId);
      intervalId = null;
    }
    pomodoro.isPaused = true;
    pomodoro.isRunning = false;
  }

  function resetTimer() {
    if (intervalId) {
      clearInterval(intervalId);
      intervalId = null;
    }
    pomodoro.isRunning = false;
    pomodoro.isPaused = false;
    pomodoro.remainingTime = getCurrentDuration();
    
    // Remove incomplete current session
    if (pomodoro.sessions.length > 0 && !pomodoro.sessions[0].completed) {
      pomodoro.sessions = pomodoro.sessions.slice(1);
    }
  }

  function skipPhase() {
    if (intervalId) {
      clearInterval(intervalId);
      intervalId = null;
    }
    completePhase(true);
  }

  function completePhase(skipped = false) {
    if (intervalId) {
      clearInterval(intervalId);
      intervalId = null;
    }

    // Mark current session as completed if it was a work session
    if (pomodoro.currentPhase === "work" && pomodoro.sessions.length > 0) {
      const currentSession = pomodoro.sessions[0];
      currentSession.endTime = Date.now();
      currentSession.duration = Math.floor((currentSession.endTime - currentSession.startTime) / 1000);
      currentSession.completed = !skipped;
      pomodoro.sessions = [...pomodoro.sessions];
      
      if (!skipped) {
        pomodoro.currentSessionCount++;
      }
    }

    // Play notification sound
    playNotificationSound();

    // Show notification
    showNotification();

    // Switch phase
    if (pomodoro.currentPhase === "work") {
      // Work completed, decide which break to take
      if (pomodoro.currentSessionCount % pomodoro.workSessionsBeforeLongBreak === 0) {
        pomodoro.currentPhase = "longBreak";
        pomodoro.remainingTime = pomodoro.longBreakDuration;
      } else {
        pomodoro.currentPhase = "shortBreak";
        pomodoro.remainingTime = pomodoro.shortBreakDuration;
      }
      
      // Auto-start break if enabled
      if (pomodoro.autoStartBreaks) {
        setTimeout(() => startTimer(), 1000);
      } else {
        pomodoro.isRunning = false;
        pomodoro.isPaused = false;
      }
    } else {
      // Break completed, go back to work
      pomodoro.currentPhase = "work";
      pomodoro.remainingTime = pomodoro.workDuration;
      
      // Auto-start work if enabled
      if (pomodoro.autoStartWork) {
        setTimeout(() => startTimer(), 1000);
      } else {
        pomodoro.isRunning = false;
        pomodoro.isPaused = false;
      }
    }
  }

  function playNotificationSound() {
    try {
      if (!audioContext) {
        audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
      }
      
      const oscillator = audioContext.createOscillator();
      const gainNode = audioContext.createGain();
      
      oscillator.connect(gainNode);
      gainNode.connect(audioContext.destination);
      
      oscillator.frequency.value = pomodoro.currentPhase === "work" ? 800 : 600;
      oscillator.type = "sine";
      
      gainNode.gain.setValueAtTime(0.3, audioContext.currentTime);
      gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.5);
      
      oscillator.start(audioContext.currentTime);
      oscillator.stop(audioContext.currentTime + 0.5);
    } catch (error) {
      console.error("Error playing notification sound:", error);
    }
  }

  function showNotification() {
    if ("Notification" in window && Notification.permission === "granted") {
      const title = pomodoro.currentPhase === "work" ? t.pomodoroComplete : t.break;
      const body = pomodoro.currentPhase === "work" 
        ? `${t.work} ${t.completed}!` 
        : `${t.break} ${t.completed}!`;
      
      new Notification(title, {
        body,
        icon: "/icon.png",
      });
    }
  }

  function openSettings() {
    tempWorkDuration = Math.floor(pomodoro.workDuration / 60);
    tempShortBreakDuration = Math.floor(pomodoro.shortBreakDuration / 60);
    tempLongBreakDuration = Math.floor(pomodoro.longBreakDuration / 60);
    tempSessionsBeforeLongBreak = pomodoro.workSessionsBeforeLongBreak;
    tempAutoStartBreaks = pomodoro.autoStartBreaks;
    tempAutoStartWork = pomodoro.autoStartWork;
    showSettings = true;
  }

  function saveSettings() {
    pomodoro.workDuration = tempWorkDuration * 60;
    pomodoro.shortBreakDuration = tempShortBreakDuration * 60;
    pomodoro.longBreakDuration = tempLongBreakDuration * 60;
    pomodoro.workSessionsBeforeLongBreak = tempSessionsBeforeLongBreak;
    pomodoro.autoStartBreaks = tempAutoStartBreaks;
    pomodoro.autoStartWork = tempAutoStartWork;
    
    // Update remaining time if not running
    if (!pomodoro.isRunning && !pomodoro.isPaused) {
      pomodoro.remainingTime = getCurrentDuration();
    }
    
    showSettings = false;
  }

  function closeSettings() {
    showSettings = false;
  }

  function getPhaseIcon() {
    switch (pomodoro.currentPhase) {
      case "work":
        return Brain;
      case "shortBreak":
      case "longBreak":
        return Coffee;
      default:
        return Brain;
    }
  }

  function getPhaseLabel() {
    switch (pomodoro.currentPhase) {
      case "work":
        return t.work;
      case "shortBreak":
        return t.shortBreak;
      case "longBreak":
        return t.longBreak;
      default:
        return t.work;
    }
  }

  function getProgress(): number {
    const total = getCurrentDuration();
    return ((total - pomodoro.remainingTime) / total) * 100;
  }

  $: PhaseIcon = getPhaseIcon();
  $: phaseLabel = getPhaseLabel();
  $: progress = getProgress();
  $: completedSessions = pomodoro.sessions.filter(s => s.completed && s.type === "work").length;
</script>

<div class="pomodoro-container">
  <!-- Main Timer Display -->
  <div class="timer-section">
    <div class="phase-indicator">
      <svelte:component this={PhaseIcon} size={32} />
      <span>{phaseLabel}</span>
    </div>
    
    <div class="timer-display">
      {formatTime(pomodoro.remainingTime)}
    </div>
    
    <!-- Progress Ring -->
    <div class="progress-ring">
      <svg viewBox="0 0 100 100">
        <circle
          class="progress-ring-bg"
          cx="50"
          cy="50"
          r="45"
        />
        <circle
          class="progress-ring-fill"
          cx="50"
          cy="50"
          r="45"
          style="stroke-dasharray: {283 * (progress / 100)} 283"
        />
      </svg>
    </div>
    
    <!-- Session Counter -->
    <div class="session-counter">
      <span>{completedSessions} {t.sessionsCompleted}</span>
    </div>
  </div>

  <!-- Controls -->
  <div class="controls">
    {#if !pomodoro.isRunning}
      <button class="btn-control btn-primary" on:click={startTimer}>
        <Play size={24} />
        {t.start}
      </button>
    {:else}
      <button class="btn-control btn-secondary" on:click={pauseTimer}>
        <Pause size={24} />
        {t.pause}
      </button>
    {/if}
    
    <button class="btn-control btn-secondary" on:click={resetTimer}>
      <RotateCcw size={24} />
      {t.reset}
    </button>
    
    <button class="btn-control btn-secondary" on:click={skipPhase}>
      <SkipForward size={24} />
    </button>
    
    <button class="btn-control btn-settings" on:click={openSettings}>
      <Settings size={24} />
    </button>
  </div>

  <!-- Sessions History -->
  {#if pomodoro.sessions.length > 0}
    <div class="sessions-section">
      <h3>{t.sessionsCompleted}</h3>
      <div class="sessions-list">
        {#each pomodoro.sessions.slice(0, 10) as session}
          <div class="session-item" class:completed={session.completed}>
            <span class="session-type">
              {#if session.type === "work"}
                <Brain size={14} />
              {:else}
                <Coffee size={14} />
              {/if}
              {session.type === "work" ? t.work : session.type === "shortBreak" ? t.shortBreak : t.longBreak}
            </span>
            <span class="session-time">
              {formatTime(session.duration || 0)}
            </span>
            {#if session.completed}
              <CheckCircle size={14} class="completed-icon" />
            {/if}
          </div>
        {/each}
      </div>
    </div>
  {/if}
</div>

<!-- Settings Modal -->
{#if showSettings}
  <div class="modal-backdrop" on:click={closeSettings}>
    <div class="modal-content" on:click|stopPropagation>
      <div class="modal-header">
        <h2>{t.pomodoroSettings}</h2>
        <button class="btn-close" on:click={closeSettings}>
          <Icon name="x" size={20} />
        </button>
      </div>
      
      <div class="modal-body">
        <!-- Work Duration -->
        <div class="form-group">
          <label>{t.workDuration} ({tempWorkDuration} {t.minutes})</label>
          <input
            type="range"
            min="1"
            max="60"
            bind:value={tempWorkDuration}
            class="duration-slider"
          />
        </div>
        
        <!-- Short Break Duration -->
        <div class="form-group">
          <label>{t.shortBreakDuration} ({tempShortBreakDuration} {t.minutes})</label>
          <input
            type="range"
            min="1"
            max="30"
            bind:value={tempShortBreakDuration}
            class="duration-slider"
          />
        </div>
        
        <!-- Long Break Duration -->
        <div class="form-group">
          <label>{t.longBreakDuration} ({tempLongBreakDuration} {t.minutes})</label>
          <input
            type="range"
            min="1"
            max="60"
            bind:value={tempLongBreakDuration}
            class="duration-slider"
          />
        </div>
        
        <!-- Sessions Before Long Break -->
        <div class="form-group">
          <label>{t.sessionsBeforeLongBreak} ({tempSessionsBeforeLongBreak})</label>
          <input
            type="range"
            min="2"
            max="10"
            bind:value={tempSessionsBeforeLongBreak}
            class="duration-slider"
          />
        </div>
        
        <!-- Auto-start Breaks -->
        <div class="form-group">
          <label class="toggle-option-label">
            <span>{t.autoStartBreaks}</span>
            <label class="toggle-switch">
              <input type="checkbox" bind:checked={tempAutoStartBreaks} />
              <span class="toggle-slider"></span>
            </label>
          </label>
        </div>
        
        <!-- Auto-start Work -->
        <div class="form-group">
          <label class="toggle-option-label">
            <span>{t.autoStartWork}</span>
            <label class="toggle-switch">
              <input type="checkbox" bind:checked={tempAutoStartWork} />
              <span class="toggle-slider"></span>
            </label>
          </label>
        </div>
      </div>
      
      <div class="modal-footer">
        <button class="btn btn-secondary" on:click={closeSettings}>
          {t.cancel}
        </button>
        <button class="btn btn-primary" on:click={saveSettings}>
          {t.save}
        </button>
      </div>
    </div>
  </div>
{/if}

<style>
  .pomodoro-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 1rem;
    gap: 1.5rem;
  }

  .timer-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 1rem;
    position: relative;
  }

  .phase-indicator {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 1.2rem;
    color: #ff9500;
    font-weight: 600;
  }

  .timer-display {
    font-size: 5rem;
    font-weight: 700;
    color: #fff;
    font-family: "SF Mono", "Monaco", "Inconsolata", monospace;
    text-shadow: 0 2px 10px rgba(255, 149, 0, 0.3);
  }

  .progress-ring {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 280px;
    height: 280px;
    pointer-events: none;
    z-index: -1;
  }

  .progress-ring svg {
    width: 100%;
    height: 100%;
    transform: rotate(-90deg);
  }

  .progress-ring-bg {
    fill: none;
    stroke: #333;
    stroke-width: 4;
  }

  .progress-ring-fill {
    fill: none;
    stroke: #ff9500;
    stroke-width: 4;
    stroke-linecap: round;
    transition: stroke-dasharray 0.3s ease;
  }

  .session-counter {
    font-size: 1rem;
    color: #888;
  }

  .controls {
    display: flex;
    gap: 0.75rem;
    flex-wrap: wrap;
    justify-content: center;
  }

  .btn-control {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.875rem 1.5rem;
    border: none;
    border-radius: 12px;
    font-size: 1rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s ease;
    -webkit-tap-highlight-color: transparent;
  }

  .btn-primary {
    background: #ff9500;
    color: #000;
  }

  .btn-primary:hover {
    background: #ffaa33;
    transform: translateY(-2px);
  }

  .btn-secondary {
    background: #1a1a1a;
    color: #fff;
    border: 1px solid #333;
  }

  .btn-secondary:hover {
    background: #2a2a2a;
    border-color: #ff9500;
  }

  .btn-settings {
    background: transparent;
    color: #888;
    padding: 0.875rem;
  }

  .btn-settings:hover {
    color: #ff9500;
  }

  .sessions-section {
    width: 100%;
    max-width: 400px;
    margin-top: 1rem;
  }

  .sessions-section h3 {
    color: #ff9500;
    margin-bottom: 0.75rem;
    font-size: 1.1rem;
  }

  .sessions-list {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  .session-item {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.75rem;
    background: #1a1a1a;
    border-radius: 10px;
    border: 1px solid #333;
  }

  .session-item.completed {
    border-color: #ff9500;
  }

  .session-type {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    flex: 1;
    color: #fff;
  }

  .session-time {
    color: #888;
    font-family: "SF Mono", monospace;
  }

  .completed-icon {
    color: #ff9500;
  }

  /* Modal Styles */
  .modal-backdrop {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.8);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
    padding: 1rem;
  }

  .modal-content {
    background: #1a1a1a;
    border-radius: 20px;
    width: 100%;
    max-width: 400px;
    max-height: 90vh;
    overflow-y: auto;
    border: 1px solid #333;
  }

  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.25rem;
    border-bottom: 1px solid #333;
  }

  .modal-header h2 {
    color: #ff9500;
    font-size: 1.25rem;
    margin: 0;
  }

  .btn-close {
    background: none;
    border: none;
    color: #888;
    cursor: pointer;
    padding: 0.5rem;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .btn-close:hover {
    color: #fff;
  }

  .modal-body {
    padding: 1.25rem;
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
  }

  .modal-footer {
    display: flex;
    gap: 0.75rem;
    padding: 1.25rem;
    border-top: 1px solid #333;
  }

  .btn {
    flex: 1;
    padding: 0.875rem;
    border: none;
    border-radius: 10px;
    font-size: 1rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s ease;
  }

  .modal-footer .btn-secondary {
    background: #333;
    color: #fff;
  }

  .modal-footer .btn-secondary:hover {
    background: #444;
  }

  .modal-footer .btn-primary {
    background: #ff9500;
    color: #000;
  }

  .modal-footer .btn-primary:hover {
    background: #ffaa33;
  }

  .form-group {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  .form-group label {
    color: #fff;
    font-size: 0.95rem;
  }

  .duration-slider {
    width: 100%;
    height: 6px;
    -webkit-appearance: none;
    appearance: none;
    background: #333;
    border-radius: 3px;
    outline: none;
  }

  .duration-slider::-webkit-slider-thumb {
    -webkit-appearance: none;
    appearance: none;
    width: 20px;
    height: 20px;
    background: #ff9500;
    border-radius: 50%;
    cursor: pointer;
  }

  .duration-slider::-moz-range-thumb {
    width: 20px;
    height: 20px;
    background: #ff9500;
    border-radius: 50%;
    cursor: pointer;
    border: none;
  }

  .toggle-option-label {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.75rem;
    background: #333;
    border-radius: 10px;
    cursor: pointer;
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
    transition: 0.4s;
    border-radius: 50%;
  }

  input:checked + .toggle-slider {
    background-color: #ff9500;
  }

  input:checked + .toggle-slider:before {
    transform: translateX(22px);
  }

  @media (max-width: 768px) {
    .timer-display {
      font-size: 4rem;
    }

    .progress-ring {
      width: 240px;
      height: 240px;
    }

    .btn-control {
      padding: 0.75rem 1.25rem;
      font-size: 0.9rem;
    }
  }
</style>
