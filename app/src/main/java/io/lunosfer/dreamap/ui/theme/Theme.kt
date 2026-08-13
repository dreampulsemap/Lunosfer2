package io.lunosfer.dreamap.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AstralGold,
    onPrimary = Void950,
    primaryContainer = AstralGold.copy(alpha = 0.2f),
    onPrimaryContainer = AstralGold,
    secondary = AetherCyan,
    onSecondary = Void950,
    secondaryContainer = AetherCyan.copy(alpha = 0.2f),
    onSecondaryContainer = AetherCyan,
    tertiary = AetherIndigo,
    onTertiary = Void950,
    tertiaryContainer = AetherIndigo.copy(alpha = 0.2f),
    onTertiaryContainer = AetherIndigo,
    background = Void950,
    onBackground = Color.White,
    surface = Void900,
    onSurface = Color.White,
    surfaceVariant = Void800,
    onSurfaceVariant = Color.White,
    error = ShadowWorkRose,
    onError = Color.White,
    errorContainer = ShadowWorkRose.copy(alpha = 0.2f),
    onErrorContainer = ShadowWorkRose,
    outline = Void800
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

