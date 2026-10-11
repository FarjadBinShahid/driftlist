package dev.farjad.driftlist.data

import dev.farjad.driftlist.data.db.ThemeEntity
import androidx.room.withTransaction
import dev.farjad.driftlist.data.db.DriftlistDatabase
import dev.farjad.driftlist.data.db.FolderItemEntity
import dev.farjad.driftlist.data.db.HomeItemEntity
import dev.farjad.driftlist.data.db.IconOverrideEntity
import dev.farjad.driftlist.data.db.PopupFolderEntity
import dev.farjad.driftlist.data.db.ShortcutEntity
import dev.farjad.driftlist.data.db.UsageRuleEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

/**
 * Backup file: versioned JSON of the user's setup. Apps themselves are not stored (they come
 * from the phone), only the user's edits to them. Widgets are left out on purpose: their
 * bindings belong to this phone and must be re-added after a restore.
 */
class Backup(private val db: DriftlistDatabase, private val settings: Settings) {

    data class Result(val restoredFavorites: Int, val skippedMissingApps: Int)

    suspend fun write(out: OutputStream) {
        val apps = db.appDao().all()
        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("created_at", System.currentTimeMillis())
            .put("app_edits", JSONArray().apply {
                apps.filter { it.customLabel != null || it.hidden }.forEach {
                    put(JSONObject().put("id", it.id).put("custom_label", it.customLabel ?: JSONObject.NULL).put("hidden", it.hidden))
                }
            })
            .put("home_items", JSONArray().apply {
                db.homeDao().items().forEach {
                    put(JSONObject().put("id", it.id).put("position", it.position).put("kind", it.kind)
                        .put("app_id", it.appId ?: JSONObject.NULL).put("folder_id", it.folderId ?: JSONObject.NULL))
                }
            })
            .put("folders", JSONArray().apply {
                db.folderDao().folders().forEach { put(JSONObject().put("id", it.id).put("title", it.title)) }
            })
            .put("shortcuts", JSONArray().apply {
                db.shortcutDao().all().forEach {
                    put(JSONObject().put("id", it.id).put("package_name", it.packageName).put("shortcut_id", it.shortcutId)
                        .put("user_serial", it.userSerial).put("label", it.label))
                }
            })
            .put("folder_items", JSONArray().apply {
                db.folderDao().items().forEach {
                    put(JSONObject().put("id", it.id).put("folder_id", it.folderId).put("position", it.position)
                        .put("kind", it.kind).put("app_id", it.appId ?: JSONObject.NULL)
                        .put("shortcut_id", it.shortcutId ?: JSONObject.NULL))
                }
            })
            .put("icon_overrides", JSONArray().apply {
                db.iconOverrideDao().all().forEach {
                    put(JSONObject().put("app_id", it.appId).put("icon_pack", it.iconPack).put("drawable_name", it.drawableName ?: JSONObject.NULL))
                }
            })
            .put("usage_rules", JSONArray().apply {
                db.usageDao().rules().forEach { put(JSONObject().put("package_name", it.packageName).put("user_serial", it.userSerial)) }
            })
            .put("themes", JSONArray().apply {
                db.themeDao().all().forEach {
                    put(JSONObject().put("id", it.id).put("name", it.name).put("look", it.look)
                        .put("icon_pack", it.iconPack ?: JSONObject.NULL).put("created_at", it.createdAt))
                }
            })
            .put("settings", JSONObject(settings.export()))
        out.write(root.toString(2).toByteArray(Charsets.UTF_8))
    }

    /** Replaces the current setup with the file's, in one transaction. Rows for apps not installed here are skipped. */
    suspend fun restore(input: InputStream): Result {
        val root = JSONObject(input.readBytes().toString(Charsets.UTF_8))
        require(root.optString("format") == FORMAT) { "Not a Driftlist backup" }
        require(root.optInt("version") in 1..VERSION) { "Backup is from a newer version of Driftlist" }

        val now = System.currentTimeMillis()
        var skipped = 0
        var favorites = 0
        db.withTransaction {
            val installed = db.appDao().all().associateBy { it.id }

            // App edits: reset everything, then apply the file's.
            db.appDao().update(installed.values.map { it.copy(customLabel = null, hidden = false) })
            root.optJSONArray("app_edits").objects().forEach { o ->
                val app = installed[o.getString("id")] ?: return@forEach
                db.appDao().update(listOf(app.copy(customLabel = o.optStringOrNull("custom_label"), hidden = o.optBoolean("hidden"))))
            }

            db.homeDao().clear()
            db.folderDao().folders().forEach { db.folderDao().delete(it.id) }
            db.shortcutDao().deleteUnused()

            root.optJSONArray("folders").objects().forEach { o ->
                db.folderDao().insertFolder(PopupFolderEntity(o.getString("id"), o.getString("title"), createdAt = now, updatedAt = now))
            }
            // Shortcuts of apps that are installed here; ids are kept so folder items still point at them.
            val installedPackages = installed.values.map { it.packageName to it.userSerial }.toSet()
            val shortcutIds = HashSet<String>()
            root.optJSONArray("shortcuts").objects().forEach { o ->
                val key = o.getString("package_name") to o.getLong("user_serial")
                if (key !in installedPackages) return@forEach
                val id = o.getString("id")
                db.shortcutDao().insert(
                    ShortcutEntity(id, key.first, o.getString("shortcut_id"), key.second, o.optString("label"), now, now),
                )
                shortcutIds += id
            }
            val folderItems = root.optJSONArray("folder_items").objects().mapNotNull { o ->
                val appId = o.optStringOrNull("app_id")
                val shortcutId = o.optStringOrNull("shortcut_id")
                when {
                    appId != null && appId in installed -> FolderItemEntity(
                        o.getString("id"), o.getString("folder_id"), o.getInt("position"), "app", appId = appId, createdAt = now,
                    )
                    shortcutId != null && shortcutId in shortcutIds -> FolderItemEntity(
                        o.getString("id"), o.getString("folder_id"), o.getInt("position"), "shortcut", shortcutId = shortcutId, createdAt = now,
                    )
                    else -> null
                }
            }
            db.folderDao().insertItems(folderItems)

            val homeItems = root.optJSONArray("home_items").objects().mapNotNull { o ->
                val appId = o.optStringOrNull("app_id")
                val folderId = o.optStringOrNull("folder_id")
                if (appId != null && appId !in installed) {
                    skipped++
                    return@mapNotNull null
                }
                HomeItemEntity(o.getString("id"), o.getInt("position"), o.getString("kind"), appId = appId, folderId = folderId, createdAt = now, updatedAt = now)
            }
            db.homeDao().insertAll(homeItems)
            favorites = homeItems.size

            db.iconOverrideDao().insertAll(
                root.optJSONArray("icon_overrides").objects().mapNotNull { o ->
                    val appId = o.getString("app_id")
                    if (appId !in installed) return@mapNotNull null
                    IconOverrideEntity(java.util.UUID.randomUUID().toString(), appId, o.getString("icon_pack"), o.optStringOrNull("drawable_name"), createdAt = now, updatedAt = now)
                }.distinctBy { it.appId to it.iconPack },
            )
            db.usageDao().setRules(
                root.optJSONArray("usage_rules").objects().map { o ->
                    UsageRuleEntity(o.getString("package_name"), o.getLong("user_serial"), now)
                },
            )
        }
        // Older backups have no themes: keep the ones on this phone.
        root.optJSONArray("themes")?.let { arr ->
            db.themeDao().replaceAll(
                arr.objects().map { o ->
                    ThemeEntity(o.getString("id"), o.getString("name"), o.getString("look"), o.optStringOrNull("icon_pack"), o.optLong("created_at", now), now)
                },
            )
        }
        root.optJSONObject("settings")?.let { s -> settings.import(s.keys().asSequence().associateWith { s.get(it) }) }
        return Result(favorites, skipped)
    }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

    companion object {
        const val FORMAT = "driftlist-backup"
        const val VERSION = 1
        const val MIME = "application/json"
    }
}
