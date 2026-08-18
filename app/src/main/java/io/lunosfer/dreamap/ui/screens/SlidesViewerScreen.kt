package io.lunosfer.dreamap.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.GoalSlide
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.SlidesViewerUiState
import io.lunosfer.dreamap.ui.viewmodel.SlidesViewerViewModel

/**
 * "Vizyon Slaytları" — components/SlidesViewer.jsx'in Android karşılığı.
 * Oto-oynatan, Stories tarzı tam ekran deneyim. Kapsam: izleme + mana ver +
 * slayt kaydet + (sahipse) sil. Yorumlar/bildir/düzenle bu sürümde yok.
 */
@Composable
fun SlidesViewerScreen(
    goalId: String,
    onBack: () -> Unit,
    onGoalClick: (String) -> Unit
) {
    val factory = remember(goalId) { SlidesViewerViewModel.Factory(goalId) }
    val viewModel: SlidesViewerViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        if (state is SlidesViewerUiState.Closed) {
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val s = state) {
            is SlidesViewerUiState.Loading -> {
                CircularProgressIndicator(
                    color = AstralGold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is SlidesViewerUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(s.message, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = AstralGold)
                    ) {
                        Text(text = "Geri Dön", color = Void950)
                    }
                }
            }
            is SlidesViewerUiState.Content -> {
                SlidesViewerContent(
                    state = s,
                    onPause = viewModel::pauseTimer,
                    onResume = viewModel::resumeTimer,
                    onNext = viewModel::nextSlide,
                    onPrevious = viewModel::previousSlide,
                    onClose = onBack,
                    onGoalClick = { s.goal?.let { onGoalClick(it.id) } },
                    onToggleMana = viewModel::toggleMana,
                    onToggleSave = viewModel::toggleSaveSlide,
                    onDelete = viewModel::deleteCurrentSlide
                )
            }
            is SlidesViewerUiState.Closed -> {}
        }
    }
}

// Bazı projelerde "back"/"close" gibi ortak string'ler farklı adlarla var
// olabilir; burada sabit bir Türkçe metne düşüyoruz ki eksik kaynak
// yüzünden derleme kırılmasın.

@Composable
private fun SlidesViewerContent(
    state: SlidesViewerUiState.Content,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    onGoalClick: () -> Unit,
    onToggleMana: () -> Unit,
    onToggleSave: () -> Unit,
    onDelete: () -> Unit
) {
    val slide = state.currentSlide ?: return
    val isPlaying = !state.isPaused

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(state.currentIndex) {
                detectTapGestures(
                    onPress = {
                        onPause()
                        tryAwaitRelease()
                        onResume()
                    },
                    onTap = { offset ->
                        if (offset.x < size.width * 0.35f) onPrevious() else onNext()
                    }
                )
            }
    ) {
        // --- Medya (video ya da Ken Burns efektli görsel) ---
        if (slide.isVideo) {
            SlideVideoLayer(slide = slide, isPlaying = isPlaying)
        } else {
            SlideImageLayer(slide = slide, progress = state.progress, variantSeed = state.currentIndex)
        }

        // --- Başlık overlay'i (alt gradyan üstünde) ---
        if (!slide.caption.isNullOrBlank()) {
            val captionColor = remember(slide.captionColor) {
                runCatching { Color(AndroidColor.parseColor(slide.captionColor ?: "#ffffff")) }
                    .getOrDefault(Color.White)
            }
            val fontFamily = when (slide.captionFont) {
                "serif" -> SerifFontFamily
                "mono" -> FontFamily.Monospace
                else -> SansFontFamily
            }
            val sizeMultiplier = slide.captionSize ?: 1f
            val biasX = (((slide.captionX ?: 50f) / 100f) * 2f - 1f).coerceIn(-1f, 1f)
            val biasY = (((slide.captionY ?: 85f) / 100f) * 2f - 1f).coerceIn(-1f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = BiasAlignment(biasX, biasY)
            ) {
                Text(
                    text = slide.caption,
                    color = captionColor,
                    fontFamily = fontFamily,
                    fontSize = (22 * sizeMultiplier).sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = (28 * sizeMultiplier).sp
                )
            }
        }

        // --- Üst overlay: ilerleme çubukları + başlık satırı ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                state.slides.forEachIndexed { index, _ ->
                    val progressVal = when {
                        index < state.currentIndex -> 1f
                        index == state.currentIndex -> state.progress
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { progressVal },
                        modifier = Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val avatarUrl = state.owner?.avatarUrl
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(36.dp).clip(CircleShape).border(1.dp, AstralGold, CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(AstralGold.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.owner?.nameOrFallback?.take(1)?.uppercase() ?: "?",
                                color = AstralGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column {
                        Text(
                            text = state.owner?.nameOrFallback ?: "",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        state.goal?.title?.let { title ->
                            Surface(
                                onClick = onGoalClick,
                                shape = RoundedCornerShape(12.dp),
                                color = AstralGold.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.TrackChanges, null, tint = AstralGold, modifier = Modifier.size(11.dp))
                                    Text(
                                        text = title,
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

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (state.isOwner) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Sil", tint = SemanticDanger400)
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                    }
                }
            }
        }

        // --- Sağ alt aksiyon şeridi: Mana ver + Kaydet ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (!state.isOwner) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onToggleMana) {
                        Icon(
                            imageVector = if (state.hasReacted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (state.hasReacted) SemanticDanger500 else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "${state.believersCount}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onToggleSave) {
                    Icon(
                        imageVector = if (slide.hasSaved == true) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = if (slide.hasSaved == true) BrandSecondary400 else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = "${slide.savesCount ?: 0}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SlideImageLayer(slide: GoalSlide, progress: Float, variantSeed: Int) {
    // Ken Burns: durağan görsele slaytın kendi süresi boyunca yavaş bir
    // zoom/pan uygulanır — SlidesViewer.jsx'teki 4 varyantlı efektin
    // basitleştirilmiş karşılığı. Aynı "progress" değeri hem üstteki
    // ilerleme çubuğunu hem bu efekti sürdüğü için duraklatınca ikisi de
    // birlikte donuyor.
    val variant = variantSeed % 4
    val scale = 1f + (progress * 0.12f)
    val panRange = 24f
    val (panX, panY) = when (variant) {
        0 -> (-progress * panRange) to (-progress * panRange)
        1 -> (progress * panRange) to (-progress * panRange)
        2 -> (-progress * panRange) to (progress * panRange)
        else -> (progress * panRange) to (progress * panRange)
    }

    AsyncImage(
        model = slide.imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = panX
                translationY = panY
            }
    )
}

@Composable
private fun SlideVideoLayer(slide: GoalSlide, isPlaying: Boolean) {
    val context = LocalContext.current
    val exoPlayer = remember(slide.id) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(slide.imageUrl))
            volume = 0f
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            prepare()
        }
    }
    DisposableEffect(slide.id) {
        onDispose { exoPlayer.release() }
    }
    LaunchedEffect(isPlaying, slide.id) {
        if (isPlaying) exoPlayer.play() else exoPlayer.pause()
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        }
    )
}
