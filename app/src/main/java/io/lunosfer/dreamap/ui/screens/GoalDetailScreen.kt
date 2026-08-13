package io.lunosfer.dreamap.ui.screens

import android.widget.Toast
import io.lunosfer.dreamap.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ModeComment
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
import io.github.jan.supabase.auth.auth
import io.lunosfer.dreamap.data.model.Goal
import io.lunosfer.dreamap.data.model.GoalComment
import io.lunosfer.dreamap.data.model.MicroGoal
import io.lunosfer.dreamap.supabase.supabaseClient
import io.lunosfer.dreamap.ui.components.PixabayMediaPickerDialog
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.GoalDetailUiState
import io.lunosfer.dreamap.ui.viewmodel.GoalDetailViewModel

@Composable
fun GoalDetailScreen(
    goalId: String,
    onBack: () -> Unit,
    onUserClick: ((String) -> Unit)? = null
) {
    val factory = remember(goalId) { GoalDetailViewModel.Factory(goalId) }
    val viewModel: GoalDetailViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val currentUserId = supabaseClient.auth.currentUserOrNull()?.id

    LaunchedEffect(state) {
        val s = state as? GoalDetailUiState.Success
        if (s?.actionError != null) {
            Toast.makeText(context, s.actionError, Toast.LENGTH_SHORT).show()
            viewModel.clearActionError()
        }
        if (s?.actionMessage != null) {
            Toast.makeText(context, s.actionMessage, Toast.LENGTH_SHORT).show()
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        containerColor = Void950
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (val s = state) {
                is GoalDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AstralGold)
                }
                is GoalDetailUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = s.message, color = Color(0xFFF87171))
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { viewModel.loadGoal() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AstralGold)
                        ) {
                            Text("Tekrar Dene")
                        }
                    }
                }
                is GoalDetailUiState.Success -> {
                    GoalDetailContent(
                        state = s,
                        currentUserId = currentUserId,
                        onBack = onBack,
                        onUserClick = onUserClick,
                        onToggleSave = viewModel::toggleSave,
                        onGiveMana = viewModel::giveMana,
                        onRemoveMana = viewModel::removeMana,
                        onAddComment = viewModel::addComment,
                        onDeleteComment = viewModel::deleteComment,
                        onUpdateStatus = { status, story, onComplete ->
                            viewModel.updateStatus(status, story, onComplete)
                        },
                        onDeleteGoal = {
                            viewModel.deleteGoal(onSuccess = onBack)
                        },
                        onGenerateCover = viewModel::generateCover,
                        onAddPixabayImage = viewModel::addPixabayImage,
                        onAddUrlImage = viewModel::addUrlImage,
                        onRemoveImage = viewModel::removeImage,
                        onTranslateText = viewModel::translate
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalDetailContent(
    state: GoalDetailUiState.Success,
    currentUserId: String?,
    onBack: () -> Unit,
    onUserClick: ((String) -> Unit)?,
    onToggleSave: () -> Unit,
    onGiveMana: (Int) -> Unit,
    onRemoveMana: () -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onUpdateStatus: (String, String?, () -> Unit) -> Unit,
    onDeleteGoal: () -> Unit,
    onGenerateCover: () -> Unit = {},
    onAddPixabayImage: (Long, String, String, String) -> Unit = { _, _, _, _ -> },
    onAddUrlImage: (String) -> Unit = {},
    onRemoveImage: (String) -> Unit = {},
    onTranslateText: (String, (String) -> Unit) -> Unit = { _, _ -> }
) {
    val goal = state.goal
    val isOwner = currentUserId != null && currentUserId == goal.userId

    var showStatusDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCommentsSection by remember { mutableStateOf(true) }
    var showPixabayDialog by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var urlInputText by remember { mutableStateOf("") }
    var translatedDesc by remember { mutableStateOf<String?>(null) }
    var isTranslatingDesc by remember { mutableStateOf(false) }

    if (showPixabayDialog) {
        PixabayMediaPickerDialog(
            onDismissRequest = { showPixabayDialog = false },
            onImageSelected = { pixabayId, imageUrl, tags, user ->
                onAddPixabayImage(pixabayId, imageUrl, tags, user)
            },
            onVideoSelected = { pixabayId, videoUrl, tags, user ->
                onAddPixabayImage(pixabayId, videoUrl, tags, user)
            }
        )
    }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            containerColor = Void900,
            title = { Text("URL ile Görsel Ekle", color = AstralGold, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = urlInputText,
                    onValueChange = { urlInputText = it },
                    placeholder = { Text("https://example.com/image.jpg", color = Color.Gray, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AstralGold,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (urlInputText.isNotBlank()) {
                            onAddUrlImage(urlInputText.trim())
                            urlInputText = ""
                            showUrlDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AetherViolet)
                ) {
                    Text("Ekle", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text("İptal", color = Color.Gray)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Nav Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Color.White)
            }

            Text(
                text = stringResource(R.string.goal_detail_title),
                color = AstralGold,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleSave) {
                    Icon(
                        imageVector = if (state.hasSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Kaydet",
                        tint = AstralGold
                    )
                }

                if (isOwner) {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Sil", tint = Color(0xFFF87171))
                    }
                }
            }
        }

        // Main Goal Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Void900),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cover Image if present
                if (!goal.coverImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = goal.coverImageUrl,
                        contentDescription = goal.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }

                // Cover & Gallery Management Bar (Owner)
                if (isOwner) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = onGenerateCover,
                            label = { Text("🤖 AI Kapak", fontSize = 10.sp, color = AstralGold) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = Void800)
                        )
                        AssistChip(
                            onClick = { showPixabayDialog = true },
                            label = { Text("🖼️ Pixabay", fontSize = 10.sp, color = Color.White) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = Void800)
                        )
                        AssistChip(
                            onClick = { showUrlDialog = true },
                            label = { Text("🔗 URL", fontSize = 10.sp, color = Color.White) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = Void800)
                        )
                        if (!goal.coverImageUrl.isNullOrBlank()) {
                            AssistChip(
                                onClick = { onRemoveImage(goal.coverImageUrl) },
                                label = { Text("🗑️", fontSize = 10.sp, color = Color(0xFFF87171)) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = Void800)
                            )
                        }
                    }
                }

                // Author Info Row
                if (goal.owner != null || goal.userId.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { if (onUserClick != null && goal.userId.isNotBlank()) onUserClick(goal.userId) }
                            .padding(vertical = 2.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!goal.owner?.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = goal.owner?.avatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(AstralGold.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = goal.owner?.nameOrFallback?.take(1)?.uppercase() ?: "?",
                                    color = AstralGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = goal.owner?.nameOrFallback ?: "@${goal.userId.take(8)}",
                            color = AstralGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Header Row: Status Badge & Believers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText = when (goal.status) {
                        "completed" -> "TAMAMLANDI 🎉"
                        "abandoned" -> "BIRAKILDI"
                        else -> "DEVAM EDİYOR"
                    }
                    val statusBg = when (goal.status) {
                        "completed" -> Color(0xFF10B981)
                        "abandoned" -> Color(0xFF6B7280)
                        else -> AetherViolet
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(statusBg.copy(alpha = 0.2f))
                            .border(BorderStroke(0.5.dp, statusBg), shape = RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusBg,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = "Mana", tint = AstralGold, modifier = Modifier.size(16.dp))
                        Text("${state.believersCount} Mana", color = AstralGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Title
                Text(
                    text = goal.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = SerifFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Description
                if (!goal.description.isNullOrBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = goal.description,
                            color = Color(0xFFCBD5E1),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )

                        // Translate option
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    if (translatedDesc == null) {
                                        isTranslatingDesc = true
                                        onTranslateText(goal.description) { res ->
                                            isTranslatingDesc = false
                                            translatedDesc = res
                                        }
                                    } else {
                                        translatedDesc = null
                                    }
                                }
                            ) {
                                Text(
                                    text = if (isTranslatingDesc) "Çevriliyor..." else if (translatedDesc != null) "Orijinali Gör" else "🌐 Çevir",
                                    color = AstralGold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (translatedDesc != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Void800)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = translatedDesc ?: "",
                                    color = Color.LightGray,
                                    fontSize = 13.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                }

                // Target Date
                if (!goal.targetDate.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = AstralGold, modifier = Modifier.size(14.dp))
                        Text(
                            text = stringResource(R.string.goal_detail_target_date, goal.targetDate.take(10)),
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }

                // Progress Bar
                val progress = (goal.completionPercentage ?: 0) / 100f
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("İlerleme", color = Color.Gray, fontSize = 11.sp)
                        Text("%${goal.completionPercentage ?: 0}", color = AstralGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = AstralGold,
                        trackColor = Void800
                    )
                }

                // Micro Goals / Roadmap
                val microGoals = goal.microGoals
                if (!microGoals.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Yol Haritası (${microGoals.count { it.isCompleted }}/${microGoals.size})", color = AstralGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        microGoals.forEach { mg ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (mg.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (mg.isCompleted) AstralGold else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = mg.title,
                                    color = if (mg.isCompleted) Color.Gray else Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Owner Info Row
                val owner = goal.owner
                if (owner != null) {
                    Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!owner.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = owner.avatarUrl,
                                contentDescription = owner.nameOrFallback,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                            )
                        }
                        Text("Vizyon Sahibi: ${owner.nameOrFallback}", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }

        // Action Buttons Row (Owner: Update Status | Non-Owner: Give/Remove Mana)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isOwner && goal.status == "active") {
                Button(
                    onClick = { showStatusDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AetherViolet)
                ) {
                    Text("Hedef Durumunu Güncelle", fontSize = 13.sp, color = Color.White)
                }
            } else if (!isOwner && goal.status == "active") {
                if (!state.hasReacted) {
                    Button(
                        onClick = { onGiveMana(1) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AstralGold)
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Void950, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Mana Ver (+1)", color = Void950, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onRemoveMana,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AstralGold),
                        border = BorderStroke(1.dp, AstralGold)
                    ) {
                        Text("Mana Reaksiyonunu Kaldır", fontSize = 12.sp)
                    }
                }
            }
        }

        // Comments Section
        GoalCommentsView(
            comments = state.comments,
            isLoading = state.isLoadingComments,
            isSubmitting = state.isSubmittingComment,
            currentUserId = currentUserId,
            onAddComment = onAddComment,
            onDeleteComment = onDeleteComment
        )
    }

    // Status Update Dialog
    if (showStatusDialog) {
        UpdateGoalStatusDialog(
            onDismiss = { showStatusDialog = false },
            onConfirm = { status, story ->
                onUpdateStatus(status, story) {
                    showStatusDialog = false
                }
            }
        )
    }

    // Delete Goal Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Void900,
            title = { Text("Vizyonu Sil", color = Color.White) },
            text = { Text("Bu vizyonu ve ilişkili mikro hedefleri silmek istediğinizden emin misiniz?", color = Color.LightGray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteGoal()
                    }
                ) {
                    Text("Sil", color = Color(0xFFF87171))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("İptal", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
private fun GoalCommentsView(
    comments: List<GoalComment>,
    isLoading: Boolean,
    isSubmitting: Boolean,
    currentUserId: String?,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Void900),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.goal_detail_comments_count, comments.size),
                color = AstralGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            if (isLoading) {
                CircularProgressIndicator(
                    color = AstralGold,
                    modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally)
                )
            } else if (comments.isEmpty()) {
                Text(
                    text = stringResource(R.string.goal_detail_no_comments),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            } else {
                comments.forEach { comment ->
                    GoalCommentRow(
                        comment = comment,
                        currentUserId = currentUserId,
                        onDelete = { onDeleteComment(comment.id) }
                    )
                }
            }

            // Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { if (it.length <= 1000) commentText = it },
                    placeholder = { Text("Yorum yazın...", color = Color.Gray, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AstralGold,
                        unfocusedBorderColor = Void800,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            onAddComment(commentText)
                            commentText = ""
                        }
                    },
                    enabled = !isSubmitting && commentText.isNotBlank(),
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (commentText.isNotBlank()) AstralGold else Void800)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Void950, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Gönder",
                            tint = Void950,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalCommentRow(
    comment: GoalComment,
    currentUserId: String?,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Void800.copy(alpha = 0.5f))
            .padding(10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val avatarUrl = comment.userProfile?.avatarUrl
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AstralGold.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = comment.userProfile?.nameOrFallback?.take(1)?.uppercase() ?: "?",
                    color = AstralGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = comment.userProfile?.nameOrFallback ?: "Kullanıcı",
                color = AstralGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = comment.content,
                color = Color.White,
                fontSize = 12.sp
            )
        }

        if (currentUserId != null && currentUserId == comment.userId) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Sil",
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun UpdateGoalStatusDialog(
    onDismiss: () -> Unit,
    onConfirm: (status: String, story: String?) -> Unit
) {
    var selectedStatus by remember { mutableStateOf("completed") }
    var storyText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Void900,
        title = { Text("Vizyon Durumunu Güncelle", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Vizyon sonucunu seçin:", color = Color.LightGray, fontSize = 13.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedStatus == "completed",
                        onClick = { selectedStatus = "completed" },
                        label = { Text("🎉 Tamamlandı", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF10B981),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedStatus == "abandoned",
                        onClick = { selectedStatus = "abandoned" },
                        label = { Text("Vazgeçtim", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6B7280),
                            selectedLabelColor = Color.White
                        )
                    )
                }

                OutlinedTextField(
                    value = storyText,
                    onValueChange = { if (it.length <= 2000) storyText = it },
                    label = {
                        Text(if (selectedStatus == "completed") "Zafer Hikayesi (Opsiyonel)" else "Vazgeçme Nedeni (Opsiyonel)")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AstralGold,
                        unfocusedBorderColor = Void800,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedStatus, storyText.trim().ifEmpty { null })
                },
                colors = ButtonDefaults.buttonColors(containerColor = AstralGold)
            ) {
                Text("Güncelle", color = Void950, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal", color = Color.Gray)
            }
        }
    )
}
