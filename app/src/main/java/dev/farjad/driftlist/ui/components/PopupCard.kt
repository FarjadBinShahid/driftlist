package dev.farjad.driftlist.ui.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import dev.farjad.driftlist.data.db.WidgetEntity
import dev.farjad.driftlist.system.widgets.WidgetHostManager
import android.graphics.Rect
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.farjad.driftlist.data.db.AppEntity
import dev.farjad.driftlist.system.apps.IconCache
import dev.farjad.driftlist.system.apps.ShortcutInfoItem
import dev.farjad.driftlist.system.notifications.NotifAction
import dev.farjad.driftlist.system.notifications.NotifItem
import dev.farjad.driftlist.ui.theme.ElevationTokens
import dev.farjad.driftlist.ui.theme.RadiusTokens
import dev.farjad.driftlist.ui.theme.SizeTokens
import dev.farjad.driftlist.ui.theme.SpaceTokens
import dev.farjad.driftlist.ui.theme.TypeTokens
import dev.farjad.driftlist.ui.theme.style

/**
 * S07: the app pop-up as a floating card that opens right at the row you swiped (measured on
 * the reference), over the list: header (open + name), notifications, then shortcuts.
 * Long-press a shortcut to put it in a folder.
 */
@Composable
fun AppPopupCard(
    anchor: Rect?,
    app: AppEntity,
    icons: IconCache,
    notifications: List<NotifItem>,
    shortcuts: List<ShortcutInfoItem>,
    shortcutIcon: suspend (ShortcutInfoItem, Int) -> ImageBitmap?,
    onOpenApp: () -> Unit,
    onOpenNotification: (NotifItem) -> Unit,
    onDismissNotification: (NotifItem) -> Unit,
    onReply: (NotifItem, String) -> Boolean,
    onRunAction: (NotifAction) -> Unit,
    onShortcut: (ShortcutInfoItem) -> Unit,
    onShortcutLongPress: (ShortcutInfoItem) -> Unit,
    onClose: () -> Unit,
    quickReplies: Boolean = true,
    popupApps: List<AppEntity> = emptyList(),
    widgets: List<WidgetEntity> = emptyList(),
    widgetHost: WidgetHostManager? = null,
    onLaunchApp: (AppEntity) -> Unit = {},
    onEdit: (() -> Unit)? = null,
) {
    val density = LocalDensity.current
    val position = remember(anchor) {
        object : PopupPositionProvider {
            override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
                val margin = with(density) { SpaceTokens.s7.roundToPx() }
                val x = with(density) { SizeTokens.edgeGutter.roundToPx() } - with(density) { SpaceTokens.s3.roundToPx() }
                val wanted = anchor?.top ?: ((windowSize.height - popupContentSize.height) / 2)
                val maxY = (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin)
                return IntOffset(x.coerceAtLeast(0), wanted.coerceIn(margin, maxY))
            }
        }
    }
    Popup(popupPositionProvider = position, onDismissRequest = onClose, properties = PopupProperties(focusable = true)) {
        Surface(
            shape = RoundedCornerShape(RadiusTokens.lg),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = ElevationTokens.pop,
            modifier = Modifier.width(SizeTokens.popupCardWidth).heightIn(max = 520.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = SizeTokens.popupHeader)
                        .clickable(onClickLabel = "Open ${app.label}", onClick = onOpenApp)
                        .padding(horizontal = SpaceTokens.s5),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s4),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(SizeTokens.sheetIcon))
                    Text(
                        app.label,
                        style = TypeTokens.label.style(MaterialTheme.colorScheme.onSurface),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).semantics { heading() },
                    )
                    if (onEdit != null) {
                        IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "Edit this pop-up") }
                    }
                }
                if (widgets.isNotEmpty() && widgetHost != null) {
                    WidgetStack(widgets, widgetHost, Modifier.padding(horizontal = SpaceTokens.s3).padding(bottom = SpaceTokens.s3))
                }
                if (popupApps.isNotEmpty()) {
                    PopupAppGrid(popupApps, icons, onLaunchApp)
                }
                LazyColumn(
                    contentPadding = PaddingValues(start = SpaceTokens.s3, end = SpaceTokens.s3, bottom = SpaceTokens.s3),
                    verticalArrangement = Arrangement.spacedBy(SpaceTokens.s2),
                ) {
                    items(notifications, key = { it.key }) { item ->
                        NotificationCard(
                            item = item,
                            onOpen = { onOpenNotification(item) },
                            onDismiss = { onDismissNotification(item) },
                            onReply = { text -> onReply(item, text) },
                            onAction = onRunAction,
                            canReply = quickReplies,
                        )
                    }
                    if (notifications.isNotEmpty() && shortcuts.isNotEmpty()) {
                        item { HorizontalDivider(Modifier.padding(vertical = SpaceTokens.s2)) }
                    }
                    if (notifications.isEmpty() && shortcuts.isEmpty() && popupApps.isEmpty() && widgets.isEmpty()) {
                        item {
                            Text(
                                "Nothing new, and no shortcuts.",
                                style = TypeTokens.bodySm.style(MaterialTheme.colorScheme.onSurfaceVariant),
                                modifier = Modifier.padding(SpaceTokens.s4),
                            )
                        }
                    }
                    items(shortcuts, key = { "sc:" + it.id }) { sc ->
                        PopupShortcutRow(sc, shortcutIcon, onClick = { onShortcut(sc) }, onLongClick = { onShortcutLongPress(sc) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PopupShortcutRow(
    item: ShortcutInfoItem,
    iconLoader: suspend (ShortcutInfoItem, Int) -> ImageBitmap?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val px = with(LocalDensity.current) { SizeTokens.iconPopup.roundToPx() }
    val icon by produceState<ImageBitmap?>(null, item.id) { value = iconLoader(item, px) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SizeTokens.popupRow)
            .clip(RoundedCornerShape(RadiusTokens.md))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = "Add to a folder")
            .semantics { customActions = listOf(CustomAccessibilityAction("Add to a folder") { onLongClick(); true }) }
            .padding(horizontal = SpaceTokens.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s5),
    ) {
        Box(
            Modifier.size(SizeTokens.iconPopup).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            icon?.let { Image(it, null, Modifier.size(SizeTokens.iconPopup)) }
        }
        Text(item.label, style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurface), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Pick a folder for a shortcut, or name a new one. */
@Composable
fun ShortcutFolderDialog(
    shortcutLabel: String,
    folders: List<Pair<String, String>>,
    onPick: (folderId: String?, newTitle: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var creating by rememberSaveable { mutableStateOf(folders.isEmpty()) }
    var title by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add “$shortcutLabel” to a folder") },
        text = {
            Column {
                if (!creating) {
                    folders.forEach { (id, name) ->
                        TextButton(onClick = { onPick(id, "") }, modifier = Modifier.fillMaxWidth()) {
                            Text(name, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    TextButton(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("New folder…", modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Folder name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            if (creating) TextButton(onClick = { onPick(null, title) }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Apps the user placed inside this pop-up, four to a row. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PopupAppGrid(apps: List<AppEntity>, icons: IconCache, onLaunch: (AppEntity) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = SpaceTokens.s3, vertical = SpaceTokens.s2),
        maxItemsInEachRow = POPUP_GRID_COLUMNS,
    ) {
        apps.forEach { app ->
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(RadiusTokens.md))
                    .clickable(onClickLabel = "Open ${app.label}") { onLaunch(app) }
                    .padding(vertical = SpaceTokens.s2),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppIcon(app.id, icons, SizeTokens.iconPopup)
                Text(
                    app.label,
                    style = TypeTokens.caption.style(MaterialTheme.colorScheme.onSurface),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = SpaceTokens.s1),
                )
            }
        }
        // Keep the last row's tiles the same width as the others.
        repeat((POPUP_GRID_COLUMNS - apps.size % POPUP_GRID_COLUMNS) % POPUP_GRID_COLUMNS) { Spacer(Modifier.weight(1f)) }
    }
    HorizontalDivider(Modifier.padding(horizontal = SpaceTokens.s5, vertical = SpaceTokens.s2))
}

private const val POPUP_GRID_COLUMNS = 4

/** S10 editor: what an app's pop-up shows (apps, a widget, its shortcuts). */
@Composable
fun PopupEditorSheet(
    app: AppEntity,
    apps: List<AppEntity>,
    selected: Set<String>,
    icons: IconCache,
    widgetCount: Int,
    shortcutsShown: Boolean,
    onToggleApp: (AppEntity) -> Unit,
    onToggleShortcuts: () -> Unit,
    onAddWidget: () -> Unit,
    onRemoveWidget: () -> Unit,
    onClose: () -> Unit,
) {
    DriftSheet(onClose) {
        Column(Modifier.fillMaxHeight(0.9f)) {
            SheetTitle("${app.label} pop-up")
            CardRow(
                "App shortcuts",
                Icons.Outlined.Bolt,
                onToggleShortcuts,
                supporting = "Show the shortcuts the app offers",
                trailing = { Switch(checked = shortcutsShown, onCheckedChange = null) },
            )
            if (widgetCount == 0) {
                CardRow("Add a widget", Icons.Outlined.Widgets, onAddWidget, supporting = "Shown at the top of this pop-up")
            } else {
                CardRow("Remove widget", Icons.Outlined.Delete, onRemoveWidget)
            }
            Text(
                "Apps in this pop-up",
                style = TypeTokens.label.style(MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(start = SpaceTokens.s7, top = SpaceTokens.s5, bottom = SpaceTokens.s2).semantics { heading() },
            )
            AppMultiPicker(apps.filter { it.id != app.id }, icons, selected, onToggleApp)
        }
    }
}
