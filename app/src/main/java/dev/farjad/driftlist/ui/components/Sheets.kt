package dev.farjad.driftlist.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.farjad.driftlist.data.db.AppEntity
import dev.farjad.driftlist.system.apps.IconCache
import dev.farjad.driftlist.system.notifications.NotifItem
import dev.farjad.driftlist.ui.theme.RadiusTokens
import dev.farjad.driftlist.ui.theme.SizeTokens
import dev.farjad.driftlist.ui.theme.SpaceTokens
import dev.farjad.driftlist.ui.theme.TypeTokens
import dev.farjad.driftlist.ui.theme.style

@Composable
private fun SheetHeader(app: AppEntity, icons: IconCache) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s5),
    ) {
        AppIcon(app.id, icons, SizeTokens.iconSheet)
        Text(
            app.label,
            style = TypeTokens.title.style(MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.semantics { heading() },
        )
    }
}

/** S07: the app pop-up, opened by swiping right on an app. Milestone 1 shows notifications only. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPopupSheet(
    app: AppEntity,
    icons: IconCache,
    notifications: List<NotifItem>,
    onOpenApp: () -> Unit,
    onOpenNotification: (NotifItem) -> Unit,
    onDismissNotification: (NotifItem) -> Unit,
    onClose: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = RadiusTokens.sheet, topEnd = RadiusTokens.sheet),
    ) {
        Column(Modifier.navigationBarsPadding()) {
            SheetHeader(app, icons)
            if (notifications.isEmpty()) {
                Text(
                    "Nothing new here.",
                    style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s4),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(SpaceTokens.s3),
                    contentPadding = PaddingValues(horizontal = SpaceTokens.s5),
                ) {
                    items(notifications, key = { it.key }) { item ->
                        NotificationCard(
                            item = item,
                            onOpen = { onOpenNotification(item) },
                            onDismiss = { onDismissNotification(item) },
                        )
                    }
                }
            }
            TextButton(
                onClick = onOpenApp,
                modifier = Modifier.align(Alignment.End).padding(SpaceTokens.s4),
            ) { Text("Open ${app.label}") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationCard(item: NotifItem, onOpen: () -> Unit, onDismiss: () -> Unit) {
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) onDismiss()
    }
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
                    Text(it, style = TypeTokens.bodySm.style(MaterialTheme.colorScheme.onSurfaceVariant), maxLines = 4, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** S08: long-press options for one app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppOptionsSheet(
    app: AppEntity,
    icons: IconCache,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onClose: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        shape = RoundedCornerShape(topStart = RadiusTokens.sheet, topEnd = RadiusTokens.sheet),
    ) {
        Column(Modifier.navigationBarsPadding().padding(bottom = SpaceTokens.s4)) {
            SheetHeader(app, icons)
            OptionRow(if (isFavorite) "Remove from favorites" else "Add to favorites", onToggleFavorite)
            OptionRow("App info", onAppInfo)
            if (!app.isSystem) OptionRow("Uninstall", onUninstall)
        }
    }
}

@Composable
private fun OptionRow(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurface),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SizeTokens.touchMin)
            .clickable(onClick = onClick)
            .padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s4),
    )
}

/** Inline card on home asking for a missing setup step (S03, S04). */
@Composable
fun SetupCard(title: String, body: String, action: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
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

