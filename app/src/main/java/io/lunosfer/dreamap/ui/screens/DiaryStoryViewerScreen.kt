package io.lunosfer.dreamap.ui.screens

import io.lunosfer.dreamap.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.lunosfer.dreamap.data.model.DiaryEntry
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.DiaryStoryViewerUiState
import io.lunosfer.dreamap.ui.viewmodel.DiaryStoryViewerViewModel

@Composable
fun DiaryStoryViewerScreen(
    userId: String,
    onBack: () -> Unit,
    onGoalClick: ((String) -> Unit)? = null
) {
    val factory = remember(userId) { DiaryStoryViewerViewModel.Factory(userId) }
    val viewModel: DiaryStoryViewerViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        if (state is DiaryStoryViewerUiState.Closed) {
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val s = state) {
            is DiaryStoryViewerUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AstralGold)
                }
            }
            is DiaryStoryViewerUiState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(s.message, color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = AstralGold)
                    ) {
                        Text("Geri Dön", color = Void950)
                    }
                }
            }
            is DiaryStoryViewerUiState.Content -> {
                val currentEntry = s.currentEntry

                if (currentEntry != null) {
                    // Story Content View
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(s.currentIndex) {
                                detectTapGestures(
                                    onPress = {
                                        viewModel.pauseTimer()
                                        tryAwaitRelease()
                                        viewModel.resumeTimer()
                                    },
                                    onTap = { offset ->
                                        if (offset.x < size.width * 0.35f) {
                                            viewModel.previousStory()
                                        } else {
                                            viewModel.nextStory()
                                        }
                                    }
                                )
                            }
                    ) {
                        // Media or Text
                        if (currentEntry.mediaType == "photo" && !currentEntry.mediaUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = currentEntry.mediaUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (currentEntry.mediaType == "video" && (!currentEntry.posterUrl.isNullOrBlank() || !currentEntry.mediaUrl.isNullOrBlank())) {
                            AsyncImage(
                                model = currentEntry.posterUrl ?: currentEntry.mediaUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Text Entry
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Void950,
                                                Void900,
                                                AetherViolet.copy(alpha = 0.3f),
                                                Void950
                                            )
                                        )
                                    )
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentEntry.caption ?: "",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = SerifFontFamily),
                                    lineHeight = 32.sp
                                )
                            }
                        }

                        // Caption Overlay for Photo/Video
                        if (currentEntry.mediaType != "text" && !currentEntry.caption.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                        )
                                    )
                                    .padding(start = 20.dp, end = 20.dp, bottom = 40.dp, top = 40.dp)
                            ) {
                                Text(
                                    text = currentEntry.caption,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Top Bar Controls Overlay
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        // Progress Bars Segmented
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            s.entries.forEachIndexed { index, _ ->
                                val progressVal = when {
                                    index < s.currentIndex -> 1f
                                    index == s.currentIndex -> s.progress
                                    else -> 0f
                                }
                                LinearProgressIndicator(
                                    progress = { progressVal },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.3f)
                                )
                            }
                        }

                        // Header Info Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (!s.owner?.avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = s.owner?.avatarUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, AstralGold, CircleShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(AstralGold.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = s.owner?.nameOrFallback?.take(1)?.uppercase() ?: "K",
                                            color = AstralGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = s.owner?.nameOrFallback ?: "Kullanıcı",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (!currentEntry.goalTitle.isNullOrBlank()) {
                                        Surface(
                                            onClick = {
                                                currentEntry.goalId?.let { gid -> onGoalClick?.invoke(gid) }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = AstralGold.copy(alpha = 0.25f),
                                            border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.TrackChanges,
                                                    contentDescription = null,
                                                    tint = AstralGold,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Text(
                                                    text = currentEntry.goalTitle,
                                                    color = AstralGold,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Actions: Delete (if self) & Close
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (s.isSelf) {
                                    IconButton(
                                        onClick = { viewModel.deleteCurrentEntry() }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Sil",
                                            tint = Color(0xFFF87171)
                                        )
                                    }
                                }

                                IconButton(onClick = onBack) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Kapat",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
            else -> {}
        }
    }
}
