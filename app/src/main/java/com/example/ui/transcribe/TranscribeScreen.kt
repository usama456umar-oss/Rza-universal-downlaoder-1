package com.example.ui.transcribe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TranscriptResult
import com.example.ui.components.TranscriptCardView
import com.example.ui.theme.*

@Composable
fun TranscribeScreen(
    onTranscribe: (input: String, language: String, detectSpeakers: Boolean, includeTimestamps: Boolean) -> Unit,
    isTranscribing: Boolean,
    transcriptResult: TranscriptResult?,
    errorMessage: String?,
    onClearResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("auto") }
    var detectSpeakers by remember { mutableStateOf(true) }
    var includeTimestamps by remember { mutableStateOf(true) }
    var showOptions by remember { mutableStateOf(false) }

    val languages = listOf(
        "auto" to "Auto-Detect",
        "English" to "English",
        "Urdu" to "Urdu",
        "Arabic" to "Arabic",
        "Spanish" to "Spanish",
        "French" to "French",
        "Hindi" to "Hindi",
        "German" to "German"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(RzaPrimaryDark)
                    .border(1.dp, RzaPrimary, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = RzaPrimaryGlow,
                    modifier = Modifier.size(30.dp)
                )
            }

            Text(
                text = "RZA AI Transcriber",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "Convert spoken audio or video links into high-accuracy timestamped scripts using Gemini AI.",
                color = RzaTextSecondary,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        // Input Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = RzaSurfaceElevated),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RzaPrimary.copy(alpha = 0.4f)))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = {
                        Text(
                            "Paste TikTok link, audio topic, or caption to transcribe...",
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

                // Options Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Transcription Settings",
                        color = RzaTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(onClick = { showOptions = !showOptions }) {
                        Icon(
                            imageVector = if (showOptions) Icons.Default.ExpandLess else Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = RzaPrimaryGlow
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showOptions) "Hide" else "Customize", fontSize = 11.sp, color = RzaPrimaryGlow)
                    }
                }

                if (showOptions) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Language:", color = RzaTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            languages.take(4).forEach { (code, name) ->
                                FilterChip(
                                    selected = selectedLanguage == code,
                                    onClick = { selectedLanguage = code },
                                    label = { Text(name, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RzaPrimaryDark,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Detect Speakers", color = Color.White, fontSize = 11.sp)
                            Switch(
                                checked = detectSpeakers,
                                onCheckedChange = { detectSpeakers = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = RzaPrimary)
                            )
                        }
                    }
                }

                // Primary Generate Transcript Action
                Button(
                    onClick = {
                        onTranscribe(inputText, selectedLanguage, detectSpeakers, includeTimestamps)
                    },
                    enabled = inputText.isNotBlank() && !isTranscribing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RzaButtonWhite,
                        contentColor = RzaButtonTextBlack
                    )
                ) {
                    if (isTranscribing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = RzaButtonTextBlack,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing with Gemini AI...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate AI Transcript", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // Error message if any
        AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
            if (!errorMessage.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A0D15))
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFFD2D9),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Transcript Result Display
        if (transcriptResult != null) {
            TranscriptCardView(
                transcript = transcriptResult,
                onClose = onClearResult
            )

            // Share Action
            Button(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "RZA AI Transcript")
                        putExtra(Intent.EXTRA_TEXT, transcriptResult.cleanScript.ifBlank { transcriptResult.fullTranscript })
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Transcript"))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RzaSurfaceElevated,
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RzaPrimary.copy(alpha = 0.4f)))
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share Transcript via Apps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
