package dev.farjad.driftlist.ui.home

import dev.farjad.driftlist.domain.ButtonTarget
import android.app.SearchManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import dev.farjad.driftlist.domain.SearchEngine
import dev.farjad.driftlist.system.search.ContactHit
import dev.farjad.driftlist.system.search.WebSuggester
import dev.farjad.driftlist.domain.CalEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import android.app.Application
import android.content.Intent
import android.graphics.Rect
import android.icu.text.Collator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.farjad.driftlist.DriftlistApp
import dev.farjad.driftlist.data.SettingsSnapshot
import dev.farjad.driftlist.data.db.AppEntity
import dev.farjad.driftlist.data.db.HomeItemEntity
import dev.farjad.driftlist.data.db.PopupFolderEntity
import dev.farjad.driftlist.data.db.ShortcutEntity
import dev.farjad.driftlist.data.db.WidgetEntity
import dev.farjad.driftlist.domain.AlphabetIndexer
import dev.farjad.driftlist.domain.AppSearch
import dev.farjad.driftlist.system.SystemActions
import dev.farjad.driftlist.system.apps.ShortcutInfoItem
import dev.farjad.driftlist.system.notifications.AppKey
import dev.farjad.driftlist.system.media.MediaState
import dev.farjad.driftlist.system.notifications.NotifAction
import dev.farjad.driftlist.system.notifications.NotifItem
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

data class AppSection(val letter: String, val apps: List<AppEntity>)

data class AppListModel(
    val sections: List<AppSection> = emptyList(),
    val letters: List<String> = emptyList(),
    val enabledLetters: Set<String> = emptySet(),
    /** Index in the LazyColumn of each section header. */
    val headerIndex: Map<String, Int> = emptyMap(),
)

/** One row on the home screen. */
sealed interface HomeEntry {
    val itemId: String

    data class App(override val itemId: String, val app: AppEntity) : HomeEntry
    data class Folder(
        override val itemId: String,
        val folder: PopupFolderEntity,
        val apps: List<AppEntity>,
        val shortcuts: List<ShortcutEntity> = emptyList(),
    ) : HomeEntry
}

data class SetupStatus(
    val isDefaultLauncher: Boolean = true,
    val hasNotificationAccess: Boolean = true,
)

data class HomeUiState(
    val loaded: Boolean = false,
    val onboardingDone: Boolean = true,
    val favorites: List<HomeEntry> = emptyList(),
    val favoriteAppIds: Set<String> = emptySet(),
    val allApps: List<AppEntity> = emptyList(),
    val hiddenApps: List<AppEntity> = emptyList(),
    val list: AppListModel = AppListModel(),
    val notifications: Map<AppKey, List<NotifItem>> = emptyMap(),
    val setup: SetupStatus = SetupStatus(),
    val headerWidgets: List<WidgetEntity> = emptyList(),
    val settings: SettingsSnapshot = SettingsSnapshot(),
    /** Most-used apps that aren't favorites, shown under them (the reference's "suggestions"). */
    val suggestions: List<AppEntity> = emptyList(),
    val recentlyInstalled: List<AppEntity> = emptyList(),
    val media: MediaState? = null,
    /** Calendar occurrences from yesterday to a week ahead; empty when the calendar is off. */
    val agenda: List<CalEvent> = emptyList(),
    val calendarAccess: Boolean = false,
    val torchOn: Boolean = false,
    /** Music apps to offer while headphones are connected (Plug and Play). */
    val musicApps: List<AppEntity> = emptyList(),
) {
    val headerWidget: WidgetEntity? get() = headerWidgets.firstOrNull()
}

val AppEntity.key: AppKey get() = AppKey(packageName, userSerial)

private data class Library(
    val apps: List<AppEntity>,
    val list: AppListModel,
    val favorites: List<HomeEntry>,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as DriftlistApp).container
    private val db = container.db
    private val appDao = db.appDao()
    private val homeDao = db.homeDao()
    private val folderDao = db.folderDao()

    val icons = container.icons
    val widgetHost = container.widgetHost

    private val setup = MutableStateFlow(SetupStatus())
    private val _homePresses = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)

    /** Emits on every HOME press; true when the user was already looking at home. */
    val homePresses: SharedFlow<Boolean> = _homePresses

    private val library = combine(
        appDao.observeAll(),
        homeDao.observeItems(),
        folderDao.observeFolders(),
        folderDao.observeItems(),
        db.shortcutDao().observeAll(),
    ) { apps, items, folders, folderItems, shortcuts ->
        val byId = apps.associateBy { it.id }
        val shortcutsById = shortcuts.associateBy { it.id }
        val foldersById = folders.associateBy { it.id }
        val byFolder = folderItems.groupBy { it.folderId }.mapValues { (_, list) -> list.sortedBy { it.position } }
        val folderApps = byFolder.mapValues { (_, list) -> list.mapNotNull { fi -> fi.appId?.let(byId::get) } }
        val folderShortcuts = byFolder.mapValues { (_, list) -> list.mapNotNull { fi -> fi.shortcutId?.let(shortcutsById::get) } }
        val favorites = items.mapNotNull { item ->
            when (item.kind) {
                HomeItemEntity.KIND_APP -> item.appId?.let(byId::get)?.let { HomeEntry.App(item.id, it) }
                HomeItemEntity.KIND_FOLDER -> item.folderId?.let(foldersById::get)?.let { f ->
                    HomeEntry.Folder(item.id, f, folderApps[f.id].orEmpty(), folderShortcuts[f.id].orEmpty())
                }
                else -> null
            }
        }
        Library(apps, buildList(apps.filterNot { it.hidden }), favorites)
    }.flowOn(Dispatchers.Default)

    val state: StateFlow<HomeUiState> = combine(
        combine(
            library,
            container.notifications.items,
            setup,
            db.widgetDao().observeHeaderWidgets(),
            container.settings.flow,
        ) { lib, notifications, setup, header, settings ->
            val favoriteIds = lib.favorites.filterIsInstance<HomeEntry.App>().mapTo(HashSet()) { it.app.id }
            val nowMs = System.currentTimeMillis()
            HomeUiState(
                loaded = true,
                onboardingDone = settings.onboardingDone,
                favorites = lib.favorites,
                favoriteAppIds = favoriteIds,
                allApps = lib.apps,
                hiddenApps = lib.apps.filter { it.hidden }.sortedBy { it.label.lowercase() },
                list = if (settings.listSortUsage) lib.list.mostUsedFirst() else lib.list,
                notifications = notifications,
                setup = setup,
                headerWidgets = header,
                settings = settings,
                suggestions = if (!settings.suggestApps) emptyList() else lib.apps
                    .filter { !it.hidden && !it.hiddenFromSuggestions && it.id !in favoriteIds && it.launchCount > 0 }
                    .sortedWith(compareByDescending<AppEntity> { it.launchCount }.thenByDescending { it.lastLaunchedAt ?: 0L })
                    .take(MAX_SUGGESTIONS),
                recentlyInstalled = lib.apps
                    .filter { !it.hidden && nowMs - it.installedAt < RECENT_WINDOW_MS }
                    .sortedByDescending { it.installedAt },
            )
        },
        container.media.state,
        agendaFlow(),
        container.calendar.access,
        combine(container.torch.on, container.headphones.connected) { torch, phones -> torch to phones },
    ) { s, media, agenda, calendarAccess, (torch, phones) ->
        s.copy(
            media = media,
            agenda = agenda,
            calendarAccess = calendarAccess,
            torchOn = torch && s.settings.torchReminder,
            musicApps = if (!phones || !s.settings.plugAndPlay) emptyList() else musicAppsFrom(s.allApps),
        )
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState(loaded = false))

    /** When an app was last opened from the A-Z list; Home within a few seconds returns there. */
    var listLaunchAt = 0L
        private set

    private val now get() = System.currentTimeMillis()

    init {
        // People upgrading from 0.1.0 already have favorites: never show them first-run onboarding.
        viewModelScope.launch {
            val first = state.first { it.loaded }
            if (!first.onboardingDone && first.favorites.isNotEmpty()) container.settings.setOnboardingDone(true)
        }
    }

    // --- setup ------------------------------------------------------------------------------

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun agendaFlow(): Flow<List<CalEvent>> = combine(
        container.settings.flow.map { it.calendarEvents to it.hiddenCalendars }.distinctUntilChanged(),
        container.calendar.access,
    ) { (on, hidden), access -> Triple(on, hidden, access) }
        .flatMapLatest { (on, hidden, access) ->
            if (!on || !access) flowOf(emptyList()) else container.calendar.changes().map { container.calendar.upcoming(hidden) }
        }
        .flowOn(Dispatchers.IO)

    fun setHeaderPosition(ratio: Float) {
        viewModelScope.launch { container.settings.setHeaderPosition(ratio) }
    }

    fun openEvent(event: CalEvent) = container.calendar.open(event)
    fun newCalendarEvent() = container.calendar.newEvent()

    fun refreshSetup() {
        val app = getApplication<Application>()
        container.calendar.refreshAccess()
        container.notifications.ensureBound()
        setup.value = SetupStatus(
            isDefaultLauncher = SystemActions.isDefaultLauncher(app),
            hasNotificationAccess = container.notifications.hasAccess(),
        )
    }

    fun setFavoritesAtBottom(value: Boolean) {
        viewModelScope.launch { container.settings.setFavoritesAtBottom(value) }
    }

    fun completeOnboarding(appIds: List<String>) {
        viewModelScope.launch {
            homeDao.replaceWithApps(appIds, now)
            container.settings.setOnboardingDone(true)
        }
    }

    fun onHomePressed(alreadyHome: Boolean) {
        _homePresses.tryEmit(alreadyHome)
    }

    // --- apps -------------------------------------------------------------------------------

    fun launchFromList(app: AppEntity, bounds: Rect?) {
        listLaunchAt = System.currentTimeMillis()
        launch(app, bounds)
    }

    fun stopSuggesting(app: AppEntity) {
        viewModelScope.launch { appDao.setHiddenFromSuggestions(app.id, true, now) }
    }

    fun setFlag(name: String, value: Boolean) {
        viewModelScope.launch { container.settings.setFlag(name, value) }
    }

    fun setButtonTarget(target: String?, swipe: Boolean = false) {
        viewModelScope.launch {
            if (swipe) {
                container.settings.setButtonSwipeTarget(target)
            } else {
                container.settings.setButtonTarget(target)
                container.settings.setFlag("button_enabled", target != null)
            }
            repinShortcuts()
        }
    }

    fun launchButtonShortcut(t: ButtonTarget.Shortcut, bounds: Rect?) =
        container.catalog.launchStoredShortcut(t.packageName, t.shortcutId, t.userSerial, bounds)

    fun torchOff() = container.torch.turnOff()

    private var musicPackages: Set<String>? = null

    private fun musicAppsFrom(apps: List<AppEntity>): List<AppEntity> {
        val pkgs = musicPackages ?: container.headphones.musicPackages().also { musicPackages = it }
        return apps.filter { !it.hidden && it.packageName in pkgs }
            .sortedWith(compareByDescending<AppEntity> { it.launchCount }.thenBy { it.label.lowercase() })
            .distinctBy { it.packageName }
            .take(MAX_MUSIC_APPS)
    }

    fun mediaPlayPause() = container.media.playPause()
    fun mediaNext() = container.media.next()
    fun mediaPrevious() = container.media.previous()
    fun mediaOpen() = container.media.open()

    fun launch(app: AppEntity, bounds: Rect?) {
        if (container.catalog.launch(app, bounds)) {
            viewModelScope.launch { appDao.markLaunched(app.id, now) }
        }
    }

    fun toggleFavorite(app: AppEntity) {
        viewModelScope.launch {
            if (app.id in state.value.favoriteAppIds) homeDao.removeApp(app.id) else homeDao.addApp(app.id, now)
        }
    }

    fun rename(app: AppEntity, label: String) {
        val clean = label.trim().takeIf { it.isNotEmpty() && it != app.systemLabel }
        viewModelScope.launch { appDao.setCustomLabel(app.id, clean, now) }
    }

    fun setHidden(app: AppEntity, hidden: Boolean) {
        viewModelScope.launch { appDao.setHidden(app.id, hidden, now) }
    }

    fun openAppInfo(app: AppEntity, bounds: Rect?) = container.catalog.openAppInfo(app, bounds)

    fun uninstall(app: AppEntity) = container.catalog.uninstall(app)

    fun shortcutsFor(app: AppEntity): List<ShortcutInfoItem> = container.catalog.shortcuts(app)

    fun launchShortcut(item: ShortcutInfoItem, bounds: Rect?) = container.catalog.launchShortcut(item, bounds)

    suspend fun shortcutIcon(item: ShortcutInfoItem, sizePx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        container.catalog.shortcutIcon(item)?.let { runCatching { it.toBitmap(sizePx, sizePx).asImageBitmap() }.getOrNull() }
    }

    fun iconPackLabel(pkg: String): String? = container.iconPacks.installed().firstOrNull { it.packageName == pkg }?.label

    /** Per-app icon from the current pack; null resets to the pack's (or system) default. */
    fun setIcon(app: AppEntity, drawableName: String?) {
        val pack = state.value.settings.iconPack ?: return
        viewModelScope.launch {
            if (drawableName == null) {
                db.iconOverrideDao().delete(app.id, pack)
            } else {
                db.iconOverrideDao().set(app.id, pack, drawableName, now)
            }
        }
    }

    // --- favorites and folders --------------------------------------------------------------

    fun addFavorites(appIds: List<String>) {
        viewModelScope.launch { appIds.forEach { homeDao.addApp(it, now) } }
    }

    fun reorderFavorites(orderedItemIds: List<String>) {
        viewModelScope.launch { homeDao.reorder(orderedItemIds, now) }
    }

    fun removeFavorite(entry: HomeEntry) {
        viewModelScope.launch {
            when (entry) {
                is HomeEntry.App -> homeDao.remove(entry.itemId)
                is HomeEntry.Folder -> folderDao.delete(entry.folder.id) // cascades to the home row
            }
        }
    }

    fun createFolder(title: String, appIds: List<String>) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            folderDao.insertFolder(PopupFolderEntity(id, title.trim().ifEmpty { "Folder" }, createdAt = now, updatedAt = now))
            folderDao.setApps(id, appIds, now)
            homeDao.addFolder(id, now)
        }
    }

    /** Adds one of an app's shortcuts to a folder; a null [folderId] creates a new folder named [newTitle]. */
    fun addShortcutToFolder(app: AppEntity, shortcut: ShortcutInfoItem, folderId: String?, newTitle: String) {
        viewModelScope.launch {
            val entity = db.shortcutDao().getOrCreate(app.packageName, shortcut.id, app.userSerial, shortcut.label, now)
            val target = folderId ?: UUID.randomUUID().toString().also { id ->
                folderDao.insertFolder(PopupFolderEntity(id, newTitle.trim().ifEmpty { "Folder" }, createdAt = now, updatedAt = now))
                homeDao.addFolder(id, now)
            }
            folderDao.addShortcut(target, entity.id, now)
            repinShortcuts()
        }
    }

    fun removeShortcutFromFolder(folderId: String, shortcut: ShortcutEntity) {
        viewModelScope.launch {
            folderDao.removeShortcut(folderId, shortcut.id)
            db.shortcutDao().deleteUnused()
            repinShortcuts()
        }
    }

    fun launchStoredShortcut(shortcut: ShortcutEntity, bounds: Rect?) =
        container.catalog.launchStoredShortcut(shortcut.packageName, shortcut.shortcutId, shortcut.userSerial, bounds)

    suspend fun storedShortcutIcon(shortcut: ShortcutEntity, sizePx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        container.catalog.storedShortcut(shortcut.packageName, shortcut.shortcutId, shortcut.userSerial)
            ?.let { container.catalog.shortcutIcon(it) }
            ?.let { runCatching { it.toBitmap(sizePx, sizePx).asImageBitmap() }.getOrNull() }
    }

    /** Pins every shortcut Driftlist uses (folders and the round button); pinning replaces per package. */
    private suspend fun repinShortcuts() {
        val stored = db.shortcutDao().all().map { (it.packageName to it.userSerial) to it.shortcutId }
        val s = container.settings.current()
        val button = listOf(s.buttonTarget, s.buttonSwipeTarget).mapNotNull { ButtonTarget.parse(it) as? ButtonTarget.Shortcut }
            .map { (it.packageName to it.userSerial) to it.shortcutId }
        container.catalog.pinShortcuts((stored + button).groupBy({ it.first }, { it.second }).mapValues { (_, ids) -> ids.distinct() })
    }

    fun updateFolder(folderId: String, title: String, appIds: List<String>) {
        viewModelScope.launch {
            folderDao.rename(folderId, title.trim().ifEmpty { "Folder" }, now)
            folderDao.setApps(folderId, appIds, now)
        }
    }

    // --- notifications ----------------------------------------------------------------------

    fun openNotification(item: NotifItem) = container.notifications.open(item)

    fun dismissNotification(item: NotifItem) = container.notifications.dismiss(item.key)

    fun reply(item: NotifItem, text: String): Boolean = container.notifications.reply(item, text)

    fun runAction(action: NotifAction) = container.notifications.runAction(action)

    // --- header widget ----------------------------------------------------------------------

    fun allocateWidgetId(): Int = widgetHost.allocate()

    fun bindWidgetIntent(appWidgetId: Int, provider: android.appwidget.AppWidgetProviderInfo): Intent? =
        widgetHost.bindOrIntent(appWidgetId, provider)

    /** Adds to the header stack (up to 4 widgets). */
    fun saveHeaderWidget(appWidgetId: Int) {
        val info = widgetHost.info(appWidgetId) ?: return widgetHost.release(appWidgetId)
        viewModelScope.launch {
            val added = db.widgetDao().addHeaderWidget(
                appWidgetId,
                info.provider.flattenToString(),
                widgetHost.serialOf(info.profile),
                now,
            )
            if (!added) widgetHost.release(appWidgetId)
        }
    }

    fun removeHeaderWidget(appWidgetId: Int) {
        viewModelScope.launch {
            db.widgetDao().deleteWidget(appWidgetId)
            widgetHost.release(appWidgetId)
        }
    }

    fun releaseWidgetId(appWidgetId: Int) = widgetHost.release(appWidgetId)

    // --- search -----------------------------------------------------------------------------

    fun searchContacts(query: String): List<ContactHit> =
        if (state.value.settings.searchContacts) container.contacts.search(query) else emptyList()

    fun contactsNeedPermission(): Boolean = state.value.settings.searchContacts && !container.contacts.hasPermission()

    suspend fun webSuggestions(query: String): List<String> {
        val s = state.value.settings
        if (!s.webSuggestions || query.trim().length < 2) return emptyList()
        return WebSuggester.fetch(s.searchEngine, query)
    }

    fun webSearch(query: String) {
        val app = getApplication<Application>()
        val url = state.value.settings.searchEngine.searchUrl(query)
        val intent = if (url == null) {
            Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        }
        if (!SystemActions.start(app, intent) && url == null) {
            SystemActions.start(app, Intent(Intent.ACTION_VIEW, Uri.parse(SearchEngine.DuckDuckGo.searchUrl(query))))
        }
    }

    fun openContact(hit: ContactHit) = container.contacts.open(hit)
    fun callContact(hit: ContactHit) = container.contacts.call(hit)
    fun messageContact(hit: ContactHit) = container.contacts.message(hit)

    /** Digital Wellbeing's page for one app (Android 10+), when the phone has one. */
    fun screenTimeIntent(app: AppEntity): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val intent = Intent(Settings.ACTION_APP_USAGE_SETTINGS).putExtra(Intent.EXTRA_PACKAGE_NAME, app.packageName)
        val pm = getApplication<Application>().packageManager
        return intent.takeIf { it.resolveActivity(pm) != null }
    }

    fun search(query: String): List<AppEntity> {
        val s = state.value
        val pool = if (s.settings.searchIncludesHidden) s.allApps else s.allApps.filterNot { it.hidden }
        return AppSearch.rank(query, pool)
    }

    private companion object {
        const val MAX_SUGGESTIONS = 3
        const val MAX_MUSIC_APPS = 4
        const val RECENT_WINDOW_MS = 14L * 24 * 60 * 60 * 1000
    }

    private fun buildList(apps: List<AppEntity>): AppListModel {
        val indexer = AlphabetIndexer.forCurrentLocales()
        val collator = Collator.getInstance(Locale.getDefault())
        // A renamed app sorts under its new name.
        val effective = apps.map { a ->
            a.customLabel?.let { a.copy(sortKey = indexer.sortKey(it), bucket = indexer.bucketOf(it)) } ?: a
        }
        val byBucket = effective
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

/** Same sections and letters; inside each letter, most-opened first (ties keep A-Z order). */
private fun AppListModel.mostUsedFirst(): AppListModel =
    copy(sections = sections.map { sec -> sec.copy(apps = sec.apps.sortedByDescending { it.launchCount }) })
