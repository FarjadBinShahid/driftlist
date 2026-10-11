package dev.farjad.driftlist.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.farjad.driftlist.ui.theme.SpaceTokens
import dev.farjad.driftlist.ui.theme.TypeTokens
import dev.farjad.driftlist.ui.theme.style

/** What a permission is for, and the exact steps when Android hides the switch. */
data class PermissionExplainer(
    val title: String,
    val body: String,
    val steps: List<String> = emptyList(),
    val confirm: String = "Open settings",
)

/** S29: explain a permission before sending the user to the system page for it. */
@Composable
fun PermissionDialog(explainer: PermissionExplainer, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(explainer.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(SpaceTokens.s3)) {
                Text(explainer.body, style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurfaceVariant))
                explainer.steps.forEachIndexed { i, step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s3)) {
                        Text("${i + 1}.", style = TypeTokens.label.style(MaterialTheme.colorScheme.primary))
                        Text(step, style = TypeTokens.bodySm.style(MaterialTheme.colorScheme.onSurface), modifier = Modifier.padding(end = SpaceTokens.s2))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(explainer.confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}

object Explainers {
    /** Steps Android 13+ needs for apps installed outside an app store ("restricted settings"). */
    private val restrictedSteps = listOf(
        "If the Driftlist switch is greyed out, go back and open App info for Driftlist.",
        "Tap the three-dot menu at the top right, then Allow restricted settings, and confirm.",
        "Come back here and switch Driftlist on.",
    )

    fun notificationAccess(sideloaded: Boolean) = PermissionExplainer(
        title = "Show notifications next to your apps",
        body = "Previews and quick replies on the home screen need notification access. Android opens a settings page; switch Driftlist on there. Nothing leaves your phone.",
        steps = if (sideloaded) restrictedSteps else emptyList(),
    )

    /** Shown only after Android stops showing its own calendar prompt ("Don't allow" twice). */
    val calendar = PermissionExplainer(
        title = "Show your calendar on home",
        body = "Driftlist reads your calendar to show what's next under the date. Events are read on this phone and never stored or sent anywhere. Android won't ask again by itself, so open App info, then Permissions, then Calendar, and choose Allow.",
        confirm = "Open App info",
    )

    fun quickLock(sideloaded: Boolean) = PermissionExplainer(
        title = "Turn on Quick Lock",
        body = "Android only lets apps turn the screen off through an accessibility service. Driftlist's service reads nothing on screen; it only locks. In the list, open Driftlist Quick Lock and switch it on.",
        steps = if (sideloaded) restrictedSteps else emptyList(),
    )

    fun usageAccess(sideloaded: Boolean) = PermissionExplainer(
        title = "Let Driftlist see which app is open",
        body = "Time checks need usage access to count how long you've been in an app. Android opens a list of apps; pick Driftlist and switch it on. The data stays on this phone.",
        steps = if (sideloaded) restrictedSteps else emptyList(),
    )
}
