package com.vivek.reclaim.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "articles", indices = [Index("interest"), Index("publishedAt")])
data class ArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val source: String,
    val url: String,
    val imageUrl: String?,
    val interest: String,
    val publishedAt: Long,
    val readMinutes: Int,
    val isRead: Boolean = false,
    /** A short plain-text teaser from the feed, shown on the featured article. */
    @ColumnInfo(defaultValue = "''") val summary: String = "",
)

/** One row per local day, used for streaks and history. [date] is ISO yyyy-MM-dd. */
@Entity(tableName = "daily_usage")
data class DailyUsageEntity(
    @PrimaryKey val date: String,
    val totalMinutes: Int,
    val withinLimit: Boolean,
)
