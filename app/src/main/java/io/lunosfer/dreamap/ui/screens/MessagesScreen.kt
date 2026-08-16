package io.lunosfer.dreamap.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import androidx.compose.ui.res.stringResource
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.Conversation
import io.lunosfer.dreamap.supabase.supabaseClient
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.MessagesViewModel
import io.lunosfer.dreamap.ui.viewmodel.UiState

@Composable
fun MessagesScreen(
    navController: NavController,
    onLoginClick: () -> Unit = {},
    viewModel: MessagesViewModel = viewModel()
) {
    val sessionStatus by supabaseClient.auth.sessionStatus.collectAsState(initial = SessionStatus.Initializing)
    val isLoggedIn = sessionStatus is SessionStatus.Authenticated || supabaseClient.auth.currentUserOrNull() != null
    val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
    val state by viewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Void950)) {
        if (!isLoggedIn) {
            MessagesNotLoggedIn(onLoginClick = onLoginClick)
        } else {
            when (val current = state) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AetherViolet)
                }
                is UiState.Error -> MessagesError(message = current.message, onRetry = viewModel::retry)
                is UiState.Success -> ConversationsList(
                    conversations = current.data,
                    currentUserId = currentUserId,
                    onConversationClick = { otherUserId ->
                        navController.navigate(Screen.Thread.routeFor(otherUserId))
                    }
                )
            }
        }
    }
}

@Composable
private fun MessagesNotLoggedIn(onLoginClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = AstralGold,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.msg_login_required_title),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.msg_login_required_desc),
            color = Color(0xFF94A3B8),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onLoginClick,
            colors = ButtonDefaults.buttonColors(containerColor = AstralGold, contentColor = Void950),
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text(stringResource(R.string.msg_login_btn), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MessagesError(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.msg_load_failed), color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily))
        Spacer(Modifier.height(8.dp))
        Text(message, color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onRetry,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AetherViolet),
            border = BorderStroke(1.dp, AetherViolet.copy(alpha = 0.4f))
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.msg_retry))
        }
    }
}

@Composable
private fun ConversationsList(conversations: List<Conversation>, currentUserId: String?, onConversationClick: (String) -> Unit) {
    if (conversations.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.msg_no_conversations_title),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.msg_no_conversations_desc),
                    color = Color(0xFF94A3B8),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(conversations, key = { it.otherUser.id }) { conversation ->
            ConversationRow(conversation, currentUserId, onClick = { onConversationClick(conversation.otherUser.id) })
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
        }
    }
}

@Composable
private fun ConversationRow(conversation: Conversation, currentUserId: String?, onClick: () -> Unit) {
    val hasUnread = conversation.unreadCount > 0
    val isLastMessageMine = conversation.lastMessage.senderId == currentUserId

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Void800),
            contentAlignment = Alignment.Center
        ) {
            if (conversation.otherUser.avatarUrl != null) {
                AsyncImage(
                    model = conversation.otherUser.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Text(
                    conversation.otherUser.nameOrFallback.take(1).uppercase(),
                    color = AstralGold,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = conversation.otherUser.nameOrFallback,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLastMessageMine) {
                    Icon(
                        imageVector = if (conversation.lastMessage.isRead) Icons.Filled.DoneAll else Icons.Filled.Done,
                        contentDescription = null,
                        tint = if (conversation.lastMessage.isRead) SemanticSuccess400 else Color(0xFF64748B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = conversation.lastMessage.content
                        ?: when (conversation.lastMessage.attachmentType) {
                            "image" -> stringResource(R.string.msg_attachment_photo)
                            "video" -> stringResource(R.string.msg_attachment_video)
                            "file" -> stringResource(R.string.msg_attachment_file)
                            else -> if (conversation.lastMessage.attachmentType != null) stringResource(R.string.msg_attachment_generic) else ""
                        },
                    color = if (hasUnread) Color.White.copy(alpha = 0.85f) else Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (hasUnread) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(ShadowWorkRose),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (conversation.unreadCount > 9) "9+" else "${conversation.unreadCount}",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
