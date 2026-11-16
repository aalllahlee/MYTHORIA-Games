package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PlayerProfileEntity::class,
        LevelProgressEntity::class,
        WorldProgressEntity::class,
        ShopOwnershipEntity::class,
        AchievementEntity::class,
        MissionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE level_progress ADD COLUMN goalMet INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE level_progress ADD COLUMN goalTimeMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS world_progress (" +
                        "worldId INTEGER NOT NULL PRIMARY KEY, " +
                        "unlocked INTEGER NOT NULL DEFAULT 0, " +
                        "gatewayLevelNumber INTEGER NOT NULL DEFAULT 5, " +
                        "gatewayGoalMet INTEGER NOT NULL DEFAULT 0, " +
                        "unlockedAtMs INTEGER NOT NULL DEFAULT 0)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dont_fall_save.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
