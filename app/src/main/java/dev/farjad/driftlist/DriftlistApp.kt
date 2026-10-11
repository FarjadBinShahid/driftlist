package dev.farjad.driftlist

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
import dev.farjad.driftlist.system.notifications.NotificationHub
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
    val backup = Backup(db, settings)
}

class DriftlistApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val c = AppContainer(this)
        container = c
        c.catalog.start()
        c.usageBreaker.start()

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
