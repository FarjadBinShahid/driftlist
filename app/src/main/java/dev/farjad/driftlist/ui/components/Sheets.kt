package dev.farjad.driftlist.ui.components

import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.Slider
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Switch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Reorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.farjad.driftlist.data.db.AppEntity
import dev.farjad.driftlist.system.apps.IconCache
import dev.farjad.driftlist.data.db.ShortcutEntity
import dev.farjad.driftlist.system.notifications.NotifAction
import dev.farjad.driftlist.system.notifications.NotifItem
import dev.farjad.driftlist.ui.theme.RadiusTokens
import dev.farjad.driftlist.ui.theme.SizeTokens
import dev.farjad.driftlist.ui.theme.SpaceTokens
import dev.farjad.driftlist.ui.theme.TypeTokens
import dev.farjad.driftlist.ui.theme.style

/** Base for every bottom sheet: token shape, nav-bar and keyboard padding. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriftSheet(onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = RadiusTokens.sheet, topEnd = RadiusTokens.sheet),
        // No Material drag handle: its slot wraps the handle in a 32 dp actionable box. The sheet
        // closes by dragging, tapping outside or Back, so ours is purely visual (below).
        dragHandle = null,
    ) {
        Column(Modifier.navigationBarsPadding().imePadding().padding(bottom = SpaceTokens.s4)) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = SpaceTokens.s5)
                    .size(SizeTokens.sheetHandleWidth, SizeTokens.sheetHandleHeight)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(RadiusTokens.pill))
                    .clearAndSetSemantics { },
            )
            content()
        }
    }
}

@Composable
fun SheetTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = TypeTokens.title.style(MaterialTheme.colorScheme.onSurface),
        modifier = modifier
            .padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s4)
            .semantics { heading() },
    )
}

@Composable
private fun SheetHeader(app: AppEntity, icons: IconCache, onIconClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s5),
    ) {
        val iconModifier = if (onIconClick != null) {
            Modifier
                .clickable(onClickLabel = "Change icon", onClick = onIconClick)
                .semantics { contentDescription = "Change icon for ${app.label}" }
        } else {
            Modifier
        }
        AppIcon(app.id, icons, SizeTokens.iconSheet, iconModifier)
        Text(
            app.label,
            style = TypeTokens.title.style(MaterialTheme.colorScheme.onSurface),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() },
        )
    }
}

/** An app row inside a sheet (surface colours, not wallpaper colours). */
@Composable
fun SheetAppRow(
    app: AppEntity,
    icons: IconCache,
    onClick: () -> Unit,
    supporting: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SizeTokens.listRow)
            .clickable(onClick = onClick)
            .padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s5),
    ) {
        AppIcon(app.id, icons, SizeTokens.iconList)
        Column(Modifier.weight(1f)) {
            Text(
                app.label,
                style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurface),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            supporting?.let { Text(it, style = TypeTokens.caption.style(MaterialTheme.colorScheme.onSurfaceVariant)) }
        }
        trailing?.invoke()
    }
}

@Composable
fun OptionRow(
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
    icon: ImageVector? = null,
    supporting: String? = null,
) {
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SizeTokens.sheetRow)
            .clickable(onClick = onClick)
            .padding(horizontal = SpaceTokens.s5),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(SizeTokens.sheetRow), contentAlignment = Alignment.Center) {
            icon?.let { Icon(it, contentDescription = null, tint = color, modifier = Modifier.size(SizeTokens.sheetIcon)) }
        }
        Column(Modifier.padding(vertical = SpaceTokens.s3)) {
            Text(label, style = TypeTokens.body.style(color))
            supporting?.let { Text(it, style = TypeTokens.caption.style(MaterialTheme.colorScheme.onSurfaceVariant)) }
        }
    }
}

// --- notification card (used by the pop-up card) ----------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationCard(
    item: NotifItem,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onReply: (String) -> Boolean,
    onAction: (NotifAction) -> Unit,
) {
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) onDismiss()
    }
    var replying by rememberSaveable(item.key) { mutableStateOf(false) }
    var draft by rememberSaveable(item.key) { mutableStateOf("") }
    var status by remember(item.key) { mutableStateOf<String?>(null) }

    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = item.clearable,
        backgroundContent = {},
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(RadiusTokens.md),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .semantics {
                    if (item.clearable) {
                        customActions = listOf(CustomAccessibilityAction("Dismiss") { onDismiss(); true })
                    }
                },
        ) {
            Column(Modifier.padding(SpaceTokens.s5)) {
                item.title?.let {
                    Text(it, style = TypeTokens.label.style(MaterialTheme.colorScheme.onSurface), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                item.text?.let {
                    Text(it, style = TypeTokens.bodySm.style(MaterialTheme.colorScheme.onSurfaceVariant), maxLines = 6, overflow = TextOverflow.Ellipsis)
                }
                status?.let {
                    Text(it, style = TypeTokens.caption.style(MaterialTheme.colorScheme.primary), modifier = Modifier.padding(top = SpaceTokens.s2))
                }
                if (replying) {
                    val focus = remember { FocusRequester() }
                    LaunchedEffect(Unit) { focus.requestFocus() }
                    val send = {
                        if (draft.isNotBlank()) {
                            val ok = onReply(draft.trim())
                            status = if (ok) "Sent" else "Couldn't send. Open the app to reply."
                            if (ok) {
                                draft = ""
                                replying = false
                            }
                        }
                    }
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.fillMaxWidth().padding(top = SpaceTokens.s3).focusRequester(focus),
                        placeholder = { Text("Reply") },
                        shape = RoundedCornerShape(RadiusTokens.lg),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { send() }),
                        trailingIcon = {
                            TextButton(onClick = { send() }, enabled = draft.isNotBlank()) { Text("Send") }
                        },
                        maxLines = 4,
                    )
                } else if (item.replyAction != null || item.buttonActions.isNotEmpty()) {
                    Row(Modifier.padding(top = SpaceTokens.s2), horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s2)) {
                        if (item.replyAction != null) {
                            TextButton(onClick = { replying = true; status = null }) { Text("Reply") }
                        }
                        item.buttonActions.forEach { action ->
                            TextButton(onClick = { onAction(action) }) {
                                Text(action.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- S08 app options --------------------------------------------------------------------------

/** S08: long-press options for one app. Rarer actions sit under Advanced, as on the reference. */
@Composable
fun AppOptionsSheet(
    app: AppEntity,
    icons: IconCache,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onEditFavorites: () -> Unit,
    onRename: (String) -> Unit,
    onChangeIcon: () -> Unit,
    onToggleHidden: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    isSuggestion: Boolean = false,
    onStopSuggesting: () -> Unit = {},
) {
    var renaming by rememberSaveable { mutableStateOf(false) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf(app.label) }
    DriftSheet(onClose) {
        SheetHeader(app, icons, onIconClick = onChangeIcon)
        if (renaming) {
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { focus.requestFocus() }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onRename(name) }),
                modifier = Modifier.fillMaxWidth().padding(horizontal = SpaceTokens.s7).focusRequester(focus),
            )
            Row(Modifier.align(Alignment.End).padding(horizontal = SpaceTokens.s4)) {
                if (app.customLabel != null) TextButton(onClick = { onRename("") }) { Text("Reset") }
                TextButton(onClick = { renaming = false }) { Text("Cancel") }
                TextButton(onClick = { onRename(name) }) { Text("Save") }
            }
        } else {
            if (isSuggestion) {
                OptionRow("Stop suggesting", onStopSuggesting, icon = Icons.Outlined.Block, supporting = "Won't appear under your favorites again")
            }
            OptionRow(
                if (isFavorite) "Remove from favorites" else "Add to favorites",
                onToggleFavorite,
                icon = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
            )
            if (isFavorite) OptionRow("Edit favorites", onEditFavorites, icon = Icons.Outlined.Reorder)
            OptionRow("App info", onAppInfo, icon = Icons.Outlined.Info)
            if (!app.isSystem) OptionRow("Uninstall", onUninstall, destructive = true, icon = Icons.Outlined.Delete)
            OptionRow(
                "More",
                { advanced = !advanced },
                icon = if (advanced) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            )
            if (advanced) {
                OptionRow("Rename", { renaming = true }, icon = Icons.Outlined.Edit)
                OptionRow("Change icon", onChangeIcon, icon = Icons.Outlined.Image)
                OptionRow(
                    if (app.hidden) "Show in app list" else "Hide from app list",
                    onToggleHidden,
                    icon = if (app.hidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                )
            }
            HorizontalDivider(Modifier.padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s2))
            OptionRow("Driftlist settings", onOpenSettings, icon = Icons.Outlined.Settings)
        }
    }
}

// --- S11 folder -------------------------------------------------------------------------------

/** S11: an open pop-up folder: its apps, then any shortcuts added from app pop-ups. */
@Composable
fun FolderSheet(
    title: String,
    apps: List<AppEntity>,
    shortcuts: List<ShortcutEntity>,
    icons: IconCache,
    shortcutIcon: suspend (ShortcutEntity, Int) -> ImageBitmap?,
    onLaunch: (AppEntity) -> Unit,
    onLaunchShortcut: (ShortcutEntity) -> Unit,
    onRemoveShortcut: (ShortcutEntity) -> Unit,
    onEdit: () -> Unit,
    onClose: () -> Unit,
) {
    DriftSheet(onClose) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SheetTitle(title, Modifier.weight(1f))
            TextButton(onClick = onEdit, modifier = Modifier.padding(end = SpaceTokens.s4)) { Text("Edit") }
        }
        if (apps.isEmpty() && shortcuts.isEmpty()) {
            Text(
                "This folder is empty. Tap Edit to add apps, or long-press a shortcut in an app's pop-up.",
                style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s4),
            )
        }
        LazyColumn(Modifier.heightIn(max = 480.dp)) {
            items(apps, key = { it.id }) { app -> SheetAppRow(app, icons, onClick = { onLaunch(app) }) }
            items(shortcuts, key = { "sc:" + it.id }) { sc ->
                FolderShortcutRow(sc, shortcutIcon, onClick = { onLaunchShortcut(sc) }, onRemove = { onRemoveShortcut(sc) })
            }
        }
    }
}

@Composable
private fun FolderShortcutRow(
    shortcut: ShortcutEntity,
    iconLoader: suspend (ShortcutEntity, Int) -> ImageBitmap?,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val px = with(LocalDensity.current) { SizeTokens.iconList.roundToPx() }
    val icon by produceState<ImageBitmap?>(null, shortcut.id) { value = iconLoader(shortcut, px) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SizeTokens.listRow)
            .clickable(onClick = onClick)
            .semantics { customActions = listOf(CustomAccessibilityAction("Remove from folder") { onRemove(); true }) }
            .padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s5),
    ) {
        Box(Modifier.size(SizeTokens.iconList)) { icon?.let { Image(it, null, Modifier.size(SizeTokens.iconList)) } }
        Text(
            shortcut.label,
            style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurface),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRemove) { Text("Remove") }
    }
}

@Composable
fun FolderOptionsSheet(title: String, onEdit: () -> Unit, onRemove: () -> Unit, onClose: () -> Unit) {
    DriftSheet(onClose) {
        SheetTitle(title)
        OptionRow("Edit folder", onEdit)
        OptionRow("Remove folder", onRemove, destructive = true)
    }
}

// --- S12 header options ---------------------------------------------------------------------

/** One rounded card row, the style the reference uses in its clock sheet. */
@Composable
fun CardRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    supporting: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(RadiusTokens.lg),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s2)
            .clip(RoundedCornerShape(RadiusTokens.lg))
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.heightIn(min = 60.dp).padding(horizontal = SpaceTokens.s5, vertical = SpaceTokens.s3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s5),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(SizeTokens.sheetIcon))
            Column(Modifier.weight(1f)) {
                Text(title, style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurface))
                supporting?.let { Text(it, style = TypeTokens.caption.style(MaterialTheme.colorScheme.onSurfaceVariant)) }
            }
            trailing?.invoke()
        }
    }
}

@Composable
fun HeaderOptionsSheet(
    widgetCount: Int,
    maxWidgets: Int,
    onAddWidget: () -> Unit,
    onRemoveWidget: () -> Unit,
    onEditFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    calendarOn: Boolean = false,
    onToggleCalendar: () -> Unit = {},
    onMoveClock: () -> Unit = {},
) {
    DriftSheet(onClose) {
        SheetTitle("Home screen")
        CardRow(
            "Calendar",
            Icons.Outlined.CalendarMonth,
            onToggleCalendar,
            supporting = "Next event under the date; tap the date for the week",
            trailing = { Switch(checked = calendarOn, onCheckedChange = null) },
        )
        if (widgetCount < maxWidgets) {
            CardRow(
                if (widgetCount == 0) "Add a widget" else "Add another widget",
                Icons.Outlined.Widgets,
                onAddWidget,
                supporting = if (widgetCount == 0) "Shown under the clock" else "Swipe between up to $maxWidgets",
            )
        }
        if (widgetCount > 0) {
            CardRow(
                if (widgetCount == 1) "Remove widget" else "Remove the widget you're looking at",
                Icons.Outlined.Delete,
                onRemoveWidget,
            )
        }
        CardRow("Move clock", Icons.Outlined.UnfoldMore, onMoveClock, supporting = "Higher or lower on the screen")
        CardRow("Edit favorites", Icons.Outlined.Reorder, onEditFavorites)
        CardRow("Driftlist settings", Icons.Outlined.Settings, onOpenSettings)
        Box(Modifier.height(SpaceTokens.s2))
    }
}

// --- setup card -------------------------------------------------------------------------------

/** Inline card on home asking for a missing setup step (S03, S04). */
@Composable
fun SetupCard(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shape = RoundedCornerShape(RadiusTokens.lg),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(SpaceTokens.s5)) {
            Text(title, style = TypeTokens.label.style(MaterialTheme.colorScheme.onSurface))
            Spacer(Modifier.height(SpaceTokens.s2))
            Text(body, style = TypeTokens.bodySm.style(MaterialTheme.colorScheme.onSurfaceVariant))
            Spacer(Modifier.height(SpaceTokens.s4))
            Button(onClick = onAction, modifier = Modifier.align(Alignment.End)) { Text(action) }
        }
    }
}

/** Slide to move the clock; home updates behind the sheet as you drag. */
@Composable
fun MoveClockSheet(position: Float, onChange: (Float) -> Unit, onReset: () -> Unit, onClose: () -> Unit) {
    DriftSheet(onClose) {
        SheetTitle("Move clock")
        Slider(
            value = position,
            onValueChange = onChange,
            valueRange = SizeTokens.headerPositionMin..SizeTokens.headerPositionMax,
            modifier = Modifier
                .padding(horizontal = SpaceTokens.s7)
                .semantics { contentDescription = "Clock position, top to lower down" },
        )
        Row(Modifier.padding(horizontal = SpaceTokens.s7), verticalAlignment = Alignment.CenterVertically) {
            Text("Higher", style = TypeTokens.caption.style(MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.weight(1f))
            Text("Lower", style = TypeTokens.caption.style(MaterialTheme.colorScheme.onSurfaceVariant))
        }
        TextButton(onClick = onReset, modifier = Modifier.padding(start = SpaceTokens.s5, top = SpaceTokens.s2)) { Text("Reset") }
        Box(Modifier.height(SpaceTokens.s2))
    }
}
