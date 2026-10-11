package dev.farjad.driftlist.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import dev.farjad.driftlist.data.Accents
import dev.farjad.driftlist.data.ClockStyle
import dev.farjad.driftlist.data.Fonts
import dev.farjad.driftlist.data.Look
import dev.farjad.driftlist.data.ReadyTheme
import dev.farjad.driftlist.data.ReadyThemes
import dev.farjad.driftlist.data.ThemeMode
import dev.farjad.driftlist.data.db.ThemeEntity
import dev.farjad.driftlist.domain.Palette
import dev.farjad.driftlist.ui.components.AppIconPlaceholder
import dev.farjad.driftlist.ui.components.ClockHeader
import dev.farjad.driftlist.ui.theme.DriftlistTheme
import dev.farjad.driftlist.ui.theme.HomeColorsForDarkWallpaper
import dev.farjad.driftlist.ui.theme.LocalHomeColors
import dev.farjad.driftlist.ui.theme.RadiusTokens
import dev.farjad.driftlist.ui.theme.SizeTokens
import dev.farjad.driftlist.ui.theme.SpaceTokens
import dev.farjad.driftlist.ui.theme.TypeTokens
import dev.farjad.driftlist.ui.theme.fontFamilyFor
import dev.farjad.driftlist.ui.theme.onWallpaper
import dev.farjad.driftlist.ui.theme.style
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// --- S18 themes hub ---------------------------------------------------------------------------

@Composable
internal fun LookHubPage(vm: SettingsViewModel, state: SettingsUiState, padding: PaddingValues, onNavigate: (Page) -> Unit) {
    val look = state.settings.look
    val packLabel = remember(state.settings.iconPack) {
        state.settings.iconPack?.let { pkg -> vm.iconPacks().firstOrNull { it.packageName == pkg }?.label ?: pkg }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item { SectionHeader("Ready-made themes") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = SpaceTokens.s7),
                horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s4),
            ) {
                items(ReadyThemes.all, key = { it.id }) { theme ->
                    ThemeCard(theme, state.wallpaperSeed, selected = theme.look == look) { vm.setLook(theme.look) }
                }
            }
        }
        item { Spacer(Modifier.height(SpaceTokens.s4)) }
        item { NavRow("Theme Studio", look.summary(Fonts::label)) { onNavigate(Page.Studio) } }
        item {
            val n = state.themes.size
            NavRow("My themes", if (n == 0) "None saved yet" else "$n saved") { onNavigate(Page.MyThemes) }
        }
        item { NavRow("Icon pack", packLabel ?: "System icons") { onNavigate(Page.IconPack) } }
    }
}

@Composable
private fun ThemeCard(theme: ReadyTheme, wallpaperSeed: Int?, selected: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val family = remember(theme.look.font) { fontFamilyFor(context, theme.look.font) }
    Surface(
        shape = RoundedCornerShape(RadiusTokens.lg),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) BorderStroke(SpaceTokens.s1, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .width(SizeTokens.themeCardWidth)
            .height(SizeTokens.themeCardHeight)
            .clip(RoundedCornerShape(RadiusTokens.lg))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = "${theme.name}. ${theme.description}" },
    ) {
        Column(Modifier.padding(SpaceTokens.s4), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(SizeTokens.eventDot * 2)
                        .clip(CircleShape)
                        .background(swatchColor(theme.look.accent ?: wallpaperSeed)),
                )
                Spacer(Modifier.weight(1f))
                if (selected) Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                "12:30",
                style = TypeTokens.title.style(MaterialTheme.colorScheme.onSurface).copy(
                    fontFamily = if (theme.look.clockStyle == ClockStyle.Mono) FontFamily.Monospace else family,
                ),
            )
            Text(theme.name, style = TypeTokens.label.style(MaterialTheme.colorScheme.onSurface), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// --- S19 Theme Studio -------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ThemeStudioPage(vm: SettingsViewModel, state: SettingsUiState, padding: PaddingValues, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val look = state.settings.look
    var imported by remember { mutableStateOf(vm.importedFonts()) }
    var naming by remember { mutableStateOf(false) }
    val pickFont = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val r = vm.importFont(context, uri)
            imported = vm.importedFonts()
            r.onSuccess { id -> vm.setLook(look.copy(font = id)) }
            snackbar.showSnackbar(r.fold({ "Font added" }, { it.message ?: "That font couldn't be added" }))
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item {
            LookPreview(look, state.wallpaperSeed, Modifier.padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s3))
        }
        item {
            Text(
                "Changes apply straight away and are kept. Save the look to My themes to come back to it later.",
                style = TypeTokens.bodySm.style(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(horizontal = SpaceTokens.s7),
            )
        }

        item { SectionHeader("Clock") }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = SpaceTokens.s7),
                horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s3),
            ) {
                ClockStyle.entries.forEach { style ->
                    FilterChip(
                        selected = look.clockStyle == style,
                        onClick = { vm.setLook(look.copy(clockStyle = style)) },
                        label = { Text(style.label) },
                        modifier = Modifier.heightIn(min = SizeTokens.touchMin).semantics { stateDescription = style.description },
                    )
                }
            }
        }

        item { SectionHeader("Font") }
        val fonts = Fonts.system + imported.map { it to Fonts.label(it) }
        items(fonts, key = { "font:" + it.first }) { (id, label) ->
            FontRow(id, label, selected = look.font == id, removable = id.startsWith(Fonts.FILE_PREFIX), onRemove = {
                vm.deleteFont(context, id)
                imported = vm.importedFonts()
                if (look.font == id) vm.setLook(look.copy(font = Fonts.DEFAULT))
            }) { vm.setLook(look.copy(font = id)) }
        }
        item {
            NavRow("Add a font file", "A .ttf or .otf from your files. It stays on this phone") {
                pickFont.launch(arrayOf("font/ttf", "font/otf", "font/sfnt", "application/x-font-ttf", "application/x-font-otf", "application/octet-stream"))
            }
        }

        item { SectionHeader("Text size on home") }
        item {
            Column(Modifier.padding(horizontal = SpaceTokens.s7)) {
                Text("${(look.fontScale * 100).roundToInt()}%", style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurface))
                Slider(
                    value = look.fontScale,
                    onValueChange = { vm.setLook(look.copy(fontScale = (it * 20).roundToInt() / 20f)) },
                    valueRange = SizeTokens.fontScaleMin..SizeTokens.fontScaleMax,
                    steps = ((SizeTokens.fontScaleMax - SizeTokens.fontScaleMin) * 20).roundToInt() - 1,
                    modifier = Modifier.semantics { contentDescription = "Text size on home" },
                )
            }
        }

        item { SectionHeader("Accent colour") }
        item {
            FlowRow(
                Modifier.padding(horizontal = SpaceTokens.s7),
                horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s2),
            ) {
                Swatch("From wallpaper", swatchColor(state.wallpaperSeed), look.accent == null, auto = true) { vm.setLook(look.copy(accent = null)) }
                Accents.presets.forEach { (name, argb) ->
                    Swatch(name, swatchColor(argb), look.accent == argb) { vm.setLook(look.copy(accent = argb)) }
                }
            }
        }

        item { SectionHeader("Light or dark") }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = SpaceTokens.s7)) {
                ThemeMode.entries.forEachIndexed { i, mode ->
                    SegmentedButton(
                        selected = look.mode == mode,
                        onClick = { vm.setLook(look.copy(mode = mode)) },
                        shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                    ) { Text(mode.label, maxLines = 1) }
                }
            }
        }
        item {
            Text(
                "Light or dark changes sheets and settings. Text on home always follows your wallpaper so it stays readable.",
                style = TypeTokens.caption.style(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s3),
            )
        }

        item {
            Button(onClick = { naming = true }, modifier = Modifier.padding(SpaceTokens.s7)) { Text("Save to My themes") }
        }
    }

    if (naming) {
        NameDialog(
            title = "Save this look",
            initial = "My theme ${state.themes.size + 1}",
            confirm = "Save",
            onConfirm = { name ->
                naming = false
                vm.saveTheme(name)
                scope.launch { snackbar.showSnackbar("Saved to My themes") }
            },
            onDismiss = { naming = false },
        )
    }
}

@Composable
private fun FontRow(id: String, label: String, selected: Boolean, removable: Boolean, onRemove: () -> Unit, onClick: () -> Unit) {
    val context = LocalContext.current
    val family = remember(id) { fontFamilyFor(context, id) }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = SizeTokens.settingsRow)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = SpaceTokens.s7),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.RadioButton(selected = selected, onClick = null)
        Text(
            label,
            style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurface).copy(fontFamily = family),
            modifier = Modifier.weight(1f).padding(start = SpaceTokens.s4),
        )
        if (removable) TextButton(onClick = onRemove) { Text("Remove") }
    }
}

@Composable
private fun Swatch(name: String, color: Color, selected: Boolean, auto: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .size(SizeTokens.touchMin)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(SizeTokens.swatch)
                .clip(CircleShape)
                .background(color)
                .then(if (selected) Modifier.border(SpaceTokens.s1 * 1.5f, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            when {
                selected -> Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White)
                auto -> Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color.White)
            }
        }
    }
}

/** A scaled-down home in the look being edited, on a backdrop made from the accent seed. */
@Composable
internal fun LookPreview(look: Look, wallpaperSeed: Int?, modifier: Modifier = Modifier) {
    val seed = look.accent ?: wallpaperSeed ?: DEFAULT_SEED
    val (h, s, _) = Palette.hsl(seed)
    val top = Color(Palette.fromHsl(h, s * 0.6f, 0.22f))
    val bottom = Color(Palette.fromHsl(h, s * 0.5f, 0.07f))
    DriftlistTheme(look, wallpaperSeed) {
        CompositionLocalProvider(LocalHomeColors provides HomeColorsForDarkWallpaper) {
            val colors = LocalHomeColors.current
            Box(
                modifier
                    .fillMaxWidth()
                    .height(SizeTokens.lookPreviewHeight)
                    .clip(RoundedCornerShape(RadiusTokens.lg))
                    .background(Brush.verticalGradient(listOf(top, bottom)))
                    .clearAndSetSemantics { contentDescription = "Preview: ${look.summary(Fonts::label)}" }
                    .padding(SpaceTokens.s7),
            ) {
                Column {
                    ClockHeader(onClockClick = {}, onDateClick = {}, onLongClick = {})
                    Spacer(Modifier.height(SpaceTokens.s4))
                    listOf("Messages", "Camera").forEach { name ->
                        Row(
                            Modifier.heightIn(min = SizeTokens.favoriteRow * look.fontScale),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s5),
                        ) {
                            AppIconPlaceholder(MaterialTheme.colorScheme.primary)
                            Text(name, style = TypeTokens.favorite.onWallpaper(colors))
                        }
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(RadiusTokens.pill),
                    modifier = Modifier.align(Alignment.BottomEnd),
                ) {
                    Text(
                        "Accent",
                        style = TypeTokens.label.style(MaterialTheme.colorScheme.onPrimary),
                        modifier = Modifier.padding(horizontal = SpaceTokens.s5, vertical = SpaceTokens.s3),
                    )
                }
            }
        }
    }
}

// --- My themes ----------------------------------------------------------------------------------

@Composable
internal fun MyThemesPage(vm: SettingsViewModel, state: SettingsUiState, padding: PaddingValues, snackbar: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    var renaming by remember { mutableStateOf<ThemeEntity?>(null) }
    var deleting by remember { mutableStateOf<ThemeEntity?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        if (state.themes.isEmpty()) {
            item {
                Text(
                    "Looks you save in Theme Studio show up here, ready to switch back to.",
                    style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.padding(SpaceTokens.s7),
                )
            }
        }
        items(state.themes, key = { it.id }) { theme ->
            val look = remember(theme.look) { Look.fromJson(theme.look) }
            val current = look == state.settings.look
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = SizeTokens.settingsRow)
                    .selectable(selected = current, role = Role.RadioButton) {
                        vm.applyTheme(theme)
                        scope.launch { snackbar.showSnackbar("${theme.name} applied") }
                    }
                    .padding(start = SpaceTokens.s7, end = SpaceTokens.s3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s5),
            ) {
                Box(Modifier.size(SizeTokens.swatch).clip(CircleShape).background(swatchColor(look.accent ?: state.wallpaperSeed)), contentAlignment = Alignment.Center) {
                    if (current) Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(theme.name, style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurface), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(look.summary(Fonts::label), style = TypeTokens.caption.style(MaterialTheme.colorScheme.onSurfaceVariant))
                }
                TextButton(onClick = { renaming = theme }) { Text("Rename") }
                TextButton(onClick = { deleting = theme }) { Text("Delete") }
            }
        }
    }
    renaming?.let { theme ->
        NameDialog("Rename theme", theme.name, "Rename", onConfirm = { vm.renameTheme(theme, it); renaming = null }, onDismiss = { renaming = null })
    }
    deleting?.let { theme ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${theme.name}?") },
            text = { Text("Your current look stays as it is.") },
            confirmButton = { TextButton(onClick = { vm.deleteTheme(theme); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun NameDialog(title: String, initial: String, confirm: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, singleLine = true, label = { Text("Name") })
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name.trim()) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun swatchColor(seed: Int?): Color =
    if (seed == null) MaterialTheme.colorScheme.primary else Color(seed).copy(alpha = 1f)

private const val DEFAULT_SEED = 0xFF3B5BCC.toInt()
