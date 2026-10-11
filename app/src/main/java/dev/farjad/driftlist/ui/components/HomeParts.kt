package dev.farjad.driftlist.ui.components

import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.ui.semantics.heading
import dev.farjad.driftlist.ui.theme.onWallpaper
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.farjad.driftlist.domain.ButtonTarget
import dev.farjad.driftlist.system.apps.ShortcutInfoItem
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.farjad.driftlist.data.db.AppEntity
import dev.farjad.driftlist.data.db.WidgetEntity
import dev.farjad.driftlist.system.apps.IconCache
import dev.farjad.driftlist.system.media.MediaState
import dev.farjad.driftlist.system.widgets.WidgetHostManager
import dev.farjad.driftlist.ui.theme.ElevationTokens
import dev.farjad.driftlist.ui.theme.LocalHomeColors
import dev.farjad.driftlist.ui.theme.RadiusTokens
import dev.farjad.driftlist.ui.theme.SizeTokens
import dev.farjad.driftlist.ui.theme.SpaceTokens
import dev.farjad.driftlist.ui.theme.TypeTokens
import dev.farjad.driftlist.ui.theme.style

/** Music card under the header while something plays (or is paused). */
@Composable
fun MediaCard(
    media: MediaState,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shape = RoundedCornerShape(RadiusTokens.lg),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SizeTokens.mediaCardHeight)
            .clickable(onClickLabel = "Open the player", onClick = onOpen)
            .semantics { contentDescription = "Now playing ${media.title}" + (media.artist?.let { " by $it" } ?: "") },
    ) {
        Row(Modifier.padding(SpaceTokens.s4), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s4)) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(RadiusTokens.md)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (media.art != null) {
                    Image(media.art.asImageBitmap(), null, Modifier.size(56.dp))
                } else {
                    Icon(Icons.Outlined.MusicNote, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(media.title, style = TypeTokens.label.style(MaterialTheme.colorScheme.onSurface), maxLines = 1, overflow = TextOverflow.Ellipsis)
                media.artist?.let {
                    Text(it, style = TypeTokens.bodySm.style(MaterialTheme.colorScheme.onSurfaceVariant), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (media.canSkip) {
                IconButton(onClick = onPrevious) { Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous") }
            }
            IconButton(onClick = onPlayPause) {
                Icon(if (media.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = if (media.playing) "Pause" else "Play")
            }
            if (media.canSkip) {
                IconButton(onClick = onNext) { Icon(Icons.Filled.SkipNext, contentDescription = "Next") }
            }
        }
    }
}

/** Header widgets: one, or a stack of up to four you swipe through. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WidgetStack(
    widgets: List<WidgetEntity>,
    host: WidgetHostManager,
    modifier: Modifier = Modifier,
    onPage: (Int) -> Unit = {},
) {
    if (widgets.size == 1) {
        LaunchedEffect(Unit) { onPage(0) }
        HostedWidget(widgets.first().appWidgetId, host, modifier)
        return
    }
    val pager = rememberPagerState { widgets.size }
    LaunchedEffect(pager.currentPage) { onPage(pager.currentPage) }
    val scope = rememberCoroutineScope()
    Column(modifier) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Next widget") {
                        scope.launch { pager.animateScrollToPage((pager.currentPage + 1) % widgets.size) }
                        true
                    },
                    CustomAccessibilityAction("Previous widget") {
                        scope.launch { pager.animateScrollToPage((pager.currentPage - 1 + widgets.size) % widgets.size) }
                        true
                    },
                )
            },
        ) { page -> HostedWidget(widgets[page].appWidgetId, host) }
        Row(Modifier.align(Alignment.CenterHorizontally).padding(top = SpaceTokens.s2), horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s2)) {
            repeat(widgets.size) { i ->
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(LocalHomeColors.current.text.copy(alpha = if (i == pager.currentPage) 0.9f else 0.35f)),
                )
            }
        }
    }
}

/** The round button at the bottom right of home (opens an app or a folder). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActionButton(
    app: AppEntity?,
    folderTitle: String?,
    icons: IconCache,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    shortcutLabel: String? = null,
    swipeLabel: String? = null,
    onSwipeUp: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val label = when {
        shortcutLabel != null -> "Open $shortcutLabel"
        app != null -> "Open ${app.label}"
        folderTitle != null -> "Open $folderTitle"
        else -> "Choose what this button opens"
    }
    val swipe by rememberUpdatedState(onSwipeUp)
    val swipePx = with(LocalDensity.current) { SizeTokens.actionButton.toPx() * 0.6f }
    Box(
        modifier = modifier
            .size(SizeTokens.actionButton)
            .shadow(ElevationTokens.pop, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .pointerInput(Unit) {
                var dy = 0f
                detectVerticalDragGestures(
                    onDragStart = { dy = 0f },
                    onDragEnd = { if (dy < -swipePx) swipe?.invoke() },
                ) { change, amount ->
                    dy += amount
                    change.consume()
                }
            }
            .semantics {
                if (onSwipeUp != null && swipeLabel != null) {
                    customActions = listOf(CustomAccessibilityAction("Swipe up: $swipeLabel") { onSwipeUp(); true })
                }
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onLongClick() },
                onLongClickLabel = "Change what it opens",
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        when {
            app != null -> AppIcon(app.id, icons, SizeTokens.iconList)
            folderTitle != null -> Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            else -> Icon(Icons.Outlined.Add, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

/** Round search button shown while the A-Z list is open. */
@Composable
fun SearchButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(SizeTokens.actionButton)
            .shadow(ElevationTokens.pop, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .clickable(onClickLabel = "Search apps", onClick = onClick)
            .semantics { contentDescription = "Search apps" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurface)
    }
}

/** Pick what the action button opens: a folder or an app. */
@Composable
fun ButtonTargetSheet(
    apps: List<AppEntity>,
    folders: List<Pair<String, String>>,
    icons: IconCache,
    onPick: (String?) -> Unit,
    onClose: () -> Unit,
    shortcutsFor: (AppEntity) -> List<ShortcutInfoItem> = { emptyList() },
    swipe: Boolean = false,
    onSwitchMode: ((Boolean) -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf<String?>(null) }
    DriftSheet(onClose) {
        Column(Modifier.fillMaxHeight(0.9f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SheetTitle(if (swipe) "Swipe up opens" else "Tap opens", Modifier.weight(1f))
                TextButton(onClick = { onPick(null) }, modifier = Modifier.padding(end = SpaceTokens.s4)) { Text(if (swipe) "Nothing" else "Turn off") }
            }
            if (onSwitchMode != null) {
                Row(Modifier.padding(horizontal = SpaceTokens.s7), horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s3)) {
                    FilterChip(selected = !swipe, onClick = { onSwitchMode(false) }, label = { Text("Tap") })
                    FilterChip(selected = swipe, onClick = { onSwitchMode(true) }, label = { Text("Swipe up") })
                }
            }
            LazyColumn(Modifier.weight(1f)) {
                if (folders.isNotEmpty()) {
                    items(folders, key = { "f:" + it.first }) { (id, title) ->
                        OptionRow(title, { onPick("folder:$id") }, icon = Icons.Outlined.Folder, supporting = "Folder")
                    }
                    item { HorizontalDivider(Modifier.padding(vertical = SpaceTokens.s2)) }
                }
                items(apps.sortedBy { it.label.lowercase() }, key = { it.id }) { app ->
                    Column {
                        SheetAppRow(app, icons, onClick = { onPick("app:${app.id}") }) {
                            TextButton(onClick = { expanded = if (expanded == app.id) null else app.id }) {
                                Text(if (expanded == app.id) "Hide shortcuts" else "Shortcuts")
                            }
                        }
                        if (expanded == app.id) {
                            val shortcuts = remember(app.id) { shortcutsFor(app) }
                            if (shortcuts.isEmpty()) {
                                Text(
                                    "No shortcuts for this app.",
                                    style = TypeTokens.caption.style(MaterialTheme.colorScheme.onSurfaceVariant),
                                    modifier = Modifier.padding(start = SpaceTokens.s11, bottom = SpaceTokens.s3),
                                )
                            }
                            shortcuts.forEach { sc ->
                                OptionRow(sc.label, {
                                    onPick(ButtonTarget.Shortcut(app.userSerial, app.packageName, sc.id, sc.label).encode())
                                }, supporting = app.label)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** S28: apps installed in the last two weeks, newest first. */
@Composable
fun RecentlyInstalledSheet(apps: List<AppEntity>, icons: IconCache, onLaunch: (AppEntity) -> Unit, onClose: () -> Unit) {
    DriftSheet(onClose) {
        SheetTitle("Recently installed")
        if (apps.isEmpty()) {
            Text(
                "Nothing new in the last two weeks.",
                style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s4),
            )
        }
        LazyColumn(Modifier.heightIn(max = 480.dp)) {
            items(apps, key = { it.id }) { app -> SheetAppRow(app, icons, onClick = { onLaunch(app) }) }
        }
        Box(Modifier.height(SpaceTokens.s2))
    }
}

/** A plain round icon stand-in, for previews that show no real apps. */
@Composable
fun AppIconPlaceholder(color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Box(modifier.size(SizeTokens.iconPopup).clip(CircleShape).background(color))
}

/** Shown while the flashlight is on, so it isn't left burning in a pocket. */
@Composable
fun TorchRow(onTurnOff: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shape = RoundedCornerShape(RadiusTokens.lg),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = SpaceTokens.s5, end = SpaceTokens.s3).heightIn(min = SizeTokens.touchMin),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s4),
        ) {
            Icon(Icons.Outlined.FlashlightOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Flashlight is on", style = TypeTokens.label.style(MaterialTheme.colorScheme.onSurface), modifier = Modifier.weight(1f))
            TextButton(onClick = onTurnOff) { Text("Turn off") }
        }
    }
}

/** Plug and Play: music apps, offered while headphones are connected. */
@Composable
fun MusicAppsRow(apps: List<AppEntity>, icons: IconCache, onLaunch: (AppEntity) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalHomeColors.current
    Column(modifier) {
        Text(
            "Headphones connected",
            style = TypeTokens.caption.onWallpaper(colors, colors.textMuted),
            modifier = Modifier.semantics { heading() },
        )
        Row(Modifier.padding(top = SpaceTokens.s2), horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s4)) {
            apps.forEach { app ->
                Box(
                    Modifier
                        .size(SizeTokens.touchMin + SpaceTokens.s3)
                        .clip(CircleShape)
                        .clickable(onClickLabel = "Open ${app.label}") { onLaunch(app) }
                        .semantics { contentDescription = app.label },
                    contentAlignment = Alignment.Center,
                ) { AppIcon(app.id, icons, SizeTokens.iconFavorite) }
            }
        }
    }
}
