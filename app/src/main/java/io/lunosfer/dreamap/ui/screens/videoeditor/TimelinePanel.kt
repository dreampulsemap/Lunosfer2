package io.lunosfer.dreamap.ui.screens.videoeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.lunosfer.dreamap.data.model.ClipType
import io.lunosfer.dreamap.data.model.MediaClip
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.ui.theme.BrandPrimary500
import io.lunosfer.dreamap.ui.theme.EditorGlass
import io.lunosfer.dreamap.ui.theme.EditorGlassBorder

private const val MS_PER_DP = 40L

@Composable
fun TimelinePanel(
    clips: List<MediaClip>,
    selectedClipId: String?,
    onClipClick: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(64.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        clips.forEach { clip ->
            val widthDp = (clip.trimmedDurationMs / MS_PER_DP).coerceAtLeast(32L).dp
            val selected = clip.id == selectedClipId
            Box(
                modifier = Modifier
                    .width(widthDp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (clip.type == ClipType.VIDEO) Color(0xFF2A2440) else Color(0xFF243046))
                    .border(if (selected) 2.dp else 1.dp, if (selected) BrandPrimary500 else EditorGlassBorder, RoundedCornerShape(10.dp))
                    .clickable { onClipClick(clip.id) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (clip.type == ClipType.VIDEO) Icons.Filled.Videocam else Icons.Filled.Image,
                    contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(16.dp),
                )
            }
        }
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(EditorGlass)
                .border(1.dp, EditorGlassBorder, RoundedCornerShape(10.dp)).clickable { onAddClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.editor_add_description), tint = Color.White)
        }
    }
}
