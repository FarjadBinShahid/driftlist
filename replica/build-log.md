# Build log

One line per screen. Screenshots are in `clone-screens/`, taken on the `Medium_Phone_API_37.0` emulator (1080×2400).

| ID | screen | date | status | missing | harder than expected |
| --- | --- | --- | --- | --- | --- |
| S01 | Welcome | 2026-10-11 | done | no terms screen (there are no terms to accept) | — |
| S02 | Pick favorites | 2026-10-11 | done | — | — |
| S03 | Set as default | 2026-10-11 | done | — | — |
| S04 | Notification access card | 2026-10-11 | done | restricted-settings walkthrough for sideloaded installs on Android 13+ | — |
| S05 | Home (favorites) | 2026-10-11 | partial | suggestions row, media card, action button, contextual rows (M3) | readable text over light wallpapers needed a stronger scrim plus a text shadow |
| S06 | App list A–Z | 2026-10-11 | partial | Recently installed footer, folders as categories | — |
| S07 | App pop-up | 2026-10-11 | done | widgets inside pop-ups (could) | inline reply: the reply intent needs `RemoteInput.addResultsToIntent`, otherwise the app gets an empty reply |
| S08 | App options | 2026-10-11 | done | — | — |
| S09 | Icon picker | 2026-10-11 | done | — | parsing the pack's appfilter.xml happens on first open; fine with Arcticons (~10k icons), watch for slow packs |
| S10 | Edit favorites | 2026-10-11 | done | — | drag reorder written by hand (Compose has no reorderable list) |
| S11 | Pop-up folder | 2026-10-11 | partial | shortcuts inside folders | — |
| S12 | Header options | 2026-10-11 | partial | clock styles, weather, calendar, position slider (M3) | — |
| S13 | Widget picker + host | 2026-10-11 | partial | stacks (M3) | widgets rendered blank until the host reported their size on layout; configure screens can only be launched by the host and come back through the deprecated `onActivityResult` |
| S14 | Agenda sheet | — | not started | M3 | — |
| S15 | Search | 2026-10-11 | partial | calculator, contacts, live web suggestions (M3) | — |
| S16 | Action button editor | — | not started | M3 | — |
| S17 | Settings | 2026-10-11 | done | — | — |
| S18–S21 | Themes, Theme Studio, fonts, wallpapers | — | not started | M3 | — |
| S22 | Productivity settings | 2026-10-11 | partial | only Time checks, search and icon pack so far | — |
| S23 | App multi-picker | 2026-10-11 | done | — | — |
| S24 | Hidden apps | 2026-10-11 | done | — | — |
| S25 | Backup & Restore | 2026-10-11 | done | — | restore must drop rows for apps not installed on this phone, or foreign keys fail |
| S26–S27 | Paywall / account | — | skip | personal project, no payments | — |
| S28 | Recently installed | — | not started | should-have | — |
| S29 | Permission explainers | 2026-10-11 | partial | dedicated dialogs; today it's cards plus settings rows | — |
| S30 | Time-check reminder | 2026-10-11 | done | screenshot not captured (the notice expires after 60 s) | the session survives short detours; verified 5- and 10-minute reminders by the notification channel timestamps |
| S31 | Notification Summary | — | not started | M3 | — |

## Milestone 3a (2026-10-12): measured layout + last must-haves

| ID | screen | status | notes |
| --- | --- | --- | --- |
| S05 | Home | done (layout) | favorites under the clock by default, with a "favorites at the bottom" setting; 40 dp column; clock at 13%; one 54 dp row size; notification age + > button. Media card, suggestions and shortcut button are still M3b |
| S06 | App list + wave | done | star / letters with apps / settings circle at ~18 dp each in the lower band; V-shaped sideways wave; section a third of the way down |
| S07 | App pop-up | done | anchored card (280 dp) at the swiped row; long-press a shortcut to add it to a folder |
| S08 | Options | done | icons, Edit favorites, More group (rename, icon, hide), settings link |
| S11 | Folder | done | shows shortcuts after apps; remove per shortcut; shortcuts pinned and included in backups |
| S15 | Search | done (M2 scope) | "Enter opens this" on the top result; Google Play row |
| S29 | Permission explainers | done | notification and usage access, with the restricted-settings steps for sideloaded installs |

Harder than expected: Compose's Popup positions in window coordinates, so the card has to be placed from the row's window bounds, not its parent. Letters spread across the whole band on phones with few apps until the slot size was fixed at the measured ~18 dp.

## Milestone 3b part 1 (2026-10-12): home and behaviour should-haves

| ID | status | notes |
| --- | --- | --- |
| Suggested apps | done | top 3 most-opened non-favorites; Stop suggesting sets `hidden_from_suggestions` |
| Music card | done | MediaSessionManager through the notification listener component; not tried with a real player on the emulator |
| Widget stacks | done | HorizontalPager, dots, accessibility next/previous; header sheet removes the page on screen |
| Action button | done | app or favorites folder; long-press to change; shortcut and swipe-up targets not built |
| List search button, Recently installed | done | footer row with count, sheet of the last 14 days |
| Left-hand mode | done | first pass left the 40 dp gutter beside the strip; mirrored the padding after seeing it on the emulator |
| Home within 3 s | done | `listLaunchAt` in the view model; verified both sides of the 3 s line |
| Quick Lock | partial | accessibility service that only calls GLOBAL_ACTION_LOCK_SCREEN; toggle and explainer verified, the lock itself needs the user to switch the service on |
| Notification Summary | done | Room v3 (summary_rule, notification_batch, batch_item) with a v2 to v3 migration test; first delivery at +5.5 h rounded up to the hour, `AlarmManager.setWindow`; delivery not observed end to end |
| Settings sub-pages (L12) | done | Home screen, Gestures and search, Look, Wellbeing, Backup, About |

Harder than expected: Notification Summary. Holding a notification means cancelling it from the shade inside the listener, so the batch has to keep enough (title, text, package) to rebuild a useful summary later.

Tests: 9 unit, 23 instrumented, all passing. Onboarding copy fixed: favorites now sit under the clock by default, not at the bottom.

## Milestone 3b part 2 (2026-10-12): theming

| ID | status | notes |
| --- | --- | --- |
| Wallpaper palette | done | WallpaperWatcher follows the wallpaper's colours (API 27+); `Palette` walks lightness until every pair passes AA (unit test, 12 seeds incl. yellow, white, black, grey) |
| Accent presets | done | eight, plus From wallpaper |
| Clock styles | done | Thin, Bold, Stacked, Compact, Mono; our own, type only |
| Fonts | done | nine system families (nothing downloaded or bundled) plus .ttf/.otf import into app storage, checked and capped at 8 MB |
| Text size | done | 85-130% for text on the wallpaper; sheets keep the system font so settings stay readable |
| Light/dark override | done | sheets and settings; status bar icons follow it (bug found on the emulator: dark icons on dark settings, fixed) |
| Ready-made themes (S18) | done | six, one tap |
| Theme Studio (S19) | partial | live preview, autosave; wallpaper isn't part of a theme |
| My themes | done | Room v4 `theme` table (migration test), in backups |
| Move clock | done | header sheet slider updates home live; also in settings |

Found on the emulator: the preview clipped its third sample row (now two). Tests: PaletteTest (3), LookTest (4), ThemeFlowsTest (2, accessibility checks on), Migration v3 to v4, Backup keeps themes and the look. Totals: 22 unit, 29 instrumented, all passing.

## Calendar (2026-10-12)

| ID | status | notes |
| --- | --- | --- |
| S05 next event | done | under the date with the calendar's colour; "In 25 min", "Until 10:30", "14:30", "Tomorrow 09:00", "Today" for all-day; long ongoing events (12 h+) don't hide what's next |
| S14 agenda sheet | done | tap the date: 7 days grouped Today / Tomorrow / weekday, all-day first, multi-day events on every day they touch; New event and Open calendar |
| Calendar filter | done | Settings > Home screen > Calendars to show |
| Weather | skip | user decision |

Read straight from CalendarContract.Instances on every change (ContentObserver) and every 15 minutes; nothing is stored. Found on the emulator: opening an event with an untyped content URI also matched Messages and the Google app, so the intent now carries `vnd.android.cursor.item/event`. Google Calendar on the emulator has no account, so it closes on a shell-made local calendar; the hand-off itself is verified in the log.

Tests: AgendaTest (6 unit), AgendaSheetTest (2 instrumented, accessibility checks on). Totals: 15 unit, 25 instrumented, all passing.

## Bugs found and fixed during the milestone 2 run-through

1. **Upgrading from 0.1.0 would have wiped favorites.** Onboarding showed for anyone without the new "onboarding done" flag, and finishing it replaced the favorites. Now it's skipped and marked done when favorites already exist.
2. **Hosted widgets drew nothing.** The provider was never told the widget's size. Fixed by reporting the size from a layout listener.
3. **A Home press during onboarding opened search right after it.** Home presses are now ignored while onboarding is showing.
4. **The search overlay let the clock show through.** It's now fully opaque.

## Verified on the emulator

- Database upgrade v1 → v2 over a real 0.1.0 install; the favorite survived.
- Search ranking.
- Long swipe up to search; Home on home opens search.
- Edit favorites: add, drag reorder.
- Create a folder and open it.
- Inline SMS preview, then reply from the pop-up (the reply arrived in the Messages app's SMS store).
- App shortcuts in the pop-up.
- Rename: re-sorts under the new letter.
- Hide and unhide.
- Settings pages.
- Widget bind and configure, then the widget renders.
- Icon pack (Arcticons Material You 15.3.2): applied to the list, a per-app pick, and the pick was remembered after switching packs.
- Backup saved as JSON, app data wiped, onboarding run, backup restored with everything back.
