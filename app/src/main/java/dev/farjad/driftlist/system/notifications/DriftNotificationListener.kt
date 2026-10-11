package dev.farjad.driftlist.system.notifications

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dev.farjad.driftlist.DriftlistApp

/** Bound by the system once the user grants notification access. Forwards everything to [NotificationHub]. */
class DriftNotificationListener : NotificationListenerService() {
    private val hub: NotificationHub
        get() = (application as DriftlistApp).container.notifications

    override fun onListenerConnected() {
        hub.attach(this)
    }

    override fun onListenerDisconnected() {
        hub.detach()
        requestRebind(ComponentName(this, DriftNotificationListener::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        hub.posted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        hub.removed(sbn)
    }
}
