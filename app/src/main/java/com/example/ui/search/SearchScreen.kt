package com.example.ui.search

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.TikTokMediaResult
import com.example.data.model.TikTokSearchResultItem
import com.example.data.model.TikTokVideoOption
import com.example.ui.components.RzaVideoPlayer
import com.example.ui.theme.*

@Composable
fun SearchScreen(
    keywordInput: String,
    onKeywordChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    searchResults: List<TikTokSearchResultItem>,
    isSearching: Boolean,
    searchError: String?,
    onDownloadItem: (TikTokSearchResultItem, type: String) -> Unit,
    onSelectForDownloader: (TikTokMediaResult) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val trendingKeywords = remember {
        listOf("charlidamelio", "khaby.lame", "mrbeast", "naat", "nasheed", "funny", "viral", "supercars", "islamic", "music")
    }

    var activeFilter by remember { mutableStateOf("all") } // "all", "videos", "photos", "creators"
    var activeSort by remember { mutableStateOf("recent") } // "recent", "popular"
    var activePreviewId by remember { mutableStateOf<String?>(null) }

    val copyText = { text: String, label: String ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied!", Toast.LENGTH_SHORT).show()
    }

    val filteredItems = remember(searchResults, activeFilter, activeSort) {
        var list = when (activeFilter) {
            "videos" -> searchResults.filter { it.itemType == "video" }
            "photos" -> searchResults.filter { it.itemType == "photo" }
            "creators" -> searchResults.filter { it.itemType == "creator" }
            else -> searchResults
        }
        val creators = list.filter { it.itemType == "creator" }
        val nonCreators = list.filter { it.itemType != "creator" }.toMutableList()
        if (activeSort == "popular") {
            nonCreators.sortByDescending { (it.playCount ?: 0) + (it.diggCount ?: 0) }
        } else {
            nonCreators.sortByDescending { it.createTime ?: 0 }
        }
        creators + nonCreators
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp)
    ) {
        // Search Header
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Search TikTok",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Explore and download public TikTok videos, trending songs, topics, or verified creators",
                    color = RzaTextSecondary,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Search TextField
                OutlinedTextField(
                    value = keywordInput,
                    onValueChange = onKeywordChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search creators (@username), topics, keywords...", color = RzaTextTertiary, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RzaPrimaryGlow) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (keywordInput.isNotBlank()) {
                                IconButton(onClick = { onKeywordChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = RzaTextSecondary)
                                }
                            }
                            IconButton(
                                onClick = { onSearch(keywordInput) },
                                enabled = keywordInput.isNotBlank() && !isSearching
                            ) {
                                if (isSearching) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = RzaPrimary, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Search", tint = RzaPrimary)
                                }
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

                // Trending Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = RzaPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Trending:", color = RzaTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    items(trendingKeywords) { kw ->
                        SuggestionChip(
                            onClick = {
                                onKeywordChange(kw)
                                onSearch(kw)
                            },
                            label = { Text(kw, fontSize = 11.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                labelColor = RzaTextSecondary
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = RzaBorderSubtle
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        }

        // Error message if any
        if (!searchError.isNullOrBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E0F16))
                ) {
                    Text(
                        text = searchError,
                        color = Color(0xFFFFD2D9),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Filters & Sorting Toolbar (when results present)
        if (searchResults.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(RzaSurfaceElevated)
                        .border(1.dp, RzaBorderSubtle, RoundedCornerShape(14.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Filter Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("all" to "All", "videos" to "Videos", "photos" to "Photos", "creators" to "Creators").forEach { (k, label) ->
                            FilterChip(
                                selected = activeFilter == k,
                                onClick = { activeFilter = k },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RzaPrimaryDark,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Sort
                    IconButton(
                        onClick = {
                            activeSort = if (activeSort == "recent") "popular" else "recent"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort: $activeSort",
                            tint = RzaPrimaryGlow,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Results List
        items(filteredItems, key = { it.id }) { item ->
            if (item.itemType == "creator") {
                // Verified Creator Profile Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = RzaSurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RzaPrimary.copy(alpha = 0.5f)))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(RzaPrimaryDark)
                                .border(1.5.dp, RzaPrimary, CircleShape)
                        ) {
                            if (item.cover.isNotBlank()) {
                                AsyncImage(
                                    model = item.cover,
                                    contentDescription = item.author.username,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = item.author.nickname ?: "@${item.author.username}",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RzaPrimary, modifier = Modifier.size(14.dp))
                            }
                            Text(text = "@${item.author.username}", color = RzaPrimaryGlow, fontSize = 12.sp)
                            if (item.author.followerCount != null) {
                                Text(
                                    text = "${item.author.followerCount} followers",
                                    color = RzaTextTertiary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onKeywordChange("@${item.author.username}")
                                onSearch("@${item.author.username}")
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RzaButtonWhite, contentColor = RzaButtonTextBlack),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("View Videos", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Video / Photo Result Card
                val isPlaying = activePreviewId == item.id
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = RzaSurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RzaBorderSubtle))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Media Player / Thumbnail Preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black)
                        ) {
                            if (isPlaying && item.downloadUrl.isNotBlank()) {
                                RzaVideoPlayer(
                                    videoUrl = item.downloadUrl,
                                    coverUrl = item.cover,
                                    qualityLabel = "HD",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                AsyncImage(
                                    model = item.cover,
                                    contentDescription = item.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                // Play button overlay
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(RzaPrimaryDark.copy(alpha = 0.85f))
                                        .align(Alignment.Center)
                                        .clickable { activePreviewId = item.id },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Preview",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                // Type badge top-left
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = item.itemType.uppercase(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // PlayCount & Duration bottom
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "@${item.author.username}",
                                        color = RzaPrimaryGlow,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (item.duration != null) {
                                        Text(
                                            text = "${item.duration}s",
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Caption
                        Text(
                            text = item.title.ifBlank { "TikTok Video" },
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Action Buttons: Quick Copy Tools + Download Video + Download Cover
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { copyText(item.title, "Caption") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.2f)))
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Caption", fontSize = 10.sp)
                            }

                            OutlinedButton(
                                onClick = { onDownloadItem(item, "cover") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.2f)))
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Cover", fontSize = 10.sp)
                            }

                            Button(
                                onClick = { onDownloadItem(item, "video") },
                                modifier = Modifier.weight(1.4f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RzaButtonWhite, contentColor = RzaButtonTextBlack),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download HD", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }

        // Load More Pagination Button
        if (searchResults.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onLoadMore,
                        enabled = !isLoadingMore,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RzaSurfaceElevated, contentColor = Color.White),
                        border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RzaPrimary.copy(alpha = 0.4f)))
                    ) {
                        if (isLoadingMore) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = RzaPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Loading more videos...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Load More Videos", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
