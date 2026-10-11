package dev.farjad.driftlist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import dev.farjad.driftlist.domain.Calculator
import dev.farjad.driftlist.system.search.ContactHit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import android.appwidget.AppWidgetHostView
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.farjad.driftlist.data.db.AppEntity
import dev.farjad.driftlist.system.apps.IconCache
import dev.farjad.driftlist.system.widgets.WidgetHostManager
import dev.farjad.driftlist.ui.theme.RadiusTokens
import dev.farjad.driftlist.ui.theme.SizeTokens
import dev.farjad.driftlist.ui.theme.SpaceTokens
import dev.farjad.driftlist.ui.theme.TypeTokens
import dev.farjad.driftlist.ui.theme.style

/** S15: full-screen search. Enter opens the top result. */
@Composable
fun SearchOverlay(
    search: (String) -> List<AppEntity>,
    suggestions: List<AppEntity>,
    icons: IconCache,
    onLaunch: (AppEntity) -> Unit,
    onLongPress: (AppEntity) -> Unit,
    onWebSearch: (String) -> Unit,
    onPlaySearch: (String) -> Unit,
    onClose: () -> Unit,
    engineLabel: String? = null,
    webSuggestions: suspend (String) -> List<String> = { emptyList() },
    contacts: (String) -> List<ContactHit> = { emptyList() },
    contactsNeedPermission: Boolean = false,
    onAllowContacts: () -> Unit = {},
    onOpenContact: (ContactHit) -> Unit = {},
    onCallContact: (ContactHit) -> Unit = {},
    onMessageContact: (ContactHit) -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query) { if (query.isBlank()) emptyList() else search(query) }
    val sum = remember(query) { Calculator.evaluate(query)?.let(Calculator::format) }
    var people by remember { mutableStateOf(emptyList<ContactHit>()) }
    var suggested by remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(query) {
        people = if (query.isBlank()) emptyList() else withContext(Dispatchers.IO) { contacts(query) }
        suggested = emptyList()
        if (query.isNotBlank() && sum == null) {
            delay(SUGGEST_DEBOUNCE_MS)
            suggested = webSuggestions(query)
        }
    }
    val clipboard = LocalClipboardManager.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BackHandler(onBack = onClose)

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search apps") },
                singleLine = true,
                shape = RoundedCornerShape(RadiusTokens.pill),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    results.firstOrNull()?.let(onLaunch) ?: if (query.isNotBlank()) onWebSearch(query) else Unit
                }),
                trailingIcon = {
                    if (query.isNotEmpty()) TextButton(onClick = { query = "" }) { Text("Clear") }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpaceTokens.s5, vertical = SpaceTokens.s4)
                    .focusRequester(focus),
            )
            LazyColumn(Modifier.weight(1f)) {
                if (query.isBlank()) {
                    if (suggestions.isNotEmpty()) {
                        item { SectionLabel("Most used") }
                        items(suggestions, key = { "s:" + it.id }) { app -> SheetAppRow(app, icons, onClick = { onLaunch(app) }) }
                    }
                } else {
                    if (sum != null) {
                        item(key = "calc") {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s3)
                                    .semantics(mergeDescendants = true) { contentDescription = "$query equals $sum" },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("= $sum", style = TypeTokens.title.style(MaterialTheme.colorScheme.onSurface), modifier = Modifier.weight(1f))
                                TextButton(onClick = { clipboard.setText(AnnotatedString(sum)) }) { Text("Copy") }
                            }
                        }
                    }
                    items(results, key = { it.id }) { app ->
                        val top = app.id == results.firstOrNull()?.id
                        SheetAppRow(app, icons, onClick = { onLaunch(app) }, supporting = if (top) "Enter opens this" else null) {
                            if (app.hidden) Text("Hidden", style = TypeTokens.caption.style(MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }
                    if (contactsNeedPermission) {
                        item(key = "contacts-allow") {
                            SheetAppRowLike("Search your contacts too", "Allow contacts access", onAllowContacts)
                        }
                    }
                    if (people.isNotEmpty()) {
                        item { SectionLabel("Contacts") }
                        items(people, key = { "c:" + it.contactId }) { hit ->
                            ContactRow(hit, onOpen = { onOpenContact(hit) }, onCall = { onCallContact(hit) }, onMessage = { onMessageContact(hit) })
                        }
                    }
                    if (suggested.isNotEmpty()) {
                        item { SectionLabel("Web") }
                        items(suggested, key = { "w:$it" }) { s ->
                            SheetAppRowLike(s, null) { onWebSearch(s) }
                        }
                    }
                    item {
                        Column(Modifier.padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s4)) {
                            if (results.isEmpty() && sum == null && people.isEmpty()) {
                                Text(
                                    "No apps match “$query”.",
                                    style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurfaceVariant),
                                )
                            }
                            TextButton(onClick = { onWebSearch(query) }) {
                                Text(if (engineLabel == null) "Search the web for “$query”" else "Search $engineLabel for “$query”")
                            }
                            TextButton(onClick = { onPlaySearch(query) }) { Text("Find “$query” on Google Play") }
                        }
                    }
                }
            }
        }
    }
}

/** A plain text row with the same height and padding as an app row. */
@Composable
private fun SheetAppRowLike(title: String, supporting: String?, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = SizeTokens.sheetRow)
            .clickable(onClick = onClick)
            .padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s3),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurface), maxLines = 1, overflow = TextOverflow.Ellipsis)
        supporting?.let { Text(it, style = TypeTokens.caption.style(MaterialTheme.colorScheme.primary)) }
    }
}

@Composable
private fun ContactRow(hit: ContactHit, onOpen: () -> Unit, onCall: () -> Unit, onMessage: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = SizeTokens.sheetRow)
            .clickable(onClickLabel = "Open contact", onClick = onOpen)
            .padding(start = SpaceTokens.s7, end = SpaceTokens.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s4),
    ) {
        Box(
            Modifier.size(SizeTokens.iconPopup).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(hit.name.take(1).uppercase(), style = TypeTokens.label.style(MaterialTheme.colorScheme.onPrimaryContainer))
        }
        Column(Modifier.weight(1f)) {
            Text(hit.name, style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurface), maxLines = 1, overflow = TextOverflow.Ellipsis)
            hit.number?.let { Text(it, style = TypeTokens.caption.style(MaterialTheme.colorScheme.onSurfaceVariant)) }
        }
        if (hit.number != null) {
            IconButton(onClick = onCall) { Icon(Icons.Outlined.Call, contentDescription = "Call ${hit.name}") }
            IconButton(onClick = onMessage) { Icon(Icons.Outlined.Sms, contentDescription = "Message ${hit.name}") }
        }
    }
}

private const val SUGGEST_DEBOUNCE_MS = 250L

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = TypeTokens.letterHeader.style(MaterialTheme.colorScheme.primary),
        modifier = Modifier.padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s3).semantics { heading() },
    )
}

/** S01 + S02: first run. A short welcome, then choose favorites. */
@Composable
fun Onboarding(
    apps: List<AppEntity>,
    icons: IconCache,
    onDone: (List<String>) -> Unit,
) {
    var step by rememberSaveable { mutableStateOf(0) }
    val chosen = remember { mutableStateListOf<String>() }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            if (step == 0) {
                Spacer(Modifier.weight(1f))
                Column(Modifier.padding(horizontal = SpaceTokens.s8)) {
                    Text("Driftlist", style = TypeTokens.clockSmall.style(MaterialTheme.colorScheme.onBackground))
                    Spacer(Modifier.height(SpaceTokens.s4))
                    Text(
                        "A quieter home screen. Your favorite apps sit within thumb reach, everything else is one slide along the alphabet away.",
                        style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                    Spacer(Modifier.height(SpaceTokens.s4))
                    Text(
                        "No accounts, no ads, nothing leaves this phone.",
                        style = TypeTokens.bodySm.style(MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { step = 1 },
                    modifier = Modifier.align(Alignment.End).padding(SpaceTokens.s7),
                ) { Text("Get started") }
            } else {
                Text(
                    "Pick your favorites",
                    style = TypeTokens.title.style(MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier.padding(start = SpaceTokens.s7, end = SpaceTokens.s7, top = SpaceTokens.s7).semantics { heading() },
                )
                Text(
                    "They sit on your home screen, under the clock. Four to seven works well. You can change them any time.",
                    style = TypeTokens.bodySm.style(MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s2),
                )
                AppMultiPicker(apps, icons, chosen.toSet()) { app ->
                    if (app.id in chosen) chosen.remove(app.id) else chosen.add(app.id)
                }
                Row(
                    Modifier.fillMaxWidth().padding(SpaceTokens.s5),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { onDone(emptyList()) }) { Text("Skip") }
                    Button(onClick = { onDone(chosen.toList()) }, enabled = chosen.isNotEmpty()) {
                        Text(if (chosen.isEmpty()) "Done" else "Done (${chosen.size})")
                    }
                }
            }
        }
    }
}

/** A hosted third-party widget, sized to the provider's minimum height. */
@Composable
fun HostedWidget(
    appWidgetId: Int,
    host: WidgetHostManager,
    modifier: Modifier = Modifier,
) {
    val info = remember(appWidgetId) { host.info(appWidgetId) }
    if (info == null) {
        Text(
            "This widget stopped working. Long-press the clock to remove or replace it.",
            style = TypeTokens.bodySm.style(MaterialTheme.colorScheme.error),
            modifier = modifier.padding(vertical = SpaceTokens.s3),
        )
        return
    }
    val density = LocalDensity.current
    val heightDp = with(density) { maxOf(info.minHeight, info.minResizeHeight).toDp() }.coerceIn(48.dp, 320.dp)
    AndroidView(
        factory = { ctx ->
            host.createView(ctx, appWidgetId).apply {
                // Widgets lay themselves out from the size the host reports. Report it on every
                // layout change, or they render at 0x0 (blank).
                addOnLayoutChangeListener { v, l, t, r, b, oldL, oldT, oldR, oldB ->
                    if (r - l != oldR - oldL || b - t != oldB - oldT) reportSize(v as AppWidgetHostView)
                }
            }
        },
        modifier = modifier.fillMaxWidth().height(heightDp),
    )
}

private fun reportSize(view: AppWidgetHostView) {
    val d = view.resources.displayMetrics.density
    val w = view.width / d
    val h = view.height / d
    if (w <= 0f || h <= 0f) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        view.updateAppWidgetSize(Bundle(), listOf(SizeF(w, h)))
    } else {
        @Suppress("DEPRECATION")
        view.updateAppWidgetSize(Bundle(), w.toInt(), h.toInt(), w.toInt(), h.toInt())
    }
}
