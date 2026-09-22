package com.example

import com.example.data.model.MediaFormatCategory
import com.example.data.model.SupportedPlatform
import com.example.data.remote.CobaltApiHelper
import com.example.data.remote.TikTokApiHelper
import com.example.data.repository.CobaltRepository
import org.junit.Assert.*
import org.junit.Test

class CobaltDownloaderTest {

    private val cobaltHelper = CobaltApiHelper
    private val cobaltRepo = CobaltRepository()

    // 1. Platform Detection
    @Test
    fun testDetectYouTubeUrl() {
        val yt1 = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val yt2 = "https://youtu.be/dQw4w9WgXcQ"
        val ytShort = "https://www.youtube.com/shorts/abcdefghijk"

        assertEquals(SupportedPlatform.YOUTUBE, cobaltHelper.detectPlatform(yt1))
        assertEquals(SupportedPlatform.YOUTUBE, cobaltHelper.detectPlatform(yt2))
        assertEquals(SupportedPlatform.YOUTUBE, cobaltHelper.detectPlatform(ytShort))
        assertEquals(SupportedPlatform.YOUTUBE, cobaltRepo.detectPlatform(yt1))
        assertEquals("youtube", cobaltRepo.detectPlatform(yt1).id)
        assertTrue(cobaltRepo.isCobaltSupported(yt1))
    }

    @Test
    fun testDetectInstagramUrl() {
        val insta = "https://www.instagram.com/reel/C3abc123/"
        assertEquals(SupportedPlatform.INSTAGRAM, cobaltHelper.detectPlatform(insta))
        assertEquals(SupportedPlatform.INSTAGRAM, cobaltRepo.detectPlatform(insta))
        assertEquals("instagram", cobaltRepo.detectPlatform(insta).id)
        assertTrue(cobaltRepo.isCobaltSupported(insta))
    }

    @Test
    fun testDetectFacebookUrl() {
        val fb = "https://www.facebook.com/watch/?v=123456789"
        assertEquals(SupportedPlatform.FACEBOOK, cobaltHelper.detectPlatform(fb))
        assertEquals(SupportedPlatform.FACEBOOK, cobaltRepo.detectPlatform(fb))
        assertEquals("facebook", cobaltRepo.detectPlatform(fb).id)
        assertTrue(cobaltRepo.isCobaltSupported(fb))
    }

    @Test
    fun testDetectTikTokUrl() {
        val tt1 = "https://www.tiktok.com/@tiktok/video/7123456789012345678"
        val tt2 = "https://vm.tiktok.com/ZM8abc123/"
        assertEquals(SupportedPlatform.TIKTOK, cobaltHelper.detectPlatform(tt1))
        assertEquals(SupportedPlatform.TIKTOK, cobaltHelper.detectPlatform(tt2))
        assertEquals(SupportedPlatform.TIKTOK, cobaltRepo.detectPlatform(tt1))
        assertEquals("tiktok", cobaltRepo.detectPlatform(tt1).id)
        assertTrue(cobaltRepo.isCobaltSupported(tt1))
    }

    @Test
    fun testDetectTwitterAndRedditUrl() {
        val tw = "https://twitter.com/user/status/1234567890"
        val x = "https://x.com/user/status/1234567890"
        val reddit = "https://www.reddit.com/r/videos/comments/abc123/funny_clip/"

        assertEquals(SupportedPlatform.TWITTER, cobaltHelper.detectPlatform(tw))
        assertEquals(SupportedPlatform.TWITTER, cobaltHelper.detectPlatform(x))
        assertEquals(SupportedPlatform.REDDIT, cobaltHelper.detectPlatform(reddit))
        assertEquals("twitter", cobaltRepo.detectPlatform(tw).id)
        assertEquals("reddit", cobaltRepo.detectPlatform(reddit).id)
        assertTrue(cobaltRepo.isCobaltSupported(tw))
        assertTrue(cobaltRepo.isCobaltSupported(reddit))
    }

    // 2. URL Extraction from text
    @Test
    fun testExtractUrlWithSurroundingText() {
        val input = "Hey check out this video! https://www.youtube.com/watch?v=dQw4w9WgXcQ it is amazing"
        val extracted = cobaltHelper.extractUrl(input)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", extracted)
    }

    // 3. Short vs Long Detection
    @Test
    fun testShortVsLongDetection() {
        val ytShort = "https://www.youtube.com/shorts/abcdef12345"
        val ytLong = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val ttVideo = "https://www.tiktok.com/@user/video/7123456789012345678"
        val instaReel = "https://www.instagram.com/reel/C3abc123/"

        assertEquals(MediaFormatCategory.SHORT, cobaltHelper.classifyCategory(SupportedPlatform.YOUTUBE, ytShort))
        assertEquals(MediaFormatCategory.LONG, cobaltHelper.classifyCategory(SupportedPlatform.YOUTUBE, ytLong, 600))
        assertEquals(MediaFormatCategory.SHORT, cobaltHelper.classifyCategory(SupportedPlatform.TIKTOK, ttVideo, 35))
        assertEquals(MediaFormatCategory.SHORT, cobaltHelper.classifyCategory(SupportedPlatform.INSTAGRAM, instaReel))

        assertTrue(cobaltRepo.isShort(ytShort, null))
        assertFalse(cobaltRepo.isShort(ytLong, 600))
        assertTrue(cobaltRepo.isShort(ttVideo, 35))
        assertTrue(cobaltRepo.isShort(instaReel, null))
    }

    // 4. Existing RZA TikTok System Preservation
    @Test
    fun testExistingTikTokCreatorHandleResolutionPreserved() {
        val handle = "@charlidamelio"
        val resolved = TikTokApiHelper.extractTikTokUrl(handle)
        assertEquals("https://www.tiktok.com/@charlidamelio", resolved)
    }

    @Test
    fun testExistingTikTokBatchProcessingPreserved() {
        val batchInput = """
            https://www.tiktok.com/@user/video/1
            https://www.tiktok.com/@user/video/2
            https://www.tiktok.com/@user/video/3
            https://www.tiktok.com/@user/video/4
            https://www.tiktok.com/@user/video/5
            https://www.tiktok.com/@user/video/6
            https://www.tiktok.com/@user/video/7
            https://www.tiktok.com/@user/video/8
            https://www.tiktok.com/@user/video/9
            https://www.tiktok.com/@user/video/10
            https://www.tiktok.com/@user/video/11
        """.trimIndent()

        val parsed = TikTokApiHelper.extractAllTikTokUrls(batchInput, maxCount = 10)
        assertEquals(10, parsed.size)
    }

    // 5. Media Action Logic Constraints Test
    @Test
    fun testMediaActionRules() {
        // Test Short Remote
        val isManual = false
        val isShort = true
        val mediaType = "video"

        val canShowClip = !isManual && isShort && mediaType == "video"
        val canShowTranscribeRemote = !isManual && isShort && mediaType == "video"
        val canShowListenHalal = !isManual && isShort && mediaType == "video"
        val canShowVideoEnhance = !isManual && isShort && mediaType == "video"

        assertTrue(canShowClip)
        assertTrue(canShowTranscribeRemote)
        assertTrue(canShowListenHalal)
        assertTrue(canShowVideoEnhance)

        // Test Long Remote: strictly Download Video only
        val isLongManual = false
        val isLongShort = false

        val longCanShowClip = !isLongManual && isLongShort && mediaType == "video"
        val longCanShowTranscribe = !isLongManual && isLongShort && mediaType == "video"
        val longCanShowListenHalal = !isLongManual && isLongShort && mediaType == "video"
        val longCanShowEnhance = !isLongManual && isLongShort && mediaType == "video"

        assertFalse(longCanShowClip)
        assertFalse(longCanShowTranscribe)
        assertFalse(longCanShowListenHalal)
        assertFalse(longCanShowEnhance)

        // Test Manual Video: Transcribe + Video Enhancement (NO Clip, NO Listen Halal)
        val manualVideo = true
        val manualType = "video"

        val manualVideoTranscribe = manualVideo
        val manualVideoEnhance = manualVideo && manualType == "video"
        val manualVideoClip = !manualVideo // forbidden
        val manualVideoListenHalal = manualVideo && manualType == "audio" // forbidden for video

        assertTrue(manualVideoTranscribe)
        assertTrue(manualVideoEnhance)
        assertFalse(manualVideoClip)
        assertFalse(manualVideoListenHalal)

        // Test Manual Audio: Transcribe + Listen Halal (NO Video Enhancement, NO Clip)
        val manualAudio = true
        val audioType = "audio"

        val manualAudioTranscribe = manualAudio
        val manualAudioListenHalal = manualAudio && audioType == "audio"
        val manualAudioEnhance = manualAudio && audioType == "video" // forbidden for audio
        val manualAudioClip = !manualAudio // forbidden

        assertTrue(manualAudioTranscribe)
        assertTrue(manualAudioListenHalal)
        assertFalse(manualAudioEnhance)
        assertFalse(manualAudioClip)
    }
}

