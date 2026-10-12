# Letterline privacy

Letterline is a home screen. It can see a lot (your apps, your notifications, your calendar if you allow it), so this page says exactly what it does with that.

**Nothing leaves your phone unless you turn on web suggestions.**

- **No accounts.** There is nothing to sign in to.
- **No analytics, no crash reporting, no ads.** Every build runs a check (`./gradlew checkNoTrackers`) that fails if a tracking, advertising or crash-reporting library is ever added, even indirectly.
- **No network by default.** The internet permission is used for one thing only: live web suggestions in search, which are off until you switch them on. When they are on, what you type in search is sent to the search engine you picked, and nothing else.
- **What stays on the phone:** your favorites, folders, names, hidden apps, themes, settings and Time checks history live in the app's own storage. Backups are files you save and keep.

## Permissions, and why

| Permission | Used for | When |
|---|---|---|
| Notification access | Previews, replies and the music card next to your apps; Notification Summary | Only if you allow it |
| Usage access | Time checks (how long you've been in an app) | Only if you turn Time checks on |
| Calendar | Your next event under the date, and the agenda | Only if you turn the calendar on |
| Contacts | Contact search | Only if you turn it on |
| Accessibility service | Quick Lock (double-tap to turn the screen off). It reads nothing on screen | Only if you switch it on in Android's settings |
| Internet | Live web suggestions | Only if you turn them on |
| Query all packages | Listing your apps, icon packs and widgets: it's a launcher | Always |
| Set wallpaper | Letterline's own wallpapers | When you pick one |

Notification text, calendar events and contacts are read when they're shown and never stored, except notifications you choose to hold for Notification Summary, which wait on the phone until they're delivered.
