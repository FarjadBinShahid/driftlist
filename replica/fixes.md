# Fixes: what Niagara's users hate, and what Driftlist does about it

Sample: **142 reviews from 5 sources**, collected 2026-10-13 by reading them, never scraping: Google Play 60 (the "most relevant" list), Lemmy 33, Niagara's own Featurebase board 23, Hacker News 21, AlternativeTo 5. 64 have star ratings (5★ 31, 4★ 10, 3★ 10, 2★ 7, 1★ 6). Every quote below is copied exactly and linked, and is in `replica/reviews.csv`. The ranking is in `replica/feedback.md` (made with `reviews.py` and the launcher themes in `replica/themes.json`).

For scale: Google Play shows 237K ratings for Niagara (5★ 207,577 · 4★ 15,762 · 3★ 6,286 · 2★ 2,263 · 1★ 5,360). Most users are happy. The complaints below come from a minority, but the same few complaints come up again and again across sources.

Not reached: Reddit (blocks our research tools), XDA (needs a login), Trustpilot (bot check). Nothing from them is used.

Already fixed by Niagara, so not counted as a gap (their GitHub releases): Bluetooth music apps (1.16.32 era), the flashlight tip (1.16.28), freezes on return to home (1.16.22, though reviews after that still report it), and separate lock screen wallpapers (1.16.32).

---

## 1. What they hate

| # | Problem | Reviews | Sources | Evidence |
|---|---|---|---|---|
| 1 | **Price and paywalls**: basics locked behind a subscription or an expensive lifetime licence, and prices going up | 37 (price) · 26 (basics behind Pro) · 22 (lifetime price, rises) | 5 · 5 · 4 | "Can't even use more than one widget without replacing the clock." ([Play, 1★](https://play.google.com/store/apps/details?id=bitpit.launcher&reviewId=e7a1dc0d-75ac-4fb4-9471-ba565af2c02a)) · "the subscription was increased by almost 70%" ([board](https://niagaralauncher.featurebase.app/p/pricing-policy)) · "My parents wont pay for niagara pro" ([board](https://niagaralauncher.featurebase.app/p/double-tap-to-turn-off)) |
| 2 | **Bugs that stay open**, especially a frozen, blank or unresponsive home screen and broken recents | 22 (bugs) · 10 (freezes) | 4 · 2 | "Every second time we use the home screen, the launcher freezes and I need to lock and then unlock my phone to unfreeze the launcher." ([Play, 3★](https://play.google.com/store/apps/details?id=bitpit.launcher&reviewId=89a6174d-c01c-40bd-8c03-2d4b506a5632)) · "the UI is unresponsive because it it doesn't realize it's in the foreground" ([Play, 2★](https://play.google.com/store/apps/details?id=bitpit.launcher&reviewId=bb9ce2f9-c3f9-4efa-b1e8-287aa5c7f4c0)) · "the inability to look at all my open apps when the launcher was active" ([Play, 1★](https://play.google.com/store/apps/details?id=bitpit.launcher&reviewId=6b134ea9-f1bb-4dcd-8e60-36c552c571fd)) |
| 3 | **Closed source, trackers, broad permissions** for an app that sees everything | 8 · plus 5 on privacy | 3 | "It sends a unique device identifier, your IP address and your app usage to Firebase." ([HN](https://news.ycombinator.com/item?id=39328990)) · "Not FOSS = massive privacy and security threat when it comes to crucial things like the app launcher." ([HN](https://news.ycombinator.com/item?id=39332990)) · "open source alternative with no trackers" ([Lemmy](https://piefed.ca/comment/4106411)) |
| 4 | **Finding apps by name only**: no glanceable icons, so you have to know what an app is called | 5 (thin: 2 sources) | 2 | "I can no longer glance to see icons, now I have to read." ([Play, 1★](https://play.google.com/store/apps/details?id=bitpit.launcher&reviewId=0b409019-b9f4-4994-93c8-cf2417033964)) · "you'll need to know the actual names of your apps" ([Play, 5★](https://play.google.com/store/apps/details?id=bitpit.launcher&reviewId=a6d715fb-b1e1-4309-846a-da7df7e2ece3)) |
| 5 | **Home layout moves under your thumb** (media, notifications and widgets push favorites around) | 4 (thin) | 2 | "Notifications, media controls, and other widgets constantly shift positions, breaking muscle memory." ([Play, 4★](https://play.google.com/store/apps/details?id=bitpit.launcher&reviewId=475cbb4f-2abf-497d-9d20-cfa298736049)) |
| 6 | **The app list opens at a random place** (single report, read by hand) | 1 (thin) | 1 | "it never starts at the top but dumps me at some random point on the page" ([Play, 1★](https://play.google.com/store/apps/details?id=bitpit.launcher&reviewId=698d0fd0-2997-43cb-b831-cf0d71ef0b1e)) |

## 2. What is missing (asked for by name)

| # | Request | Reviews | Sources | Evidence | Driftlist today |
|---|---|---|---|---|---|
| 1 | **Room for widgets / more than one widget** | 18 | 4 | "the biggest issue for me is lack of room for widgets." ([Play, 2★](https://play.google.com/store/apps/details?id=bitpit.launcher&reviewId=92587e66-85b3-4b67-9e0f-2160773a281c)) | Up to 4 header widgets, plus widgets in folders and pop-ups, all free. No widgets between favorites |
| 2 | **Folders and categories in the app list** | 13 | 4 | "The developer refuses to make categories/folders to group apps." ([HN](https://news.ycombinator.com/item?id=26470892)) | Folders appear in the A-Z list (opt-in). No auto categories |
| 3 | **Layout freedom**: left, centre or right; more favorites; icons only | 9 | 3 | "simple things like being able to have your homepage icons/text left, center, or right is STILL not an option." ([Play, 3★](https://play.google.com/store/apps/details?id=bitpit.launcher&reviewId=745842bd-6d59-4204-9049-9c69d2fe5b67)) · "I need a lot more than maybe ten favorites on screen" ([HN](https://news.ycombinator.com/item?id=39328866)) | Missing |
| 4 | **More gestures**, especially on the round button | 7 (+155 votes on the board) | 4 | "Currently, the Niagara button supports only two gestures: tap and swipe up." ([board](https://niagaralauncher.featurebase.app/p/currently-the-niagara-button-supports-only-two-gestures-tap-and)) | Tap and swipe up only |
| 5 | **Search straight away, and forgiving search** | 4 (thin) | 2 | "It'd be great if there was a simple way to just search DIRECTLY without having to go on the alphabet, click the search icon and then type." ([Play, 3★](https://play.google.com/store/apps/details?id=bitpit.launcher&reviewId=96ea6deb-382a-4103-bfb8-cd70b8630622)) · "It would be nice to show the nearest match instead." ([board](https://niagaralauncher.featurebase.app/p/search-bar-improvements)) | Swipe up or Home opens search; no typo tolerance |
| 6 | **Text colours between light and dark** | 4 (thin) | 2 | "sometimes the dark is too dark and the light is just too light, a middleground would be very appreciated" ([Play, 5★](https://play.google.com/store/apps/details?id=bitpit.launcher&reviewId=eb2848ab-b701-4f56-a26a-c646f924df77)) | Home text follows the wallpaper; no choice |
| 7 | **Unread dots instead of (or as well as) preview text** | 2 (thin) | 2 | "I'd appreciate if you can add a red dot in the corner of app icons indicating that i have unread notifications for that app." ([Play, 5★](https://play.google.com/store/apps/details?id=bitpit.launcher&reviewId=27362f19-6e75-423e-83c6-cc986f2433e0)) | Preview text and count only |

## 3. What is unsolved (whole groups Niagara doesn't serve)

1. **People who won't pay for a launcher, or can't.** Students, people whose parents control purchases, people on older phones, people in countries where €22 a year is a lot. Evidence: the price themes above (37, 26 and 22 reviews across 4 to 5 sources). "My parents wont pay for niagara pro and my phone is a discontinued model…" ([board](https://niagaralauncher.featurebase.app/p/double-tap-to-turn-off)).
2. **Privacy-minded users** (GrapheneOS, /e/OS, F-Droid only) who like Niagara but won't install a closed-source app with notification access and analytics. Evidence: 8 reviews, 3 sources. "It sucks because Niagara is easily my favourite launcher, but I can’t afford to have a proprietary app with insane privileges across my device." ([Lemmy](https://piefed.social/comment/11034145)).
3. **Visual people** who recognise apps by their icon, not their name. Evidence: thin, 5 reviews from 2 sources.

---

## 4. Fix plan

Ranked by evidence times how cheap the fix is. Each item is added to `features.csv` with `original = no`.

| # | Change | Size | Skill | Evidence |
|---|---|---|---|---|
| F1 | **Make "everything free, nothing leaves the phone" provable.** Add an open-source licence, a short privacy page, F-Droid-ready metadata, and a build check that fails if analytics or tracker libraries ever appear. Driftlist already has no analytics and no network by default | S | /replica-brand, /replica-launch | Hate #1 (85 mentions across the three price themes, 5 sources), hate #3 (8 reviews, 3 sources) |
| F2 | **Home that never freezes or goes blank on return.** StrictMode in debug builds, nothing heavy on the main thread when home resumes, a UI test that leaves and returns to home 50 times, and a recents/overview check on the S23 | M | /replica-test | Hate #2 (22 + 10 reviews, 4 sources) |
| F3 | **Layout freedom for favorites:** alignment (left, centre, right), an optional icon grid (4 or 5 across, icons only or with labels), and a setting for how many favorites | M | /replica-build | Missing #3 (9 reviews, 3 sources), hate #4 (5 reviews) |
| F4 | **Favorites stay put:** a "Keep favorites in place" option. The music card, flashlight row and headphone row go below favorites or into the widget stack instead of pushing them down | S | /replica-build | Hate #5 (4 reviews, 2 sources) |
| F5 | **More round-button gestures:** double-tap, swipe down and long-press can each be given an action (an app, a folder, a shortcut, search, Quick Lock, or the notification shade) | S | /replica-build | Missing #4 (7 reviews, 4 sources; 155 votes on Niagara's board) |
| F6 | **Search that forgives typos and opens instantly:** nearest-match ranking (typo distance 1–2) and an optional search pill on home that opens straight into typing | S | /replica-build | Missing #5 (4 reviews, 2 sources) |
| F7 | **Predictable app list:** it always opens at the top unless you jumped to a letter, and keeps your place only for the 3-second return-from-app case | S | /replica-build | Hate #6 (1 review, read by hand; cheap to guarantee) |
| F8 | **Unread dots:** an option to show a small dot (Dots icon style, or next to the icon) instead of preview text | S | /replica-build | Missing #7 (thin) |

Not planned:
- **Google Tasks in the agenda** needs a Google account and the network, which breaks "nothing leaves the phone".
- **Weather** was your call to leave out.
- **Widgets between favorites** (missing #1) is a bigger change to the home layout. Revisit after F3 and F4.

---

## 5. The angle

**A. Free, all of it.**
> For people who love the calm, one-handed list but won't rent their home screen, Driftlist gives you everything (themes, fonts, widget stacks, double-tap to sleep), free, with no account.
> Evidence: price and paywalls, 37 reviews across 5 sources, plus basic features behind Pro, 26 reviews across 5 sources.

**B. Private by design.**
> For privacy-minded Android users who won't give a closed-source launcher their notifications, Driftlist is open source, has no trackers, and makes no network calls unless you turn on web suggestions.
> Evidence: closed source, trackers and permissions, 8 reviews across 3 sources, plus privacy, 5 reviews across 3 sources.

**C. Find apps your way.**
> For people who find apps by colour and icon, not by name, Driftlist gives you the list, a grid, or dots coloured like each app's icon.
> Evidence: finding apps by name only, 5 reviews across 2 sources (thin), plus layout freedom, 9 reviews across 3 sources.

**Recommended: A and B together, led by B.** Being free answers the biggest complaint, but free alone is easy to copy and says nothing about why to trust a launcher that reads your notifications. "Open source, no trackers, and nothing to pay" answers the top two complaints at once, and Driftlist already works that way. All it needs is the proof (F1).

Rules for using this: the original's name stays out of Driftlist's name, ads and store listing, and reviewers' words are research, never testimonials.
