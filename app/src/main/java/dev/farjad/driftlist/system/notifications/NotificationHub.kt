package dev.farjad.driftlist.system.notifications

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Which app a notification belongs to: package plus profile. */
data class AppKey(val packageName: String, val userSerial: Long)

data class NotifItem(
    val key: String,
    val app: AppKey,
    val title: String?,
    val text: String?,
    val postedAt: Long,
    val clearable: Boolean,
    val contentIntent: PendingIntent?,
) {
    /** One line for the inline preview under an app's name. */
    val preview: String
        get() = listOfNotNull(title?.takeIf { it.isNotBlank() }, text?.takeIf { it.isNotBlank() })
            .joinToString(": ")
}

/**
 * Live, in-memory mirror of the notifications the listener can see, grouped by app.
 * Nothing here is written to disk.
 */
class NotificationHub(private val context: Context) {
    private val userManager = context.getSystemService(UserManager::class.java)
    private val _items = MutableStateFlow<Map<AppKey, List<NotifItem>>>(emptyMap())
    val items: StateFlow<Map<AppKey, List<NotifItem>>> = _items.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    @Volatile
    private var service: DriftNotificationListener? = null

    fun hasAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /** Ask the system to reconnect the listener if access is granted but it is not bound. */
    fun ensureBound() {
        if (hasAccess() && service == null) {
            NotificationListenerService.requestRebind(ComponentName(context, DriftNotificationListener::class.java))
        }
    }

    fun dismiss(key: String) {
        service?.cancelNotification(key)
    }

    fun open(item: NotifItem): Boolean {
        val intent = item.contentIntent ?: return false
        return try {
            val options = launchOptions()
            intent.send(context, 0, null, null, null, null, options)
            if (item.clearable) dismiss(item.key)
            true
        } catch (e: PendingIntent.CanceledException) {
            Log.w(TAG, "notification intent cancelled", e)
            false
        }
    }

    /** Home is visible when a notification is tapped, so let its PendingIntent start an activity. */
    private fun launchOptions(): android.os.Bundle? = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA -> ActivityOptions.makeBasic()
            .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE)
            .toBundle()
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> @Suppress("DEPRECATION") ActivityOptions.makeBasic()
            .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            .toBundle()
        else -> null
    }

    internal fun attach(listener: DriftNotificationListener) {
        service = listener
        _connected.value = true
        replaceAll(listener.activeNotifications.orEmpty())
    }

    internal fun detach() {
        service = null
        _connected.value = false
        _items.value = emptyMap()
    }

    internal fun replaceAll(all: Array<out StatusBarNotification>) {
        _items.value = all.mapNotNull(::toItem)
            .groupBy { it.app }
            .mapValues { (_, list) -> list.sortedByDescending { it.postedAt } }
    }

    internal fun posted(sbn: StatusBarNotification) {
        val item = toItem(sbn)
        _items.update { current ->
            val app = item?.app ?: AppKey(sbn.packageName, serialOf(sbn))
            val others = current[app].orEmpty().filterNot { it.key == sbn.key }
            val list = if (item != null) (listOf(item) + others) else others
            if (list.isEmpty()) current - app else current + (app to list)
        }
    }

    internal fun removed(sbn: StatusBarNotification) {
        val app = AppKey(sbn.packageName, serialOf(sbn))
        _items.update { current ->
            val list = current[app].orEmpty().filterNot { it.key == sbn.key }
            if (list.isEmpty()) current - app else current + (app to list)
        }
    }

    private fun toItem(sbn: StatusBarNotification): NotifItem? {
        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null
        if (sbn.isOngoing) return null // media, downloads, navigation: not "news"
        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()
        if (title.isNullOrBlank() && text.isNullOrBlank()) return null
        return NotifItem(
            key = sbn.key,
            app = AppKey(sbn.packageName, serialOf(sbn)),
            title = title,
            text = text,
            postedAt = sbn.postTime,
            clearable = sbn.isClearable,
            contentIntent = n.contentIntent,
        )
    }

    private fun serialOf(sbn: StatusBarNotification): Long = userManager.getSerialNumberForUser(sbn.user)

    private companion object {
        const val TAG = "NotificationHub"
    }
}
