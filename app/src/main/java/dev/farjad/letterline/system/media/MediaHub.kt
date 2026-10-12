package dev.farjad.letterline.system.media

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.util.Log
import dev.farjad.letterline.system.notifications.LetterlineNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the home screen's music card shows. */
data class MediaState(
    val packageName: String,
    val title: String,
    val artist: String?,
    val art: Bitmap?,
    val playing: Boolean,
    val canSkip: Boolean,
)

/**
 * Follows the active media session (needs notification access, which Android uses as the
 * permission for reading other apps' media sessions) and exposes play / pause / skip.
 */
class MediaHub(private val context: Context) {
    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, LetterlineNotificationListener::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow<MediaState?>(null)
    val state: StateFlow<MediaState?> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var started = false

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onSessionDestroyed() = pick(null)
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list -> pick(list) }

    /** Safe to call repeatedly; does nothing until notification access is granted. */
    fun start() {
        if (started) return
        try {
            manager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, handler)
            started = true
            pick(manager.getActiveSessions(listenerComponent))
        } catch (e: SecurityException) {
            Log.i(TAG, "no notification access yet; media card off")
        }
    }

    private fun pick(list: List<MediaController>?) {
        val next = list.orEmpty().firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: list.orEmpty().firstOrNull { it.playbackState?.state == PlaybackState.STATE_PAUSED }
        if (next?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(controllerCallback)
            controller = next
            next?.registerCallback(controllerCallback, handler)
        }
        publish()
    }

    private fun publish() {
        val c = controller
        val meta = c?.metadata
        val playback = c?.playbackState
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
        if (c == null || title.isNullOrBlank() || playback == null || playback.state == PlaybackState.STATE_STOPPED || playback.state == PlaybackState.STATE_NONE) {
            _state.value = null
            return
        }
        _state.value = MediaState(
            packageName = c.packageName,
            title = title,
            artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
            art = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART),
            playing = playback.state == PlaybackState.STATE_PLAYING,
            canSkip = playback.actions and (PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0L,
        )
    }

    fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
    }

    fun next() = controller?.transportControls?.skipToNext()
    fun previous() = controller?.transportControls?.skipToPrevious()

    /** Opens the playing item in its app (the session's own activity). */
    fun open(): Boolean {
        val intent: PendingIntent = controller?.sessionActivity ?: return false
        return runCatching { intent.send() }.isSuccess
    }

    private companion object {
        const val TAG = "MediaHub"
    }
}
