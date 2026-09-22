package com.example.ui.downloader

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BulkDownloadItem
import com.example.data.model.TikTokMediaResult
import com.example.data.model.TikTokVideoOption
import com.example.data.model.TranscriptResult
import com.example.ui.components.MediaResultView
import com.example.ui.theme.*

@Composable
fun DownloaderScreen(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    onSubmitSingle: (String) -> Unit,
    onSubmitBatch: (String) -> Unit,
    onClear: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    resultData: TikTokMediaResult?,
    bulkItems: List<BulkDownloadItem>,
    isBulkActive: Boolean,
    isBulkQueueRunning: Boolean,
    onToggleBulkSelect: (String) -> Unit,
    onSelectAllBulk: () -> Unit,
    onSelectNoneBulk: () -> Unit,
    onStartBulkDownload: () -> Unit,
    onRemoveBulkItem: (String) -> Unit,
    onDownloadVideo: (TikTokVideoOption) -> Unit,
    onDownloadAudio: (String) -> Unit,
    onDownloadCover: (String) -> Unit,
    onDownloadPhoto: (String, Int) -> Unit,
    onLoadNextVideo: () -> Unit,
    isLoadingNext: Boolean,
    onTranscribe: () -> Unit,
    isTranscribing: Boolean,
    transcriptResult: TranscriptResult?,
    onCloseTranscript: () -> Unit,
    onLoadManualMedia: (Uri, Boolean, String?, Int?) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeMode by remember { mutableStateOf("single") } // "single" or "batch"

    val mediaPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val path = it.path.orEmpty()
            val isAudio = path.endsWith(".mp3", true) || path.endsWith(".m4a", true) || path.endsWith(".wav", true)
            val isVideo = !isAudio
            onLoadManualMedia(it, isVideo, if (isVideo) "Local Video File" else "Local Audio File", 45)
        }
    }

    val handleSmartPaste = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val pastedText = clip.getItemAt(0).text?.toString().orEmpty()
            if (pastedText.isNotBlank()) {
                onUrlChange(pastedText)
                Toast.makeText(context, "Link pasted!", Toast.LENGTH_SHORT).show()
                if (!pastedText.contains("\n") && activeMode == "single") {
                    onSubmitSingle(pastedText)
                }
            } else {
                Toast.makeText(context, "Clipboard is empty.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Clipboard is empty.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Hero Branding Badge
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF28144D), Color(0xFF130A24))
                        )
                    )
                    .border(2.dp, Brush.linearGradient(listOf(RzaPrimaryGlow, RzaAccentCyan)), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_deer_logo),
                    contentDescription = "RZA Deer Emblem 𐂂",
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "𐂂",
                    color = RzaPrimaryGlow,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "RZA Universal Downloader",
                    color = Color.White,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
            }

            Text(
                text = "Download media from YouTube, TikTok, Instagram & more via Cobalt engine. Native RZA AI transcription and TikTok creator tools preserved.",
                color = RzaTextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Supported Platforms Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("YouTube", "TikTok", "Instagram", "Facebook", "Cobalt").forEachIndexed { idx, name ->
                    if (idx > 0) {
                        Text(
                            text = "•",
                            color = RzaPrimaryGlow.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                    Text(
                        text = name,
                        color = if (name == "Cobalt") RzaAccentEmerald else RzaPrimaryGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Mode Switcher: Single Link vs Batch Links
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(RzaSurfaceElevated)
                .border(1.dp, RzaBorderSubtle, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { activeMode = "single" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeMode == "single") RzaPrimaryDark else Color.Transparent,
                    contentColor = if (activeMode == "single") Color.White else RzaTextSecondary
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Single Link", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { activeMode = "batch" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeMode == "batch") RzaPrimaryDark else Color.Transparent,
                    contentColor = if (activeMode == "batch") Color.White else RzaTextSecondary
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Batch Links (Up to 10)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Input Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = RzaSurfaceElevated),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RzaPrimary.copy(alpha = 0.4f)))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (activeMode == "single") {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = onUrlChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Paste TikTok video or profile link here...", color = RzaTextTertiary, fontSize = 13.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, tint = RzaPrimaryGlow)
                        },
                        trailingIcon = {
                            if (urlInput.isNotBlank()) {
                                IconButton(onClick = onClear) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = RzaTextSecondary)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RzaPrimary,
                            unfocusedBorderColor = RzaBorderSubtle,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = onUrlChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        placeholder = {
                            Text(
                                "Paste up to 10 TikTok links here (one per line):\nhttps://www.tiktok.com/@user/video/...\nhttps://www.tiktok.com/@user/video/...",
                                color = RzaTextTertiary,
                                fontSize = 12.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RzaPrimary,
                            unfocusedBorderColor = RzaBorderSubtle,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                // Action Buttons Row (Paste Link + Download)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = handleSmartPaste,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.25f)))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = RzaPrimaryGlow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste Link", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (activeMode == "single") onSubmitSingle(urlInput)
                            else onSubmitBatch(urlInput)
                        },
                        enabled = urlInput.isNotBlank() && !isLoading,
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RzaButtonWhite,
                            contentColor = RzaButtonTextBlack
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = RzaButtonTextBlack,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resolving...", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (activeMode == "single") "Download" else "Process Batch",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Quick Try Links Pill Selector (Horizontal scrollable row)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Quick Try Examples & Manual Media:",
                        color = RzaTextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                val sample = "https://www.tiktok.com/@tiktok/video/7123456789012345678"
                                onUrlChange(sample)
                                onSubmitSingle(sample)
                            },
                            label = { Text("TikTok", fontSize = 10.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = RzaSurface,
                                labelColor = RzaPrimaryGlow
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RzaBorderSubtle),
                            shape = RoundedCornerShape(8.dp)
                        )

                        AssistChip(
                            onClick = {
                                val sample = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                                onUrlChange(sample)
                                onSubmitSingle(sample)
                            },
                            label = { Text("YouTube (Cobalt)", fontSize = 10.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = RzaSurface,
                                labelColor = Color(0xFFFF5252)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RzaBorderSubtle),
                            shape = RoundedCornerShape(8.dp)
                        )

                        AssistChip(
                            onClick = {
                                val sample = "@charlidamelio"
                                onUrlChange(sample)
                                onSubmitSingle(sample)
                            },
                            label = { Text("@Creator", fontSize = 10.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = RzaSurface,
                                labelColor = RzaAccentCyan
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RzaBorderSubtle),
                            shape = RoundedCornerShape(8.dp)
                        )

                        AssistChip(
                            onClick = {
                                onLoadManualMedia(
                                    Uri.parse("android.resource://manual/sample_video"),
                                    true,
                                    "Recorded Short Clip.mp4",
                                    35
                                )
                            },
                            label = { Text("📁 Local Video", fontSize = 10.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = RzaSurface,
                                labelColor = RzaAccentEmerald
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RzaBorderSubtle),
                            shape = RoundedCornerShape(8.dp)
                        )

                        AssistChip(
                            onClick = {
                                onLoadManualMedia(
                                    Uri.parse("android.resource://manual/sample_audio"),
                                    false,
                                    "Voice Note Recording.m4a",
                                    60
                                )
                            },
                            label = { Text("📁 Local Audio", fontSize = 10.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = RzaSurface,
                                labelColor = Color(0xFFFFB74D)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RzaBorderSubtle),
                            shape = RoundedCornerShape(8.dp)
                        )

                        AssistChip(
                            onClick = { mediaPickerLauncher.launch("*/*") },
                            label = { Text("📂 Pick File", fontSize = 10.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = RzaSurface,
                                labelColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RzaBorderSubtle),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        // Error Banner
        AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
            if (!errorMessage.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A0D15)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RzaError.copy(alpha = 0.4f)))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = RzaError,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFFD2D9),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Animated Resolving Card with Deer Emblem
        AnimatedVisibility(visible = isLoading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = RzaSurfaceElevated),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(RzaPrimary.copy(alpha = 0.5f))
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF3B1D6D), Color(0xFF1B0F33))
                                )
                            )
                            .border(1.dp, RzaPrimaryGlow.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_deer_logo),
                            contentDescription = "RZA Deer Engine 𐂂",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "𐂂 ",
                                color = RzaPrimaryGlow,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Extracting Clean Media Stream...",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Bypassing watermarks and querying servers",
                            color = RzaTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = RzaPrimaryGlow,
                        strokeWidth = 2.5.dp
                    )
                }
            }
        }

        // Results Presentation
        if (isBulkActive && bulkItems.isNotEmpty()) {
            BulkDownloadView(
                items = bulkItems,
                isQueueRunning = isBulkQueueRunning,
                onToggleSelect = onToggleBulkSelect,
                onSelectAll = onSelectAllBulk,
                onSelectNone = onSelectNoneBulk,
                onStartBulkDownload = onStartBulkDownload,
                onRemoveItem = onRemoveBulkItem,
                onClearAll = onClear
            )
        } else if (resultData != null) {
            MediaResultView(
                data = resultData,
                onClear = onClear,
                onDownloadVideo = onDownloadVideo,
                onDownloadAudio = onDownloadAudio,
                onDownloadCover = onDownloadCover,
                onDownloadPhoto = onDownloadPhoto,
                onLoadNextVideo = onLoadNextVideo,
                isLoadingNext = isLoadingNext,
                onTranscribe = onTranscribe,
                isTranscribing = isTranscribing,
                transcriptResult = transcriptResult,
                onCloseTranscript = onCloseTranscript
            )
        }

        // Community Banner (WhatsApp Link from Web App)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://chat.whatsapp.com/IzOUwa7d3538fR4H0hOaUg"))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cannot open community link.", Toast.LENGTH_SHORT).show()
                    }
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = RzaSurfaceElevated),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RzaPrimary.copy(alpha = 0.25f)))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(RzaAccentGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Join WhatsApp Community",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Get free tools, updates & useful resources",
                            color = RzaTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = RzaPrimaryGlow,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
