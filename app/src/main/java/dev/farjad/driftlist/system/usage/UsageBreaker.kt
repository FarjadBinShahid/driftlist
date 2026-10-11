package dev.farjad.driftlist.system.usage

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.UserManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.farjad.driftlist.R
import dev.farjad.driftlist.data.db.DriftlistDatabase
import dev.farjad.driftlist.data.db.UsageSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Gentle reminders after long stretches in chosen apps. Never blocks anything.
 *
 * While the screen is on and at least one app is chosen, polls UsageStatsManager every
 * [POLL_MS] for the foreground app. A session survives short detours (under [GRACE_MS]).
 */
class UsageBreaker(
    private val context: Context,
    private val db: DriftlistDatabase,
    private val scope: CoroutineScope,
) {
    private val usage = context.getSystemService(UsageStatsManager::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private var loop: Job? = null
    private var foreground: String? = null
    private var lastQuery = 0L

    @Volatile var enabled = false
        private set

    @Suppress("DEPRECATION") // unsafeCheckOpNoThrow is the documented check for usage access
    fun hasUsageAccess(): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun usageAccessIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun start() {
        createChannel()
        ContextCompat.registerReceiver(
            context,
            object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    when (intent.action) {
                        Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> resume()
                        Intent.ACTION_SCREEN_OFF -> pause()
                    }
                }
            },
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        if (value) resume() else pause()
    }

    private fun resume() {
        if (!enabled || loop?.isActive == true) return
        loop = scope.launch {
            while (isActive) {
                runCatching { tick() }
                delay(POLL_MS)
            }
        }
    }

    private fun pause() {
        loop?.cancel()
        loop = null
    }

    private suspend fun tick() {
        if (!hasUsageAccess()) return
        val now = System.currentTimeMillis()
        val dao = db.usageDao()
        val rules = dao.rules().mapTo(HashSet()) { it.packageName }
        if (rules.isEmpty()) return

        foreground = currentForeground(now) ?: foreground
        val fg = foreground
        val open = dao.openSession()

        if (fg != null && fg in rules) {
            val session = if (open != null && open.packageName == fg && now - open.lastSeenAt <= GRACE_MS) {
                open.copy(lastSeenAt = now)
            } else {
                open?.let { dao.saveSession(it.copy(endedAt = it.lastSeenAt)) }
                UsageSessionEntity(UUID.randomUUID().toString(), fg, mySerial(), startedAt = now, lastSeenAt = now)
            }
            val minutes = ((now - session.startedAt) / 60_000L).toInt()
            val due = LADDER.lastOrNull { it <= minutes && it > session.lastReminderMinute }
            if (due != null) {
                remind(fg, due)
                dao.saveSession(session.copy(lastReminderMinute = due))
            } else {
                dao.saveSession(session)
            }
        } else if (open != null && now - open.lastSeenAt > GRACE_MS) {
            dao.saveSession(open.copy(endedAt = open.lastSeenAt))
        }
    }

    /** Latest app that came to the foreground since the last query, or null if nothing changed. */
    private fun currentForeground(now: Long): String? {
        val from = if (lastQuery == 0L) now - 10 * 60_000L else lastQuery - 1_000L
        lastQuery = now
        val events = usage.queryEvents(from, now)
        val event = UsageEvents.Event()
        var latest: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            @Suppress("DEPRECATION")
            val resumed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            } else {
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            }
            if (resumed) latest = event.packageName
        }
        return latest
    }

    private suspend fun remind(packageName: String, minutes: Int) {
        if (!canPostNotifications()) return
        val label = db.appDao().all().firstOrNull { it.packageName == packageName }?.label ?: packageName
        val text = when {
            minutes < 60 -> "$minutes minutes in $label. Still where you want to be?"
            minutes % 60 == 0 -> "${minutes / 60} h in $label. Still where you want to be?"
            else -> "${minutes / 60} h ${minutes % 60} min in $label. Still where you want to be?"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Time check")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000L)
            .build()
        @Suppress("MissingPermission") // checked in canPostNotifications()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel() {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Time checks", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Reminders after long sessions in the apps you chose"
            },
        )
    }

    private fun mySerial(): Long = userManager.getSerialNumberForUser(Process.myUserHandle())

    companion object {
        const val CHANNEL = "usage_breaker"
        private const val NOTIFICATION_ID = 4101
        private const val POLL_MS = 30_000L
        private const val GRACE_MS = 90_000L

        /** Minutes of continuous use at which a reminder fires. Gets sparser as the session grows. */
        val LADDER = listOf(5, 10, 20, 30, 45, 60, 90, 120, 180, 240)
    }
}
