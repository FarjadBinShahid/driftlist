package dev.farjad.driftlist

import dev.farjad.driftlist.system.HeadphoneWatcher
import dev.farjad.driftlist.system.TorchWatcher
import dev.farjad.driftlist.system.search.ContactSearch
import dev.farjad.driftlist.system.WallpaperWatcher
import dev.farjad.driftlist.system.calendar.CalendarSource
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import dev.farjad.driftlist.data.Backup
import dev.farjad.driftlist.data.Settings
import dev.farjad.driftlist.data.db.DriftlistDatabase
import dev.farjad.driftlist.system.apps.AppCatalog
import dev.farjad.driftlist.system.apps.IconCache
import dev.farjad.driftlist.system.apps.IconPacks
import dev.farjad.driftlist.system.media.MediaHub
import dev.farjad.driftlist.system.notifications.NotificationHub
import dev.farjad.driftlist.system.notifications.NotificationSummary
import dev.farjad.driftlist.system.usage.UsageBreaker
import dev.farjad.driftlist.system.widgets.WidgetHostManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Hand-wired dependencies. Small enough that a DI framework would cost more than it saves for now. */
class AppContainer(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db = DriftlistDatabase.create(context)
    val settings = Settings(context)
    val catalog = AppCatalog(context, db, scope)
    val iconPacks = IconPacks(context)
    val icons = IconCache(context, catalog, iconPacks)
    val notifications = NotificationHub(context)
    val widgetHost = WidgetHostManager(context)
    val usageBreaker = UsageBreaker(context, db, scope)
    val media = MediaHub(context)
    val calendar = CalendarSource(context)
    val wallpaper = WallpaperWatcher(context)
    val contacts = ContactSearch(context)
    val torch = TorchWatcher(context)
    val headphones = HeadphoneWatcher(context)
    val summary = NotificationSummary(context, db, scope)
    val backup = Backup(db, settings)
}

class DriftlistApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val c = AppContainer(this)
        container = c
        c.catalog.onSynced = c.icons::onCatalogSynced
        c.catalog.start()
        c.wallpaper.start()
        c.torch.start()
        c.headphones.start()
        c.usageBreaker.start()
        c.summary.start()
        c.notifications.interceptor = c.summary::intercept
        c.notifications.onConnected = { c.media.start() }
        if (c.notifications.hasAccess()) c.media.start()
        c.scope.launch {
            c.settings.flow.map { it.notificationSummary }.distinctUntilChanged().collect { c.summary.enabled = it }
        }

        // Icons follow the chosen pack and the user's per-app picks.
        c.scope.launch {
            combine(c.settings.flow.map { it.iconPack }.distinctUntilChanged(), c.db.iconOverrideDao().observeAll()) { pack, overrides ->
                pack to overrides
            }.collect { (pack, overrides) -> c.icons.update(pack, overrides) }
        }
        c.scope.launch {
            c.settings.flow.map { it.usageBreaker }.distinctUntilChanged().collect { c.usageBreaker.setEnabled(it) }
        }

        // Locale changes re-bucket the alphabet.
        ContextCompat.registerReceiver(
            this,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    c.catalog.requestSync()
                }
            },
            IntentFilter(Intent.ACTION_LOCALE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }
}
