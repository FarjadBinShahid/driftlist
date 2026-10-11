-- Schema for the launcher clone. On-device SQLite, accessed through Room.
-- Source of truth: replica/recon.md "Inferred data model".
--
-- Conventions (SQLite edition of the replica rules):
--   ids        TEXT uuid generated in Kotlin (UUID.randomUUID()). These survive backup/restore
--              between devices, unlike autoincrement ints.
--   times      INTEGER epoch milliseconds, UTC. Convert to local time only in the UI.
--   booleans   INTEGER 0/1 with a CHECK.
--   owner      none. The database is single-user and lives in the app sandbox. The Android
--              *profile* (personal / work / clone) is part of an app's identity, so it is
--              stored as user_serial (UserManager.getSerialNumberForUser), not as an owner.
--   access     enforced by the OS app sandbox; no server, no RLS. Repositories are the
--              only writers (see architecture.md).
--   FKs        Room sets PRAGMA foreign_keys = ON. Every on delete rule below is deliberate.
--
-- Settings that are single values (left-hand mode, haptics, web engine, header config,
-- weather config, Niagara-button actions...) live in DataStore, not here. See architecture.md.

PRAGMA foreign_keys = ON;

-- ---------------------------------------------------------------------------
-- Apps and shortcuts
-- ---------------------------------------------------------------------------

-- One row per launchable activity per profile. Kept in sync with LauncherApps:
-- upserted on startup and on package callbacks, deleted when the package is removed.
-- Deleting cascades to favorites, folder items, icon overrides and pop-up config,
-- so an uninstalled app disappears everywhere at once (S05/S06 edge case).
create table app (
  id                       text primary key,
  package_name             text not null,
  activity_class           text not null,
  user_serial              integer not null,
  system_label             text not null,
  custom_label             text,                      -- S08 rename
  sort_key                 text not null,             -- transliterated, collation-ready label (S06)
  bucket                   text not null,             -- alphabet bucket: 'A'..'Z', locale extras, '#'
  hidden                   integer not null default 0 check (hidden in (0,1)),   -- S24
  hidden_from_suggestions  integer not null default 0 check (hidden_from_suggestions in (0,1)),
  category_folder_id       text references popup_folder(id) on delete set null, -- Pro: folder shown as category
  installed_at             integer not null,
  last_launched_at         integer,
  launch_count             integer not null default 0,
  created_at               integer not null,
  updated_at               integer not null,
  unique (package_name, activity_class, user_serial)
);
create index app_package_user on app (package_name, user_serial);
create index app_sort on app (hidden, bucket, sort_key);       -- S06 list query
create index app_recent on app (installed_at desc);              -- S28
create index app_suggest on app (hidden_from_suggestions, launch_count desc);
create index app_category on app (category_folder_id);

-- Pinned / used deep shortcuts (LauncherApps.ShortcutQuery). Labels cached for fast cold start.
create table shortcut (
  id            text primary key,
  package_name  text not null,
  shortcut_id   text not null,
  user_serial   integer not null,
  label         text not null,
  created_at    integer not null,
  updated_at    integer not null,
  unique (package_name, shortcut_id, user_serial)
);

-- ---------------------------------------------------------------------------
-- Widgets
-- ---------------------------------------------------------------------------

create table widget_stack (
  id          text primary key,
  location    text not null check (location in ('home','app_popup','folder')),
  two_column  integer not null default 0 check (two_column in (0,1)),  -- foldables
  created_at  integer not null,
  updated_at  integer not null
);

create table widget (
  id              text primary key,
  stack_id        text not null references widget_stack(id) on delete cascade,
  app_widget_id   integer not null unique,     -- AppWidgetHost id; free it with deleteAppWidgetId on delete
  provider        text not null,               -- flattened ComponentName
  user_serial     integer not null,
  position        integer not null,
  height_dp       integer,                     -- null = provider default
  created_at      integer not null,
  updated_at      integer not null
);
create index widget_stack_pos on widget (stack_id, position);

-- Hard rule from recon: a stack holds at most 4 widgets.
create trigger widget_stack_max_4
before insert on widget
when (select count(*) from widget where stack_id = new.stack_id) >= 4
begin
  select raise(abort, 'widget stack is full (max 4)');
end;

-- ---------------------------------------------------------------------------
-- Pop-ups and folders
-- ---------------------------------------------------------------------------

create table popup_folder (
  id                text primary key,
  title             text not null,
  widget_stack_id   text references widget_stack(id) on delete set null,   -- Pro
  created_at        integer not null,
  updated_at        integer not null
);
create index popup_folder_stack on popup_folder (widget_stack_id);

-- Folder contents. No nested folders, by design (recon: pop-ups article).
create table folder_item (
  id           text primary key,
  folder_id    text not null references popup_folder(id) on delete cascade,
  position     integer not null,
  kind         text not null check (kind in ('app','shortcut')),
  app_id       text references app(id) on delete cascade,
  shortcut_id  text references shortcut(id) on delete cascade,
  created_at   integer not null,
  check ((kind = 'app' and app_id is not null and shortcut_id is null)
      or (kind = 'shortcut' and shortcut_id is not null and app_id is null))
);
create index folder_item_folder_pos on folder_item (folder_id, position);
create index folder_item_app on folder_item (app_id);
create index folder_item_shortcut on folder_item (shortcut_id);

-- Per-app pop-up customisation (S07). The row only exists once the user edits something.
create table app_popup_config (
  app_id           text primary key references app(id) on delete cascade,
  widget_stack_id  text references widget_stack(id) on delete set null,
  show_shortcuts   integer not null default 1 check (show_shortcuts in (0,1)),
  updated_at       integer not null
);
create index app_popup_stack on app_popup_config (widget_stack_id);

create table popup_hidden_shortcut (
  app_id        text not null references app(id) on delete cascade,
  shortcut_key  text not null,          -- system shortcut id (not necessarily pinned)
  primary key (app_id, shortcut_key)
);

-- ---------------------------------------------------------------------------
-- Home screen
-- ---------------------------------------------------------------------------

-- Ordered favorites list (S05/S10). Exactly one target per row, matching kind.
-- No unique(position): reorders rewrite every position in one transaction instead.
create table home_item (
  id               text primary key,
  position         integer not null,
  kind             text not null check (kind in ('app','folder','widget_stack','shortcut')),
  app_id           text references app(id) on delete cascade,
  folder_id        text references popup_folder(id) on delete cascade,
  widget_stack_id  text references widget_stack(id) on delete cascade,
  shortcut_id      text references shortcut(id) on delete cascade,
  created_at       integer not null,
  updated_at       integer not null,
  check (
    (kind = 'app'          and app_id is not null and folder_id is null and widget_stack_id is null and shortcut_id is null) or
    (kind = 'folder'       and folder_id is not null and app_id is null and widget_stack_id is null and shortcut_id is null) or
    (kind = 'widget_stack' and widget_stack_id is not null and app_id is null and folder_id is null and shortcut_id is null) or
    (kind = 'shortcut'     and shortcut_id is not null and app_id is null and folder_id is null and widget_stack_id is null)
  )
);
create index home_item_pos on home_item (position);
create index home_item_app on home_item (app_id);
create index home_item_folder on home_item (folder_id);
create index home_item_stack on home_item (widget_stack_id);
create index home_item_shortcut on home_item (shortcut_id);
-- An app is a favorite at most once.
create unique index home_item_app_once on home_item (app_id) where app_id is not null;

-- ---------------------------------------------------------------------------
-- Look: icons and themes
-- ---------------------------------------------------------------------------

-- Per-app, per-icon-pack choice, so switching packs and back restores picks (Icon Assistant Auto-Restore).
create table icon_override (
  id             text primary key,
  app_id         text not null references app(id) on delete cascade,
  icon_pack      text not null,            -- icon pack package name, or 'builtin:<id>'
  drawable_name  text,                     -- entry inside the pack
  custom_uri     text,                     -- user-picked image (copied into app storage)
  created_at     integer not null,
  updated_at     integer not null,
  unique (app_id, icon_pack),
  check (drawable_name is not null or custom_uri is not null)
);

create table theme (
  id             text primary key,
  name           text not null,
  source         text not null check (source in ('builtin','custom','imported')),
  wallpaper_uri  text,                     -- copied into app storage; null = keep system wallpaper
  wallpaper_blur integer not null default 0 check (wallpaper_blur between 0 and 100),
  icon_pack      text,                     -- null = system icons
  clock_style    text not null default 'plain',
  font_uri       text,                     -- null = default bundled font
  font_scale     real not null default 1.0 check (font_scale between 0.8 and 1.5),
  accent_color   integer,                  -- ARGB; null = derive from wallpaper
  mode           text not null default 'auto' check (mode in ('light','dark','auto')),
  is_active      integer not null default 0 check (is_active in (0,1)),
  created_at     integer not null,
  updated_at     integer not null
);
-- Exactly zero or one active theme.
create unique index theme_one_active on theme (is_active) where is_active = 1;
create index theme_updated on theme (updated_at desc);   -- My Themes list

-- ---------------------------------------------------------------------------
-- Widgets with data: calendar and weather
-- ---------------------------------------------------------------------------

create table calendar_excluded (
  calendar_id  integer primary key           -- CalendarContract.Calendars._ID
);

-- Single-row cache (id is always 1). Raw provider JSON so the UI can render offline.
create table weather_cache (
  id           integer primary key check (id = 1),
  fetched_at   integer not null,
  lat          real not null,
  lon          real not null,
  payload      text not null                -- JSON: current, hourly[24], daily[7]
);

-- ---------------------------------------------------------------------------
-- Wellbeing: Usage Breaker and Notification Summary
-- ---------------------------------------------------------------------------

-- Usage is tracked per package, not per activity (UsageStatsManager reports packages).
create table usage_rule (
  package_name  text not null,
  user_serial   integer not null,
  created_at    integer not null,
  primary key (package_name, user_serial)
);

create table usage_session (
  id                    text primary key,
  package_name          text not null,
  user_serial           integer not null,
  started_at            integer not null,
  last_seen_at          integer not null,   -- foreground heartbeat; a gap > GRACE ends the session
  last_reminder_minute  integer not null default 0 check (last_reminder_minute in (0,5,10,20,30,45,60,90,120,180,240)),
  ended_at              integer
);
create index usage_session_open on usage_session (package_name, user_serial, ended_at);
create index usage_session_started on usage_session (started_at);   -- cleanup job

create table summary_rule (
  package_name  text not null,               -- '*silent*' = summarize all silent notifications
  user_serial   integer not null,
  primary key (package_name, user_serial)
);

create table notification_batch (
  id             text primary key,
  scheduled_for  integer not null,
  delivered_at   integer,
  cleared_at     integer,
  created_at     integer not null
);
-- At most one open (undelivered) batch.
create unique index batch_one_open on notification_batch ((1)) where delivered_at is null;
create index batch_scheduled on notification_batch (scheduled_for);

create table batch_item (
  batch_id          text not null references notification_batch(id) on delete cascade,
  notification_key  text not null,           -- StatusBarNotification.key
  package_name      text not null,
  title             text,
  body              text,
  posted_at         integer not null,
  content_intent    integer not null default 0 check (content_intent in (0,1)),  -- PendingIntent kept in memory only
  primary key (batch_id, notification_key)   -- idempotent: the same notification re-posted updates, not duplicates
);
create index batch_item_package on batch_item (package_name);
