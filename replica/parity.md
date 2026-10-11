# Parity: Driftlist 0.2.0 (build 932fb1c) vs Niagara Launcher

Date: 2026-10-11. Tools: `replica/parity.py` (features), `replica/imgdiff.py` (layout, not run; see below).

## Verdict: not shippable yet, but close on the core

| | |
| --- | --- |
| **Overall** | **60.9 / 100** (feature score; there is no layout score) |
| Must-haves | **27 of 29** done, 2 partial |
| Open bugs | no S1, no S2; one S4 (BUG-011) |
| Rule | "shippable" needs all must-haves done and a score of 80+. Two must-haves are partial and the score is under 80. |

The core loop is at parity: favorites, the wave alphabet, inline notifications with reply, search, folders, rename and hide, icon packs, a widget, Time checks, and backup. The gap is almost entirely **milestone 3**:
- theming (18%)
- widgets with data, i.e. calendar and weather (25%)
- the smaller home-screen extras

## Feature score by area (weakest first)

| area | score | what's missing |
| --- | --- | --- |
| accessibility | 0 | left-hand mode |
| theming | 18 | themes, Theme Studio, fonts, clock styles, wallpaper palette |
| widgets | 25 | calendar, weather, stacks |
| layout | 25 | two-column foldable layout; landscape only partial |
| search | 56 | calculator, live web suggestions, contacts |
| home | 58 | media card, suggestions row, action button, header slider |
| pop-ups | 63 | shortcuts in folders, custom app pop-ups |
| gestures | 70 | Quick Lock; return to list position within 3 s |
| app list | 75 | Recently installed |
| wellbeing | 75 | Notification Summary |
| core | 77 | permission explainer dialogs, full work-profile handling |
| icons | 94 | learning icon suggestions |
| onboarding, notifications, settings, backup | 100 | — |

8 rows are left out on purpose: Niagara's artwork, its server-side data, its community, accounts and payments, and Sesame. The full missing list in build order is in the parity tool output: `python replica/parity.py replica/features.csv`.

## Layout diff: not run

There are no screenshots of the original in `replica/screens/`; the recon was public-source only, by your choice. Comparing against nothing would produce a made-up number, so there is no layout score. To get one, either:
- save Niagara's public Play Store screenshots into `replica/screens/` (reference only, never shipped), or
- screenshot Niagara on a phone you own.

Then run `python replica/imgdiff.py replica/screens/S05.png replica/clone-screens/S05-home.png --out replica/diffs/S05.png`.

Driftlist's own screenshots for every built screen are already in `replica/clone-screens/`.

## Behaviour diff

Driftlist's side was walked on the emulator. Niagara's side comes from its help center and walkthroughs in `recon.md`, not from using it.

| flow | Niagara | Driftlist | fix or keep |
| --- | --- | --- | --- |
| F02 open a favorite | 1 tap | 1 tap | keep |
| F03 open any app | 1 drag on the alphabet + 1 tap | same | keep |
| F05 reply to a message | swipe right › Reply › type › send | same | keep |
| F04 search | long swipe up, or Home on home | same; Enter opens the top result | keep |
| F01 first run | terms › pick favorites › set default › notification card | welcome › pick favorites › set default › notification card (no terms step) | keep: one tap fewer |
| F10 hide an app | long-press › Advanced › Hide from app list (2 taps) | long-press › Hide from app list (1 tap) | keep: simpler |
| F06 edit favorites | long-press a **favorite** › Edit favorites | long-press the **clock** › Edit favorites; long-pressing a favorite only offers "Remove from favorites" | **fix**: add "Edit favorites" to a favorite's options sheet |
| F07 create a folder | Edit favorites › Create pop-up folder | Edit favorites › New folder | keep |
| F08 add a widget | long-press clock › Add custom widget | long-press clock › Add a widget | keep |
| F18 backup | one tap, file goes to Downloads | system file picker: you choose where it goes (one extra tap) | keep: you choose the location |
| F13 Time checks | 5/10/20…240 min ladder, Pro only | same ladder, free | keep |
| Pro features | ~14 features behind a paywall | everything free | keep |
| Icon picker | Icon Assistant learns from other users' picks | name-matched icons first, nothing leaves the phone | keep (privacy) |
| HOME from an app within 3 s | returns to the app list where you were | returns to favorites | fix later (should-have) |

## Top five to build next

1. **Close the two must-haves.** Shortcuts inside folders, and real permission explainer dialogs (including the "restricted settings" steps for sideloaded installs on Android 13+). Together with "Edit favorites" in the favorite sheet, that's a small milestone, and it brings must-haves to 29 of 29.
2. **Calendar and weather** (S14 agenda sheet, events and weather in the header): two should-haves, +4 weighted points. Open-Meteo needs no key.
3. **Theming, part 1:** wallpaper-derived colours, three or four clock styles of our own, and bundled OFL fonts. These are the cheapest theming wins.
4. **Home extras:** media card, suggested apps row, and the shortcut button. Three should-haves, all small.
5. **Notification Summary, Quick Lock and widget stacks:** the remaining should-haves with real user value.

Items 1 to 5 would take the feature score from about 61 to about 80, the "shippable" line. Theme Studio and community-style theme sharing are the biggest remaining single items, and are best done last.

## Next

`/replica-build` for item 1 (and then 2 to 5). `/replica-entrepreneur` once parity is near 80, so the clone also fixes what Niagara users complain about. That is what turns "a copy" into "better than the original".
