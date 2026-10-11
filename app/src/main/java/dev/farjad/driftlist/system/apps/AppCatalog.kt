package dev.farjad.driftlist.system.apps

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
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

/** An app shortcut (long-press menu entry) as shown in the app pop-up. */
data class ShortcutInfoItem(val id: String, val label: String, val info: ShortcutInfo)

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

    /** Called after every sync, once [info] knows the current set of apps. */
    @Volatile var onSynced: (() -> Unit)? = null

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
        onSynced?.invoke()
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

    /**
     * The app's shortcuts (static and dynamic). Only the default launcher may read them;
     * otherwise this returns an empty list.
     */
    fun shortcuts(app: AppEntity): List<ShortcutInfoItem> {
        val info = infos[app.id] ?: return emptyList()
        if (!runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false)) return emptyList()
        val query = LauncherApps.ShortcutQuery()
            .setPackage(app.packageName)
            .setActivity(info.componentName)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
            )
        return runCatching { launcherApps.getShortcuts(query, info.user).orEmpty() }
            .getOrDefault(emptyList())
            .filter { it.isEnabled }
            .sortedBy { it.rank }
            .distinctBy { it.id }
            .take(MAX_SHORTCUTS)
            .map { ShortcutInfoItem(it.id, (it.shortLabel ?: it.longLabel ?: it.id).toString(), it) }
    }

    fun shortcutIcon(item: ShortcutInfoItem) =
        runCatching { launcherApps.getShortcutIconDrawable(item.info, context.resources.displayMetrics.densityDpi) }.getOrNull()

    fun launchShortcut(item: ShortcutInfoItem, sourceBounds: Rect?) {
        runCatching { launcherApps.startShortcut(item.info, sourceBounds, null) }
            .onFailure { Log.w(TAG, "shortcut launch failed", it) }
    }

    private fun userFor(serial: Long): UserHandle? = userManager.getUserForSerialNumber(serial)

    /** Looks up a stored shortcut so its icon can be drawn; null if the app no longer offers it. */
    fun storedShortcut(packageName: String, shortcutId: String, userSerial: Long): ShortcutInfoItem? {
        val user = userFor(userSerial) ?: return null
        if (!runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false)) return null
        val query = LauncherApps.ShortcutQuery()
            .setPackage(packageName)
            .setShortcutIds(listOf(shortcutId))
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
            )
        val info = runCatching { launcherApps.getShortcuts(query, user)?.firstOrNull() }.getOrNull() ?: return null
        return ShortcutInfoItem(info.id, (info.shortLabel ?: info.longLabel ?: info.id).toString(), info)
    }

    fun launchStoredShortcut(packageName: String, shortcutId: String, userSerial: Long, sourceBounds: Rect?): Boolean {
        val user = userFor(userSerial) ?: return false
        return runCatching { launcherApps.startShortcut(packageName, shortcutId, sourceBounds, null, user) }
            .onFailure { Log.w(TAG, "stored shortcut launch failed", it) }
            .isSuccess
    }

    /**
     * Pins every shortcut a folder uses so dynamic shortcuts don't vanish when the app rotates
     * them. LauncherApps wants the full pinned set per package and user.
     */
    fun pinShortcuts(byPackage: Map<Pair<String, Long>, List<String>>) {
        if (!runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false)) return
        for ((key, ids) in byPackage) {
            val user = userFor(key.second) ?: continue
            runCatching { launcherApps.pinShortcuts(key.first, ids, user) }
                .onFailure { Log.w(TAG, "pin failed for ${key.first}", it) }
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
        const val MAX_SHORTCUTS = 5
    }
}
