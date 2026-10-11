package dev.farjad.driftlist.data

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

    suspend fun setCalendarHidden(id: Long, hidden: Boolean) = context.dataStore.edit {
        val current = it[Keys.hiddenCalendars].orEmpty()
        it[Keys.hiddenCalendars] = if (hidden) current + id.toString() else current - id.toString()
    }

    suspend fun setButtonTarget(target: String?) = context.dataStore.edit {
        if (target == null) it.remove(Keys.buttonTarget) else it[Keys.buttonTarget] = target
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
    }
}
