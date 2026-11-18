package moe.tnxg.koma.feature.home.timeline.course

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** One closed contour so clipping, fill and the inset border share the notches. */
internal object CourseTicketShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val width = size.width
        val height = size.height
        val corner = with(density) { 16.dp.toPx() }.coerceAtMost(minOf(width, height) / 2f)
        val notch = with(density) { 6.dp.toPx() }
            .coerceAtMost(minOf(width / 2f, height / 2f - corner))
        val middle = height / 2f
        return Outline.Generic(Path().apply {
            moveTo(corner, 0f)
            lineTo(width - corner, 0f)
            arcTo(Rect(width - 2 * corner, 0f, width, 2 * corner), -90f, 90f, false)
            lineTo(width, middle - notch)
            if (notch > 0f) {
                arcTo(Rect(width - notch, middle - notch, width + notch, middle + notch), -90f, -180f, false)
            }
            lineTo(width, height - corner)
            arcTo(Rect(width - 2 * corner, height - 2 * corner, width, height), 0f, 90f, false)
            lineTo(corner, height)
            arcTo(Rect(0f, height - 2 * corner, 2 * corner, height), 90f, 90f, false)
            lineTo(0f, middle + notch)
            if (notch > 0f) {
                arcTo(Rect(-notch, middle - notch, notch, middle + notch), 90f, -180f, false)
            }
            lineTo(0f, corner)
            arcTo(Rect(0f, 0f, 2 * corner, 2 * corner), 180f, 90f, false)
            close()
        })
    }
}
