package io.lunosfer.dreamap.ui.screens

import android.widget.Toast
import io.lunosfer.dreamap.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.lunosfer.dreamap.data.model.Dream
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.PublicProfileUiState
import io.lunosfer.dreamap.ui.viewmodel.PublicProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onDreamClick: (Long) -> Unit,
    onDiaryJournalClick: () -> Unit = {}
) {
    val factory = remember(userId) { PublicProfileViewModel.Factory(userId) }
    val viewModel: PublicProfileViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state) {
        val s = state as? PublicProfileUiState.Success
        s?.actionMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearActionMessage()
        }
        s?.actionError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearActionError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Kullanıcı Profili",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Void950)
            )
        },
        containerColor = Void950
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val s = state) {
                is PublicProfileUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AstralGold)
                }
                is PublicProfileUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = s.message, color = SemanticDanger400)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.loadProfile(0) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AstralGold)
                        ) {
                            Text(stringResource(R.string.public_profile_retry_btn))
                        }
                    }
                }
                is PublicProfileUiState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header Span
                        item(span = { GridItemSpan(2) }) {
                            ProfileHeaderCard(
                                state = s,
                                onFollowClick = viewModel::sendFollowRequest
                            )
                        }

                        // Dreams Section Header
                        item(span = { GridItemSpan(2) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.public_profile_dreams_tab, s.dreams.size),
                                    color = AstralGold,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedButton(
                                    onClick = onDiaryJournalClick,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AstralGold),
                                    border = BorderStroke(1.dp, AstralGold),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.profile_tab_journal), fontSize = 12.sp)
                                }
                            }
                        }

                        if (s.dreams.isEmpty()) {
                            item(span = { GridItemSpan(2) }) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Void900),
                                    border = BorderStroke(1.dp, Void800)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(stringResource(R.string.public_profile_no_dreams), color = Color.Gray, fontSize = 13.sp)
                                    }
                                }
                            }
                        } else {
                            items(s.dreams, key = { it.id }) { dream ->
                                PublicDreamCard(
                                    dream = dream,
                                    onClick = { onDreamClick(dream.id) }
                                )
                            }
                        }

                        // Load More Button
                        if (s.hasMore) {
                            item(span = { GridItemSpan(2) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (s.isLoadingMore) {
                                        CircularProgressIndicator(color = AstralGold, modifier = Modifier.size(24.dp))
                                    } else {
                                        OutlinedButton(
                                            onClick = { viewModel.loadNextPage() },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AstralGold),
                                            border = BorderStroke(1.dp, AstralGold)
                                        ) {
                                            Text(stringResource(R.string.public_profile_load_more), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    state: PublicProfileUiState.Success,
    onFollowClick: () -> Unit
) {
    val prof = state.profile

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Void900),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Avatar
            if (!prof.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = prof.avatarUrl,
                    contentDescription = prof.nameOrFallback,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(2.dp, AstralGold, CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AstralGold.copy(alpha = 0.2f))
                        .border(2.dp, AstralGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = prof.nameOrFallback.take(1).uppercase(),
                        color = AstralGold,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Name & Username
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = prof.nameOrFallback,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "@${prof.username ?: "user"}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            // stringResource(R.string.public_profile_follows_you) Badge
            if (state.followsViewer) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AetherViolet.copy(alpha = 0.3f))
                        .border(0.5.dp, AetherViolet, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = stringResource(R.string.public_profile_follows_you),
                        color = AetherViolet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bio
            if (!prof.bio.isNullOrBlank()) {
                Text(
                    text = prof.bio,
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            // Action Button / Badge
            if (state.isSelf) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Void800)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(stringResource(R.string.public_profile_is_you), color = AstralGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                when (state.friendshipStatus) {
                    "accepted" -> {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            colors = ButtonDefaults.outlinedButtonColors(
                                disabledContainerColor = Void800,
                                disabledContentColor = AstralGold
                            ),
                            border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.public_profile_following), fontSize = 13.sp)
                        }
                    }
                    "pending" -> {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            colors = ButtonDefaults.outlinedButtonColors(
                                disabledContainerColor = Void800,
                                disabledContentColor = Color.Gray
                            ),
                            border = BorderStroke(1.dp, Color.Gray)
                        ) {
                            Text(stringResource(R.string.public_profile_follow_pending), fontSize = 12.sp)
                        }
                    }
                    else -> { // null
                        Button(
                            onClick = onFollowClick,
                            colors = ButtonDefaults.buttonColors(containerColor = AstralGold)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Void950, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.public_profile_follow_btn), color = Void950, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicDreamCard(
    dream: Dream,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Void900),
        border = BorderStroke(1.dp, Void800)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!dream.aiImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = dream.aiImageUrl,
                    contentDescription = dream.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            Text(
                text = dream.displayTitle,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = SemanticDanger400, modifier = Modifier.size(12.dp))
                        Text("${dream.likesCount ?: 0}", color = Color.Gray, fontSize = 11.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(Icons.Default.ModeComment, contentDescription = null, tint = AstralGold, modifier = Modifier.size(12.dp))
                        Text("${dream.commentsCount ?: 0}", color = Color.Gray, fontSize = 11.sp)
                    }
                }

                Text(
                    text = dream.createdAt.take(10),
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}
