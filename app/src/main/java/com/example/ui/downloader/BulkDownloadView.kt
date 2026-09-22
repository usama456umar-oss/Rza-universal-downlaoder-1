package com.example.ui.downloader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BulkDownloadItem
import com.example.ui.theme.*

@Composable
fun BulkDownloadView(
    items: List<BulkDownloadItem>,
    isQueueRunning: Boolean,
    onToggleSelect: (id: String) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onStartBulkDownload: () -> Unit,
    onRemoveItem: (id: String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedCount = items.count { it.selected && (it.status == "ready" || it.status == "completed") }
    val isAllSelected = items.isNotEmpty() && items.all { it.selected }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Toolbar
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = RzaSurfaceElevated),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RzaPrimary.copy(alpha = 0.4f)))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(RzaPrimary)
                        )
                        Text(
                            text = "BATCH QUEUE (${items.size} LINKS)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Select All Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.clickable {
                            if (isAllSelected) onSelectNone() else onSelectAll()
                        }
                    ) {
                        Icon(
                            imageVector = if (isAllSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                            contentDescription = null,
                            tint = if (isAllSelected) RzaPrimary else RzaTextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isAllSelected) "Deselect" else "Select All",
                            color = RzaPrimaryGlow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Download Selected Button
                    Button(
                        onClick = onStartBulkDownload,
                        enabled = selectedCount > 0 && !isQueueRunning,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RzaButtonWhite,
                            contentColor = RzaButtonTextBlack
                        )
                    ) {
                        if (isQueueRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = RzaButtonTextBlack,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Downloading...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Download Selected ($selectedCount)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Clear All
                    IconButton(
                        onClick = onClearAll,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear All Links",
                            tint = RzaError
                        )
                    }
                }
            }
        }

        // Batch Items
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEachIndexed { index, item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (item.status) {
                            "completed" -> Color(0xFF0F1E16)
                            "failed" -> Color(0xFF1E0F14)
                            else -> RzaSurfaceElevated
                        }
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            when (item.status) {
                                "completed" -> RzaAccentEmerald.copy(alpha = 0.4f)
                                "failed" -> RzaError.copy(alpha = 0.4f)
                                else -> RzaBorderSubtle
                            }
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Checkbox
                                Icon(
                                    imageVector = if (item.selected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                    contentDescription = null,
                                    tint = if (item.selected) RzaPrimary else RzaTextTertiary,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { onToggleSelect(item.id) }
                                )

                                Text(
                                    text = "#${index + 1}",
                                    color = RzaTextTertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                // Status Pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when (item.status) {
                                                "ready" -> RzaPrimaryDark.copy(alpha = 0.7f)
                                                "completed" -> Color(0xFF14532D)
                                                "failed" -> Color(0xFF7F1D1D)
                                                "resolving" -> Color(0xFF3B1D6D)
                                                else -> Color.Black.copy(alpha = 0.6f)
                                            }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = item.status.uppercase(),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onRemoveItem(item.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Link",
                                    tint = RzaTextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Item details if resolved
                        val media = item.data
                        if (media != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!media.cover.isNullOrBlank()) {
                                    AsyncImage(
                                        model = media.cover,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "@${media.author.username}",
                                        color = RzaPrimaryGlow,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = media.title.ifBlank { "TikTok Media" },
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else if (item.status == "failed") {
                            Text(
                                text = item.error ?: "Failed to resolve link.",
                                color = RzaError,
                                fontSize = 11.sp
                            )
                        } else {
                            Text(
                                text = item.url,
                                color = RzaTextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
