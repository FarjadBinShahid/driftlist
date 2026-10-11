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
| S09 | Icon picker | 2026-10-11 | partial | not tested with a real icon pack (needs a Play install) | — |
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
- Backup saved as JSON, app data wiped, onboarding run, backup restored with everything back.
