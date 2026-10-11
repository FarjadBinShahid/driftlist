package dev.farjad.driftlist.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.farjad.driftlist.ui.theme.LocalHomeColors
import dev.farjad.driftlist.ui.theme.SizeTokens
import dev.farjad.driftlist.ui.theme.TypeTokens
import dev.farjad.driftlist.ui.theme.onWallpaper
import kotlinx.coroutines.delay
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClockHeader(
    onClockClick: () -> Unit,
    onDateClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = LocalHomeColors.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var now by remember { mutableStateOf(ZonedDateTime.now()) }

    // Tick on the minute, and re-read the time whenever home comes back to the foreground.
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now = ZonedDateTime.now()
                delay(60_000L - System.currentTimeMillis() % 60_000L)
            }
        }
    }

    val locale = Locale.getDefault()
    val time = now.format(DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm", locale))
    val date = now.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale))

    Column(modifier) {
        Text(
            time,
            style = TypeTokens.clock.onWallpaper(colors),
            modifier = Modifier
                .combinedClickable(onClick = onClockClick, onLongClick = onLongClick)
                .semantics { contentDescription = "$time, open clock. Long-press for header options" },
        )
        Text(
            date,
            style = TypeTokens.date.onWallpaper(colors),
            modifier = Modifier
                .heightIn(min = SizeTokens.touchMin)
                .wrapContentHeight(Alignment.CenterVertically)
                .combinedClickable(onClick = onDateClick, onLongClick = onLongClick)
                .semantics { contentDescription = "$date, open calendar" },
        )
    }
}
