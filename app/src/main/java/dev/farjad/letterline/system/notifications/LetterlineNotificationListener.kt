package dev.farjad.letterline.system.notifications

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dev.farjad.letterline.LetterlineApp

/** Bound by the system once the user grants notification access. Forwards everything to [NotificationHub]. */
class LetterlineNotificationListener : NotificationListenerService() {
    private val hub: NotificationHub
        get() = (application as LetterlineApp).container.notifications

    override fun onListenerConnected() {
        hub.attach(this)
    }

    override fun onListenerDisconnected() {
        hub.detach()
        requestRebind(ComponentName(this, LetterlineNotificationListener::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        hub.posted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        hub.removed(sbn)
    }
}
