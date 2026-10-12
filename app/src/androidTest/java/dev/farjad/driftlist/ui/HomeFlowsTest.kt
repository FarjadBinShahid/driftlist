package dev.farjad.driftlist.ui

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performSemanticsAction
import dev.farjad.driftlist.domain.AlphabetIndexer
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.farjad.driftlist.MainActivity
import dev.farjad.driftlist.TestState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end flows on the real home screen (see replica/test-plan.md for the case IDs).
 * Runs against the apps that ship on the emulator image. Accessibility checks (ATF) run on
 * every action.
 */
@RunWith(AndroidJUnit4::class)
class HomeFlowsTest {
    private val rule = createEmptyComposeRule()
    private var scenario: ActivityScenario<MainActivity>? = null

    /** Closes the activity only after the Compose rule has finished its own teardown. */
    private val closer = object : ExternalResource() {
        override fun after() {
            scenario?.close()
            scenario = null
        }
    }

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(closer).around(rule)

    /** Launch home after TestState.reset() and run [block]. Relaunching closes the previous activity. */
    private fun home(block: ComposeTestRule.() -> Unit) {
        rule.enableAccessibilityChecks()
        scenario?.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        rule.waitForIdle()
        rule.block()
    }

    private fun pressBack() {
        scenario!!.onActivity { (it as ComponentActivity).onBackPressedDispatcher.onBackPressed() }
    }

    private fun ComposeTestRule.waitForText(text: String, substring: Boolean = false) =
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty() }

    /** Uses the alphabet's accessibility control, the same way TalkBack does. */
    private fun ComposeTestRule.jumpTo(letter: String) {
        val letters = runBlocking {
            val indexer = AlphabetIndexer.forCurrentLocales()
            val buckets = TestState.container.db.appDao().all().filterNot { it.hidden }.map { it.bucket }.toSet()
            indexer.letters.filter { it in buckets }
        }
        onNode(hasContentDescription("Alphabet")).performSemanticsAction(SemanticsActions.SetProgress) {
            it(letters.indexOf(letter).toFloat())
        }
    }

    private fun ComposeTestRule.openSearch() {
        onRoot().performTouchInput { swipeUp(startY = bottom * 0.85f, endY = bottom * 0.25f, durationMillis = 500) }
        waitForText("Search apps")
    }

    private fun ComposeTestRule.searchField() = onNode(hasSetTextAction())

    // F01-H1
    @Test
    fun onboardingPicksFavoritesInOrder() {
        TestState.reset(onboardingDone = false)
        home {
            onNodeWithText("Get started").performClick()
            waitForText("Pick your favorites")
            onNodeWithText("Clock").performClick()
            onNodeWithText("Calendar").performClick()
            onNodeWithText("Done (2)").performClick()
            waitForText("Clock")
        }
        val favorites = runBlocking {
            val c = TestState.container
            val byId = c.db.appDao().all().associateBy { it.id }
            c.db.homeDao().items().map { byId.getValue(it.appId!!).systemLabel }
        }
        assertEquals(listOf("Clock", "Calendar"), favorites)
    }

    // F01-H2
    @Test
    fun skippingOnboardingShowsTheEmptyHint() {
        TestState.reset(onboardingDone = false)
        home {
            onNodeWithText("Get started").performClick()
            onNodeWithText("Skip").performClick()
            waitForText("long-press it to pin it here", substring = true)
        }
    }

    // BUG-011 / F03-E7: in landscape the header moves beside the favorites, so the last one stays on screen
    @Test
    fun landscapeKeepsEveryFavoriteOnScreen() {
        TestState.reset(favorites = listOf("Calendar", "Camera", "Chrome", "Clock", "Contacts", "Files")) { c ->
            c.settings.setFlag("allow_rotation", true)
        }
        val ui = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
        try {
            ui.setRotation(android.app.UiAutomation.ROTATION_FREEZE_90)
            home {
                waitForText("Files")
                onNodeWithText("Files").assertIsDisplayed()
            }
        } finally {
            ui.setRotation(android.app.UiAutomation.ROTATION_FREEZE_0)
            ui.setRotation(android.app.UiAutomation.ROTATION_UNFREEZE)
            runBlocking { TestState.container.settings.setFlag("allow_rotation", false) }
        }
    }

    // F09-H3: apps placed inside an app's pop-up show there, and nowhere on home
    @OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
    @Test
    fun appPopupShowsAppsPlacedInsideIt() {
        TestState.reset(favorites = listOf("Clock")) { c ->
            val clock = TestState.app("Clock")
            val calendar = TestState.app("Calendar")
            val id = dev.farjad.driftlist.ui.home.APP_POPUP_FOLDER + clock.id
            c.db.folderDao().insertFolder(dev.farjad.driftlist.data.db.PopupFolderEntity(id, "Clock", createdAt = 1, updatedAt = 1))
            c.db.folderDao().setApps(id, listOf(calendar.id), 1)
        }
        home {
            waitForText("Clock")
            assertTrue(onAllNodes(hasText("Calendar")).fetchSemanticsNodes().isEmpty())
            onNodeWithText("Clock").performCustomAccessibilityActionWithLabel("Notifications and shortcuts")
            waitForText("Calendar")
            onNodeWithContentDescription("Edit this pop-up").assertIsDisplayed()
        }
    }

    // F2 (fixes.md): leaving and returning to home many times never leaves it frozen or blank
    @Test
    fun homeStaysResponsiveAfterManyReturns() {
        TestState.reset(favorites = listOf("Clock", "Camera"))
        home {
            waitForText("Clock")
            repeat(50) {
                scenario!!.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
                scenario!!.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            }
            waitForText("Camera")
            onNodeWithText("Camera").performTouchInput { longClick() }
            waitForText("App info")
        }
    }

    // F3 + F6 (fixes.md): favorites as an icon grid, and the search pill
    @Test
    fun favoritesGridAndSearchPill() {
        TestState.reset(favorites = listOf("Clock", "Camera", "Chrome")) { c ->
            c.settings.setFavoritesLayout("start", 4)
            c.settings.setFlag("search_pill", true)
        }
        try {
            home {
                waitUntil(timeoutMillis = 5_000) { onAllNodes(hasContentDescription("Chrome")).fetchSemanticsNodes().isNotEmpty() }
                onNodeWithContentDescription("Chrome").performTouchInput { longClick() }
                waitForText("App info")
                pressBack()
                onNodeWithText("Search").performClick()
                waitForText("Search apps")
            }
        } finally {
            runBlocking {
                TestState.container.settings.setFavoritesLayout("start", 0)
                TestState.container.settings.setFlag("search_pill", false)
            }
        }
    }

    // F04-H4: the calculator answers sums typed into search
    @Test
    fun searchDoesSums() {
        TestState.reset()
        home {
            openSearch()
            searchField().performTextInput("12 × (3 + 4)")
            waitForText("= 84")
            onNodeWithText("Copy").assertIsDisplayed()
            searchField().performTextClearance()
            searchField().performTextInput("clock")
            waitForText("Clock")
            assertTrue(onAllNodes(hasText("=", substring = true)).fetchSemanticsNodes().isEmpty())
        }
    }

    // F04-H1, F04-E1, F04-E5
    @Test
    fun searchFindsAppsAndHandlesNoMatches() {
        TestState.reset()
        home {
            openSearch()
            searchField().performTextInput("cal")
            waitForText("Calendar")
            searchField().performTextClearance()
            searchField().performTextInput("zqx 😀")
            waitForText("No apps match", substring = true)
            onNodeWithText("Search the web for", substring = true).assertIsDisplayed()
        }
    }

    // F04-E3, F04-E4
    @Test
    fun hiddenAppsFollowTheSearchSetting() {
        TestState.reset(searchIncludesHidden = true) { c -> c.db.appDao().setHidden(TestState.app("Calendar").id, true, 1) }
        home {
            openSearch()
            searchField().performTextInput("calen")
            waitForText("Hidden")
        }
        TestState.reset(searchIncludesHidden = false) { c -> c.db.appDao().setHidden(TestState.app("Calendar").id, true, 1) }
        home {
            openSearch()
            searchField().performTextInput("calen")
            waitForText("No apps match", substring = true)
        }
    }

    // F10-H1, F10-E1, F10-E2, F03-E6
    @Test
    fun renameAcceptsLongEmojiNamesAndBlankResets() {
        TestState.reset(favorites = listOf("Clock"))
        val longName = "Réveil ⏰ " + "x".repeat(52) // 60 characters
        home {
            waitForText("Clock")
            onNodeWithText("Clock").performTouchInput { longClick() }
            onNodeWithText("More").performClick()
            onNodeWithText("Rename").performClick()
            searchField().performTextClearance()
            searchField().performTextInput(longName)
            onNodeWithText("Save").performClick()
            waitForText(longName)
            onNodeWithText(longName).assertIsDisplayed()

            onNodeWithText(longName).performTouchInput { longClick() }
            onNodeWithText("More").performClick()
            onNodeWithText("Rename").performClick()
            searchField().performTextClearance()
            onNodeWithText("Save").performClick()
            waitForText("Clock")
        }
        val clock = runBlocking { TestState.app("Clock") }
        assertEquals(null, clock.customLabel)
    }

    // F10-H2 (hide), F03-H1 (alphabet jump), F03-E5 (back to favorites)
    @Test
    fun alphabetJumpHideAndBack() {
        TestState.reset()
        home {
            jumpTo("M")
            waitForText("Maps")
            onNodeWithText("Maps").performTouchInput { longClick() }
            onNodeWithText("More").performClick()
            onNodeWithText("Hide from app list").performClick()
            waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Maps")).fetchSemanticsNodes().isEmpty() }
            pressBack()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodes(hasContentDescription("open clock", substring = true)).fetchSemanticsNodes().isNotEmpty()
            }
        }
        assertTrue(runBlocking { TestState.app("Maps").hidden })
    }

    // F06-H2
    @Test
    fun removingAFavoriteKeepsTheAppInstalled() {
        TestState.reset(favorites = listOf("Clock", "Calendar"))
        home {
            waitForText("Calendar")
            onNodeWithText("Calendar").performTouchInput { longClick() }
            onNodeWithText("Remove from favorites").performClick()
            waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Calendar")).fetchSemanticsNodes().isEmpty() }
            onNodeWithText("Clock").assertIsDisplayed()
        }
        val home = runBlocking { TestState.container.db.homeDao().items() }
        assertEquals(1, home.size)
    }
}
