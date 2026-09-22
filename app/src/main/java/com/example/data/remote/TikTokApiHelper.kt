package com.example.data.remote

import com.example.data.model.TikTokAuthor
import com.example.data.model.TikTokAudioOption
import com.example.data.model.TikTokMediaResult
import com.example.data.model.TikTokPhotoOption
import com.example.data.model.TikTokSearchResultItem
import com.example.data.model.TikTokVideoOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object TikTokApiHelper {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private val URL_REGEX = Pattern.compile(
        "https?://(?:[a-zA-Z0-9-]+\\.)?(?:tiktok\\.com|douyin\\.com)/[^\\s\"'<>`]+",
        Pattern.CASE_INSENSITIVE
    )

    fun extractTikTokUrl(input: String?): String? {
        if (input.isNullOrBlank()) return null
        var trimmed = input.trim().replace(Regex("^[\"']+|[\"']+$"), "").trim()

        val atMatch = Regex("^@([a-zA-Z0-9_.-]+)$").matchEntire(trimmed)
        if (atMatch != null) {
            return "https://www.tiktok.com/@${atMatch.groupValues[1]}"
        }

        val matcher = URL_REGEX.matcher(trimmed)
        if (matcher.find()) {
            return matcher.group().replace(Regex("[.,;!?)\"'>\\]`]+$"), "")
        }

        if (trimmed.startsWith("tiktok.com/") || trimmed.startsWith("www.tiktok.com/")) {
            return extractTikTokUrl("https://$trimmed")
        }

        return null
    }

    fun extractAllTikTokUrls(input: String?, maxCount: Int = 10): List<String> {
        if (input.isNullOrBlank()) return emptyList()
        val detected = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        val lines = input.split(Regex("[\\r\\n,;]+"))

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val single = extractTikTokUrl(trimmed)
            if (single != null && seen.add(single)) {
                detected.add(single)
                if (detected.size >= maxCount) break
            }
        }
        return detected
    }

    suspend fun unshortenUrl(rawUrl: String): String = withContext(Dispatchers.IO) {
        if (!rawUrl.contains("vm.tiktok.com") && !rawUrl.contains("vt.tiktok.com") && !rawUrl.contains("/t/")) {
            return@withContext rawUrl
        }
        try {
            val req = Request.Builder()
                .url(rawUrl)
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(req).execute().use { response ->
                val finalUrl = response.request.url.toString()
                if (finalUrl.isNotBlank()) finalUrl else rawUrl
            }
        } catch (e: Exception) {
            rawUrl
        }
    }

    suspend fun resolveTikTokMetadata(rawUrl: String): Result<TikTokMediaResult> = withContext(Dispatchers.IO) {
        val cleanUrl = extractTikTokUrl(rawUrl) ?: return@withContext Result.failure(
            IllegalArgumentException("Please enter a valid TikTok link.")
        )
        val resolvedUrl = unshortenUrl(cleanUrl)

        // Strategy 1: TikWM API
        try {
            val endpoint = "https://www.tikwm.com/api/?url=${URLEncoder.encode(resolvedUrl, "UTF-8")}&hd=1"
            val req = Request.Builder()
                .url(endpoint)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .header("Referer", "https://www.tikwm.com/")
                .build()

            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val bodyString = resp.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        val json = JSONObject(bodyString)
                        if (json.optInt("code", -1) == 0 && json.has("data")) {
                            val data = json.getJSONObject("data")
                            val media = parseTikWmData(data, resolvedUrl)
                            return@withContext Result.success(media)
                        } else {
                            val msg = json.optString("msg", "")
                            if (msg.contains("private", ignoreCase = true)) {
                                return@withContext Result.failure(IllegalStateException("This TikTok video is private."))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Fall through to fallback
        }

        // Strategy 2: TikWM via POST
        try {
            val endpoint = "https://www.tikwm.com/api/"
            val postBody = "url=${URLEncoder.encode(resolvedUrl, "UTF-8")}&hd=1"
                .toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())
            val req = Request.Builder()
                .url(endpoint)
                .post(postBody)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()

            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val bodyString = resp.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        val json = JSONObject(bodyString)
                        if (json.optInt("code", -1) == 0 && json.has("data")) {
                            val data = json.getJSONObject("data")
                            val media = parseTikWmData(data, resolvedUrl)
                            return@withContext Result.success(media)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Fall through
        }

        // Strategy 3: Official TikTok oEmbed for basic metadata
        try {
            val oembedUrl = "https://www.tiktok.com/oembed?url=${URLEncoder.encode(resolvedUrl, "UTF-8")}"
            val req = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val bodyString = resp.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        val oembed = JSONObject(bodyString)
                        val title = oembed.optString("title", "")
                        val authorName = oembed.optString("author_name", "tiktok_user")
                        val authorUnique = oembed.optString("author_unique_id", authorName)
                        val thumb = oembed.optString("thumbnail_url", "")
                        val tags = extractHashtags(title)

                        val fallbackMedia = TikTokMediaResult(
                            id = System.currentTimeMillis().toString(),
                            type = "video",
                            title = title,
                            hashtags = tags,
                            author = TikTokAuthor(
                                username = authorUnique,
                                nickname = authorName
                            ),
                            cover = thumb.ifBlank { null },
                            videos = emptyList(),
                            sourceUrl = resolvedUrl
                        )
                        return@withContext Result.success(fallbackMedia)
                    }
                }
            }
        } catch (e: Exception) {
            // Fall through
        }

        Result.failure(Exception("Unable to resolve media from this link. Please check if it is public."))
    }

    private fun parseTikWmData(data: JSONObject, sourceUrl: String): TikTokMediaResult {
        val id = data.optString("id", System.currentTimeMillis().toString())
        val title = data.optString("title", "")
        val cover = data.optString("cover").ifBlank { data.optString("origin_cover").ifBlank { null } }
        val duration = if (data.has("duration")) data.optInt("duration") else null

        val authorObj = data.optJSONObject("author")
        val author = if (authorObj != null) {
            TikTokAuthor(
                username = authorObj.optString("unique_id", authorObj.optString("id", "tiktok_user")),
                nickname = authorObj.optString("nickname").ifBlank { null },
                avatar = authorObj.optString("avatar").ifBlank { null },
                signature = authorObj.optString("signature").ifBlank { null },
                followerCount = if (authorObj.has("follower_count")) authorObj.optLong("follower_count") else null,
                heartCount = if (authorObj.has("heart_count")) authorObj.optLong("heart_count") else null,
                videoCount = if (authorObj.has("video_count")) authorObj.optLong("video_count") else null
            )
        } else {
            TikTokAuthor()
        }

        val hashtags = extractHashtags(title)

        // Photo Carousel check
        val images = data.optJSONArray("images")
        if (images != null && images.length() > 0) {
            val photos = mutableListOf<TikTokPhotoOption>()
            for (i in 0 until images.length()) {
                val pUrl = images.optString(i, "")
                if (pUrl.startsWith("http")) {
                    photos.add(TikTokPhotoOption(index = i + 1, url = pUrl))
                }
            }

            var audioOption: TikTokAudioOption? = null
            val musicUrl = data.optString("music").ifBlank {
                data.optJSONObject("music_info")?.optString("play") ?: ""
            }
            if (musicUrl.startsWith("http")) {
                val mInfo = data.optJSONObject("music_info")
                audioOption = TikTokAudioOption(
                    title = mInfo?.optString("title"),
                    author = mInfo?.optString("author"),
                    url = musicUrl,
                    cover = mInfo?.optString("cover")
                )
            }

            return TikTokMediaResult(
                id = id,
                type = "photo",
                title = title,
                hashtags = hashtags,
                author = author,
                duration = duration,
                cover = cover ?: photos.firstOrNull()?.url,
                photos = photos,
                audio = audioOption,
                sourceUrl = sourceUrl
            )
        }

        // Video options
        val videos = mutableListOf<TikTokVideoOption>()
        val hdPlay = data.optString("hdplay")
        if (hdPlay.isNotBlank()) {
            val fullHd = if (hdPlay.startsWith("http")) hdPlay else "https://www.tikwm.com$hdPlay"
            videos.add(
                TikTokVideoOption(
                    id = "hd",
                    label = "HD No Watermark (Best Quality)",
                    quality = "Best (HD)",
                    url = fullHd,
                    size = if (data.has("hd_size")) data.optLong("hd_size") else null
                )
            )
        }

        val standardPlay = data.optString("play")
        if (standardPlay.isNotBlank()) {
            val fullStandard = if (standardPlay.startsWith("http")) standardPlay else "https://www.tikwm.com$standardPlay"
            videos.add(
                TikTokVideoOption(
                    id = "standard",
                    label = if (videos.isEmpty()) "HD No Watermark" else "Standard (No Watermark)",
                    quality = "Standard",
                    url = fullStandard,
                    size = if (data.has("size")) data.optLong("size") else null
                )
            )
        }

        val wmPlay = data.optString("wmplay")
        if (wmPlay.isNotBlank()) {
            val fullWm = if (wmPlay.startsWith("http")) wmPlay else "https://www.tikwm.com$wmPlay"
            videos.add(
                TikTokVideoOption(
                    id = "watermarked",
                    label = "Watermarked Copy",
                    quality = "Watermarked",
                    url = fullWm,
                    size = if (data.has("wm_size")) data.optLong("wm_size") else null
                )
            )
        }

        var audioOption: TikTokAudioOption? = null
        val musicUrl = data.optString("music").ifBlank {
            data.optJSONObject("music_info")?.optString("play") ?: ""
        }
        if (musicUrl.startsWith("http")) {
            val mInfo = data.optJSONObject("music_info")
            audioOption = TikTokAudioOption(
                title = mInfo?.optString("title"),
                author = mInfo?.optString("author"),
                url = musicUrl,
                cover = mInfo?.optString("cover")
            )
        }

        return TikTokMediaResult(
            id = id,
            type = "video",
            title = title,
            hashtags = hashtags,
            author = author,
            duration = duration,
            cover = cover,
            videos = videos,
            audio = audioOption,
            sourceUrl = sourceUrl
        )
    }

    suspend fun searchPosts(keyword: String, cursor: String = "0", count: Int = 18): Result<List<TikTokSearchResultItem>> = withContext(Dispatchers.IO) {
        val cleanKeyword = keyword.trim()
        if (cleanKeyword.isBlank()) return@withContext Result.success(emptyList())

        val isCreatorQuery = cleanKeyword.startsWith("@")
        val cleanUsername = cleanKeyword.removePrefix("@").trim()

        if (isCreatorQuery) {
            // Fetch creator's specific posts
            try {
                val postEndpoint = "https://www.tikwm.com/api/user/posts"
                val body = "unique_id=${URLEncoder.encode(cleanUsername, "UTF-8")}&count=$count&cursor=$cursor"
                    .toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())
                val req = Request.Builder()
                    .url(postEndpoint)
                    .post(body)
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", "https://www.tikwm.com/")
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val bodyStr = resp.body?.string()
                        if (!bodyStr.isNullOrBlank()) {
                            val json = JSONObject(bodyStr)
                            val vids = json.optJSONObject("data")?.optJSONArray("videos")
                            if (vids != null && vids.length() > 0) {
                                val list = parseVideosArrayToSearchItems(vids)
                                return@withContext Result.success(list)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Continue to challenge
            }
        }

        // Search challenges / topic
        try {
            // First find challenge ID if needed
            var challengeId = ""
            val searchChallengeUrl = "https://www.tikwm.com/api/challenge/search"
            val body1 = "keywords=${URLEncoder.encode(cleanKeyword, "UTF-8")}&count=5&cursor=0"
                .toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())
            val req1 = Request.Builder()
                .url(searchChallengeUrl)
                .post(body1)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "https://www.tikwm.com/")
                .build()

            client.newCall(req1).execute().use { resp1 ->
                if (resp1.isSuccessful) {
                    val s1 = resp1.body?.string()
                    if (!s1.isNullOrBlank()) {
                        val list = JSONObject(s1).optJSONObject("data")?.optJSONArray("challenge_list")
                        if (list != null && list.length() > 0) {
                            challengeId = list.getJSONObject(0).optString("id", "")
                        }
                    }
                }
            }

            if (challengeId.isNotBlank()) {
                val postsUrl = "https://www.tikwm.com/api/challenge/posts"
                val body2 = "challenge_id=${URLEncoder.encode(challengeId, "UTF-8")}&count=$count&cursor=$cursor"
                    .toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())
                val req2 = Request.Builder()
                    .url(postsUrl)
                    .post(body2)
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", "https://www.tikwm.com/")
                    .build()

                client.newCall(req2).execute().use { resp2 ->
                    if (resp2.isSuccessful) {
                        val s2 = resp2.body?.string()
                        if (!s2.isNullOrBlank()) {
                            val vids = JSONObject(s2).optJSONObject("data")?.optJSONArray("videos")
                            if (vids != null && vids.length() > 0) {
                                val list = parseVideosArrayToSearchItems(vids)
                                return@withContext Result.success(list)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Fall through
        }

        // Fallback: search users matching keyword
        try {
            val userSearchUrl = "https://www.tikwm.com/api/user/search"
            val body = "keywords=${URLEncoder.encode(cleanKeyword, "UTF-8")}&count=6&cursor=0"
                .toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())
            val req = Request.Builder()
                .url(userSearchUrl)
                .post(body)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "https://www.tikwm.com/")
                .build()

            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string()
                    if (!bodyStr.isNullOrBlank()) {
                        val users = JSONObject(bodyStr).optJSONObject("data")?.optJSONArray("user_list")
                        if (users != null && users.length() > 0) {
                            val list = mutableListOf<TikTokSearchResultItem>()
                            for (i in 0 until users.length()) {
                                val u = users.getJSONObject(i).optJSONObject("user") ?: continue
                                val uniqueId = u.optString("uniqueId", u.optString("unique_id", "creator"))
                                val nick = u.optString("nickname", uniqueId)
                                val avatar = u.optString("avatarLarger", u.optString("avatarMedium", u.optString("avatarThumb", "")))
                                list.add(
                                    TikTokSearchResultItem(
                                        id = "creator-$uniqueId",
                                        itemType = "creator",
                                        title = u.optString("signature", "@$uniqueId Official Profile"),
                                        cover = avatar,
                                        author = TikTokAuthor(
                                            username = uniqueId,
                                            nickname = nick,
                                            avatar = avatar,
                                            followerCount = if (u.has("followerCount")) u.optLong("followerCount") else null,
                                            signature = u.optString("signature", "")
                                        )
                                    )
                                )
                            }
                            return@withContext Result.success(list)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Fall through
        }

        Result.success(emptyList())
    }

    private fun parseVideosArrayToSearchItems(vids: JSONArray): List<TikTokSearchResultItem> {
        val items = mutableListOf<TikTokSearchResultItem>()
        for (i in 0 until vids.length()) {
            val v = vids.getJSONObject(i)
            val id = v.optString("video_id", v.optString("id", System.currentTimeMillis().toString()))
            val title = v.optString("title", "")
            val cover = v.optString("cover", v.optString("origin_cover", ""))
            val play = v.optString("hdplay", v.optString("play", ""))
            val fullPlay = if (play.startsWith("http")) play else if (play.isNotBlank()) "https://www.tikwm.com$play" else ""
            val fullCover = if (cover.startsWith("http")) cover else if (cover.isNotBlank()) "https://www.tikwm.com$cover" else ""

            val aObj = v.optJSONObject("author")
            val author = if (aObj != null) {
                TikTokAuthor(
                    username = aObj.optString("unique_id", aObj.optString("id", "tiktok_user")),
                    nickname = aObj.optString("nickname", aObj.optString("unique_id", "User")),
                    avatar = aObj.optString("avatar", "")
                )
            } else {
                TikTokAuthor()
            }

            val images = v.optJSONArray("images")
            val isPhoto = images != null && images.length() > 0
            val photoUrls = mutableListOf<String>()
            if (images != null) {
                for (j in 0 until images.length()) {
                    val p = images.optString(j, "")
                    if (p.startsWith("http")) photoUrls.add(p)
                }
            }

            items.add(
                TikTokSearchResultItem(
                    id = id,
                    itemType = if (isPhoto) "photo" else "video",
                    title = title,
                    hashtags = extractHashtags(title),
                    cover = fullCover,
                    duration = if (v.has("duration")) v.optInt("duration") else null,
                    playCount = if (v.has("play_count")) v.optLong("play_count") else null,
                    diggCount = if (v.has("digg_count")) v.optLong("digg_count") else null,
                    downloadUrl = fullPlay,
                    photos = if (photoUrls.isNotEmpty()) photoUrls else null,
                    author = author,
                    createTime = if (v.has("create_time")) v.optLong("create_time") else null
                )
            )
        }
        return items
    }

    private fun extractHashtags(text: String?): List<String> {
        if (text.isNullOrBlank()) return emptyList()
        val tags = mutableListOf<String>()
        val matcher = Pattern.compile("#([\\p{L}\\p{N}_]+)").matcher(text)
        val seen = mutableSetOf<String>()
        while (matcher.find()) {
            val tag = "#${matcher.group(1)}"
            if (seen.add(tag.lowercase())) {
                tags.add(tag)
            }
        }
        return tags
    }
}
