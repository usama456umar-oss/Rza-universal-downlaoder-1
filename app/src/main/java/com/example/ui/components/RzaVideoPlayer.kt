package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.ui.theme.RzaBorderSubtle
import com.example.ui.theme.RzaPrimary
import com.example.ui.theme.RzaPrimaryDark

@Composable
fun RzaVideoPlayer(
    videoUrl: String?,
    coverUrl: String?,
    qualityLabel: String = "HD",
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }

    LaunchedEffect(videoUrl) {
        isPlaying = false
        isLoading = false
        hasError = false
        videoViewInstance?.stopPlayback()
    }

    DisposableEffect(Unit) {
        onDispose {
            videoViewInstance?.stopPlayback()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
            .border(1.dp, RzaBorderSubtle, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!videoUrl.isNullOrBlank() && !hasError && isPlaying) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    VideoView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            isLoading = false
                            start()
                        }
                        setOnErrorListener { _, _, _ ->
                            isLoading = false
                            hasError = true
                            isPlaying = false
                            true
                        }
                        videoViewInstance = this
                        setVideoURI(Uri.parse(videoUrl))
                    }
                },
                update = { view ->
                    videoViewInstance = view
                }
            )
        } else {
            // Poster Cover Preview
            if (!coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = "Video Poster",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F0A1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = RzaPrimary.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        // Top Quality Pill Badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.75f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(RzaPrimary)
                )
                Text(
                    text = "$qualityLabel Preview",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Center Play / Pause Action Overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = RzaPrimary,
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp
                )
            }
        } else if (!videoUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) Color.Black.copy(alpha = 0.5f) else RzaPrimaryDark.copy(alpha = 0.9f))
                    .border(1.5.dp, RzaPrimary, CircleShape)
                    .clickable {
                        if (isPlaying) {
                            videoViewInstance?.pause()
                            isPlaying = false
                        } else {
                            isLoading = true
                            isPlaying = true
                            videoViewInstance?.let {
                                it.setVideoURI(Uri.parse(videoUrl))
                                it.start()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause Preview" else "Play Preview",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        if (hasError) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Stream preview unavailable",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
