# Letterline

A calm, list-first Android launcher: favorites on the home screen, a wave alphabet on the edge to reach any app with one thumb, and notifications shown next to the apps they belong to.

It's an independent, clean-room project inspired by minimalist list launchers. No code, assets or copy come from any other launcher.

**Open source, no trackers, nothing to pay.** No accounts, no analytics, no ads, and no network requests unless you turn on web suggestions in search. See [PRIVACY.md](PRIVACY.md); every build runs `checkNoTrackers`, which fails if a tracking, ads or crash-reporting library is ever added.

## Status

0.3.0: a complete list launcher (everything except weather), plus fixes for what launcher users complain about most (`replica/fixes.md`): favorites grid and alignment, more round-button gestures, typo-tolerant search, unread dots, and a home screen that stays responsive. Progress is tracked in `replica/features.csv`.

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

## Licence

Letterline is released under the [MIT License](LICENSE).

The bundled fonts (Manrope, Outfit, Space Grotesk, Comfortaa, Lora, Playfair Display, Fraunces, DM Serif Display, JetBrains Mono, Bebas Neue, Caveat) are not covered by the MIT License. They are unmodified files from [google/fonts](https://github.com/google/fonts) under the SIL Open Font License 1.1; each licence is in `app/src/main/assets/licenses/fonts/` and shown in Settings > About > Font licences.
