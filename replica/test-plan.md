# Test plan: Driftlist 0.2.0

## Results (2026-10-11, build 03e1585)

| layer | cases | passed | failed |
| --- | --- | --- | --- |
| Unit tests (JVM): search ranking, wave maths | 6 | 6 | 0 |
| Instrumented tests (emulator): Room, migration, backup, icon cache, home flows with accessibility checks | 21 | 21 | 0 |
| Manual on the emulator | 31 | 30 | 1 (BUG-011, S4, open) |

Bugs: S1 ×1, S2 ×1, S3 ×5, S4 ×4. Ten fixed, one open (S4). No open S1 or S2. Details are in `bugs.md`.

Run the automated suite:

```bash
./gradlew testDebugUnitTest connectedDebugAndroidTest
```

The debug build installs as `dev.farjad.driftlist.debug`, so it sits next to the release build, and the tests make it the home app while they run.

Flows come from `replica/recon.md`. Case IDs: **H** happy path, **E** edge case, **N** negative case.

How each case is tested:
- **auto-ui**: Compose UI test on the emulator (`app/src/androidTest/.../ui`)
- **auto-db**: Room / backup instrumented test (`app/src/androidTest/.../data`)
- **unit**: JVM unit test (`app/src/test`)
- **manual**: driven on the emulator by `adb`, with a screenshot in `clone-screens/`
- **M3**: feature not built yet, so not tested

Web-only items from the generic checklist don't apply to an on-device launcher: two tabs, refresh mid-flow, expired session, second user's data, payments.

The Android equivalents are used instead:
- process death and reinstall
- app upgrade
- work profile
- font scale
- dark mode
- landscape
- TalkBack labels
- permission revoked

Emulator: Medium_Phone_API_37.0, 1080×2400, Android 17 (API 37).

## F01 First run

| ID | case | expected | how |
| --- | --- | --- | --- |
| F01-H1 | Fresh install → Get started → pick 2 apps → Done | Home shows the 2 favorites in pick order | auto-ui |
| F01-H2 | Skip on the favorites step | Home with the empty-favorites hint | auto-ui |
| F01-E1 | Search inside the picker narrows the list; ticks survive clearing the search | Selected count unchanged | auto-ui |
| F01-E2 | Press Home during onboarding | Onboarding stays; no search opens afterwards | manual (bug B03, fixed) |
| F01-E3 | Upgrade from 0.1.0 with favorites | No onboarding, favorites kept | auto-db (migration) + manual |
| F01-E4 | Font scale 1.5 | Text wraps, buttons reachable | manual |
| F01-N1 | Decline the default-launcher prompt | Home shows the "Make Driftlist your home screen" card | manual |
| F01-N2 | Notification access never granted | Card stays; no previews; nothing crashes | manual |

## F02 Open a favorite / F03 Open any app with the wave alphabet

| ID | case | expected | how |
| --- | --- | --- | --- |
| F03-H1 | Slide the alphabet to a letter | List shows that letter's section | auto-ui |
| F03-H2 | Tap an app in the list | App launches, launch count increments | auto-db (markLaunched) + manual |
| F03-E1 | Letter with no apps | Selection snaps to the nearest letter that has apps | unit (`Wave.nearestEnabled`) |
| F03-E2 | Renamed app | Sorted under its new name's letter | auto-db (`buildList`) + manual |
| F03-E3 | Accented and non-Latin names | Accents ignored when searching; non-Latin names go under # or are transliterated | unit (search) / manual |
| F03-E4 | App uninstalled while shown | Row disappears; favorite removed | auto-db (sync cascade) |
| F03-E5 | Back from the app list | Returns to favorites | auto-ui |
| F03-E6 | 60-character app name | Ellipsized on one line, no layout break | auto-ui |
| F03-E7 | Landscape | No crash; list usable | manual |

## F04 Search

| ID | case | expected | how |
| --- | --- | --- | --- |
| F04-H1 | Type part of a name | Matching app is first; Go launches it | auto-ui + unit |
| F04-H2 | Long swipe up on home | Search opens with the keyboard | manual |
| F04-H3 | Home pressed while on home | Search opens | manual |
| F04-E1 | No match | "No apps match" + web search button | auto-ui |
| F04-E2 | Initials ("gm") / accents ("cafe") | Found | unit |
| F04-E3 | Hidden app with "find hidden apps" on | Found and marked Hidden | auto-ui |
| F04-E4 | Same with the setting off | Not found | auto-ui |
| F04-E5 | Emoji in the query | No crash, "No apps match" | auto-ui |
| F04-N1 | Calculator / contacts / live web suggestions | — | M3 |

## F05 Read and reply to a notification

| ID | case | expected | how |
| --- | --- | --- | --- |
| F05-H1 | Incoming SMS | Inline preview under Messages | manual |
| F05-H2 | Swipe right → Reply → Send | "Sent"; the reply lands in the SMS store | manual |
| F05-E1 | Swipe a notification left | Dismissed from the pop-up and the shade | manual |
| F05-E2 | App with no notifications | "Nothing new here." plus shortcuts | manual |
| F05-N1 | Notification access revoked | Previews disappear, card returns, no crash | manual |
| F05-N2 | Reply intent cancelled by the app | "Couldn't send" message | to check (hard to trigger) |

## F06 Edit favorites

| ID | case | expected | how |
| --- | --- | --- | --- |
| F06-H1 | Add apps → reorder by drag → Done | Home order matches | manual + auto-db (reorder) |
| F06-H2 | Remove a favorite | Gone from home, still in the list | auto-ui |
| F06-E1 | Add the same app twice | Only one favorite | auto-db (unique) |
| F06-E2 | TalkBack Move up/down actions | Order changes without dragging | manual (labels present) |

## F07 Folder

| ID | case | expected | how |
| --- | --- | --- | --- |
| F07-H1 | New folder with a name and apps | Folder row on home; tap opens its apps | auto-db + manual |
| F07-E1 | Empty name | Saved as "Folder" | auto-db |
| F07-E2 | App in a folder gets uninstalled | Removed from the folder | auto-db (cascade) |
| F07-E3 | Remove the folder | Folder and its home row gone; apps untouched | auto-db |

## F08 Header widget

| ID | case | expected | how |
| --- | --- | --- | --- |
| F08-H1 | Add widget → allow → configure → shows | Widget rendered under the clock | manual (bug B02, fixed) |
| F08-E1 | Replace the widget | Old id released, new one shown | manual |
| F08-N1 | Cancel the bind dialog | Nothing added; id released | manual |
| F08-N2 | Widget provider uninstalled | "This widget stopped working" message | to check |

## F09 Icon / F10 Rename and hide

| ID | case | expected | how |
| --- | --- | --- | --- |
| F09-H1 | Pick a pack, then a per-app icon | Applied; remembered after switching packs | manual (Arcticons) |
| F09-N1 | No pack installed | Picker explains and links to settings | manual |
| F10-H1 | Rename | New label shown everywhere | auto-ui |
| F10-E1 | Rename to emoji / accents / 60 chars | Shown, ellipsized | auto-ui |
| F10-E2 | Rename to blank | Reverts to the system name | auto-ui |
| F10-H2 | Hide, then unhide from settings | Disappears from the list, comes back | auto-ui |

## F13 Time checks

| ID | case | expected | how |
| --- | --- | --- | --- |
| F13-H1 | Enable, pick an app, use it for 10 min | Reminders at 5 and 10 min | manual (verified by notification timestamps) |
| F13-N1 | Usage access missing | Warning row with a deep link | manual |
| F13-N2 | Notification permission denied | Warning row; no crash | manual |

## F18 Backup and restore

| ID | case | expected | how |
| --- | --- | --- | --- |
| F18-H1 | Save, then restore over a different setup | Everything back | auto-db + manual |
| F18-E1 | Backup contains an app not installed here | Skipped, count reported | auto-db |
| F18-N1 | Not a Driftlist file | "Not a Driftlist backup", nothing changed | auto-db |
| F18-N2 | Newer backup version | Refused | auto-db |

## Platform cases

| ID | case | expected | how |
| --- | --- | --- | --- |
| P-1 | Database upgrade v1 → v2 | Data kept | auto-db (MigrationTestHelper) |
| P-2 | Process killed, then Home | Home redraws from the database | manual |
| P-3 | Dark mode | Sheets and settings switch theme; home follows the wallpaper | manual |
| P-4 | Font scale 1.5 | No clipped text in rows or sheets | manual |
| P-5 | TalkBack labels | Rows, letters and clock have meaningful descriptions | manual (accessibility tree dump) |
| P-6 | Automated accessibility checks on Compose screens | No ATF errors | auto-ui (`enableAccessibilityChecks`) |

## Not built yet (milestone 3)

These flows are not tested:
- F11 / F12: themes, Theme Studio, share and import
- F14: Notification Summary
- F15: Quick Lock
- F16: weather and calendar
- F17: shortcut button
- F19: Pro (skipped)
