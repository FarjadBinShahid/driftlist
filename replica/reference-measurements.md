# Niagara 1.16.31 on Galaxy S23 (1080x2340, 480 dpi = 3 px/dp, 360x780 dp)
Captured 2026-10-11 by the user driving the phone; layout from the accessibility tree (uiautomator), no app files read.

## 01 home
- content column: x 40 dp, width 262 dp (alphabet lives in the remaining right ~58 dp)
- clock: top 101 dp (13% of height), height 53 dp; date row 29 dp below it
- widget stack: directly under the date row, full column width (262 dp)
- favorites: directly under the widget, flowing down; row 54 dp, icon 42 dp (6 dp inset), label x=108 dp (20 dp after icon), ~16 sp
- alphabet: star at top (~193 dp), letters only for existing apps (X absent), settings circle at bottom (~704 dp); top aligned with the widget, not full height
- top scrim: 82 dp
- action button (FAB): 56 dp, bottom-right, ~14 dp from the content column's right edge

## 02 app list + wave (burst of 20 frames)
- list rows identical to favorites: 54 dp, icon 42 dp, label x=108 dp, ~16 sp; letter header row 43 dp, in the icon column (x 40, 28 dp wide)
- while sliding: the selected section's header sits ~1/3 down the screen (not pinned to the top)
- wave: letters shift LEFT (little or no scaling) in a V/triangle across the WHOLE strip; peak offset ~40% of screen width (~140 dp) at the finger, falling off roughly linearly to the ends
- bubble: filled circle with the letter, ~48 dp, at the finger, overlapping the list
- at rest: strip straightens and sits in the lower ~55% of the screen (star ... circle); a round search button (~48 dp, magnifier) appears bottom-right in the list view

## 03 app pop-up (swipe right on Messages; no notification present, shortcuts only)
- NOT a bottom sheet: a floating rounded card anchored at the swiped row, drawn over the list
- card: x 40 dp (content column start), width 280 dp; header row 48 dp = "open" arrow icon + app name (tap opens the app)
- shortcut rows: 52 dp, round 36 dp icons/avatars, label x = 116 dp
- the list and alphabet stay visible around it; search button stays bottom-right

## 04 long-press options (on an app in the list)
- bottom sheet, full width, rounded top, no visible drag handle; list dims behind it
- header: 36 dp app icon at x 32 dp + app name ~24 sp bold
- rows 52 dp, side margin 16 dp, leading icon column, label x = 68 dp
- order: Favorite / App info / Screen time / Add to category / Uninstall / Advanced (expands) | divider | Niagara settings / Niagara Pro (upsell)

## 04b long-press on a suggested app (home)
- Instagram/Facebook on the home screen in shot 01 were SUGGESTIONS (most used), not favorites
- suggestion sheet adds a first row: "Stop suggesting" with a secondary line ("Don't show below favorites")
- Advanced (collapsed) contains: Edit <app> pop-up / Edit icon/name (rename + icon in one screen) / Hide from app list

## 05 search
- opaque background; pill field at top (x 48, 236 x 48 dp) with a three-dot overflow menu inside it on the right; keyboard opens at once
- empty query: nothing listed (no suggestions)
- results: rows 48 dp (top result 50 dp), label x = 102 dp; the top result has a hint line "Tap Search to open" (keyboard search key launches it)
- last row: "Search for more apps" (Play Store search) with a small dot icon; no web row in this configuration

## 06 long-press clock -> "Niagara widget" sheet
- bottom sheet with a large title ("Niagara widget", ~24 sp, x 32 dp)
- rows are separate rounded CARDS (~56-62 dp tall, 8 dp gaps, ~28 dp side margin), each: leading icon, title (+ optional grey subtitle), trailing chevron or switch
- items: Clock style > | Calendar (Show upcoming events, Show date) > | Weather (Location, Temperature units) [switch] | Add Custom Widget > | Move Widget > | Show Media Widget (While playing music) [switch] | Show Battery Percentage [switch]

## 07 settings (top level)
- header: large centred logo, then "Niagara settings" / "Free version" (~28 sp, centred), ~280 dp tall in total
- six rows with leading icons, label x = 80 dp, row ~51-68 dp (taller with subtitle): Productivity (Stay efficient and organized) / Themes (Give your home screen your personal touch) / Niagara Pro (Try for 7 days) / Help & Feedback / Community / Advanced
- settings are split into sub-pages; no long single page
- settings header collapses into a pill-shaped sticky title bar when scrolled

## 08 Productivity settings (full page)
- big centred title that collapses into a pinned pill bar; section labels at x 68 dp; rows: icon, title, 1-2 line description, optional switch
- Instant Access: Quick Replies [switch] / Calendar Agenda (tap the date) / Calendar Preview [switch] / Weather Forecast [switch] / Media Player [switch]
- Digital Wellbeing: Usage Breaker [switch] / Notification Summary [switch]
- App Organization: Hide Apps / Folders & Pop-Ups / Suggest Often-Used Apps [switch, on] / Sort All Apps List by Usage [switch, on] ("Prioritize most-used apps for each letter")
- Smart Search: Smart Search ("Apps, contacts, and web in one place")
- Shortcuts & Gestures: Niagara Button / Quick Lock (double-tap the alphabet)
- Advanced: Add Custom Widget / Move Widget / Show Battery Percentage / Allow Home Screen Rotation / Allow Haptic Feedback (Follow system)
