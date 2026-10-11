package dev.farjad.driftlist

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import dev.farjad.driftlist.data.db.DriftlistDatabase
import dev.farjad.driftlist.system.apps.AppCatalog
import dev.farjad.driftlist.system.apps.IconCache
import dev.farjad.driftlist.system.notifications.NotificationHub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Hand-wired dependencies. Small enough that a DI framework would cost more than it saves for now. */
class AppContainer(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db = DriftlistDatabase.create(context)
    val catalog = AppCatalog(context, db, scope)
    val icons = IconCache(context, catalog)
    val notifications = NotificationHub(context)
}

class DriftlistApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.catalog.start()

        // Locale changes re-bucket the alphabet.
        ContextCompat.registerReceiver(
            this,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    container.catalog.requestSync()
                }
            },
            IntentFilter(Intent.ACTION_LOCALE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }
}
