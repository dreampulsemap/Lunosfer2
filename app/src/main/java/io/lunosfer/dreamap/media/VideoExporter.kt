package io.lunosfer.dreamap.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import io.lunosfer.dreamap.data.model.AspectRatioOption
import io.lunosfer.dreamap.data.model.ClipType
import io.lunosfer.dreamap.data.model.LUNOSFER_FILTERS
import io.lunosfer.dreamap.data.model.MediaClip
import io.lunosfer.dreamap.data.model.MusicTrack
import io.lunosfer.dreamap.data.model.TextOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * VisionVideoEditor.jsx'in export motorunun (canvas + MediaRecorder)
 * native karşılığı — gerçek Media3 Transformer pipeline'ı: klipleri,
 * filtreleri (canlı önizlemeyle AYNI zincir) ve müziği tek bir
 * Composition'da birleştirip donanım hızlandırmalı MP4 export ediyor.
 *
 * BİLİNEN SINIRLAMA (v1): metin overlay'leri export edilen KLİBİN TAMAMI
 * boyunca gösteriliyor, klip içi tam start/end ms hassasiyetiyle değil —
 * düzgün çözümü klibi overlay sınırlarında ekstra parçalara bölmek
 * (splitSelectedClip ile aynı mekanizma, otomatik tetiklenmiş hali).
 *
 * NOT: EditedMediaItemSequence/Composition.Builder ve BitmapOverlay/
 * OverlayEffect'in tam builder imzası Media3 sürümüne göre ufak değişebilir.
 */
@UnstableApi
object VideoExporter {

    suspend fun export(
        context: Context,
        clips: List<MediaClip>,
        textOverlays: List<TextOverlay>,
        musicTrack: MusicTrack?,
        aspectRatio: AspectRatioOption,
        outputFile: File,
        onProgress: (Float) -> Unit,
        onComplete: (success: Boolean) -> Unit,
    ) {
        if (clips.isEmpty()) {
            onComplete(false)
            return
        }

        val ranges = timelineRanges(clips)

        val editedItems = clips.map { clip ->
            val filter = LUNOSFER_FILTERS.find { it.id == clip.filterId }
            val videoEffects = filter?.toMedia3Effects().orEmpty().toMutableList()

            val range = ranges.getValue(clip.id)
            val overlaysForClip = textOverlays.filter { it.startMs < range.second && it.endMs > range.first }
            if (overlaysForClip.isNotEmpty()) {
                val bitmap = renderOverlayBitmap(overlaysForClip, aspectRatio)
                videoEffects += OverlayEffect(
                    androidx.media3.common.util.ImmutableList.of(
                        BitmapOverlay.createStaticBitmapOverlay(bitmap)
                    )
                )
            }

            val itemBuilder = MediaItem.Builder()
                .setUri(clip.uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.trimStartMs)
                        .setEndPositionMs(clip.trimEndMs)
                        .build()
                )

            val editedBuilder = EditedMediaItem.Builder(itemBuilder.build())
                .setEffects(Effects(emptyList(), videoEffects))
                .setRemoveAudio(clip.type == ClipType.IMAGE)

            if (clip.type == ClipType.IMAGE) {
                editedBuilder
                    .setDurationUs(clip.trimmedDurationMs * 1000)
                    .setFrameRate(30)
            }

            editedBuilder.build()
        }

        val mainSequence = EditedMediaItemSequence(editedItems)
        val sequences = mutableListOf(mainSequence)

        musicTrack?.let { music ->
            val totalDurationMs = clips.sumOf { it.trimmedDurationMs }
            val musicItem = EditedMediaItem.Builder(
                MediaItem.Builder()
                    .setUri(music.uri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(music.offsetMs)
                            .setEndPositionMs(music.offsetMs + totalDurationMs)
                            .build()
                    )
                    .build()
            ).build()
            sequences += EditedMediaItemSequence(listOf(musicItem))
        }

        val composition = Composition.Builder(sequences).build()

        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    onComplete(true)
                }
                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    onComplete(false)
                }
            })
            .build()

        transformer.start(composition, outputFile.absolutePath)

        val progressHolder = ProgressHolder()
        while (coroutineContext.isActive) {
            val state = transformer.getProgress(progressHolder)
            if (state == Transformer.PROGRESS_STATE_NOT_STARTED) break
            onProgress(progressHolder.progress / 100f)
            delay(250)
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

    private fun renderOverlayBitmap(overlays: List<TextOverlay>, aspectRatio: AspectRatioOption): Bitmap {
        val bitmap = Bitmap.createBitmap(aspectRatio.widthPx, aspectRatio.heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = aspectRatio.widthPx * 0.06f
        }
        overlays.forEach { overlay ->
            paint.color = AndroidColor.parseColor(String.format("#%06X", 0xFFFFFF and overlay.colorArgb.toInt()))
            canvas.drawText(
                overlay.content,
                aspectRatio.widthPx * overlay.xFraction,
                aspectRatio.heightPx * overlay.yFraction,
                paint,
            )
        }
        return bitmap
    }
}
