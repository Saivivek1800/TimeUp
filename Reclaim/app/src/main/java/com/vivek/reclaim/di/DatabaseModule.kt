package com.vivek.reclaim.di

import android.content.Context
import androidx.room.Room
import com.vivek.reclaim.data.db.ArticleDao
import com.vivek.reclaim.data.db.DailyUsageDao
import com.vivek.reclaim.data.db.ReclaimDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ReclaimDatabase =
        Room.databaseBuilder(context, ReclaimDatabase::class.java, "reclaim.db")
            .addMigrations(ReclaimDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideArticleDao(db: ReclaimDatabase): ArticleDao = db.articleDao()

    @Provides
    fun provideDailyUsageDao(db: ReclaimDatabase): DailyUsageDao = db.dailyUsageDao()
}
