package dev.farjad.driftlist.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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

/**
 * The edge alphabet. Sliding a finger along it magnifies the letters near the finger in a
 * wave, ticks a haptic per letter, and reports the letter so the list can jump there.
 */
@Composable
fun WaveAlphabet(
    letters: List<String>,
    enabledLetters: Set<String>,
    onLetter: (String) -> Unit,
    modifier: Modifier = Modifier,
    onTouchChange: (Boolean) -> Unit = {},
) {
    val colors = LocalHomeColors.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val maxOffsetPx = with(LocalDensity.current) { WaveTokens.maxOffset.toPx() }
    val bubblePx = with(LocalDensity.current) { SizeTokens.alphabetBubble.toPx() }

    var heightPx by remember { mutableIntStateOf(1) }
    var fingerY by remember { mutableStateOf<Float?>(null) }
    var current by remember { mutableStateOf<String?>(null) }
    val intensity = remember { Animatable(0f) }
    val enabled = letters.map { it in enabledLetters }
    val enabledList = letters.filter { it in enabledLetters }
    var a11yIndex by remember(enabledList) { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .width(SizeTokens.alphabetTouchWidth)
            .fillMaxHeight()
            .systemGestureExclusion()
            .onSizeChanged { heightPx = it.height.coerceAtLeast(1) }
            // TalkBack sees one adjustable control instead of 27 tiny buttons: swipe up or down
            // to move letter by letter, and the list follows.
            .semantics {
                contentDescription = "Alphabet"
                if (enabledList.isNotEmpty()) {
                    stateDescription = enabledList[a11yIndex.coerceIn(0, enabledList.lastIndex)]
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        a11yIndex.toFloat(),
                        0f..enabledList.lastIndex.toFloat(),
                        steps = (enabledList.size - 2).coerceAtLeast(0),
                    )
                    setProgress { value ->
                        val i = value.roundToInt().coerceIn(0, enabledList.lastIndex)
                        a11yIndex = i
                        onLetter(enabledList[i])
                        true
                    }
                }
            }
            .pointerInput(letters, enabledLetters) {
                fun pick(y: Float) {
                    fingerY = y
                    val slot = Wave.nearestEnabled(Wave.slotAt(y, heightPx.toFloat(), letters.size), enabled)
                    if (slot < 0) return
                    val letter = letters[slot]
                    if (letter != current) {
                        current = letter
                        a11yIndex = enabledList.indexOf(letter).coerceAtLeast(0)
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onLetter(letter)
                    }
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    onTouchChange(true)
                    scope.launch { intensity.animateTo(1f, tween(MotionTokens.fastMs)) }
                    pick(down.position.y)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        pick(change.position.y)
                        change.consume()
                    }
                    current = null
                    onTouchChange(false)
                    scope.launch {
                        intensity.animateTo(
                            0f,
                            spring(MotionTokens.springWaveDamping, MotionTokens.springWaveStiffness),
                        )
                        fingerY = null
                    }
                }
            },
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .width(SizeTokens.alphabetStripWidth)
                .align(Alignment.CenterEnd)
                .clearAndSetSemantics { },
        ) {
            letters.forEachIndexed { i, letter ->
                val isEnabled = enabled[i]
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(SizeTokens.alphabetStripWidth)
                        .graphicsLayer {
                            val y = fingerY
                            val t = if (y == null) {
                                0f
                            } else {
                                val slot = heightPx.toFloat() / letters.size
                                val distance = (y - (i + 0.5f) * slot) / slot
                                Wave.intensity(distance, WaveTokens.influenceLetters) * intensity.value
                            }
                            val scale = 1f + (WaveTokens.maxScale - 1f) * t
                            scaleX = scale
                            scaleY = scale
                            translationX = -maxOffsetPx * t
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        letter,
                        style = TypeTokens.alphabet.onWallpaper(
                            colors,
                            if (isEnabled) colors.alphabet else colors.alphabet.copy(alpha = 0.4f),
                        ),
                        fontWeight = if (letter == current) FontWeight.Black else null,
                    )
                }
            }
        }

        // Bubble showing the selected letter, drawn to the left of the strip.
        val y = fingerY
        val letter = current
        if (y != null && letter != null) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = -(bubblePx + maxOffsetPx + 16.dp.toPx()).roundToInt(),
                            y = (y - bubblePx / 2).roundToInt(),
                        )
                    }
                    .size(SizeTokens.alphabetBubble)
                    .shadow(ElevationTokens.pop, CircleShape)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(letter, style = TypeTokens.title.style(MaterialTheme.colorScheme.onSurface))
            }
        }
    }
}
