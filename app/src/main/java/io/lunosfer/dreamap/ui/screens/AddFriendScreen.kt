package io.lunosfer.dreamap.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.Friendship
import io.lunosfer.dreamap.data.model.UserSearchResult
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.AddFriendUiState
import io.lunosfer.dreamap.ui.viewmodel.AddFriendViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendScreen(
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: AddFriendViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val contentState = state as? AddFriendUiState.Content ?: AddFriendUiState.Content()

    LaunchedEffect(contentState.actionMessage, contentState.actionError) {
        contentState.actionMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearActionMessage()
        }
        contentState.actionError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearActionError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.friend_search_title),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_button_desc), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Void950)
            )
        },
        containerColor = Void950
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Input Field
            OutlinedTextField(
                value = contentState.query,
                onValueChange = { viewModel.searchUsers(it) },
                placeholder = { Text(stringResource(R.string.friend_search_placeholder), color = Color.Gray, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AstralGold) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AstralGold,
                    unfocusedBorderColor = Void800,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // Pending Requests Section (if any)
            if (contentState.pendingRequests.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Void900),
                    border = BorderStroke(1.dp, AetherViolet.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.friend_pending_requests, contentState.pendingRequests.size),
                            color = AstralGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        contentState.pendingRequests.forEach { req ->
                            PendingRequestRow(
                                friendship = req,
                                onUserClick = onUserClick,
                                onAccept = { viewModel.respondToRequest(req.id, "accepted") },
                                onReject = { viewModel.respondToRequest(req.id, "rejected") }
                            )
                        }
                    }
                }
            }

            // Search Results Section
            Text(
                text = if (contentState.query.isBlank()) stringResource(R.string.friend_search_results) else stringResource(R.string.friend_results_count, contentState.searchResults.size),
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            if (contentState.isSearching) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AstralGold)
                }
            } else if (contentState.searchResults.isEmpty() && contentState.query.isNotBlank()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.friend_not_found), color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(contentState.searchResults, key = { it.id }) { user ->
                        UserSearchRow(
                            user = user,
                            onUserClick = { onUserClick(user.id) },
                            onFollowClick = { viewModel.sendFollowRequest(user.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingRequestRow(
    friendship: Friendship,
    onUserClick: (String) -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val requester = friendship.requester
    val userId = requester?.id ?: friendship.userId ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Void800)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { if (userId.isNotBlank()) onUserClick(userId) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val avatarUrl = requester?.avatarUrl
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AstralGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = requester?.nameOrFallback?.take(1)?.uppercase() ?: "?",
                        color = AstralGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column {
                Text(
                    text = requester?.nameOrFallback ?: stringResource(R.string.friend_default_user),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "@${requester?.username ?: "user"}",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = onAccept,
                modifier = Modifier.height(34.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AstralGold),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(stringResource(R.string.friend_accept), color = Void950, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onReject,
                modifier = Modifier.height(34.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SemanticDanger400),
                border = BorderStroke(1.dp, SemanticDanger400),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Text(stringResource(R.string.friend_reject), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun UserSearchRow(
    user: UserSearchResult,
    onUserClick: () -> Unit,
    onFollowClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Void900),
        border = BorderStroke(1.dp, Void800)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onUserClick() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!user.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(AstralGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.nameOrFallback.take(1).uppercase(),
                            color = AstralGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column {
                    Text(
                        text = user.nameOrFallback,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${user.username ?: "user"}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            val status = user.friendshipStatus
            when (status) {
                "accepted" -> {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        colors = ButtonDefaults.outlinedButtonColors(
                            disabledContainerColor = Void800,
                            disabledContentColor = AstralGold
                        ),
                        border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.5f)),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.friend_following), fontSize = 12.sp)
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
                        border = BorderStroke(1.dp, Color.Gray),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(stringResource(R.string.friend_pending), fontSize = 12.sp)
                    }
                }
                else -> { // null
                    Button(
                        onClick = onFollowClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AstralGold),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Void950, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.friend_follow), color = Void950, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
