# Bugs: Driftlist

Device for all bugs: emulator Medium_Phone_API_37.0 (Android 17, 1080×2400). Every bug below was reproduced. Things I suspected but did not reproduce are under "To check".

| ID | sev | title | status |
| --- | --- | --- | --- |
| BUG-001 | S1 | Upgrading from 0.1.0 replaced favorites via onboarding | fixed in a1fbaa9 |
| BUG-002 | S2 | Hosted widgets rendered blank | fixed in 9e59dff |
| BUG-003 | S3 | Home pressed during onboarding opened search right after | fixed in 073ff2d |
| BUG-004 | S4 | Search overlay let the home screen show through | fixed in 9e59dff |
| BUG-005 | S3 | App icons blank when home draws before the first app sync | fixed in 03e1585 |
| BUG-006 | S3 | Wave alphabet letters were 32 dp TalkBack targets | fixed in 325ce6d |
| BUG-007 | S3 | Sheet drag handle: 32 dp actionable target | fixed in 325ce6d |
| BUG-008 | S4 | Date under the clock was a 45 dp target | fixed in 325ce6d |
| BUG-009 | S3 | Change-icon button in the options sheet had no label | fixed in 325ce6d |
| BUG-010 | S4 | "On for 1 apps" | fixed in 03e1585 |
| BUG-011 | S4 | Landscape: last favorite starts below the fold | fixed in M4 batch 2 (side-by-side layout; test landscapeKeepsEveryFavoriteOnScreen) |

Open S1: **none**. Open S2: **none**.

---

### BUG-001: Upgrading from 0.1.0 replaced favorites via onboarding

- Severity: S1 (data loss)
- Flow / case: F01 / F01-E3
- Screen: S01, S02
- Build: pre-a1fbaa9 (milestone 2 work in progress). Device: emulator, release build

Steps
1. Install 0.1.0 and pin a favorite (Clock).
2. Install the milestone 2 build over it.
3. Press Home.

Expected: home with the Clock favorite.
Actual: onboarding showed, because the new "onboarding done" setting didn't exist yet. Finishing it called `replaceWithApps` and wiped the favorite.
Evidence: found while preparing the upgrade test; `clone-screens/S05-home.png` shows the post-fix result.
Suspected cause: first-run detection relied only on a new flag.
Status: fixed in a1fbaa9. Onboarding now shows only when there are no favorites, and existing favorites mark it done. The upgrade path is covered by `MigrationTest.v1ToV2KeepsAppsAndFavorites`.

### BUG-002: Hosted widgets rendered blank

- Severity: S2 (feature broken)
- Flow / case: F08 / F08-H1
- Screen: S05, S13
- Build: a1fbaa9

Steps
1. Long-press the clock › Add a widget › Clock › Digital.
2. Allow, then choose a style.

Expected: the widget appears under the clock.
Actual: an empty gap. The provider logged `widgetSizePx=0x0`.
Evidence: logcat line from `com.google.android.deskclock`; the widget's view existed in the accessibility tree ("Digital").
Suspected cause: the host never reported the widget size (`updateAppWidgetSize`).
Status: fixed in 9e59dff (size is reported from a layout listener). Verified: `clone-screens/S05-home-widget.png`.

### BUG-003: Home pressed during onboarding opened search right after

- Severity: S3
- Flow / case: F01 / F01-E2
- Screen: S01, S15
- Build: 9e59dff

Steps
1. Clear app data and open home (onboarding shows).
2. Press Home.
3. Finish onboarding.

Expected: home.
Actual: the search overlay opened.
Evidence: `clone-screens/S05-after-onboarding.png` (before the fix).
Status: fixed in 073ff2d. Home presses are ignored while onboarding is on screen.

### BUG-004: Search overlay let the home screen show through

- Severity: S4
- Flow / case: F04-H1
- Screen: S15
- Build: a1fbaa9
- Steps: open search.
- Expected: a clean surface.
- Actual: the clock and letters were faintly visible behind the results (97% opacity).
- Status: fixed in 9e59dff.

### BUG-005: App icons blank when home draws before the first app sync

- Severity: S3
- Flow / case: P-2 / F02
- Screen: S05, S06
- Build: 325ce6d, release

Steps
1. `adb install -r` the release build, or change the font scale, so the process restarts while home is visible.
2. Look at the favorites.

Expected: icons.
Actual: rows with labels but no icons. Folder icons were empty too. Icons in sheets opened later were fine.
Evidence: `clone-screens/P4-font-1.5-home.png`.
Suspected cause: `IconCache.load` returned null when `AppCatalog` hadn't read LauncherApps yet, and nothing ever asked again.
Status: fixed in 03e1585. The catalog signals each sync, and the cache bumps its version if anything was missed. Test written first: `IconCacheTest.iconsRequestedBeforeTheFirstSyncReloadAfterIt`.

### BUG-006: Wave alphabet letters were 32 dp TalkBack targets

- Severity: S3 (accessibility)
- Flow / case: P-6, F03
- Screen: S05, S06
- Build: 073ff2d
- Steps: run `HomeFlowsTest` with accessibility checks on.
- Expected: no ATF errors.
- Actual: `TouchTargetSizeCheck` reported each enabled letter at 32 dp high.
- Evidence: failing tests in the first `connectedDebugAndroidTest` run.
- Status: fixed in 325ce6d. TalkBack now sees one adjustable "Alphabet" control (swipe up/down to move letter by letter); the individual letters are hidden from accessibility. Covered by every `HomeFlowsTest` case, and `alphabetJumpHideAndBack` drives the control.

### BUG-007: Sheet drag handle had a 32 dp actionable target

- Severity: S3 (accessibility)
- Flow / case: P-6
- Screen: S07, S08, S10, S11, S12
- Build: 073ff2d
- Steps: open any sheet with accessibility checks on.
- Actual: `TouchTargetSizeCheck` failed on the 32 × 36 dp drag handle.
- Status: fixed in 325ce6d. Material's handle slot is no longer used; the handle is drawn as plain content. Sheets still close by dragging, tapping outside or Back.

### BUG-008: Date under the clock was a 45 dp target

- Severity: S4 (accessibility)
- Flow / case: P-6
- Screen: S05
- Build: 073ff2d
- Status: fixed in 325ce6d (48 dp minimum height).

### BUG-009: Change-icon button in the options sheet had no label

- Severity: S3 (accessibility)
- Flow / case: P-6, F09
- Screen: S08
- Build: 073ff2d
- Actual: `SpeakableTextPresentCheck` failed on the tappable app icon.
- Status: fixed in 325ce6d (labelled "Change icon for <app>").

### BUG-010: "On for 1 apps"

- Severity: S4
- Screen: S17
- Build: 325ce6d
- Steps: turn on Time checks with one app selected, go back to Settings.
- Status: fixed in 03e1585.

### BUG-011: Landscape: last favorite starts below the fold

- Severity: S4
- Flow / case: F03-E7
- Screen: S05
- Build: 03e1585
- Steps: rotate to landscape with 5 favorites.
- Expected: all favorites visible, or a layout made for landscape.
- Actual: the 64 sp clock plus its top space leave room for 4 rows; the fifth (Media) needs a scroll.
- Evidence: `clone-screens/F03-E7-landscape.png`.
- Status: open. A landscape and foldable layout (two columns, smaller clock) is in the M3 list.

---

## To check (suspected, not reproduced)

- **F05-N2:** a reply whose PendingIntent was cancelled by the app should show "Couldn't send". It's hard to trigger on the emulator.
- **F08-N2:** uninstalling a widget's provider should show "This widget stopped working". Not tried yet.
- **Icon pack parsing time:** the first open of the icon picker parses the whole pack on the main thread. It was fine with Arcticons (~10k icons); a bigger pack could stutter.
- **Usage Breaker after process death:** the 30-second loop restarts on the next screen-on, but not if the launcher process is killed while the screen stays on.
