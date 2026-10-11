package dev.farjad.driftlist.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import dev.farjad.driftlist.domain.Wave
import dev.farjad.driftlist.ui.theme.ElevationTokens
import dev.farjad.driftlist.ui.theme.LocalHomeColors
import dev.farjad.driftlist.ui.theme.MotionTokens
import dev.farjad.driftlist.ui.theme.SizeTokens
import dev.farjad.driftlist.ui.theme.TypeTokens
import dev.farjad.driftlist.ui.theme.WaveTokens
import dev.farjad.driftlist.ui.theme.onWallpaper
import dev.farjad.driftlist.ui.theme.style
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val STAR = "☆" // favorites
private const val CIRCLE = "○" // settings

/**
 * The edge alphabet: the favorites star, the letters that have apps, and a settings circle,
 * stacked in the lower part of the screen. Sliding a finger along it pushes the letters out
 * sideways in a V around the finger, ticks a haptic per letter and reports the letter so the
 * list can jump there.
 */
@Composable
fun WaveAlphabet(
    letters: List<String>,
    enabledLetters: Set<String>,
    onLetter: (String) -> Unit,
    onFavorites: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHomeColors.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val maxOffsetPx = with(density) { WaveTokens.maxOffset.toPx() }
    val bubblePx = with(density) { SizeTokens.alphabetBubble.toPx() }

    // Only letters with apps, framed by the star and the circle.
    val shown = letters.filter { it in enabledLetters }
    val entries = listOf(STAR) + shown + CIRCLE
    val latestLetter by rememberUpdatedState(onLetter)
    val latestFavorites by rememberUpdatedState(onFavorites)
    val latestSettings by rememberUpdatedState(onSettings)

    var heightPx by remember { mutableIntStateOf(1) }
    var fingerY by remember { mutableStateOf<Float?>(null) }
    var current by remember { mutableStateOf<String?>(null) }
    var a11yIndex by remember(shown) { mutableIntStateOf(0) }
    val intensity = remember { Animatable(0f) }

    // The band the entries occupy, as measured on the reference: roughly a quarter of the way down to near the bottom.
    val bandTop = heightPx * SizeTokens.alphabetTopRatio
    val bandHeight = (heightPx * (SizeTokens.alphabetBottomRatio - SizeTokens.alphabetTopRatio)).coerceAtLeast(1f)
    val slot = bandHeight / entries.size

    Box(
        modifier = modifier
            .width(SizeTokens.alphabetTouchWidth)
            .fillMaxHeight()
            .onSizeChanged { heightPx = it.height.coerceAtLeast(1) }
            .semantics {
                contentDescription = "Alphabet"
                customActions = listOf(
                    CustomAccessibilityAction("Favorites") { latestFavorites(); true },
                    CustomAccessibilityAction("Driftlist settings") { latestSettings(); true },
                )
                if (shown.isNotEmpty()) {
                    stateDescription = shown[a11yIndex.coerceIn(0, shown.lastIndex)]
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        a11yIndex.toFloat(),
                        0f..shown.lastIndex.toFloat(),
                        steps = (shown.size - 2).coerceAtLeast(0),
                    )
                    setProgress { value ->
                        val i = value.roundToInt().coerceIn(0, shown.lastIndex)
                        a11yIndex = i
                        latestLetter(shown[i])
                        true
                    }
                }
            },
    ) {
        // The touch band; system back gestures are excluded only here.
        Box(
            Modifier
                .offset { IntOffset(0, bandTop.roundToInt()) }
                .width(SizeTokens.alphabetTouchWidth)
                .size(SizeTokens.alphabetTouchWidth, with(density) { bandHeight.toDp() })
                .systemGestureExclusion()
                .pointerInput(entries) {
                    fun pick(localY: Float) {
                        fingerY = localY
                        val i = Wave.slotAt(localY, bandHeight, entries.size)
                        val entry = entries[i]
                        if (entry != current) {
                            current = entry
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            when (entry) {
                                STAR -> latestFavorites()
                                CIRCLE -> Unit // settings only on release, so sliding past it is harmless
                                else -> {
                                    a11yIndex = shown.indexOf(entry).coerceAtLeast(0)
                                    latestLetter(entry)
                                }
                            }
                        }
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        scope.launch { intensity.animateTo(1f, tween(MotionTokens.fastMs)) }
                        pick(down.position.y)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            pick(change.position.y)
                            change.consume()
                        }
                        if (current == CIRCLE) latestSettings()
                        current = null
                        scope.launch {
                            intensity.animateTo(0f, spring(MotionTokens.springWaveDamping, MotionTokens.springWaveStiffness))
                            fingerY = null
                        }
                    }
                },
        )

        // Entries, hidden from accessibility (the strip above is one adjustable control).
        Box(Modifier.fillMaxHeight().width(SizeTokens.alphabetTouchWidth).clearAndSetSemantics { }) {
            val influence = entries.size * WaveTokens.influenceFraction
            entries.forEachIndexed { i, entry ->
                val center = bandTop + (i + 0.5f) * slot
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, (center - slot / 2).roundToInt()) }
                        .size(SizeTokens.alphabetTouchWidth, with(density) { slot.toDp() })
                        .graphicsLayer {
                            val y = fingerY
                            val t = if (y == null) 0f else Wave.intensity((y - (i + 0.5f) * slot) / slot, influence) * intensity.value
                            val scale = 1f + (WaveTokens.maxScale - 1f) * t
                            scaleX = scale
                            scaleY = scale
                            translationX = -maxOffsetPx * t
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        entry,
                        style = TypeTokens.alphabet.onWallpaper(colors, colors.alphabet),
                        fontWeight = if (entry == current) FontWeight.Black else null,
                    )
                }
            }
        }

        // Bubble with the selected letter, at the finger, out where the wave peaks.
        val y = fingerY
        val selected = current
        if (y != null && selected != null && selected != STAR && selected != CIRCLE) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = -(maxOffsetPx + bubblePx * 0.6f).roundToInt(),
                            y = (bandTop + y - bubblePx / 2).roundToInt(),
                        )
                    }
                    .size(SizeTokens.alphabetBubble)
                    .shadow(ElevationTokens.pop, CircleShape)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(selected, style = TypeTokens.title.style(MaterialTheme.colorScheme.onSurface))
            }
        }
    }
}

