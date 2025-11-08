# 🛠️ Development Guide - Sun Clock

## Architecture Overview

### Tech Stack
- **Runtime**: Bun (fast JavaScript runtime)
- **Framework**: Svelte 5 (reactive UI)
- **Build Tool**: Vite (fast bundler)
- **Mobile**: Capacitor 7 (native wrapper)
- **Language**: TypeScript (type safety)

### Project Structure

```
gunes/
├── src/
│   ├── components/          # Reusable UI components
│   │   ├── CircularClock.svelte      # Animated clock with alarm markers
│   │   ├── AlarmList.svelte          # List view of all alarms
│   │   ├── AlarmEditor.svelte        # Modal for creating/editing alarms
│   │   └── SunTimesDisplay.svelte    # Shows upcoming sun times
│   │
│   ├── services/            # Business logic layer
│   │   ├── alarm.ts         # Alarm scheduling & notifications
│   │   ├── location.ts      # GPS location services
│   │   └── sunCalc.ts       # Sunrise/sunset calculations
│   │
│   ├── stores/              # Svelte stores (state management)
│   │   └── index.ts         # Centralized store definitions
│   │
│   ├── utils/               # Helper functions
│   │   └── translations.ts  # i18n translations (11 languages)
│   │
│   ├── types/               # TypeScript type definitions
│   │   └── suncalc.d.ts     # Type definitions for suncalc library
│   │
│   ├── types.ts             # App-wide TypeScript interfaces
│   ├── App.svelte           # Root component
│   ├── main.ts              # App entry point
│   └── app.css              # Global styles
│
├── android/                 # Native Android project (auto-generated)
├── public/                  # Static assets
├── capacitor.config.ts      # Capacitor configuration
├── package.json             # Dependencies
├── tsconfig.json            # TypeScript config
└── vite.config.ts           # Vite configuration
```

## Core Concepts

### 1. State Management (Stores)

We use Svelte stores for reactive state management:

```typescript
// stores/index.ts
- alarms: Store for all alarm data
- settings: User preferences (language, clock mode, etc.)
- location: Current GPS coordinates
- sunTimes: Calculated sunrise/sunset for next 5 days
```

**Important**: Data persists to localStorage automatically.

### 2. Services Layer

#### Alarm Service (`services/alarm.ts`)
- Schedules notifications using Capacitor Local Notifications
- Handles alarm triggers
- Manages vibration via Haptics API
- Supports gradual volume increase
- Reschedules recurring alarms

#### Location Service (`services/location.ts`)
- Gets user's GPS coordinates
- Requests necessary permissions
- Provides location watching capabilities
- Caches location (5-minute validity)

#### Sun Calc Service (`services/sunCalc.ts`)
- Uses suncalc library for astronomical calculations
- Converts sun-based time to actual time
- Calculates upcoming sun times (5 days)
- Handles timezone conversions

### 3. Component Architecture

#### CircularClock.svelte
- Real-time analog clock display
- Shows alarm markers on clock face
- Supports both normal and sun-based time modes
- Animated hands (hour, minute, second)

**Key Props**:
```typescript
alarms: Alarm[]           // Alarms to display
currentTime: Date         // Current time
use24Hour: boolean        // Time format
useSunBased: boolean      // Clock mode
sunriseTime: Date | null  // For sun-based calculations
```

#### AlarmEditor.svelte
- Modal dialog for alarm CRUD operations
- Dynamic form based on clock mode
- Sound picker (placeholder - implement native picker)
- Day selector for recurring alarms

**Events**:
- `save`: Emitted when alarm is saved
- `close`: Emitted when modal is closed

#### AlarmList.svelte
- Displays all alarms with toggle switches
- Shows alarm metadata (repeat days, options)
- Inline edit/delete actions

## Data Flow

```
User Action
    ↓
Component Event
    ↓
Store Update
    ↓
Service Call (if needed)
    ↓
Native API (Capacitor)
    ↓
Update Store
    ↓
Reactive UI Update
```

### Example: Creating an Alarm

1. User clicks "Add Alarm" → Opens AlarmEditor
2. User fills form → Clicks "Save"
3. AlarmEditor emits save event
4. App.svelte handles event:
   - Updates alarms store
   - Calls alarmService.scheduleAlarm()
5. AlarmService:
   - Requests notification permission
   - Calculates alarm times (next 7 days)
   - Schedules native notifications
6. Store update triggers reactive UI refresh
7. Alarm appears in AlarmList

## Key Features Implementation

### Sun-Based Alarms

**How it works**:
1. User sets offset from sunrise (e.g., +30 minutes)
2. App gets user's location
3. SunCalc library calculates sunrise for that location
4. Alarm time = sunrise + offset
5. Recalculates daily as sunrise changes

**Code Flow**:
```typescript
// services/sunCalc.ts
convertSunTimeToActualTime(offset, location, date)
  → getTimes(date, lat, lon)
  → sunrise + offset
  → actual alarm time
```

### Gradual Volume

Implemented in Android native layer:
1. Alarm starts at low volume
2. Increases linearly over set duration (10-120 seconds)
3. Uses NotificationChannel with custom sound
4. Volume controlled by system AudioManager

### Multilingual Support

**System Language Detection**:
```typescript
// utils/translations.ts
detectSystemLanguage()
  → navigator.language
  → Map to supported language
  → Default: English
```

**Usage in Components**:
```typescript
$: t = getTranslation(settings.language)
// Then use: t.addAlarm, t.save, etc.
```

## Development Workflow

### 1. Making Changes

**Edit Svelte Components**:
```bash
# Start dev server (hot reload)
bun run dev
# Open http://localhost:5173
```

**Test on Device**:
```bash
bun run build
bunx cap sync android
bunx cap open android
# Click Run in Android Studio
```

### 2. Adding New Features

#### Adding a New Component

1. Create file in `src/components/`
2. Define props with `export let`
3. Import in parent component
4. Use in template

```svelte
<!-- src/components/MyFeature.svelte -->
<script lang="ts">
  export let data: string;
</script>

<div>{data}</div>
```

#### Adding a New Service

1. Create file in `src/services/`
2. Implement singleton pattern
3. Export instance

```typescript
// src/services/myService.ts
export class MyService {
  private static instance: MyService;
  
  static getInstance() {
    if (!MyService.instance) {
      MyService.instance = new MyService();
    }
    return MyService.instance;
  }
  
  // Methods...
}

export const myService = MyService.getInstance();
```

#### Adding a New Store

```typescript
// stores/index.ts
function createMyStore() {
  const { subscribe, set, update } = writable<MyType>(defaultValue);
  
  return {
    subscribe,
    set,
    update: (data) => update(current => ({ ...current, ...data })),
  };
}

export const myStore = createMyStore();
```

### 3. Adding a Language

Edit `src/utils/translations.ts`:

```typescript
// 1. Add language code to Language type
export type Language = 'en' | 'tr' | 'newlang' | ...;

// 2. Add translations object
const translations: Record<Language, Translation> = {
  // ...
  newlang: {
    appName: 'Translated Name',
    alarms: 'Translated Text',
    // ... all keys
  }
};

// 3. Add detection
export function detectSystemLanguage(): Language {
  const lang = navigator.language.toLowerCase();
  if (lang.startsWith('newlang')) return 'newlang';
  // ...
}

// 4. Add to supportedLanguages
export const supportedLanguages = [
  // ...
  { code: 'newlang', name: 'Native Name' }
];
```

## Debugging

### Browser DevTools (Web)

```bash
bun run dev
# Open http://localhost:5173
# F12 → Console/Network/Elements
```

### Android Studio (Native)

1. Run app in debug mode
2. Open "Logcat" panel
3. Filter by package: `dev.mudrock.gunes`
4. Look for console.log outputs and errors

**Useful filters**:
```
tag:Capacitor     # Capacitor logs
tag:chromium      # Web logs
package:mine      # App logs only
```

### Common Issues

**Alarms not firing**:
- Check `AlarmService.scheduledAlarms` Set
- Verify notification permissions
- Check Logcat for scheduling errors
- Ensure device not in battery saver mode

**Location not updating**:
- Check GPS permissions in app settings
- Verify location services enabled
- Check Logcat for location errors
- Try `locationService.getCurrentLocation()` manually

**Stores not persisting**:
- Check localStorage in browser DevTools
- Verify `saveToStorage()` is called in onMount
- Check for localStorage quota exceeded errors

## Performance Optimization

### Bundle Size
- Keep dependencies minimal
- Use tree-shaking (automatically handled by Vite)
- Lazy load heavy components if needed

### Runtime Performance
- Use Svelte's reactive statements ($:) wisely
- Avoid expensive calculations in render loop
- Use `onMount` for initialization
- Clean up subscriptions in `onDestroy`

### Battery Optimization
- Use coarse location when possible
- Cache location (5-minute default)
- Use efficient notification scheduling
- Minimize wake locks

## Testing

### Manual Testing Checklist

**Alarm Features**:
- [ ] Create normal alarm
- [ ] Create sun-based alarm
- [ ] Edit alarm
- [ ] Delete alarm
- [ ] Toggle alarm on/off
- [ ] Alarm rings at correct time
- [ ] Vibration works
- [ ] Sound plays
- [ ] Gradual volume increases
- [ ] Delete after use works
- [ ] Recurring alarms work

**Location Features**:
- [ ] Location permission requested
- [ ] Location obtained
- [ ] Sun times calculated
- [ ] Sun times update with location
- [ ] Manual location refresh works

**UI/UX**:
- [ ] Clock displays correctly
- [ ] Alarm markers visible on clock
- [ ] Language changes work
- [ ] Settings persist
- [ ] Responsive on different screen sizes
- [ ] Dark theme consistent

## Building for Production

### Debug Build
```bash
bun run build
bunx cap sync android
bunx cap open android
# Build → Build Bundle(s) / APK(s) → Build APK(s)
```

### Release Build

1. **Update version** in `android/app/build.gradle`:
```gradle
versionCode 2
versionName "1.1.0"
```

2. **Create keystore** (first time only):
```bash
keytool -genkey -v -keystore my-release-key.keystore \
  -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000
```

3. **Build release APK**:
```bash
cd android
./gradlew assembleRelease
# Output: android/app/build/outputs/apk/release/app-release.apk
```

4. **Sign APK** (if not auto-signed):
```bash
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 \
  -keystore my-release-key.keystore \
  app-release-unsigned.apk my-key-alias
```

## Best Practices

### Code Style
- Use TypeScript for type safety
- Follow Svelte conventions
- Use descriptive variable names
- Comment complex logic
- Keep components small and focused

### Git Workflow
- Commit often with clear messages
- Create branches for features
- Test before merging
- Don't commit `node_modules/` or `android/build/`

### Security
- Never hardcode API keys
- Validate user input
- Request minimal permissions
- Use HTTPS for external APIs
- Keep dependencies updated

## Useful Resources

- [Svelte Tutorial](https://svelte.dev/tutorial)
- [Capacitor Docs](https://capacitorjs.com/docs)
- [SunCalc Library](https://github.com/mourner/suncalc)
- [Bun Documentation](https://bun.sh/docs)
- [Android Developer Docs](https://developer.android.com/)

## Getting Help

1. Check this guide
2. Review README.md and QUICKSTART.md
3. Search existing issues
4. Check Capacitor/Svelte docs
5. Enable debug logging in code

---

Happy developing! ☀️