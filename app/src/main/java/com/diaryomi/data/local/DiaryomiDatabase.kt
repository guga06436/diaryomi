package com.diaryomi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.diaryomi.data.local.converter.Converters
import com.diaryomi.data.local.entity.SearchResultEntity
import com.diaryomi.data.local.entity.TrackedSearchEntity

@Database(
    entities = [TrackedSearchEntity::class, SearchResultEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DiaryomiDatabase : RoomDatabase() {
    abstract fun trackedSearchDao(): TrackedSearchDao
    abstract fun searchResultDao(): SearchResultDao
}
