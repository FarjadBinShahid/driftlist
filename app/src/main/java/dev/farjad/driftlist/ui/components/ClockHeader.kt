package dev.farjad.driftlist.ui.components

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.farjad.driftlist.data.ClockStyle
import dev.farjad.driftlist.ui.theme.LocalClockStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.style.TextOverflow
import dev.farjad.driftlist.domain.Agenda
import dev.farjad.driftlist.domain.CalEvent
import dev.farjad.driftlist.ui.theme.SpaceTokens
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
    upcoming: List<CalEvent> = emptyList(),
    dateOpensAgenda: Boolean = false,
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

    val clockStyle = LocalClockStyle.current
    val clockModifier = Modifier
        .combinedClickable(onClick = onClockClick, onLongClick = onLongClick)
        .semantics(mergeDescendants = true) { contentDescription = "$time, open clock. Long-press for header options" }
    val dateText: @Composable () -> Unit = {
        Text(
            date,
            style = TypeTokens.date.onWallpaper(colors),
            modifier = Modifier
                .heightIn(min = SizeTokens.touchMin)
                .wrapContentHeight(Alignment.CenterVertically)
                .combinedClickable(onClick = onDateClick, onLongClick = onLongClick)
                .semantics { contentDescription = if (dateOpensAgenda) "$date, show the week ahead" else "$date, open calendar" },
        )
    }

    Column(modifier) {
        when (clockStyle) {
            ClockStyle.Thin -> Text(time, style = TypeTokens.clock.onWallpaper(colors), modifier = clockModifier)
            ClockStyle.Bold -> Text(time, style = TypeTokens.clockBold.onWallpaper(colors), modifier = clockModifier)
            ClockStyle.Mono -> Text(
                time,
                style = TypeTokens.clockMono.onWallpaper(colors).copy(fontFamily = FontFamily.Monospace),
                modifier = clockModifier,
            )
            ClockStyle.Stacked -> {
                val (hours, minutes) = time.split(':').let { it[0].padStart(2, '0') to it.getOrElse(1) { "" } }
                Column(clockModifier) {
                    Text(hours, style = TypeTokens.clockStacked.onWallpaper(colors).copy(fontWeight = FontWeight.Bold))
                    Text(minutes, style = TypeTokens.clockStacked.onWallpaper(colors, colors.textMuted))
                }
            }
            ClockStyle.Compact -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s4)) {
                Text(time, style = TypeTokens.clockSmall.onWallpaper(colors), modifier = clockModifier)
                dateText()
            }
        }
        if (clockStyle != ClockStyle.Compact) dateText()
        val next = remember(upcoming, now) { Agenda.nextUp(upcoming, now.toInstant().toEpochMilli(), now.zone) }
        if (next != null) {
            val line = Agenda.headerLine(next, now.toInstant().toEpochMilli(), now.zone, DateFormat.is24HourFormat(context), locale)
            Row(
                Modifier
                    .heightIn(min = SizeTokens.touchMin)
                    .combinedClickable(onClick = onDateClick, onLongClick = onLongClick)
                    .semantics(mergeDescendants = true) { contentDescription = "Next: $line. Show the week ahead" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s3),
            ) {
                EventDot(next)
                Text(
                    line,
                    style = TypeTokens.bodySm.onWallpaper(colors),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
