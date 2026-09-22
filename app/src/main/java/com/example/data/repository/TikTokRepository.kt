package com.example.data.repository

import com.example.data.model.BulkDownloadItem
import com.example.data.model.TikTokMediaResult
import com.example.data.model.TikTokSearchResultItem
import com.example.data.remote.TikTokApiHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TikTokRepository {

    suspend fun resolveSingleUrl(url: String): Result<TikTokMediaResult> {
        return TikTokApiHelper.resolveTikTokMetadata(url)
    }

    suspend fun resolveBulkItem(item: BulkDownloadItem): BulkDownloadItem = withContext(Dispatchers.IO) {
        val res = TikTokApiHelper.resolveTikTokMetadata(item.url)
        if (res.isSuccess) {
            item.copy(status = "ready", data = res.getOrNull(), error = null)
        } else {
            item.copy(status = "failed", error = res.exceptionOrNull()?.message ?: "Failed to resolve media stream")
        }
    }

    suspend fun searchContent(
        keyword: String,
        cursor: String = "0"
    ): Result<List<TikTokSearchResultItem>> {
        return TikTokApiHelper.searchPosts(keyword, cursor)
    }

    suspend fun loadNextCreatorVideo(
        username: String,
        seenIds: List<String>
    ): Result<TikTokMediaResult> = withContext(Dispatchers.IO) {
        val searchRes = TikTokApiHelper.searchPosts("@$username", "0", 20)
        if (searchRes.isSuccess) {
            val items = searchRes.getOrNull().orEmpty().filter { it.itemType != "creator" }
            val unseen = items.firstOrNull { it.id !in seenIds }
            if (unseen != null) {
                val media = TikTokMediaResult(
                    id = unseen.id,
                    type = unseen.itemType,
                    title = unseen.title,
                    hashtags = unseen.hashtags,
                    author = unseen.author,
                    duration = unseen.duration,
                    cover = unseen.cover,
                    videos = if (unseen.downloadUrl.isNotBlank()) listOf(
                        com.example.data.model.TikTokVideoOption(
                            id = "hd",
                            label = "HD (Best Available)",
                            quality = "HD",
                            url = unseen.downloadUrl
                        )
                    ) else emptyList(),
                    photos = unseen.photos?.mapIndexed { idx, p ->
                        com.example.data.model.TikTokPhotoOption(index = idx + 1, url = p)
                    } ?: emptyList(),
                    sourceUrl = "https://www.tiktok.com/@${unseen.author.username}/video/${unseen.id}"
                )
                return@withContext Result.success(media)
            }
        }
        Result.failure(Exception("No more videos available from @$username"))
    }
}
