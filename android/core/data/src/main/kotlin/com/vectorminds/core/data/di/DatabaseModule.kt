package com.vectorminds.core.data.di

import android.content.Context
import androidx.room.Room
import com.vectorminds.core.data.db.VectorMindsDatabase
import com.vectorminds.core.data.db.dao.ActionLogDao
import com.vectorminds.core.data.db.dao.TrendCacheDao
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
    fun provideDatabase(@ApplicationContext context: Context): VectorMindsDatabase {
        return Room.databaseBuilder(
            context,
            VectorMindsDatabase::class.java,
            "vectorminds.db"
        ).build()
    }

    @Provides
    fun provideTrendCacheDao(db: VectorMindsDatabase): TrendCacheDao = db.trendCacheDao()

    @Provides
    fun provideActionLogDao(db: VectorMindsDatabase): ActionLogDao = db.actionLogDao()
}
