package com.example.data.model

data class TikTokAuthor(
    val username: String = "tiktok_user",
    val nickname: String? = null,
    val avatar: String? = null,
    val signature: String? = null,
    val followerCount: Long? = null,
    val heartCount: Long? = null,
    val videoCount: Long? = null
)

data class TikTokVideoOption(
    val id: String,
    val label: String,
    val quality: String,
    val url: String,
    val format: String = "mp4",
    val size: Long? = null
)

data class TikTokPhotoOption(
    val index: Int,
    val url: String
)

data class TikTokAudioOption(
    val title: String? = null,
    val author: String? = null,
    val url: String,
    val format: String = "mp3",
    val cover: String? = null
)

data class TikTokMediaResult(
    val id: String,
    val type: String, // "video" | "photo" | "audio"
    val title: String = "",
    val hashtags: List<String> = emptyList(),
    val author: TikTokAuthor = TikTokAuthor(),
    val duration: Int? = null,
    val cover: String? = null,
    val videos: List<TikTokVideoOption> = emptyList(),
    val photos: List<TikTokPhotoOption> = emptyList(),
    val audio: TikTokAudioOption? = null,
    val sourceUrl: String = "",
    val platform: String = "tiktok", // "tiktok", "youtube", "instagram", "facebook", "manual", etc.
    val isManual: Boolean = false,
    val isShort: Boolean = true,
    val localUri: String? = null
)

data class BulkDownloadItem(
    val id: String,
    val url: String,
    val status: String = "waiting", // "waiting", "resolving", "ready", "downloading", "completed", "failed"
    val selected: Boolean = true,
    val data: TikTokMediaResult? = null,
    val error: String? = null
)

data class TikTokSearchResultItem(
    val id: String,
    val itemType: String = "video", // "video" | "photo" | "creator"
    val title: String = "",
    val hashtags: List<String> = emptyList(),
    val cover: String = "",
    val duration: Int? = null,
    val playCount: Long? = null,
    val diggCount: Long? = null,
    val downloadUrl: String = "",
    val photos: List<String>? = null,
    val author: TikTokAuthor = TikTokAuthor(),
    val createTime: Long? = null
)

data class TranscriptSegment(
    val id: Int = 0,
    val start: String = "00:00",
    val end: String = "00:05",
    val speaker: String = "Speaker 1",
    val text: String = ""
)

data class TranscriptResult(
    val fullTranscript: String = "",
    val cleanScript: String = "",
    val language: String = "Auto-detected",
    val speakers: List<String> = emptyList(),
    val segments: List<TranscriptSegment> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
