package com.vivek.reclaim.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    /** Unread first, then newest, filtered to the user's interests. */
    @Query("SELECT * FROM articles WHERE interest IN (:interests) ORDER BY isRead ASC, publishedAt DESC")
    fun observeByInterests(interests: Set<String>): Flow<List<ArticleEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNew(articles: List<ArticleEntity>)

    @Query("UPDATE articles SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("DELETE FROM articles WHERE publishedAt < :olderThan AND isRead = 1")
    suspend fun deleteReadOlderThan(olderThan: Long)

    @Query("DELETE FROM articles WHERE publishedAt < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)

    @Query("SELECT COUNT(*) FROM articles")
    suspend fun count(): Int

    @Query("SELECT DISTINCT interest FROM articles")
    suspend fun cachedInterests(): List<String>
}

@Dao
interface DailyUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(usage: DailyUsageEntity)

    @Query("SELECT * FROM daily_usage ORDER BY date DESC LIMIT :days")
    fun observeRecent(days: Int): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage ORDER BY date DESC LIMIT :days")
    suspend fun recent(days: Int): List<DailyUsageEntity>
}
