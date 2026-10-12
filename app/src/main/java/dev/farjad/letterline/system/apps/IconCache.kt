package dev.farjad.letterline.system.apps

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import dev.farjad.letterline.data.db.IconOverrideEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * In-memory icon cache. Resolves, in order: the user's per-app pick for the current pack,
 * the current icon pack's icon, the system icon. Bumps [version] whenever the inputs change
 * so on-screen icons reload.
 */
class IconCache(
    private val context: Context,
    private val catalog: AppCatalog,
    val packs: IconPacks,
) {
    private val cache = LruCache<String, ImageBitmap>(400)
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version

    @Volatile private var pack: String? = null
    @Volatile private var overrides: Map<String, IconOverrideEntity> = emptyMap()

    /** Set when an icon was requested before the catalog knew its app (cold start race). */
    @Volatile private var missed = false

    /** Hooked to AppCatalog.onSynced: anything that came up empty gets another try. */
    fun onCatalogSynced() {
        if (missed) {
            missed = false
            _version.value += 1
        }
    }

    fun update(pack: String?, overrides: List<IconOverrideEntity>) {
        val key = pack ?: IconOverrideEntity.SYSTEM
        val relevant = overrides.filter { it.iconPack == key }.associateBy { it.appId }
        if (pack == this.pack && relevant == this.overrides) return
        this.pack = pack
        this.overrides = relevant
        cache.evictAll()
        _version.value += 1
    }

    fun peek(appId: String, sizePx: Int): ImageBitmap? = cache.get(key(appId, sizePx))

    suspend fun load(appId: String, sizePx: Int): ImageBitmap? {
        peek(appId, sizePx)?.let { return it }
        return withContext(Dispatchers.IO) {
            resolve(appId)?.let { d ->
                runCatching { d.toBitmap(sizePx, sizePx).asImageBitmap() }.getOrNull()
            }?.also { cache.put(key(appId, sizePx), it) }
        }
    }

    private fun resolve(appId: String): Drawable? {
        val info = catalog.info(appId) ?: run {
            missed = true
            return null
        }
        val component = info.componentName.flattenToString()
        val currentPack = pack
        overrides[appId]?.drawableName?.let { name ->
            val source = currentPack ?: return@let
            packs.drawable(source, name)?.let { return badge(it, info) }
        }
        if (currentPack != null) {
            packs.drawableFor(currentPack, component)?.let { return badge(it, info) }
        }
        return runCatching { info.getBadgedIcon(context.resources.displayMetrics.densityDpi) }.getOrNull()
    }

    /** Keep the work-profile badge on pack icons. */
    private fun badge(icon: Drawable, info: android.content.pm.LauncherActivityInfo): Drawable =
        context.packageManager.getUserBadgedIcon(icon, info.user)

    /** One icon from a pack, for the picker grid (not cached). */
    suspend fun packIcon(pack: String, name: String, sizePx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        packs.drawable(pack, name)?.let { runCatching { it.toBitmap(sizePx, sizePx).asImageBitmap() }.getOrNull() }
    }

    /** The system icon for an app, ignoring packs (for the picker's "Default" cell). */
    suspend fun systemIcon(appId: String, sizePx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        catalog.info(appId)?.let { info ->
            runCatching {
                info.getBadgedIcon(context.resources.displayMetrics.densityDpi).toBitmap(sizePx, sizePx).asImageBitmap()
            }.getOrNull()
        }
    }

    fun currentPack(): String? = pack

    private fun key(appId: String, sizePx: Int) = "$appId@$sizePx"
}
