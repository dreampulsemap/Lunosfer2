package io.lunosfer.dreamap.ui.screens.videoeditor

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.lunosfer.dreamap.data.model.TextOverlay

@Composable
fun TextOverlayLayer(
    overlays: List<TextOverlay>,
    playheadMs: Long,
    onOverlayMove: (TextOverlay, Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        overlays.filter { playheadMs in it.startMs..it.endMs }.forEach { overlay ->
            val xDp = maxWidth * overlay.xFraction
            val yDp = maxHeight * overlay.yFraction

            Text(
                text = overlay.content,
                color = Color(overlay.colorArgb.toInt()),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .offset(x = xDp - 60.dp, y = yDp - 20.dp)
                    .pointerInput(overlay.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newX = (overlay.xFraction + dragAmount.x / widthPx).coerceIn(0.05f, 0.95f)
                            val newY = (overlay.yFraction + dragAmount.y / heightPx).coerceIn(0.05f, 0.95f)
                            onOverlayMove(overlay, newX, newY)
                        }
                    },
            )
        }
    }
}
