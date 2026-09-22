package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.CobaltPickerItem
import com.example.data.model.CobaltRequest
import com.example.data.model.CobaltResponse
import com.example.data.model.CobaltServerInfo
import com.example.data.model.MediaFormatCategory
import com.example.data.model.SupportedPlatform
import com.example.data.model.TikTokAudioOption
import com.example.data.model.TikTokAuthor
import com.example.data.model.TikTokMediaResult
import com.example.data.model.TikTokPhotoOption
import com.example.data.model.TikTokVideoOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object CobaltApiHelper {

    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36 RzaDownloader/2.0"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // Server fallback list
    private val DEFAULT_FALLBACK_SERVERS = listOf(
        "https://api.cobalt.tools/",
        "https://cobalt-api.kwiatekm.pl/",
        "https://cobalt.api.scip.dev/"
    )

    @Volatile
    private var customServerUrl: String? = null

    fun setCustomServerUrl(url: String?) {
        customServerUrl = url?.trim()?.takeIf { it.isNotBlank() }
    }

    fun getActiveServerUrl(): String {
        customServerUrl?.let { return if (it.endsWith("/")) it else "$it/" }
        val configUrl = try { BuildConfig.COBALT_API_URL } catch (_: Throwable) { "" }
        if (configUrl.isNotBlank() && configUrl.startsWith("http")) {
            return if (configUrl.endsWith("/")) configUrl else "$configUrl/"
        }
        return DEFAULT_FALLBACK_SERVERS.first()
    }

    private fun getApiKeys(): String {
        return try {
            val key = BuildConfig.COBALT_API_KEY
            if (key.isNotBlank() && !key.contains("MY_COBALT_API_KEY")) key else ""
        } catch (_: Throwable) {
            ""
        }
    }

    /**
     * Extracts a valid URL from arbitrary user input (handles surrounding text or share sheets).
     */
    fun extractUrl(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val pattern = Pattern.compile("https?://[^\\s<>\"']+", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(trimmed)
        return if (matcher.find()) {
            matcher.group().trim()
        } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            null
        }
    }

    /**
     * Detects the platform of an individual media URL.
     */
    fun detectPlatform(url: String): SupportedPlatform {
        val lower = url.lowercase()
        return when {
            lower.contains("youtube.com") || lower.contains("youtu.be") -> SupportedPlatform.YOUTUBE
            lower.contains("tiktok.com") || lower.contains("douyin.com") -> SupportedPlatform.TIKTOK
            lower.contains("instagram.com") -> SupportedPlatform.INSTAGRAM
            lower.contains("facebook.com") || lower.contains("fb.watch") || lower.contains("fb.com") -> SupportedPlatform.FACEBOOK
            lower.contains("twitter.com") || lower.contains("x.com") -> SupportedPlatform.TWITTER
            lower.contains("reddit.com") || lower.contains("v.redd.it") -> SupportedPlatform.REDDIT
            lower.contains("pinterest.com") || lower.contains("pin.it") -> SupportedPlatform.PINTEREST
            lower.contains("soundcloud.com") -> SupportedPlatform.SOUNDCLOUD
            lower.contains("vimeo.com") -> SupportedPlatform.VIMEO
            lower.contains("bilibili.com") -> SupportedPlatform.BILIBILI
            lower.contains("dailymotion.com") -> SupportedPlatform.DAILYMOTION
            lower.contains("twitch.tv") -> SupportedPlatform.TWITCH
            lower.contains("bsky.app") -> SupportedPlatform.BLUESKY
            lower.contains("tumblr.com") -> SupportedPlatform.TUMBLR
            lower.contains("streamable.com") -> SupportedPlatform.STREAMABLE
            lower.contains("vk.com") -> SupportedPlatform.VK
            lower.startsWith("content://") || lower.startsWith("file://") -> SupportedPlatform.MANUAL
            else -> SupportedPlatform.UNKNOWN
        }
    }

    /**
     * Classifies media into SHORT vs LONG vs UNKNOWN.
     * Uses platform-native short-form indicators first.
     * YouTube: /shorts/ -> SHORT; /watch?v= -> LONG unless duration <= 180s.
     * TikTok: Inherently native short-form platform -> SHORT.
     * Instagram: /reel/ or /reels/ -> SHORT.
     * Facebook: /reel/ or /reels/ -> SHORT.
     */
    fun classifyCategory(
        platform: SupportedPlatform,
        url: String,
        durationSeconds: Int? = null
    ): MediaFormatCategory {
        val lower = url.lowercase()

        // 1. Explicit native short-form URL structures
        if (lower.contains("/shorts/")) {
            return MediaFormatCategory.SHORT
        }
        if ((platform == SupportedPlatform.INSTAGRAM || lower.contains("instagram.com")) &&
            (lower.contains("/reel/") || lower.contains("/reels/"))
        ) {
            return MediaFormatCategory.SHORT
        }
        if ((platform == SupportedPlatform.FACEBOOK || lower.contains("facebook.com") || lower.contains("fb.watch")) &&
            (lower.contains("/reel/") || lower.contains("/reels/"))
        ) {
            return MediaFormatCategory.SHORT
        }
        if (platform == SupportedPlatform.TIKTOK || lower.contains("tiktok.com")) {
            // TikTok is natively a short-form video platform
            return MediaFormatCategory.SHORT
        }
        if (platform == SupportedPlatform.TWITCH && (lower.contains("clips.twitch.tv") || lower.contains("/clip/"))) {
            return MediaFormatCategory.SHORT
        }

        // 2. Standard YouTube video
        if (platform == SupportedPlatform.YOUTUBE || lower.contains("youtube.com") || lower.contains("youtu.be")) {
            if (durationSeconds != null) {
                return if (durationSeconds <= 180) MediaFormatCategory.SHORT else MediaFormatCategory.LONG
            }
            return MediaFormatCategory.LONG
        }

        // 3. Reliable duration metadata if present
        if (durationSeconds != null) {
            return if (durationSeconds <= 180) MediaFormatCategory.SHORT else MediaFormatCategory.LONG
        }

        // 4. Safest behavior: If reliable classification is unavailable, return UNKNOWN
        return MediaFormatCategory.UNKNOWN
    }

    /**
     * Sends an individual media URL to Cobalt and resolves the downloadable media.
     * Adheres strictly to Cobalt API contract:
     * POST / with JSON { url, videoQuality, audioFormat, audioBitrate, downloadMode }
     */
    suspend fun resolveMedia(
        rawMediaUrl: String,
        preferredQuality: String = "1080",
        audioFormat: String = "mp3"
    ): Result<TikTokMediaResult> = withContext(Dispatchers.IO) {
        val mediaUrl = extractUrl(rawMediaUrl)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid or empty media URL."))

        val platform = detectPlatform(mediaUrl)

        // Try primary server, then fallbacks
        val serversToTry = buildList {
            add(getActiveServerUrl())
            for (fb in DEFAULT_FALLBACK_SERVERS) {
                val formatted = if (fb.endsWith("/")) fb else "$fb/"
                if (!contains(formatted)) add(formatted)
            }
        }

        var lastError: Exception? = null

        for (baseUrl in serversToTry) {
            try {
                val cobaltResult = executeCobaltRequest(baseUrl, mediaUrl, preferredQuality, audioFormat)
                if (cobaltResult.isSuccess) {
                    val cobaltResponse = cobaltResult.getOrThrow()
                    val mediaResult = mapCobaltResponseToMediaResult(cobaltResponse, mediaUrl, platform)
                    return@withContext Result.success(mediaResult)
                } else {
                    lastError = cobaltResult.exceptionOrNull() as? Exception ?: Exception("Cobalt error")
                    // If it's a platform-specific refusal (e.g. YouTube disabled on this specific instance),
                    // continue to next server or report clearly.
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        val errMsg = lastError?.message ?: "Unable to resolve media from Cobalt server."
        Result.failure(Exception(errMsg, lastError))
    }

    /**
     * Executes the actual Cobalt HTTP request according to Cobalt API v10 / v7 specifications.
     */
    private fun executeCobaltRequest(
        baseUrl: String,
        mediaUrl: String,
        videoQuality: String,
        audioFormat: String
    ): Result<CobaltResponse> {
        val apiKey = getApiKeys()

        // Cobalt v10 standard payload
        val v10Payload = JSONObject().apply {
            put("url", mediaUrl)
            put("videoQuality", videoQuality)
            put("audioFormat", audioFormat)
            put("audioBitrate", "320")
            put("filenameStyle", "auto")
            put("downloadMode", "auto")
            put("youtubeVideoCodec", "h264")
        }

        val requestBody = v10Payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val requestBuilder = Request.Builder()
            .url(baseUrl)
            .post(requestBody)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")

        if (apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Api-Key $apiKey")
            requestBuilder.header("Api-Key", apiKey)
            requestBuilder.header("x-api-key", apiKey)
        }

        val request = requestBuilder.build()

        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()

                if (response.code == 404) {
                    // Try Cobalt v7 endpoint: POST /api/json
                    return executeCobaltV7Fallback(baseUrl, mediaUrl, videoQuality)
                }

                if (responseBody.isBlank()) {
                    return Result.failure(Exception("Cobalt server returned an empty response (HTTP ${response.code})."))
                }

                val json = try {
                    JSONObject(responseBody)
                } catch (e: Exception) {
                    return Result.failure(Exception("Malformed JSON from Cobalt server: $responseBody", e))
                }

                val status = json.optString("status")

                when (status) {
                    "tunnel", "redirect" -> {
                        val downloadUrl = json.optString("url")
                        val filename = json.optString("filename")
                        if (downloadUrl.isBlank()) {
                            return Result.failure(Exception("Cobalt returned $status without a download URL."))
                        }
                        return Result.success(
                            CobaltResponse(
                                status = status,
                                url = downloadUrl,
                                filename = filename.ifBlank { null },
                                rawResponse = responseBody
                            )
                        )
                    }
                    "picker" -> {
                        val pickerArray = json.optJSONArray("picker")
                        val items = mutableListOf<CobaltPickerItem>()
                        if (pickerArray != null) {
                            for (i in 0 until pickerArray.length()) {
                                val itemObj = pickerArray.optJSONObject(i) ?: continue
                                val itemUrl = itemObj.optString("url")
                                val itemType = itemObj.optString("type", "photo")
                                val itemThumb = itemObj.optString("thumb")
                                if (itemUrl.isNotBlank()) {
                                    items.add(CobaltPickerItem(type = itemType, url = itemUrl, thumb = itemThumb.ifBlank { null }))
                                }
                            }
                        }
                        return Result.success(
                            CobaltResponse(
                                status = "picker",
                                picker = items,
                                rawResponse = responseBody
                            )
                        )
                    }
                    "error" -> {
                        val errorObj = json.optJSONObject("error")
                        val code = errorObj?.optString("code") ?: json.optString("text", "Unknown Cobalt error")
                        val context = errorObj?.optJSONObject("context")?.toString()
                        val humanMsg = formatCobaltErrorMessage(code, context)
                        return Result.failure(Exception(humanMsg))
                    }
                    else -> {
                        // Some community instances return "stream" or direct url
                        val streamUrl = json.optString("url")
                        if (streamUrl.isNotBlank()) {
                            return Result.success(
                                CobaltResponse(
                                    status = "stream",
                                    url = streamUrl,
                                    rawResponse = responseBody
                                )
                            )
                        }
                        val errorMsg = json.optString("text", "Unknown Cobalt response status: $status")
                        return Result.failure(Exception(errorMsg))
                    }
                }
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Fallback for older Cobalt v7 instances using POST /api/json
     */
    private fun executeCobaltV7Fallback(
        baseUrl: String,
        mediaUrl: String,
        videoQuality: String
    ): Result<CobaltResponse> {
        val v7Url = if (baseUrl.endsWith("/")) "${baseUrl}api/json" else "$baseUrl/api/json"
        val payload = JSONObject().apply {
            put("url", mediaUrl)
            put("vQuality", videoQuality)
        }

        val request = Request.Builder()
            .url(v7Url)
            .post(payload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful || body.isBlank()) {
                    return Result.failure(Exception("Cobalt v7 endpoint failed (HTTP ${response.code})"))
                }
                val json = JSONObject(body)
                val status = json.optString("status")
                val url = json.optString("url")
                if (status in listOf("stream", "redirect", "tunnel") && url.isNotBlank()) {
                    return Result.success(
                        CobaltResponse(
                            status = status,
                            url = url,
                            rawResponse = body
                        )
                    )
                }
                val errText = json.optString("text", "Cobalt v7 error: $status")
                return Result.failure(Exception(errText))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Formats technical Cobalt error codes into clean, informative messages.
     */
    fun formatCobaltErrorMessage(code: String, context: String?): String {
        return when {
            code.contains("error.api.auth") -> "This Cobalt instance requires an API token. You can configure your self-hosted server in Settings."
            code.contains("error.api.youtube.disabled") -> "YouTube downloads are disabled on this Cobalt server. Please use a self-hosted instance with YouTube enabled."
            code.contains("error.api.unsupported") -> "This link is not supported by the configured Cobalt server."
            code.contains("error.api.rate_exceeded") -> "Cobalt rate limit reached. Please wait a moment or switch instances."
            code.contains("error.api.media.unavailable") || code.contains("error.api.content.private") -> "This media is private, removed, or unavailable."
            code.isNotBlank() -> "Cobalt error: $code${if (!context.isNullOrBlank()) " ($context)" else ""}"
            else -> "Unable to download this media from Cobalt."
        }
    }

    /**
     * Maps a successful CobaltResponse to the app's unified TikTokMediaResult model.
     */
    fun mapCobaltResponseToMediaResult(
        response: CobaltResponse,
        originalUrl: String,
        platform: SupportedPlatform
    ): TikTokMediaResult {
        val category = classifyCategory(platform, originalUrl)
        val isShort = (category == MediaFormatCategory.SHORT)

        val cleanTitle = buildTitle(response.filename, platform, originalUrl)

        if (response.status == "picker") {
            val photos = mutableListOf<TikTokPhotoOption>()
            val videos = mutableListOf<TikTokVideoOption>()

            response.picker.forEachIndexed { index, item ->
                if (item.type == "video") {
                    videos.add(
                        TikTokVideoOption(
                            id = "cobalt_v_${index + 1}",
                            label = "Item ${index + 1} (Video)",
                            quality = "HD",
                            url = item.url
                        )
                    )
                } else {
                    photos.add(
                        TikTokPhotoOption(
                            index = index + 1,
                            url = item.url
                        )
                    )
                }
            }

            return if (photos.isNotEmpty() && videos.isEmpty()) {
                TikTokMediaResult(
                    id = "cobalt_${System.currentTimeMillis()}",
                    type = "photo",
                    title = cleanTitle,
                    cover = photos.firstOrNull()?.url,
                    author = TikTokAuthor(
                        username = platform.displayName.lowercase(),
                        nickname = platform.displayName
                    ),
                    photos = photos,
                    sourceUrl = originalUrl,
                    platform = platform.id,
                    isShort = isShort,
                    isManual = false
                )
            } else {
                TikTokMediaResult(
                    id = "cobalt_${System.currentTimeMillis()}",
                    type = "video",
                    title = cleanTitle,
                    cover = response.picker.firstOrNull()?.thumb,
                    author = TikTokAuthor(
                        username = platform.displayName.lowercase(),
                        nickname = platform.displayName
                    ),
                    videos = videos,
                    photos = photos,
                    sourceUrl = originalUrl,
                    platform = platform.id,
                    isShort = isShort,
                    isManual = false
                )
            }
        }

        // Single video / stream / tunnel
        val downloadUrl = response.url.orEmpty()
        val videoOptions = listOf(
            TikTokVideoOption(
                id = "cobalt_hd",
                label = "Full HD (Cobalt Engine)",
                quality = "HD",
                url = downloadUrl
            )
        )

        val audioOption = TikTokAudioOption(
            title = "$cleanTitle Audio",
            author = platform.displayName,
            url = downloadUrl
        )

        return TikTokMediaResult(
            id = "cobalt_${System.currentTimeMillis()}",
            type = "video",
            title = cleanTitle,
            cover = extractThumbnailGuess(originalUrl, platform),
            author = TikTokAuthor(
                username = platform.displayName.lowercase(),
                nickname = platform.displayName
            ),
            videos = videoOptions,
            audio = audioOption,
            sourceUrl = originalUrl,
            platform = platform.id,
            isShort = isShort,
            isManual = false
        )
    }

    private fun buildTitle(filename: String?, platform: SupportedPlatform, url: String): String {
        if (!filename.isNullOrBlank()) {
            return filename.substringBeforeLast(".")
        }
        return "${platform.displayName} Media"
    }

    private fun extractThumbnailGuess(url: String, platform: SupportedPlatform): String? {
        if (platform == SupportedPlatform.YOUTUBE) {
            val videoId = extractYouTubeVideoId(url)
            if (videoId != null) {
                return "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
            }
        }
        return null
    }

    fun extractYouTubeVideoId(url: String): String? {
        val patterns = listOf(
            Pattern.compile("youtube\\.com/watch\\?v=([a-zA-Z0-9_-]{11})"),
            Pattern.compile("youtu\\.be/([a-zA-Z0-9_-]{11})"),
            Pattern.compile("youtube\\.com/shorts/([a-zA-Z0-9_-]{11})"),
            Pattern.compile("youtube\\.com/embed/([a-zA-Z0-9_-]{11})")
        )
        for (pattern in patterns) {
            val m = pattern.matcher(url)
            if (m.find()) {
                return m.group(1)
            }
        }
        return null
    }

    /**
     * Checks server connectivity and fetches service capabilities.
     */
    suspend fun checkServerHealth(serverUrl: String = getActiveServerUrl()): Result<CobaltServerInfo> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(serverUrl)
            .get()
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code} from server"))
                }
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val cobaltObj = json.optJSONObject("cobalt")
                val version = cobaltObj?.optString("version") ?: json.optString("version", "Cobalt")
                val services = mutableListOf<String>()
                val sArray = cobaltObj?.optJSONArray("services")
                if (sArray != null) {
                    for (i in 0 until sArray.length()) {
                        services.add(sArray.optString(i))
                    }
                }
                return@withContext Result.success(
                    CobaltServerInfo(
                        version = version,
                        url = serverUrl,
                        services = services,
                        isReachable = true
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
