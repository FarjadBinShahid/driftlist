# Components: launcher clone

Source list: `replica/recon.md` › Components. Tokens: `tokens.json`, generated into Kotlin as `Tokens.kt`
(`LightTokens` / `DarkTokens` / `HomeTokens`, `TypeTokens`, `SpaceTokens`, `SizeTokens`, `RadiusTokens`, `MotionTokens`, `WaveTokens`).
Platform: Jetpack Compose + Material 3. "Focus" means a keyboard / D-pad focus ring (2 dp, `accent`), and "a11y"
means TalkBack.

**Assets rule.** Icons come from Material Symbols Rounded (Apache-2.0). Fonts are Inter plus the OFL pack in
`tokens.json`. Every label and empty-state line is written fresh. No Niagara logo, icon art, wallpapers,
clock designs or copy.

**Global a11y rules.**
- Every gesture-only action (swipe right for the pop-up, long-press for options, the alphabet drag, double-tap to sleep) also exists as a TalkBack **custom action** or a visible control. A gesture is never the only way in.
- Touch targets are at least 48 dp (`SizeTokens.touchMin`), even where the visual is smaller.
- Respect font scale: rows grow in height, they never clip. Respect "Remove animations": the wave becomes a plain highlight and sheets fade.
- Home text on the wallpaper always uses the `HomeTokens` on-dark / on-light pair chosen by wallpaper luminance, plus the home scrim.

---

## Home and lists

```
AppRow
  variants  favorite (56 dp, type favorite, icon 40), list (48 dp, type list, icon 32),
            suggestion (list size, muted label), search-result (list size + match highlight),
            folder (favorite size, stacked mini-icons as the icon)
  states    default, pressed (8% text-colour overlay, radius md), has-notification (preview line under the label),
            work-profile (badge on the icon), unavailable (quiet-mode work app: 38% alpha, not clickable),
            dragging (S10: elevation pop, 1.02 scale), focused
  tokens    text: HomeTokens text-on-dark|light; preview: preview-on-*; gutter edge-gutter;
            gap icon->label space[4] (12 dp)
  gestures  tap: launch (clip-reveal from row bounds) · swipe right > 56 dp or fling: open AppPopup ·
            long-press: AppOptionsSheet (haptic LONG_PRESS)
  a11y      one node: "<label>, <n> notifications"; custom actions: Open notifications and shortcuts,
            Options, Add to / Remove from favorites
  used on   S05 S06 S10 S11 S15 S28
```

```
WaveAlphabet
  variants  right edge (default), left edge (left-hand mode: invisible strip, bubble mirrored)
  layout    letters stacked vertically, centred on the list area; visual width 32 dp, touch width 48 dp
            (alphabet-touch-width); registered as systemGestureExclusionRects (Android limit: 200 dp per edge,
            so exclude the band around the finger, not the whole strip)
  states    idle (type alphabet, alphabet-idle-on-*), touching (letters within influence-letters scale up to
            max-scale and shift inward up to max-offset on a cosine falloff; the selected letter shows in
            AlphabetBubble), letter-change (haptic CLOCK_TICK, list scrolls to the section), released
            (spring-wave settle), disabled-bucket (no apps under the letter: 40% alpha, skipped by the
            selection), reduced-motion (no scaling, highlight only)
  extra     double-tap on the strip = Quick Lock when enabled (Pro); the star or top region returns to favorites
  tokens    WaveTokens.*, MotionTokens.springWave*, TypeTokens.alphabet
  a11y      exposed as a list of buttons "Jump to A" ... (TalkBack users get a letter list, not a drag surface);
            traversal after the app list
  used on   S05 S06
```

```
AlphabetBubble
  variants  right, left
  states    hidden, shown (scale-in 120 ms), letter changing (crossfade)
  tokens    size alphabet-bubble 56 dp, surface-raised, text title, radius pill, elevation pop
  a11y      decorative (the letter is announced by WaveAlphabet)
  used on   S05 S06
```

```
LetterHeader
  variants  Latin, locale-extra (Å Ä Ö Ñ Æ Ø), # bucket
  states    normal, sticky (pinned at the top while its section scrolls)
  tokens    TypeTokens.letterHeader, HomeTokens text-muted-on-*
  a11y      heading
  used on   S06
```

```
ListFooterRow
  variants  recently-installed, pro (debug toggle in this build), settings
  states    default, pressed
  tokens    as AppRow list, with a Material Symbol in place of the app icon
  used on   S06
```

```
ClockHeader
  variants  plain (clock + date), at-a-glance (+ weather chip, next event, battery), clock style N (Pro),
            replaced-by-widget
  states    default, moved (vertical offset from PositionSlider), pressed-clock (opens the clock app),
            pressed-date (opens AgendaSheet), long-press (HeaderOptionsSheet)
  tokens    TypeTokens.clock / clockSmall / date; top space = headerTopSpaceRatio x screen height
  a11y      "<time>, <date>"; custom actions Open clock, Open agenda, Header options
  used on   S05 S19
```

```
MediaCard
  states    hidden, playing, paused, no-artwork (gradient placeholder from the accent), controls-unsupported
            (no buttons, tap only)
  layout    88 dp high, artwork 56 dp radius md, title + artist, prev / play-pause / next (48 dp targets)
  tokens    surface-raised at 85% over the wallpaper, radius lg, elevation card
  a11y      "Now playing <title> by <artist>"; buttons labelled
  used on   S05
```

```
SuggestionsRow (section)
  states    hidden (no suggestions), 1-4 apps
  tokens    AppRow suggestion
  a11y      heading "Suggested"
  used on   S05
```

```
ContextRow
  variants  headphones (music apps), flashlight-on (with Turn off)
  states    shown, dismissed
  used on   S05
```

```
ActionButton (the bottom-right shortcut button)
  states    hidden, unassigned (+ icon), app (app icon), folder (grid icon), pressed, swipe-up hint
  tokens    action-button 56 dp, accent-container / on-accent-container, radius pill, elevation pop
  gestures  tap: primary action · swipe up: secondary (Pro) · long-press: ButtonActionEditor (S16)
  a11y      "Shortcut button, opens <target>"; custom actions Secondary action, Edit
  used on   S05
```

## Notifications

```
NotificationCard
  variants  inline-preview (one line under an AppRow), compact (in a pop-up), expanded (full text + actions),
            messaging (MessagingStyle: sender + last 3 messages)
  states    unread, read, expanded, replying, sending, sent (collapses to "Sent"), failed (danger text + retry),
            dismissing (swipe left: slide + fade 220 ms, then removed)
  tokens    surface-raised, radius md, body-sm / preview, text-muted for time
  a11y      "<app>: <title>, <text>, <time>"; actions as buttons; custom action Dismiss
  used on   S05 S06 S07 S11
```

```
InlineReplyField
  states    empty (send disabled), typing, sending (progress in the send button), sent, error
  tokens    surface, border-input (1 dp), radius pill, body
  a11y      label "Reply to <sender>"; IME action Send
  used on   S07
```

## Sheets and pop-ups

```
BottomSheet (base for every sheet)
  variants  options (fits content), popup (up to 85% height, scrollable), agenda (full height, 2 snap points)
  states    hidden, opening (spring-sheet), expanded, dragging, dismissing (drag down / scrim tap / back)
  tokens    surface-raised, radius sheet (top corners), handle 32x4 border, backdrop scrim at sheet-backdrop-alpha
  a11y      a dialog pane with a title; focus moves into the sheet and returns to the source row on close;
            back dismisses
  used on   S07 S08 S11 S12 S14
```

```
AppPopup (S07 content)
  sections  header (icon, label, edit button) · notifications (NotificationCard list) · shortcuts (rows with
            shortcut icons) · widget / stack (Pro)
  states    empty (no notifications: shortcuts only + a quiet "Nothing new" line), with-notifications,
            low-priority-only, editing (checkboxes on shortcuts, Add widget button)
  used on   S07
```

```
AppOptionsSheet (S08 content)
  rows      header (icon: tap to change the icon · label: tap to rename) · Add to favorites / Edit favorites ·
            Hide from list · App info · Uninstall (hidden for system apps)
  states    favorite / not favorite, system app, rename-editing (inline text field, Save / Cancel)
  used on   S08
```

```
HeaderOptionsSheet (S12 content)
  rows      Move (PositionSlider) · Clock style · Weather (toggle + units) · Calendar (toggle + filter) ·
            Media card (toggle) · Add widget · Replace clock with widget
  states    free (Pro rows show ProBadge and open the debug toggle), pro
  used on   S12
```

```
PositionSlider
  states    default, at-min, at-max (arrow disabled)
  layout    up / down arrow buttons (48 dp) + a value readout
  a11y      adjustable: "Header position, <n> of <max>", swipe up / down to change
  used on   S12
```

```
AgendaSheet (S14 content)
  sections  weather now · HourlyStrip · DailyForecast · events by day
  states    no calendar permission (PermissionPrompt inline), no events ("Nothing scheduled" + Open calendar),
            events, weather off (section hidden), no location (Set location button), stale (caption "Updated <x> ago")
  used on   S14
```

```
HourlyStrip / DailyForecast
  states    loading (skeleton), loaded, stale, error
  tokens    caption, body-sm, surface
  used on   S14
```

## Widgets

```
WidgetHost (wraps AppWidgetHostView in AndroidView)
  states    loading (skeleton at the provider's min height), loaded, configuring, error ("This widget stopped
            working" + Remove / Retry), needs-rebind (after restore: placeholder + Re-add)
  tokens    radius lg clip, surface at 0% (transparent, so widgets keep their own background)
  a11y      the hosted view's own semantics; container labelled "<provider> widget"
  used on   S05 S07 S11 S13
```

```
WidgetStackPager
  states    single (no dots), 2-4 pages (dot indicator), swiping, two-column (wide screens)
  a11y      custom actions Next widget / Previous widget
  used on   S05 S07 S11
```

```
WidgetPicker (S13)
  layout    searchable list grouped by app, each with a preview image and size
  states    loading, list, empty search, bind-denied (explainer + Try again)
  used on   S13
```

## Search

```
SearchField
  states    empty (placeholder "Search apps, maths and the web"), typing (clear button), focused (keyboard up)
  tokens    surface, radius pill, body, 48 dp min height
  a11y      label "Search"; IME action Go = open the first result
  used on   S15
```

```
SearchResults
  sections  apps (AppRow search-result) · calculator (CalcResultRow) · contacts (ContactRow, Pro) ·
            web (WebSuggestionRow list + "Search <engine> for '<q>'")
  states    empty query (suggested apps), results, no app matches (web only), offline (web rows hidden)
  used on   S15
```

```
CalcResultRow       states: valid (large result, tap copies, "Copied" toast), hidden when invalid · used on S15
WebSuggestionRow    states: default, wraps to 2 lines max · trailing arrow fills the query · used on S15
ContactRow          states: with photo, initials avatar · Call / Message buttons (48 dp) · used on S15
```

## Settings and pickers

```
SettingsRow
  variants  toggle, navigation (chevron), value (current value on the right), slider, destructive
  states    on, off, disabled, pro-locked (ProBadge; tap opens the explainer / debug toggle),
            permission-missing (warning icon + "Needs permission" caption; tap opens PermissionPrompt)
  tokens    settings-row 64 dp, body + caption text-muted, surface
  a11y      the whole row is one switch / button; state announced
  used on   S17 S22 S24 S25
```

```
SettingsSectionHeader   type label, accent · heading · used on S17 S22
ProBadge                pill, accent-container / on-accent-container, caption · decorative + "Pro" in the row label · S12 S17 S18 S22
```

```
AppMultiPicker
  states    loading, list, none selected, some selected (count in the top bar), searching, no matches
  layout    search field + checkbox rows (AppRow list + trailing Checkbox) + optional extra toggle at the top
  a11y      each row is a checkbox "<label>, checked"
  used on   S02 S23
```

```
DragList (Edit favorites)
  states    idle, dragging (lifted row), drop-target gap, removing (swipe or the remove button)
  layout    leading drag handle (Material Symbol drag_indicator), trailing remove button
  a11y      custom actions Move up / Move down / Remove (dragging is not required)
  used on   S10
```

```
IconGrid (icon picker)
  states    loading, icons, pack lacks this app (suggestions row first + caption), no packs installed
            (explainer + "Get icon packs" opens the Play search), selected (accent ring 2 dp)
  layout    pack selector chips on top, 5-column grid of 56 dp cells, search
  used on   S09
```

```
ThemeCard        variants: built-in, mine, imported · states: applied (check + accent outline), locked (ProBadge), pressed · mini preview of wallpaper + clock + icons · S18
LivePreview      scaled home mock (favorites + clock) at 0.6x; recomposes on every edit with a 120 ms crossfade · S19
ColorSwatchPicker  wallpaper-derived swatches (5) + Custom (hue slider) · selected = 2 dp ring · contrast warning if the chosen accent fails 3:1 on surface · S19
FontRow          sample "Aa Bb 0123" set in the font · bundled / imported · selected · S20
WallpaperGrid    built-in (our own art), Pick from gallery (system picker), apply target Home / Lock / Both, blur slider · S21
```

## Feedback

```
PermissionPrompt
  variants  dialog (S29), inline card (TutorialCard style on home, AgendaSheet)
  content   what it unlocks (one sentence) · Allow (deep link to the exact settings page) · Not now
  states    default, after-deny (explains the manual path), restricted-settings (Android 13+ sideload:
            numbered steps for App info > Allow restricted settings)
  used on   S04 S29 S14 S22
```

```
TutorialCard       inline card on home; states: shown, dismissed (never shown again) · S04
Toast / Snackbar   info, undo (Removed from favorites · Undo, 4 s) · bottom, above the ActionButton · S10 S25 S15
EmptyState         one Material Symbol + one fresh line + an optional action · S06 (no apps found), S24, S28, S18
```

## Build notes for replica-build

- Put these in `ui/components/` and show every state in a debug-only **DesignCatalogActivity**, plus Compose `@Preview`s with `uiMode` light/dark and `fontScale` 1.0 and 1.5. That catalog is the design-system check.
- Wire `LightTokens` / `DarkTokens` into a Material 3 `ColorScheme`. When the theme's accent is "auto", override `primary` / `primaryContainer` from the wallpaper palette (Material Color Utilities), then re-run the 3:1 check at runtime and fall back to the token accent if it fails.
- Pick `HomeTokens` on-dark vs on-light from the wallpaper's luminance (`WallpaperColors.colorHints` and `HINT_SUPPORTS_DARK_TEXT` on API 27+, or average luminance of a downscaled bitmap).
