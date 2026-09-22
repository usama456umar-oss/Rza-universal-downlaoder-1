package com.example.data.repository

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DownloadRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.downloadHistoryDao()

    val allHistory: Flow<List<DownloadHistoryEntity>> = dao.getAllHistory()

    suspend fun downloadMediaFile(
        mediaUrl: String,
        type: String, // "video", "photo", "audio", "cover"
        title: String,
        username: String,
        nickname: String? = null,
        avatarUrl: String? = null,
        coverUrl: String? = null,
        quality: String = "HD",
        index: Int? = null,
        platform: String = "tiktok"
    ): Result<Long> = withContext(Dispatchers.IO) {
        if (mediaUrl.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Media download URL is empty."))
        }

        try {
            val cleanUser = username.removePrefix("@").replace(Regex("[^a-zA-Z0-9_]"), "_").ifBlank { "User" }
            val cleanQual = quality.replace(Regex("[^a-zA-Z0-9]"), "")
            val ext = when (type.lowercase()) {
                "audio" -> "mp3"
                "photo", "cover", "avatar" -> "jpg"
                else -> "mp4"
            }
            val mime = when (type.lowercase()) {
                "audio" -> "audio/mpeg"
                "photo", "cover", "avatar" -> "image/jpeg"
                else -> "video/mp4"
            }
            val plat = if (platform.isNotBlank()) platform.replaceFirstChar { it.uppercase() } else "Media"
            val idxSuffix = if (index != null) "_Photo_${String.format("%02d", index)}" else ""
            val filename = "Rza_${plat}_${cleanUser}_${cleanQual}${idxSuffix}.${ext}"

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                ?: return@withContext Result.failure(IllegalStateException("DownloadManager unavailable."))

            val request = DownloadManager.Request(Uri.parse(mediaUrl))
                .setTitle(filename)
                .setDescription("Downloading $type ($plat)")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "RzaDownloader/$filename")
                .setMimeType(mime)
                .addRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")

            if (mediaUrl.contains("tikwm.com")) {
                request.addRequestHeader("Referer", "https://www.tikwm.com/")
            } else if (mediaUrl.contains("tiktok.com")) {
                request.addRequestHeader("Referer", "https://www.tiktok.com/")
            }

            val downloadId = downloadManager.enqueue(request)

            // Persist to local Room history
            val historyEntity = DownloadHistoryEntity(
                title = title.ifBlank { "$plat ${type.replaceFirstChar { it.uppercase() }}" },
                username = username,
                nickname = nickname,
                avatarUrl = avatarUrl,
                coverUrl = coverUrl ?: if (type == "photo" || type == "cover") mediaUrl else null,
                mediaType = type,
                quality = quality,
                format = ext.uppercase(),
                downloadUrl = mediaUrl,
                localFilePath = "Downloads/RzaDownloader/$filename",
                timestamp = System.currentTimeMillis()
            )
            dao.insert(historyEntity)

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Download started: $filename", Toast.LENGTH_SHORT).show()
            }

            Result.success(downloadId)
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Download error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
            Result.failure(e)
        }
    }

    suspend fun deleteHistoryItem(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }
}
