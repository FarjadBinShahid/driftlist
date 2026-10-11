package dev.farjad.driftlist.data

import dev.farjad.driftlist.data.db.ThemeEntity
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.farjad.driftlist.data.db.AppEntity
import dev.farjad.driftlist.data.db.DriftlistDatabase
import dev.farjad.driftlist.data.db.PopupFolderEntity
import dev.farjad.driftlist.data.db.UsageRuleEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** F18: backup and restore. */
@RunWith(AndroidJUnit4::class)
class BackupTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var source: DriftlistDatabase
    private lateinit var target: DriftlistDatabase
    private val settings = Settings(context)

    @Before
    fun open() {
        source = Room.inMemoryDatabaseBuilder(context, DriftlistDatabase::class.java).build()
        target = Room.inMemoryDatabaseBuilder(context, DriftlistDatabase::class.java).build()
    }

    @After
    fun close() {
        source.close()
        target.close()
    }

    private fun app(id: String) = AppEntity(
        id = id, packageName = "pkg.$id", activityClass = "Main", userSerial = 0,
        systemLabel = id.uppercase(), sortKey = id, bucket = id.take(1).uppercase(),
        installedAt = 1, createdAt = 1, updatedAt = 1,
    )

    private fun backupOf(db: DriftlistDatabase): ByteArray = runBlocking {
        ByteArrayOutputStream().also { Backup(db, settings).write(it) }.toByteArray()
    }

    @Test
    fun roundTripRestoresSetupAndSkipsMissingApps() = runBlocking {
        source.appDao().sync(listOf(app("a"), app("b")))
        source.appDao().setCustomLabel("a", "Renamed", 1)
        source.appDao().setHidden("b", true, 1)
        source.homeDao().addApp("a", 1)
        source.homeDao().addApp("b", 1)
        source.folderDao().insertFolder(PopupFolderEntity("f", "Work", createdAt = 1, updatedAt = 1))
        source.folderDao().setApps("f", listOf("a", "b"), 1)
        source.homeDao().addFolder("f", 1)
        source.usageDao().setRules(listOf(UsageRuleEntity("pkg.a", 0, 1)))
        val bytes = backupOf(source)

        // The new phone only has app "a", plus a favorite of its own that the restore must replace.
        target.appDao().sync(listOf(app("a"), app("c")))
        target.homeDao().addApp("c", 1)

        val result = Backup(target, settings).restore(ByteArrayInputStream(bytes))

        assertEquals(1, result.skippedMissingApps)
        val home = target.homeDao().items()
        assertEquals(listOf("app", "folder"), home.map { it.kind })
        assertEquals("a", home.first().appId)
        assertEquals("Renamed", target.appDao().all().first { it.id == "a" }.customLabel)
        assertEquals(listOf("a"), target.folderDao().items().map { it.appId })
        assertEquals("Work", target.folderDao().folders().single().title)
        assertEquals(listOf("pkg.a"), target.usageDao().rules().map { it.packageName })
    }

    @Test
    fun roundTripKeepsShortcutsInFolders() = runBlocking {
        source.appDao().sync(listOf(app("a")))
        source.folderDao().insertFolder(PopupFolderEntity("f", "Chats", createdAt = 1, updatedAt = 1))
        source.folderDao().setApps("f", listOf("a"), 1)
        val sc = source.shortcutDao().getOrCreate("pkg.a", "compose", 0, "New message", 1)
        source.folderDao().addShortcut("f", sc.id, 1)
        source.homeDao().addFolder("f", 1)
        val bytes = backupOf(source)

        target.appDao().sync(listOf(app("a")))
        Backup(target, settings).restore(ByteArrayInputStream(bytes))

        val items = target.folderDao().items().sortedBy { it.position }
        assertEquals(listOf("app", "shortcut"), items.map { it.kind })
        val restored = target.shortcutDao().all().single()
        assertEquals("compose", restored.shortcutId)
        assertEquals("New message", restored.label)
        assertEquals(restored.id, items.last().shortcutId)
    }

    @Test
    fun roundTripKeepsThemesAndTheCurrentLook() = runBlocking {
        val bold = Look(ClockStyle.Bold, "system:serif", 1.1f, 0xFFE0604F.toInt(), ThemeMode.Dark)
        source.themeDao().upsert(ThemeEntity("t1", "Evening", bold.toJson(), ThemeEntity.ICONS_SYSTEM, 1, 1))
        settings.setLook(bold)
        settings.setHeaderPosition(0.3f)
        val bytes = backupOf(source)

        settings.setLook(Look())
        settings.setHeaderPosition(0.13f)
        Backup(target, settings).restore(ByteArrayInputStream(bytes))

        val theme = target.themeDao().all().single()
        assertEquals("Evening", theme.name)
        assertEquals(bold, Look.fromJson(theme.look))
        assertEquals(bold, settings.current().look)
        assertEquals(0.3f, settings.current().headerPosition, 0.001f)
        settings.setLook(Look())
        settings.setHeaderPosition(0.13f)
        Unit
    }

    @Test
    fun restoreClearsEditsNotInTheBackup() = runBlocking {
        source.appDao().sync(listOf(app("a")))
        val bytes = backupOf(source)
        target.appDao().sync(listOf(app("a")))
        target.appDao().setCustomLabel("a", "Old name", 1)
        target.appDao().setHidden("a", true, 1)

        Backup(target, settings).restore(ByteArrayInputStream(bytes))

        val a = target.appDao().all().single()
        assertNull(a.customLabel)
        assertFalse(a.hidden)
    }

    @Test
    fun rejectsFilesThatAreNotBackups() = runBlocking {
        target.appDao().sync(listOf(app("a")))
        target.homeDao().addApp("a", 1)
        try {
            Backup(target, settings).restore(ByteArrayInputStream("""{"hello": "world"}""".toByteArray()))
            fail("expected a rejection")
        } catch (e: IllegalArgumentException) {
            assertEquals("Not a Driftlist backup", e.message)
        }
        assertEquals(1, target.homeDao().items().size)
    }

    @Test
    fun rejectsBackupsFromANewerVersion() = runBlocking {
        val future = """{"format": "driftlist-backup", "version": 99}"""
        try {
            Backup(target, settings).restore(ByteArrayInputStream(future.toByteArray()))
            fail("expected a rejection")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("newer"))
        }
    }
}
