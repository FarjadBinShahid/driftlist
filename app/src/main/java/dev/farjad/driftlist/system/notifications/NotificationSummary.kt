package dev.farjad.driftlist.system.notifications

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.farjad.driftlist.DriftlistApp
import dev.farjad.driftlist.R
import dev.farjad.driftlist.data.db.BatchItemEntity
import dev.farjad.driftlist.data.db.DriftlistDatabase
import dev.farjad.driftlist.data.db.NotificationBatchEntity
import dev.farjad.driftlist.domain.SummarySchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.util.UUID

/**
 * Notification Summary (S31): notifications from chosen apps are taken out of the shade, kept,
 * and delivered together as one notification a few times a day.
 */
class NotificationSummary(
    private val context: Context,
    private val db: DriftlistDatabase,
    private val scope: CoroutineScope,
) {
    /** Kept in memory so the listener can decide synchronously. */
    val rules = MutableStateFlow<Set<String>>(emptySet())
    @Volatile var enabled = false

    fun start() {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Notification summaries", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Held notifications, delivered together"
            },
        )
        scope.launch { db.summaryDao().observeRules().collect { list -> rules.value = list.mapTo(HashSet()) { it.packageName } } }
    }

    /**
     * Called by the listener for every new notification. Returns true when the notification was
     * held (the caller then removes it from the shade).
     */
    fun intercept(sbn: StatusBarNotification): Boolean {
        if (!enabled || sbn.packageName == context.packageName) return false
        if (sbn.isOngoing || !sbn.isClearable) return false
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        if (sbn.packageName !in rules.value) return false
        val extras = sbn.notification.extras
        val item = BatchItemEntity(
            batchId = "",
            notificationKey = sbn.key,
            packageName = sbn.packageName,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            body = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString(),
            postedAt = sbn.postTime,
        )
        scope.launch {
            val now = System.currentTimeMillis()
            val batch = db.summaryDao().hold(item) {
                NotificationBatchEntity(UUID.randomUUID().toString(), SummarySchedule.nextDelivery(now, ZoneId.systemDefault()), createdAt = now)
            }
            schedule(batch.scheduledFor)
        }
        return true
    }

    private fun schedule(at: Long) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        // An inexact 10-minute window: no exact-alarm permission needed, still lands near the hour.
        alarms.setWindow(AlarmManager.RTC_WAKEUP, at, 10 * 60 * 1000L, deliveryIntent())
    }

    private fun deliveryIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, SummaryReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Posts the open batch as one notification and closes it. */
    suspend fun deliver() {
        val dao = db.summaryDao()
        val batch = dao.openBatch() ?: return
        val items = dao.items(batch.id)
        dao.markDelivered(batch.id, System.currentTimeMillis())
        dao.deleteDelivered()
        if (items.isEmpty()) return
        val labels = db.appDao().all().associate { it.packageName to it.label }
        val style = NotificationCompat.InboxStyle()
        items.takeLast(MAX_LINES).forEach { i ->
            val app = labels[i.packageName] ?: i.packageName
            style.addLine(listOfNotNull(app, i.title, i.body).joinToString(" · "))
        }
        if (items.size > MAX_LINES) style.setSummaryText("+${items.size - MAX_LINES} more")
        val apps = items.map { labels[it.packageName] ?: it.packageName }.distinct()
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${items.size} held notification${if (items.size == 1) "" else "s"}")
            .setContentText(apps.take(3).joinToString(", ") + if (apps.size > 3) " and more" else "")
            .setStyle(style)
            .setAutoCancel(true)
            .build()
        val nm = NotificationManagerCompat.from(context)
        if (nm.areNotificationsEnabled()) {
            @Suppress("MissingPermission") // checked just above
            nm.notify(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val CHANNEL = "notification_summary"
        private const val NOTIFICATION_ID = 4202
        private const val MAX_LINES = 7
    }
}

/** Fired by AlarmManager at the batch's delivery time. */
class SummaryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as DriftlistApp
        val pending = goAsync()
        app.container.scope.launch {
            try {
                app.container.summary.deliver()
            } finally {
                pending.finish()
            }
        }
    }
}
