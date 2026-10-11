package dev.farjad.driftlist.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.farjad.driftlist.domain.CalEvent
import dev.farjad.driftlist.ui.components.AgendaSheet
import dev.farjad.driftlist.ui.theme.DriftlistTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** F16 (calendar part): the agenda sheet lists the week, opens events, and has an empty state. */
@RunWith(AndroidJUnit4::class)
class AgendaSheetTest {
    @get:Rule
    val rule = createComposeRule()

    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.now(zone)

    private fun at(day: LocalDate, h: Int) = day.atTime(h, 0).atZone(zone).toInstant().toEpochMilli()

    private fun show(events: List<CalEvent>, onEvent: (CalEvent) -> Unit = {}) {
        rule.enableAccessibilityChecks()
        rule.setContent {
            DriftlistTheme {
                AgendaSheet(events, is24h = true, onEvent = onEvent, onNewEvent = {}, onOpenCalendar = {}, onClose = {})
            }
        }
    }

    @Test
    fun listsTodayAndTomorrowAndOpensAnEvent() {
        val dentist = CalEvent(1, 10, "Dentist", at(today.plusDays(1), 15), at(today.plusDays(1), 16), false, 0xFF3366CC.toInt(), 1, "Main St")
        val holiday = CalEvent(
            2, 11, "Holiday",
            today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            true, 0, 1,
        )
        var opened: CalEvent? = null
        show(listOf(dentist, holiday)) { opened = it }

        rule.onNodeWithText("Today").assertIsDisplayed()
        rule.onNodeWithText("Tomorrow").assertIsDisplayed()
        rule.onNodeWithText("All day").assertIsDisplayed()
        rule.onNodeWithText("15:00 – 16:00 · Main St").assertIsDisplayed()
        rule.onNodeWithText("Dentist").performClick()
        assertEquals(10L, opened?.eventId)
    }

    @Test
    fun emptyWeekSaysSo() {
        show(emptyList())
        rule.onNodeWithText("Nothing on your calendar for the next week.").assertIsDisplayed()
        rule.onNodeWithText("New event").assertIsDisplayed()
    }
}
