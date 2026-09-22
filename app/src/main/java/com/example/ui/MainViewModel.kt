package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.DownloadHistoryEntity
import com.example.data.model.*
import com.example.data.remote.GeminiTranscribeService
import com.example.data.remote.TikTokApiHelper
import com.example.data.repository.CobaltRepository
import com.example.data.repository.DownloadRepository
import com.example.data.repository.TikTokRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val downloadRepo = DownloadRepository(application)
    private val tikTokRepo = TikTokRepository()
    private val cobaltRepo = CobaltRepository()

    // Navigation Tab
    private val _currentTab = MutableStateFlow("downloader")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Downloader State
    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _resultData = MutableStateFlow<TikTokMediaResult?>(null)
    val resultData: StateFlow<TikTokMediaResult?> = _resultData.asStateFlow()

    // Batch Links State
    private val _bulkItems = MutableStateFlow<List<BulkDownloadItem>>(emptyList())
    val bulkItems: StateFlow<List<BulkDownloadItem>> = _bulkItems.asStateFlow()

    private val _isBulkActive = MutableStateFlow(false)
    val isBulkActive: StateFlow<Boolean> = _isBulkActive.asStateFlow()

    private val _isBulkQueueRunning = MutableStateFlow(false)
    val isBulkQueueRunning: StateFlow<Boolean> = _isBulkQueueRunning.asStateFlow()

    // Next Creator Video state
    private val _seenVideoIds = MutableStateFlow<List<String>>(emptyList())
    private val _isLoadingNext = MutableStateFlow(false)
    val isLoadingNext: StateFlow<Boolean> = _isLoadingNext.asStateFlow()

    // AI Transcription State
    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

    private val _transcriptResult = MutableStateFlow<TranscriptResult?>(null)
    val transcriptResult: StateFlow<TranscriptResult?> = _transcriptResult.asStateFlow()

    private val _transcribeError = MutableStateFlow<String?>(null)
    val transcribeError: StateFlow<String?> = _transcribeError.asStateFlow()

    // Search State
    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    private val _searchResults = MutableStateFlow<List<TikTokSearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<TikTokSearchResultItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private val _searchCursor = MutableStateFlow("0")
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // Download History State from Room
    val historyList: StateFlow<List<DownloadHistoryEntity>> = downloadRepo.allHistory
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    fun onUrlChange(input: String) {
        _urlInput.value = input
        _errorMessage.value = null
    }

    fun clearDownloader() {
        _urlInput.value = ""
        _errorMessage.value = null
        _resultData.value = null
        _bulkItems.value = emptyList()
        _isBulkActive.value = false
        _transcriptResult.value = null
    }

    fun submitSingle(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            _errorMessage.value = "Please enter a valid URL or creator handle."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _resultData.value = null
            _isBulkActive.value = false
            _transcriptResult.value = null

            // 1. TikTok Creator Handle Resolution (@username)
            if (trimmed.startsWith("@")) {
                val res = tikTokRepo.resolveSingleUrl(trimmed)
                _isLoading.value = false
                if (res.isSuccess) {
                    val media = res.getOrNull()
                    _resultData.value = media
                    if (media != null) {
                        _seenVideoIds.value = _seenVideoIds.value + media.id
                    }
                } else {
                    _errorMessage.value = res.exceptionOrNull()?.message ?: "Unable to find creator $trimmed"
                }
                return@launch
            }

            // 2. Extract valid URL from input
            val extractedUrl = cobaltRepo.extractUrl(trimmed) ?: trimmed
            val platform = cobaltRepo.detectPlatform(extractedUrl)

            // 3. Platform Routing
            if (platform == SupportedPlatform.TIKTOK) {
                // For individual TikTok video URLs, attempt Cobalt first
                val cobaltRes = cobaltRepo.resolveMedia(extractedUrl)
                if (cobaltRes.isSuccess) {
                    _isLoading.value = false
                    val media = cobaltRes.getOrNull()
                    _resultData.value = media
                    if (media != null) {
                        _seenVideoIds.value = _seenVideoIds.value + media.id
                    }
                    return@launch
                }

                // If Cobalt fails or is unavailable, smoothly fallback to TikWM
                val tikClean = TikTokApiHelper.extractTikTokUrl(extractedUrl) ?: extractedUrl
                val tikTokRes = tikTokRepo.resolveSingleUrl(tikClean)
                _isLoading.value = false
                if (tikTokRes.isSuccess) {
                    val media = tikTokRes.getOrNull()
                    _resultData.value = media
                    if (media != null) {
                        _seenVideoIds.value = _seenVideoIds.value + media.id
                    }
                } else {
                    val err = cobaltRes.exceptionOrNull()?.message
                        ?: tikTokRes.exceptionOrNull()?.message
                        ?: "Unable to resolve TikTok media."
                    _errorMessage.value = err
                }
                return@launch
            }

            // 4. Cobalt supported individual media (YouTube, Instagram, Facebook, etc.)
            val cobaltRes = cobaltRepo.resolveMedia(extractedUrl)
            _isLoading.value = false
            if (cobaltRes.isSuccess) {
                val media = cobaltRes.getOrNull()
                _resultData.value = media
                if (media != null) {
                    _seenVideoIds.value = _seenVideoIds.value + media.id
                }
            } else {
                _errorMessage.value = cobaltRes.exceptionOrNull()?.message
                    ?: "Unable to resolve media from ${platform.displayName}."
            }
        }
    }

    fun loadManualMedia(
        uri: android.net.Uri,
        isVideo: Boolean,
        title: String? = null,
        durationSeconds: Int? = null
    ) {
        val cleanTitle = title?.ifBlank { null } ?: if (isVideo) "Local Video" else "Local Audio"
        val isShort = if (isVideo) (durationSeconds == null || durationSeconds <= 180) else false

        _resultData.value = TikTokMediaResult(
            id = "manual_${System.currentTimeMillis()}",
            type = if (isVideo) "video" else "audio",
            title = cleanTitle,
            author = TikTokAuthor(
                username = "local_device",
                nickname = "Device Media"
            ),
            sourceUrl = uri.toString(),
            localUri = uri.toString(),
            platform = "manual",
            isShort = isShort,
            isManual = true,
            videos = if (isVideo) listOf(
                TikTokVideoOption(
                    id = "manual_file",
                    label = "Device Video File",
                    quality = "Original",
                    url = uri.toString()
                )
            ) else emptyList(),
            audio = if (!isVideo) TikTokAudioOption(
                title = cleanTitle,
                author = "Local Audio",
                url = uri.toString()
            ) else null
        )
        _errorMessage.value = null
        _transcriptResult.value = null
    }

    fun setCustomCobaltServer(url: String?) {
        cobaltRepo.setCustomServerUrl(url)
    }

    fun getActiveCobaltServer(): String {
        return cobaltRepo.getActiveServerUrl()
    }

    fun submitBatch(input: String) {
        val extractedUrls = TikTokApiHelper.extractAllTikTokUrls(input, maxCount = 10)
        if (extractedUrls.isEmpty()) {
            _errorMessage.value = "No valid TikTok links found in the text."
            return
        }

        val initialItems = extractedUrls.mapIndexed { idx, u ->
            BulkDownloadItem(
                id = "${System.currentTimeMillis()}-$idx",
                url = u,
                status = "resolving"
            )
        }

        _bulkItems.value = initialItems
        _isBulkActive.value = true
        _resultData.value = null
        _errorMessage.value = null

        viewModelScope.launch {
            val updated = initialItems.toMutableList()
            for (i in updated.indices) {
                val resolved = tikTokRepo.resolveBulkItem(updated[i])
                updated[i] = resolved
                _bulkItems.value = updated.toList()
            }
        }
    }

    fun toggleBulkSelect(id: String) {
        _bulkItems.value = _bulkItems.value.map {
            if (it.id == id) it.copy(selected = !it.selected) else it
        }
    }

    fun selectAllBulk() {
        _bulkItems.value = _bulkItems.value.map { it.copy(selected = true) }
    }

    fun selectNoneBulk() {
        _bulkItems.value = _bulkItems.value.map { it.copy(selected = false) }
    }

    fun removeBulkItem(id: String) {
        _bulkItems.value = _bulkItems.value.filter { it.id != id }
    }

    fun startBulkDownload() {
        val toDownload = _bulkItems.value.filter { it.selected && it.data != null }
        if (toDownload.isEmpty()) return

        viewModelScope.launch {
            _isBulkQueueRunning.value = true
            toDownload.forEach { item ->
                val media = item.data ?: return@forEach
                val bestVideo = media.videos.firstOrNull()
                if (bestVideo != null) {
                    downloadRepo.downloadMediaFile(
                        mediaUrl = bestVideo.url,
                        type = "video",
                        title = media.title,
                        username = media.author.username,
                        nickname = media.author.nickname,
                        avatarUrl = media.author.avatar,
                        coverUrl = media.cover,
                        quality = bestVideo.quality
                    )
                } else if (media.photos.isNotEmpty()) {
                    media.photos.firstOrNull()?.let { p ->
                        downloadRepo.downloadMediaFile(
                            mediaUrl = p.url,
                            type = "photo",
                            title = media.title,
                            username = media.author.username,
                            coverUrl = p.url,
                            quality = "Photo"
                        )
                    }
                }
            }
            _isBulkQueueRunning.value = false
            _bulkItems.value = _bulkItems.value.map {
                if (it.selected && it.status == "ready") it.copy(status = "completed") else it
            }
        }
    }

    fun downloadVideo(videoOption: TikTokVideoOption) {
        val media = _resultData.value ?: return
        viewModelScope.launch {
            downloadRepo.downloadMediaFile(
                mediaUrl = videoOption.url,
                type = "video",
                title = media.title,
                username = media.author.username,
                nickname = media.author.nickname,
                avatarUrl = media.author.avatar,
                coverUrl = media.cover,
                quality = videoOption.quality,
                platform = media.platform
            )
        }
    }

    fun downloadAudio(audioUrl: String) {
        val media = _resultData.value ?: return
        viewModelScope.launch {
            downloadRepo.downloadMediaFile(
                mediaUrl = audioUrl,
                type = "audio",
                title = media.audio?.title ?: "${media.title} (Audio)",
                username = media.author.username,
                nickname = media.author.nickname,
                avatarUrl = media.author.avatar,
                coverUrl = media.audio?.cover ?: media.cover,
                quality = "MP3",
                platform = media.platform
            )
        }
    }

    fun downloadCover(coverUrl: String) {
        val media = _resultData.value ?: return
        viewModelScope.launch {
            downloadRepo.downloadMediaFile(
                mediaUrl = coverUrl,
                type = "cover",
                title = "${media.title} (Cover)",
                username = media.author.username,
                nickname = media.author.nickname,
                avatarUrl = media.author.avatar,
                coverUrl = coverUrl,
                quality = "HD Cover",
                platform = media.platform
            )
        }
    }

    fun downloadPhoto(photoUrl: String, index: Int) {
        val media = _resultData.value ?: return
        viewModelScope.launch {
            downloadRepo.downloadMediaFile(
                mediaUrl = photoUrl,
                type = "photo",
                title = "${media.title} (Photo #$index)",
                username = media.author.username,
                nickname = media.author.nickname,
                avatarUrl = media.author.avatar,
                coverUrl = photoUrl,
                quality = "HD",
                index = index,
                platform = media.platform
            )
        }
    }

    fun loadNextVideo() {
        val media = _resultData.value ?: return
        val username = media.author.username
        if (username.isBlank() || media.platform != "tiktok") return

        viewModelScope.launch {
            _isLoadingNext.value = true
            val nextRes = tikTokRepo.loadNextCreatorVideo(username, _seenVideoIds.value)
            _isLoadingNext.value = false
            if (nextRes.isSuccess) {
                val nextMedia = nextRes.getOrNull()
                if (nextMedia != null) {
                    _resultData.value = nextMedia
                    _seenVideoIds.value = _seenVideoIds.value + nextMedia.id
                    _transcriptResult.value = null
                }
            } else {
                _errorMessage.value = nextRes.exceptionOrNull()?.message ?: "No more videos found for @$username"
            }
        }
    }

    fun transcribeCurrentMedia() {
        val media = _resultData.value ?: return
        val context = buildString {
            if (media.isManual) {
                append("Local Media Title: \"${media.title}\". Format: ${media.type}. ")
            } else {
                val platName = media.platform.replaceFirstChar { it.uppercase() }
                append("$platName Media by @${media.author.username}. ")
                if (media.title.isNotBlank()) append("Title: \"${media.title}\". ")
                if (media.hashtags.isNotEmpty()) append("Hashtags: ${media.hashtags.joinToString(" ")}. ")
            }
        }
        runTranscription(context, "auto", true, true)
    }

    fun runTranscription(input: String, lang: String, detectSpeakers: Boolean, timestamps: Boolean) {
        viewModelScope.launch {
            _isTranscribing.value = true
            _transcribeError.value = null
            _transcriptResult.value = null

            val res = GeminiTranscribeService.transcribeSpeechOrCaption(
                inputContext = input,
                targetLanguage = lang,
                detectSpeakers = detectSpeakers,
                includeTimestamps = timestamps
            )
            _isTranscribing.value = false
            if (res.isSuccess) {
                _transcriptResult.value = res.getOrNull()
            } else {
                _transcribeError.value = res.exceptionOrNull()?.message ?: "AI transcription error."
            }
        }
    }

    fun closeTranscript() {
        _transcriptResult.value = null
        _transcribeError.value = null
    }

    // Search Functions
    fun onKeywordChange(kw: String) {
        _searchKeyword.value = kw
        _searchError.value = null
    }

    fun search(keyword: String) {
        if (keyword.isBlank()) return
        viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null
            _searchCursor.value = "0"

            val res = tikTokRepo.searchContent(keyword, cursor = "0")
            _isSearching.value = false
            if (res.isSuccess) {
                val items = res.getOrNull().orEmpty()
                _searchResults.value = items
                if (items.isEmpty()) {
                    _searchError.value = "No results found for \"$keyword\"."
                }
            } else {
                _searchError.value = res.exceptionOrNull()?.message ?: "Search failed."
            }
        }
    }

    fun loadMoreSearch() {
        val kw = _searchKeyword.value
        if (kw.isBlank() || _isLoadingMore.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            val nextCursor = (_searchResults.value.size).toString()
            val res = tikTokRepo.searchContent(kw, cursor = nextCursor)
            _isLoadingMore.value = false
            if (res.isSuccess) {
                val items = res.getOrNull().orEmpty()
                if (items.isNotEmpty()) {
                    _searchResults.value = _searchResults.value + items
                }
            }
        }
    }

    fun downloadSearchItem(item: TikTokSearchResultItem, type: String) {
        viewModelScope.launch {
            val url = if (type == "cover") item.cover else item.downloadUrl
            if (url.isNotBlank()) {
                downloadRepo.downloadMediaFile(
                    mediaUrl = url,
                    type = type,
                    title = item.title,
                    username = item.author.username,
                    nickname = item.author.nickname,
                    avatarUrl = item.author.avatar,
                    coverUrl = item.cover,
                    quality = if (type == "cover") "HD Cover" else "HD"
                )
            }
        }
    }

    fun selectSearchItemForDownloader(itemResult: TikTokMediaResult) {
        _resultData.value = itemResult
        _currentTab.value = "downloader"
    }

    // History Functions
    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            downloadRepo.deleteHistoryItem(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            downloadRepo.clearAllHistory()
        }
    }

    fun redownloadHistory(item: DownloadHistoryEntity) {
        viewModelScope.launch {
            downloadRepo.downloadMediaFile(
                mediaUrl = item.downloadUrl,
                type = item.mediaType,
                title = item.title,
                username = item.username,
                nickname = item.nickname,
                avatarUrl = item.avatarUrl,
                coverUrl = item.coverUrl,
                quality = item.quality
            )
        }
    }
}
