# Recon map: Niagara Launcher (Android)

Scope: core launcher loop plus the Niagara Pro feature set. That covers the home screen with favorites, the wave-alphabet app list, search, inline notifications, pop-ups, widgets and stacks, calendar, weather and media, themes and Theme Studio, icons and fonts, Usage Breaker, Notification Summary, Quick Lock, hidden apps, and backup and restore.
For: a personal / learning project. It will not ship to Play, so Pro gating becomes feature flags and there are no payments.
Date: 2026-10-11
Original: `bitpit.launcher` by Mellowdrop Studio (formerly 8bitPit). 10M+ installs, 4.7★ (~247K reviews), updated 2026-09-30.

Method: public sources only. Nothing was logged into, and there was no APK, decompilation or network inspection. Claims marked *(unverified)* come from a single or dated source.

## Sources

| # | source | URL | notes |
| --- | --- | --- | --- |
| 1 | Play listing | https://play.google.com/store/apps/details?id=bitpit.launcher&hl=en | feature pitch grouped as Customization / Productivity / Wellbeing; Theme Studio in "What's new"; accessibility service used only for screen-off |
| 2 | Help center index | https://help.niagaralauncher.app/collection/1-public-knowledge-base | 6 categories, the most complete feature list |
| 3 | Help: Features (p1, p2) | https://help.niagaralauncher.app/category/45-upcoming-features · /2 | ~33 feature articles; mixes shipped and proposed |
| 4 | Help: How-To's | https://help.niagaralauncher.app/category/22-how-tos | favorites, clock, search gesture, alphabet localization, theme sharing |
| 5 | Help: Niagara Pro Features | https://help.niagaralauncher.app/article/40-niagara-pro-features | full Pro list, effectively the pricing page |
| 6 | Help: Tips and Tricks | https://help.niagaralauncher.app/article/34-tips-and-tricks | gesture map |
| 7 | Help: Pop-ups | https://help.niagaralauncher.app/article/115-pop-ups | free/Pro table, no nesting |
| 8 | Help: Search gesture | https://help.niagaralauncher.app/article/90-mastering-the-search-gesture | long swipe up = search, quick swipe = scroll |
| 9 | Help: Edit favorites | https://help.niagaralauncher.app/article/127-edit-favorites | drag handle reorder |
| 10 | Help: Niagara Button | https://help.niagaralauncher.app/article/116-niagara-button | bottom-right action button |
| 11 | Help: Notification Summary | https://help.niagaralauncher.app/article/122-notification-summary | batch schedule 5.5–6.5 h |
| 12 | Help: Notification gesture | https://help.niagaralauncher.app/article/98-notification-gesture | swipe down anywhere = shade |
| 13 | Help: Backup settings | https://help.niagaralauncher.app/article/37-backup-settings | manual `.nlb` file in Downloads |
| 14 | Help: Widgets | https://help.niagaralauncher.app/article/41-adding-multiple-widgets | stacks up to 4, pop-up widgets |
| 15 | Help: Calendar | https://help.niagaralauncher.app/article/76-calendar-faq | CalendarProvider, event filter |
| 16 | Help: Weather | https://help.niagaralauncher.app/article/105-weather-widget | OpenWeather, hourly 24 h + 7-day, 30-min refresh |
| 17 | Help: Media player | https://help.niagaralauncher.app/article/11-media-widget | auto-appearing media card |
| 18 | Help: Calculator | https://help.niagaralauncher.app/article/173-integrated-calculator | expression grammar |
| 19 | Help: Icon Assistant | https://help.niagaralauncher.app/article/130-icon-assistant | Quick-Edit, Auto-Correct, Auto-Restore |
| 20 | Help: Theme sharing | https://help.niagaralauncher.app/article/193-theme-sharing | `.nlt` files, import is Pro |
| 21 | Help: Web search suggestions | https://help.niagaralauncher.app/article/186-web-search-suggestions | 7 engines |
| 22 | Help: Custom clock | https://help.niagaralauncher.app/article/23-custom-clock | clock styles, replace clock with widget |
| 23 | Help: Hide shortcuts | https://help.niagaralauncher.app/article/84-hide-shortcuts | edit shortcuts in an app pop-up |
| 24 | Help: Usage Breaker | https://help.niagaralauncher.app/article/181-usage-breaker | fixed reminder ladder 5→240 min |
| 25 | Help: Color system | https://help.niagaralauncher.app/article/133-color-system | wallpaper-derived palette, Material You backport |
| 26 | Help: Alphabet localization | https://help.niagaralauncher.app/article/155-alphabet-localization | transliteration, locale letters, `#` bucket |
| 27 | Help: Double tap to sleep | https://help.niagaralauncher.app/article/19-double-tap-to-sleep | accessibility service, alphabet only |
| 28 | Help: Foldables | https://help.niagaralauncher.app/article/100-foldable-device-support | two-column layout |
| 29 | Help: Purchase help | https://help.niagaralauncher.app/article/10-purchase-help | subscription and lifetime, Niagara ID, 3 devices |
| 30 | Help: Password / hidden apps | https://help.niagaralauncher.app/article/63-password-protected-hidden-apps | no app locking, by design |
| 31 | Help: Lock layout | https://help.niagaralauncher.app/article/113-lock-home-screen-layout | not offered, by design |
| 32 | Help: Dual apps | https://help.niagaralauncher.app/article/54-dual-app-support | multi-user profiles |
| 33 | Help: Move clock / pressing home / restricted settings | https://help.niagaralauncher.app/article/156-move-clock · /article/86-pressing-home-doesnt-show-favorites · /article/134-fix-restricted-settings | clock position slider, home-press behaviour, permission error state |
| 34 | Onboarding walkthrough (2024) | https://gigazine.net/gsc_news/en/20240217-android-niagara-launcher | first-run screens in order, 7-day trial |
| 35 | Theme Studio launch (2026-04-16) | https://9to5google.com/2026/04/16/niagara-launcher-adding-theme-studio-for-creating-sharing-your-homescreen-themes-gallery/ · https://www.androidheadlines.com/2026/04/niagara-launcher-theme-studio.html | editor: wallpaper → icons, clock, font, live preview |
| 36 | Artistic Update (2025-11) | https://9to5google.com/2025/11/06/niagara-launchers-new-themes-perfectly-avoid-clashing-with-your-home-screen/ | theme = wallpaper + clock + icon pack |
| 37 | Summer Update (2024) | https://www.androidheadlines.com/2024/05/niagara-launcher-summer-update-major-features.html | calculator, web search, contacts, invites |
| 38 | Niagara ID, Pro outside Play | https://www.androidpolice.com/niagara-launcher-pro-decouples-google-play-all-android-phones/ | account-based licence |
| 39 | Public issue tracker and releases | https://github.com/NiagaraLauncher/Niagara-Issues/releases | v1.16.x notes; issue titles used as evidence only |
| 40 | Official site | https://niagaralauncher.com/ · https://niagaralauncher.com/niagara-pro-features/ | free vs Pro, pricing |
| 41 | Feature board | https://niagaralauncher.featurebase.app/p/usage-breaker-timer | user requests |
| 42 | Hide from app list | https://tech.yahoo.com/apps/articles/custom-launcher-fix-bloated-samsung-133119240.html | long-press › Advanced › Hide |

Not reachable: Reddit, YouTube, Medium (403), Android Authority (403). Walkthrough videos would sharpen animation timings. If you ever install the app, screenshots of S05, S06, S07, S15 and S19 are the most valuable to drop into `replica/screens/`.

Terms: no account was used. Niagara's ToS was not consulted because the recon is public-source only, and that is safe whatever the ToS says.

## Core loop

You glance at a calm list of favorites, slide a thumb down the right edge and the alphabet "waves" to any app, which you open in one motion. Notifications are read and answered inline, without opening the app.

Design rules Niagara states openly, which a faithful clone should keep: one-handed reach first, list instead of grid, no nested folders, no layout lock (deliberate taps instead), and few gesture settings ("stay simple").

## Screens

| ID | screen | route / how to reach | purpose | key components | states seen |
| --- | --- | --- | --- | --- | --- |
| S01 | Welcome / terms | first launch | consent to privacy policy and terms | logo, short intro (video, shortened in v1.16.24), Agree & Continue | default |
| S02 | Pick favorites | onboarding step 2 | seed the home screen | searchable multi-select app list, check FAB bottom-right | none selected, some selected, max reached *(unverified: old limit of 8)* |
| S03 | Set as default launcher | onboarding step 3 | become HOME | "Welcome" prompt + Yes, then the system RoleManager / home-app dialog | accepted, declined (re-prompt) |
| S04 | Notification access card | home screen after onboarding | ask for notification-listener access | tutorial card (Details, Enable, Permit), then the system settings toggle | not granted, granted, restricted-settings error (sideloaded, Android 13+) |
| S05 | Home (favorites) | HOME press, unlock | daily driver | clock + date header (style per theme), optional at-a-glance (weather, next event, battery), media card, favorites list (icon + label + inline notification preview), suggested apps row, contextual rows (headphones → music apps, flashlight on), Niagara Button (bottom-right), wave alphabet (right edge, invisible left edge in left-hand mode) | empty (no favorites, so suggestions only), filled, media playing, notification preview on a row, light/dark, wallpaper blur, two-column (foldable), landscape |
| S06 | App list (A–Z) | slide on the alphabet, or a long scroll from S05 | reach every app | sticky letter headers, app rows (icon, label, inline notification), wave alphabet with magnified letters and selection bubble, footer rows: Recently installed, Niagara Pro, Settings | scrolling, letter selected, `#` bucket, locale extra letters (Å Ä Ö Ñ Æ Ø), pop-up folders shown as categories (Pro), new app badge *(unverified)* |
| S07 | App pop-up | swipe right on any app row | app-level hub | app header + edit icon, notification cards (expand, swipe-left dismiss, inline reply, actions), app shortcuts list, widget / widget stack (Pro) | no notifications (shortcuts only), notifications, low-priority only, widget present, editing |
| S08 | App options sheet | long-press an app row | per-app actions | bottom sheet: icon (tap → S09), label (tap → rename), Favorite / Edit favorites, App info, Advanced › Hide from app list, Uninstall *(unverified)* | in favorites, not in favorites, system app (no uninstall) |
| S09 | Icon picker | S08 › tap icon | change one app's icon | icon pack selector, grid of pack icons, Icon Assistant suggestions (Quick-Edit), search, reset to default | pack covers app, pack lacks app (suggestions), no packs installed |
| S10 | Edit favorites | long-press a favorite › Edit favorites | curate home | draggable rows (four-dot handle), remove, add, Create pop-up folder, widget stack rows | empty, filled, dragging |
| S11 | Pop-up folder | tap a folder row on home, the app list or the Niagara Button | group apps | sheet: title (tap to rename), app and shortcut rows, optional widget / stack (Pro), notifications from members | empty, filled, editing |
| S12 | Header options sheet | long-press clock, date or agenda | configure the top area | Move widget (position slider with arrows), Clock › style, Weather toggle, Calendar › Show upcoming events, Add custom widget, Replace clock with widget, Media player toggle | free (Pro rows badged), Pro |
| S13 | Widget picker + stack | S12 › Add custom widget, or S07 edit › Add widget | host Android widgets | system-style widget list grouped by app, size preview; stack pager with dots, swipe to switch; two-column toggle on wide screens | no widgets, single, stack of 2–4, bind-permission denied, "Can't show content" error |
| S14 | Agenda sheet | tap the date | today and upcoming | events list (time, title, calendar colour), weather now, 24-hour hourly strip, 7-day forecast | no calendar permission, no events, events, weather off, location missing (re-prompt) |
| S15 | Search | long swipe up on home, or HOME press while on favorites | find anything | query field + keyboard, app results (Enter opens the first), calculator result, contact results (call / message) *(conflicting sources)*, web suggestions from the chosen engine | empty query (recents *unverified*), results, no results (web fallback), math result, hidden apps still match |
| S16 | Niagara Button action editor | long-press the Niagara Button › Edit tap action | assign the button | action list (app, shortcut, pop-up folder), swipe-up secondary action (Pro), create pop-up folder | unassigned, app assigned, folder assigned |
| S17 | Settings root | App list footer › Settings | all settings | sections: Themes (Look), Productivity, Features, Advanced, Niagara Pro / Manage membership, Help & feedback | free, Pro |
| S18 | Themes hub | Settings › Themes | pick a look | Ready-made themes (3 free, 18+ Pro), My Themes, Create Theme, Edit Current Theme, Community Themes (Share current / Import) | no custom themes, list, Pro-locked theme |
| S19 | Theme Studio editor | Themes › Create Theme → pick wallpaper | build a theme | live home preview, tabs or rows for Wallpaper, Icons (pack / Anycons / Dots), Clock style, Text (font, size), Accent colour, light/dark; autosave | new, editing, unshareable (system font or external wallpaper) |
| S20 | Font picker | S19 › Text, or Edit Current Theme › letter icon | choose typeface | bundled fonts list with sample text, Add font (.ttf / .otf import), size slider | default, custom imported |
| S21 | Wallpaper picker | S19, or Settings › Look › Wallpaper | set background | built-in collection grid, system picker handoff, apply to Home / Lock / Both, blur toggle | default |
| S22 | Productivity settings | Settings › Productivity | behaviour toggles | Niagara Button, Media Player, Quick Lock (double-tap), Usage Breaker, Notification Summary, Calendar Agenda › Filter events, Smart Search › web engine, haptics, left-hand mode, two-column layout, Add custom widget | toggles off/on, Pro-locked rows, permission-missing rows |
| S23 | App multi-picker | Usage Breaker / Notification Summary / Hidden apps | choose apps for a rule | searchable checklist of apps, extra toggle (e.g. "Summarize silent notifications") | none, some selected |
| S24 | Hidden apps | Settings › Features › Hidden apps | unhide | list of hidden apps with unhide action | empty, filled |
| S25 | Backup & Restore | Settings › Advanced › Backup & Restore | move setups | Create backup, Share backup, Restore backup (SAF file picker for .nlb) | success toast, invalid file, restore confirm |
| S26 | Pro paywall | App list footer › Niagara Pro, or any Pro-badged row | upsell | feature list, plans (monthly / yearly / lifetime, regional), trial (7-day seen), purchase CTA | trial available, subscribed, lifetime |
| S27 | Niagara ID / membership | after purchase, or Settings › Niagara Pro | licence on 3 devices | password-less email sign-in, device list, Manage membership | signed out, signed in, device limit replaced oldest |
| S28 | Recently installed | App list footer | find new apps | list sorted by install date | empty, filled |
| S29 | Permission explainers | when a feature needs a permission | ask clearly | dialog: why + "Read article" / "I have read the article" → system settings deep link. Covers: notification listener, usage access, accessibility, location, calendar, contacts, restricted settings | each permission granted / denied |
| S30 | Usage Breaker reminder | system notification while in a tracked app | nudge, never block | notification: session length + "still want to be here?" | at 5/10/20/30/45/60/90/120/180/240 min |
| S31 | Notification Summary batch | system notification or shade at a batch time | deliver held notifications | batch notification listing held items, clear batch | batch pending, delivered, cleared |

31 screens / sheets (S29 groups ~7 permission dialogs).

## Flows

```
F01 First run
    S01 -> S02 -> S03 (system dialog) -> S05 -> S04 -> system notif-access -> S05
    happy path clicks: ~8 (agree, ~4 favorites, check, yes, set default, enable, toggle, back)
    edge: user declines default launcher; 0 favorites picked (suggestions-only home);
          notification access blocked as "restricted setting" on sideloaded builds; Android Go (no listener)

F02 Open a favorite
    S05 -> tap row -> app
    happy path clicks: 1
    edge: app uninstalled meanwhile (row disappears); work-profile app badge

F03 Open any app with the wave alphabet
    S05 -> thumb-drag on the alphabet (S06 scrolls live) -> lift -> tap app
    happy path clicks: 1 drag + 1 tap
    edge: left-hand mode (left edge); non-Latin names (# bucket / transliteration); foldable two-column;
          HOME within 3 s returns to the last S06 position, repeated HOME returns to S05

F04 Search: app, maths or web
    S05 -> long swipe up -> S15 -> type -> Enter / tap result
    happy path clicks: swipe + 2–3 letters + Enter
    edge: short swipe only scrolls; expression -> calculator row; no app match -> web suggestions;
          Perplexity gives one result only; hidden apps still appear

F05 Read and reply to a notification from home
    S05 (inline preview on row) -> swipe right -> S07 -> expand -> reply field -> send
    happy path clicks: swipe + tap + type + send
    edge: no listener permission (S29); low-priority notifs appear in the pop-up only; DND; swipe-left dismiss;
          conversation with several messages

F06 Edit favorites
    S05 -> long-press favorite -> S08 -> Edit favorites -> S10 -> drag / remove / add -> done
    happy path clicks: 4–5
    edge: removing all (home goes fully adaptive); hide a suggestion via long-press

F07 Create a pop-up folder
    S10 -> Create pop-up folder -> pick apps (S23-style) -> name -> S05 shows folder row -> tap -> S11
    happy path clicks: ~6
    edge: Pro: show the folder as a category in the app list; attach the folder to the Niagara Button; no nesting allowed

F08 Add a widget / build a stack (Pro)
    S05 -> long-press clock -> S12 -> Add custom widget -> S13 -> pick -> (bind permission) -> placed
      second widget -> "Create stack?" -> stack (max 4) -> swipe to switch
    happy path clicks: ~5
    edge: widget configure activity; bind permission denied; widget crash "Can't show content";
          two-column on wide screens; widget inside an app pop-up (S07 › edit › Add widget)

F09 Change an app icon (Icon Assistant, Pro)
    S06 -> long-press app -> S08 -> tap icon -> S09 -> choose suggestion -> back
    happy path clicks: 4
    edge: pack lacks the app; switching packs restores per-pack picks (Auto-Restore)

F10 Rename / hide an app
    S08 -> tap label -> edit -> save   |   S08 -> Advanced -> Hide from app list
    happy path clicks: 3 / 3
    edge: unhide via S24; hidden app still searchable (setting to exclude)

F11 Apply or create a theme
    S17 -> S18 -> ready-made theme -> Apply      (3 clicks)
    S18 -> Create Theme -> S21 wallpaper -> S19 edit icons / clock / font / accent -> autosave -> Apply   (~8)
    edge: Pro-locked themes; wallpaper colour extraction lag; dark/light derivation; Material You colour not dominant

F12 Share / import a theme
    S18 -> Community Themes -> Share current theme -> Android share sheet (.nlt saved to Downloads)
    S18 -> Community Themes -> Import -> SAF picker -> My Themes -> Apply (Pro)
    edge: theme uses the system font or an external wallpaper (cannot share); file over the size limit

F13 Set up Usage Breaker
    S17 -> S22 -> Usage Breaker -> S29 (usage access) -> S23 pick apps -> done; later S30 fires
    happy path clicks: ~6
    edge: usage access missing ("App isn't available"); brief detours do not reset the session; Android < 9

F14 Set up Notification Summary
    S22 -> Notification Summary -> S23 pick apps (+ silent toggle) -> batches arrive (S31)
    edge: no notifications in the window (deliver on the next one); clear batch reschedules 5–6 h out; Android < 8

F15 Enable Quick Lock (double-tap to sleep)
    S22 -> Quick Lock -> S29 accessibility -> system toggle -> double-tap the alphabet
    edge: restricted-setting error; floating accessibility button; unlock within 3 s returns to S06

F16 Turn on weather + calendar
    S05 -> long-press clock -> S12 -> Weather on -> location permission or fixed city -> units
    S12 -> Calendar -> Show upcoming events -> calendar permission -> S14 by tapping the date
    edge: location fails for days (re-prompt); no calendar provider (e.g. Proton); all-day priority rules

F17 Configure the Niagara Button
    S22 -> Niagara Button on -> S05 shows the button -> long-press -> S16 -> pick app / folder (+ swipe-up action, Pro)

F18 Back up and restore
    S17 -> Advanced -> S25 -> Create backup (.nlb to Downloads) -> Share
    new phone: S25 -> Restore -> pick file -> confirm -> relayout
    edge: widgets need re-binding; shortcuts and wallpapers not included; corrupt / older-version file

F19 Upgrade to Pro (original only; the clone uses a debug "Pro" flag)
    S06 footer -> S26 -> plan -> Play billing -> S27 Niagara ID
```

Happy-path numbers to beat: open any app = **1 drag + 1 tap**; open a favorite = **1 tap**; reply to a notification = **swipe + tap + send**.

## Components

| component | variants | states | used on |
| --- | --- | --- | --- |
| AppRow | favorite (large), list (normal), suggestion, search result, folder row | default, pressed, has-notification (inline preview text), dragging, hidden-search | S05 S06 S10 S11 S15 |
| WaveAlphabet | right edge, left edge (invisible, left-hand mode) | idle, touching (wave magnification + selection bubble + haptic tick), double-tap (sleep) | S05 S06 |
| LetterHeader | Latin, locale-extra, `#` | sticky, normal | S06 |
| ClockHeader | ~10–20 clock styles (Pro), plain | with weather, with next event, with battery, moved (position offset), replaced by widget | S05 S19 |
| MediaCard | — | hidden, playing, paused, artwork missing | S05 |
| NotificationCard | compact (inline preview), expanded, messaging-style | unread, expanded, replying, dismiss (swipe-left), action buttons | S05 S06 S07 S11 |
| InlineReplyField | — | empty, typing, sending, sent | S07 |
| BottomSheet | options sheet, pop-up sheet, agenda sheet | collapsed, expanded, dragging-to-dismiss | S07 S08 S11 S12 S14 |
| WidgetHostView | single, stack page | loading, loaded, error "Can't show content", configuring | S05 S07 S11 S13 |
| WidgetStackPager | 1–4 pages, one- or two-column | page n, swiping | S05 S07 S11 |
| NiagaraButton | circle FAB bottom-right | unassigned, app icon, folder, Pro badge | S05 |
| SearchField | — | empty, typing, cleared | S15 |
| CalcResultRow | — | valid, hidden when invalid | S15 |
| WebSuggestionRow | per engine | 1–2 line wrap | S15 |
| ContactRow | call / message actions | — | S15 |
| DragList | four-dot handle | idle, dragging, drop target | S10 |
| AppMultiPicker | checklist + search | none, some, all | S02 S23 |
| SettingsRow | toggle, nav, slider, Pro-locked (badge), permission-missing (warning) | on, off, disabled | S17 S22 S24 S25 |
| ProBadge | small pill | — | S12 S17 S18 S22 |
| ThemeCard | ready-made, my theme, community | applied, locked, selected | S18 |
| LivePreview | scaled home mock | updating | S19 |
| IconGrid | pack icons, suggestions row | selected, missing | S09 |
| ColorSwatchPicker | accent, wallpaper-derived set | selected | S19 |
| FontRow | bundled, imported | selected | S20 |
| PositionSlider | up/down arrows | — | S12 |
| HourlyStrip / DailyForecast | — | loading, stale, no location | S14 |
| PermissionDialog | per permission | default, after-deny (deep link) | S04 S29 |
| Toast / Snackbar | info, undo | — | S10 S25 |
| TutorialCard | inline home card | shown, dismissed | S04 |

## Inferred data model

Everything lives on the device (Room / SQLite plus DataStore). The original has a server only for licensing (Niagara ID) and Icon Assistant suggestions, and the clone needs neither.

```
LauncherApp   component (package/activity), user_handle (profile), label, custom_label?, hidden (bool),
              install_time, last_used_at, launch_count, category? (pop-up folder id)
              evidence: S06, S08 rename/hide, S28 recently installed, suggestions on S05, dual-app article
              confidence: high

HomeItem      id, position, type (app | folder | widget_stack | shortcut), ref_id
              evidence: S10 drag list mixing apps, folders and stacks; favorites article
              confidence: high

PopupFolder   id, title, show_in_app_list (bool, Pro), attached_to_button (bool)
              items: [ {type app|shortcut, ref}, ordered ], widget_stack_id?
              evidence: pop-ups article (free/Pro table, no nesting)
              confidence: high

AppPopupConfig  app_component, hidden_shortcut_ids[], widget_stack_id?, custom_items[] (Pro: "used like folders")
              evidence: S07, hide shortcuts article
              confidence: medium

WidgetStack   id, location (home_header | app_popup | folder), two_column (bool), pages: Widget[] (max 4)
Widget        app_widget_id (AppWidgetHost id), provider_component, min/target size, order
              evidence: widgets article, foldable article
              confidence: high

Shortcut      package, shortcut_id, user_handle, label, pinned (bool)
              evidence: pop-ups, hide shortcuts (via LauncherApps shortcuts API)
              confidence: medium

IconOverride  app_component, icon_pack_id, drawable_name | custom_uri
              evidence: Icon Assistant "remembers per icon pack" (Auto-Restore)
              confidence: high

Theme         id, name, source (ready_made | custom | community), wallpaper_ref, wallpaper_blur,
              icon_pack_id, clock_style_id, font_ref, font_scale, accent_color?, mode (light|dark|auto),
              created_at, updated_at
              evidence: Theme Studio articles, theme sharing (.nlt bundles font + wallpaper), color system
              confidence: medium (exact fields guessed)

HeaderConfig  clock_style_id, position_offset, show_weather, show_events, replaced_by_widget_id?,
              media_card_enabled
              evidence: S12 sheet, move clock, custom clock
              confidence: medium

WeatherConfig mode (current | fixed), fixed_location {name, lat, lon}?, units (C|F), last_fetch_at
WeatherCache  fetched_at, current, hourly[24], daily[7]
              evidence: weather article
              confidence: high

CalendarFilter  excluded_calendar_ids[]
              evidence: calendar FAQ (filter events, Pro)
              confidence: high

UsageBreakerRule  app_component, enabled
UsageSession  app_component, started_at, last_seen_at, reminders_fired[] (minutes)
              evidence: Usage Breaker ladder, "short detours don't reset"
              confidence: medium (session model guessed)

SummaryRule   app_component | ALL_SILENT
NotificationBatch  id, scheduled_for, delivered_at?, cleared_at?, items[] (notification keys)
              evidence: Notification Summary article (5.5–6.5 h, top of the hour)
              confidence: medium

Settings      key/value: left_hand_mode, haptics, swipe_up_search, web_engine, quick_lock,
              niagara_button {enabled, tap_action, swipe_action}, hidden_in_search, two_column, ...
              evidence: S17/S22 rows
              confidence: high

Entitlement   is_pro (in the clone: a debug flag); original: niagara_id, plan, devices[≤3]
              evidence: purchase help
              confidence: high (original), n/a for the clone

BackupFile (.nlb)  versioned JSON (likely zipped) of all of the above except wallpapers, widget bindings
              and most shortcuts
ThemeFile (.nlt)   Theme + embedded font + wallpaper, with a size cap
              evidence: backup and theme-sharing articles
              confidence: medium (format guessed; design your own)
```

Relationships: HomeItem n→1 (LauncherApp | PopupFolder | WidgetStack | Shortcut). PopupFolder 1-n items and 0-1 WidgetStack. WidgetStack 1-n Widget (≤4). LauncherApp 0-1 AppPopupConfig, 0-n IconOverride (one per pack). Theme 1-1 active. UsageBreakerRule 1-n UsageSession. SummaryRule n-n NotificationBatch via notification keys.

Live system data that is read but not stored: notifications (NotificationListenerService), media sessions (MediaSessionManager), calendar events (CalendarContract), contacts, usage stats (UsageStatsManager).

## Feature matrix

See `features.csv`. Must: 29, should: 25, could: 19, skip: 9 (82 rows). Tier (free / Pro) is in the `notes` column.

## Out of scope (cannot or should not be cloned)

- **Niagara's artwork:** the Niagara Dots and Anycons icon art, the built-in wallpapers, the ready-made and artist themes, clock-style designs and the logo. Make your own or use permissively licensed sets.
- **Anycons ML categorisation and Icon Assistant's crowd-sourced suggestions.** These come from the original's server and its users' data. A local heuristic ("icons of similar apps in the pack") is buildable, but the crowd data is not.
- **The community:** the Discord #community-themes hub and its themes. You can rebuild the `.nlt`-style share/import mechanism with your own format, but not the community.
- **Niagara ID licensing, Play billing, invite rewards, regional pricing.** A personal project needs none of these; gate Pro features behind a debug flag.
- **Sesame integration.** Discontinued by the original, and depends on a third-party app.
- **OpenWeather data under their key.** Weather is buildable with your own free API key (OpenWeather or Open-Meteo); their key and paid plan are not yours.
- **Fonts they bundle under licence.** Ship OFL fonts (e.g. Google Fonts) instead.
- **"Kagi" signed-in search.** Depends on a user's paid account; plain engines are fine.
- **True app locking or hiding from the OS.** Not possible for a non-root launcher (Niagara says so too). "Hide from list" is the honest version.

## Size

Screens 31 (about 38 including the permission dialogs), flows 19, entities 18.

Hard parts, hardest first:
1. **Wave alphabet + gesture arbitration.** The custom touch handling on the edge strip (magnification wave, haptic ticks, live list scrolling), plus telling apart a quick scroll, a long swipe up (search), a swipe down (shade, via reflection / `StatusBarManager` expand) and a swipe right on a row (pop-up). This is the product's soul and the hardest to get to *feel* right.
2. **Inline notifications.** NotificationListenerService mapping notifications to app rows, inline previews, expand, RemoteInput replies, dismiss, the notification-summary scheduler (hold and re-post batches), and Android 13+ restricted-settings handling for sideloaded builds.
3. **AppWidgetHost with stacks and pop-up placement.** Binding permission, configure activities, resizing, the stack pager, two-column layout, and re-binding after restore.

Also non-trivial: icon-pack parsing (the de facto ADW / Nova `appfilter.xml` convention is public), a wallpaper palette extractor (Palette / Material Color Utilities, both open source), Usage Breaker (UsageStatsManager polling or a foreground check with a session model), Theme Studio with live preview, locale-aware alphabet bucketing (ICU `AlphabeticIndex`), the calculator parser, and a backup format with versioning.

Size: **L (a quarter)** for core + Pro, part-time. The free core alone (S01–S08, S10–S11, S15, S17, S22, S25, F01–F06) is **M (a few weeks)**. Suggested slice for the first build: F02 + F03 + F05, which is home, wave alphabet and inline notifications. That is the loop; everything else is configuration on top.

Platform note: this must be a **native Android app** (Kotlin + Jetpack Compose, or Views for the hot list). A launcher needs the HOME intent filter, LauncherApps, AppWidgetHost and a NotificationListenerService, and none of those are reachable from a web or cross-platform shell without heavy native modules.
