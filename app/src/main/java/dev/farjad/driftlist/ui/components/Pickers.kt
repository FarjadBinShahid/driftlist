package dev.farjad.driftlist.ui.components

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import dev.farjad.driftlist.data.db.AppEntity
import dev.farjad.driftlist.domain.AppSearch
import dev.farjad.driftlist.system.apps.IconCache
import dev.farjad.driftlist.ui.home.HomeEntry
import dev.farjad.driftlist.ui.theme.ElevationTokens
import dev.farjad.driftlist.ui.theme.RadiusTokens
import dev.farjad.driftlist.ui.theme.SizeTokens
import dev.farjad.driftlist.ui.theme.SpaceTokens
import dev.farjad.driftlist.ui.theme.TypeTokens
import dev.farjad.driftlist.ui.theme.style
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RoundedCornerShape(RadiusTokens.pill),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = modifier.fillMaxWidth(),
    )
}

/** S02 / S23: searchable checklist of apps. Takes the remaining height of its parent column. */
@Composable
fun ColumnScope.AppMultiPicker(
    apps: List<AppEntity>,
    icons: IconCache,
    selected: Set<String>,
    onToggle: (AppEntity) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(apps, query) {
        if (query.isBlank()) apps.sortedBy { AppSearch.normalize(it.label) } else AppSearch.rank(query, apps, limit = 200)
    }
    SearchField(query, { query = it }, "Search apps", Modifier.padding(horizontal = SpaceTokens.s5, vertical = SpaceTokens.s3))
    if (shown.isEmpty()) {
        Text(
            "No apps match “$query”.",
            style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(SpaceTokens.s7),
        )
    }
    LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
        items(shown, key = { it.id }) { app ->
            val checked = app.id in selected
            SheetAppRow(app, icons, onClick = { onToggle(app) }) {
                Checkbox(checked = checked, onCheckedChange = { onToggle(app) })
            }
        }
    }
}

/** Create or edit a pop-up folder: a name and a set of apps. */
@Composable
fun FolderEditorSheet(
    initialTitle: String,
    initialAppIds: List<String>,
    apps: List<AppEntity>,
    icons: IconCache,
    onSave: (String, List<String>) -> Unit,
    onClose: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    val chosen = remember { mutableStateListOf<String>().apply { addAll(initialAppIds) } }
    DriftSheet(onClose) {
        Column(Modifier.fillMaxHeight(0.92f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SheetTitle(if (initialAppIds.isEmpty() && initialTitle.isEmpty()) "New folder" else "Edit folder", Modifier.weight(1f))
                TextButton(
                    onClick = { onSave(title, chosen.toList()) },
                    enabled = chosen.isNotEmpty(),
                    modifier = Modifier.padding(end = SpaceTokens.s4),
                ) { Text("Save") }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Folder name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = SpaceTokens.s5),
            )
            Text(
                "${chosen.size} apps",
                style = TypeTokens.caption.style(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s2),
            )
            AppMultiPicker(apps, icons, chosen.toSet()) { app ->
                if (app.id in chosen) chosen.remove(app.id) else chosen.add(app.id)
            }
        }
    }
}

/** S10: reorder, remove and add favorites; create folders. */
@Composable
fun EditFavoritesSheet(
    entries: List<HomeEntry>,
    icons: IconCache,
    onReorder: (List<String>) -> Unit,
    onRemove: (HomeEntry) -> Unit,
    onAddApps: () -> Unit,
    onNewFolder: () -> Unit,
    onClose: () -> Unit,
) {
    val order = remember(entries) { mutableStateListOf<HomeEntry>().apply { addAll(entries) } }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowPx = with(LocalDensity.current) { SizeTokens.favoriteRow.toPx() }

    fun move(from: Int, to: Int) {
        if (to !in order.indices || from !in order.indices) return
        order.add(to, order.removeAt(from))
    }

    DriftSheet(onClose) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SheetTitle("Edit favorites", Modifier.weight(1f))
            TextButton(onClick = onClose, modifier = Modifier.padding(end = SpaceTokens.s4)) { Text("Done") }
        }
        if (order.isEmpty()) {
            Text(
                "No favorites yet. Add a few apps you open every day.",
                style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s3),
            )
        }
        Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
            order.forEachIndexed { index, entry ->
                key(entry.itemId) {
                    val dragging = draggingId == entry.itemId
                    val label = when (entry) {
                        is HomeEntry.App -> entry.app.label
                        is HomeEntry.Folder -> entry.folder.title
                    }
                    Row(
                        modifier = Modifier
                            .zIndex(if (dragging) 1f else 0f)
                            .offset { IntOffset(0, if (dragging) dragOffset.roundToInt() else 0) }
                            .then(if (dragging) Modifier.shadow(ElevationTokens.pop) else Modifier)
                            .background(MaterialTheme.colorScheme.surface)
                            .fillMaxWidth()
                            .height(SizeTokens.favoriteRow)
                            .padding(horizontal = SpaceTokens.s5)
                            .semantics(mergeDescendants = true) {
                                contentDescription = label
                                customActions = listOf(
                                    CustomAccessibilityAction("Move up") { move(index, index - 1); onReorder(order.map { it.itemId }); true },
                                    CustomAccessibilityAction("Move down") { move(index, index + 1); onReorder(order.map { it.itemId }); true },
                                    CustomAccessibilityAction("Remove") { onRemove(entry); true },
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s4),
                    ) {
                        // Drag handle: three bars, drawn with tokens rather than an icon asset.
                        Column(
                            modifier = Modifier
                                .size(SizeTokens.touchMin)
                                .pointerInput(entry.itemId) {
                                    detectDragGestures(
                                        onDragStart = { draggingId = entry.itemId; dragOffset = 0f },
                                        onDragEnd = {
                                            draggingId = null
                                            dragOffset = 0f
                                            onReorder(order.map { it.itemId })
                                        },
                                        onDragCancel = { draggingId = null; dragOffset = 0f },
                                    ) { change, amount ->
                                        change.consume()
                                        dragOffset += amount.y
                                        val i = order.indexOfFirst { it.itemId == entry.itemId }
                                        if (dragOffset > rowPx / 2 && i < order.lastIndex) {
                                            move(i, i + 1); dragOffset -= rowPx
                                        } else if (dragOffset < -rowPx / 2 && i > 0) {
                                            move(i, i - 1); dragOffset += rowPx
                                        }
                                    }
                                },
                            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            repeat(3) {
                                Box(
                                    Modifier
                                        .size(width = 18.dp, height = 2.dp)
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(RadiusTokens.pill)),
                                )
                            }
                        }
                        when (entry) {
                            is HomeEntry.App -> AppIcon(entry.app.id, icons, SizeTokens.iconList)
                            is HomeEntry.Folder -> FolderIcon(entry.apps, icons, SizeTokens.iconList)
                        }
                        Text(
                            label,
                            style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurface),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { onRemove(entry) }) { Text("Remove") }
                    }
                }
            }
        }
        Row(Modifier.padding(horizontal = SpaceTokens.s4, vertical = SpaceTokens.s3), horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s3)) {
            TextButton(onClick = onAddApps) { Text("Add apps") }
            TextButton(onClick = onNewFolder) { Text("New folder") }
        }
    }
}

/** Pick apps to add as favorites. */
@Composable
fun AddFavoritesSheet(
    apps: List<AppEntity>,
    icons: IconCache,
    onAdd: (List<String>) -> Unit,
    onClose: () -> Unit,
) {
    val chosen = remember { mutableStateListOf<String>() }
    DriftSheet(onClose) {
        Column(Modifier.fillMaxHeight(0.92f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SheetTitle("Add favorites", Modifier.weight(1f))
                TextButton(
                    onClick = { onAdd(chosen.toList()) },
                    enabled = chosen.isNotEmpty(),
                    modifier = Modifier.padding(end = SpaceTokens.s4),
                ) { Text(if (chosen.isEmpty()) "Add" else "Add ${chosen.size}") }
            }
            AppMultiPicker(apps, icons, chosen.toSet()) { app ->
                if (app.id in chosen) chosen.remove(app.id) else chosen.add(app.id)
            }
        }
    }
}

/** S13: choose a widget provider. */
@Composable
fun WidgetPickerSheet(
    providers: List<AppWidgetProviderInfo>,
    onPick: (AppWidgetProviderInfo) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    var query by rememberSaveable { mutableStateOf("") }
    val rows = remember(providers) {
        providers.map { info ->
            val app = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString() }
                .getOrDefault(info.provider.packageName)
            Triple(info, app, info.loadLabel(pm) ?: app)
        }.sortedWith(compareBy({ it.second.lowercase() }, { it.third.lowercase() }))
    }
    val shown = remember(rows, query) {
        if (query.isBlank()) rows else rows.filter { (_, app, label) ->
            AppSearch.score(query, app) != null || AppSearch.score(query, label) != null
        }
    }
    DriftSheet(onClose) {
        Column(Modifier.fillMaxHeight(0.92f)) {
            SheetTitle("Add a widget")
            SearchField(query, { query = it }, "Search widgets", Modifier.padding(horizontal = SpaceTokens.s5, vertical = SpaceTokens.s2))
            if (shown.isEmpty()) {
                Text(
                    if (rows.isEmpty()) "No apps on this phone offer widgets." else "No widgets match “$query”.",
                    style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.padding(SpaceTokens.s7),
                )
            }
            LazyColumn(Modifier.weight(1f)) {
                items(shown, key = { it.first.provider.flattenToString() + it.first.profile.hashCode() }) { (info, app, label) ->
                    val density = context.resources.displayMetrics.densityDpi
                    val px = with(LocalDensity.current) { SizeTokens.iconSheet.roundToPx() }
                    val icon by produceState<ImageBitmap?>(null, info) {
                        value = withContext(Dispatchers.IO) {
                            runCatching { info.loadIcon(context, density).toBitmap(px, px).asImageBitmap() }.getOrNull()
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = SizeTokens.settingsRow)
                            .clickable { onPick(info) }
                            .padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s3),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s5),
                    ) {
                        Box(Modifier.size(SizeTokens.iconSheet)) { icon?.let { Image(it, null, Modifier.size(SizeTokens.iconSheet)) } }
                        Column(Modifier.weight(1f)) {
                            Text(label, style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurface), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "$app · ${info.minWidth.pxToDp()} × ${info.minHeight.pxToDp()} dp",
                                style = TypeTokens.caption.style(MaterialTheme.colorScheme.onSurfaceVariant),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Int.pxToDp(): Int = with(LocalDensity.current) { this@pxToDp.toDp().value.roundToInt() }

/** S09: choose an icon for one app from the current icon pack. */
@Composable
fun IconPickerSheet(
    app: AppEntity,
    icons: IconCache,
    pack: String?,
    packLabel: String?,
    onPick: (String?) -> Unit,
    onOpenIconSettings: () -> Unit,
    onClose: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val names = remember(pack) { pack?.let { icons.packs.allNames(it) }.orEmpty() }
    val suggested = remember(pack, app.id) { pack?.let { icons.packs.nameFor(it, "${app.packageName}/${app.activityClass}") } }
    val shown = remember(names, query, app.id) {
        val base = if (query.isBlank()) {
            // Suggestions first: names that contain a word of the app's label.
            val words = AppSearch.normalize(app.label).split(' ').filter { it.length > 2 }
            val (hits, rest) = names.partition { n -> words.any { w -> n.contains(w) } }
            hits + rest
        } else {
            val q = AppSearch.normalize(query).replace(' ', '_')
            names.filter { it.contains(q) }
        }
        base.take(600)
    }
    val cellPx = with(LocalDensity.current) { SizeTokens.iconSheet.roundToPx() }

    DriftSheet(onClose) {
        Column(Modifier.fillMaxHeight(0.92f)) {
            SheetTitle("Icon for ${app.label}")
            if (pack == null) {
                Text(
                    "Icons come from an icon pack. Install one from the Play Store, then choose it in Driftlist settings.",
                    style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s3),
                )
                TextButton(onClick = onOpenIconSettings, modifier = Modifier.padding(horizontal = SpaceTokens.s5)) { Text("Open icon settings") }
                return@Column
            }
            Text(
                "From ${packLabel ?: pack}" + if (suggested == null) " · this pack has no icon for this app yet" else "",
                style = TypeTokens.caption.style(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(horizontal = SpaceTokens.s7),
            )
            SearchField(query, { query = it }, "Search icons", Modifier.padding(horizontal = SpaceTokens.s5, vertical = SpaceTokens.s2))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(64.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(SpaceTokens.s5),
                horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s3),
                verticalArrangement = Arrangement.spacedBy(SpaceTokens.s3),
            ) {
                item(key = "__default") {
                    val bmp by produceState<ImageBitmap?>(null, app.id) { value = icons.systemIcon(app.id, cellPx) }
                    IconCell(bmp, "Default icon", selected = false) { onPick(null) }
                }
                items(shown, key = { it }) { name ->
                    val bmp by produceState<ImageBitmap?>(null, pack, name) { value = icons.packIcon(pack, name, cellPx) }
                    IconCell(bmp, name.replace('_', ' '), selected = name == suggested) { onPick(name) }
                }
            }
        }
    }
}

@Composable
private fun IconCell(bitmap: ImageBitmap?, description: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(RadiusTokens.md))
            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(RadiusTokens.md)) else Modifier)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(SpaceTokens.s2),
        contentAlignment = Alignment.Center,
    ) {
        bitmap?.let { Image(it, null, Modifier.size(SizeTokens.iconSheet)) }
    }
}
