package dev.farjad.driftlist.system.apps

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import dev.farjad.driftlist.data.db.AppEntity
import dev.farjad.driftlist.data.db.DriftlistDatabase
import dev.farjad.driftlist.domain.AlphabetIndexer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * The launcher's view of installed apps. Reads LauncherApps for every profile, keeps the `app`
 * table in sync, and launches things.
 */
class AppCatalog(
    private val context: Context,
    private val db: DriftlistDatabase,
    private val scope: CoroutineScope,
) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val infos = ConcurrentHashMap<String, LauncherActivityInfo>()
    private val syncLock = Mutex()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = requestSync()
        override fun onPackageAdded(packageName: String, user: UserHandle) = requestSync()
        override fun onPackageChanged(packageName: String, user: UserHandle) = requestSync()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = requestSync()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = requestSync()
    }

    fun start() {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        requestSync()
    }

    fun requestSync() {
        scope.launch { sync() }
    }

    suspend fun sync() = syncLock.withLock {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val indexer = AlphabetIndexer.forCurrentLocales()
            val fresh = ArrayList<AppEntity>()
            val seen = HashSet<String>()
            for (user in userManager.userProfiles) {
                val serial = userManager.getSerialNumberForUser(user)
                for (info in launcherApps.getActivityList(null, user)) {
                    val component = info.componentName
                    if (component.packageName == context.packageName) continue
                    val id = AppEntity.idFor(component.packageName, component.className, serial)
                    val label = info.label?.toString()?.trim().orEmpty().ifEmpty { component.packageName }
                    infos[id] = info
                    seen += id
                    fresh += AppEntity(
                        id = id,
                        packageName = component.packageName,
                        activityClass = component.className,
                        userSerial = serial,
                        systemLabel = label,
                        sortKey = indexer.sortKey(label),
                        bucket = indexer.bucketOf(label),
                        isSystem = info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                        installedAt = info.firstInstallTime,
                        createdAt = now,
                        updatedAt = now,
                    )
                }
            }
            infos.keys.retainAll(seen)
            db.appDao().sync(fresh)
        }
    }

    fun info(appId: String): LauncherActivityInfo? = infos[appId]

    fun launch(app: AppEntity, sourceBounds: Rect?): Boolean {
        val info = infos[app.id] ?: return false
        return try {
            launcherApps.startMainActivity(info.componentName, info.user, sourceBounds, null)
            true
        } catch (e: RuntimeException) {
            // SecurityException / ActivityNotFoundException: the app vanished or is disabled.
            Log.w(TAG, "launch failed for ${app.id}", e)
            requestSync()
            false
        }
    }

    fun openAppInfo(app: AppEntity, sourceBounds: Rect?) {
        val info = infos[app.id] ?: return
        runCatching { launcherApps.startAppDetailsActivity(info.componentName, info.user, sourceBounds, null) }
    }

    fun uninstall(app: AppEntity) {
        val info = infos[app.id] ?: return
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null))
            .putExtra(Intent.EXTRA_USER, info.user)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "no uninstaller", e)
        }
    }

    private companion object {
        const val TAG = "AppCatalog"
    }
}
