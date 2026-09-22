package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.TikTokAudioOption
import com.example.data.model.TikTokPhotoOption
import com.example.ui.theme.*

@Composable
fun PhotoCarouselView(
    photos: List<TikTokPhotoOption>,
    audioOption: TikTokAudioOption?,
    onDownloadPhoto: (photoUrl: String, index: Int) -> Unit,
    onDownloadAudio: (audioUrl: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIndices by remember(photos) {
        mutableStateOf(photos.map { it.index }.toSet())
    }

    val isAllSelected = selectedIndices.size == photos.size

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Toolbar: Select All / None & Download Selected
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RzaSurfaceElevated)
                .border(1.dp, RzaBorderSubtle, RoundedCornerShape(16.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable {
                    selectedIndices = if (isAllSelected) emptySet() else photos.map { it.index }.toSet()
                }
            ) {
                Icon(
                    imageVector = if (isAllSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = if (isAllSelected) RzaPrimary else RzaTextTertiary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isAllSelected) "Deselect All (${selectedIndices.size}/${photos.size})" else "Select All (${selectedIndices.size}/${photos.size})",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    photos.filter { it.index in selectedIndices }.forEach { p ->
                        onDownloadPhoto(p.url, p.index)
                    }
                },
                enabled = selectedIndices.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RzaButtonWhite,
                    contentColor = RzaButtonTextBlack
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Save Selected (${selectedIndices.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Photo Grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            photos.chunked(2).forEach { rowPhotos ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowPhotos.forEach { photo ->
                        val isSelected = photo.index in selectedIndices
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black)
                                .border(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) RzaPrimary else RzaBorderSubtle,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    selectedIndices = if (isSelected) {
                                        selectedIndices - photo.index
                                    } else {
                                        selectedIndices + photo.index
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = photo.url,
                                contentDescription = "Photo ${photo.index}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Index badge top-left
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "#${photo.index}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Selection tick top-right
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) RzaPrimary else Color.Black.copy(alpha = 0.6f))
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Single Save Button bottom
                            IconButton(
                                onClick = { onDownloadPhoto(photo.url, photo.index) },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.75f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Save Photo",
                                    tint = RzaPrimaryGlow,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // If row has only 1 item, fill remaining space
                    if (rowPhotos.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Background Audio Download Button if available
        if (audioOption != null && audioOption.url.isNotBlank()) {
            OutlinedButton(
                onClick = { onDownloadAudio(audioOption.url) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RzaPrimary.copy(alpha = 0.5f)))
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = RzaPrimaryGlow,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Download Background Audio (${audioOption.format.uppercase()})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
