package dev.farjad.driftlist.ui.home

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import android.app.role.RoleManager
import dev.farjad.driftlist.data.db.AppEntity
import dev.farjad.driftlist.system.SystemActions
import dev.farjad.driftlist.system.apps.IconCache
import dev.farjad.driftlist.system.notifications.NotifItem
import dev.farjad.driftlist.ui.components.AppOptionsSheet
import dev.farjad.driftlist.ui.components.AppPopupSheet
import dev.farjad.driftlist.ui.components.AppRow
import dev.farjad.driftlist.ui.components.AppRowVariant
import dev.farjad.driftlist.ui.components.ClockHeader
import dev.farjad.driftlist.ui.components.SetupCard
import dev.farjad.driftlist.ui.components.WaveAlphabet
import dev.farjad.driftlist.ui.theme.LocalHomeColors
import dev.farjad.driftlist.ui.theme.ScrimTokens
import dev.farjad.driftlist.ui.theme.SizeTokens
import dev.farjad.driftlist.ui.theme.SpaceTokens
import dev.farjad.driftlist.ui.theme.TypeTokens
import dev.farjad.driftlist.ui.theme.style
import kotlin.math.abs

private enum class Pane { Favorites, AllApps }

/** S05 favorites + S06 app list, sharing one wave alphabet. */
@Composable
fun HomeScreen(vm: HomeViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = LocalHomeColors.current

    var pane by rememberSaveable { mutableStateOf(Pane.Favorites) }
    var popupFor by remember { mutableStateOf<AppEntity?>(null) }
    var optionsFor by remember { mutableStateOf<AppEntity?>(null) }
    val listState = rememberLazyListState()

    BackHandler(enabled = pane == Pane.AllApps) { pane = Pane.Favorites }
    LaunchedEffect(Unit) {
        vm.homePresses.collect {
            popupFor = null
            optionsFor = null
            pane = Pane.Favorites
        }
    }

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

    val notificationsFor = { app: AppEntity -> state.notifications[app.key].orEmpty() }
    val rowFor: @Composable (AppEntity, AppRowVariant) -> Unit = { app, variant ->
        val notes = notificationsFor(app)
        AppRow(
            app = app,
            icons = vm.icons,
            variant = variant,
            preview = notes.firstOrNull()?.preview,
            notificationCount = notes.size,
            onClick = { bounds -> vm.launch(app, bounds) },
            onLongClick = { optionsFor = app },
            onSwipeRight = { popupFor = app },
        )
    }

    Box(Modifier.fillMaxSize()) {
        // Scrim over the wallpaper: light at the top behind the clock, stronger at the bottom behind the list.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to colors.scrim.copy(alpha = ScrimTokens.homeMaxAlpha * 0.64f),
                    0.25f to colors.scrim.copy(alpha = 0f),
                    0.4f to colors.scrim.copy(alpha = 0f),
                    1f to colors.scrim.copy(alpha = ScrimTokens.homeMaxAlpha),
                ),
            ),
        )

        Row(Modifier.fillMaxSize().systemBarsPadding()) {
            Box(Modifier.weight(1f).fillMaxSize()) {
                when (pane) {
                    Pane.Favorites -> FavoritesPane(
                        state = state,
                        row = rowFor,
                        onAskDefault = askToBeDefault,
                        onAskNotifications = {
                            SystemActions.start(context, SystemActions.notificationAccessIntent(context))
                        },
                    )
                    Pane.AllApps -> AppListPane(state.list, listState, rowFor)
                }
            }
            WaveAlphabet(
                letters = state.list.letters,
                enabledLetters = state.list.enabledLetters,
                onLetter = { letter ->
                    pane = Pane.AllApps
                    state.list.headerIndex[letter]?.let { listState.requestScrollToItem(it) }
                },
            )
        }
    }

    popupFor?.let { app ->
        AppPopupSheet(
            app = app,
            icons = vm.icons,
            notifications = notificationsFor(app),
            onOpenApp = { popupFor = null; vm.launch(app, null) },
            onOpenNotification = { item: NotifItem -> popupFor = null; vm.openNotification(item) },
            onDismissNotification = vm::dismissNotification,
            onClose = { popupFor = null },
        )
    }
    optionsFor?.let { app ->
        AppOptionsSheet(
            app = app,
            icons = vm.icons,
            isFavorite = app.id in state.favoriteIds,
            onToggleFavorite = { optionsFor = null; vm.toggleFavorite(app) },
            onAppInfo = { optionsFor = null; vm.openAppInfo(app, null) },
            onUninstall = { optionsFor = null; vm.uninstall(app) },
            onClose = { optionsFor = null },
        )
    }
}

@Composable
private fun FavoritesPane(
    state: HomeUiState,
    row: @Composable (AppEntity, AppRowVariant) -> Unit,
    onAskDefault: () -> Unit,
    onAskNotifications: () -> Unit,
) {
    val context = LocalContext.current
    val colors = LocalHomeColors.current
    val swipeDownPx = with(LocalDensity.current) { 96.dp.toPx() }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            // Swipe down anywhere opens the notification shade. Observed on the Initial pass and never
            // consumed, so scrolling and taps underneath keep working.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var fired = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val dy = change.position.y - down.position.y
                        val dx = change.position.x - down.position.x
                        if (!fired && dy > swipeDownPx && abs(dx) < dy / 2) {
                            fired = true
                            SystemActions.expandNotificationShade(context)
                        }
                    }
                }
            },
    ) {
        val topSpace = maxHeight * SizeTokens.headerTopSpaceRatio
        Column(Modifier.fillMaxSize().padding(start = SizeTokens.edgeGutter)) {
            Spacer(Modifier.height(topSpace - 48.dp))
            ClockHeader(
                onClockClick = { SystemActions.openClock(context) },
                onDateClick = { SystemActions.openCalendar(context) },
            )
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
                    body = "Allow notification access to read new messages right here. Nothing leaves your phone.",
                    action = "Allow",
                    onAction = onAskNotifications,
                    modifier = Modifier.padding(end = SpaceTokens.s3),
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Bottom,
                contentPadding = PaddingValues(bottom = SpaceTokens.s7),
            ) {
                if (state.loaded && state.favorites.isEmpty()) {
                    item {
                        Text(
                            "Slide along the letters on the right to find an app, then long-press it to pin it here.",
                            style = TypeTokens.body.style(colors.textMuted),
                            modifier = Modifier.padding(end = SpaceTokens.s7, bottom = SpaceTokens.s5),
                        )
                    }
                }
                items(state.favorites, key = { it.id }) { app -> row(app, AppRowVariant.Favorite) }
            }
        }
    }
}

@Composable
private fun AppListPane(
    model: AppListModel,
    listState: LazyListState,
    row: @Composable (AppEntity, AppRowVariant) -> Unit,
) {
    val colors = LocalHomeColors.current
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(start = SizeTokens.edgeGutter),
        contentPadding = PaddingValues(top = SpaceTokens.s7, bottom = SpaceTokens.s10),
    ) {
        for (section in model.sections) {
            item(key = "header:${section.letter}") {
                Text(
                    section.letter,
                    style = TypeTokens.letterHeader.style(colors.textMuted),
                    modifier = Modifier
                        .padding(top = SpaceTokens.s5, bottom = SpaceTokens.s2)
                        .semantics { heading() },
                )
            }
            items(section.apps, key = { it.id }) { app -> row(app, AppRowVariant.List) }
        }
    }
}
