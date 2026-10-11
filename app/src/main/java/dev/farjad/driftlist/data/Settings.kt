package dev.farjad.driftlist.data

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
)

class Settings(private val context: Context) {
    private object Keys {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val iconPack = stringPreferencesKey("icon_pack")
        val swipeUpSearch = booleanPreferencesKey("swipe_up_search")
        val searchIncludesHidden = booleanPreferencesKey("search_includes_hidden")
        val usageBreaker = booleanPreferencesKey("usage_breaker")
        val favoritesAtBottom = booleanPreferencesKey("favorites_at_bottom")
    }

    val flow: Flow<SettingsSnapshot> = context.dataStore.data.map { p ->
        SettingsSnapshot(
            onboardingDone = p[Keys.onboardingDone] ?: false,
            iconPack = p[Keys.iconPack],
            swipeUpSearch = p[Keys.swipeUpSearch] ?: true,
            searchIncludesHidden = p[Keys.searchIncludesHidden] ?: true,
            usageBreaker = p[Keys.usageBreaker] ?: false,
            favoritesAtBottom = p[Keys.favoritesAtBottom] ?: false,
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

    /** For backup/restore. */
    suspend fun export(): Map<String, Any> = context.dataStore.data.first().asMap().mapKeys { it.key.name }

    suspend fun import(values: Map<String, Any?>) = context.dataStore.edit { p ->
        p.clear()
        (values["onboarding_done"] as? Boolean)?.let { p[Keys.onboardingDone] = it }
        (values["icon_pack"] as? String)?.let { p[Keys.iconPack] = it }
        (values["swipe_up_search"] as? Boolean)?.let { p[Keys.swipeUpSearch] = it }
        (values["search_includes_hidden"] as? Boolean)?.let { p[Keys.searchIncludesHidden] = it }
        (values["usage_breaker"] as? Boolean)?.let { p[Keys.usageBreaker] = it }
        (values["favorites_at_bottom"] as? Boolean)?.let { p[Keys.favoritesAtBottom] = it }
    }
}
