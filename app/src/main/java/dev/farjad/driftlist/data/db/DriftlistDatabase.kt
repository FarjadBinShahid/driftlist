package dev.farjad.driftlist.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AppEntity::class,
        HomeItemEntity::class,
        ShortcutEntity::class,
        WidgetStackEntity::class,
        WidgetEntity::class,
        PopupFolderEntity::class,
        FolderItemEntity::class,
        IconOverrideEntity::class,
        UsageRuleEntity::class,
        UsageSessionEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class DriftlistDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun homeDao(): HomeDao
    abstract fun folderDao(): FolderDao
    abstract fun widgetDao(): WidgetDao
    abstract fun iconOverrideDao(): IconOverrideDao
    abstract fun usageDao(): UsageDao

    companion object {
        fun create(context: Context): DriftlistDatabase =
            Room.databaseBuilder(context, DriftlistDatabase::class.java, "driftlist.db").build()
    }
}
