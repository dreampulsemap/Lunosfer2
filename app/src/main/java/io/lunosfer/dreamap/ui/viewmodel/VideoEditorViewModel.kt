package io.lunosfer.dreamap.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import io.github.jan.supabase.auth.auth
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.*
import io.lunosfer.dreamap.data.network.NetworkModule
import io.lunosfer.dreamap.data.repository.VideoEditorRepository
import io.lunosfer.dreamap.media.VideoExporter
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class VideoEditorState(
    val goalId: String = "",
    val clips: List<MediaClip> = emptyList(),
    val selectedClipId: String? = null,
    val textOverlays: List<TextOverlay> = emptyList(),
    val selectedTextId: String? = null,
    val musicTrack: MusicTrack? = null,
    val aspectRatio: AspectRatioOption = AspectRatioOption.PORTRAIT,
    val activeTool: EditorTool = EditorTool.NONE,
    val isPlaying: Boolean = false,
    val playheadMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val isImportingPixabay: Boolean = false,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val didPublish: Boolean = false,
    val toast: String? = null,
) {
    val selectedClip: MediaClip? get() = clips.find { it.id == selectedClipId }
    val hasContent: Boolean get() = clips.isNotEmpty()
}

/**
 * VisionVideoEditor.jsx'in Reels-tarzı native karşılığı. Tek ExoPlayer
 * motoru + StateFlow. Goal her zaman önceden var (GoalDetailScreen'den
 * açılıyor) — burada sadece klip/filtre/metin/müzik düzenleyip export +
 * Storage upload + save-vision-video yapılıyor.
 */
@UnstableApi
class VideoEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext: Context get() = getApplication()

    private val repository = VideoEditorRepository()

    val player: ExoPlayer by lazy {
        ExoPlayer.Builder(application).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _state.value = _state.value.copy(isPlaying = isPlaying)
                    if (isPlaying) startPositionTicker() else positionTicker?.cancel()
                }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val clip = _state.value.clips.find { it.id == mediaItem?.mediaId }
                    applyLiveFilter(clip)
                }
            })
        }
    }

    private val _state = MutableStateFlow(VideoEditorState())
    val state: StateFlow<VideoEditorState> = _state

    private var positionTicker: Job? = null

    fun init(goalId: String) {
        if (_state.value.goalId == goalId) return
        _state.value = _state.value.copy(goalId = goalId)
    }

    // ---------- Klip ekleme (galeri / kamera) ----------

    fun addVideoClips(uris: List<Uri>) = viewModelScope.launch {
        val newClips = uris.map { uri ->
            val durationMs = probeDurationMs(uri)
            MediaClip(uri = uri, type = ClipType.VIDEO, sourceDurationMs = durationMs, trimEndMs = durationMs)
        }
        _state.value = _state.value.copy(clips = _state.value.clips + newClips)
        rebuildTimeline()
    }

    fun addImageClips(uris: List<Uri>) {
        val newClips = uris.map { uri ->
            MediaClip(uri = uri, type = ClipType.IMAGE, sourceDurationMs = DEFAULT_IMAGE_DURATION_MS, trimEndMs = DEFAULT_IMAGE_DURATION_MS)
        }
        _state.value = _state.value.copy(clips = _state.value.clips + newClips)
        rebuildTimeline()
    }

    // ---------- Pixabay (birincil giriş noktası) ----------

    /** PixabayMediaPickerDialog.onVideoSelected'den çağrılır — önce import-video ile
     * kota/cache kontrolünden geçirip DÖNEN (kendi Storage'ımızdaki) URL'i klip yapar. */
    fun importPixabayVideoAsClip(pixabayId: Long, videoUrl: String, tags: String, user: String) = viewModelScope.launch {
        _state.value = _state.value.copy(isImportingPixabay = true)
        try {
            val response = NetworkModule.api.importPixabayVideo(
                io.lunosfer.dreamap.data.model.PixabayVideoImportRequest(pixabayId = pixabayId, videoUrl = videoUrl, tags = tags, user = user)
            )
            val finalUrl = response.url
            if (response.ok != false && finalUrl != null) {
                addVideoClips(listOf(Uri.parse(finalUrl)))
            } else {
                _state.value = _state.value.copy(toast = response.error ?: appContext.getString(R.string.toast_pixabay_generic_error))
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(toast = appContext.getString(R.string.toast_pixabay_import_failed_format, e.message ?: ""))
        } finally {
            _state.value = _state.value.copy(isImportingPixabay = false)
        }
    }

    /** onImageSelected'den çağrılır — görsel için ayrı bir kota/cache adımı yok, doğrudan klip. */
    fun addPixabayImageAsClip(imageUrl: String) {
        addImageClips(listOf(Uri.parse(imageUrl)))
    }

    // ---------- Klip yönetimi ----------

    fun removeClip(clipId: String) {
        _state.value = _state.value.copy(
            clips = _state.value.clips.filterNot { it.id == clipId },
            selectedClipId = _state.value.selectedClipId.takeUnless { it == clipId },
        )
        rebuildTimeline()
    }

    fun selectClip(clipId: String) { _state.value = _state.value.copy(selectedClipId = clipId) }

    fun splitSelectedClip() {
        val s = _state.value
        val clip = s.selectedClip ?: return
        val range = timelineRanges(s.clips)[clip.id] ?: return
        val cutAtClipRelativeMs = (s.playheadMs - range.first).coerceIn(1L, clip.trimmedDurationMs - 1)
        if (cutAtClipRelativeMs <= 0) return

        val splitPointAbsolute = clip.trimStartMs + cutAtClipRelativeMs
        val first = clip.copy(id = UUID.randomUUID().toString(), trimEndMs = splitPointAbsolute)
        val second = clip.copy(id = UUID.randomUUID().toString(), trimStartMs = splitPointAbsolute)

        _state.value = s.copy(
            clips = s.clips.flatMap { if (it.id == clip.id) listOf(first, second) else listOf(it) },
            selectedClipId = first.id,
        )
        rebuildTimeline()
    }

    fun setClipSpeed(clipId: String, speed: Float) {
        _state.value = _state.value.copy(clips = _state.value.clips.map { if (it.id == clipId) it.copy(speed = speed) else it })
    }

    fun setClipVolume(clipId: String, volume: Float) {
        _state.value = _state.value.copy(clips = _state.value.clips.map { if (it.id == clipId) it.copy(volume = volume) else it })
    }

    // ---------- Filtreler ----------

    fun setFilter(clipId: String, filterId: String) {
        _state.value = _state.value.copy(clips = _state.value.clips.map { if (it.id == clipId) it.copy(filterId = filterId) else it })
        if (_state.value.selectedClipId == clipId) applyLiveFilter(_state.value.clips.find { it.id == clipId })
    }

    private fun applyLiveFilter(clip: MediaClip?) {
        val filter = LUNOSFER_FILTERS.find { it.id == (clip?.filterId ?: "none") } ?: return
        player.setVideoEffects(filter.toMedia3Effects())
    }

    // ---------- Metin overlay ----------

    fun addTextOverlay(content: String) {
        val s = _state.value
        val overlay = TextOverlay(content = content, startMs = s.playheadMs, endMs = s.playheadMs + 3_000L)
        _state.value = s.copy(textOverlays = s.textOverlays + overlay, selectedTextId = overlay.id)
    }

    fun updateTextOverlay(updated: TextOverlay) {
        _state.value = _state.value.copy(textOverlays = _state.value.textOverlays.map { if (it.id == updated.id) updated else it })
    }

    fun removeTextOverlay(id: String) {
        _state.value = _state.value.copy(textOverlays = _state.value.textOverlays.filterNot { it.id == id })
    }

    // ---------- Müzik ----------

    fun setMusic(uri: Uri) { _state.value = _state.value.copy(musicTrack = MusicTrack(uri = uri)) }
    fun setMusicVolume(volume: Float) {
        _state.value.musicTrack?.let { _state.value = _state.value.copy(musicTrack = it.copy(volume = volume)) }
    }
    fun removeMusic() { _state.value = _state.value.copy(musicTrack = null) }

    // ---------- Oynatma / transport ----------

    fun togglePlayPause() { if (player.isPlaying) player.pause() else player.play() }
    fun seekTo(ms: Long) { player.seekTo(ms); _state.value = _state.value.copy(playheadMs = ms) }
    fun setAspectRatio(option: AspectRatioOption) { _state.value = _state.value.copy(aspectRatio = option) }
    fun setActiveTool(tool: EditorTool) {
        _state.value = _state.value.copy(activeTool = if (_state.value.activeTool == tool) EditorTool.NONE else tool)
    }

    private fun startPositionTicker() {
        positionTicker?.cancel()
        positionTicker = viewModelScope.launch {
            while (true) {
                _state.value = _state.value.copy(playheadMs = player.currentPosition)
                delay(200)
            }
        }
    }

    private fun rebuildTimeline() {
        val s = _state.value
        val items = s.clips.map { clip ->
            MediaItem.Builder()
                .setMediaId(clip.id)
                .setUri(clip.uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.trimStartMs)
                        .setEndPositionMs(clip.trimEndMs)
                        .build()
                )
                .build()
        }
        player.setMediaItems(items)
        player.prepare()
        _state.value = _state.value.copy(totalDurationMs = s.clips.sumOf { it.trimmedDurationMs })
        if (s.selectedClipId == null && s.clips.isNotEmpty()) {
            _state.value = _state.value.copy(selectedClipId = s.clips.first().id)
        }
    }

    private fun timelineRanges(clips: List<MediaClip>): Map<String, Pair<Long, Long>> {
        var cursor = 0L
        val map = mutableMapOf<String, Pair<Long, Long>>()
        for (clip in clips) {
            val end = cursor + clip.trimmedDurationMs
            map[clip.id] = cursor to end
            cursor = end
        }
        return map
    }

    private fun probeDurationMs(uri: Uri): Long {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(getApplication(), uri)
            val ms = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            retriever.release()
            ms ?: 5_000L
        } catch (e: Exception) {
            5_000L
        }
    }

    // ---------- Paylaş (export + Storage + save-vision-video) ----------

    fun publish() = viewModelScope.launch {
        val s = _state.value
        val userId = supabaseClient.auth.currentUserOrNull()?.id
        if (!s.hasContent || userId == null) {
            _state.value = s.copy(toast = if (userId == null) appContext.getString(R.string.toast_sign_in_required) else null)
            return@launch
        }
        _state.value = s.copy(isExporting = true, exportProgress = 0f)
        val outputFile = File(getApplication<Application>().getExternalFilesDir(null), "lunosfer_reel_${System.currentTimeMillis()}.mp4")

        var succeeded = false
        VideoExporter.export(
            context = getApplication(),
            clips = s.clips,
            textOverlays = s.textOverlays,
            musicTrack = s.musicTrack,
            aspectRatio = s.aspectRatio,
            outputFile = outputFile,
            onProgress = { progress -> _state.value = _state.value.copy(exportProgress = progress) },
            onComplete = { success -> succeeded = success },
        )

        if (!succeeded) {
            _state.value = _state.value.copy(isExporting = false, toast = appContext.getString(R.string.toast_export_failed))
            return@launch
        }

        repository.attachToGoal(outputFile, userId, s.goalId)
            .onSuccess { _state.value = _state.value.copy(isExporting = false, didPublish = true, toast = appContext.getString(R.string.toast_publish_success)) }
            .onFailure { err -> _state.value = _state.value.copy(isExporting = false, toast = appContext.getString(R.string.toast_upload_failed_format, err.message ?: "")) }
    }

    fun consumeToast() { _state.value = _state.value.copy(toast = null) }

    override fun onCleared() {
        positionTicker?.cancel()
        player.release()
        super.onCleared()
    }
}
