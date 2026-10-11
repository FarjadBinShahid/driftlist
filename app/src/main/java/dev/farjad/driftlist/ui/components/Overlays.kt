package dev.farjad.driftlist.ui.components

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
    onClose: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query) { if (query.isBlank()) emptyList() else search(query) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BackHandler(onBack = onClose)

    Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.97f), modifier = Modifier.fillMaxSize()) {
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
                    items(results, key = { it.id }) { app ->
                        SheetAppRow(app, icons, onClick = { onLaunch(app) }) {
                            if (app.hidden) Text("Hidden", style = TypeTokens.caption.style(MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }
                    item {
                        Column(Modifier.padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s4)) {
                            if (results.isEmpty()) {
                                Text(
                                    "No apps match “$query”.",
                                    style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurfaceVariant),
                                )
                            }
                            TextButton(onClick = { onWebSearch(query) }) { Text("Search the web for “$query”") }
                        }
                    }
                }
            }
        }
    }
}

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
                    "They sit at the bottom of your home screen. Four to seven works well. You can change them any time.",
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
        factory = { ctx -> host.createView(ctx, appWidgetId) },
        update = { view: AppWidgetHostView ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && view.width > 0) {
                val w = view.width / view.resources.displayMetrics.density
                view.updateAppWidgetSize(Bundle(), listOf(SizeF(w, heightDp.value)))
            }
        },
        modifier = modifier.fillMaxWidth().height(heightDp),
    )
}
