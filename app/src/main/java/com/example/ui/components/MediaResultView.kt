package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.TikTokMediaResult
import com.example.data.model.TikTokVideoOption
import com.example.data.model.TranscriptResult
import com.example.ui.theme.*

@Composable
fun MediaResultView(
    data: TikTokMediaResult,
    onClear: () -> Unit,
    onDownloadVideo: (videoOption: TikTokVideoOption) -> Unit,
    onDownloadAudio: (audioUrl: String) -> Unit,
    onDownloadCover: (coverUrl: String) -> Unit,
    onDownloadPhoto: (photoUrl: String, index: Int) -> Unit,
    onLoadNextVideo: () -> Unit,
    isLoadingNext: Boolean,
    onTranscribe: () -> Unit,
    isTranscribing: Boolean,
    transcriptResult: TranscriptResult?,
    onCloseTranscript: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedVideoId by remember(data) {
        mutableStateOf(data.videos.firstOrNull()?.id ?: "standard")
    }

    val selectedVideo = data.videos.find { it.id == selectedVideoId } ?: data.videos.firstOrNull()

    // Clipboard copy helpers
    var copiedTitle by remember { mutableStateOf(false) }
    var copiedTags by remember { mutableStateOf(false) }
    var copiedUser by remember { mutableStateOf(false) }
    var copiedLink by remember { mutableStateOf(false) }
    var copiedAll by remember { mutableStateOf(false) }

    val copyText = { text: String, label: String ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied!", Toast.LENGTH_SHORT).show()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = RzaSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RzaPrimary.copy(alpha = 0.35f)))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status bar + Reset action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusBadgeText = when {
                    data.isManual && data.type == "video" -> "LOCAL VIDEO READY"
                    data.isManual && data.type == "audio" -> "LOCAL AUDIO READY"
                    data.type == "photo" -> "${data.platform.uppercase()} PHOTO CAROUSEL"
                    data.isShort -> "${data.platform.uppercase()} SHORT VIDEO"
                    else -> "${data.platform.uppercase()} LONG VIDEO"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (data.isShort) RzaAccentEmerald else RzaPrimary)
                    )
                    Text(
                        text = statusBadgeText,
                        color = RzaPrimaryGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }

                FilledTonalButton(
                    onClick = onClear,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.1f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (data.isManual) "Clear" else "New Link", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Creator / Source Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Creator / Source Avatar
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(RzaSurfaceElevated)
                            .border(1.5.dp, RzaPrimary.copy(alpha = 0.5f), CircleShape)
                    ) {
                        if (!data.author.avatar.isNullOrBlank()) {
                            AsyncImage(
                                model = data.author.avatar,
                                contentDescription = data.author.username,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = if (data.isManual) {
                                    if (data.type == "audio") Icons.Default.Audiotrack else Icons.Default.FolderOpen
                                } else {
                                    Icons.Default.Person
                                },
                                contentDescription = null,
                                tint = RzaPrimaryGlow,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = if (data.isManual) {
                                data.title.ifBlank { "Local Device File" }
                            } else {
                                data.author.nickname ?: "@${data.author.username}"
                            },
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (data.isManual) {
                                if (data.type == "video") "Local Video" else "Local Audio"
                            } else {
                                "@${data.author.username} • ${data.platform.replaceFirstChar { it.uppercase() }}"
                            },
                            color = RzaPrimaryGlow.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Copy All Button
                OutlinedButton(
                    onClick = {
                        val allFormatted = buildString {
                            append("TITLE: ${data.title}\n")
                            if (data.hashtags.isNotEmpty()) append("HASHTAGS: ${data.hashtags.joinToString(" ")}\n")
                            append("CREATOR: @${data.author.username}\n")
                            append("PLATFORM: ${data.platform}\n")
                            append("LINK: ${data.sourceUrl}")
                        }
                        copyText(allFormatted, "All details")
                        copiedAll = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.2f))),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (copiedAll) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = if (copiedAll) RzaAccentEmerald else RzaPrimaryGlow
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (copiedAll) "Copied" else "Copy All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Quick Copy Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Copy Caption
                AssistChip(
                    onClick = {
                        copyText(data.title, "Caption")
                        copiedTitle = true
                    },
                    label = { Text(if (copiedTitle) "Caption ✓" else "Copy Caption", fontSize = 10.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Notes,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                )

                // Copy Hashtags
                if (data.hashtags.isNotEmpty()) {
                    AssistChip(
                        onClick = {
                            copyText(data.hashtags.joinToString(" "), "Hashtags")
                            copiedTags = true
                        },
                        label = { Text(if (copiedTags) "Tags ✓" else "Copy Tags", fontSize = 10.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(labelColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Copy Link
                AssistChip(
                    onClick = {
                        copyText(data.sourceUrl, "Link")
                        copiedLink = true
                    },
                    label = { Text(if (copiedLink) "Link ✓" else "Copy Link", fontSize = 10.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // Media Presentation (Video Player or Photo Carousel)
            if (data.type == "video") {
                // Interactive Video Player Preview
                RzaVideoPlayer(
                    videoUrl = selectedVideo?.url,
                    coverUrl = data.cover,
                    qualityLabel = selectedVideo?.quality ?: "HD",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                )

                // Quality Selector (if multiple stream options available)
                if (data.videos.size > 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "SELECT STREAM QUALITY",
                            color = RzaTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            data.videos.forEach { v ->
                                val isSelected = v.id == selectedVideoId
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) RzaPrimaryDark else RzaSurfaceElevated)
                                        .border(
                                            1.dp,
                                            if (isSelected) RzaPrimary else RzaBorderSubtle,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedVideoId = v.id }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = v.quality,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = v.label,
                                            color = RzaTextSecondary,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Primary Video Download Button (Solid White for highest contrast)
                if (selectedVideo != null && !data.isManual) {
                    if (data.isShort) {
                        // Short Remote Video: Row with Download Video + 𐂂 Clip — Coming Soon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onDownloadVideo(selectedVideo) },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RzaButtonWhite,
                                    contentColor = RzaButtonTextBlack
                                ),
                                shape = RoundedCornerShape(14.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = RzaButtonTextBlack
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Download Video",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            ComingSoonAction(
                                label = "𐂂 Clip — Coming Soon",
                                icon = Icons.Default.ContentCut,
                                featureName = "Video Clipping",
                                modifier = Modifier.weight(1.1f)
                            )
                        }
                    } else {
                        // Long Remote Video: Download Video ONLY
                        Button(
                            onClick = { onDownloadVideo(selectedVideo) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RzaButtonWhite,
                                contentColor = RzaButtonTextBlack
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = RzaButtonTextBlack
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Download Video (${selectedVideo.quality})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Secondary Audio & Cover Buttons (for remote media)
                if (!data.isManual) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (data.audio != null && data.audio.url.isNotBlank()) {
                            OutlinedButton(
                                onClick = { onDownloadAudio(data.audio.url) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.2f)))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = RzaPrimaryGlow,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Audio (MP3)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!data.cover.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = { onDownloadCover(data.cover) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.2f)))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = RzaPrimaryGlow,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cover (HD)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Feature Action Stack based on Media Category:
                // 1. Short Remote Video: Column with Transcribe, Listen Halal (Coming Soon), Video Enhancement (Coming Soon)
                if (!data.isManual && data.isShort) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TranscribeActionButton(
                            onTranscribe = onTranscribe,
                            isTranscribing = isTranscribing
                        )

                        ComingSoonAction(
                            label = "𐂂 Listen Halal — Coming Soon",
                            icon = Icons.Default.Headphones,
                            featureName = "Listen Halal",
                            modifier = Modifier.fillMaxWidth()
                        )

                        ComingSoonAction(
                            label = "𐂂 Video Enhancement — Coming Soon",
                            icon = Icons.Default.AutoFixHigh,
                            featureName = "Video Enhancement",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Next Creator Video Button (Strictly for TikTok creator videos)
                    if (data.platform == "tiktok" && data.author.username.isNotBlank()) {
                        Button(
                            onClick = onLoadNextVideo,
                            enabled = !isLoadingNext,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RzaSurfaceElevated,
                                contentColor = RzaPrimaryGlow
                            ),
                            border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RzaPrimary.copy(alpha = 0.3f)))
                        ) {
                            if (isLoadingNext) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = RzaPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Loading next video...", fontSize = 12.sp)
                            } else {
                                Text("Load Next Video from @${data.author.username}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Manual Video: Transcribe + Video Enhancement (Coming Soon). (NO Clip, NO Listen Halal)
                if (data.isManual && data.type == "video") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TranscribeActionButton(
                            onTranscribe = onTranscribe,
                            isTranscribing = isTranscribing
                        )

                        ComingSoonAction(
                            label = "𐂂 Video Enhancement — Coming Soon",
                            icon = Icons.Default.AutoFixHigh,
                            featureName = "Video Enhancement",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // In-place Transcript Result
                AnimatedVisibility(visible = transcriptResult != null) {
                    if (transcriptResult != null) {
                        TranscriptCardView(
                            transcript = transcriptResult,
                            onClose = onCloseTranscript
                        )
                    }
                }

            } else if (data.type == "audio") {
                // MANUAL AUDIO MODE: Transcribe + Listen Halal (Coming Soon). (NO Video Enhancement, NO Clip)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = RzaSurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RzaPrimary.copy(alpha = 0.3f)))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2B144E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = RzaPrimaryGlow,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = data.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Local Audio File Ready for Transcription",
                                color = RzaTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TranscribeActionButton(
                        onTranscribe = onTranscribe,
                        isTranscribing = isTranscribing
                    )

                    ComingSoonAction(
                        label = "𐂂 Listen Halal — Coming Soon",
                        icon = Icons.Default.Headphones,
                        featureName = "Listen Halal",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // In-place Transcript Result
                AnimatedVisibility(visible = transcriptResult != null) {
                    if (transcriptResult != null) {
                        TranscriptCardView(
                            transcript = transcriptResult,
                            onClose = onCloseTranscript
                        )
                    }
                }

            } else {
                // PHOTO CAROUSEL MODE
                PhotoCarouselView(
                    photos = data.photos,
                    audioOption = data.audio,
                    onDownloadPhoto = onDownloadPhoto,
                    onDownloadAudio = onDownloadAudio
                )
            }

            // Caption Box
            if (data.title.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0C0718))
                        .border(1.dp, RzaBorderSubtle, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "CAPTION",
                        color = RzaTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = data.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ComingSoonAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    featureName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            Toast.makeText(context, "$featureName is coming soon in the next update!", Toast.LENGTH_SHORT).show()
        },
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White.copy(alpha = 0.04f),
            contentColor = Color.White.copy(alpha = 0.85f)
        ),
        border = ButtonDefaults.outlinedButtonBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.18f))
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = RzaPrimaryGlow.copy(alpha = 0.8f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TranscribeActionButton(
    onTranscribe: () -> Unit,
    isTranscribing: Boolean,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onTranscribe,
        enabled = !isTranscribing,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Color(0xFF2E1854),
            contentColor = Color.White
        )
    ) {
        if (isTranscribing) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = RzaPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Transcribing speech with AI...", fontSize = 12.sp)
        } else {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = RzaPrimaryGlow,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("𐂂 Transcribe", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

