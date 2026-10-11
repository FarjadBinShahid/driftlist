# Driftlist

A calm, list-first Android launcher: favorites on the home screen, a wave alphabet on the edge to reach any app with one thumb, and notifications shown next to the apps they belong to.

It's a personal, clean-room project inspired by the UX of Niagara Launcher. No code, assets or copy come from the original.

## Status

Milestone 1 (vertical slice):

- Set as the default launcher
- Favorites with clock and date
- A–Z app list with the wave alphabet
- Inline notification previews and the app pop-up
- App options: favorite, app info, uninstall

## Build

Requirements: Android Studio (for its bundled JDK and the Android SDK). Nothing else.

```bash
# Windows (Git Bash); on macOS/Linux point JAVA_HOME at Android Studio's jbr
export JAVA_HOME="$LOCALAPPDATA/Programs/Android Studio/jbr"
./gradlew assembleDebug testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or open the folder in Android Studio and press Run.

## Stack

Kotlin, Jetpack Compose (Material 3), Room, coroutines/Flow. On-device only: no accounts, no analytics.

## Docs

- `replica/recon.md`: screens, flows, components and data model, from public sources only
- `replica/features.csv`: feature matrix and build progress
- `replica/architecture.md`: stack, entry points, jobs and build order
- `replica/schema.sql`: full on-device schema (Room implements it table by table)
- `replica/design/`: design tokens (`tokens.json`), contrast checker, Compose token generator and component specs
