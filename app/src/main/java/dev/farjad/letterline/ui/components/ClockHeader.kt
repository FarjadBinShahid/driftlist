package dev.farjad.letterline.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.farjad.letterline.data.ClockStyle
import dev.farjad.letterline.ui.theme.LocalClockStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import dev.farjad.letterline.domain.Agenda
import dev.farjad.letterline.domain.CalEvent
import dev.farjad.letterline.ui.theme.SpaceTokens
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
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.farjad.letterline.ui.theme.LocalHomeColors
import dev.farjad.letterline.ui.theme.SizeTokens
import dev.farjad.letterline.ui.theme.TypeTokens
import dev.farjad.letterline.ui.theme.onWallpaper
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
        .trimBelowBaseline(SpaceTokens.s2)
        .combinedClickable(onClick = onClockClick, onLongClick = onLongClick)
        .semantics(mergeDescendants = true) { contentDescription = "$time, open clock. Long-press for header options" }
    val dateText: @Composable () -> Unit = {
        Text(
            date,
            style = TypeTokens.date.onWallpaper(colors),
            // Sits right under the time, as on the reference: no 48dp box around it, just a little
            // padding, so the header reads as one block.
            modifier = Modifier
                .combinedClickable(onClick = onDateClick, onLongClick = onLongClick)
                .padding(vertical = SpaceTokens.s1)
                .semantics { contentDescription = if (dateOpensAgenda) "$date, show the week ahead" else "$date, open calendar" },
        )
    }

    // Big clock digits carry a font's full ascent and descent; trimming the line box keeps the
    // date tucked under the digits instead of a line-height away.
    val trim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
    Column(modifier) {
        when (clockStyle) {
            ClockStyle.Thin -> Text(time, style = TypeTokens.clock.onWallpaper(colors).copy(lineHeightStyle = trim), modifier = clockModifier)
            ClockStyle.Bold -> Text(time, style = TypeTokens.clockBold.onWallpaper(colors).copy(lineHeightStyle = trim), modifier = clockModifier)
            ClockStyle.Mono -> Text(
                time,
                style = TypeTokens.clockMono.onWallpaper(colors).copy(fontFamily = FontFamily.Monospace, lineHeightStyle = trim),
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
                    .padding(top = SpaceTokens.s2)
                    .combinedClickable(onClick = onDateClick, onLongClick = onLongClick)
                    .padding(vertical = SpaceTokens.s2)
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

/**
 * Ends the layout a little under the last baseline. Big clock digits have no descenders, so the
 * font's descent is just empty space between the time and the date.
 */
private fun Modifier.trimBelowBaseline(gap: Dp) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val baseline = placeable[LastBaseline]
    val height = if (baseline == AlignmentLine.Unspecified) placeable.height else minOf(placeable.height, baseline + gap.roundToPx())
    layout(placeable.width, height) { placeable.place(0, 0) }
}

