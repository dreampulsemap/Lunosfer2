package io.lunosfer.dreamap.ui.screens.videoeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.EditorTool
import io.lunosfer.dreamap.ui.theme.BrandPrimary500
import io.lunosfer.dreamap.ui.theme.EditorGlass

private data class RailItem(val tool: EditorTool, val icon: androidx.compose.ui.graphics.vector.ImageVector, val labelRes: Int)

private val railItems = listOf(
    RailItem(EditorTool.FILTERS, Icons.Filled.AutoAwesome, R.string.editor_tool_filters),
    RailItem(EditorTool.TEXT, Icons.Filled.TextFields, R.string.editor_tool_text),
    RailItem(EditorTool.MUSIC, Icons.Filled.MusicNote, R.string.editor_tool_music),
    RailItem(EditorTool.ADJUST, Icons.Filled.Tune, R.string.editor_tool_adjust),
)

@Composable
fun ToolRail(activeTool: EditorTool, onToolClick: (EditorTool) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            .background(EditorGlass)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        railItems.forEach { item ->
            val selected = activeTool == item.tool
            val label = stringResource(item.labelRes)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onToolClick(item.tool) }.padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(if (selected) BrandPrimary500 else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(item.icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(2.dp))
                Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
