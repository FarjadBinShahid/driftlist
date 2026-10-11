package dev.farjad.driftlist.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One launchable activity in one profile. Mirrors `app` in replica/schema.sql.
 *
 * The id is deterministic ("package/activity#userSerial") rather than a random uuid, so the
 * same app keeps the same id across re-syncs, reinstalls of the launcher and backup restores.
 */
@Entity(
    tableName = "app",
    indices = [
        Index(value = ["package_name", "activity_class", "user_serial"], unique = true),
        Index(value = ["package_name", "user_serial"]),
        Index(value = ["hidden", "bucket", "sort_key"]),
        Index(value = ["installed_at"]),
        Index(value = ["hidden_from_suggestions", "launch_count"]),
    ],
)
data class AppEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "activity_class") val activityClass: String,
    @ColumnInfo(name = "user_serial") val userSerial: Long,
    @ColumnInfo(name = "system_label") val systemLabel: String,
    @ColumnInfo(name = "custom_label") val customLabel: String? = null,
    @ColumnInfo(name = "sort_key") val sortKey: String,
    val bucket: String,
    val hidden: Boolean = false,
    @ColumnInfo(name = "hidden_from_suggestions") val hiddenFromSuggestions: Boolean = false,
    @ColumnInfo(name = "is_system") val isSystem: Boolean = false,
    @ColumnInfo(name = "installed_at") val installedAt: Long,
    @ColumnInfo(name = "last_launched_at") val lastLaunchedAt: Long? = null,
    @ColumnInfo(name = "launch_count") val launchCount: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
) {
    val label: String get() = customLabel ?: systemLabel

    companion object {
        fun idFor(packageName: String, activityClass: String, userSerial: Long) =
            "$packageName/$activityClass#$userSerial"
    }
}

/** Pinned deep shortcut (LauncherApps). Label cached so it renders before LauncherApps answers. */
@Entity(
    tableName = "shortcut",
    indices = [Index(value = ["package_name", "shortcut_id", "user_serial"], unique = true)],
)
data class ShortcutEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "shortcut_id") val shortcutId: String,
    @ColumnInfo(name = "user_serial") val userSerial: Long,
    val label: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(tableName = "widget_stack")
data class WidgetStackEntity(
    @PrimaryKey val id: String,
    /** 'header' (under the clock), 'home', 'app_popup' or 'folder'. */
    val location: String,
    @ColumnInfo(name = "two_column") val twoColumn: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
) {
    companion object {
        const val HEADER = "header"
    }
}

@Entity(
    tableName = "widget",
    foreignKeys = [
        ForeignKey(WidgetStackEntity::class, ["id"], ["stack_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [
        Index(value = ["stack_id", "position"]),
        Index(value = ["app_widget_id"], unique = true),
    ],
)
data class WidgetEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "stack_id") val stackId: String,
    @ColumnInfo(name = "app_widget_id") val appWidgetId: Int,
    val provider: String,
    @ColumnInfo(name = "user_serial") val userSerial: Long,
    val position: Int,
    @ColumnInfo(name = "height_dp") val heightDp: Int? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "popup_folder",
    foreignKeys = [
        ForeignKey(WidgetStackEntity::class, ["id"], ["widget_stack_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index(value = ["widget_stack_id"])],
)
data class PopupFolderEntity(
    @PrimaryKey val id: String,
    val title: String,
    @ColumnInfo(name = "widget_stack_id") val widgetStackId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** Folder contents. No nested folders, by design. */
@Entity(
    tableName = "folder_item",
    foreignKeys = [
        ForeignKey(PopupFolderEntity::class, ["id"], ["folder_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(AppEntity::class, ["id"], ["app_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ShortcutEntity::class, ["id"], ["shortcut_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [
        Index(value = ["folder_id", "position"]),
        Index(value = ["app_id"]),
        Index(value = ["shortcut_id"]),
    ],
)
data class FolderItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "folder_id") val folderId: String,
    val position: Int,
    val kind: String,
    @ColumnInfo(name = "app_id") val appId: String? = null,
    @ColumnInfo(name = "shortcut_id") val shortcutId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** Ordered favorites. Exactly one of the target columns is set, matching [kind]. */
@Entity(
    tableName = "home_item",
    foreignKeys = [
        ForeignKey(AppEntity::class, ["id"], ["app_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(PopupFolderEntity::class, ["id"], ["folder_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(WidgetStackEntity::class, ["id"], ["widget_stack_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ShortcutEntity::class, ["id"], ["shortcut_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [
        Index(value = ["position"]),
        Index(value = ["app_id"], unique = true),
        Index(value = ["folder_id"]),
        Index(value = ["widget_stack_id"]),
        Index(value = ["shortcut_id"]),
    ],
)
data class HomeItemEntity(
    @PrimaryKey val id: String,
    val position: Int,
    val kind: String = KIND_APP,
    @ColumnInfo(name = "app_id") val appId: String? = null,
    @ColumnInfo(name = "folder_id", defaultValue = "NULL") val folderId: String? = null,
    @ColumnInfo(name = "widget_stack_id", defaultValue = "NULL") val widgetStackId: String? = null,
    @ColumnInfo(name = "shortcut_id", defaultValue = "NULL") val shortcutId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
) {
    companion object {
        const val KIND_APP = "app"
        const val KIND_FOLDER = "folder"
    }
}

/** Per-app, per-icon-pack choice, so switching packs and back restores the user's picks. */
@Entity(
    tableName = "icon_override",
    foreignKeys = [ForeignKey(AppEntity::class, ["id"], ["app_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["app_id", "icon_pack"], unique = true)],
)
data class IconOverrideEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "app_id") val appId: String,
    /** Icon pack package name, or [SYSTEM] for "system icons". */
    @ColumnInfo(name = "icon_pack") val iconPack: String,
    @ColumnInfo(name = "drawable_name") val drawableName: String?,
    @ColumnInfo(name = "custom_uri") val customUri: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
) {
    companion object {
        const val SYSTEM = "system"
    }
}

@Entity(tableName = "usage_rule", primaryKeys = ["package_name", "user_serial"])
data class UsageRuleEntity(
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "user_serial") val userSerial: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "usage_session",
    indices = [
        Index(value = ["package_name", "user_serial", "ended_at"]),
        Index(value = ["started_at"]),
    ],
)
data class UsageSessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "user_serial") val userSerial: Long,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "last_seen_at") val lastSeenAt: Long,
    @ColumnInfo(name = "last_reminder_minute") val lastReminderMinute: Int = 0,
    @ColumnInfo(name = "ended_at") val endedAt: Long? = null,
)
