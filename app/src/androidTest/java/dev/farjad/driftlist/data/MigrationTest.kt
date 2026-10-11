package dev.farjad.driftlist.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.farjad.driftlist.data.db.DriftlistDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** P-1 / F01-E3: people upgrading from 0.1.0 (schema v1) keep their apps and favorites. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DriftlistDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun v1ToV2KeepsAppsAndFavorites() {
        helper.createDatabase(DB, 1).apply {
            execSQL(
                """insert into app (id, package_name, activity_class, user_serial, system_label, custom_label,
                   sort_key, bucket, hidden, hidden_from_suggestions, is_system, installed_at, last_launched_at,
                   launch_count, created_at, updated_at)
                   values ('a', 'pkg.a', 'Main', 0, 'Alpha', 'Mine', 'alpha', 'A', 1, 0, 0, 1, null, 3, 1, 1)""",
            )
            execSQL("insert into home_item (id, position, kind, app_id, created_at, updated_at) values ('h', 0, 'app', 'a', 1, 1)")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 2, true)

        db.query("select custom_label, hidden, launch_count from app where id = 'a'").use { c ->
            c.moveToFirst()
            assertEquals("Mine", c.getString(0))
            assertEquals(1, c.getInt(1))
            assertEquals(3, c.getInt(2))
        }
        db.query("select app_id, kind, folder_id from home_item").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertEquals("a", c.getString(0))
            assertEquals("app", c.getString(1))
            assertEquals(true, c.isNull(2))
        }
        db.close()
    }

    @Test
    fun v2ToV3KeepsFavoritesAndAddsSummaryTables() {
        helper.createDatabase(DB, 2).apply {
            execSQL(
                """insert into app (id, package_name, activity_class, user_serial, system_label, custom_label,
                   sort_key, bucket, hidden, hidden_from_suggestions, is_system, installed_at, last_launched_at,
                   launch_count, created_at, updated_at)
                   values ('a', 'pkg.a', 'Main', 0, 'Alpha', null, 'alpha', 'A', 0, 0, 0, 1, null, 0, 1, 1)""",
            )
            execSQL("insert into home_item (id, position, kind, app_id, created_at, updated_at) values ('h', 0, 'app', 'a', 1, 1)")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 3, true)

        db.query("select count(*) from home_item").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        db.execSQL("insert into summary_rule (package_name, user_serial) values ('pkg.a', 0)")
        db.query("select count(*) from summary_rule").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        db.close()
    }

    @Test
    fun v3ToV4KeepsSummaryRulesAndAddsThemes() {
        helper.createDatabase(DB, 3).apply {
            execSQL("insert into summary_rule (package_name, user_serial) values ('pkg.a', 0)")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 4, true)

        db.query("select count(*) from summary_rule").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        db.execSQL("insert into theme (id, name, look, icon_pack, created_at, updated_at) values ('t', 'Mine', '{}', null, 1, 1)")
        db.query("select name from theme").use { c -> c.moveToFirst(); assertEquals("Mine", c.getString(0)) }
        db.close()
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
