package io.lunosfer.dreamap.data.model

import android.net.Uri
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.RgbAdjustment
import java.util.UUID

enum class ClipType { VIDEO, IMAGE }

const val DEFAULT_IMAGE_DURATION_MS = 5_000L
const val MAX_IMAGE_DURATION_MS = 30_000L

data class MediaClip(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val type: ClipType,
    val sourceDurationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long,
    val filterId: String = "none",
    val speed: Float = 1f,
    val volume: Float = 1f,
) {
    val trimmedDurationMs: Long get() = (trimEndMs - trimStartMs).coerceAtLeast(0L)
}

enum class CaptionFont(val id: String) { SANS("sans"), SERIF("serif"), MONO("mono") }

val CAPTION_COLOR_PRESETS = listOf(
    0xFFFFFFFF, 0xFF0A0A0F, 0xFFF5C451, 0xFFE879F9, 0xFF22D3EE, 0xFFFB7185,
)

data class TextOverlay(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.5f,
    val colorArgb: Long = CAPTION_COLOR_PRESETS[0],
    val font: CaptionFont = CaptionFont.SANS,
    val startMs: Long,
    val endMs: Long,
)

data class MusicTrack(
    val uri: Uri,
    val volume: Float = 0.8f,
    val offsetMs: Long = 0L,
)

enum class AspectRatioOption(val label: String, val widthPx: Int, val heightPx: Int) {
    PORTRAIT("9:16", 1080, 1920),
    SQUARE("1:1", 1080, 1080),
    LANDSCAPE("16:9", 1920, 1080),
}

enum class EditorTool { FILTERS, TEXT, MUSIC, ADJUST, NONE }

/**
 * VisionVideoEditor.jsx'teki FILTERS dizisiyle birebir aynı 12 filtre.
 * TEK effects listesi hem player.setVideoEffects() (canlı önizleme) hem
 * Transformer (export, bkz. media/VideoExporter.kt) tarafından kullanılıyor.
 *
 * NOT: HslAdjustment.Builder()'ın parametre adları Media3 sürümüne göre
 * ufak farklılık gösterebilir — ilk derlemede hata verirse yalnızca
 * toMedia3Effects() içindeki bu iki satırı düzeltmeniz yeterli.
 */
data class LunosferFilter(
    val id: String,
    val nameTr: String,
    val nameEn: String,
    val brightness: Float = 1f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val hueRotateDeg: Float = 0f,
    val warmthTint: Float = 0f,
) {
    @OptIn(UnstableApi::class)
    fun toMedia3Effects(): List<Effect> {
        if (id == "none") return emptyList()
        val effects = mutableListOf<Effect>()
        if (contrast != 1f) effects += Contrast(contrast - 1f)
        if (saturation != 1f || hueRotateDeg != 0f) {
            effects += HslAdjustment.Builder()
                .adjustSaturation(saturation)
                .adjustHue(hueRotateDeg)
                .build()
        }
        if (brightness != 1f || warmthTint != 0f) {
            val warm = warmthTint.coerceIn(-1f, 1f)
            effects += RgbAdjustment.Builder()
                .setRedScale(brightness + warm * 0.12f)
                .setGreenScale(brightness + warm * 0.03f)
                .setBlueScale(brightness - warm * 0.12f)
                .build()
        }
        return effects
    }
}

val LUNOSFER_FILTERS = listOf(
    LunosferFilter("none", "Orijinal", "Original"),
    LunosferFilter("vivid", "Canlı", "Vivid", brightness = 1.03f, contrast = 1.15f, saturation = 1.5f),
    LunosferFilter("warm", "Sıcak", "Warm", brightness = 1.08f, saturation = 1.25f, hueRotateDeg = -8f, warmthTint = 0.6f),
    LunosferFilter("cool", "Soğuk", "Cool", brightness = 1.02f, contrast = 1.05f, saturation = 1.15f, hueRotateDeg = 15f, warmthTint = -0.5f),
    LunosferFilter("bw", "Siyah Beyaz", "B&W", contrast = 1.1f, saturation = 0f),
    LunosferFilter("vintage", "Vintage", "Vintage", brightness = 1.05f, contrast = 0.9f, saturation = 0.85f, warmthTint = 0.35f),
    LunosferFilter("contrast", "Yüksek Kontrast", "High Contrast", contrast = 1.4f, saturation = 1.1f),
    LunosferFilter("soft", "Yumuşak", "Soft", brightness = 1.1f, contrast = 0.85f, saturation = 0.9f),
    LunosferFilter("retro", "Retro", "Retro", contrast = 0.95f, saturation = 1.3f, hueRotateDeg = -15f, warmthTint = 0.4f),
    LunosferFilter("matte", "Mat", "Matte", brightness = 1.08f, contrast = 0.8f, saturation = 0.75f),
    LunosferFilter("neon", "Neon", "Neon", contrast = 1.25f, saturation = 1.8f, hueRotateDeg = -5f),
    LunosferFilter("night", "Gece", "Night", brightness = 0.85f, contrast = 1.2f, saturation = 0.9f, hueRotateDeg = 10f),
)
