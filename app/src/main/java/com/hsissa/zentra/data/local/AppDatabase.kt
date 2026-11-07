package com.hsissa.zentra.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UsageRecord::class,
        FocusStreakEntity::class,
        AppLimitEntity::class,
        FocusSessionEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Old records may have been captured before the end of the day.
                db.execSQL("ALTER TABLE `usage_records` ADD COLUMN `isFinalized` INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `focus_streaks` (
                        `id` INTEGER NOT NULL,
                        `currentStreak` INTEGER NOT NULL,
                        `longestStreak` INTEGER NOT NULL,
                        `totalFocusMinutes` INTEGER NOT NULL,
                        `lastGoalMetTimestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `app_limits` (
                        `packageName` TEXT NOT NULL,
                        `limitMinutes` INTEGER NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        PRIMARY KEY(`packageName`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `focus_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `completed` INTEGER NOT NULL,
                        `sessionType` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zentra_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
