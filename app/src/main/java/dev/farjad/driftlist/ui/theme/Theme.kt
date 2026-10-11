package dev.farjad.driftlist.ui.theme

import android.graphics.Typeface
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import dev.farjad.driftlist.data.ClockStyle
import dev.farjad.driftlist.data.Fonts
import dev.farjad.driftlist.data.Look
import dev.farjad.driftlist.data.ThemeMode
import dev.farjad.driftlist.domain.Palette
import dev.farjad.driftlist.system.FontStore
import java.io.File
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
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

/**
 * Sheets and settings follow [Look.mode]; the accent comes from [Look.accent] or, when that is
 * null, the wallpaper's [wallpaperSeed]. Home text gets the look's font and size via [LocalHomeType].
 */
@Composable
fun DriftlistTheme(look: Look = Look(), wallpaperSeed: Int? = null, content: @Composable () -> Unit) {
    val dark = when (look.mode) {
        ThemeMode.Auto -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val seed = look.accent ?: wallpaperSeed
    val scheme = remember(dark, seed) { schemeFor(dark, seed) }
    val context = LocalContext.current
    val family = remember(look.font) { fontFamilyFor(context, look.font) }
    CompositionLocalProvider(
        LocalHomeType provides HomeType(family, look.fontScale),
        LocalClockStyle provides look.clockStyle,
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

private fun schemeFor(dark: Boolean, seed: Int?): ColorScheme {
    val base = if (dark) DarkScheme else LightScheme
    if (seed == null) return base
    val set = if (dark) {
        Palette.dark(seed, DarkTokens.bg.toArgb(), DarkTokens.surfaceRaised.toArgb())
    } else {
        Palette.light(seed, LightTokens.bg.toArgb(), LightTokens.surfaceRaised.toArgb())
    }
    return base.copy(
        primary = Color(set.accent),
        onPrimary = Color(set.onAccent),
        primaryContainer = Color(set.container),
        onPrimaryContainer = Color(set.onContainer),
        secondaryContainer = Color(set.container),
        onSecondaryContainer = Color(set.onContainer),
    )
}

/** Font and size for text drawn on the wallpaper (home, list, clock). Sheets keep the system font. */
@Immutable
data class HomeType(val family: FontFamily = FontFamily.Default, val scale: Float = 1f)

val LocalHomeType = staticCompositionLocalOf { HomeType() }
val LocalClockStyle = staticCompositionLocalOf { ClockStyle.Thin }

/** System families by name; imported files from app storage. Anything unreadable falls back to the default. */
fun fontFamilyFor(context: Context, id: String): FontFamily = runCatching {
    when {
        id == Fonts.DEFAULT -> FontFamily.Default
        id == "system:serif" -> FontFamily.Serif
        id == "system:monospace" -> FontFamily.Monospace
        id == "system:cursive" -> FontFamily.Cursive
        id.startsWith("system:") -> FontFamily(Typeface.create(id.removePrefix("system:"), Typeface.NORMAL))
        id.startsWith(Fonts.FILE_PREFIX) -> {
            val file = File(FontStore.dir(context), id.removePrefix(Fonts.FILE_PREFIX))
            if (file.isFile) FontFamily(Font(file)) else FontFamily.Default
        }
        else -> FontFamily.Default
    }
}.getOrDefault(FontFamily.Default)

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
    /** Soft shadow under light text so it stays legible over bright patches of a dark wallpaper. */
    val shadow: Shadow?,
)

/** A [TypeToken] styled for text drawn on the wallpaper, in the look's font and size. */
@Composable
@ReadOnlyComposable
fun TypeToken.onWallpaper(colors: HomeColors, color: Color = colors.text): TextStyle {
    val type = LocalHomeType.current
    return style(color).copy(
        shadow = colors.shadow,
        fontFamily = type.family,
        fontSize = (size * type.scale).sp,
        lineHeight = (line * type.scale).sp,
    )
}

val HomeColorsForDarkWallpaper = HomeColors(
    text = HomeTokens.textOnDark,
    textMuted = HomeTokens.textMutedOnDark,
    preview = HomeTokens.previewOnDark,
    alphabet = HomeTokens.alphabetIdleOnDark,
    scrim = Color.Black,
    darkText = false,
    shadow = Shadow(color = Color.Black.copy(alpha = 0.45f), offset = Offset(0f, 2f), blurRadius = 8f),
)

val HomeColorsForLightWallpaper = HomeColors(
    text = HomeTokens.textOnLight,
    textMuted = HomeTokens.textMutedOnLight,
    preview = HomeTokens.previewOnLight,
    alphabet = HomeTokens.alphabetIdleOnLight,
    scrim = Color.White,
    darkText = true,
    shadow = null,
)

val LocalHomeColors = staticCompositionLocalOf { HomeColorsForDarkWallpaper }
