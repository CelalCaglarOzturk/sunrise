package dev.mudrock.gunes.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Defined based on old app.css
private val OrangePrimary = Color(0xFFFF9500)
private val OrangeDark = Color(0xFFE68600)
private val BlackBg = Color(0xFF000000)
private val DarkGrayBg = Color(0xFF1A1A1A)
private val ElevatedBg = Color(0xFF2A2A2A)
private val WhiteText = Color(0xFFFFFFFF)
private val GrayText = Color(0xFF888888)

private val DarkColorScheme = darkColorScheme(
    primary = OrangePrimary,
    onPrimary = Color.Black,
    primaryContainer = OrangeDark,
    onPrimaryContainer = WhiteText,
    secondary = OrangePrimary, // Using primary as secondary for accent
    onSecondary = Color.Black,
    background = BlackBg,
    onBackground = WhiteText,
    surface = DarkGrayBg,
    onSurface = WhiteText,
    surfaceVariant = ElevatedBg,
    onSurfaceVariant = WhiteText,
    tertiary = ElevatedBg
)

// We enforce dark theme for this app as per the original design
private val LightColorScheme = DarkColorScheme

@Composable
fun SunriseTheme(
    content: @Composable () -> Unit
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

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
