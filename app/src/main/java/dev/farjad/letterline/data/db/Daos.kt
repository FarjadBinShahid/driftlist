package dev.farjad.letterline.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
abstract class AppDao {
    @Query("select * from app")
    abstract suspend fun all(): List<AppEntity>

    @Query("select * from app")
    abstract fun observeAll(): Flow<List<AppEntity>>

    // ABORT, never REPLACE: REPLACE deletes the row first, which would cascade to home_item.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(apps: List<AppEntity>)

    @Update
    abstract suspend fun update(apps: List<AppEntity>)

    @Query("delete from app where id in (:ids)")
    abstract suspend fun delete(ids: List<String>)

    @Query("update app set launch_count = launch_count + 1, last_launched_at = :now where id = :id")
    abstract suspend fun markLaunched(id: String, now: Long)

    @Query("update app set custom_label = :label, updated_at = :now where id = :id")
    abstract suspend fun setCustomLabel(id: String, label: String?, now: Long)

    @Query("update app set hidden = :hidden, updated_at = :now where id = :id")
    abstract suspend fun setHidden(id: String, hidden: Boolean, now: Long)

    @Query("update app set hidden_from_suggestions = :hidden, updated_at = :now where id = :id")
    abstract suspend fun setHiddenFromSuggestions(id: String, hidden: Boolean, now: Long)

    /**
     * Make the table match what LauncherApps reports, keeping the user's own edits
     * (rename, hidden, counters) on apps that are still installed.
     */
    @Transaction
    open suspend fun sync(fresh: List<AppEntity>) {
        val existing = all().associateBy { it.id }
        val freshIds = fresh.mapTo(HashSet()) { it.id }

        val toInsert = ArrayList<AppEntity>()
        val toUpdate = ArrayList<AppEntity>()
        for (app in fresh) {
            val old = existing[app.id]
            if (old == null) {
                toInsert += app
            } else if (old.systemLabel != app.systemLabel || old.sortKey != app.sortKey ||
                old.bucket != app.bucket || old.isSystem != app.isSystem
            ) {
                toUpdate += old.copy(
                    systemLabel = app.systemLabel,
                    sortKey = app.sortKey,
                    bucket = app.bucket,
                    isSystem = app.isSystem,
                    updatedAt = app.updatedAt,
                )
            }
        }
        val toDelete = existing.keys.filter { it !in freshIds }

        if (toDelete.isNotEmpty()) delete(toDelete)
        if (toUpdate.isNotEmpty()) update(toUpdate)
        if (toInsert.isNotEmpty()) insert(toInsert)
    }
}

@Dao
abstract class HomeDao {
    @Query("select * from home_item order by position")
    abstract fun observeItems(): Flow<List<HomeItemEntity>>

    @Query("select * from home_item order by position")
    abstract suspend fun items(): List<HomeItemEntity>

    @Query("select coalesce(max(position), -1) from home_item")
    abstract suspend fun maxPosition(): Int

    @Query("select count(*) from home_item where app_id = :appId")
    abstract suspend fun countApp(appId: String): Int

    @Insert
    abstract suspend fun insert(item: HomeItemEntity)

    @Insert
    abstract suspend fun insertAll(items: List<HomeItemEntity>)

    @Query("delete from home_item where app_id = :appId")
    abstract suspend fun removeApp(appId: String)

    @Query("delete from home_item where id = :id")
    abstract suspend fun remove(id: String)

    @Query("delete from home_item")
    abstract suspend fun clear()

    @Query("update home_item set position = :position, updated_at = :now where id = :id")
    abstract suspend fun setPosition(id: String, position: Int, now: Long)

    @Transaction
    open suspend fun addApp(appId: String, now: Long) {
        if (countApp(appId) > 0) return
        insert(
            HomeItemEntity(
                id = UUID.randomUUID().toString(),
                position = maxPosition() + 1,
                appId = appId,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    @Transaction
    open suspend fun addFolder(folderId: String, now: Long) {
        insert(
            HomeItemEntity(
                id = UUID.randomUUID().toString(),
                position = maxPosition() + 1,
                kind = HomeItemEntity.KIND_FOLDER,
                folderId = folderId,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    /** Rewrites every position in one go, never patches (see architecture.md "Races"). */
    @Transaction
    open suspend fun reorder(orderedIds: List<String>, now: Long) {
        orderedIds.forEachIndexed { index, id -> setPosition(id, index, now) }
    }

    /** Replace favorites with these apps, in order (onboarding). */
    @Transaction
    open suspend fun replaceWithApps(appIds: List<String>, now: Long) {
        clear()
        insertAll(
            appIds.mapIndexed { i, appId ->
                HomeItemEntity(UUID.randomUUID().toString(), i, appId = appId, createdAt = now, updatedAt = now)
            },
        )
    }
}

@Dao
abstract class ShortcutDao {
    @Query("select * from shortcut")
    abstract fun observeAll(): Flow<List<ShortcutEntity>>

    @Query("select * from shortcut")
    abstract suspend fun all(): List<ShortcutEntity>

    @Query("select * from shortcut where package_name = :pkg and shortcut_id = :shortcutId and user_serial = :serial")
    abstract suspend fun find(pkg: String, shortcutId: String, serial: Long): ShortcutEntity?

    @Insert
    abstract suspend fun insert(shortcut: ShortcutEntity)

    /** Shortcuts no folder or favorite uses any more. */
    @Query(
        """
        delete from shortcut where id not in (select shortcut_id from folder_item where shortcut_id is not null)
        and id not in (select shortcut_id from home_item where shortcut_id is not null)
        """,
    )
    abstract suspend fun deleteUnused()

    @Transaction
    open suspend fun getOrCreate(pkg: String, shortcutId: String, serial: Long, label: String, now: Long): ShortcutEntity {
        find(pkg, shortcutId, serial)?.let { return it }
        val entity = ShortcutEntity(UUID.randomUUID().toString(), pkg, shortcutId, serial, label, now, now)
        insert(entity)
        return entity
    }
}

@Dao
abstract class FolderDao {
    @Query("select * from popup_folder")
    abstract fun observeFolders(): Flow<List<PopupFolderEntity>>

    @Query("select * from folder_item order by folder_id, position")
    abstract fun observeItems(): Flow<List<FolderItemEntity>>

    @Query("select * from popup_folder")
    abstract suspend fun folders(): List<PopupFolderEntity>

    @Query("select * from folder_item")
    abstract suspend fun items(): List<FolderItemEntity>

    @Insert
    abstract suspend fun insertFolder(folder: PopupFolderEntity)

    @Insert
    abstract suspend fun insertItems(items: List<FolderItemEntity>)

    @Query("update popup_folder set title = :title, updated_at = :now where id = :id")
    abstract suspend fun rename(id: String, title: String, now: Long)

    @Query("delete from popup_folder where id = :id")
    abstract suspend fun delete(id: String)

    @Query("delete from folder_item where folder_id = :folderId and kind = 'app'")
    abstract suspend fun clearApps(folderId: String)

    @Query("select coalesce(max(position), -1) from folder_item where folder_id = :folderId")
    abstract suspend fun maxPosition(folderId: String): Int

    @Query("delete from folder_item where folder_id = :folderId and shortcut_id = :shortcutId")
    abstract suspend fun removeShortcut(folderId: String, shortcutId: String)

    @Query("select count(*) from folder_item where folder_id = :folderId and shortcut_id = :shortcutId")
    abstract suspend fun countShortcut(folderId: String, shortcutId: String): Int

    /** Replaces the folder's apps; shortcuts in it are kept and stay after the apps. */
    @Transaction
    open suspend fun setApps(folderId: String, appIds: List<String>, now: Long) {
        clearApps(folderId)
        insertItems(
            appIds.mapIndexed { i, appId ->
                FolderItemEntity(UUID.randomUUID().toString(), folderId, i - appIds.size, "app", appId = appId, createdAt = now)
            },
        )
    }

    @Transaction
    open suspend fun addShortcut(folderId: String, shortcutId: String, now: Long) {
        if (countShortcut(folderId, shortcutId) > 0) return
        insertItems(
            listOf(
                FolderItemEntity(UUID.randomUUID().toString(), folderId, maxPosition(folderId) + 1, "shortcut", shortcutId = shortcutId, createdAt = now),
            ),
        )
    }
}

data class HeaderWidget(val stackId: String, val widget: WidgetEntity)

@Dao
abstract class WidgetDao {
    @Query(
        """
        select widget.* from widget
        join widget_stack on widget_stack.id = widget.stack_id
        where widget_stack.location = 'header'
        order by widget.position
        """,
    )
    abstract fun observeHeaderWidgets(): Flow<List<WidgetEntity>>

    /** Widgets in folders and app pop-ups (stack ids "folder:<id>" and "app:<appId>"). */
    @Query(
        """
        select widget.* from widget
        join widget_stack on widget_stack.id = widget.stack_id
        where widget_stack.location != 'header'
        order by widget.position
        """,
    )
    abstract fun observeOtherWidgets(): Flow<List<WidgetEntity>>

    @Query("select * from widget_stack where id = :id")
    abstract suspend fun stack(id: String): WidgetStackEntity?

    @Query("select * from widget where stack_id = :stackId")
    abstract suspend fun widgetsIn(stackId: String): List<WidgetEntity>

    @Query("delete from widget_stack where id = :id")
    abstract suspend fun deleteStack(id: String)

    /** Adds a widget to a named stack, creating it if needed (max 4). False when full. */
    @Transaction
    open suspend fun addWidgetTo(stackId: String, location: String, appWidgetId: Int, provider: String, userSerial: Long, now: Long): Boolean {
        if (stack(stackId) == null) insertStack(WidgetStackEntity(stackId, location, createdAt = now, updatedAt = now))
        val count = countIn(stackId)
        if (count >= MAX_STACK) return false
        insertWidget(
            WidgetEntity(UUID.randomUUID().toString(), stackId, appWidgetId, provider, userSerial, position = count, createdAt = now, updatedAt = now),
        )
        return true
    }

    @Query("select * from widget_stack")
    abstract suspend fun stacks(): List<WidgetStackEntity>

    @Query("select * from widget")
    abstract suspend fun widgets(): List<WidgetEntity>

    @Query("select app_widget_id from widget")
    abstract suspend fun boundIds(): List<Int>

    @Insert
    abstract suspend fun insertStack(stack: WidgetStackEntity)

    @Insert
    abstract suspend fun insertWidget(widget: WidgetEntity)

    @Query("delete from widget_stack where location = 'header'")
    abstract suspend fun clearHeader()

    @Query("select * from widget_stack where location = 'header' limit 1")
    abstract suspend fun headerStack(): WidgetStackEntity?

    @Query("select count(*) from widget where stack_id = :stackId")
    abstract suspend fun countIn(stackId: String): Int

    @Query("delete from widget where app_widget_id = :appWidgetId")
    abstract suspend fun deleteWidget(appWidgetId: Int)

    /** Adds a widget to the header stack (max 4). Returns false when the stack is full. */
    @Transaction
    open suspend fun addHeaderWidget(appWidgetId: Int, provider: String, userSerial: Long, now: Long): Boolean {
        val stack = headerStack() ?: WidgetStackEntity(UUID.randomUUID().toString(), WidgetStackEntity.HEADER, createdAt = now, updatedAt = now)
            .also { insertStack(it) }
        val count = countIn(stack.id)
        if (count >= MAX_STACK) return false
        insertWidget(
            WidgetEntity(UUID.randomUUID().toString(), stack.id, appWidgetId, provider, userSerial, position = count, createdAt = now, updatedAt = now),
        )
        return true
    }

    companion object {
        const val MAX_STACK = 4
    }

    /** Milestone 2 keeps exactly one header widget: replace whatever was there. */
    @Transaction
    open suspend fun setHeaderWidget(appWidgetId: Int, provider: String, userSerial: Long, now: Long) {
        clearHeader()
        val stackId = UUID.randomUUID().toString()
        insertStack(WidgetStackEntity(stackId, WidgetStackEntity.HEADER, createdAt = now, updatedAt = now))
        insertWidget(
            WidgetEntity(
                id = UUID.randomUUID().toString(),
                stackId = stackId,
                appWidgetId = appWidgetId,
                provider = provider,
                userSerial = userSerial,
                position = 0,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }
}

@Dao
abstract class SummaryDao {
    @Query("select * from summary_rule")
    abstract fun observeRules(): Flow<List<SummaryRuleEntity>>

    @Query("select * from summary_rule")
    abstract suspend fun rules(): List<SummaryRuleEntity>

    @Insert
    abstract suspend fun insertRules(rules: List<SummaryRuleEntity>)

    @Query("delete from summary_rule")
    abstract suspend fun clearRules()

    @Transaction
    open suspend fun setRules(rules: List<SummaryRuleEntity>) {
        clearRules()
        insertRules(rules)
    }

    @Query("select * from notification_batch where delivered_at is null limit 1")
    abstract suspend fun openBatch(): NotificationBatchEntity?

    @Insert
    abstract suspend fun insertBatch(batch: NotificationBatchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertItem(item: BatchItemEntity)

    @Query("select * from batch_item where batch_id = :batchId order by posted_at")
    abstract suspend fun items(batchId: String): List<BatchItemEntity>

    @Query("update notification_batch set delivered_at = :now where id = :batchId")
    abstract suspend fun markDelivered(batchId: String, now: Long)

    @Query("delete from notification_batch where delivered_at is not null")
    abstract suspend fun deleteDelivered()

    /** Adds to the open batch, opening one (with its delivery time) if needed. Returns the batch. */
    @Transaction
    open suspend fun hold(item: BatchItemEntity, newBatch: () -> NotificationBatchEntity): NotificationBatchEntity {
        val batch = openBatch() ?: newBatch().also { insertBatch(it) }
        upsertItem(item.copy(batchId = batch.id))
        return batch
    }
}

@Dao
abstract class IconOverrideDao {
    @Query("select * from icon_override")
    abstract fun observeAll(): Flow<List<IconOverrideEntity>>

    @Query("select * from icon_override")
    abstract suspend fun all(): List<IconOverrideEntity>

    @Insert
    abstract suspend fun insert(item: IconOverrideEntity)

    @Insert
    abstract suspend fun insertAll(items: List<IconOverrideEntity>)

    @Query("delete from icon_override where app_id = :appId and icon_pack = :pack")
    abstract suspend fun delete(appId: String, pack: String)

    @Transaction
    open suspend fun set(appId: String, pack: String, drawableName: String, now: Long) {
        delete(appId, pack)
        insert(IconOverrideEntity(UUID.randomUUID().toString(), appId, pack, drawableName, createdAt = now, updatedAt = now))
    }
}

@Dao
abstract class UsageDao {
    @Query("select * from usage_rule")
    abstract fun observeRules(): Flow<List<UsageRuleEntity>>

    @Query("select * from usage_rule")
    abstract suspend fun rules(): List<UsageRuleEntity>

    @Insert
    abstract suspend fun insertRules(rules: List<UsageRuleEntity>)

    @Query("delete from usage_rule")
    abstract suspend fun clearRules()

    @Transaction
    open suspend fun setRules(rules: List<UsageRuleEntity>) {
        clearRules()
        insertRules(rules)
    }

    @Query("select * from usage_session where ended_at is null limit 1")
    abstract suspend fun openSession(): UsageSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveSession(session: UsageSessionEntity)

    @Query("delete from usage_session where ended_at is not null and started_at < :before")
    abstract suspend fun deleteEndedBefore(before: Long)
}

@Dao
abstract class ThemeDao {
    @Query("select * from theme order by updated_at desc")
    abstract fun observeAll(): Flow<List<ThemeEntity>>

    @Query("select * from theme order by updated_at desc")
    abstract suspend fun all(): List<ThemeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(theme: ThemeEntity)

    @Query("update theme set name = :name, updated_at = :now where id = :id")
    abstract suspend fun rename(id: String, name: String, now: Long)

    @Query("delete from theme where id = :id")
    abstract suspend fun delete(id: String)

    @Query("delete from theme")
    abstract suspend fun clear()

    @Transaction
    open suspend fun replaceAll(themes: List<ThemeEntity>) {
        clear()
        themes.forEach { upsert(it) }
    }
}
