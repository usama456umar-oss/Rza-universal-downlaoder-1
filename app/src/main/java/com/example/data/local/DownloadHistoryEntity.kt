package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_history")
data class DownloadHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val username: String,
    val nickname: String? = null,
    val avatarUrl: String? = null,
    val coverUrl: String? = null,
    val mediaType: String, // "video", "photo", "audio", "cover"
    val quality: String,
    val format: String,
    val downloadUrl: String,
    val localFilePath: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
