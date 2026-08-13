package io.lunosfer.dreamap.ui.components

import io.lunosfer.dreamap.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.lunosfer.dreamap.data.model.DiaryRing
import io.lunosfer.dreamap.ui.theme.AetherViolet
import io.lunosfer.dreamap.ui.theme.AstralGold
import io.lunosfer.dreamap.ui.theme.Void800
import io.lunosfer.dreamap.ui.theme.Void900
import io.lunosfer.dreamap.ui.theme.Void950
import io.lunosfer.dreamap.ui.viewmodel.DiaryFeedUiState
import io.lunosfer.dreamap.ui.viewmodel.DiaryFeedViewModel

@Composable
fun DiaryRingsBar(
    viewModel: DiaryFeedViewModel = viewModel(),
    onOpenComposer: () -> Unit,
    onOpenViewer: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    val rings = (state as? DiaryFeedUiState.Success)?.rings ?: emptyList()

    if (state is DiaryFeedUiState.Loading) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Void900)
                )
            }
        }
        return
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(rings, key = { it.userId }) { ring ->
            RingItem(
                ring = ring,
                onAddClick = onOpenComposer,
                onRingClick = { onOpenViewer(ring.userId) }
            )
        }
    }
}

@Composable
private fun RingItem(
    ring: DiaryRing,
    onAddClick: () -> Unit,
    onRingClick: () -> Unit
) {
    val isUnseen = ring.hasUnseen
    val borderBrush = when {
        ring.isSelf && ring.entryCount == 0 -> null
        isUnseen -> Brush.linearGradient(listOf(AstralGold, AetherViolet, Color(0xFFEC4899)))
        else -> Brush.linearGradient(listOf(Void800, Color.Gray.copy(alpha = 0.5f)))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .clickable {
                if (ring.isSelf && ring.entryCount == 0) {
                    onAddClick()
                } else {
                    onRingClick()
                }
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(64.dp)
        ) {
            // Border Circle
            if (borderBrush != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(borderBrush)
                        .padding(3.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(BorderStroke(2.dp, Void800), CircleShape)
                        .padding(3.dp)
                )
            }

            // Avatar Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Void950),
                contentAlignment = Alignment.Center
            ) {
                if (!ring.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ring.avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AstralGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ring.nameOrFallback.take(1).uppercase(),
                            color = AstralGold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Plus icon for self empty ring
            if (ring.isSelf) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(AstralGold)
                        .border(1.5.dp, Void950, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Ekle",
                        tint = Void950,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = if (ring.isSelf) stringResource(R.string.diary_ring_self) else ring.nameOrFallback,
            color = if (isUnseen) Color.White else Color.Gray,
            fontSize = 11.sp,
            fontWeight = if (isUnseen) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
