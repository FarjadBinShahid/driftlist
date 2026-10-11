package dev.farjad.driftlist.data

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.farjad.driftlist.data.db.AppEntity
import dev.farjad.driftlist.data.db.DriftlistDatabase
import dev.farjad.driftlist.system.apps.AppCatalog
import dev.farjad.driftlist.system.apps.IconCache
import dev.farjad.driftlist.system.apps.IconPacks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** B05: icons asked for before the first app sync must load once the sync finishes. */
@RunWith(AndroidJUnit4::class)
class IconCacheTest {
    @Test
    fun iconsRequestedBeforeTheFirstSyncReloadAfterIt() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val db = Room.inMemoryDatabaseBuilder(context, DriftlistDatabase::class.java).build()
        val catalog = AppCatalog(context, db, CoroutineScope(SupervisorJob() + Dispatchers.Default))
        val icons = IconCache(context, catalog, IconPacks(context))
        catalog.onSynced = icons::onCatalogSynced

        // Any launchable app on the device that isn't us.
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val info = launcherApps.getActivityList(null, Process.myUserHandle())
            .first { it.componentName.packageName != context.packageName }
        val serial = context.getSystemService(UserManager::class.java).getSerialNumberForUser(Process.myUserHandle())
        val id = AppEntity.idFor(info.componentName.packageName, info.componentName.className, serial)

        assertNull("no icon before the catalog knows the app", icons.load(id, 64))
        val before = icons.version.value

        catalog.sync()

        assertTrue("version bumps so on-screen icons retry", icons.version.value > before)
        assertNotNull(icons.load(id, 64))
        db.close()
    }
}
