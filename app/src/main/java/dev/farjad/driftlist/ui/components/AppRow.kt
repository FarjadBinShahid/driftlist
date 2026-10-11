package dev.farjad.driftlist.ui.components

import android.graphics.Rect
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.farjad.driftlist.data.db.AppEntity
import dev.farjad.driftlist.system.apps.IconCache
import dev.farjad.driftlist.ui.theme.LocalHomeColors
import dev.farjad.driftlist.ui.theme.RadiusTokens
import dev.farjad.driftlist.ui.theme.SizeTokens
import dev.farjad.driftlist.ui.theme.SpaceTokens
import dev.farjad.driftlist.ui.theme.TypeTokens
import dev.farjad.driftlist.ui.theme.onWallpaper
import kotlin.math.roundToInt

enum class AppRowVariant { Favorite, List }

@Composable
fun AppIcon(appId: String, icons: IconCache, size: Dp, modifier: Modifier = Modifier) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val version by icons.version.collectAsState()
    val bitmap: ImageBitmap? by produceState(icons.peek(appId, px), appId, px, version) {
        value = icons.load(appId, px)
    }
    val current = bitmap
    if (current != null) {
        Image(current, contentDescription = null, modifier = modifier.size(size))
    } else {
        Box(modifier.size(size))
    }
}

/** Four tiny icons in a rounded square: the "icon" of a folder. */
@Composable
fun FolderIcon(apps: List<AppEntity>, icons: IconCache, size: Dp) {
    val cell = (size - 6.dp) / 2
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(RadiusTokens.icon))
            .background(LocalHomeColors.current.text.copy(alpha = 0.16f))
            .padding(2.dp),
    ) {
        apps.take(4).forEachIndexed { i, app ->
            AppIcon(
                app.id,
                icons,
                cell,
                Modifier.offset(x = (cell + 2.dp) * (i % 2), y = (cell + 2.dp) * (i / 2)),
            )
        }
    }
}

/**
 * Shared row shell: icon, label, optional preview line. Tap, long-press and an optional
 * swipe-right, each also exposed to TalkBack.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherRow(
    label: String,
    variant: AppRowVariant,
    icon: @Composable (Dp) -> Unit,
    preview: String?,
    a11yLabel: String,
    onClick: (Rect?) -> Unit,
    onLongClick: () -> Unit,
    onSwipeRight: (() -> Unit)?,
    swipeActionLabel: String = "Notifications and shortcuts",
    modifier: Modifier = Modifier,
) {
    val colors = LocalHomeColors.current
    val haptics = LocalHapticFeedback.current
    val threshold = with(LocalDensity.current) { 56.dp.toPx() }
    var bounds by remember { mutableStateOf<Rect?>(null) }
    var dragX by remember { mutableFloatStateOf(0f) }

    val favorite = variant == AppRowVariant.Favorite
    val rowHeight = if (favorite) SizeTokens.favoriteRow else SizeTokens.listRow
    val iconSize = if (favorite) SizeTokens.iconFavorite else SizeTokens.iconList
    val labelStyle = (if (favorite) TypeTokens.favorite else TypeTokens.list).onWallpaper(colors)

    var m = modifier
        .fillMaxWidth()
        .heightIn(min = rowHeight)
        .onGloballyPositioned { coords ->
            val r = coords.boundsInWindow()
            bounds = Rect(r.left.roundToInt(), r.top.roundToInt(), r.right.roundToInt(), r.bottom.roundToInt())
        }
        .offset { IntOffset((dragX / 3).roundToInt(), 0) }
        .clip(RoundedCornerShape(RadiusTokens.md))
    if (onSwipeRight != null) {
        m = m.pointerInput(label) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (dragX > threshold) onSwipeRight()
                    dragX = 0f
                },
                onDragCancel = { dragX = 0f },
            ) { change, amount ->
                dragX = (dragX + amount).coerceAtLeast(0f)
                change.consume()
            }
        }
    }
    Row(
        modifier = m
            .combinedClickable(
                onClick = { onClick(bounds) },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
            .semantics(mergeDescendants = true) {
                contentDescription = a11yLabel
                customActions = listOfNotNull(
                    onSwipeRight?.let { swipe -> CustomAccessibilityAction(swipeActionLabel) { swipe(); true } },
                    CustomAccessibilityAction("Options") { onLongClick(); true },
                )
            }
            .padding(vertical = SpaceTokens.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s4),
    ) {
        icon(iconSize)
        Column {
            Text(label, style = labelStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!preview.isNullOrBlank()) {
                Text(
                    preview,
                    style = TypeTokens.preview.onWallpaper(colors, colors.preview),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** One app on the home screen or in the list. Swipe right opens the app pop-up. */
@Composable
fun AppRow(
    app: AppEntity,
    icons: IconCache,
    variant: AppRowVariant,
    preview: String?,
    notificationCount: Int,
    onClick: (Rect?) -> Unit,
    onLongClick: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LauncherRow(
        label = app.label,
        variant = variant,
        icon = { size -> AppIcon(app.id, icons, size) },
        preview = preview,
        a11yLabel = if (notificationCount > 0) "${app.label}, $notificationCount notifications" else app.label,
        onClick = onClick,
        onLongClick = onLongClick,
        onSwipeRight = onSwipeRight,
        modifier = modifier,
    )
}

/** A pop-up folder on the home screen. Tap opens it. */
@Composable
fun FolderRow(
    title: String,
    apps: List<AppEntity>,
    icons: IconCache,
    preview: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LauncherRow(
        label = title,
        variant = AppRowVariant.Favorite,
        icon = { size -> FolderIcon(apps, icons, size) },
        preview = preview,
        a11yLabel = "$title, folder with ${apps.size} apps",
        onClick = { onClick() },
        onLongClick = onLongClick,
        onSwipeRight = null,
        modifier = modifier,
    )
}
