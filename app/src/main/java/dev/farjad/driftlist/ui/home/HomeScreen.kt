package dev.farjad.driftlist.ui.home

import dev.farjad.driftlist.ui.components.MoveClockSheet
import android.Manifest
import android.app.Activity
import android.net.Uri
import androidx.core.app.ActivityCompat
import dev.farjad.driftlist.ui.components.AgendaSheet
import android.app.SearchManager
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.farjad.driftlist.data.db.AppEntity
import dev.farjad.driftlist.data.db.WidgetDao
import dev.farjad.driftlist.system.QuickLockService
import dev.farjad.driftlist.system.SystemActions
import dev.farjad.driftlist.system.notifications.NotifItem
import dev.farjad.driftlist.ui.components.AddFavoritesSheet
import dev.farjad.driftlist.ui.components.AppOptionsSheet
import dev.farjad.driftlist.ui.components.AppPopupCard
import dev.farjad.driftlist.ui.components.Explainers
import dev.farjad.driftlist.ui.components.PermissionDialog
import dev.farjad.driftlist.ui.components.PermissionExplainer
import dev.farjad.driftlist.ui.components.ShortcutFolderDialog
import dev.farjad.driftlist.domain.RelativeTime
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.launch
import dev.farjad.driftlist.ui.components.AppRow
import dev.farjad.driftlist.ui.components.AppRowVariant
import dev.farjad.driftlist.ui.components.ClockHeader
import dev.farjad.driftlist.ui.components.EditFavoritesSheet
import dev.farjad.driftlist.ui.components.FolderEditorSheet
import dev.farjad.driftlist.ui.components.FolderOptionsSheet
import dev.farjad.driftlist.ui.components.FolderRow
import dev.farjad.driftlist.ui.components.FolderSheet
import dev.farjad.driftlist.ui.components.HeaderOptionsSheet
import dev.farjad.driftlist.ui.components.ActionButton
import dev.farjad.driftlist.ui.components.ButtonTargetSheet
import dev.farjad.driftlist.ui.components.MediaCard
import dev.farjad.driftlist.ui.components.RecentlyInstalledSheet
import dev.farjad.driftlist.ui.components.SearchButton
import dev.farjad.driftlist.ui.components.WidgetStack
import dev.farjad.driftlist.ui.components.IconPickerSheet
import dev.farjad.driftlist.ui.components.Onboarding
import dev.farjad.driftlist.ui.components.SearchOverlay
import dev.farjad.driftlist.ui.components.SetupCard
import dev.farjad.driftlist.ui.components.WaveAlphabet
import dev.farjad.driftlist.ui.components.WidgetPickerSheet
import dev.farjad.driftlist.ui.theme.LocalHomeColors
import dev.farjad.driftlist.ui.theme.ScrimTokens
import dev.farjad.driftlist.ui.theme.SizeTokens
import dev.farjad.driftlist.ui.theme.SpaceTokens
import dev.farjad.driftlist.ui.theme.TypeTokens
import dev.farjad.driftlist.ui.theme.onWallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

private enum class Pane { Favorites, AllApps }

/** Everything that can sit on top of home. One at a time. */
private sealed interface Overlay {
    data class Popup(val app: AppEntity, val anchor: android.graphics.Rect?) : Overlay
    data class AddShortcut(val app: AppEntity, val shortcut: dev.farjad.driftlist.system.apps.ShortcutInfoItem) : Overlay
    data class Permission(val explainer: PermissionExplainer, val intent: Intent) : Overlay
    data class Options(val app: AppEntity) : Overlay
    data class IconPicker(val app: AppEntity) : Overlay
    data class Folder(val entry: HomeEntry.Folder) : Overlay
    data class FolderOptions(val entry: HomeEntry.Folder) : Overlay
    data class FolderEditor(val folderId: String?, val title: String, val appIds: List<String>) : Overlay
    data object EditFavorites : Overlay
    data object AddFavorites : Overlay
    data object HeaderOptions : Overlay
    data object WidgetPicker : Overlay
    data object Search : Overlay
    data object ButtonTarget : Overlay
    data object Recent : Overlay
    data object Agenda : Overlay
    data object MoveClock : Overlay
}

/** S05 favorites + S06 app list sharing one wave alphabet, plus every sheet opened from them. */
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onOpenSettings: () -> Unit,
    onConfigureWidget: (appWidgetId: Int, done: (Boolean) -> Unit) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = LocalHomeColors.current

    var pane by rememberSaveable { mutableStateOf(Pane.Favorites) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    val listState = rememberLazyListState()
    val latestState by rememberUpdatedState(state)

    BackHandler(enabled = pane == Pane.AllApps && overlay == null) { pane = Pane.Favorites }
    LaunchedEffect(Unit) {
        vm.homePresses.collect { alreadyHome ->
            val s = latestState
            val onboarding = s.loaded && !s.onboardingDone && s.favorites.isEmpty()
            if (!alreadyHome && pane == Pane.AllApps && System.currentTimeMillis() - vm.listLaunchAt < LIST_RETURN_MS) {
                overlay = null
                return@collect
            }
            overlay = if (!onboarding && alreadyHome && overlay == null && pane == Pane.Favorites && s.settings.swipeUpSearch) {
                Overlay.Search
            } else {
                null
            }
            pane = Pane.Favorites
        }
    }

    // --- setup: default launcher role -----------------------------------------------------------
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.refreshSetup()
    }
    val askToBeDefault: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = context.getSystemService(RoleManager::class.java)
            roleLauncher.launch(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
        } else {
            SystemActions.start(context, SystemActions.homeSettingsIntent())
        }
    }

    // --- calendar: Android's own prompt first; our explainer only once Android stops asking ------
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.refreshSetup()
        if (granted) {
            vm.setFlag("calendar_events", true)
        } else {
            val activity = context as? Activity
            if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CALENDAR)) {
                overlay = Overlay.Permission(
                    Explainers.calendar,
                    Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            }
        }
    }
    val toggleCalendar: () -> Unit = {
        when {
            state.settings.calendarEvents -> vm.setFlag("calendar_events", false)
            state.calendarAccess -> vm.setFlag("calendar_events", true)
            else -> calendarPermission.launch(Manifest.permission.READ_CALENDAR)
        }
    }
    val agendaOn = state.settings.calendarEvents && state.calendarAccess

    // --- widget add flow: allocate -> bind (maybe ask) -> configure (maybe) -> save ---------------
    var pendingWidgetId by remember { mutableStateOf<Int?>(null) }
    val finishWidget: (Int) -> Unit = { id ->
        if (vm.widgetHost.needsConfigure(id)) {
            onConfigureWidget(id) { ok -> if (ok) vm.saveHeaderWidget(id) else vm.releaseWidgetId(id) }
        } else {
            vm.saveHeaderWidget(id)
        }
    }
    val bindLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = pendingWidgetId ?: return@rememberLauncherForActivityResult
        pendingWidgetId = null
        if (result.resultCode == android.app.Activity.RESULT_OK) finishWidget(id) else vm.releaseWidgetId(id)
    }

    if (state.loaded && !state.onboardingDone && state.favorites.isEmpty()) {
        Onboarding(
            apps = state.allApps.filterNot { it.hidden },
            icons = vm.icons,
            onDone = { ids ->
                vm.completeOnboarding(ids)
                if (!state.setup.isDefaultLauncher) askToBeDefault()
            },
        )
        return
    }

    val notificationsFor = { app: AppEntity -> state.notifications[app.key].orEmpty() }
    val nowMs = System.currentTimeMillis()
    val appRow: @Composable (AppEntity, AppRowVariant) -> Unit = { app, variant ->
        val notes = notificationsFor(app)
        val latest = notes.firstOrNull()
        AppRow(
            app = app,
            icons = vm.icons,
            variant = variant,
            preview = latest?.preview,
            notificationCount = notes.size,
            onClick = { bounds -> if (variant == AppRowVariant.List) vm.launchFromList(app, bounds) else vm.launch(app, bounds) },
            onLongClick = { overlay = Overlay.Options(app) },
            onSwipeRight = { anchor -> overlay = Overlay.Popup(app, anchor) },
            previewAge = latest?.let { RelativeTime.short(nowMs - it.postedAt) },
        )
    }
    val sideloaded = remember { SystemActions.isSideloaded(context) }
    val listScope = rememberCoroutineScope()
    var widgetPage by remember { mutableStateOf(0) }
    val onDoubleTap: (() -> Unit)? = if (!state.settings.quickLock) null else {
        {
            if (QuickLockService.isEnabled(context)) {
                QuickLockService.lock()
            } else {
                overlay = Overlay.Permission(
                    Explainers.quickLock(sideloaded),
                    Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS),
                )
            }
        }
    }
    val thirdOfScreenPx = with(LocalDensity.current) { (LocalConfiguration.current.screenHeightDp.dp / 3).toPx() }

    Box(Modifier.fillMaxSize()) {
        // Scrim over the wallpaper. Home: darker behind the clock and the favorites, clear in between.
        // App list: even all the way down, since text fills the screen.
        val scrim = colors.scrim
        val max = ScrimTokens.homeMaxAlpha
        Box(
            Modifier.fillMaxSize().background(
                if (pane == Pane.AllApps) {
                    Brush.verticalGradient(0f to scrim.copy(alpha = max * 0.8f), 1f to scrim.copy(alpha = max))
                } else {
                    Brush.verticalGradient(
                        0f to scrim.copy(alpha = max * 0.8f),
                        0.32f to scrim.copy(alpha = max * 0.45f),
                        0.5f to scrim.copy(alpha = 0f),
                        1f to scrim.copy(alpha = max),
                    )
                },
            ),
        )

        val strip: @Composable () -> Unit = {
            WaveAlphabet(
                letters = state.list.letters,
                enabledLetters = state.list.enabledLetters,
                onLetter = { letter ->
                    pane = Pane.AllApps
                    state.list.headerIndex[letter]?.let { index ->
                        listState.requestScrollToItem(index)
                        // The reference lands the section header about a third of the way down.
                        listScope.launch {
                            withFrameNanos { }
                            listState.scrollToItem(index)
                            listState.scrollBy(-thirdOfScreenPx)
                        }
                    }
                },
                onFavorites = { pane = Pane.Favorites },
                onSettings = onOpenSettings,
                leftHanded = state.settings.leftHanded,
                haptics = state.settings.haptics,
                onDoubleTap = onDoubleTap,
            )
        }
        Row(Modifier.fillMaxSize().systemBarsPadding()) {
            if (state.settings.leftHanded) strip()
            Box(Modifier.weight(1f).fillMaxSize()) {
                when (pane) {
                    Pane.Favorites -> FavoritesPane(
                        state = state,
                        vm = vm,
                        appRow = appRow,
                        onFolderClick = { overlay = Overlay.Folder(it) },
                        onFolderLongClick = { overlay = Overlay.FolderOptions(it) },
                        onHeaderLongClick = { overlay = Overlay.HeaderOptions },
                        onDateClick = { if (agendaOn) overlay = Overlay.Agenda else SystemActions.openCalendar(context) },
                        onWidgetPage = { widgetPage = it },
                        onSuggestionRow = { app -> appRow(app, AppRowVariant.Favorite) },
                        onButtonClick = {
                            val target = state.settings.buttonTarget
                            when {
                                target == null -> overlay = Overlay.ButtonTarget
                                target.startsWith("app:") -> state.allApps.firstOrNull { it.id == target.removePrefix("app:") }
                                    ?.let { vm.launch(it, null) } ?: run { overlay = Overlay.ButtonTarget }
                                else -> state.favorites.filterIsInstance<HomeEntry.Folder>()
                                    .firstOrNull { it.folder.id == target.removePrefix("folder:") }
                                    ?.let { overlay = Overlay.Folder(it) } ?: run { overlay = Overlay.ButtonTarget }
                            }
                        },
                        onButtonLongClick = { overlay = Overlay.ButtonTarget },
                        onSearch = { if (state.settings.swipeUpSearch) overlay = Overlay.Search },
                        onAskDefault = askToBeDefault,
                        onAskNotifications = {
                            overlay = Overlay.Permission(
                                Explainers.notificationAccess(sideloaded),
                                SystemActions.notificationAccessIntent(context),
                            )
                        },
                    )
                    Pane.AllApps -> AppListPane(
                        model = state.list,
                        listState = listState,
                        appRow = appRow,
                        recentCount = state.recentlyInstalled.size,
                        leftHanded = state.settings.leftHanded,
                        onRecent = { overlay = Overlay.Recent },
                        onSearch = { overlay = Overlay.Search },
                        onOpenSettings = onOpenSettings,
                    )
                }
            }
            if (!state.settings.leftHanded) strip()
        }
    }

    // --- overlays --------------------------------------------------------------------------------
    val close = { overlay = null }
    when (val o = overlay) {
        null -> Unit
        is Overlay.Popup -> AppPopupCard(
            anchor = o.anchor,
            app = o.app,
            icons = vm.icons,
            notifications = notificationsFor(o.app),
            shortcuts = remember(o.app.id) { vm.shortcutsFor(o.app) },
            shortcutIcon = { item, px -> vm.shortcutIcon(item, px) },
            onOpenApp = { close(); vm.launch(o.app, o.anchor) },
            onOpenNotification = { item: NotifItem -> close(); vm.openNotification(item) },
            onDismissNotification = vm::dismissNotification,
            onReply = vm::reply,
            onRunAction = { action -> vm.runAction(action) },
            onShortcut = { sc -> close(); vm.launchShortcut(sc, o.anchor) },
            onShortcutLongPress = { sc -> overlay = Overlay.AddShortcut(o.app, sc) },
            onClose = close,
        )
        is Overlay.AddShortcut -> ShortcutFolderDialog(
            shortcutLabel = o.shortcut.label,
            folders = state.favorites.filterIsInstance<HomeEntry.Folder>().map { it.folder.id to it.folder.title },
            onPick = { folderId, title -> close(); vm.addShortcutToFolder(o.app, o.shortcut, folderId, title) },
            onDismiss = close,
        )
        is Overlay.Permission -> PermissionDialog(
            explainer = o.explainer,
            onConfirm = { close(); SystemActions.start(context, o.intent) },
            onDismiss = close,
        )
        is Overlay.Options -> {
            val app = state.allApps.firstOrNull { it.id == o.app.id } ?: o.app
            AppOptionsSheet(
                app = app,
                icons = vm.icons,
                isFavorite = app.id in state.favoriteAppIds,
                onToggleFavorite = { close(); vm.toggleFavorite(app) },
                onEditFavorites = { overlay = Overlay.EditFavorites },
                onOpenSettings = { close(); onOpenSettings() },
                onRename = { name -> close(); vm.rename(app, name) },
                onChangeIcon = { overlay = Overlay.IconPicker(app) },
                onToggleHidden = { close(); vm.setHidden(app, !app.hidden) },
                onAppInfo = { close(); vm.openAppInfo(app, null) },
                onUninstall = { close(); vm.uninstall(app) },
                onClose = close,
                isSuggestion = state.suggestions.any { it.id == app.id },
                onStopSuggesting = { close(); vm.stopSuggesting(app) },
            )
        }
        is Overlay.IconPicker -> {
            val pack = state.settings.iconPack
            IconPickerSheet(
                app = o.app,
                icons = vm.icons,
                pack = pack,
                packLabel = remember(pack) { pack?.let(vm::iconPackLabel) },
                onPick = { name -> close(); vm.setIcon(o.app, name) },
                onOpenIconSettings = { close(); onOpenSettings() },
                onClose = close,
            )
        }
        is Overlay.Folder -> {
            val entry = state.favorites.filterIsInstance<HomeEntry.Folder>().firstOrNull { it.folder.id == o.entry.folder.id } ?: o.entry
            FolderSheet(
                title = entry.folder.title,
                apps = entry.apps,
                shortcuts = entry.shortcuts,
                icons = vm.icons,
                shortcutIcon = { sc, px -> vm.storedShortcutIcon(sc, px) },
                onLaunch = { app -> close(); vm.launch(app, null) },
                onLaunchShortcut = { sc -> close(); vm.launchStoredShortcut(sc, null) },
                onRemoveShortcut = { sc -> vm.removeShortcutFromFolder(entry.folder.id, sc) },
                onEdit = { overlay = Overlay.FolderEditor(entry.folder.id, entry.folder.title, entry.apps.map { it.id }) },
                onClose = close,
            )
        }
        is Overlay.FolderOptions -> FolderOptionsSheet(
            title = o.entry.folder.title,
            onEdit = { overlay = Overlay.FolderEditor(o.entry.folder.id, o.entry.folder.title, o.entry.apps.map { it.id }) },
            onRemove = { close(); vm.removeFavorite(o.entry) },
            onClose = close,
        )
        is Overlay.FolderEditor -> FolderEditorSheet(
            initialTitle = o.title,
            initialAppIds = o.appIds,
            apps = state.allApps.filterNot { it.hidden },
            icons = vm.icons,
            onSave = { title, ids ->
                close()
                if (o.folderId == null) vm.createFolder(title, ids) else vm.updateFolder(o.folderId, title, ids)
            },
            onClose = close,
        )
        Overlay.EditFavorites -> EditFavoritesSheet(
            entries = state.favorites,
            icons = vm.icons,
            onReorder = vm::reorderFavorites,
            onRemove = vm::removeFavorite,
            onAddApps = { overlay = Overlay.AddFavorites },
            onNewFolder = { overlay = Overlay.FolderEditor(null, "", emptyList()) },
            onClose = close,
        )
        Overlay.AddFavorites -> AddFavoritesSheet(
            apps = state.allApps.filter { !it.hidden && it.id !in state.favoriteAppIds },
            icons = vm.icons,
            onAdd = { ids -> vm.addFavorites(ids); overlay = Overlay.EditFavorites },
            onClose = { overlay = Overlay.EditFavorites },
        )
        Overlay.HeaderOptions -> HeaderOptionsSheet(
            widgetCount = state.headerWidgets.size,
            maxWidgets = WidgetDao.MAX_STACK,
            onAddWidget = { overlay = Overlay.WidgetPicker },
            onRemoveWidget = {
                close()
                state.headerWidgets.getOrNull(widgetPage.coerceAtMost(state.headerWidgets.lastIndex))
                    ?.let { vm.removeHeaderWidget(it.appWidgetId) }
            },
            onEditFavorites = { overlay = Overlay.EditFavorites },
            onOpenSettings = { close(); onOpenSettings() },
            onClose = close,
            calendarOn = agendaOn,
            onToggleCalendar = toggleCalendar,
            onMoveClock = { overlay = Overlay.MoveClock },
        )
        Overlay.MoveClock -> MoveClockSheet(
            position = state.settings.headerPosition,
            onChange = vm::setHeaderPosition,
            onReset = { vm.setHeaderPosition(SizeTokens.headerTopSpaceRatio) },
            onClose = close,
        )
        Overlay.Agenda -> AgendaSheet(
            events = state.agenda,
            is24h = android.text.format.DateFormat.is24HourFormat(context),
            onEvent = { e -> close(); vm.openEvent(e) },
            onNewEvent = { close(); vm.newCalendarEvent() },
            onOpenCalendar = { close(); SystemActions.openCalendar(context) },
            onClose = close,
        )
        Overlay.WidgetPicker -> {
            val providers = remember { vm.widgetHost.providers() }
            WidgetPickerSheet(
                providers = providers,
                onPick = { info ->
                    close()
                    val id = vm.allocateWidgetId()
                    val bind = vm.bindWidgetIntent(id, info)
                    if (bind == null) {
                        finishWidget(id)
                    } else {
                        pendingWidgetId = id
                        bindLauncher.launch(bind)
                    }
                },
                onClose = close,
            )
        }
        Overlay.ButtonTarget -> ButtonTargetSheet(
            apps = state.allApps.filterNot { it.hidden },
            folders = state.favorites.filterIsInstance<HomeEntry.Folder>().map { it.folder.id to it.folder.title },
            icons = vm.icons,
            onPick = { target -> close(); vm.setButtonTarget(target) },
            onClose = close,
        )
        Overlay.Recent -> RecentlyInstalledSheet(
            apps = state.recentlyInstalled,
            icons = vm.icons,
            onLaunch = { app -> close(); vm.launch(app, null) },
            onClose = close,
        )
        Overlay.Search -> SearchOverlay(
            search = vm::search,
            suggestions = remember(state.allApps) {
                state.allApps.filter { !it.hidden && it.launchCount > 0 }.sortedByDescending { it.launchCount }.take(6)
            },
            icons = vm.icons,
            onLaunch = { app -> close(); vm.launch(app, null) },
            onLongPress = { app -> overlay = Overlay.Options(app) },
            onWebSearch = { q ->
                close()
                SystemActions.start(context, Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, q))
            },
            onPlaySearch = { q ->
                close()
                SystemActions.start(context, Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://search?q=" + android.net.Uri.encode(q) + "&c=apps")))
            },
            onClose = close,
        )
    }
}

@Composable
private fun FavoritesPane(
    state: HomeUiState,
    vm: HomeViewModel,
    appRow: @Composable (AppEntity, AppRowVariant) -> Unit,
    onFolderClick: (HomeEntry.Folder) -> Unit,
    onFolderLongClick: (HomeEntry.Folder) -> Unit,
    onHeaderLongClick: () -> Unit,
    onDateClick: () -> Unit,
    onWidgetPage: (Int) -> Unit,
    onSuggestionRow: @Composable (AppEntity) -> Unit,
    onButtonClick: () -> Unit,
    onButtonLongClick: () -> Unit,
    onSearch: () -> Unit,
    onAskDefault: () -> Unit,
    onAskNotifications: () -> Unit,
) {
    val context = LocalContext.current
    val colors = LocalHomeColors.current
    val swipeDownPx = with(LocalDensity.current) { 96.dp.toPx() }
    val swipeUpPx = with(LocalDensity.current) { 140.dp.toPx() }
    val search by rememberUpdatedState(onSearch)

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            // Swipe down anywhere: notification shade. Long swipe up: search. Observed on the Initial
            // pass and never consumed, so scrolling and taps underneath keep working.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var fired = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (fired) continue
                        val dy = change.position.y - down.position.y
                        val dx = change.position.x - down.position.x
                        if (dy > swipeDownPx && abs(dx) < dy / 2) {
                            fired = true
                            SystemActions.expandNotificationShade(context)
                        } else if (-dy > swipeUpPx && abs(dx) < -dy / 2) {
                            fired = true
                            search()
                        }
                    }
                }
            },
    ) {
        val topSpace = maxHeight * state.settings.headerPosition
        Column(Modifier.fillMaxSize().padding(start = if (state.settings.leftHanded) SpaceTokens.s3 else SizeTokens.edgeGutter, end = if (state.settings.leftHanded) SpaceTokens.s5 else 0.dp)) {
            Spacer(Modifier.height(topSpace))
            ClockHeader(
                onClockClick = { SystemActions.openClock(context) },
                onDateClick = onDateClick,
                onLongClick = onHeaderLongClick,
                upcoming = state.agenda,
                dateOpensAgenda = state.settings.calendarEvents && state.calendarAccess,
            )
            if (state.headerWidgets.isNotEmpty()) {
                Spacer(Modifier.height(SpaceTokens.s4))
                WidgetStack(state.headerWidgets, vm.widgetHost, Modifier.padding(end = SpaceTokens.s3), onPage = onWidgetPage)
            }
            val media = state.media
            if (state.settings.mediaCard && media != null) {
                Spacer(Modifier.height(SpaceTokens.s4))
                MediaCard(
                    media = media,
                    onOpen = vm::mediaOpen,
                    onPlayPause = vm::mediaPlayPause,
                    onNext = vm::mediaNext,
                    onPrevious = vm::mediaPrevious,
                    modifier = Modifier.padding(end = SpaceTokens.s3),
                )
            }
            if (state.loaded && !state.setup.isDefaultLauncher) {
                Spacer(Modifier.height(SpaceTokens.s5))
                SetupCard(
                    title = "Make Driftlist your home screen",
                    body = "Pressing Home will bring you here instead of your old launcher.",
                    action = "Set as home",
                    onAction = onAskDefault,
                    modifier = Modifier.padding(end = SpaceTokens.s3),
                )
            }
            if (state.loaded && !state.setup.hasNotificationAccess) {
                Spacer(Modifier.height(SpaceTokens.s4))
                SetupCard(
                    title = "See notifications next to your apps",
                    body = "Allow notification access to read and reply right here. Nothing leaves your phone.",
                    action = "Allow",
                    onAction = onAskNotifications,
                    modifier = Modifier.padding(end = SpaceTokens.s3),
                )
            }
            if (!state.settings.favoritesAtBottom) Spacer(Modifier.height(SpaceTokens.s4))
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = if (state.settings.favoritesAtBottom) Arrangement.Bottom else Arrangement.Top,
                contentPadding = PaddingValues(bottom = SpaceTokens.s7),
            ) {
                if (state.loaded && state.favorites.isEmpty()) {
                    item {
                        Text(
                            "Slide along the letters on the right to find an app, then long-press it to pin it here.",
                            style = TypeTokens.body.onWallpaper(colors, colors.textMuted),
                            modifier = Modifier.padding(end = SpaceTokens.s7, bottom = SpaceTokens.s5),
                        )
                    }
                }
                items(state.favorites, key = { it.itemId }) { entry ->
                    when (entry) {
                        is HomeEntry.App -> appRow(entry.app, AppRowVariant.Favorite)
                        is HomeEntry.Folder -> {
                            val latest = entry.apps.flatMap { state.notifications[it.key].orEmpty() }.maxByOrNull { it.postedAt }
                            FolderRow(
                                title = entry.folder.title,
                                apps = entry.apps,
                                icons = vm.icons,
                                preview = latest?.preview,
                                onClick = { onFolderClick(entry) },
                                onLongClick = { onFolderLongClick(entry) },
                            )
                        }
                    }
                }
                if (state.suggestions.isNotEmpty()) {
                    item(key = "suggestions:header") {
                        Text(
                            "Suggested",
                            style = TypeTokens.caption.onWallpaper(colors, colors.textMuted),
                            modifier = Modifier.padding(top = SpaceTokens.s5, bottom = SpaceTokens.s2).semantics { heading() },
                        )
                    }
                    items(state.suggestions, key = { "s:" + it.id }) { app -> onSuggestionRow(app) }
                }
            }
        }
        if (state.settings.buttonEnabled) {
            val target = state.settings.buttonTarget
            ActionButton(
                app = target?.takeIf { it.startsWith("app:") }?.let { t -> state.allApps.firstOrNull { it.id == t.removePrefix("app:") } },
                folderTitle = target?.takeIf { it.startsWith("folder:") }?.let { t ->
                    state.favorites.filterIsInstance<HomeEntry.Folder>().firstOrNull { it.folder.id == t.removePrefix("folder:") }?.folder?.title
                },
                icons = vm.icons,
                onClick = onButtonClick,
                onLongClick = onButtonLongClick,
                modifier = Modifier
                    .align(if (state.settings.leftHanded) Alignment.BottomStart else Alignment.BottomEnd)
                    .padding(SpaceTokens.s5),
            )
        }
    }
}

@Composable
private fun AppListPane(
    model: AppListModel,
    listState: LazyListState,
    appRow: @Composable (AppEntity, AppRowVariant) -> Unit,
    recentCount: Int,
    leftHanded: Boolean,
    onRecent: () -> Unit,
    onSearch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = LocalHomeColors.current
    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(start = if (leftHanded) SpaceTokens.s3 else SizeTokens.edgeGutter, end = if (leftHanded) SpaceTokens.s5 else 0.dp),
        contentPadding = PaddingValues(top = SpaceTokens.s7, bottom = SpaceTokens.s10),
    ) {
        for (section in model.sections) {
            item(key = "header:${section.letter}") {
                Text(
                    section.letter,
                    style = TypeTokens.letterHeader.onWallpaper(colors, colors.textMuted),
                    modifier = Modifier
                        .padding(top = SpaceTokens.s5, bottom = SpaceTokens.s2)
                        .semantics { heading() },
                )
            }
            items(section.apps, key = { it.id }) { app -> appRow(app, AppRowVariant.List) }
        }
        if (recentCount > 0) {
            item(key = "footer:recent") {
                Text(
                    "Recently installed ($recentCount)",
                    style = TypeTokens.list.onWallpaper(colors, colors.textMuted),
                    modifier = Modifier
                        .padding(top = SpaceTokens.s7)
                        .fillMaxWidth()
                        .heightIn(min = SizeTokens.listRow)
                        .clickable(onClick = onRecent)
                        .padding(vertical = SpaceTokens.s4),
                )
            }
        }
        item(key = "footer:settings") {
            Text(
                "Driftlist settings",
                style = TypeTokens.list.onWallpaper(colors, colors.textMuted),
                modifier = Modifier
                    .padding(top = SpaceTokens.s7)
                    .fillMaxWidth()
                    .heightIn(min = SizeTokens.listRow)
                    .clickable(onClick = onOpenSettings)
                    .padding(vertical = SpaceTokens.s4),
            )
        }
    }
    SearchButton(
        onClick = onSearch,
        modifier = Modifier
            .align(if (leftHanded) Alignment.BottomStart else Alignment.BottomEnd)
            .padding(SpaceTokens.s5),
    )
    }
}

private const val LIST_RETURN_MS = 3_000L
