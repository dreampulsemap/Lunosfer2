package io.lunosfer.dreamap.ui.screens.videoeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.ui.theme.BrandPrimary500
import io.lunosfer.dreamap.ui.theme.EditorGlass
import io.lunosfer.dreamap.ui.theme.PillShapeEditor

@Composable
fun FloatingTopBar(
    isExporting: Boolean,
    exportProgress: Float,
    canSave: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-116).dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(EditorGlass),
            ) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.editor_close), tint = Color.White)
            }

            if (isExporting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(PillShapeEditor).background(EditorGlass).padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    CircularProgressIndicator(
                        progress = { exportProgress },
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = BrandPrimary500,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("${(exportProgress * 100).toInt()}%", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Button(
                    onClick = onSave,
                    enabled = canSave,
                    shape = PillShapeEditor,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary500),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    Text(stringResource(R.string.editor_publish))
                }
            }
        }
    }
}
