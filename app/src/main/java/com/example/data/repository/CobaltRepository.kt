package com.example.data.repository

import com.example.data.model.CobaltServerInfo
import com.example.data.model.SupportedPlatform
import com.example.data.model.TikTokMediaResult
import com.example.data.remote.CobaltApiHelper

class CobaltRepository {

    suspend fun resolveMedia(
        url: String,
        preferredQuality: String = "1080"
    ): Result<TikTokMediaResult> {
        return CobaltApiHelper.resolveMedia(url, preferredQuality)
    }

    fun detectPlatform(url: String): SupportedPlatform {
        return CobaltApiHelper.detectPlatform(url)
    }

    fun isCobaltSupported(url: String): Boolean {
        val platform = detectPlatform(url)
        return platform != SupportedPlatform.UNKNOWN && platform != SupportedPlatform.MANUAL
    }

    fun isShort(url: String, durationSec: Int? = null): Boolean {
        val platform = detectPlatform(url)
        return CobaltApiHelper.classifyCategory(platform, url, durationSec) == com.example.data.model.MediaFormatCategory.SHORT
    }

    fun extractUrl(input: String): String? {
        return CobaltApiHelper.extractUrl(input)
    }

    suspend fun checkServerHealth(): Result<CobaltServerInfo> {
        return CobaltApiHelper.checkServerHealth()
    }

    fun setCustomServerUrl(url: String?) {
        CobaltApiHelper.setCustomServerUrl(url)
    }

    fun getActiveServerUrl(): String {
        return CobaltApiHelper.getActiveServerUrl()
    }
}
