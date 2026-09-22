package com.example

import com.example.data.remote.TikTokApiHelper
import org.junit.Assert.*
import org.junit.Test

class TikTokApiHelperTest {

    @Test
    fun testExtractStandardTikTokUrl() {
        val raw = "Check this video https://www.tiktok.com/@tiktok/video/7123456789012345678?is_from_webapp=1"
        val extracted = TikTokApiHelper.extractTikTokUrl(raw)
        assertNotNull(extracted)
        assertTrue(extracted!!.contains("tiktok.com/@tiktok/video/7123456789012345678"))
    }

    @Test
    fun testExtractShortenedTikTokUrl() {
        val raw = "https://vm.tiktok.com/ZM8abc123/"
        val extracted = TikTokApiHelper.extractTikTokUrl(raw)
        assertEquals("https://vm.tiktok.com/ZM8abc123/", extracted)
    }

    @Test
    fun testExtractUsernameHandle() {
        val raw = "@charlidamelio"
        val extracted = TikTokApiHelper.extractTikTokUrl(raw)
        assertEquals("https://www.tiktok.com/@charlidamelio", extracted)
    }

    @Test
    fun testExtractBatchUrls() {
        val rawBatch = """
            https://www.tiktok.com/@user1/video/10001
            https://vm.tiktok.com/ZM8abc123/
            https://www.tiktok.com/@user2/video/10002
        """.trimIndent()

        val list = TikTokApiHelper.extractAllTikTokUrls(rawBatch, maxCount = 5)
        assertEquals(3, list.size)
    }
}
