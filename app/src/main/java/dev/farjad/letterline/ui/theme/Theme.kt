package dev.farjad.letterline.ui.theme

import dev.farjad.letterline.data.IconStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import dev.farjad.letterline.R
import android.graphics.Typeface
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import dev.farjad.letterline.data.ClockStyle
import dev.farjad.letterline.data.Fonts
import dev.farjad.letterline.data.Look
import dev.farjad.letterline.data.ThemeMode
import dev.farjad.letterline.domain.Palette
import dev.farjad.letterline.system.FontStore
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
    // Tonal buttons, chips and switches read the secondary and tertiary roles; left unset they
    // fall back to Material's purple baseline instead of the theme's accent.
    secondary = LightTokens.accent,
    onSecondary = LightTokens.onAccent,
    secondaryContainer = LightTokens.accentContainer,
    onSecondaryContainer = LightTokens.onAccentContainer,
    tertiary = LightTokens.accent,
    onTertiary = LightTokens.onAccent,
    tertiaryContainer = LightTokens.accentContainer,
    onTertiaryContainer = LightTokens.onAccentContainer,
    surfaceTint = LightTokens.accent,
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
    // Tonal buttons, chips and switches read the secondary and tertiary roles; left unset they
    // fall back to Material's purple baseline instead of the theme's accent.
    secondary = DarkTokens.accent,
    onSecondary = DarkTokens.onAccent,
    secondaryContainer = DarkTokens.accentContainer,
    onSecondaryContainer = DarkTokens.onAccentContainer,
    tertiary = DarkTokens.accent,
    onTertiary = DarkTokens.onAccent,
    tertiaryContainer = DarkTokens.accentContainer,
    onTertiaryContainer = DarkTokens.onAccentContainer,
    surfaceTint = DarkTokens.accent,
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
fun LetterlineTheme(look: Look = Look(), wallpaperSeed: Int? = null, content: @Composable () -> Unit) {
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
        LocalIconStyle provides look.iconStyle,
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
        secondary = Color(set.accent),
        onSecondary = Color(set.onAccent),
        secondaryContainer = Color(set.container),
        onSecondaryContainer = Color(set.onContainer),
        tertiary = Color(set.accent),
        onTertiary = Color(set.onAccent),
        tertiaryContainer = Color(set.container),
        onTertiaryContainer = Color(set.onContainer),
        surfaceTint = Color(set.accent),
    )
}

/** Font and size for text drawn on the wallpaper (home, list, clock). Sheets keep the system font. */
@Immutable
data class HomeType(val family: FontFamily = FontFamily.Default, val scale: Float = 1f)

val LocalHomeType = staticCompositionLocalOf { HomeType() }
val LocalClockStyle = staticCompositionLocalOf { ClockStyle.Thin }
val LocalIconStyle = staticCompositionLocalOf { IconStyle.Icons }

/** System families by name; imported files from app storage. Anything unreadable falls back to the default. */
fun fontFamilyFor(context: Context, id: String): FontFamily = runCatching {
    when {
        id == Fonts.DEFAULT -> FontFamily.Default
        id.startsWith(Fonts.BUNDLED_PREFIX) -> bundledFamily(id) ?: FontFamily.Default
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

private val bundledRes = mapOf(
    "font:manrope" to R.font.manrope,
    "font:outfit" to R.font.outfit,
    "font:space_grotesk" to R.font.space_grotesk,
    "font:comfortaa" to R.font.comfortaa,
    "font:lora" to R.font.lora,
    "font:playfair_display" to R.font.playfair_display,
    "font:fraunces" to R.font.fraunces,
    "font:dm_serif_display" to R.font.dm_serif_display,
    "font:jetbrains_mono" to R.font.jetbrains_mono,
    "font:bebas_neue" to R.font.bebas_neue,
    "font:caveat" to R.font.caveat,
)

/** Weights the type tokens use. Variable files get each one from their weight axis. */
private val weights = listOf(300, 400, 500, 600, 700, 800)

@OptIn(ExperimentalTextApi::class)
private fun bundledFamily(id: String): FontFamily? {
    val res = bundledRes[id] ?: return null
    val info = Fonts.bundled.firstOrNull { it.id == id } ?: return null
    if (!info.variable) return FontFamily(Font(res))
    return FontFamily(
        weights.map { w ->
            Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
        },
    )
}
