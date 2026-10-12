package dev.farjad.driftlist.data

import androidx.datastore.preferences.core.intPreferencesKey
import dev.farjad.driftlist.domain.SearchEngine
import androidx.datastore.preferences.core.floatPreferencesKey
import dev.farjad.driftlist.ui.theme.SizeTokens
import androidx.datastore.preferences.core.stringSetPreferencesKey
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Single-value settings. Tables live in Room; everything here is one key, one value. */
data class SettingsSnapshot(
    val onboardingDone: Boolean = false,
    /** Icon pack package name, or null for system icons. */
    val iconPack: String? = null,
    val swipeUpSearch: Boolean = true,
    val searchIncludesHidden: Boolean = true,
    val usageBreaker: Boolean = false,
    /** false = favorites follow the clock (the reference's layout); true = anchored to the bottom. */
    val favoritesAtBottom: Boolean = false,
    val suggestApps: Boolean = true,
    val mediaCard: Boolean = true,
    val buttonEnabled: Boolean = false,
    /** "app:<appId>" or "folder:<folderId>", or null when unassigned. */
    val buttonTarget: String? = null,
    val leftHanded: Boolean = false,
    val haptics: Boolean = true,
    val quickLock: Boolean = false,
    val notificationSummary: Boolean = false,
    /** Next event under the date, agenda on tapping it. Needs READ_CALENDAR. */
    val calendarEvents: Boolean = false,
    /** Calendars left out of the header and agenda. Device-specific ids, so not part of backups. */
    val hiddenCalendars: Set<Long> = emptySet(),
    /** Theme Studio's current look (clock, font, size, accent, light/dark). */
    val look: Look = Look(),
    /** Where the clock sits, as a fraction of the screen height from the top ("Move clock"). */
    val headerPosition: Float = SizeTokens.headerTopSpaceRatio,
    val searchEngine: SearchEngine = SearchEngine.System,
    /** Off by default: when on, typed search text goes to [searchEngine] for suggestions. */
    val webSuggestions: Boolean = false,
    val searchContacts: Boolean = false,
    /** Within each letter, most-opened apps first. */
    val listSortUsage: Boolean = false,
    val quickReplies: Boolean = true,
    val allowRotation: Boolean = false,
    val hideStatusBar: Boolean = false,
    /** What swiping up on the round button opens; same format as [buttonTarget]. */
    val buttonSwipeTarget: String? = null,
    val plugAndPlay: Boolean = true,
    val torchReminder: Boolean = true,
    /** Show the header widget(s) in place of the clock. */
    val replaceClock: Boolean = false,
    /** Apps whose pop-up leaves out the app's shortcuts. */
    val shortcutsHidden: Set<String> = emptySet(),
    /** Favorites folders also listed as categories at the end of the A-Z list. */
    val foldersInList: Boolean = false,
    /** 0 off, 1 light, 2 strong (Android 12+ phones that support window blur). */
    val wallpaperBlur: Int = 0,
    /** Round button: swipe down and double-tap targets (same format as [buttonTarget]). */
    val buttonDownTarget: String? = null,
    val buttonDoubleTarget: String? = null,
    /** Cards (music, flashlight, headphones) go below favorites so favorites never move. */
    val pinFavorites: Boolean = false,
    /** "start", "center" or "end". */
    val favoritesAlign: String = "start",
    /** 0 = list; 4 or 5 = an icon grid with that many columns. */
    val favoritesGrid: Int = 0,
    val gridLabels: Boolean = true,
    val searchPill: Boolean = false,
    /** A small dot for unread notifications instead of preview text. */
    val notifDots: Boolean = false,
)

class Settings(private val context: Context) {
    private object Keys {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val iconPack = stringPreferencesKey("icon_pack")
        val swipeUpSearch = booleanPreferencesKey("swipe_up_search")
        val searchIncludesHidden = booleanPreferencesKey("search_includes_hidden")
        val usageBreaker = booleanPreferencesKey("usage_breaker")
        val favoritesAtBottom = booleanPreferencesKey("favorites_at_bottom")
        val suggestApps = booleanPreferencesKey("suggest_apps")
        val mediaCard = booleanPreferencesKey("media_card")
        val buttonEnabled = booleanPreferencesKey("button_enabled")
        val buttonTarget = stringPreferencesKey("button_target")
        val leftHanded = booleanPreferencesKey("left_handed")
        val haptics = booleanPreferencesKey("haptics")
        val quickLock = booleanPreferencesKey("quick_lock")
        val notificationSummary = booleanPreferencesKey("notification_summary")
        val calendarEvents = booleanPreferencesKey("calendar_events")
        val hiddenCalendars = stringSetPreferencesKey("hidden_calendars")
        val look = stringPreferencesKey("look")
        val headerPosition = floatPreferencesKey("header_position")
        val searchEngine = stringPreferencesKey("search_engine")
        val webSuggestions = booleanPreferencesKey("web_suggestions")
        val searchContacts = booleanPreferencesKey("search_contacts")
        val listSortUsage = booleanPreferencesKey("list_sort_usage")
        val quickReplies = booleanPreferencesKey("quick_replies")
        val allowRotation = booleanPreferencesKey("allow_rotation")
        val hideStatusBar = booleanPreferencesKey("hide_status_bar")
        val buttonSwipeTarget = stringPreferencesKey("button_swipe_target")
        val plugAndPlay = booleanPreferencesKey("plug_and_play")
        val torchReminder = booleanPreferencesKey("torch_reminder")
        val replaceClock = booleanPreferencesKey("replace_clock")
        val shortcutsHidden = stringSetPreferencesKey("shortcuts_hidden")
        val foldersInList = booleanPreferencesKey("folders_in_list")
        val wallpaperBlur = intPreferencesKey("wallpaper_blur")
        val buttonDownTarget = stringPreferencesKey("button_down_target")
        val buttonDoubleTarget = stringPreferencesKey("button_double_target")
        val pinFavorites = booleanPreferencesKey("pin_favorites")
        val favoritesAlign = stringPreferencesKey("favorites_align")
        val favoritesGrid = intPreferencesKey("favorites_grid")
        val gridLabels = booleanPreferencesKey("grid_labels")
        val searchPill = booleanPreferencesKey("search_pill")
        val notifDots = booleanPreferencesKey("notif_dots")
    }

    private val booleanKeys = mapOf(
        "onboarding_done" to Keys.onboardingDone,
        "swipe_up_search" to Keys.swipeUpSearch,
        "search_includes_hidden" to Keys.searchIncludesHidden,
        "usage_breaker" to Keys.usageBreaker,
        "favorites_at_bottom" to Keys.favoritesAtBottom,
        "suggest_apps" to Keys.suggestApps,
        "media_card" to Keys.mediaCard,
        "button_enabled" to Keys.buttonEnabled,
        "left_handed" to Keys.leftHanded,
        "haptics" to Keys.haptics,
        "quick_lock" to Keys.quickLock,
        "notification_summary" to Keys.notificationSummary,
        "calendar_events" to Keys.calendarEvents,
        "web_suggestions" to Keys.webSuggestions,
        "search_contacts" to Keys.searchContacts,
        "list_sort_usage" to Keys.listSortUsage,
        "quick_replies" to Keys.quickReplies,
        "allow_rotation" to Keys.allowRotation,
        "hide_status_bar" to Keys.hideStatusBar,
        "plug_and_play" to Keys.plugAndPlay,
        "torch_reminder" to Keys.torchReminder,
        "replace_clock" to Keys.replaceClock,
        "folders_in_list" to Keys.foldersInList,
        "pin_favorites" to Keys.pinFavorites,
        "grid_labels" to Keys.gridLabels,
        "search_pill" to Keys.searchPill,
        "notif_dots" to Keys.notifDots,
    )

    val flow: Flow<SettingsSnapshot> = context.dataStore.data.map { p ->
        SettingsSnapshot(
            onboardingDone = p[Keys.onboardingDone] ?: false,
            iconPack = p[Keys.iconPack],
            swipeUpSearch = p[Keys.swipeUpSearch] ?: true,
            searchIncludesHidden = p[Keys.searchIncludesHidden] ?: true,
            usageBreaker = p[Keys.usageBreaker] ?: false,
            favoritesAtBottom = p[Keys.favoritesAtBottom] ?: false,
            suggestApps = p[Keys.suggestApps] ?: true,
            mediaCard = p[Keys.mediaCard] ?: true,
            buttonEnabled = p[Keys.buttonEnabled] ?: false,
            buttonTarget = p[Keys.buttonTarget],
            leftHanded = p[Keys.leftHanded] ?: false,
            haptics = p[Keys.haptics] ?: true,
            quickLock = p[Keys.quickLock] ?: false,
            notificationSummary = p[Keys.notificationSummary] ?: false,
            calendarEvents = p[Keys.calendarEvents] ?: false,
            hiddenCalendars = p[Keys.hiddenCalendars].orEmpty().mapNotNullTo(HashSet()) { it.toLongOrNull() },
            look = Look.fromJson(p[Keys.look]),
            headerPosition = (p[Keys.headerPosition] ?: SizeTokens.headerTopSpaceRatio)
                .coerceIn(SizeTokens.headerPositionMin, SizeTokens.headerPositionMax),
            searchEngine = SearchEngine.fromName(p[Keys.searchEngine]),
            webSuggestions = p[Keys.webSuggestions] ?: false,
            searchContacts = p[Keys.searchContacts] ?: false,
            listSortUsage = p[Keys.listSortUsage] ?: false,
            quickReplies = p[Keys.quickReplies] ?: true,
            allowRotation = p[Keys.allowRotation] ?: false,
            hideStatusBar = p[Keys.hideStatusBar] ?: false,
            buttonSwipeTarget = p[Keys.buttonSwipeTarget],
            plugAndPlay = p[Keys.plugAndPlay] ?: true,
            torchReminder = p[Keys.torchReminder] ?: true,
            replaceClock = p[Keys.replaceClock] ?: false,
            shortcutsHidden = p[Keys.shortcutsHidden].orEmpty(),
            foldersInList = p[Keys.foldersInList] ?: false,
            wallpaperBlur = (p[Keys.wallpaperBlur] ?: 0).coerceIn(0, 2),
            buttonDownTarget = p[Keys.buttonDownTarget],
            buttonDoubleTarget = p[Keys.buttonDoubleTarget],
            pinFavorites = p[Keys.pinFavorites] ?: false,
            favoritesAlign = (p[Keys.favoritesAlign] ?: "start").takeIf { it in setOf("start", "center", "end") } ?: "start",
            favoritesGrid = (p[Keys.favoritesGrid] ?: 0).takeIf { it in setOf(0, 4, 5) } ?: 0,
            gridLabels = p[Keys.gridLabels] ?: true,
            searchPill = p[Keys.searchPill] ?: false,
            notifDots = p[Keys.notifDots] ?: false,
        )
    }

    suspend fun current(): SettingsSnapshot = flow.first()

    suspend fun setOnboardingDone(value: Boolean) = context.dataStore.edit { it[Keys.onboardingDone] = value }
    suspend fun setIconPack(pkg: String?) = context.dataStore.edit {
        if (pkg == null) it.remove(Keys.iconPack) else it[Keys.iconPack] = pkg
    }
    suspend fun setSwipeUpSearch(value: Boolean) = context.dataStore.edit { it[Keys.swipeUpSearch] = value }
    suspend fun setSearchIncludesHidden(value: Boolean) = context.dataStore.edit { it[Keys.searchIncludesHidden] = value }
    suspend fun setUsageBreaker(value: Boolean) = context.dataStore.edit { it[Keys.usageBreaker] = value }
    suspend fun setFavoritesAtBottom(value: Boolean) = context.dataStore.edit { it[Keys.favoritesAtBottom] = value }

    /** Generic boolean setter for the simple switches (keys as in [booleanKeys]). */
    suspend fun setFlag(name: String, value: Boolean) {
        val key = booleanKeys[name] ?: error("unknown setting $name")
        context.dataStore.edit { it[key] = value }
    }

    suspend fun setSearchEngine(engine: SearchEngine) = context.dataStore.edit { it[Keys.searchEngine] = engine.name }

    suspend fun setLook(look: Look) = context.dataStore.edit { it[Keys.look] = look.toJson() }

    suspend fun setHeaderPosition(ratio: Float) = context.dataStore.edit {
        it[Keys.headerPosition] = ratio.coerceIn(SizeTokens.headerPositionMin, SizeTokens.headerPositionMax)
    }

    suspend fun setWallpaperBlur(level: Int) = context.dataStore.edit { it[Keys.wallpaperBlur] = level.coerceIn(0, 2) }

    suspend fun setShortcutsHidden(appId: String, hidden: Boolean) = context.dataStore.edit {
        val current = it[Keys.shortcutsHidden].orEmpty()
        it[Keys.shortcutsHidden] = if (hidden) current + appId else current - appId
    }

    suspend fun setCalendarHidden(id: Long, hidden: Boolean) = context.dataStore.edit {
        val current = it[Keys.hiddenCalendars].orEmpty()
        it[Keys.hiddenCalendars] = if (hidden) current + id.toString() else current - id.toString()
    }

    suspend fun setButtonTarget(target: String?) = context.dataStore.edit {
        if (target == null) it.remove(Keys.buttonTarget) else it[Keys.buttonTarget] = target
    }

    suspend fun setButtonSwipeTarget(target: String?) = context.dataStore.edit {
        if (target == null) it.remove(Keys.buttonSwipeTarget) else it[Keys.buttonSwipeTarget] = target
    }

    /** [slot]: 0 tap, 1 swipe up, 2 swipe down, 3 double-tap. */
    suspend fun setButtonSlot(slot: Int, target: String?) = context.dataStore.edit {
        val key = when (slot) {
            0 -> Keys.buttonTarget
            1 -> Keys.buttonSwipeTarget
            2 -> Keys.buttonDownTarget
            else -> Keys.buttonDoubleTarget
        }
        if (target == null) it.remove(key) else it[key] = target
    }

    suspend fun setFavoritesLayout(align: String, grid: Int) = context.dataStore.edit {
        it[Keys.favoritesAlign] = align
        it[Keys.favoritesGrid] = grid
    }

    /** For backup/restore. */
    suspend fun export(): Map<String, Any> = context.dataStore.data.first().asMap()
        .mapKeys { it.key.name }
        .filterKeys { it != "hidden_calendars" }

    suspend fun import(values: Map<String, Any?>) = context.dataStore.edit { p ->
        p.clear()
        for ((name, key) in booleanKeys) (values[name] as? Boolean)?.let { p[key] = it }
        (values["icon_pack"] as? String)?.let { p[Keys.iconPack] = it }
        (values["button_target"] as? String)?.let { p[Keys.buttonTarget] = it }
        (values["look"] as? String)?.let { p[Keys.look] = it }
        (values["search_engine"] as? String)?.let { p[Keys.searchEngine] = it }
        (values["button_swipe_target"] as? String)?.let { p[Keys.buttonSwipeTarget] = it }
        (values["wallpaper_blur"] as? Number)?.let { p[Keys.wallpaperBlur] = it.toInt() }
        (values["button_down_target"] as? String)?.let { p[Keys.buttonDownTarget] = it }
        (values["button_double_target"] as? String)?.let { p[Keys.buttonDoubleTarget] = it }
        (values["favorites_align"] as? String)?.let { p[Keys.favoritesAlign] = it }
        (values["favorites_grid"] as? Number)?.let { p[Keys.favoritesGrid] = it.toInt() }
        (values["shortcuts_hidden"] as? org.json.JSONArray)?.let { arr -> p[Keys.shortcutsHidden] = (0 until arr.length()).map(arr::getString).toSet() }
        @Suppress("UNCHECKED_CAST")
        (values["shortcuts_hidden"] as? Collection<String>)?.let { p[Keys.shortcutsHidden] = it.toSet() }
        (values["header_position"] as? Number)?.let { p[Keys.headerPosition] = it.toFloat() }
    }
}
