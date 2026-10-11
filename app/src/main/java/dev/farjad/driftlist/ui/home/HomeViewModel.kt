package dev.farjad.driftlist.ui.home

import android.app.Application
import android.graphics.Rect
import android.icu.text.Collator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.farjad.driftlist.DriftlistApp
import dev.farjad.driftlist.data.db.AppEntity
import dev.farjad.driftlist.domain.AlphabetIndexer
import dev.farjad.driftlist.system.SystemActions
import dev.farjad.driftlist.system.notifications.AppKey
import dev.farjad.driftlist.system.notifications.NotifItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

data class AppSection(val letter: String, val apps: List<AppEntity>)

data class AppListModel(
    val sections: List<AppSection> = emptyList(),
    val letters: List<String> = emptyList(),
    val enabledLetters: Set<String> = emptySet(),
    /** Index in the LazyColumn of each section header. */
    val headerIndex: Map<String, Int> = emptyMap(),
)

data class SetupStatus(
    val isDefaultLauncher: Boolean = true,
    val hasNotificationAccess: Boolean = true,
)

data class HomeUiState(
    val loaded: Boolean = false,
    val favorites: List<AppEntity> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val list: AppListModel = AppListModel(),
    val notifications: Map<AppKey, List<NotifItem>> = emptyMap(),
    val setup: SetupStatus = SetupStatus(),
)

val AppEntity.key: AppKey get() = AppKey(packageName, userSerial)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as DriftlistApp).container
    private val appDao = container.db.appDao()
    private val homeDao = container.db.homeDao()

    val icons = container.icons

    private val setup = MutableStateFlow(SetupStatus())

    private val _homePresses = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val homePresses: SharedFlow<Unit> = _homePresses

    private val listModel = appDao.observeVisible()
        .map(::buildList)
        .flowOn(Dispatchers.Default)

    val state: StateFlow<HomeUiState> = combine(
        homeDao.observeFavorites(),
        listModel,
        container.notifications.items,
        setup,
    ) { favorites, list, notifications, setup ->
        HomeUiState(
            loaded = true,
            favorites = favorites,
            favoriteIds = favorites.mapTo(HashSet()) { it.id },
            list = list,
            notifications = notifications,
            setup = setup,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    fun refreshSetup() {
        val app = getApplication<Application>()
        container.notifications.ensureBound()
        setup.value = SetupStatus(
            isDefaultLauncher = SystemActions.isDefaultLauncher(app),
            hasNotificationAccess = container.notifications.hasAccess(),
        )
    }

    fun onHomePressed() {
        _homePresses.tryEmit(Unit)
    }

    fun launch(app: AppEntity, bounds: Rect?) {
        if (container.catalog.launch(app, bounds)) {
            viewModelScope.launch { appDao.markLaunched(app.id, System.currentTimeMillis()) }
        }
    }

    fun toggleFavorite(app: AppEntity) {
        viewModelScope.launch {
            if (app.id in state.value.favoriteIds) {
                homeDao.removeApp(app.id)
            } else {
                homeDao.addApp(app.id, System.currentTimeMillis())
            }
        }
    }

    fun openAppInfo(app: AppEntity, bounds: Rect?) = container.catalog.openAppInfo(app, bounds)

    fun uninstall(app: AppEntity) = container.catalog.uninstall(app)

    fun openNotification(item: NotifItem) = container.notifications.open(item)

    fun dismissNotification(item: NotifItem) = container.notifications.dismiss(item.key)

    private fun buildList(apps: List<AppEntity>): AppListModel {
        val indexer = AlphabetIndexer.forCurrentLocales()
        val collator = Collator.getInstance(Locale.getDefault())
        val byBucket = apps
            .sortedWith { a, b -> collator.compare(a.sortKey, b.sortKey) }
            .groupBy { if (it.bucket in indexer.letters) it.bucket else AlphabetIndexer.OTHER }

        val sections = indexer.letters.mapNotNull { letter ->
            byBucket[letter]?.let { AppSection(letter, it) }
        }
        val headerIndex = HashMap<String, Int>()
        var index = 0
        for (section in sections) {
            headerIndex[section.letter] = index
            index += 1 + section.apps.size
        }
        return AppListModel(
            sections = sections,
            letters = indexer.letters,
            enabledLetters = sections.mapTo(HashSet()) { it.letter },
            headerIndex = headerIndex,
        )
    }
}
