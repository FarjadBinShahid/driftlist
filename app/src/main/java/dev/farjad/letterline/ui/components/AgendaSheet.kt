package dev.farjad.letterline.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.farjad.letterline.domain.Agenda
import dev.farjad.letterline.domain.Agenda.displayTitle
import dev.farjad.letterline.domain.CalEvent
import dev.farjad.letterline.ui.theme.RadiusTokens
import dev.farjad.letterline.ui.theme.SizeTokens
import dev.farjad.letterline.ui.theme.SpaceTokens
import dev.farjad.letterline.ui.theme.TypeTokens
import dev.farjad.letterline.ui.theme.style
import java.time.LocalDate
import java.time.ZoneId

/** S14 agenda: the week ahead from the system calendar. Opened by tapping the date. */
@Composable
fun AgendaSheet(
    events: List<CalEvent>,
    is24h: Boolean,
    onEvent: (CalEvent) -> Unit,
    onNewEvent: () -> Unit,
    onOpenCalendar: () -> Unit,
    onClose: () -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val days = remember(events) { Agenda.groupByDay(events, today, AGENDA_DAYS, zone) }

    AppSheet(onClose) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SheetTitle("Coming up", Modifier.weight(1f))
            FilledTonalButton(onClick = onNewEvent, modifier = Modifier.padding(end = SpaceTokens.s5)) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text("New event", modifier = Modifier.padding(start = SpaceTokens.s2))
            }
        }
        if (days.isEmpty()) {
            Text(
                "Nothing on your calendar in the next 7 days.",
                style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s5),
            )
        }
        LazyColumn(Modifier.heightIn(max = SizeTokens.agendaMaxHeight)) {
            for ((day, list) in days) {
                item(key = "d:$day") {
                    Text(
                        Agenda.dayLabel(day, today),
                        style = TypeTokens.label.style(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .padding(start = SpaceTokens.s7, end = SpaceTokens.s7, top = SpaceTokens.s5, bottom = SpaceTokens.s2)
                            .semantics { heading() },
                    )
                }
                items(list, key = { "e:$day:${it.instanceId}" }) { event ->
                    EventRow(event, Agenda.timeRange(event, day, zone, is24h)) { onEvent(event) }
                }
            }
        }
        TextButton(onClick = onOpenCalendar, modifier = Modifier.padding(start = SpaceTokens.s5, top = SpaceTokens.s2)) {
            Text("Open calendar")
        }
        Box(Modifier.height(SpaceTokens.s2))
    }
}

@Composable
private fun EventRow(event: CalEvent, time: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = SizeTokens.sheetRow)
            .clickable(onClickLabel = "Open in calendar", onClick = onClick)
            .padding(horizontal = SpaceTokens.s7, vertical = SpaceTokens.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpaceTokens.s5),
    ) {
        Box(
            Modifier
                .width(SizeTokens.eventBarWidth)
                .height(SizeTokens.eventBarHeight)
                .clip(RoundedCornerShape(RadiusTokens.sm))
                .background(event.swatch()),
        )
        Column(Modifier.weight(1f)) {
            Text(
                event.displayTitle,
                style = TypeTokens.body.style(MaterialTheme.colorScheme.onSurface),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(time, event.location).joinToString(" · "),
                style = TypeTokens.caption.style(MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Small round swatch in the event's calendar colour (used in the header line too). */
@Composable
fun EventDot(event: CalEvent, modifier: Modifier = Modifier) {
    Box(modifier.size(SizeTokens.eventDot).clip(CircleShape).background(event.swatch()))
}

/** The calendar's own colour, made opaque; falls back to the theme's accent when a calendar has none. */
@Composable
private fun CalEvent.swatch(): Color =
    if (color == 0) MaterialTheme.colorScheme.primary else Color(color).copy(alpha = 1f)

private const val AGENDA_DAYS = 7
