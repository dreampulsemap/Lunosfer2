package io.lunosfer.dreamap.ui.screens.videoeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.AspectRatioOption
import io.lunosfer.dreamap.ui.theme.EditorGlass
import io.lunosfer.dreamap.ui.theme.PillShapeEditor
import java.util.Locale

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

@Composable
fun TransportBar(
    isPlaying: Boolean,
    playheadMs: Long,
    totalDurationMs: Long,
    aspectRatio: AspectRatioOption,
    onPlayPauseClick: () -> Unit,
    onAspectRatioClick: () -> Unit,
    onSplitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconButton(onClick = onPlayPauseClick, modifier = Modifier.size(38.dp).clip(androidx.compose.foundation.shape.CircleShape).background(EditorGlass)) {
            Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
        }
        Text("${formatMs(playheadMs)} / ${formatMs(totalDurationMs)}", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.clip(PillShapeEditor).background(EditorGlass).clickable { onSplitClick() }.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.ContentCut, contentDescription = stringResource(R.string.editor_split), tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.editor_split), color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
        Row(
            modifier = Modifier.clip(PillShapeEditor).background(EditorGlass).clickable { onAspectRatioClick() }.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.AspectRatio, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(aspectRatio.label, color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}
