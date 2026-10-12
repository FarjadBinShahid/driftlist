package dev.farjad.letterline.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.farjad.letterline.data.db.AppEntity
import dev.farjad.letterline.data.db.LetterlineDatabase
import dev.farjad.letterline.data.db.PopupFolderEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var db: LetterlineDatabase

    @Before
    fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LetterlineDatabase::class.java).build()
    }

    @After
    fun close() = db.close()

    private fun app(id: String, label: String = id) = AppEntity(
        id = id, packageName = "pkg.$id", activityClass = "Main", userSerial = 0,
        systemLabel = label, sortKey = label.lowercase(), bucket = label.take(1).uppercase(),
        installedAt = 1, createdAt = 1, updatedAt = 1,
    )

    @Test
    fun syncKeepsUserEditsAndDropsUninstalledApps() = runBlocking {
        val apps = db.appDao()
        apps.sync(listOf(app("a", "Alpha"), app("b", "Beta")))
        apps.setCustomLabel("a", "My Alpha", 2)
        apps.setHidden("a", true, 2)

        // Beta uninstalled, Alpha's system label changed by an update.
        apps.sync(listOf(app("a", "Alpha 2")))

        val all = apps.all()
        assertEquals(listOf("a"), all.map { it.id })
        assertEquals("My Alpha", all.single().customLabel)
        assertEquals("Alpha 2", all.single().systemLabel)
        assertTrue(all.single().hidden)
    }

    @Test
    fun uninstallCascadesToFavoritesFoldersAndIcons() = runBlocking {
        db.appDao().sync(listOf(app("a"), app("b")))
        db.homeDao().addApp("a", 1)
        db.folderDao().insertFolder(PopupFolderEntity("f", "Folder", createdAt = 1, updatedAt = 1))
        db.folderDao().setApps("f", listOf("a", "b"), 1)
        db.iconOverrideDao().set("a", "pack", "icon", 1)

        db.appDao().sync(listOf(app("b")))

        assertTrue(db.homeDao().items().isEmpty())
        assertEquals(listOf("b"), db.folderDao().items().map { it.appId })
        assertTrue(db.iconOverrideDao().all().isEmpty())
    }

    @Test
    fun addingTheSameFavoriteTwiceKeepsOne() = runBlocking {
        db.appDao().sync(listOf(app("a")))
        db.homeDao().addApp("a", 1)
        db.homeDao().addApp("a", 2)
        assertEquals(1, db.homeDao().items().size)
    }

    @Test
    fun reorderRewritesEveryPosition() = runBlocking {
        db.appDao().sync(listOf(app("a"), app("b"), app("c")))
        listOf("a", "b", "c").forEach { db.homeDao().addApp(it, 1) }
        val ids = db.homeDao().items().map { it.id }
        db.homeDao().reorder(listOf(ids[2], ids[0], ids[1]), 2)
        assertEquals(listOf("c", "a", "b"), db.homeDao().items().map { it.appId })
        assertEquals(listOf(0, 1, 2), db.homeDao().items().map { it.position })
    }

    @Test
    fun removingAFolderRemovesItsHomeRowButNotItsApps() = runBlocking {
        db.appDao().sync(listOf(app("a")))
        db.folderDao().insertFolder(PopupFolderEntity("f", "Folder", createdAt = 1, updatedAt = 1))
        db.folderDao().setApps("f", listOf("a"), 1)
        db.homeDao().addFolder("f", 1)

        db.folderDao().delete("f")

        assertTrue(db.homeDao().items().isEmpty())
        assertTrue(db.folderDao().items().isEmpty())
        assertEquals(1, db.appDao().all().size)
    }

    @Test
    fun iconOverrideIsPerPack() = runBlocking {
        db.appDao().sync(listOf(app("a")))
        db.iconOverrideDao().set("a", "packOne", "one", 1)
        db.iconOverrideDao().set("a", "packTwo", "two", 1)
        db.iconOverrideDao().set("a", "packOne", "uno", 2)
        val byPack = db.iconOverrideDao().all().associate { it.iconPack to it.drawableName }
        assertEquals(mapOf("packOne" to "uno", "packTwo" to "two"), byPack)
    }

    @Test
    fun launchCountIncrements() = runBlocking {
        db.appDao().sync(listOf(app("a")))
        db.appDao().markLaunched("a", 5)
        db.appDao().markLaunched("a", 6)
        val a = db.appDao().all().single()
        assertEquals(2, a.launchCount)
        assertEquals(6L, a.lastLaunchedAt)
    }

    @Test
    fun renameToNullRestoresSystemLabel() = runBlocking {
        db.appDao().sync(listOf(app("a", "Alpha")))
        db.appDao().setCustomLabel("a", "X", 1)
        db.appDao().setCustomLabel("a", null, 2)
        val a = db.appDao().all().single()
        assertNull(a.customLabel)
        assertEquals("Alpha", a.label)
    }
}
