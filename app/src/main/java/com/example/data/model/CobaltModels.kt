package com.example.data.model

enum class SupportedPlatform(
    val id: String,
    val displayName: String,
    val badge: String,
    val hasNativeShorts: Boolean
) {
    YOUTUBE("youtube", "YouTube", "▶ YouTube", true),
    TIKTOK("tiktok", "TikTok", "♫ TikTok", true),
    INSTAGRAM("instagram", "Instagram", "📷 Instagram", true),
    FACEBOOK("facebook", "Facebook", "👥 Facebook", true),
    TWITTER("twitter", "X (Twitter)", "𝕏 Twitter", false),
    REDDIT("reddit", "Reddit", "🤖 Reddit", false),
    PINTEREST("pinterest", "Pinterest", "📌 Pinterest", false),
    SOUNDCLOUD("soundcloud", "SoundCloud", "☁ SoundCloud", false),
    VIMEO("vimeo", "Vimeo", "🎬 Vimeo", false),
    BILIBILI("bilibili", "Bilibili", "📺 Bilibili", false),
    DAILYMOTION("dailymotion", "Dailymotion", "🎥 Dailymotion", false),
    TWITCH("twitch", "Twitch", "🎮 Twitch", true),
    BLUESKY("bluesky", "Bluesky", "🦋 Bluesky", false),
    TUMBLR("tumblr", "Tumblr", "📝 Tumblr", false),
    STREAMABLE("streamable", "Streamable", "▶ Streamable", false),
    VK("vk", "VK", "📱 VK", false),
    MANUAL("manual", "Local Media", "📁 Device File", false),
    UNKNOWN("unknown", "Web Media", "🌐 Web Media", false)
}

enum class MediaFormatCategory {
    SHORT,
    LONG,
    UNKNOWN
}

data class CobaltRequest(
    val url: String,
    val videoQuality: String = "1080",
    val audioFormat: String = "mp3",
    val audioBitrate: String = "320",
    val filenameStyle: String = "auto",
    val downloadMode: String = "auto",
    val youtubeVideoCodec: String = "h264"
)

data class CobaltPickerItem(
    val type: String, // "photo", "video"
    val url: String,
    val thumb: String? = null
)

data class CobaltResponse(
    val status: String, // "tunnel", "redirect", "picker", "error", "stream"
    val url: String? = null,
    val filename: String? = null,
    val picker: List<CobaltPickerItem> = emptyList(),
    val errorCode: String? = null,
    val errorContext: String? = null,
    val rawResponse: String? = null
)

data class CobaltServerInfo(
    val version: String,
    val url: String,
    val services: List<String> = emptyList(),
    val isReachable: Boolean = true
)
