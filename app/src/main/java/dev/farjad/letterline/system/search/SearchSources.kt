package dev.farjad.letterline.system.search

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.core.content.ContextCompat
import dev.farjad.letterline.domain.SearchEngine
import dev.farjad.letterline.domain.WebSuggestions
import dev.farjad.letterline.system.SystemActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Live web suggestions. Only called when the user has switched them on: the typed text goes to
 * the chosen engine's autocomplete endpoint, nothing else (no cookies, no ids).
 */
object WebSuggester {
    private const val TIMEOUT_MS = 2_500
    private const val MAX_BYTES = 64 * 1024

    suspend fun fetch(engine: SearchEngine, query: String): List<String> = withContext(Dispatchers.IO) {
        val url = engine.suggestUrl(query) ?: return@withContext emptyList()
        runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = true
                useCaches = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Letterline")
            }
            try {
                if (conn.responseCode !in 200..299) return@runCatching emptyList()
                val bytes = conn.inputStream.use { input ->
                    val buf = ByteArray(MAX_BYTES)
                    var n = 0
                    while (n < MAX_BYTES) {
                        val r = input.read(buf, n, MAX_BYTES - n)
                        if (r < 0) break
                        n += r
                    }
                    buf.copyOf(n)
                }
                WebSuggestions.parse(bytes.toString(Charsets.UTF_8), query)
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(emptyList())
    }
}

data class ContactHit(val contactId: Long, val lookupKey: String?, val name: String, val number: String?, val photo: Uri?)

/** Contacts in search (opt-in, READ_CONTACTS). Results are read live; nothing is stored. */
class ContactSearch(private val context: Context) {
    fun hasPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun search(query: String, limit: Int = 5): List<ContactHit> {
        if (query.isBlank() || !hasPermission()) return emptyList()
        return try {
            val uri = Uri.withAppendedPath(Phone.CONTENT_FILTER_URI, Uri.encode(query.trim()))
            context.contentResolver.query(
                uri,
                arrayOf(Phone.CONTACT_ID, Phone.LOOKUP_KEY, Phone.DISPLAY_NAME_PRIMARY, Phone.NUMBER, Phone.PHOTO_THUMBNAIL_URI),
                null, null, "${Phone.IS_SUPER_PRIMARY} DESC, ${Phone.TIMES_CONTACTED} DESC",
            )?.use { c ->
                val seen = LinkedHashMap<Long, ContactHit>()
                while (c.moveToNext() && seen.size < limit) {
                    val id = c.getLong(0)
                    if (id in seen) continue
                    seen[id] = ContactHit(id, c.getString(1), c.getString(2).orEmpty(), c.getString(3), c.getString(4)?.let(Uri::parse))
                }
                seen.values.toList()
            }.orEmpty()
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }

    fun open(hit: ContactHit) {
        val uri = hit.lookupKey?.let { ContactsContract.Contacts.getLookupUri(hit.contactId, it) }
            ?: ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, hit.contactId)
        SystemActions.start(context, Intent(Intent.ACTION_VIEW, uri))
    }

    /** Opens the dialler with the number filled in; Letterline never places calls itself. */
    fun call(hit: ContactHit) {
        hit.number?.let { SystemActions.start(context, Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", it, null))) }
    }

    fun message(hit: ContactHit) {
        hit.number?.let { SystemActions.start(context, Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", it, null))) }
    }
}
