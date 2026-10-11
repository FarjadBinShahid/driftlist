# Parity: Driftlist 0.2.0 (build 932fb1c) vs Niagara Launcher

Date: 2026-10-11. Tools: `replica/parity.py` (features), `replica/imgdiff.py` (layout, not run; see below).

## Verdict: not shippable yet, but close on the core

| | |
| --- | --- |
| **Overall** | **60.9 / 100** feature parity; home layout **15.2** (different, see L1 to L7) |
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

## Layout diff (home screen S05)

Reference images, all approved by you and kept git-ignored in `replica/screens/source/`. None of them ship.

- `niagara-hero.png`: a straight-on Niagara home screen in a phone frame. This is the only one usable for the automatic diff.
- `hero-image-mobile.png`, `hero-image-tablet-1.png`: niagaralauncher.com hero photos.
- `play-find-apps-faster.png`: a Play Store listing image of the wave alphabet.
- `review-photo-mon-clock.jpg`: a review photo of the "MON" clock style with a world-clock widget.

| screen | score | verdict |
| --- | --- | --- |
| S05 home | 15.2 | different |

How it was measured: Niagara's screen was cropped from its frame (the bottom ~27% is outside the image), and Driftlist's home was cropped to the same proportion. Then `imgdiff.py`, layout mode (edges, 12 columns). The JSON is in `replica/diffs/S05.json`; the diff image is local only.

Caveat: the two wallpapers differ, and edge maps count wallpaper texture, so the score understates the similarity. The real structural differences, by eye across all five references:

| # | Niagara | Driftlist | parity fix? |
| --- | --- | --- | --- |
| L1 | Favorites flow **directly under the clock**, in the upper middle; empty space below | Favorites anchored to the **bottom**; empty space between clock and favorites | Yes. Biggest layout gap. Option: "favorites under the clock" as the default, with bottom-anchored as a setting |
| L2 | Alphabet shows **only letters that have apps**, as a short strip spanning roughly the favorites area | Full-height strip with all 27 letters; empty ones faded | Yes: drop empty letters, shorten the strip |
| L3 | ☆ at the top of the alphabet (back to favorites), ○ at the bottom (settings) | Neither; settings is the last row of the list | Yes: cheap |
| L4 | Wave arcs the letters out to the left in a curve, bubble just left of the finger | Same idea; similar curve and bubble | Keep; tune after a side-by-side on device |
| L5 | Notification preview reads "Gmail • 2h" / "5 new emails" plus a small › button on the row | "Gmail: 5 new emails" on one line, no time, no button | Yes: add the time, and a › button that opens the pop-up (also good for TalkBack) |
| L6 | Clock: time, then date + weather icon + temperature on one line; several clock styles ("TUE", "MON" with world clocks) | Time + date only | Milestone 3: weather and clock styles (already in the top five) |
| L7 | Smaller labels and icons on favorites (about 16 sp next to a 40 dp icon) | 22 sp favorite labels | Tune: try 18 sp |

## Measured on the user's own phone (Niagara 1.16.31, Galaxy S23)

You drove the phone and I captured screenshots plus the accessibility layout (positions and sizes only; no app files read). Captures stay local in `replica/screens/niagara-phone/`. The layout numbers and Niagara's UI labels are in `replica/reference-measurements.md`.

Layout diff, phone home vs Driftlist home: **17.5 (different)**. The two were in different states: the phone has a calendar widget and two suggestions, Driftlist has five favorites and a different wallpaper. So the number is mostly noise; the measurements below are what matter.

| # | Niagara (measured) | Driftlist now | change |
| --- | --- | --- | --- |
| L1 | Content column at x 40 dp, 262 dp wide. Clock top at 101 dp (13%); widget directly under the date; favorites/suggestions directly under the widget | Column at 24 dp; favorites pinned to the bottom | Make "under the clock" the default; 40 dp column |
| L2 | Alphabet: only letters that have apps; ☆ top, ○ bottom; at rest it sits level with the widget down to ~704 dp | Full height, all 27 letters | As measured |
| L8 | **App pop-up is a floating card at the swiped row** (280 dp wide, 48 dp header with an "open" arrow plus the name, 52 dp rows with 36 dp icons), not a bottom sheet | Bottom sheet | Rebuild as an anchored card |
| L9 | **List rows equal favorite rows**: 54 dp, 42 dp icon, label at 108 dp, ~16 sp; letter header 43 dp | List 48 dp / 32 dp icon / 18 sp; favorites 56 dp / 40 dp / 22 sp | One row size, 54 / 42 / 16 sp |
| L10 | **Wave**: letters slide left in a V across the *whole* strip (peak ~140 dp at the finger, roughly linear falloff), barely scaled; 48 dp bubble at the finger; the section header lands ~1/3 down the screen | Cosine over 4 letters, scale 2.4×, 28 dp shift; header scrolled to the top | Match the shape; scroll the header to 1/3 |
| L11 | Long-press sheet: 36 dp icon + 24 sp name; rows of 52 dp with leading icons: Favorite / App info / Screen time / Add to category / Uninstall / Advanced ▸ (Edit pop-up, Edit icon/name, Hide). Suggestions add "Stop suggesting" first; footer links to settings | Text-only rows, all visible, no Advanced group | Icons; group the rarer actions under Advanced; "Stop suggesting" for suggestions |
| L12 | Settings: six top-level pages (Productivity, Themes, Pro, Help, Community, Advanced) with a big centred title that collapses into a pinned pill bar; sub-pages grouped into sections, switches with two-line descriptions | One long page | Split into sub-pages |
| L13 | Clock long-press → "Niagara widget" sheet of rounded **cards** (Clock style, Calendar, Weather, Add Custom Widget, Move Widget, Media, Battery %) | Plain text rows | Card rows |
| L14 | Search: pill field with a ⋮ menu; empty state shows nothing; the top result says "Tap Search to open"; last row "Search for more apps" (Play) | "Most used" when empty; web search row | Add the hint and a Play Store row; keep "Most used" (deliberately different) |
| L15 | Round search button bottom-right while in the app list; Niagara Button (56 dp) bottom-right on home | No button | Milestone 3 (Niagara Button already planned) |

New features found on the phone: sort the app list by usage within each letter, a Quick Replies on/off setting, a Screen time shortcut in app options, and a home-screen rotation setting. All four are added to `features.csv` as could-haves. Feature parity is now 60.1 with 77 rows counted; the score dipped slightly because of the new rows.

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

1. **Close the two must-haves, and the measured layout gaps L1, L2, L3, L5 and L8 to L11.** That means the column and position, the alphabet, the anchored pop-up card, one row size, the real wave shape, and the long-press sheet with icons. Gaps: favorites under the clock, only letters that have apps, the ☆ and ○ markers, and the notification time plus › button. Shortcuts inside folders, and real permission explainer dialogs (including the "restricted settings" steps for sideloaded installs on Android 13+). Together with "Edit favorites" in the favorite sheet, that's a small milestone, and it brings must-haves to 29 of 29.
2. **Calendar and weather** (S14 agenda sheet, events and weather in the header): two should-haves, +4 weighted points. Open-Meteo needs no key.
3. **Theming, part 1:** wallpaper-derived colours, three or four clock styles of our own, and bundled OFL fonts. These are the cheapest theming wins.
4. **Home extras:** media card, suggested apps row, and the shortcut button. Three should-haves, all small.
5. **Notification Summary, Quick Lock and widget stacks:** the remaining should-haves with real user value.

Items 1 to 5 would take the feature score from about 61 to about 80, the "shippable" line. Theme Studio and community-style theme sharing are the biggest remaining single items, and are best done last.

## Next

`/replica-build` for item 1 (and then 2 to 5). `/replica-entrepreneur` once parity is near 80, so the clone also fixes what Niagara users complain about. That is what turns "a copy" into "better than the original".
