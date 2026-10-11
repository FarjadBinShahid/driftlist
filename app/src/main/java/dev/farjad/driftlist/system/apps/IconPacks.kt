package dev.farjad.driftlist.system.apps

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.util.concurrent.ConcurrentHashMap

data class IconPack(val packageName: String, val label: String)

/**
 * Third-party icon packs, read through the long-standing community convention: a pack declares
 * one of a few well-known intent actions and ships `appfilter.xml` mapping
 * `ComponentInfo{package/activity}` to drawable names (and optionally `drawable.xml` listing all icons).
 */
class IconPacks(private val context: Context) {
    private class Loaded(
        val resources: Resources,
        val byComponent: Map<String, String>,
        val allNames: List<String>,
    )

    private val loaded = ConcurrentHashMap<String, Loaded>()

    fun installed(): List<IconPack> {
        val pm = context.packageManager
        return PACK_ACTIONS
            .flatMap { action -> pm.queryIntentActivities(Intent(action), PackageManager.GET_META_DATA) }
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .map { IconPack(it.packageName, pm.getApplicationLabel(it).toString()) }
            .sortedBy { it.label.lowercase() }
    }

    /** The pack's icon for an activity ("package/activity"), or null if the pack does not cover it. */
    fun drawableFor(pack: String, component: String): Drawable? {
        val pk = load(pack) ?: return null
        val name = pk.byComponent[component] ?: return null
        return drawable(pack, name)
    }

    fun nameFor(pack: String, component: String): String? = load(pack)?.byComponent?.get(component)

    @SuppressLint("DiscouragedApi") // icon packs are addressed by resource name by design
    fun drawable(pack: String, name: String): Drawable? {
        val pk = load(pack) ?: return null
        val id = pk.resources.getIdentifier(name, "drawable", pack)
        if (id == 0) return null
        return runCatching { pk.resources.getDrawable(id, null) }.getOrNull()
    }

    /** Every icon name in the pack, for the per-app picker. */
    fun allNames(pack: String): List<String> = load(pack)?.allNames.orEmpty()

    private fun load(pack: String): Loaded? = loaded[pack] ?: runCatching { parse(pack) }
        .onFailure { Log.w(TAG, "cannot read icon pack $pack", it) }
        .getOrNull()
        ?.also { loaded[pack] = it }

    @SuppressLint("DiscouragedApi")
    private fun parse(pack: String): Loaded {
        val res = context.packageManager.getResourcesForApplication(pack)
        val byComponent = HashMap<String, String>()
        readXml(res, pack, "appfilter") { parser ->
            if (parser.name == "item") {
                val component = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")
                if (component != null && drawable != null) {
                    COMPONENT.find(component)?.let { byComponent[it.groupValues[1]] = drawable }
                }
            }
        }
        val all = LinkedHashSet<String>()
        readXml(res, pack, "drawable") { parser ->
            if (parser.name == "item") parser.getAttributeValue(null, "drawable")?.let(all::add)
        }
        if (all.isEmpty()) all.addAll(byComponent.values)
        return Loaded(res, byComponent, all.toList())
    }

    /** Packs ship these as res/xml/<name>.xml or assets/<name>.xml. */
    @SuppressLint("DiscouragedApi")
    private fun readXml(res: Resources, pack: String, name: String, onStartTag: (XmlPullParser) -> Unit) {
        val xmlId = res.getIdentifier(name, "xml", pack)
        val parser: XmlPullParser = if (xmlId != 0) {
            res.getXml(xmlId)
        } else {
            val stream = runCatching { res.assets.open("$name.xml") }.getOrNull() ?: return
            XmlPullParserFactory.newInstance().newPullParser().apply { setInput(stream, "UTF-8") }
        }
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) onStartTag(parser)
            event = parser.next()
        }
    }

    private companion object {
        const val TAG = "IconPacks"
        val COMPONENT = Regex("ComponentInfo\\{([^}]+)\\}")
        val PACK_ACTIONS = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "com.gau.go.launcherex.theme",
            "org.adw.launcher.icons.ACTION_PICK_ICON",
        )
    }
}
