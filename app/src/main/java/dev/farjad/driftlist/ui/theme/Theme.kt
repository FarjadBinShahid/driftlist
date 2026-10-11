package dev.farjad.driftlist.ui.theme

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

private val LightScheme = lightColorScheme(
    primary = LightTokens.accent,
    onPrimary = LightTokens.onAccent,
    primaryContainer = LightTokens.accentContainer,
    onPrimaryContainer = LightTokens.onAccentContainer,
    background = LightTokens.bg,
    onBackground = LightTokens.text,
    surface = LightTokens.surfaceRaised,
    onSurface = LightTokens.text,
    surfaceVariant = LightTokens.surface,
    onSurfaceVariant = LightTokens.textMuted,
    surfaceContainerLow = LightTokens.surfaceRaised,
    surfaceContainer = LightTokens.surface,
    outline = LightTokens.borderInput,
    outlineVariant = LightTokens.border,
    error = LightTokens.danger,
    scrim = LightTokens.scrim,
)

private val DarkScheme = darkColorScheme(
    primary = DarkTokens.accent,
    onPrimary = DarkTokens.onAccent,
    primaryContainer = DarkTokens.accentContainer,
    onPrimaryContainer = DarkTokens.onAccentContainer,
    background = DarkTokens.bg,
    onBackground = DarkTokens.text,
    surface = DarkTokens.surfaceRaised,
    onSurface = DarkTokens.text,
    surfaceVariant = DarkTokens.surface,
    onSurfaceVariant = DarkTokens.textMuted,
    surfaceContainerLow = DarkTokens.surfaceRaised,
    surfaceContainer = DarkTokens.surface,
    outline = DarkTokens.borderInput,
    outlineVariant = DarkTokens.border,
    error = DarkTokens.danger,
    scrim = DarkTokens.scrim,
)

@Composable
fun DriftlistTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme,
        content = content,
    )
}

fun TypeToken.style(color: Color = Color.Unspecified): TextStyle = TextStyle(
    color = color,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = tracking.sp,
)

/** Colours for text drawn straight on the wallpaper (home + app list). */
@Immutable
data class HomeColors(
    val text: Color,
    val textMuted: Color,
    val preview: Color,
    val alphabet: Color,
    val scrim: Color,
    val darkText: Boolean,
)

val HomeColorsForDarkWallpaper = HomeColors(
    text = HomeTokens.textOnDark,
    textMuted = HomeTokens.textMutedOnDark,
    preview = HomeTokens.previewOnDark,
    alphabet = HomeTokens.alphabetIdleOnDark,
    scrim = Color.Black,
    darkText = false,
)

val HomeColorsForLightWallpaper = HomeColors(
    text = HomeTokens.textOnLight,
    textMuted = HomeTokens.textMutedOnLight,
    preview = HomeTokens.previewOnLight,
    alphabet = HomeTokens.alphabetIdleOnLight,
    scrim = Color.White,
    darkText = true,
)

val LocalHomeColors = staticCompositionLocalOf { HomeColorsForDarkWallpaper }

/** True when the system says the current wallpaper wants dark text (API 31+). Otherwise assume a dark wallpaper. */
fun wallpaperWantsDarkText(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    val colors = runCatching {
        WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
    }.getOrNull() ?: return false
    return colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0
}
