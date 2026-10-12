package dev.farjad.letterline.system.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.provider.CalendarContract.Attendees
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Instances
import androidx.core.content.ContextCompat
import dev.farjad.letterline.domain.CalEvent
import dev.farjad.letterline.system.SystemActions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

private const val EVENT_TYPE = "vnd.android.cursor.item/event"

data class CalendarInfo(val id: Long, val name: String, val account: String, val color: Int)

/** Reads the system calendar (S14). Nothing is copied into Letterline's database. */
class CalendarSource(private val context: Context) {
    private val _access = MutableStateFlow(hasPermission())
    val access: StateFlow<Boolean> = _access

    fun hasPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun refreshAccess() {
        _access.value = hasPermission()
    }

    /** Emits now, on every calendar change, and every 15 minutes so the week window keeps rolling. */
    fun changes(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        trySend(Unit)
        val registered = runCatching {
            context.contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
        }.isSuccess
        val ticker = launch {
            while (true) {
                delay(15 * 60_000L)
                trySend(Unit)
            }
        }
        awaitClose {
            ticker.cancel()
            if (registered) context.contentResolver.unregisterContentObserver(observer)
        }
    }.conflate()

    fun calendars(): List<CalendarInfo> = guarded {
        context.contentResolver.query(
            Calendars.CONTENT_URI,
            arrayOf(Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME, Calendars.ACCOUNT_NAME, Calendars.CALENDAR_COLOR),
            "${Calendars.VISIBLE} = 1",
            null,
            "${Calendars.ACCOUNT_NAME}, ${Calendars.CALENDAR_DISPLAY_NAME}",
        )?.use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(CalendarInfo(c.getLong(0), c.getString(1).orEmpty(), c.getString(2).orEmpty(), c.getInt(3)))
                }
            }
        }.orEmpty()
    }

    /** Event occurrences from yesterday to a week ahead (the domain trims to what's shown). */
    fun upcoming(hiddenCalendars: Set<Long>, zone: ZoneId = ZoneId.systemDefault()): List<CalEvent> {
        val today = LocalDate.now(zone)
        val from = today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = today.plusDays(8).atStartOfDay(zone).toInstant().toEpochMilli()
        return events(from, to).filter { it.calendarId !in hiddenCalendars }
    }

    fun events(fromMs: Long, toMs: Long): List<CalEvent> = guarded {
        val uri = Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, fromMs)
            ContentUris.appendId(it, toMs)
        }.build()
        context.contentResolver.query(
            uri,
            arrayOf(
                Instances._ID, Instances.EVENT_ID, Instances.TITLE, Instances.BEGIN, Instances.END,
                Instances.ALL_DAY, Instances.DISPLAY_COLOR, Instances.CALENDAR_ID, Instances.EVENT_LOCATION,
            ),
            "${Instances.VISIBLE} = 1 AND ${Instances.SELF_ATTENDEE_STATUS} != ${Attendees.ATTENDEE_STATUS_DECLINED}",
            null,
            "${Instances.BEGIN} ASC",
        )?.use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        CalEvent(
                            instanceId = c.getLong(0),
                            eventId = c.getLong(1),
                            title = c.getString(2).orEmpty(),
                            begin = c.getLong(3),
                            end = c.getLong(4),
                            allDay = c.getInt(5) == 1,
                            color = c.getInt(6),
                            calendarId = c.getLong(7),
                            location = c.getString(8)?.takeIf { it.isNotBlank() },
                        ),
                    )
                }
            }
        }.orEmpty()
    }

    fun open(event: CalEvent) {
        // Typed explicitly: an untyped content URI also matches apps that merely view any link.
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(ContentUris.withAppendedId(Events.CONTENT_URI, event.eventId), EVENT_TYPE)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.begin)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end)
        SystemActions.start(context, intent)
    }

    fun newEvent() {
        SystemActions.start(context, Intent(Intent.ACTION_INSERT, Events.CONTENT_URI))
    }

    /** Access can be revoked at any moment from system settings; never crash over it. */
    private inline fun <T> guarded(block: () -> List<T>): List<T> {
        if (!hasPermission()) return emptyList()
        return try {
            block()
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }
}
