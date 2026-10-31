package com.vivek.reclaim.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

/**
 * An hourglass whose sand drains from the top chamber to the bottom one. When the bottom is full
 * it turns over 180 degrees and the sand runs down again, forever.
 *
 * A turned-over, full-bottom hourglass looks exactly like a fresh one, so the loop restarts from
 * "top full, upright" with no visible jump.
 */
@Composable
fun SandTimer(
    modifier: Modifier = Modifier,
    outlineColor: Color = Color.White,
    sandColor: Color = MaterialTheme.colorScheme.secondary,
    drainMillis: Int = 3200,
    flipMillis: Int = 800,
) {
    val totalMillis = drainMillis + flipMillis
    val drainShare = drainMillis.toFloat() / totalMillis
    val transition = rememberInfiniteTransition(label = "sand-timer")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(totalMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "sand-timer-progress",
    )

    Canvas(
        modifier = modifier
            .size(width = 48.dp, height = 64.dp)
            .graphicsLayer { rotationZ = flipAngle(t, drainShare) },
    ) {
        drawSandTimer(drainProgress(t, drainShare), outlineColor, sandColor)
    }
}

/** 0 -> 1 while the sand drains, then stays at 1 while the hourglass turns over. */
internal fun drainProgress(t: Float, drainShare: Float): Float = (t / drainShare).coerceIn(0f, 1f)

/** 0 while draining, then eases 0 -> 180 degrees during the flip. */
internal fun flipAngle(t: Float, drainShare: Float): Float =
    if (t < drainShare) {
        0f
    } else {
        180f * FastOutSlowInEasing.transform(((t - drainShare) / (1f - drainShare)).coerceIn(0f, 1f))
    }

private fun DrawScope.drawSandTimer(progress: Float, outlineColor: Color, sandColor: Color) {
    val strokeWidth = 3.dp.toPx()
    val inset = strokeWidth / 2
    val left = inset
    val right = size.width - inset
    val top = inset
    val bottom = size.height - inset
    val centerX = size.width / 2
    val neckY = size.height / 2
    val neckHalf = 2.dp.toPx()
    val halfWidth = (right - left) / 2
    val chamberHeight = neckY - top

    // Sand volume in a triangular chamber grows with the square of its height, hence the sqrt.
    val topFill = sqrt(1f - progress)
    if (topFill > 0.001f) {
        val height = chamberHeight * topFill
        val half = halfWidth * topFill
        drawPath(
            Path().apply {
                moveTo(centerX - half, neckY - height)
                lineTo(centerX + half, neckY - height)
                lineTo(centerX, neckY)
                close()
            },
            color = sandColor,
        )
    }

    val bottomFill = 1f - sqrt(1f - progress)
    val surfaceDistance = chamberHeight * (1f - bottomFill)
    if (progress > 0.001f) {
        val surfaceHalf = halfWidth * (1f - bottomFill)
        drawPath(
            Path().apply {
                moveTo(centerX - halfWidth, bottom)
                lineTo(centerX + halfWidth, bottom)
                lineTo(centerX + surfaceHalf, neckY + surfaceDistance)
                lineTo(centerX - surfaceHalf, neckY + surfaceDistance)
                close()
            },
            color = sandColor,
        )
    }

    // The falling stream, only while sand is actually moving.
    if (progress in 0.03f..0.97f) {
        drawLine(
            color = sandColor,
            start = Offset(centerX, neckY),
            end = Offset(centerX, neckY + surfaceDistance),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

    drawPath(
        Path().apply {
            moveTo(left, top)
            lineTo(right, top)
            lineTo(centerX + neckHalf, neckY)
            lineTo(right, bottom)
            lineTo(left, bottom)
            lineTo(centerX - neckHalf, neckY)
            close()
        },
        color = outlineColor,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}
