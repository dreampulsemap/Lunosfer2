package io.lunosfer.dreamap.ui.screens.videoeditor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.AspectRatioOption
import io.lunosfer.dreamap.data.model.EditorTool
import io.lunosfer.dreamap.ui.components.PixabayMediaPickerDialog
import io.lunosfer.dreamap.ui.theme.BrandPrimary500
import io.lunosfer.dreamap.ui.theme.PillShapeEditor
import io.lunosfer.dreamap.ui.theme.Void950
import io.lunosfer.dreamap.ui.viewmodel.VideoEditorViewModel
import java.io.File

/**
 * VisionVideoEditor.jsx'in Reels-tarzı native karşılığı. Video HER ZAMAN
 * tam ekran, chrome onun ÜSTÜNE yüzüyor. Medya ekleme önceliği: Pixabay
 * (birincil, mevcut PixabayMediaPickerDialog'u yeniden kullanıyor — kota/
 * cache mantığı için önce import-video çağrılıyor) > galeri > kamera
 * (ikincil, sistem Kamera uygulamasına çıkan basit bir intent).
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoEditorScreen(
    goalId: String,
    onClose: () -> Unit,
    viewModel: VideoEditorViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPixabayDialog by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    LaunchedEffect(goalId) { viewModel.init(goalId) }

    LaunchedEffect(state.toast) {
        state.toast?.let { snackbarHostState.showSnackbar(it); viewModel.consumeToast() }
    }

    LaunchedEffect(state.didPublish) {
        if (state.didPublish) onClose()
    }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(limitOrNull = 10),
    ) { uris ->
        if (uris.isNotEmpty()) {
            val videoUris = uris.filter { context.contentResolver.getType(it)?.startsWith("video") == true }
            val imageUris = uris.filter { context.contentResolver.getType(it)?.startsWith("image") == true }
            if (videoUris.isNotEmpty()) viewModel.addVideoClips(videoUris)
            if (imageUris.isNotEmpty()) viewModel.addImageClips(imageUris)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) viewModel.addVideoClips(listOf(uri))
        pendingCameraUri = null
    }
    fun launchCamera() {
        val dir = File(context.cacheDir, "reel_recordings").apply { mkdirs() }
        val file = File(dir, "reel_${System.currentTimeMillis()}.mp4")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }

    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.setMusic(it) }
    }

    if (showPixabayDialog) {
        PixabayMediaPickerDialog(
            onDismissRequest = { showPixabayDialog = false },
            onImageSelected = { _, imageUrl, _, _ ->
                showPixabayDialog = false
                viewModel.addPixabayImageAsClip(imageUrl)
            },
            onVideoSelected = { pixabayId, videoUrl, tags, user ->
                showPixabayDialog = false
                viewModel.importPixabayVideoAsClip(pixabayId, videoUrl, tags, user)
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        if (state.hasContent) {
            PreviewPlayer(
                player = viewModel.player,
                aspectRatio = state.aspectRatio,
                onTap = viewModel::togglePlayPause,
                modifier = Modifier.fillMaxSize(),
            )
            TextOverlayLayer(
                overlays = state.textOverlays,
                playheadMs = state.playheadMs,
                onOverlayMove = { overlay, x, y -> viewModel.updateTextOverlay(overlay.copy(xFraction = x, yFraction = y)) },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            EmptyState(
                isImportingPixabay = state.isImportingPixabay,
                onPixabayClick = { showPixabayDialog = true },
                onGalleryClick = { mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                onCameraClick = ::launchCamera,
                modifier = Modifier.fillMaxSize(),
            )
        }

        FloatingTopBar(
            isExporting = state.isExporting,
            exportProgress = state.exportProgress,
            canSave = state.hasContent && !state.isExporting,
            onClose = onClose,
            onSave = viewModel::publish,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        if (state.hasContent) {
            ToolRail(
                activeTool = state.activeTool,
                onToolClick = viewModel::setActiveTool,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
            )
        }

        Box(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(180.dp)
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
        )

        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 8.dp)) {
            AnimatedVisibility(
                visible = state.activeTool != EditorTool.NONE,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                ToolPanelSheet(
                    tool = state.activeTool,
                    selectedClip = state.selectedClip,
                    musicTrack = state.musicTrack,
                    onFilterSelect = { filterId -> state.selectedClipId?.let { viewModel.setFilter(it, filterId) } },
                    onAddText = viewModel::addTextOverlay,
                    onPickMusic = { musicPicker.launch("audio/*") },
                    onRemoveMusic = viewModel::removeMusic,
                    onMusicVolumeChange = viewModel::setMusicVolume,
                    onSpeedChange = { speed -> state.selectedClipId?.let { viewModel.setClipSpeed(it, speed) } },
                    onVolumeChange = { vol -> state.selectedClipId?.let { viewModel.setClipVolume(it, vol) } },
                )
            }

            if (state.hasContent) {
                TransportBar(
                    isPlaying = state.isPlaying,
                    playheadMs = state.playheadMs,
                    totalDurationMs = state.totalDurationMs,
                    aspectRatio = state.aspectRatio,
                    onPlayPauseClick = viewModel::togglePlayPause,
                    onAspectRatioClick = {
                        val next = when (state.aspectRatio) {
                            AspectRatioOption.PORTRAIT -> AspectRatioOption.SQUARE
                            AspectRatioOption.SQUARE -> AspectRatioOption.LANDSCAPE
                            AspectRatioOption.LANDSCAPE -> AspectRatioOption.PORTRAIT
                        }
                        viewModel.setAspectRatio(next)
                    },
                    onSplitClick = viewModel::splitSelectedClip,
                )
                TimelinePanel(
                    clips = state.clips,
                    selectedClipId = state.selectedClipId,
                    onClipClick = viewModel::selectClip,
                    onAddClick = { showPixabayDialog = true }, // timeline'a + de birincil olarak Pixabay'i açar
                )
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp))
    }
}

/** Boş durum: Pixabay birincil/büyük CTA, galeri + kamera altında küçük ikincil seçenekler. */
@Composable
private fun EmptyState(
    isImportingPixabay: Boolean,
    onPixabayClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(Void950), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
            Text(stringResource(R.string.editor_new_reel_title), color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.editor_new_reel_subtitle), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onPixabayClick,
                enabled = !isImportingPixabay,
                shape = PillShapeEditor,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary500),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (isImportingPixabay) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.editor_pixabay_button))
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                SecondaryOption(icon = Icons.Filled.PhotoLibrary, label = stringResource(R.string.editor_gallery_button), onClick = onGalleryClick)
                SecondaryOption(icon = Icons.Filled.CameraAlt, label = stringResource(R.string.editor_camera_button), onClick = onCameraClick)
            }
        }
    }
}

@Composable
private fun SecondaryOption(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelMedium)
    }
}
