package io.lunosfer.dreamap.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.Dream
import io.lunosfer.dreamap.data.model.FeedItem
import io.lunosfer.dreamap.data.model.Goal
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.HomeViewModel
import io.lunosfer.dreamap.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Locale

import io.lunosfer.dreamap.ui.components.DiaryRingsBar

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onDreamClick: (Long) -> Unit = {},
    onOpenComposer: () -> Unit = {},
    onOpenViewer: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Void950)) {
        when (val current = state) {
            is UiState.Loading -> HomeLoading()
            is UiState.Error -> HomeError(message = current.message, onRetry = viewModel::retry)
            is UiState.Success -> HomeFeedList(
                items = current.data,
                onDreamClick = onDreamClick,
                onOpenComposer = onOpenComposer,
                onOpenViewer = onOpenViewer
            )
        }
    }
}

@Composable
private fun HomeLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AstralGold)
    }
}

@Composable
private fun HomeError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.error_feed),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            color = Color(0xFF94A3B8),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(contentColor = AstralGold),
            border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.4f))
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun HomeFeedList(
    items: List<FeedItem>,
    onDreamClick: (Long) -> Unit,
    onOpenComposer: () -> Unit,
    onOpenViewer: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            DiaryRingsBar(
                onOpenComposer = onOpenComposer,
                onOpenViewer = onOpenViewer
            )
        }

        if (items.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.home_feed_empty_title),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.home_feed_empty_desc),
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.home_welcome_title),
                    color = AstralGold.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 2.sp,
                        fontFamily = SansFontFamily
                    )
                )
                Text(
                    text = "Astral Gezgin",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = SerifFontFamily,
                        fontWeight = FontWeight.Light
                    )
                )
            }
        }

        items(items, key = { it.createdAt + it.hashCode() }) { feedItem ->
            when (feedItem) {
                is FeedItem.DreamItem -> DreamFeedCard(feedItem.dream, onDreamClick)
                is FeedItem.VisionItem -> VisionFeedCard(feedItem.goal)
            }
        }
    }
}

@Composable
private fun FeedCardOwnerHeader(
    ownerName: String,
    avatarUrl: String?,
    dreamDate: String? = null,
    visibility: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Void800),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(
                        ownerName.take(1).uppercase(),
                        color = AstralGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column {
                Text(
                    ownerName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!dreamDate.isNullOrBlank()) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val dateDisplay = try {
                        val date = sdf.parse(dreamDate)
                        SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(date ?: java.util.Date())
                    } catch (e: Exception) {
                        dreamDate.take(10)
                    }
                    Text(
                        dateDisplay,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (!visibility.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Void800)
                    .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)), shape = RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = visibility.uppercase(),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

@Composable
fun getHomeSlideTitle(pageIndex: Int): String {
    return when (pageIndex) {
        0 -> stringResource(R.string.dream_slide_title_0)
        1 -> stringResource(R.string.dream_slide_title_1)
        2 -> stringResource(R.string.dream_slide_title_2)
        else -> ""
    }
}

/**
 * Ana sayfa kartı - 3 Sayfalı Yana Kaydırılabilir Pager (Görsel + Metin + AI Analiz)
 * Instagram Post formatında sabit 4:5 frame yüksekliği, kaydırırken boyut değişmez.
 */
@Composable
private fun DreamFeedCard(dream: Dream, onDreamClick: (Long) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val currentLocale = Locale.getDefault().language

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Void900),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Owner, Date, Visibility
            FeedCardOwnerHeader(
                ownerName = dream.owner?.nameOrFallback ?: "Bilinmeyen",
                avatarUrl = dream.owner?.avatarUrl,
                dreamDate = dream.dreamDate ?: dream.createdAt,
                visibility = dream.visibility
            )

            // Top-left page indicator text (e.g., "Rüya Görseli (1/3)") in gray monospace style
            val slideLabel = getHomeSlideTitle(pagerState.currentPage)
            Text(
                text = "$slideLabel (${pagerState.currentPage + 1}/3)",
                color = Color(0xFF94A3B8),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
            )

            // Sabit 4:5 Aspect Ratio Pager Çerçevesi (Instagram Post Tarzı)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 5f)
            ) { page ->
                when (page) {
                    0 -> DreamImagePage(dream = dream, onDreamClick = onDreamClick)
                    1 -> DreamTextPage(dream = dream)
                    2 -> DreamAnalysisPage(dream = dream)
                }
            }

            // Paylaşım Alt Etkileşim Barı (Beğeni, Yorum, Detay Linki)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, start = 2.dp, end = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("❤ ${dream.likesCount ?: 0}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Message,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("${dream.commentsCount ?: 0}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }

                Text(
                    text = stringResource(R.string.home_feed_detail_btn),
                    color = AstralGold.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onDreamClick(dream.id) }
                )
            }
        }
    }
}

@Composable
private fun DreamImagePage(dream: Dream, onDreamClick: (Long) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onDreamClick(dream.id) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Void800),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!dream.aiImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = dream.aiImageUrl,
                    contentDescription = dream.displayTitle,
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
                                    Void800
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
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "LUNOSFER",
                            color = AstralGold.copy(alpha = 0.6f),
                            fontSize = 11.sp,
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
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = dream.displayTitle,
                        color = AstralGold,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = SerifFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!dream.aiArchetypes.isNullOrEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(dream.aiArchetypes) { arch ->
                                ChipView(text = arch, isSelected = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DreamTextPage(dream: Dream) {
    val titleLabel = getHomeSlideTitle(1)

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Void800),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "📖", fontSize = 16.sp)
                Text(
                    text = titleLabel,
                    color = AstralGold,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SerifFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            if (!dream.aiTitle.isNullOrBlank()) {
                Text(
                    text = dream.aiTitle,
                    color = AstralGold,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = SerifFontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Text(
                text = dream.content,
                color = Color(0xFFE2E8F0),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                fontSize = 14.sp
            )

            if (!dream.userSelectedSentiment.isNullOrBlank()) {
                val emotions = dream.userSelectedSentiment.split(",").map { it.trim() }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(emotions) { emotion ->
                        ChipView(text = emotion)
                    }
                }
            }
        }
    }
}

@Composable
private fun DreamAnalysisPage(dream: Dream) {
    val titleLabel = getHomeSlideTitle(2)
    val analysis = dream.aiJungianAnalysis
    val locale = Locale.getDefault().language

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Void800),
        border = BorderStroke(1.dp, AetherViolet.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "✨", fontSize = 16.sp)
                    Text(
                        text = titleLabel,
                        color = AstralGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AetherViolet.copy(alpha = 0.2f))
                        .border(BorderStroke(0.5.dp, AetherViolet.copy(alpha = 0.4f)), shape = RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("AI JUNG", color = AstralGold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            val titleText = analysis?.title?.get(locale)
                ?: analysis?.title?.get("en")
                ?: dream.displayTitle

            Text(
                text = titleText,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = SerifFontFamily,
                    fontWeight = FontWeight.Bold
                )
            )

            val summaryText = analysis?.summary?.get(locale)
                ?: analysis?.summary?.get("en")

            if (!summaryText.isNullOrBlank()) {
                Text(
                    text = summaryText,
                    color = Color(0xFFE2E8F0),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    fontSize = 13.sp
                )
            }

            val motivText = analysis?.motiv?.get(locale)
                ?: analysis?.motiv?.get("en")

            if (!motivText.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Void900.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "\"$motivText\"",
                        color = AstralGold,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (!dream.userSelectedSentiment.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Void900)
                        .border(BorderStroke(0.5.dp, AstralGold.copy(alpha = 0.3f)), shape = RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_feed_sentiment_label, dream.userSelectedSentiment),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            val archetypes = analysis?.archetypes ?: dream.aiArchetypes
            if (!archetypes.isNullOrEmpty()) {
                Text(
                    text = stringResource(R.string.home_feed_archetypes_label),
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(archetypes) { arch ->
                        ChipView(text = arch, isSelected = true)
                    }
                }
            }
        }
    }
}

/** goals tablosundan bir kart — GoalCard.jsx'in ön yüzüyle aynı alanlar (title, cover, completion). */
@Composable
private fun VisionFeedCard(goal: Goal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Void900),
        border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FeedCardOwnerHeader(
                ownerName = goal.owner?.nameOrFallback ?: "Bilinmeyen",
                avatarUrl = goal.owner?.avatarUrl,
                visibility = stringResource(R.string.home_feed_vision_badge)
            )

            Box {
                if (goal.coverImageUrl != null) {
                    AsyncImage(
                        model = goal.coverImageUrl,
                        contentDescription = goal.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Void800),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.TrackChanges, contentDescription = null, tint = AstralGold.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                    }
                }
            }

            Text(
                text = goal.title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(((goal.completionPercentage ?: 0) / 100f).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(AstralGold)
                    )
                }
                Text(stringResource(R.string.home_feed_completion_text, goal.completionPercentage ?: 0), color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ChipView(text: String, isSelected: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isSelected) AstralGold.copy(alpha = 0.15f) else Void800)
            .border(
                BorderStroke(
                    0.5.dp,
                    if (isSelected) AstralGold.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) AstralGold else Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
