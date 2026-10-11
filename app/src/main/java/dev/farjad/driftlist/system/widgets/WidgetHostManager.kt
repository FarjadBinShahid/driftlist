package dev.farjad.driftlist.system.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.os.UserManager
import android.util.Log

/** Hosts third-party widgets through the official AppWidgetHost API. */
class WidgetHostManager(private val context: Context) {
    private val manager = AppWidgetManager.getInstance(context)
    private val userManager = context.getSystemService(UserManager::class.java)
    val host = AppWidgetHost(context, HOST_ID)

    fun allocate(): Int = host.allocateAppWidgetId()

    fun release(appWidgetId: Int) {
        runCatching { host.deleteAppWidgetId(appWidgetId) }
    }

    fun info(appWidgetId: Int): AppWidgetProviderInfo? = manager.getAppWidgetInfo(appWidgetId)

    fun providers(): List<AppWidgetProviderInfo> =
        userManager.userProfiles.flatMap { manager.getInstalledProvidersForProfile(it) }

    /** Binds silently if the user already allowed it; otherwise returns the system "allow" dialog intent. */
    fun bindOrIntent(appWidgetId: Int, provider: AppWidgetProviderInfo): Intent? {
        val bound = runCatching {
            manager.bindAppWidgetIdIfAllowed(appWidgetId, provider.profile, provider.provider, null)
        }.getOrDefault(false)
        if (bound) return null
        return Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, provider.profile)
    }

    fun needsConfigure(appWidgetId: Int): Boolean = info(appWidgetId)?.configure != null

    fun createView(context: Context, appWidgetId: Int): AppWidgetHostView =
        host.createView(context, appWidgetId, info(appWidgetId))

    fun serialOf(user: UserHandle): Long = userManager.getSerialNumberForUser(user)

    fun startListening() {
        runCatching { host.startListening() }.onFailure { Log.w(TAG, "startListening", it) }
    }

    fun stopListening() {
        runCatching { host.stopListening() }
    }

    /** Free ids the host still holds that no widget row references (cancelled adds, old crashes). */
    fun releaseOrphans(referenced: Collection<Int>) {
        val keep = referenced.toSet()
        host.appWidgetIds.filterNot { it in keep }.forEach(::release)
    }

    companion object {
        private const val TAG = "WidgetHost"
        const val HOST_ID = 0x0D41F
    }
}
