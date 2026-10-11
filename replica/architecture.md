# Architecture: launcher clone (a rebuild of Niagara Launcher's core + Pro features)

Inputs: `replica/recon.md` (S01–S31, F01–F19) and `replica/features.csv` (82 rows).
Working name: "the launcher" until `/replica-brand` names it. The package id is a placeholder: `dev.farjad.launcher`.

## The shape of this app

This is not a web app. A launcher is **a native Android app with no backend**. Everything is on the device:
the only network calls are weather and (optionally) web-search suggestions, both using public APIs and the
user's own keys. So the replica defaults (Next.js, Postgres, Stripe, Vercel) are swapped for the Android
equivalents below. The rule is the same: boring, documented, one database, nothing extra.

## Stack

| layer | choice | why |
| --- | --- | --- |
| language | Kotlin (latest stable), JVM 17 toolchain | Required for LauncherApps, AppWidgetHost and NotificationListenerService; first-class on Android |
| UI | Jetpack Compose + Material 3 | Declarative states (empty / filled / error) map one-to-one onto the recon's states; custom gestures via `pointerInput` |
| hot list | Compose `LazyColumn` with stable keys; fall back to a RecyclerView in `AndroidView` only if profiling shows jank | The A–Z list and wave alphabet must hold 120 fps on a 300-app list |
| widgets host | `AppWidgetHost` + `AppWidgetHostView` wrapped in `AndroidView` | Only official way to host third-party widgets; no Compose-native alternative |
| database | Room over SQLite (`replica/schema.sql`) | One local DB; compile-time-checked queries; Flow-based observation drives the UI |
| settings | Jetpack DataStore (Preferences) | Single values (left-hand mode, haptics, header, weather config, button actions) do not need tables |
| DI | Hilt | Standard, documented; scopes services (listener, accessibility) cleanly |
| async | Kotlin coroutines + Flow | Room, DataStore and system callbacks all expose Flow |
| background jobs | WorkManager (+ `AlarmManager.setWindow` for batch delivery) | Survives process death and reboots; battery-aware |
| networking | OkHttp + kotlinx.serialization | Two tiny endpoints; Retrofit is unnecessary |
| images | Coil 3 | Async icon and wallpaper decoding with memory cache |
| colour | Material Color Utilities (`com.google.android.material:material` color utils / `material-color-utilities`) | Open-source wallpaper palette extraction + Material You backport (F11) |
| alphabet | `android.icu.text.AlphabeticIndex` + `Transliterator` (API 24+/29+) | Locale-correct buckets, `#`, Å/Ä/Ö/Ñ (help 155) |
| calculator | Hand-written recursive-descent parser (or exp4j, Apache-2.0) | Small grammar from help 173; no eval |
| weather | **Open-Meteo** forecast API (keyless, CC-BY 4.0) | Official public API, no key to manage; OpenWeather with your own key is the swap |
| web suggestions | Brave Search **Suggest API** with your own key; otherwise no suggestions, just "Search the web for …" via the engine's public search URL | Only documented APIs. Google/DDG autocomplete endpoints are undocumented, so they are not used |
| auth | **None** | Single-user, on-device. No accounts (Niagara ID is out of scope) |
| payments | **None.** `BuildConfig.PRO_UNLOCKED` / debug toggle in Settings | Personal project; Pro features are feature flags (`FeatureGate`) |
| email | None | — |
| crash reporting | None at first; add Firebase Crashlytics or Sentry only if the app is shared | Keep data on device |
| build / CI | Gradle (Kotlin DSL) + version catalog; GitHub Actions `assembleDebug` + unit tests later | Standard |
| min / target SDK | **minSdk 26** (Android 8), target latest stable | Notification channels and batching need 8; Usage Breaker and Quick Lock check for 9+ (`GLOBAL_ACTION_LOCK_SCREEN` is API 28) |

Modules (keep it small; split later only if build times hurt):

```
app/                    single Gradle module to start
  core/        FeatureGate, dispatchers, time, result types
  data/        Room DB, DAOs, DataStore, repositories, backup codec
  system/      AppCatalog (LauncherApps), Notifications (listener), Widgets (host), Usage, Calendar, Media, Wallpaper
  domain/      use cases (LaunchApp, ReorderFavorites, BuildAlphabetIndex, ScheduleBatch, ...)
  ui/          screens by S-ID, components, theme (tokens from /replica-design)
```

## Schema

Tables: **18**. Access rules: **app sandbox + data-layer only**. There is no server, so no RLS. Repositories
are the only code that writes to Room. UI and services go through use cases, and every write that touches
more than one table runs in `withTransaction`.

Full DDL: [`schema.sql`](schema.sql) (validated in SQLite; triggers and partial unique indexes tested).

| table | holds | key rules |
| --- | --- | --- |
| `app` | every launchable activity per profile, plus rename / hide / usage counters | unique (package, activity, user_serial); delete cascades everywhere |
| `shortcut` | pinned deep shortcuts | unique (package, shortcut_id, user_serial) |
| `widget_stack`, `widget` | hosted widgets and stacks | trigger: max 4 per stack; `app_widget_id` unique |
| `popup_folder`, `folder_item` | pop-up folders | kind check (app/shortcut); no nesting |
| `app_popup_config`, `popup_hidden_shortcut` | per-app pop-up edits | row exists only after an edit |
| `home_item` | ordered favorites (app / folder / stack / shortcut) | exactly-one-target check; an app is a favorite once (partial unique) |
| `icon_override` | per app, per icon pack | unique (app, pack): Auto-Restore for free |
| `theme` | saved themes | partial unique: one active |
| `calendar_excluded`, `weather_cache` | calendar filter, last forecast | weather is a single row |
| `usage_rule`, `usage_session` | Usage Breaker | reminder minute constrained to the ladder |
| `summary_rule`, `notification_batch`, `batch_item` | Notification Summary | one open batch (partial unique); (batch, key) PK = idempotent |

DataStore keys (not tables): `leftHandMode`, `haptics`, `swipeUpSearch`, `searchExcludesHidden`, `webEngine`,
`braveApiKey`, `header{clockPosition, showWeather, showEvents, mediaCard, replacedByWidgetId}`,
`weather{mode, fixedName, lat, lon, units}`, `button{enabled, tap, swipe}`, `quickLock`, `twoColumn`,
`hideStatusBar`, `plugAndPlay`, `onboardingDone`, `proUnlocked (debug)`.

Not stored, read live: notifications, media sessions, calendar events, contacts, usage stats.

## API

There is no HTTP API to design. The "routes" of a launcher are its **system entry points** and its
**use cases**. They are listed by flow, in the same format.

### System entry points (AndroidManifest)

| entry point | does | who calls it | input | output | flow |
| --- | --- | --- | --- | --- | --- |
| `MainActivity` (`MAIN` + `HOME` + `DEFAULT`, `singleTask`, `launchMode` stable) | hosts every screen; HOME press → `onNewIntent` → home / search / last list position | the system | HOME intent | UI | F02 F03 F04 |
| `OnboardingActivity` (or a nav graph start destination) | S01–S03 | MainActivity when `onboardingDone == false` | — | sets onboarding flag, favorites | F01 |
| `RoleManager.createRequestRoleIntent(ROLE_HOME)` | ask to become the default launcher (API 29+; `Settings.ACTION_HOME_SETTINGS` below that) | S03 | — | role granted / denied | F01 |
| `LauncherNotificationListener : NotificationListenerService` | mirrors active notifications into an in-memory `StateFlow<Map<AppKey, List<Notif>>>`; intercepts Summary apps (cancel + store in batch) | the system, after user grants access | `StatusBarNotification` | flow consumed by S05 S06 S07 | F05 F14 |
| `QuickLockService : AccessibilityService` | `performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)` on a double-tap of the alphabet. No event reading (`accessibilityEventTypes` = none) | our UI via a bound singleton | — | screen off | F15 |
| `LauncherApps.Callback` (registered in `AppCatalog`) | install / remove / update / profile available | the system | package, user | upsert / delete `app` rows | all |
| `ACTION_CONFIGURATION_CHANGED` / locale changed receiver | rebuild sort keys + buckets | the system | — | `app.sort_key`, `app.bucket` | F03 |
| `AppWidgetHost.startListening` (in MainActivity `onStart`) | widget updates | the system | — | widget views | F08 |
| `ACTION_APPWIDGET_BIND` / configure activity launch | bind permission + widget config | our UI | provider, appWidgetId | bound widget | F08 |
| FileProvider + `ACTION_SEND` / `ACTION_OPEN_DOCUMENT` | share and import `.lbk` backups and `.lth` themes (our own formats, not Niagara's) | user | file | rows | F12 F18 |

### Use cases (the app's internal API), by flow

| use case | does | who | input | output | flow |
| --- | --- | --- | --- | --- | --- |
| `CompleteOnboarding` | save picked favorites, set flag | S02 | app ids | home_item rows | F01 |
| `ObserveHome` | favorites + suggestions + media + header state | S05 | — | `Flow<HomeState>` | F02 |
| `LaunchApp` | `LauncherApps.startMainActivity` with source bounds, bump `launch_count`/`last_launched_at` | S05 S06 S11 S15 | app id, view bounds | launched | F02 F03 |
| `ObserveAppList` | non-hidden apps grouped by bucket, notification previews joined in | S06 | — | `Flow<List<Section>>` | F03 |
| `BuildAlphabetIndex` | ICU buckets for the current locales, letter → list index map | S06 | apps, locales | `AlphabetIndex` | F03 |
| `Search` | apps (prefix > word-start > fuzzy), calculator, contacts (Pro), web suggestions (debounced 150 ms) | S15 | query | `Flow<SearchResults>` | F04 |
| `EvaluateExpression` | calculator | Search | string | number or null | F04 |
| `ReplyToNotification` | fill `RemoteInput`, send the action's `PendingIntent` | S07 | key, text | sent / failed | F05 |
| `DismissNotification` | `cancelNotification(key)` | S07 | key | — | F05 |
| `ToggleFavorite`, `ReorderFavorites`, `RemoveFavorite` | edit `home_item` in one transaction | S08 S10 | ids, new order | rows | F06 |
| `CreateFolder`, `EditFolder`, `DeleteFolder`, `SetFolderAsCategory` (Pro) | pop-up folders | S10 S11 | title, items | rows | F07 |
| `AddWidget`, `RemoveWidget`, `MoveWidgetInStack` | allocate id → bind → configure → insert; `deleteAppWidgetId` on remove | S12 S13 S07 | provider, stack | rows | F08 |
| `SetIconOverride`, `SuggestIcons` (Pro) | write override for the current pack; suggest by package-name / label similarity inside the pack | S09 | app, pack, drawable | row | F09 |
| `RenameApp`, `SetHidden` | update `app` | S08 S24 | id, value | row | F10 |
| `ApplyTheme`, `SaveTheme`, `ExtractPalette` | set active theme, apply wallpaper (`WallpaperManager`), derive colours | S18 S19 S21 | theme | `Flow<ThemeTokens>` | F11 |
| `ExportTheme`, `ImportTheme` (Pro) | zip theme JSON + font + wallpaper (size cap 10 MB) | S18 | theme / uri | file / row | F12 |
| `SetUsageRules`, `TrackUsage` | rules CRUD; heartbeat job decides reminders | S22 S23 | packages | sessions, notifications | F13 |
| `SetSummaryRules`, `ScheduleBatch`, `DeliverBatch`, `ClearBatch` | Notification Summary | S22 S23 S31 | packages | batch rows, posted summary | F14 |
| `SetQuickLock` | check accessibility enabled, deep link if not | S22 S29 | — | — | F15 |
| `RefreshWeather` | Open-Meteo fetch → `weather_cache` | header, S14 | lat/lon | cache | F16 |
| `ObserveAgenda` | `CalendarContract.Instances` next 7 days minus `calendar_excluded` | S14 | — | `Flow<List<Event>>` | F16 |
| `SetButtonAction` | DataStore | S16 | action | — | F17 |
| `CreateBackup`, `RestoreBackup` | versioned JSON of all tables + DataStore → zip `.lbk`; restore = replace in one transaction | S25 | uri | file / rows | F18 |

### Outbound HTTP (public APIs only, user's own keys)

| method url | does | who | input | output | flow |
| --- | --- | --- | --- | --- | --- |
| `GET https://api.open-meteo.com/v1/forecast?latitude&longitude&current=…&hourly=…&daily=…&timezone=auto` | forecast | `RefreshWeather` | lat/lon rounded to 2 dp | JSON | F16 |
| `GET https://geocoding-api.open-meteo.com/v1/search?name=` | fixed-location lookup | S14 location picker | city name | candidates | F16 |
| `GET https://api.search.brave.com/res/v1/suggest/search?q=` (header `X-Subscription-Token`) | web suggestions, only if a key is set | `Search` | query | suggestions | F04 |
| `ACTION_VIEW https://<engine>/search?q=` | open the web search in the browser | `Search` | query | browser | F04 |

Webhooks in: none. Webhooks out: none.

### Jobs

| job | schedule | does |
| --- | --- | --- |
| `UsageBreakerWorker` | while any rule exists: a 60 s loop, started on screen-on (`ACTION_SCREEN_ON` dynamic receiver) and stopped on screen-off; *not* a 15-min periodic job, which is too coarse | `UsageStatsManager.queryEvents` since the last check → foreground package → extend or end the session (60 s grace) → post a reminder when crossing 5/10/20/30/45/60/90/120/180/240 min |
| `BatchDeliveryAlarm` | `AlarmManager.setWindow` at `scheduled_for` (top of the hour, 5.5–6.5 h after the batch opens) | post one summary notification, mark delivered, open the next batch on the next intercepted notification |
| `WeatherRefreshWorker` | periodic 30 min, `NetworkType.CONNECTED`; also on agenda open if older than 10 min | refresh `weather_cache` |
| `CleanupWorker` | daily, idle + charging | delete ended `usage_session` rows older than 30 days and delivered batches older than 7 days; free orphaned `app_widget_id`s (`host.appWidgetIds` minus `widget.app_widget_id`) |
| `IconCacheWarmup` | on theme / icon pack change | pre-render icons to a disk cache so cold start shows icons instantly |

## The parts that bite

- **Cold start and process death.** The launcher gets killed and restarted constantly, and HOME must paint in under 300 ms. Keep the last app list in Room (not only in memory), cache rendered icons to disk, and never block the first frame on LauncherApps.
- **Gesture arbitration (wave alphabet).** Edge strip vs. list scroll vs. long swipe up (search) vs. swipe down (shade) vs. swipe right on a row (pop-up) vs. system back/home gestures. Use `systemGestureExclusionRects` on the alphabet strip (max 200 dp per edge, an Android limit), velocity and distance thresholds, and haptic `CLOCK_TICK` per letter. Prototype this first: it is the product.
- **Opening the notification shade.** No public API. `StatusBarManager.expandNotificationsPanel` via reflection needs `EXPAND_STATUS_BAR` and can break on OEM builds. Wrap it, catch failures, and degrade gracefully (fall back to nothing and hide the hint).
- **Notification listener lifecycle.** It can be unbound by the OS; call `requestRebind` on start. Grouped and conversation notifications need de-duplication by group key. Android 13+ sideloaded builds hit "Restricted settings" (S29 flow). Intercepting for Summary means `cancelNotification` *after* storing, and the original app's re-posts must update, not duplicate (PK on key).
- **Time zones and DST.** Batch "top of the hour" is computed in local time with `java.time.ZonedDateTime` and stored as UTC. Recompute on `ACTION_TIMEZONE_CHANGED`. Calendar all-day events are stored in UTC by CalendarContract, so they must be shown without shifting to the previous day.
- **Idempotency.** Backup restore runs in one transaction (wipe + insert) and is safe to repeat. `batch_item` PK (batch, key) absorbs re-posts. Widget add allocates the id first and deletes it on any cancel path, or ids leak.
- **Races.** Package callbacks arrive while the user is reordering favorites, so all `home_item` writes are serialized through one repository mutex plus transactions. Positions are rewritten whole, never patched.
- **Widgets.** `bindAppWidgetIdIfAllowed` vs. the bind dialog; configure activities that return `RESULT_CANCELED`; OEM widgets that crash ("Can't show content" state); restored backups have no widget bindings, so placeholders ask the user to re-add.
- **Multi-profile.** Work profile apps carry a badge; quiet mode makes them unavailable (grey out, don't delete). The identity is (package, activity, user_serial), never the package alone.
- **OEM battery killers** (Xiaomi, Samsung) stop the listener and workers. A help screen links to dontkillmyapp.com-style instructions, which is what the original does.
- **Locale sorting.** Transliteration is API 29+; below that, fall back to `Normalizer` + `#`. Rebuild on locale change.
- **Search speed.** Under 300 apps, in-memory filtering beats FTS. Precompute normalized labels and keep everything in memory. Contacts query is debounced and cursor-limited to 5.
- **Rate limits.** Open-Meteo's free tier is 10k calls/day, which is plenty at 30-min refresh. Brave Suggest's free tier is capped, so debounce at 150 ms and require 2+ characters.
- **File size limits.** Theme export capped at 10 MB, fonts at 2 MB, wallpapers downscaled to screen size before bundling.
- **Privacy / GDPR.** No account and no analytics. Only rounded coordinates leave the device (weather), plus search queries if suggestions are on. Uninstall deletes everything. Say so in a one-screen privacy note.
- **Not applicable:** realtime, multi-tenancy, email deliverability, payments, webhooks.

## Build order

Every milestone ends with the app installable and usable as the daily launcher on your phone.

1. **Vertical slice: "it's my launcher now"** (target: one weekend)
   - Screens: S03 (set default), S05 (favorites, plain clock), S06 (A–Z list + **wave alphabet**), S07 (notifications only, no reply yet), S08 (Favorite toggle, App info)
   - Tables: `app`, `home_item`
   - Use cases: `ObserveHome`, `ObserveAppList`, `BuildAlphabetIndex`, `LaunchApp`, `ToggleFavorite`
   - System: HOME activity, `LauncherApps.Callback`, `NotificationListenerService` (read-only previews)
   - Proves: Compose list performance, gesture arbitration, listener plumbing.

2. **Must-haves** (features.csv `must`, by area)
   - Onboarding: S01, S02, S04, S29 permission dialogs (F01)
   - Home: S10 edit favorites (reorder / remove), S08 rename + hide, S24 hidden apps (F06, F10)
   - Notifications: inline reply + dismiss in S07, app shortcuts in S07 (F05)
   - Gestures: swipe down to shade, long swipe up to search
   - Search: S15 apps (F04)
   - Pop-ups: S11 pop-up folders (F07); tables `popup_folder`, `folder_item`, `shortcut`
   - Widgets: single custom widget in the header (S12, S13); tables `widget_stack`, `widget` (F08 part 1)
   - Look: light / dark; S09 icon picker + third-party icon packs; table `icon_override` (F09)
   - Wellbeing: Usage Breaker (S22, S23, S30); tables `usage_rule`, `usage_session` (F13)
   - Settings S17 / S22; backup and restore S25 (F18)

3. **Should-haves**
   - Haptics, locale buckets, work profile, recently installed (S28), suggestions row
   - HOME-press behaviour (search / last list position)
   - Calculator + web search (engine choice, Brave Suggest)
   - Media card, header position slider, action button (S16, F17)
   - Left-hand mode, font size
   - Wallpaper palette + accent (`ExtractPalette`), clock styles, custom fonts (S20)
   - Themes: built-in themes, Theme Studio (S19), My Themes, `theme` table (F11)
   - Icon Auto-Restore, widget stacks (up to 4)
   - Calendar agenda + weather (S14, F16)
   - Notification Summary (S31, F14), Quick Lock (F15)

4. **Could-haves:** category folders, customizable app pop-ups, hide shortcuts, contacts in search, widgets in pop-ups, two-column / landscape, theme export / import (F12), Icon Assistant suggestions, accent override, blur, wallpaper picker, hide status bar, Plug and Play, flashlight row, calendar filter, exclude hidden from search.

5. **Fixes from replica-entrepreneur:** not run yet. Run `/replica-entrepreneur` before milestone 3 if you want the clone to fix what Niagara users complain about (e.g. the Usage Breaker 5-minute first reminder, Discord-only theme sharing).
