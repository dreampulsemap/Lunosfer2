package io.lunosfer.dreamap.ui.screens.videoeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.*
import io.lunosfer.dreamap.ui.theme.BrandPrimary500
import io.lunosfer.dreamap.ui.theme.Void900

@Composable
fun ToolPanelSheet(
    tool: EditorTool,
    selectedClip: MediaClip?,
    musicTrack: MusicTrack?,
    onFilterSelect: (String) -> Unit,
    onAddText: (String) -> Unit,
    onPickMusic: () -> Unit,
    onRemoveMusic: () -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).background(Void900).padding(vertical = 14.dp),
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).width(36.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)))
        Spacer(Modifier.height(14.dp))
        when (tool) {
            EditorTool.FILTERS -> FilterRow(selectedFilterId = selectedClip?.filterId ?: "none", onSelect = onFilterSelect)
            EditorTool.TEXT -> TextComposer(onAddText = onAddText)
            EditorTool.MUSIC -> MusicPane(musicTrack, onPickMusic, onRemoveMusic, onMusicVolumeChange)
            EditorTool.ADJUST -> AdjustPane(selectedClip, onSpeedChange, onVolumeChange)
            EditorTool.NONE -> {}
        }
    }
}

/** Filtre id'sini lokalize ismine çevirir — LunosferFilter.nameTr/nameEn artık kullanılmıyor. */
private fun filterNameRes(filterId: String): Int = when (filterId) {
    "vivid" -> R.string.filter_vivid
    "warm" -> R.string.filter_warm
    "cool" -> R.string.filter_cool
    "bw" -> R.string.filter_bw
    "vintage" -> R.string.filter_vintage
    "contrast" -> R.string.filter_contrast
    "soft" -> R.string.filter_soft
    "retro" -> R.string.filter_retro
    "matte" -> R.string.filter_matte
    "neon" -> R.string.filter_neon
    "night" -> R.string.filter_night
    else -> R.string.filter_none
}

@Composable
private fun FilterRow(selectedFilterId: String, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LUNOSFER_FILTERS.forEach { filter ->
            val selected = filter.id == selectedFilterId
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF23283A))
                        .border(2.dp, if (selected) BrandPrimary500 else Color.Transparent, RoundedCornerShape(14.dp))
                        .clickable { onSelect(filter.id) },
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(filterNameRes(filter.id)),
                    color = if (selected) BrandPrimary500 else Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun TextComposer(onAddText: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value, onValueChange = { value = it }, placeholder = { Text(stringResource(R.string.editor_text_placeholder)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        FilledIconButton(
            onClick = { if (value.isNotBlank()) { onAddText(value); value = "" } },
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = BrandPrimary500),
        ) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.editor_add_description)) }
    }
}

@Composable
private fun MusicPane(musicTrack: MusicTrack?, onPick: () -> Unit, onRemove: () -> Unit, onVolumeChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (musicTrack == null) {
            OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.LibraryMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.editor_pick_music))
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.LibraryMusic, contentDescription = null, tint = BrandPrimary500)
                Text(stringResource(R.string.editor_music_added), color = Color.White, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove) { Icon(Icons.Filled.MusicOff, contentDescription = stringResource(R.string.editor_remove_music_description), tint = Color.White) }
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.editor_volume_label), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
            Slider(value = musicTrack.volume, onValueChange = onVolumeChange, colors = SliderDefaults.colors(thumbColor = BrandPrimary500, activeTrackColor = BrandPrimary500))
        }
    }
}

@Composable
private fun AdjustPane(selectedClip: MediaClip?, onSpeedChange: (Float) -> Unit, onVolumeChange: (Float) -> Unit) {
    if (selectedClip == null) {
        Text(stringResource(R.string.editor_select_clip_first), color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(horizontal = 16.dp))
        return
    }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.editor_speed_format, selectedClip.speed), color = Color.White, style = MaterialTheme.typography.labelMedium)
        Slider(value = selectedClip.speed, onValueChange = onSpeedChange, valueRange = 0.5f..2f, colors = SliderDefaults.colors(thumbColor = BrandPrimary500, activeTrackColor = BrandPrimary500))
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.editor_clip_volume_format, (selectedClip.volume * 100).toInt()), color = Color.White, style = MaterialTheme.typography.labelMedium)
        Slider(value = selectedClip.volume, onValueChange = onVolumeChange, colors = SliderDefaults.colors(thumbColor = BrandPrimary500, activeTrackColor = BrandPrimary500))
    }
}
