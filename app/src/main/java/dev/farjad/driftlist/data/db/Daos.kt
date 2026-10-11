package dev.farjad.driftlist.data.db

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

    @Query("select * from app where hidden = 0")
    abstract fun observeVisible(): Flow<List<AppEntity>>

    // ABORT, never REPLACE: REPLACE deletes the row first, which would cascade to home_item.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(apps: List<AppEntity>)

    @Update
    abstract suspend fun update(apps: List<AppEntity>)

    @Query("delete from app where id in (:ids)")
    abstract suspend fun delete(ids: List<String>)

    @Query("update app set launch_count = launch_count + 1, last_launched_at = :now where id = :id")
    abstract suspend fun markLaunched(id: String, now: Long)

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
    @Query(
        """
        select app.* from home_item
        join app on app.id = home_item.app_id
        where app.hidden = 0
        order by home_item.position
        """,
    )
    abstract fun observeFavorites(): Flow<List<AppEntity>>

    @Query("select coalesce(max(position), -1) from home_item")
    abstract suspend fun maxPosition(): Int

    @Query("select count(*) from home_item where app_id = :appId")
    abstract suspend fun countApp(appId: String): Int

    @Insert
    abstract suspend fun insert(item: HomeItemEntity)

    @Query("delete from home_item where app_id = :appId")
    abstract suspend fun removeApp(appId: String)

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
}
