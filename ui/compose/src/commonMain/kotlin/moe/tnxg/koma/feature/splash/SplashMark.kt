package moe.tnxg.koma.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.withTransform
import moe.tnxg.koma.ui.theme.KomaTheme

// Launcher foreground coordinates, transformed to its 42×42 content box.
private const val Box = 42f
private const val ChipRadius = 1.95f
private const val TallRadius = 2.55f

internal enum class Tone { Slot, Stem, Middle, Violet, Blue, Bar }

/** One timetable cell in the mark; [delayMs] staggers its entrance. */
internal data class SplashPiece(
    val x: Float, val y: Float, val width: Float, val height: Float,
    val radius: Float, val tone: Tone, val delayMs: Int,
)

internal val splashPieces = listOf(
    // Cells settle in sequence, but the complete K is visible before any animation runs.
    SplashPiece(14.7f, 0f, 12.6f, 6.75f, ChipRadius, Tone.Slot, 0),
    SplashPiece(14.7f, 8.85f, 12.6f, 6.75f, ChipRadius, Tone.Slot, 22),
    SplashPiece(14.7f, 26.4f, 12.6f, 6.75f, ChipRadius, Tone.Slot, 44),
    SplashPiece(14.7f, 35.25f, 12.6f, 6.75f, ChipRadius, Tone.Slot, 66),
    SplashPiece(29.4f, 17.625f, 12.6f, 6.75f, ChipRadius, Tone.Slot, 88),
    SplashPiece(0f, 0f, 12.6f, 42f, TallRadius, Tone.Stem, 70),
    SplashPiece(29.4f, 0f, 12.6f, 15.6f, TallRadius, Tone.Violet, 95),
    SplashPiece(14.7f, 17.625f, 12.6f, 6.75f, ChipRadius, Tone.Middle, 115),
    SplashPiece(29.4f, 26.4f, 12.6f, 15.6f, TallRadius, Tone.Blue, 125),
    // Course title bars, last: the small white strokes that read as lesson names.
    SplashPiece(3.0f, 2.55f, 4.2f, 1.2f, .6f, Tone.Bar, 160),
    SplashPiece(32.4f, 2.55f, 3.6f, 1.2f, .6f, Tone.Bar, 182),
    SplashPiece(32.4f, 28.95f, 3.6f, 1.2f, .6f, Tone.Bar, 204),
)

/**
 * The launcher icon's K, drawn block by block. Each [reveals] Animatable runs 0..1; reading it
 * inside the Canvas records a redraw, not a recomposition, so the stagger stays cheap.
 */
@Composable
internal fun SplashMark(
    reveals: List<Animatable<Float, AnimationVector1D>>,
    modifier: Modifier = Modifier,
) {
    val colors = KomaTheme.colors
    Canvas(modifier) {
        val unit = size.minDimension / Box
        val stem = Brush.verticalGradient(
            listOf(colors.accent, lerp(colors.accent, Color.Black, .24f)),
            startY = 0f, endY = Box * unit,
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(colors.accent.copy(alpha = .10f), Color.Transparent),
                center = center, radius = size.minDimension * .95f,
            ),
            radius = size.minDimension * .95f,
            center = center,
        )
        splashPieces.forEachIndexed { index, piece ->
            val progress = reveals[index].value
            // Branding is opaque even before the first animation frame; only its transform moves.
            val scale = .94f + .06f * progress
            val pivot = Offset((piece.x + piece.width / 2) * unit, (piece.y + piece.height / 2) * unit)
            val topLeft = Offset(piece.x * unit, piece.y * unit)
            val cell = Size(piece.width * unit, piece.height * unit)
            val corner = CornerRadius(piece.radius * unit)
            val color = when (piece.tone) {
                Tone.Slot -> colors.accentSoft
                Tone.Middle -> colors.accent2
                Tone.Violet -> colors.toneViolet
                Tone.Blue -> colors.toneBlue
                Tone.Bar -> Color.White
                Tone.Stem -> colors.accent
            }
            val alpha = if (piece.tone == Tone.Bar) .85f else 1f
            withTransform({ scale(scale, scale, pivot) }) {
                if (piece.tone == Tone.Stem) {
                    drawRoundRect(stem, topLeft, cell, corner)
                } else {
                    drawRoundRect(color, topLeft, cell, corner, alpha = alpha)
                }
            }
        }
    }
}
