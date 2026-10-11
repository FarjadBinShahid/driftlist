package dev.farjad.driftlist.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AppEntity::class, HomeItemEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class DriftlistDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun homeDao(): HomeDao

    companion object {
        fun create(context: Context): DriftlistDatabase =
            Room.databaseBuilder(context, DriftlistDatabase::class.java, "driftlist.db").build()
    }
}
