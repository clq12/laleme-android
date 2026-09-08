package com.laxiang.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RecordEntity::class, SettingsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LaxiangDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
    abstract fun settingsDao(): SettingsDao
}
