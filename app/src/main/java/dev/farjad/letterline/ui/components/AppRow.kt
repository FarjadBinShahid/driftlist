package dev.farjad.letterline.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import dev.farjad.letterline.data.IconStyle
import dev.farjad.letterline.domain.Palette
import dev.farjad.letterline.ui.theme.LocalIconStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import dev.farjad.letterline.data.db.AppEntity
import dev.farjad.letterline.system.apps.IconCache
import dev.farjad.letterline.ui.theme.LocalHomeColors
import dev.farjad.letterline.ui.theme.RadiusTokens
import dev.farjad.letterline.ui.theme.SizeTokens
import dev.farjad.letterline.ui.theme.SpaceTokens
import dev.farjad.letterline.ui.theme.TypeTokens
import dev.farjad.letterline.ui.theme.onWallpaper
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
    onSwipeRight: ((Rect?) -> Unit)?,
    swipeActionLabel: String = "Notifications and shortcuts",
    labelSuffix: String? = null,
    showDetailsButton: Boolean = false,
    modifier: Modifier = Modifier,
    dot: @Composable () -> Unit = { HomeDot(LocalHomeColors.current.text) },
    align: String = "start",
    unread: Boolean = false,
    /** With centre or right alignment: every row's label takes this width, so icons line up. */
    labelWidth: Dp? = null,
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
                    if (dragX > threshold) onSwipeRight(bounds)
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
                    onSwipeRight?.let { swipe -> CustomAccessibilityAction(swipeActionLabel) { swipe(bounds); true } },
                    CustomAccessibilityAction("Options") { onLongClick(); true },
                )
            }
            .padding(vertical = SpaceTokens.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = when (align) {
            "center" -> Arrangement.spacedBy(SizeTokens.iconLabelGap, Alignment.CenterHorizontally)
            "end" -> Arrangement.spacedBy(SizeTokens.iconLabelGap, Alignment.End)
            else -> Arrangement.spacedBy(SizeTokens.iconLabelGap)
        },
    ) {
        when (LocalIconStyle.current) {
            IconStyle.Icons -> icon(iconSize)
            IconStyle.Dots -> Box(Modifier.size(SizeTokens.iconDotBox), contentAlignment = Alignment.Center) { dot() }
            IconStyle.None -> Unit
        }
        Column(
            when {
                align == "start" -> Modifier.weight(1f)
                labelWidth != null -> Modifier.width(labelWidth)
                else -> Modifier.weight(1f, fill = false)
            },
        ) {
            Text(
                if (labelSuffix != null) "$label \u00B7 $labelSuffix" else label,
                style = labelStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!preview.isNullOrBlank()) {
                Text(
                    preview,
                    style = TypeTokens.preview.onWallpaper(colors, colors.preview),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (unread) {
            Box(Modifier.size(SizeTokens.eventDot).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
        if (showDetailsButton && onSwipeRight != null) {
            IconButton(onClick = { onSwipeRight(bounds) }, modifier = Modifier.size(SizeTokens.rowAction)) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Open notifications",
                    tint = colors.text,
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
    onSwipeRight: (Rect?) -> Unit,
    modifier: Modifier = Modifier,
    previewAge: String? = null,
    spokenLabel: String = app.label,
    paused: Boolean = false,
    align: String = "start",
    dotsOnly: Boolean = false,
    labelWidth: Dp? = null,
) {
    LauncherRow(
        label = app.label,
        variant = variant,
        icon = { size -> AppIcon(app.id, icons, size) },
        preview = when {
            paused -> "Work apps are paused. Tap to turn them on"
            dotsOnly -> null
            else -> preview
        },
        a11yLabel = when {
            paused -> "$spokenLabel, paused"
            notificationCount > 0 -> "$spokenLabel, $notificationCount notifications"
            else -> spokenLabel
        },
        onClick = onClick,
        onLongClick = onLongClick,
        onSwipeRight = onSwipeRight,
        labelSuffix = if (notificationCount > 0 && !dotsOnly) previewAge else null,
        showDetailsButton = notificationCount > 0 && !dotsOnly,
        align = align,
        unread = dotsOnly && notificationCount > 0,
        labelWidth = labelWidth,
        modifier = if (paused) modifier.alpha(PAUSED_ALPHA) else modifier,
        dot = { AppDot(app.id, icons) },
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
    align: String = "start",
    labelWidth: Dp? = null,
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
        dot = { HomeDot(LocalHomeColors.current.text, ring = true) },
        align = align,
        labelWidth = labelWidth,
    )
}

/** The Dots icon style: a small dot in [color]. */
@Composable
fun HomeDot(color: Color, ring: Boolean = false) {
    Box(
        Modifier
            .size(SizeTokens.iconDot)
            .clip(CircleShape)
            .then(if (ring) Modifier.border(SpaceTokens.s1, color, CircleShape) else Modifier.background(color)),
    )
}

/** A dot in the app's own colour: the average of its icon, nudged to stay visible on the wallpaper. */
@Composable
fun AppDot(appId: String, icons: IconCache) {
    val fallback = LocalHomeColors.current.text
    val color by produceState(fallback, appId) {
        value = withContext(Dispatchers.Default) {
            icons.load(appId, DOT_SAMPLE_PX)?.let { averageColor(it.asAndroidBitmap()) } ?: fallback
        }
    }
    HomeDot(color)
}

private fun averageColor(bmp: android.graphics.Bitmap): Color {
    var r = 0L; var g = 0L; var b = 0L; var n = 0L
    for (x in 0 until bmp.width) for (y in 0 until bmp.height) {
        val p = bmp.getPixel(x, y)
        if ((p ushr 24) < 128) continue
        r += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; b += p and 0xFF; n++
    }
    if (n == 0L) return Color.White
    val (h, s, _) = Palette.hsl((0xFF shl 24) or ((r / n).toInt() shl 16) or ((g / n).toInt() shl 8) or (b / n).toInt())
    // Mid lightness, decent saturation: readable on dark and light wallpapers alike.
    return Color(Palette.fromHsl(h, s.coerceIn(0.35f, 0.9f), 0.6f))
}

private const val DOT_SAMPLE_PX = 12

private const val PAUSED_ALPHA = 0.5f
