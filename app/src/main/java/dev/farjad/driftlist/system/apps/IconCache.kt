package dev.farjad.driftlist.system.apps

import android.content.Context
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** In-memory icon cache. Icons render off the main thread; the list shows a blank square until then. */
class IconCache(private val context: Context, private val catalog: AppCatalog) {
    private val cache = LruCache<String, ImageBitmap>(400)

    fun peek(appId: String, sizePx: Int): ImageBitmap? = cache.get(key(appId, sizePx))

    suspend fun load(appId: String, sizePx: Int): ImageBitmap? {
        peek(appId, sizePx)?.let { return it }
        val info = catalog.info(appId) ?: return null
        return withContext(Dispatchers.IO) {
            runCatching {
                info.getBadgedIcon(context.resources.displayMetrics.densityDpi)
                    .toBitmap(sizePx, sizePx)
                    .asImageBitmap()
            }.getOrNull()?.also { cache.put(key(appId, sizePx), it) }
        }
    }

    fun clear() = cache.evictAll()

    private fun key(appId: String, sizePx: Int) = "$appId@$sizePx"
}
