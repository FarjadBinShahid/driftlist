package dev.farjad.driftlist.ui.home

import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import dev.farjad.driftlist.ui.components.AppIcon
import dev.farjad.driftlist.ui.components.FavoriteTile
import dev.farjad.driftlist.ui.components.FolderIcon
import dev.farjad.driftlist.ui.components.SearchPill
import dev.farjad.driftlist.ui.components.actionIconFor
import dev.farjad.driftlist.ui.components.PopupEditorSheet
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TextButton
import dev.farjad.driftlist.domain.ButtonTarget
import dev.farjad.driftlist.ui.components.MusicAppsRow
import dev.farjad.driftlist.ui.components.TorchRow
import dev.farjad.driftlist.domain.SearchEngine
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
    /** [stackId] null = the header; else a folder's or app pop-up's stack. */
    data class WidgetPicker(val stackId: String?) : Overlay
    data class PopupEditor(val app: AppEntity) : Overlay
    data object Search : Overlay
    /** [slot]: 0 tap, 1 swipe up, 2 swipe down, 3 double-tap. */
    data class ButtonTarget(val slot: Int) : Overlay
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
    // F7: leaving the list resets it, so the next time it opens it starts at the top (a letter
    // jump still lands on its letter, and the 3-second return from an app keeps your place).
    LaunchedEffect(pane) { if (pane == Pane.Favorites) listState.scrollToItem(0) }
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

    /** Runs a round-button target; false when it's unset or gone (then the picker opens). */
    fun runTarget(value: String?): Boolean = when (val t = dev.farjad.driftlist.domain.ButtonTarget.parse(value)) {
        null -> false
        is dev.farjad.driftlist.domain.ButtonTarget.App -> state.allApps.firstOrNull { it.id == t.appId }?.let { vm.launch(it, null); true } ?: false
        is dev.farjad.driftlist.domain.ButtonTarget.Folder -> state.favorites.filterIsInstance<HomeEntry.Folder>()
            .firstOrNull { it.folder.id == t.folderId }?.let { overlay = Overlay.Folder(it); true } ?: false
        is dev.farjad.driftlist.domain.ButtonTarget.Shortcut -> { vm.launchButtonShortcut(t, null); true }
        is dev.farjad.driftlist.domain.ButtonTarget.Action -> {
            when (t.kind) {
                dev.farjad.driftlist.domain.ButtonTarget.Kind.Search -> overlay = Overlay.Search
                dev.farjad.driftlist.domain.ButtonTarget.Kind.Shade -> SystemActions.expandNotificationShade(context)
                dev.farjad.driftlist.domain.ButtonTarget.Kind.Lock ->
                    if (QuickLockService.isEnabled(context)) {
                        QuickLockService.lock()
                    } else {
                        overlay = Overlay.Permission(
                            Explainers.quickLock(SystemActions.isSideloaded(context)),
                            Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS),
                        )
                    }
            }
            true
        }
    }
    val contactsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) vm.setFlag("search_contacts", false)
        // Reopen search so it picks up the new permission.
        if (overlay == Overlay.Search) { overlay = null; overlay = Overlay.Search }
    }

    // --- widget add flow: allocate -> bind (maybe ask) -> configure (maybe) -> save ---------------
    var pendingWidgetId by remember { mutableStateOf<Int?>(null) }
    var pendingWidgetStack by remember { mutableStateOf<String?>(null) }
    val finishWidget: (Int) -> Unit = { id ->
        val stack = pendingWidgetStack
        val save = { if (stack == null) vm.saveHeaderWidget(id) else vm.saveStackWidget(id, stack) }
        if (vm.widgetHost.needsConfigure(id)) {
            onConfigureWidget(id) { ok -> if (ok) save() else vm.releaseWidgetId(id) }
        } else {
            save()
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
    // Centre/right alignment lines the icons up: every favorite's label is as wide as the longest.
    val measurer = rememberTextMeasurer()
    val favStyle = TypeTokens.favorite.onWallpaper(colors)
    val density = LocalDensity.current
    val favoriteLabelWidth: Dp? = if (state.settings.favoritesAlign == "start") null else remember(state.favorites, state.suggestions, favStyle) {
        val labels = state.favorites.map { e -> if (e is HomeEntry.App) e.app.label else (e as HomeEntry.Folder).folder.title } + state.suggestions.map { it.label }
        val widest = labels.maxOfOrNull { measurer.measure(it, favStyle, maxLines = 1).size.width } ?: 0
        with(density) { widest.toDp() + SpaceTokens.s1 }.coerceAtMost(LABEL_WIDTH_MAX)
    }
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
            spokenLabel = if (app.userSerial in state.otherProfiles) remember(app.id) { vm.profileLabel(app) } else app.label,
            paused = app.userSerial in state.pausedProfiles,
            align = if (variant == AppRowVariant.Favorite) state.settings.favoritesAlign else "start",
            labelWidth = if (variant == AppRowVariant.Favorite) favoriteLabelWidth else null,
            dotsOnly = state.settings.notifDots,
        )
    }
    val appTile: @Composable (AppEntity) -> Unit = { app ->
        val notes = notificationsFor(app)
        FavoriteTile(
            label = app.label,
            showLabel = state.settings.gridLabels,
            unread = notes.size,
            onClick = { vm.launch(app, null) },
            onLongClick = { overlay = Overlay.Options(app) },
            onDetails = { overlay = Overlay.Popup(app, null) },
        ) { AppIcon(app.id, vm.icons, SizeTokens.iconFavorite) }
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
                        onButtonClick = { if (!runTarget(state.settings.buttonTarget)) overlay = Overlay.ButtonTarget(0) },
                        onButtonLongClick = { overlay = Overlay.ButtonTarget(0) },
                        onButtonSwipe = { if (!runTarget(state.settings.buttonSwipeTarget)) overlay = Overlay.ButtonTarget(1) },
                        onButtonDown = { if (!runTarget(state.settings.buttonDownTarget)) overlay = Overlay.ButtonTarget(2) },
                        onButtonDouble = { if (!runTarget(state.settings.buttonDoubleTarget)) overlay = Overlay.ButtonTarget(3) },
                        appTile = appTile,
                        favoriteLabelWidth = favoriteLabelWidth,
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
                        folders = if (state.settings.foldersInList) state.favorites.filterIsInstance<HomeEntry.Folder>() else emptyList(),
                        hasWorkProfile = state.otherProfiles.isNotEmpty(),
                        workPaused = state.pausedProfiles.isNotEmpty(),
                        onToggleWork = { vm.setWorkPaused(state.pausedProfiles.isEmpty()) },
                        icons = vm.icons,
                        onFolderClick = { overlay = Overlay.Folder(it) },
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
            shortcuts = remember(o.app.id, state.settings.shortcutsHidden) {
                if (o.app.id in state.settings.shortcutsHidden) emptyList() else vm.shortcutsFor(o.app)
            },
            popupApps = state.popupApps[o.app.id].orEmpty(),
            widgets = state.stackWidgets[appStack(o.app.id)].orEmpty(),
            widgetHost = vm.widgetHost,
            onLaunchApp = { app -> close(); vm.launch(app, null) },
            onEdit = { overlay = Overlay.PopupEditor(o.app) },
            shortcutIcon = { item, px -> vm.shortcutIcon(item, px) },
            onOpenApp = { close(); vm.launch(o.app, o.anchor) },
            onOpenNotification = { item: NotifItem -> close(); vm.openNotification(item) },
            onDismissNotification = vm::dismissNotification,
            onReply = vm::reply,
            onRunAction = { action -> vm.runAction(action) },
            onShortcut = { sc -> close(); vm.launchShortcut(sc, o.anchor) },
            quickReplies = state.settings.quickReplies,
            onShortcutLongPress = { sc -> overlay = Overlay.AddShortcut(o.app, sc) },
            onClose = close,
        )
        is Overlay.PopupEditor -> {
            val inside = state.popupApps[o.app.id].orEmpty().map { it.id }
            val widgets = state.stackWidgets[appStack(o.app.id)].orEmpty()
            PopupEditorSheet(
                app = o.app,
                apps = state.allApps.filterNot { it.hidden },
                selected = inside.toSet(),
                icons = vm.icons,
                widgetCount = widgets.size,
                shortcutsShown = o.app.id !in state.settings.shortcutsHidden,
                onToggleApp = { app -> vm.setPopupApps(o.app, if (app.id in inside) inside - app.id else inside + app.id) },
                onToggleShortcuts = { vm.setShortcutsHidden(o.app, o.app.id !in state.settings.shortcutsHidden) },
                onAddWidget = { overlay = Overlay.WidgetPicker(appStack(o.app.id)) },
                onRemoveWidget = { widgets.firstOrNull()?.let { vm.removeHeaderWidget(it.appWidgetId) } },
                onClose = close,
            )
        }
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
                onEditPopup = { overlay = Overlay.PopupEditor(app) },
                onScreenTime = remember(app.id) { vm.screenTimeIntent(app) }?.let { intent -> { close(); SystemActions.start(context, intent) } },
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
                widgets = state.stackWidgets[folderStack(entry.folder.id)].orEmpty(),
                widgetHost = vm.widgetHost,
                onAddWidget = { overlay = Overlay.WidgetPicker(folderStack(entry.folder.id)) },
                onRemoveWidget = {
                    state.stackWidgets[folderStack(entry.folder.id)]?.firstOrNull()?.let { vm.removeHeaderWidget(it.appWidgetId) }
                },
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
            onAddWidget = { overlay = Overlay.WidgetPicker(null) },
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
            clockReplaced = state.settings.replaceClock,
            onToggleReplaceClock = { vm.setFlag("replace_clock", !state.settings.replaceClock) },
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
        is Overlay.WidgetPicker -> {
            val providers = remember { vm.widgetHost.providers() }
            WidgetPickerSheet(
                providers = providers,
                onPick = { info ->
                    close()
                    pendingWidgetStack = o.stackId
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
        is Overlay.ButtonTarget -> ButtonTargetSheet(
            apps = state.allApps.filterNot { it.hidden },
            folders = state.favorites.filterIsInstance<HomeEntry.Folder>().map { it.folder.id to it.folder.title },
            icons = vm.icons,
            onPick = { target -> close(); vm.setButtonTarget(target, slot = o.slot) },
            onClose = close,
            shortcutsFor = vm::shortcutsFor,
            slot = o.slot,
            onSwitchSlot = { slot -> overlay = Overlay.ButtonTarget(slot) },
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
            onWebSearch = { q -> close(); vm.webSearch(q) },
            engineLabel = state.settings.searchEngine.takeIf { it != SearchEngine.System }?.label,
            webSuggestions = vm::webSuggestions,
            contacts = vm::searchContacts,
            contactsNeedPermission = remember(state.settings.searchContacts) { vm.contactsNeedPermission() },
            onAllowContacts = { contactsPermission.launch(Manifest.permission.READ_CONTACTS) },
            onOpenContact = { hit -> close(); vm.openContact(hit) },
            onCallContact = { hit -> close(); vm.callContact(hit) },
            onMessageContact = { hit -> close(); vm.messageContact(hit) },
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
    onButtonSwipe: () -> Unit,
    onButtonDown: () -> Unit,
    onButtonDouble: () -> Unit,
    appTile: @Composable (AppEntity) -> Unit,
    favoriteLabelWidth: Dp?,
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
        // Landscape and wide screens (foldables, tablets): header on the left, favorites on the right.
        val wide = maxWidth > maxHeight || maxWidth >= WIDE_LAYOUT_MIN
        val topSpace = maxHeight * state.settings.headerPosition
        val wideTopSpace = topSpace.coerceAtMost(maxHeight * WIDE_TOP_MAX)
        val startPad = if (state.settings.leftHanded) SpaceTokens.s3 else SizeTokens.edgeGutter
        val endPad = if (state.settings.leftHanded) SpaceTokens.s5 else 0.dp
        val showClock = !(state.settings.replaceClock && state.headerWidgets.isNotEmpty())

        // Music, flashlight and headphone cards. Above favorites by default; below them when the
        // user pins favorites in place (F4), so nothing pushes favorites around.
        val cards: @Composable () -> Unit = {
            if (state.torchOn) {
                Spacer(Modifier.height(SpaceTokens.s4))
                TorchRow(onTurnOff = vm::torchOff, modifier = Modifier.padding(end = SpaceTokens.s3))
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
            if (state.musicApps.isNotEmpty() && media?.playing != true) {
                Spacer(Modifier.height(SpaceTokens.s4))
                MusicAppsRow(state.musicApps, vm.icons, onLaunch = { vm.launch(it, null) })
            }
        }
        val pinnedCards = state.settings.pinFavorites &&
            (state.torchOn || (state.settings.mediaCard && state.media != null) || state.musicApps.isNotEmpty())

        val header: @Composable ColumnScope.() -> Unit = {
            if (showClock) {
                ClockHeader(
                    onClockClick = { SystemActions.openClock(context) },
                    onDateClick = onDateClick,
                    onLongClick = onHeaderLongClick,
                    upcoming = state.agenda,
                    dateOpensAgenda = state.settings.calendarEvents && state.calendarAccess,
                )
            }
            if (state.headerWidgets.isNotEmpty()) {
                if (showClock) Spacer(Modifier.height(SpaceTokens.s4))
                WidgetStack(state.headerWidgets, vm.widgetHost, Modifier.padding(end = SpaceTokens.s3), onPage = onWidgetPage)
                if (!showClock) {
                    // The clock's long-press is gone; keep a way into the header options.
                    TextButton(onClick = onHeaderLongClick) { Text("Header options") }
                }
            }
            if (!state.settings.pinFavorites) cards()
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
        }

        val favorites: @Composable (Modifier, Boolean) -> Unit = { modifier, bottom ->
            if (state.settings.favoritesGrid > 0) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(state.settings.favoritesGrid),
                    modifier = modifier.fillMaxWidth().padding(end = SpaceTokens.s3),
                    verticalArrangement = if (bottom) Arrangement.Bottom else Arrangement.Top,
                    contentPadding = PaddingValues(bottom = SpaceTokens.s7),
                ) {
                    items(state.favorites, key = { it.itemId }) { entry ->
                        when (entry) {
                            is HomeEntry.App -> appTile(entry.app)
                            is HomeEntry.Folder -> FavoriteTile(
                                label = entry.folder.title,
                                showLabel = state.settings.gridLabels,
                                unread = entry.apps.sumOf { state.notifications[it.key].orEmpty().size },
                                onClick = { onFolderClick(entry) },
                                onLongClick = { onFolderLongClick(entry) },
                            ) { FolderIcon(entry.apps, vm.icons, SizeTokens.iconFavorite) }
                        }
                    }
                    if (state.suggestions.isNotEmpty()) {
                        item(key = "suggestions:header", span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "Suggested",
                                style = TypeTokens.caption.onWallpaper(colors, colors.textMuted),
                                modifier = Modifier.padding(top = SpaceTokens.s5, bottom = SpaceTokens.s2).semantics { heading() },
                            )
                        }
                        items(state.suggestions, key = { "s:" + it.id }) { app -> appTile(app) }
                    }
                    if (pinnedCards) item(key = "cards", span = { GridItemSpan(maxLineSpan) }) { Column { cards() } }
                }
            } else {
            LazyColumn(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = if (bottom) Arrangement.Bottom else Arrangement.Top,
                contentPadding = PaddingValues(bottom = SpaceTokens.s7),
            ) {
                if (state.loaded && state.favorites.isEmpty()) {
                    item {
                        Text(
                            "Slide along the letters to find an app, then long-press it to pin it here.",
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
                                align = state.settings.favoritesAlign,
                                labelWidth = favoriteLabelWidth,
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
                if (pinnedCards) item(key = "cards") { Column { cards() } }
            }
            }
        }

        if (wide) {
            Row(Modifier.fillMaxSize().padding(start = startPad, end = endPad)) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(end = SpaceTokens.s7),
                ) {
                    Spacer(Modifier.height(wideTopSpace))
                    header()
                }
                favorites(Modifier.weight(1f).fillMaxHeight().padding(top = SpaceTokens.s7), true)
            }
        } else {
            Column(Modifier.fillMaxSize().padding(start = startPad, end = endPad)) {
                Spacer(Modifier.height(topSpace))
                header()
                if (!state.settings.favoritesAtBottom) Spacer(Modifier.height(SpaceTokens.s4))
                favorites(Modifier.weight(1f), state.settings.favoritesAtBottom)
            }
        }

        if (state.settings.searchPill) {
            SearchPill(onClick = onSearch, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = SpaceTokens.s5))
        }
        if (state.settings.buttonEnabled) {
            val tap = ButtonTarget.parse(state.settings.buttonTarget)
            val swipeTarget = ButtonTarget.parse(state.settings.buttonSwipeTarget)
            ActionButton(
                app = when (tap) {
                    is ButtonTarget.App -> state.allApps.firstOrNull { it.id == tap.appId }
                    is ButtonTarget.Shortcut -> state.allApps.firstOrNull { it.packageName == tap.packageName && it.userSerial == tap.userSerial }
                    else -> null
                },
                folderTitle = (tap as? ButtonTarget.Folder)?.let { t ->
                    state.favorites.filterIsInstance<HomeEntry.Folder>().firstOrNull { it.folder.id == t.folderId }?.folder?.title
                },
                shortcutLabel = (tap as? ButtonTarget.Shortcut)?.label,
                icons = vm.icons,
                onClick = onButtonClick,
                onLongClick = onButtonLongClick,
                swipeLabel = targetLabel(swipeTarget, state),
                onSwipeUp = if (swipeTarget != null) onButtonSwipe else null,
                actionIcon = (tap as? ButtonTarget.Action)?.let { actionIconFor(it.kind) },
                actionLabel = (tap as? ButtonTarget.Action)?.kind?.label,
                downLabel = targetLabel(ButtonTarget.parse(state.settings.buttonDownTarget), state),
                onSwipeDown = if (state.settings.buttonDownTarget != null) onButtonDown else null,
                doubleLabel = targetLabel(ButtonTarget.parse(state.settings.buttonDoubleTarget), state),
                onDoubleTap = if (state.settings.buttonDoubleTarget != null) onButtonDouble else null,
                modifier = Modifier
                    .align(if (state.settings.leftHanded) Alignment.BottomStart else Alignment.BottomEnd)
                    .padding(SpaceTokens.s5),
            )
        }
    }
}

/** Spoken name of a round-button target. */
private fun targetLabel(target: ButtonTarget?, state: HomeUiState): String? = when (target) {
    null -> null
    is ButtonTarget.App -> state.allApps.firstOrNull { it.id == target.appId }?.label
    is ButtonTarget.Folder -> state.favorites.filterIsInstance<HomeEntry.Folder>().firstOrNull { it.folder.id == target.folderId }?.folder?.title
    is ButtonTarget.Shortcut -> target.label
    is ButtonTarget.Action -> target.kind.label
}

private val WIDE_LAYOUT_MIN = 600.dp
private val LABEL_WIDTH_MAX = 220.dp
private const val WIDE_TOP_MAX = 0.12f

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
    folders: List<HomeEntry.Folder> = emptyList(),
    icons: dev.farjad.driftlist.system.apps.IconCache? = null,
    onFolderClick: (HomeEntry.Folder) -> Unit = {},
    hasWorkProfile: Boolean = false,
    workPaused: Boolean = false,
    onToggleWork: () -> Unit = {},
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
        if (folders.isNotEmpty() && icons != null) {
            item(key = "header:folders") {
                Text(
                    "Folders",
                    style = TypeTokens.letterHeader.onWallpaper(colors, colors.textMuted),
                    modifier = Modifier.padding(top = SpaceTokens.s5, bottom = SpaceTokens.s2).semantics { heading() },
                )
            }
            items(folders, key = { "folder:" + it.folder.id }) { entry ->
                FolderRow(title = entry.folder.title, apps = entry.apps, icons = icons, preview = null, onClick = { onFolderClick(entry) }, onLongClick = { onFolderClick(entry) })
            }
        }
        if (hasWorkProfile) {
            item(key = "footer:work") {
                Text(
                    if (workPaused) "Turn on work apps" else "Pause work apps",
                    style = TypeTokens.list.onWallpaper(colors, colors.textMuted),
                    modifier = Modifier
                        .padding(top = SpaceTokens.s7)
                        .fillMaxWidth()
                        .heightIn(min = SizeTokens.listRow)
                        .clickable(onClick = onToggleWork)
                        .padding(vertical = SpaceTokens.s4),
                )
            }
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
