package io.lunosfer.dreamap.ui.screens

import android.widget.Toast
import io.lunosfer.dreamap.R
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
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.lunosfer.dreamap.data.model.AppNotification
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.NotificationsUiState
import io.lunosfer.dreamap.ui.viewmodel.NotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onDreamClick: (Long) -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: NotificationsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state) {
        val s = state as? NotificationsUiState.Success
        if (s?.actionError != null) {
            Toast.makeText(context, s.actionError, Toast.LENGTH_SHORT).show()
            viewModel.clearActionError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Bildirimler",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Color.White)
                    }
                },
                actions = {
                    val s = state as? NotificationsUiState.Success
                    if (s != null && s.unreadCount > 0) {
                        TextButton(onClick = { viewModel.markAsRead(null) }) {
                            Text(stringResource(R.string.notifications_mark_all_read), color = AstralGold, fontSize = 12.sp)
                        }
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
                is NotificationsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AstralGold)
                }
                is NotificationsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = s.message, color = SemanticDanger400)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.loadNotifications() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AstralGold)
                        ) {
                            Text(stringResource(R.string.notifications_refresh))
                        }
                    }
                }
                is NotificationsUiState.Success -> {
                    if (s.notifications.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Text(stringResource(R.string.notifications_empty), color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(s.notifications, key = { it.id }) { notification ->
                                NotificationRow(
                                    notification = notification,
                                    onClick = {
                                        if (!notification.isRead) {
                                            viewModel.markAsRead(notification.id)
                                        }
                                        if (notification.dreamId != null) {
                                            onDreamClick(notification.dreamId)
                                        } else if (!notification.actorId.isNullOrBlank()) {
                                            onUserClick(notification.actorId)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: AppNotification,
    onClick: () -> Unit
) {
    val actor = notification.actor
    val isUnread = !notification.isRead

    val (icon, title, body) = when (notification.type) {
        "new_follower" -> Triple(
            Icons.Default.Person,
            "Yeni Takipçi",
            "${actor?.nameOrFallback ?: "Biri"} seni takip etmeye başladı."
        )
        "friend_request" -> Triple(
            Icons.Default.PersonAdd,
            "Takip İsteği",
            "${actor?.nameOrFallback ?: "Biri"} sana takip isteği gönderdi."
        )
        "analysis_ready" -> Triple(
            Icons.Default.AutoAwesome,
            "Rüya Analizi Hazır",
            "Rüyanızın derin Jungcu analizi tamamlandı! Görmek için tıklayın."
        )
        "analysis_failed" -> Triple(
            Icons.Default.ErrorOutline,
            "Analiz Başarısız",
            "Rüya analizi oluşturulurken bir hata oluştu."
        )
        else -> Triple(
            Icons.Default.Notifications,
            "Yeni Bildirim",
            "Yeni bir bildiriminiz var."
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnread) Void800.copy(alpha = 0.8f) else Void900
        ),
        border = BorderStroke(
            1.dp,
            if (isUnread) AstralGold.copy(alpha = 0.4f) else Void800
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Actor Avatar or Type Icon
            if (actor?.avatarUrl != null && actor.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = actor.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isUnread) AstralGold.copy(alpha = 0.2f) else Void800
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isUnread) AstralGold else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (!notification.createdAt.isNullOrBlank()) {
                        Text(
                            text = notification.createdAt.take(10),
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = body,
                    color = if (isUnread) Color(0xFFE2E8F0) else Color.Gray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            // Unread Dot
            if (isUnread) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AstralGold)
                )
            }
        }
    }
}
