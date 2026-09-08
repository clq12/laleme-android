package com.laxiang.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int,
    val webdavUrl: String,
    val username: String,
    val password: String,
    val remotePath: String,
    val lastSyncAt: Long?
)
