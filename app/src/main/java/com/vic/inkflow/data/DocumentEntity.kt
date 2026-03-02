package com.vic.inkflow.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val uri: String,
    val displayName: String,
    val lastOpenedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val lastPageIndex: Int = 0
)
