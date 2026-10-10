package com.laxiang.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "records")
data class RecordEntity(
    @PrimaryKey val id: String,
    val occurredAt: Long,
    val durationSeconds: Int,
    val bristolType: Int,
    val stoolColor: String?,
    val stoolAmount: String?,
    val stoolSmell: String?,
    val bowelFeelings: String?,
    /** 当时的当前状态/诱因，多选，逗号分隔的 id，例如 "cold,heavy_flavor"。 */
    val bodyStatuses: String?,
    val notes: String,
    val photoPath: String?,
    val photoRemoteUrl: String?,
    val photoSyncedAt: Long?,
    val deleted: Boolean,
    val updatedAt: Long
)
