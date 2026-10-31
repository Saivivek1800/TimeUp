package com.vivek.reclaim.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ArticleEntity::class, DailyUsageEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class ReclaimDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun dailyUsageDao(): DailyUsageDao

    companion object {
        /**
         * Adds the article summary. Articles are only a cache of the public feeds, so the old rows are cleared and
         * fetched again with summaries. daily_usage (the streak history) is left untouched.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN summary TEXT NOT NULL DEFAULT ''")
                db.execSQL("DELETE FROM articles")
            }
        }
    }
}
