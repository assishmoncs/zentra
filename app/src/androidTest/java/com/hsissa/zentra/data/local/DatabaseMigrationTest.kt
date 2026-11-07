package com.hsissa.zentra.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migration2To3PreservesHistoryAndMarksItUnfinalized() {
        val name = "migration-test"
        helper.createDatabase(name, 2).apply {
            execSQL("INSERT INTO usage_records (dayTimestamp, totalScreenTimeMillis, weightedScreenTimeMillis, topAppsJson) VALUES (123, 60000, 30000, '[]')")
            close()
        }
        helper.runMigrationsAndValidate(name, 3, true, AppDatabase.MIGRATION_2_3).use { db ->
            db.query("SELECT totalScreenTimeMillis, isFinalized FROM usage_records WHERE dayTimestamp = 123").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(60_000L, cursor.getLong(0))
                assertEquals(0, cursor.getInt(1))
            }
        }
    }
}
