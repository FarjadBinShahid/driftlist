package dev.farjad.letterline.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import dev.farjad.letterline.ui.theme.onWallpaper
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.farjad.letterline.domain.ButtonTarget
import dev.farjad.letterline.system.apps.ShortcutInfoItem
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
import androidx.compose.ui.text.font.FontWeight
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
import dev.farjad.letterline.data.db.AppEntity
import dev.farjad.letterline.data.db.WidgetEntity
import dev.farjad.letterline.system.apps.IconCache
import dev.farjad.letterline.system.media.MediaState
import dev.farjad.letterline.system.widgets.WidgetHostManager
import dev.farjad.letterline.ui.theme.ElevationTokens
import dev.farjad.letterline.ui.theme.LocalHomeColors
import dev.farjad.letterline.ui.theme.RadiusTokens
import dev.farjad.letterline.ui.theme.SizeTokens
import dev.farjad.letterline.ui.theme.SpaceTokens
import dev.farjad.letterline.ui.theme.TypeTokens
import dev.farjad.letterline.ui.theme.style

/**
 * Music under the header, drawn straight on the wallpaper like the app rows: the cover on the
 * left, title and artist in the home font beside it, and the controls underneath, play in a ring.
 */
@Composable
fun MediaCard(
    media: MediaState,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHomeColors.current
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open the player", onClick = onOpen)
            .semantics { contentDescription = "Now playing ${media.title}" + (media.artist?.let { " by $it" } ?: "") },
        horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s5),
    ) {
        Box(
            Modifier.size(SizeTokens.mediaArt).clip(RoundedCornerShape(RadiusTokens.sm)).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (media.art != null) {
                Image(media.art.asImageBitmap(), null, Modifier.size(SizeTokens.mediaArt), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Outlined.MusicNote, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Column(Modifier.weight(1f).height(SizeTokens.mediaArt)) {
            Text(media.title, style = TypeTokens.list.onWallpaper(colors).copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            media.artist?.let {
                Text(it, style = TypeTokens.bodySm.onWallpaper(colors, colors.textMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                if (media.canSkip) {
                    IconButton(onClick = onPrevious) { Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = colors.text) }
                }
                IconButton(onClick = onPlayPause) {
                    Box(
                        Modifier.size(SizeTokens.mediaPlayRing).border(2.dp, colors.text, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (media.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (media.playing) "Pause" else "Play",
                            tint = colors.text,
                        )
                    }
                }
                if (media.canSkip) {
                    IconButton(onClick = onNext) { Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = colors.text) }
                }
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
    actionIcon: ImageVector? = null,
    actionLabel: String? = null,
    downLabel: String? = null,
    onSwipeDown: (() -> Unit)? = null,
    doubleLabel: String? = null,
    onDoubleTap: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val label = when {
        actionLabel != null -> actionLabel
        shortcutLabel != null -> "Open $shortcutLabel"
        app != null -> "Open ${app.label}"
        folderTitle != null -> "Open $folderTitle"
        else -> "Choose what this button opens"
    }
    val swipe by rememberUpdatedState(onSwipeUp)
    val down by rememberUpdatedState(onSwipeDown)
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
                    onDragEnd = {
                        if (dy < -swipePx) swipe?.invoke()
                        if (dy > swipePx) down?.invoke()
                    },
                ) { change, amount ->
                    dy += amount
                    change.consume()
                }
            }
            .semantics {
                customActions = listOfNotNull(
                    if (onSwipeUp != null && swipeLabel != null) CustomAccessibilityAction("Swipe up: $swipeLabel") { onSwipeUp(); true } else null,
                    if (onSwipeDown != null && downLabel != null) CustomAccessibilityAction("Swipe down: $downLabel") { onSwipeDown(); true } else null,
                    if (onDoubleTap != null && doubleLabel != null) CustomAccessibilityAction("Double-tap: $doubleLabel") { onDoubleTap(); true } else null,
                )
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onLongClick() },
                onLongClickLabel = "Change what it opens",
                onDoubleClick = onDoubleTap,
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        when {
            actionIcon != null -> Icon(actionIcon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
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
    slot: Int = 0,
    onSwitchSlot: ((Int) -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf<String?>(null) }
    AppSheet(onClose) {
        Column(Modifier.fillMaxHeight(0.9f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SheetTitle(BUTTON_SLOTS[slot] + " opens", Modifier.weight(1f))
                TextButton(onClick = { onPick(null) }, modifier = Modifier.padding(end = SpaceTokens.s4)) { Text(if (slot == 0) "Turn off" else "Nothing") }
            }
            if (onSwitchSlot != null) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = SpaceTokens.s7),
                    horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s3),
                ) {
                    BUTTON_SLOTS.forEachIndexed { i, name ->
                        FilterChip(selected = slot == i, onClick = { onSwitchSlot(i) }, label = { Text(name) })
                    }
                }
            }
            LazyColumn(Modifier.weight(1f)) {
                items(ButtonTarget.Kind.entries, key = { "k:" + it.name }) { kind ->
                    OptionRow(kind.label, { onPick(ButtonTarget.Action(kind).encode()) }, icon = actionIconFor(kind), supporting = "Built in")
                }
                item { HorizontalDivider(Modifier.padding(vertical = SpaceTokens.s2)) }
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
    AppSheet(onClose) {
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

/** The round button's four gestures, in the order the sheet shows them. */
val BUTTON_SLOTS = listOf("Tap", "Swipe up", "Swipe down", "Double-tap")

fun actionIconFor(kind: ButtonTarget.Kind): ImageVector = when (kind) {
    ButtonTarget.Kind.Search -> Icons.Outlined.Search
    ButtonTarget.Kind.Lock -> Icons.Outlined.Lock
    ButtonTarget.Kind.Shade -> Icons.Outlined.Notifications
}

/** F6: a search pill on home that opens straight into typing. */
@Composable
fun SearchPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(RadiusTokens.pill),
        modifier = modifier
            .heightIn(min = SizeTokens.touchMin)
            .clip(RoundedCornerShape(RadiusTokens.pill))
            .clickable(onClickLabel = "Search apps", onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = SpaceTokens.s6, vertical = SpaceTokens.s3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s3),
        ) {
            Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Search", style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurfaceVariant))
        }
    }
}

/** F3: one favorite in the icon grid. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoriteTile(
    label: String,
    showLabel: Boolean,
    unread: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDetails: (() -> Unit)? = null,
    icon: @Composable () -> Unit,
) {
    val colors = LocalHomeColors.current
    val haptics = LocalHapticFeedback.current
    Column(
        modifier
            .clip(RoundedCornerShape(RadiusTokens.md))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onLongClick() },
                onLongClickLabel = "Options",
            )
            .semantics(mergeDescendants = true) {
                contentDescription = if (unread > 0) "$label, $unread notifications" else label
                if (onDetails != null) customActions = listOf(CustomAccessibilityAction("Notifications and shortcuts") { onDetails(); true })
            }
            .padding(vertical = SpaceTokens.s3),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            icon()
            if (unread > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(SizeTokens.eventDot + SpaceTokens.s1)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
        if (showLabel) {
            Text(
                label,
                style = TypeTokens.caption.onWallpaper(colors),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = SpaceTokens.s2, start = SpaceTokens.s1, end = SpaceTokens.s1),
            )
        }
    }
}
