package dev.farjad.driftlist.system

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** [seed]: the wallpaper's main colour (API 27+), the seed for "colours from wallpaper". */
data class WallpaperInfo(val seed: Int? = null, val darkText: Boolean = false)

/** Follows the home wallpaper's colours, so the accent and text colours change with it. */
class WallpaperWatcher(private val context: Context) {
    private val _state = MutableStateFlow(read())
    val state: StateFlow<WallpaperInfo> = _state

    fun start() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return
        runCatching {
            WallpaperManager.getInstance(context).addOnColorsChangedListener(
                { _, which -> if (which and WallpaperManager.FLAG_SYSTEM != 0) refresh() },
                Handler(Looper.getMainLooper()),
            )
        }
    }

    fun refresh() {
        _state.value = read()
    }

    private fun read(): WallpaperInfo {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return WallpaperInfo()
        val colors = runCatching {
            WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        }.getOrNull() ?: return WallpaperInfo()
        val darkText = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0
        return WallpaperInfo(colors.primaryColor.toArgb(), darkText)
    }
}
