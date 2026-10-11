package dev.farjad.driftlist.ui

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.farjad.driftlist.TestState
import dev.farjad.driftlist.data.ClockStyle
import dev.farjad.driftlist.data.Look
import dev.farjad.driftlist.data.ReadyThemes
import dev.farjad.driftlist.ui.settings.SettingsActivity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** F17 themes: ready-made themes, Theme Studio edits, saving to My themes (accessibility checks on). */
@RunWith(AndroidJUnit4::class)
class ThemeFlowsTest {
    private val rule = createEmptyComposeRule()
    private var scenario: ActivityScenario<SettingsActivity>? = null

    private val closer = object : ExternalResource() {
        override fun after() {
            scenario?.close()
            scenario = null
            runBlocking { TestState.container.settings.setLook(Look()) }
        }
    }

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(closer).around(rule)

    private fun settings(block: ComposeTestRule.() -> Unit) {
        TestState.reset()
        runBlocking { TestState.container.settings.setLook(Look()) }
        rule.enableAccessibilityChecks()
        scenario = ActivityScenario.launch(SettingsActivity::class.java)
        rule.waitForIdle()
        rule.onNodeWithText("Look").performClick()
        rule.block()
    }

    /** Lazy lists only compose what's near the screen: scroll the list to the node first. */
    private fun ComposeTestRule.scrollTo(matcher: SemanticsMatcher) =
        onNode(hasScrollToNodeAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).performScrollToNode(matcher)

    private fun look() = runBlocking { TestState.container.settings.current().look }

    private fun ComposeTestRule.waitForLook(predicate: (Look) -> Boolean) =
        waitUntil(timeoutMillis = 5_000) { predicate(look()) }

    @Test
    fun readyMadeThemeAppliesInOneTap() = settings {
        val nightShift = ReadyThemes.all.first { it.id == "night-shift" }
        onNodeWithContentDescription(nightShift.name, substring = true).performClick()
        // It comes with a wallpaper: keep ours, take the rest.
        onNodeWithText("Keep mine").performClick()
        waitForLook { it.sameStyle(nightShift.look) && it.wallpaper == null }
    }

    @Test
    fun studioEditsApplyStraightAwayAndSaveToMyThemes() = settings {
        onNodeWithText("Theme Studio").performClick()
        onNodeWithText("Stacked").performClick()
        waitForLook { it.clockStyle == ClockStyle.Stacked }

        scrollTo(hasText("Serif"))
        onNodeWithText("Serif").performClick()
        waitForLook { it.font == "system:serif" }

        scrollTo(hasContentDescription("Coral"))
        onNodeWithContentDescription("Coral").performClick()
        waitForLook { it.accent == 0xFFE0604F.toInt() }

        scrollTo(hasText("Save to My themes"))
        onNodeWithText("Save to My themes").performClick()
        onNodeWithText("Save").performClick()
        waitUntil(timeoutMillis = 5_000) { runBlocking { TestState.container.db.themeDao().all().size == 1 } }
        val saved = runBlocking { TestState.container.db.themeDao().all().single() }
        assertEquals(look(), Look.fromJson(saved.look))

        // Change the look, then switch back from My themes.
        scrollTo(hasText("Thin"))
        onNodeWithText("Thin").performClick()
        waitForLook { it.clockStyle == ClockStyle.Thin }
        scenario!!.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        onNodeWithText("My themes").performClick()
        onNode(hasText(saved.name)).performClick()
        waitForLook { it.clockStyle == ClockStyle.Stacked }
    }
}
