# Letterline: brand

Angle (from `fixes.md`): **open source, no trackers, nothing to pay**, for people who like a calm, one-handed list but won't rent their home screen or hand a closed app their notifications.

## 1. Name

The working name was Driftlist. Screening on 2026-10-13 found two software products already using it: an ADHD task manager at driftlist.app, which describes itself as "calm and low-pressure", and "Drift Lists", a shared-checklist app on Google Play. The user chose to rename to **Letterline**: the line of letters on the screen edge that every app is one slide along.

Candidates considered (20), by style:

| style | names |
|---|---|
| descriptive | Quietlist, Thumbline, Plainhome, Onethumb, Evenlist |
| compound | Letterline, Edgelist, Sidelist, Railwise, Listwise, Calmdeck, Sidereach |
| invented | Talvo, Orlen, Vesk, Kallo |
| metaphor | Lectern, Almanac, Margin, Hearth |

Filtered out:
- **Water metaphors** (tide, stream, falls), too close in meaning to the original.
- **"-ly" twins.**
- **Names with many same-name apps:** Margin, Hearth and Almanac each had 9 or more App Store matches.
- **Names whose .com, .app and GitHub username were all taken.**
- **Stillist**, which reads as "stylist".

### Checks (screening, not legal clearance)

| check | Letterline | Calmdeck | Plainhome | Sidereach | Driftlist (old) |
|---|---|---|---|---|---|
| US trademark (tmsearch.uspto.gov) | no results, 2026-10-13 | no results, 2026-10-13 | no results, 2026-10-13 | no results, 2026-10-13 | no results, 2026-10-13 |
| EU (EUIPO / TMview) | to run (TMview didn't load in the research browser) | to run | to run | to run | to run |
| Canada (ised-isde.canada.ca) | to run | to run | to run | to run | to run |
| WIPO Global Brand Database | to run | to run | to run | to run | to run |
| .app domain (registry RDAP) | **free**, 2026-10-13 | free | free | free | taken (registered 2025-08-26; a task app called Driftlist) |
| .com domain (registry RDAP) | taken | taken | taken | taken | taken (car parts shop since 2016) |
| Google Play (exact name) | no app, 2026-10-13 | no app | no app | no app | "Drift Lists" (shared checklists) |
| App Store (iTunes Search API) | no app, 2026-10-13 | no app | no app | no app | no app |
| GitHub username | taken | free | free | taken | free |
| X, Instagram, TikTok | to run | to run | to run | to run | to run |
| web: "name + launcher" | to run | to run | to run | to run | see above |

Before spending money on the name (a domain, a store listing, a registration), a trademark lawyer should do a proper search. The "to run" rows were not checked; nothing here says they are free.

## 2. Palette

The original's brand is blue: sky `#7FB9E9`, bright `#109DFF`, navy `#0B1957` (from its website's own colours). Driftlist's old accent `#3B5BCC` was in the same family, so Letterline moves to **moss green**.

| role | light | dark |
|---|---|---|
| accent | `#2F6A4F` | `#8FD1AD` |
| on accent | `#FFFFFF` | `#0D2E1F` |
| accent container | `#DCEFE4` | `#1F4D37` |
| on accent container | `#123D2A` | `#DCEFE4` |

Written into `replica/design/tokens.json` and regenerated into `Tokens.kt`. `contrast.py`: **38 pairs, 0 failing AA**. "Moss" is also an accent preset in Theme Studio, and the colour previews fall back to it. Users can still pick blue ("Ocean") as their own accent.

## 3. Logo brief

- **Idea:** the gesture the app is built on. A short list, and beside it the line of letters bowing out to meet your thumb, with the touched letter lit.
- **Mark:** a symbol (no letterform), cream `#F4F1E8` on moss `#2F6A4F`, with the touched letter in pale green `#B9E3C9`. A wordmark can sit beside it in Manrope (bundled, OFL) when one is needed.
- **Must work** at 16px (the dots merge into a soft curve, and the three bars still read as a list) and as a 1024px icon.
- **Deliverables:**
  - Android adaptive icon: done, `app/src/main/res/drawable/ic_launcher_foreground.xml`, also used as the monochrome layer.
  - SVG master: done, `replica/brand/letterline-icon.svg`.
  - To make if needed: a 1024px opaque PNG for stores, a favicon set and a 1200x630 social image.
- **Must not resemble the original's mark.** The original's icon is two slanted, curved bars forming an "N" on a smoky dark-blue background. Letterline shares no letterform, no slanted pair, and no blue, and its shapes are horizontal bars and round dots. Checked side by side on 2026-10-13.

## 4. Voice

**Calm**, not sleepy. **Plain**, not cold. **Honest**, not preachy.

| do | don't |
|---|---|
| Say what happens: "Then the Home button brings you here" | Hype it: "Supercharge your home screen!" |
| Use the words people use: "hold", "slide", "home" | Use launcher jargon: "long-press", "drawer", "intent" |
| Name the cost of an action: "You'll need to add widgets again" | Hide it in a vague warning |
| Say what to do when something fails: "Try another folder" | Apologise or blame: "Oops! Something went wrong" |
| State privacy as fact: "It all stays on your phone" | Lecture: "Unlike other apps that spy on you…" |

### The 10 most-seen strings, rewritten

| where | before | after |
|---|---|---|
| welcome | "A quieter home screen. Your favorite apps sit within thumb reach, everything else is one slide along the alphabet away." | "Your favorite apps sit where your thumb rests. Every other app is one slide along the letters away." |
| welcome, small print | "No accounts, no ads, nothing leaves this phone." | "Free and open source. No accounts, no ads, and nothing leaves your phone." |
| welcome button | "Get started" | "Choose favorites" |
| favorites picker title | "Pick your favorites" | "Choose your favorites" |
| favorites picker help | "They sit on your home screen, under the clock. Four to seven works well. You can change them any time." | "They sit under the clock. Four to seven is plenty, and you can change them any time." |
| empty home | "Slide along the letters to find an app, then long-press it to pin it here." | "Slide along the letters on the edge to find an app, then hold it to pin it here." |
| set as home card | "Make Letterline your home screen" / "Pressing Home will bring you here instead of your old launcher." | "Use Letterline as your home screen" / "Then the Home button brings you here, not to your old launcher." |
| notification card | "See notifications next to your apps" / "…Nothing leaves your phone." | "Show notifications next to their apps" / "Allow notification access to read and reply from here. It all stays on your phone." |
| search, no results | "No apps match “x”." | "No apps called “x”." |
| backup and restore | "Couldn't save the backup" · "That file couldn't be restored" · "…will be replaced. Widgets need to be added again afterwards." | "The backup wasn't saved. Try another folder." · "That file couldn't be restored. Choose a Letterline backup (.json)." · "This replaces your favorites, folders, app names and settings with the ones in the backup. You'll need to add widgets again." |

## 5. Sweep

`python sweep.py . --config replica/brand.json` on 2026-10-13: **Clean.** It searched for the original's name, company, package and domains, and for its brand colours plus the old Driftlist blues, everywhere except the `replica/` research folder.

The old name is gone from the app, README, privacy page and store text. It remains only where it should:
- Backups and theme files made before the rename still import.
- The GitHub repository is still called `driftlist`. Renaming it is the user's call, and GitHub redirects the old URL.
