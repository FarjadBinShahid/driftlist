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

/** Ordered favorites. Milestone 1 only stores apps; folders, stacks and shortcuts arrive with their tables. */
@Entity(
    tableName = "home_item",
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            parentColumns = ["id"],
            childColumns = ["app_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["position"]),
        Index(value = ["app_id"], unique = true),
    ],
)
data class HomeItemEntity(
    @PrimaryKey val id: String,
    val position: Int,
    val kind: String = KIND_APP,
    @ColumnInfo(name = "app_id") val appId: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
) {
    companion object {
        const val KIND_APP = "app"
    }
}
