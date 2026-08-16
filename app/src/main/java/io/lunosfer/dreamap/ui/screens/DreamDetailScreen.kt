package io.lunosfer.dreamap.ui.screens

import android.widget.Toast
import io.lunosfer.dreamap.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.github.jan.supabase.auth.auth
import io.lunosfer.dreamap.data.model.*
import io.lunosfer.dreamap.supabase.supabaseClient
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.DreamDetailUiState
import io.lunosfer.dreamap.ui.viewmodel.DreamDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DreamDetailScreen(
    dreamId: Long,
    onBack: () -> Unit,
    onUserClick: ((String) -> Unit)? = null,
    viewModel: DreamDetailViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val currentUserId = supabaseClient.auth.currentUserOrNull()?.id

    LaunchedEffect(dreamId) {
        viewModel.loadDream(dreamId)
    }

    // Action error / success toasts
    LaunchedEffect(state) {
        val s = state as? DreamDetailUiState.Success
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
                is DreamDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AstralGold)
                }
                is DreamDetailUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Hata: ${s.message}", color = SemanticDanger400)
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { viewModel.loadDream(dreamId) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AstralGold)
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                is DreamDetailUiState.Success -> {
                    DreamDetailContent(
                        state = s,
                        currentUserId = currentUserId,
                        onBack = onBack,
                        onUserClick = onUserClick,
                        onRefresh = { viewModel.loadDream(dreamId) },
                        onAnalyze = { viewModel.analyzeDream(dreamId, s.dream.content, s.dream.originalLanguage ?: "en") },
                        onRequestDeepAnalysis = { viewModel.requestDeepAnalysis(dreamId) },
                        onToggleLike = { viewModel.toggleLike(dreamId, currentUserId) },
                        onAddComment = { text -> viewModel.addComment(dreamId, currentUserId, text) },
                        onDeleteComment = { commentId ->
                            if (currentUserId != null) viewModel.deleteComment(commentId, currentUserId)
                        },
                        onUpdateDream = { request ->
                            viewModel.updateDream(request) { viewModel.loadDream(dreamId) }
                        },
                        onDeleteDream = { softDelete ->
                            if (currentUserId != null) {
                                viewModel.deleteDream(dreamId, currentUserId, softDelete, onSuccess = onBack)
                            }
                        },
                        onBoostDream = { viewModel.boostDream(dreamId) },
                        onAddBounty = { amount -> viewModel.addBounty(dreamId, amount) }
                    )
                }
            }
        }
    }
}

@Composable
private fun getDetailSlideTitle(pageIndex: Int): String {
    return when (pageIndex) {
        0 -> stringResource(id = io.lunosfer.dreamap.R.string.dream_slide_title_0)
        1 -> stringResource(id = io.lunosfer.dreamap.R.string.dream_slide_title_1)
        2 -> stringResource(id = io.lunosfer.dreamap.R.string.dream_slide_title_2)
        else -> stringResource(R.string.dream_detail_slide_title_detail)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DreamDetailContent(
    state: DreamDetailUiState.Success,
    currentUserId: String?,
    onBack: () -> Unit,
    onUserClick: ((String) -> Unit)?,
    onRefresh: () -> Unit,
    onAnalyze: () -> Unit,
    onRequestDeepAnalysis: () -> Unit,
    onToggleLike: () -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: (Long) -> Unit,
    onUpdateDream: (UpdateDreamRequest) -> Unit,
    onDeleteDream: (softDelete: Boolean) -> Unit,
    onBoostDream: () -> Unit,
    onAddBounty: (Int) -> Unit
) {
    val dream = state.dream
    val isOwner = currentUserId != null && currentUserId == dream.userId

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBountyDialog by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { 3 })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val dateDisplay = try {
                        val date = sdf.parse(dream.dreamDate)
                        SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(date ?: Date())
                    } catch (e: Exception) {
                        dream.dreamDate.take(10)
                    }

                    Text(
                        text = dateDisplay,
                        color = AstralGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (dream.owner != null || dream.userId.isNotBlank()) {
                        Text(
                            text = "@${dream.owner?.username ?: dream.owner?.nameOrFallback ?: "yazar"}",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    if (onUserClick != null && dream.userId.isNotBlank()) {
                                        onUserClick(dream.userId)
                                    }
                                }
                        )
                    }

                    if (!dream.locationName.isNullOrBlank()) {
                        Text(
                            text = dream.locationName,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Visibility badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Void800)
                        .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)), shape = RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = dream.visibility.uppercase(),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    )
                }

                // Owner actions: Edit & Delete buttons
                if (isOwner) {
                    IconButton(onClick = { showEditDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.dream_detail_edit_btn), tint = AstralGold, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.dream_detail_delete_btn), tint = SemanticDanger400, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Page Indicator
        val slideLabel = getDetailSlideTitle(pagerState.currentPage)
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$slideLabel (${pagerState.currentPage + 1}/3)",
                color = Color(0xFF94A3B8),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            if (state.bounty > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AstralGold.copy(alpha = 0.2f))
                        .border(BorderStroke(0.5.dp, AstralGold), shape = RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dream_detail_bounty_label, state.bounty),
                        color = AstralGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3-Page Horizontal Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            when (page) {
                0 -> DreamImageCardPage(dream = dream)
                1 -> DreamTextCardPage(dream = dream)
                2 -> DreamAnalysisCardPage(
                    dream = dream,
                    isGeneratingDeepAnalysis = state.isGeneratingDeepAnalysis,
                    deepAnalysisResult = state.deepAnalysisResult,
                    onRefresh = onRefresh,
                    onAnalyze = onAnalyze,
                    onRequestDeepAnalysis = onRequestDeepAnalysis
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Social Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Void900)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape = RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Like Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onToggleLike() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (state.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(R.string.dream_detail_like_cd),
                    tint = if (state.isLiked) SemanticDanger500 else Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "${state.likesCount}",
                    color = if (state.isLiked) SemanticDanger500 else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Comment Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showCommentsSheet = !showCommentsSheet }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ModeComment,
                    contentDescription = stringResource(R.string.dream_detail_comments_cd),
                    tint = AstralGold,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "${state.commentsCount}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Owner features: Boost & Add Bounty
            if (isOwner) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onBoostDream,
                        colors = ButtonDefaults.buttonColors(containerColor = AetherViolet),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.dream_detail_boost_btn), fontSize = 11.sp, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = { showBountyDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AstralGold),
                        border = BorderStroke(1.dp, AstralGold),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.dream_detail_add_bounty_btn), fontSize = 11.sp)
                    }
                }
            }
        }

        // Inline Comments Section (Expandable)
        if (showCommentsSheet) {
            Spacer(modifier = Modifier.height(8.dp))
            CommentsSection(
                comments = state.comments,
                isLoading = state.isLoadingComments,
                isSubmitting = state.isSubmittingComment,
                currentUserId = currentUserId,
                onAddComment = onAddComment,
                onDeleteComment = onDeleteComment
            )
        }
    }

    // Edit Dream Dialog
    if (showEditDialog && currentUserId != null) {
        EditDreamDialog(
            dream = dream,
            currentUserId = currentUserId,
            onDismiss = { showEditDialog = false },
            onSave = { request ->
                onUpdateDream(request)
                showEditDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Void900,
            title = { Text(stringResource(R.string.dream_detail_delete_title), color = Color.White) },
            text = { Text(stringResource(R.string.dream_detail_delete_desc), color = Color.LightGray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteDream(false) // Hard delete
                    }
                ) {
                    Text(stringResource(R.string.dream_detail_delete_permanent), color = SemanticDanger400)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            onDeleteDream(true) // Soft delete
                        }
                    ) {
                        Text(stringResource(R.string.dream_detail_delete_hide), color = AstralGold)
                    }
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(R.string.dream_detail_cancel), color = Color.Gray)
                    }
                }
            }
        )
    }

    // Add Bounty Dialog
    if (showBountyDialog) {
        AddBountyDialog(
            onDismiss = { showBountyDialog = false },
            onConfirm = { amount ->
                onAddBounty(amount)
                showBountyDialog = false
            }
        )
    }
}

@Composable
private fun CommentsSection(
    comments: List<DreamComment>,
    isLoading: Boolean,
    isSubmitting: Boolean,
    currentUserId: String?,
    onAddComment: (String) -> Unit,
    onDeleteComment: (Long) -> Unit
) {
    var commentText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Void900),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text = stringResource(R.string.dream_detail_comments_count, comments.size),
                color = AstralGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = AstralGold,
                        modifier = Modifier.size(24.dp).align(Alignment.Center)
                    )
                } else if (comments.isEmpty()) {
                    Text(
                        text = stringResource(R.string.dream_detail_no_comments),
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(comments) { comment ->
                            CommentItemRow(
                                comment = comment,
                                currentUserId = currentUserId,
                                onDelete = { onDeleteComment(comment.id) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text(stringResource(R.string.dream_detail_comment_placeholder), color = Color.Gray, fontSize = 12.sp) },
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
                            contentDescription = stringResource(R.string.dream_detail_comment_send_cd),
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
private fun CommentItemRow(
    comment: DreamComment,
    currentUserId: String?,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Void800.copy(alpha = 0.5f))
            .padding(8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val avatarUrl = comment.userProfile?.avatarUrl
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = stringResource(R.string.dream_detail_avatar_cd),
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
                    contentDescription = stringResource(R.string.dream_detail_comment_delete_cd),
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun EditDreamDialog(
    dream: DreamDetail,
    currentUserId: String,
    onDismiss: () -> Unit,
    onSave: (UpdateDreamRequest) -> Unit
) {
    var content by remember { mutableStateOf(dream.content) }
    var locationName by remember { mutableStateOf(dream.locationName ?: "") }
    var visibility by remember { mutableStateOf(dream.visibility) }
    var inFeed by remember { mutableStateOf(dream.inFeed) }
    var tagInput by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf(dream.tags ?: emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val errEmptyText = stringResource(R.string.dream_detail_err_empty_text)
    val errTooLongText = stringResource(R.string.dream_detail_err_too_long)

    val maxTags = 10

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Void900,
        title = { Text(stringResource(R.string.dream_detail_edit_title), color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorMessage != null) {
                    Text(errorMessage!!, color = SemanticDanger400, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(R.string.dream_detail_edit_text_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AstralGold,
                        unfocusedBorderColor = Void800,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text(stringResource(R.string.dream_detail_edit_location_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AstralGold,
                        unfocusedBorderColor = Void800,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Tags Input
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { input ->
                        if (input.contains(",")) {
                            val cleanTag = input.replace(",", "").trim().lowercase()
                            if (cleanTag.isNotEmpty() && tags.size < maxTags && !tags.contains(cleanTag)) {
                                tags = tags + cleanTag
                            }
                            tagInput = ""
                        } else {
                            tagInput = input
                        }
                    },
                    label = { Text(stringResource(R.string.dream_detail_edit_tags_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AstralGold,
                        unfocusedBorderColor = Void800,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                if (tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.forEach { tag ->
                            AssistChip(
                                onClick = { tags = tags - tag },
                                label = { Text(tag, fontSize = 11.sp) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp)) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = Void800, labelColor = Color.White)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = inFeed,
                        onCheckedChange = { inFeed = it },
                        colors = CheckboxDefaults.colors(checkedColor = AstralGold, checkmarkColor = Void950)
                    )
                    Text(stringResource(R.string.dream_detail_edit_visibility_label), color = Color.White, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.isBlank()) {
                        errorMessage = errEmptyText
                        return@Button
                    }
                    if (content.length > 12000) {
                        errorMessage = errTooLongText
                        return@Button
                    }
                    val processedTags = tags.map { it.trim().lowercase() }.take(maxTags)
                    val req = UpdateDreamRequest(
                        dreamId = dream.id,
                        userId = currentUserId,
                        content = content.trim(),
                        locationName = locationName.trim(),
                        visibility = visibility,
                        inFeed = inFeed,
                        tags = processedTags
                    )
                    onSave(req)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AstralGold)
            ) {
                Text(stringResource(R.string.dream_detail_save_btn), color = Void950, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dream_detail_cancel), color = Color.Gray)
            }
        }
    )
}

@Composable
private fun AddBountyDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var amountText by remember { mutableStateOf("5") }
    var errorText by remember { mutableStateOf<String?>(null) }
    val errRangeText = stringResource(R.string.dream_detail_bounty_err_range)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Void900,
        title = { Text(stringResource(R.string.dream_detail_bounty_title), color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.dream_detail_bounty_desc), color = Color.LightGray, fontSize = 13.sp)
                if (errorText != null) {
                    Text(errorText!!, color = SemanticDanger400, fontSize = 12.sp)
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
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
                    val amt = amountText.toIntOrNull()
                    if (amt == null || amt < 1 || amt > 50) {
                        errorText = errRangeText
                        return@Button
                    }
                    onConfirm(amt)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AstralGold)
            ) {
                Text(stringResource(R.string.dream_detail_add_btn), color = Void950, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dream_detail_cancel), color = Color.Gray)
            }
        }
    )
}

@Composable
private fun DreamImageCardPage(
    dream: DreamDetail
) {
    val locale = Locale.getDefault().language
    val titleMap = dream.aiJungianAnalysis?.title
    val titleText = titleMap?.get(locale)
        ?: titleMap?.get("en")
        ?: dream.content.take(40)

    val archetypes = dream.aiJungianAnalysis?.archetypes
        ?: dream.tags

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Void900),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val imageUrl = dream.displayImageUrl
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = stringResource(R.string.dream_detail_image_cd),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        AstralGold.copy(alpha = 0.18f),
                                        AetherViolet.copy(alpha = 0.28f),
                                        Void900
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = AstralGold.copy(alpha = 0.7f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "LUNOSFER",
                                color = AstralGold.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }

                // Dark gradient scrim ONLY inside this image card at the bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Void950.copy(alpha = 0.75f),
                                    Void950.copy(alpha = 0.95f)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (titleText.isNotBlank()) {
                            Text(
                                text = titleText,
                                color = AstralGold,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = SerifFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!archetypes.isNullOrEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(archetypes) { arch ->
                                    ChipView(text = arch, isSelected = true)
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
private fun DreamTextCardPage(
    dream: DreamDetail
) {
    val titleLabel = getDetailSlideTitle(1)

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Void900),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "📖", fontSize = 18.sp)
                Text(
                    text = titleLabel,
                    color = AstralGold,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SerifFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Text(
                text = dream.content,
                color = Color(0xFFE2E8F0),
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 26.sp,
                    letterSpacing = 0.2.sp
                ),
                fontSize = 15.sp
            )

            if (!dream.userSelectedSentiment.isNullOrBlank()) {
                val emotions = dream.userSelectedSentiment.split(",").map { it.trim() }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(emotions) { emotion ->
                        ChipView(text = emotion, isSelected = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun DreamAnalysisCardPage(
    dream: DreamDetail,
    isGeneratingDeepAnalysis: Boolean = false,
    deepAnalysisResult: String? = null,
    onRefresh: () -> Unit,
    onAnalyze: () -> Unit,
    onRequestDeepAnalysis: () -> Unit
) {
    val locale = Locale.getDefault().language
    val titleLabel = getDetailSlideTitle(2)

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Void900),
        border = BorderStroke(1.dp, AetherViolet.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "✨", fontSize = 18.sp)
                    Text(
                        text = titleLabel,
                        color = AstralGold,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = SerifFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AetherViolet.copy(alpha = 0.2f))
                        .border(BorderStroke(0.5.dp, AetherViolet.copy(alpha = 0.4f)), shape = RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "AI JUNG",
                        color = AstralGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            when (dream.analysisStatus) {
                "completed" -> {
                    val analysis = dream.aiJungianAnalysis
                    if (analysis != null) {
                        val title = analysis.title?.get(locale)
                            ?: analysis.title?.get("en")
                        val summary = analysis.summary?.get(locale)
                            ?: analysis.summary?.get("en")
                            ?: ""
                        val motiv = analysis.motiv?.get(locale)
                            ?: analysis.motiv?.get("en")
                            ?: ""

                        if (!title.isNullOrBlank()) {
                            Text(
                                text = title,
                                color = AstralGold,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = SerifFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        if (summary.isNotBlank()) {
                            Text(
                                text = summary,
                                color = Color(0xFFE2E8F0),
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                            )
                        }

                        if (!analysis.sentiment.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Void800)
                                    .border(BorderStroke(0.5.dp, AstralGold.copy(alpha = 0.3f)), shape = RoundedCornerShape(50))
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.dream_detail_sentiment_label, analysis.sentiment),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (motiv.isNotBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Void800.copy(alpha = 0.6f)),
                                border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "\"$motiv\"",
                                    color = AstralGold,
                                    fontStyle = FontStyle.Italic,
                                    modifier = Modifier.padding(14.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        if (!analysis.archetypes.isNullOrEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(analysis.archetypes) { arch ->
                                    ChipView(text = arch, isSelected = true)
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Derinlemesine Analiz Bölümü
                    val activeDeepResult = deepAnalysisResult ?: dream.aiJungianAnalysis?.summary?.get("deep")
                    if (!activeDeepResult.isNullOrBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Void800.copy(alpha = 0.8f)),
                            border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🔮", fontSize = 16.sp)
                                    Text(
                                        text = stringResource(R.string.dream_detail_deep_analysis_title),
                                        color = AstralGold,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily)
                                    )
                                }
                                Text(
                                    text = activeDeepResult,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onRequestDeepAnalysis,
                            enabled = !isGeneratingDeepAnalysis,
                            colors = ButtonDefaults.buttonColors(containerColor = AetherViolet),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isGeneratingDeepAnalysis) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.dream_detail_deep_analysis_loading), color = Color.White, fontSize = 13.sp)
                            } else {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = AstralGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.dream_detail_request_deep_analysis_btn), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
                "failed" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(io.lunosfer.dreamap.R.string.dream_analysis_failed),
                            color = SemanticDanger400,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onAnalyze,
                            colors = ButtonDefaults.buttonColors(containerColor = AstralGold)
                        ) {
                            Text(
                                stringResource(io.lunosfer.dreamap.R.string.dream_analysis_retry),
                                color = Void950,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                else -> { // processing or null
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(io.lunosfer.dreamap.R.string.dream_analysis_pending),
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh_button_desc), tint = AstralGold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipView(text: String, isSelected: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) AstralGold.copy(alpha = 0.2f) else Void800)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) AstralGold else Color.White,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
