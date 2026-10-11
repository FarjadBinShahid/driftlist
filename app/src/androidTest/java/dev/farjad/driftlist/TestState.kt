package dev.farjad.driftlist

import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import dev.farjad.driftlist.data.db.AppEntity
import kotlinx.coroutines.runBlocking

/**
 * Puts the real app (database + settings) into a known state before a UI test.
 * Tests run on the emulator against the apps it ships with (Calendar, Clock, Chrome, Maps...).
 */
object TestState {
    val app: DriftlistApp get() = ApplicationProvider.getApplicationContext()
    val container: AppContainer get() = app.container

    fun reset(
        onboardingDone: Boolean = true,
        favorites: List<String> = emptyList(),
        searchIncludesHidden: Boolean = true,
        extra: suspend (AppContainer) -> Unit = {},
    ) = runBlocking {
        makeDefaultLauncher()
        val c = container
        c.db.clearAllTables()
        c.catalog.sync()
        c.settings.import(
            mapOf(
                "onboarding_done" to onboardingDone,
                "search_includes_hidden" to searchIncludesHidden,
                "swipe_up_search" to true,
            ),
        )
        val now = System.currentTimeMillis()
        favorites.forEach { label -> c.db.homeDao().addApp(app(label).id, now) }
        extra(c)
    }

    /** Debug builds are reinstalled per run, which drops the home role; grant it via the test shell. */
    private fun makeDefaultLauncher() {
        val pkg = app.packageName
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("cmd role add-role-holder android.app.role.HOME $pkg 0")
            .close()
        Thread.sleep(300)
    }

    suspend fun app(label: String): AppEntity =
        container.db.appDao().all().first { it.systemLabel == label }
}
