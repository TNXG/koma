package moe.tnxg.koma.ui.component.media

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap

@Immutable
internal data class KodamaPathSpec(
    val d: String,
    val fill: Color? = null,
    val alpha: Float = 1f,
)

@Immutable
internal data class KodamaStrokeSpec(
    val d: String,
    val strokeWidth: Float,
    val cap: StrokeCap = StrokeCap.Butt,
)

@Immutable
internal data class KodamaCircleSpec(
    val cx: Float,
    val cy: Float,
    val r: Float,
    val fill: Color? = null,
    val alpha: Float = 1f,
    val stroked: Boolean = false,
    val strokeWidth: Float = 0f,
)

@Immutable
internal data class KodamaEllipseSpec(
    val cx: Float,
    val cy: Float,
    val rx: Float,
    val ry: Float,
    val strokeWidth: Float,
)

@Immutable
internal data class KodamaRectSpec(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rx: Float = 0f,
    val alpha: Float = 1f,
)

@Immutable
internal data class KodamaVectorShape(
    val viewBoxWidth: Float,
    val viewBoxHeight: Float,
    val paths: List<KodamaPathSpec> = emptyList(),
    val strokes: List<KodamaStrokeSpec> = emptyList(),
    val circles: List<KodamaCircleSpec> = emptyList(),
    val ellipses: List<KodamaEllipseSpec> = emptyList(),
    val rects: List<KodamaRectSpec> = emptyList(),
)

internal fun kodamaEyeShape(type: KodamaEyeType): KodamaVectorShape = when (type) {
    KodamaEyeType.Round -> KodamaVectorShape(
        viewBoxWidth = 63f,
        viewBoxHeight = 15f,
        paths = listOf(
            KodamaPathSpec("M62.4 7.2C62.4 11.1765 59.1765 14.4 55.2 14.4C51.2236 14.4 48 11.1765 48 7.2C48 3.22355 51.2236 0 55.2 0C59.1765 0 62.4 3.22355 62.4 7.2Z"),
            KodamaPathSpec("M14.4 7.2C14.4 11.1765 11.1765 14.4 7.2 14.4C3.22355 14.4 0 11.1765 0 7.2C0 3.22355 3.22355 0 7.2 0C11.1765 0 14.4 3.22355 14.4 7.2Z"),
        ),
    )
    KodamaEyeType.Cross -> KodamaVectorShape(
        viewBoxWidth = 71f,
        viewBoxHeight = 23f,
        paths = listOf(
            KodamaPathSpec("M11.5 0C12.9411 0 13.6619 0.000460386 14.1748 0.354492C14.3742 0.49213 14.547 0.664882 14.6846 0.864258C15.0384 1.37711 15.0391 2.09739 15.0391 3.53809V7.96094H19.4619C20.9027 7.96094 21.6229 7.9615 22.1357 8.31543C22.3352 8.45308 22.5079 8.62578 22.6455 8.8252C22.9995 9.3381 23 10.0589 23 11.5C23 12.9408 22.9995 13.661 22.6455 14.1738C22.5079 14.3733 22.3352 14.5459 22.1357 14.6836C21.6229 15.0375 20.9027 15.0381 19.4619 15.0381H15.0391V19.4619C15.0391 20.9026 15.0384 21.6229 14.6846 22.1357C14.547 22.3351 14.3742 22.5079 14.1748 22.6455C13.6619 22.9995 12.9411 23 11.5 23C10.0592 23 9.33903 22.9994 8.82617 22.6455C8.62674 22.5079 8.45309 22.3352 8.31543 22.1357C7.96175 21.6229 7.96191 20.9024 7.96191 19.4619V15.0381H3.53809C2.0973 15.0381 1.37711 15.0375 0.864258 14.6836C0.664834 14.5459 0.492147 14.3733 0.354492 14.1738C0.000498831 13.661 -5.88036e-08 12.9408 0 11.5C6.2999e-08 10.0589 0.000460356 9.3381 0.354492 8.8252C0.492144 8.62578 0.664842 8.45308 0.864258 8.31543C1.37711 7.9615 2.09731 7.96094 3.53809 7.96094H7.96191V3.53809C7.96191 2.09765 7.96175 1.37709 8.31543 0.864258C8.45309 0.664828 8.62674 0.492149 8.82617 0.354492C9.33903 0.000555366 10.0592 1.62347e-09 11.5 0Z"),
            KodamaPathSpec("M58.7695 0C60.2107 0 60.9314 0.000460386 61.4443 0.354492C61.6437 0.49213 61.8165 0.664882 61.9541 0.864258C62.308 1.37711 62.3086 2.09739 62.3086 3.53809V7.96094H66.7314C68.1722 7.96094 68.8924 7.9615 69.4053 8.31543C69.6047 8.45308 69.7774 8.62578 69.915 8.8252C70.2691 9.3381 70.2695 10.0589 70.2695 11.5C70.2695 12.9408 70.269 13.661 69.915 14.1738C69.7774 14.3733 69.6047 14.5459 69.4053 14.6836C68.8924 15.0375 68.1722 15.0381 66.7314 15.0381H62.3086V19.4619C62.3086 20.9026 62.308 21.6229 61.9541 22.1357C61.8165 22.3351 61.6437 22.5079 61.4443 22.6455C60.9314 22.9995 60.2107 23 58.7695 23C57.3287 23 56.6086 22.9994 56.0957 22.6455C55.8963 22.5079 55.7226 22.3352 55.585 22.1357C55.2313 21.6229 55.2314 20.9024 55.2314 19.4619V15.0381H50.8076C49.3668 15.0381 48.6466 15.0375 48.1338 14.6836C47.9344 14.5459 47.7617 14.3733 47.624 14.1738C47.27 13.661 47.2695 12.9408 47.2695 11.5C47.2695 10.0589 47.27 9.3381 47.624 8.8252C47.7617 8.62578 47.9344 8.45308 48.1338 8.31543C48.6466 7.9615 49.3668 7.96094 50.8076 7.96094H55.2314V3.53809C55.2314 2.09765 55.2313 1.37709 55.585 0.864258C55.7226 0.664828 55.8963 0.492149 56.0957 0.354492C56.6086 0.000555366 57.3287 1.62347e-09 58.7695 0Z"),
        ),
    )
    KodamaEyeType.Line -> KodamaVectorShape(
        viewBoxWidth = 70f,
        viewBoxHeight = 10f,
        circles = listOf(
            KodamaCircleSpec(cx = 4f, cy = 5f, r = 3.5f),
            KodamaCircleSpec(cx = 66f, cy = 5f, r = 3.5f),
        ),
        rects = listOf(
            KodamaRectSpec(x = 12f, y = 1.5f, width = 16f, height = 7f, rx = 3.5f),
            KodamaRectSpec(x = 42f, y = 1.5f, width = 16f, height = 7f, rx = 3.5f),
        ),
    )
    KodamaEyeType.Curved -> KodamaVectorShape(
        viewBoxWidth = 63f,
        viewBoxHeight = 9f,
        paths = listOf(
            KodamaPathSpec("M0 5.06511C0 4.94513 0 4.88513 0.00771184 4.79757C0.0483059 4.33665 0.341025 3.76395 0.690821 3.46107C0.757274 3.40353 0.783996 3.38422 0.837439 3.34559C2.40699 2.21129 6.03888 0 10.5 0C14.9611 0 18.593 2.21129 20.1626 3.34559C20.216 3.38422 20.2427 3.40353 20.3092 3.46107C20.659 3.76395 20.9517 4.33665 20.9923 4.79757C21 4.88513 21 4.94513 21 5.06511C21 6.01683 21 6.4927 20.9657 6.6754C20.7241 7.96423 19.8033 8.55941 18.5289 8.25054C18.3483 8.20676 17.8198 7.96876 16.7627 7.49275C14.975 6.68767 12.7805 6 10.5 6C8.21954 6 6.02504 6.68767 4.23727 7.49275C3.18025 7.96876 2.65174 8.20676 2.47108 8.25054C1.19668 8.55941 0.275917 7.96423 0.0342566 6.6754C0 6.4927 0 6.01683 0 5.06511Z"),
            KodamaPathSpec("M42 5.06511C42 4.94513 42 4.88513 42.0077 4.79757C42.0483 4.33665 42.341 3.76395 42.6908 3.46107C42.7573 3.40353 42.784 3.38422 42.8374 3.34559C44.407 2.21129 48.0389 0 52.5 0C56.9611 0 60.593 2.21129 62.1626 3.34559C62.216 3.38422 62.2427 3.40353 62.3092 3.46107C62.659 3.76395 62.9517 4.33665 62.9923 4.79757C63 4.88513 63 4.94513 63 5.06511C63 6.01683 63 6.4927 62.9657 6.6754C62.7241 7.96423 61.8033 8.55941 60.5289 8.25054C60.3483 8.20676 59.8198 7.96876 58.7627 7.49275C56.975 6.68767 54.7805 6 52.5 6C50.2195 6 48.025 6.68767 46.2373 7.49275C45.1802 7.96876 44.6517 8.20676 44.4711 8.25054C43.1967 8.55941 42.2759 7.96423 42.0343 6.6754C42 6.4927 42 6.01683 42 5.06511Z"),
        ),
    )
    KodamaEyeType.Wink -> KodamaVectorShape(
        viewBoxWidth = 63f,
        viewBoxHeight = 15f,
        strokes = listOf(KodamaStrokeSpec("M1 9C3 4 5 3 7.5 3C10 3 12 4 14 9", 3.5f, StrokeCap.Round)),
        circles = listOf(KodamaCircleSpec(cx = 55.2f, cy = 7.2f, r = 7.2f)),
    )
    KodamaEyeType.Heart -> KodamaVectorShape(
        viewBoxWidth = 63f,
        viewBoxHeight = 15f,
        paths = listOf(
            KodamaPathSpec("M7.5 14C5 11 0 8.5 0 5C0 2.2 2.2 0 5 0C6.5 0 7.5 1.2 7.5 1.2C7.5 1.2 8.5 0 10 0C12.8 0 15 2.2 15 5C15 8.5 10 11 7.5 14Z"),
            KodamaPathSpec("M55.5 14C53 11 48 8.5 48 5C48 2.2 50.2 0 53 0C54.5 0 55.5 1.2 55.5 1.2C55.5 1.2 56.5 0 58 0C60.8 0 63 2.2 63 5C63 8.5 58 11 55.5 14Z"),
        ),
    )
}

internal fun kodamaEyebrowShape(type: KodamaEyebrowType): KodamaVectorShape? = when (type) {
    KodamaEyebrowType.Arched -> KodamaVectorShape(
        viewBoxWidth = 63f,
        viewBoxHeight = 8f,
        strokes = listOf(
            KodamaStrokeSpec("M2 7C4 2.5 9 0.5 14 2", 3f, StrokeCap.Round),
            KodamaStrokeSpec("M49 2C54 0.5 59 2.5 61 7", 3f, StrokeCap.Round),
        ),
    )
    KodamaEyebrowType.Flat -> KodamaVectorShape(
        viewBoxWidth = 63f,
        viewBoxHeight = 8f,
        strokes = listOf(
            KodamaStrokeSpec("M2 4H14", 3f, StrokeCap.Round),
            KodamaStrokeSpec("M49 4H61", 3f, StrokeCap.Round),
        ),
    )
    KodamaEyebrowType.Raised -> KodamaVectorShape(
        viewBoxWidth = 63f,
        viewBoxHeight = 8f,
        strokes = listOf(
            KodamaStrokeSpec("M2 6C5 1 11 0 14 1.5", 3f, StrokeCap.Round),
            KodamaStrokeSpec("M49 1.5C52 0 58 1 61 6", 3f, StrokeCap.Round),
        ),
    )
    KodamaEyebrowType.None -> null
}

internal fun kodamaMouthShape(type: KodamaMouthType): KodamaVectorShape = when (type) {
    KodamaMouthType.Smile -> KodamaVectorShape(
        viewBoxWidth = 30f,
        viewBoxHeight = 16f,
        strokes = listOf(KodamaStrokeSpec("M4 5C8 14 22 14 26 5", 3f, StrokeCap.Round)),
    )
    KodamaMouthType.Grin -> KodamaVectorShape(
        viewBoxWidth = 30f,
        viewBoxHeight = 16f,
        strokes = listOf(KodamaStrokeSpec("M3 5C5 14 10 16 15 16C20 16 25 14 27 5", 3f, StrokeCap.Round)),
    )
    KodamaMouthType.O -> KodamaVectorShape(
        viewBoxWidth = 30f,
        viewBoxHeight = 16f,
        ellipses = listOf(KodamaEllipseSpec(cx = 15f, cy = 8f, rx = 5f, ry = 6f, strokeWidth = 3f)),
    )
    KodamaMouthType.Cat -> KodamaVectorShape(
        viewBoxWidth = 30f,
        viewBoxHeight = 16f,
        strokes = listOf(KodamaStrokeSpec("M5 5C7 12 13 12 15 5C17 12 23 12 25 5", 3f, StrokeCap.Round)),
    )
    KodamaMouthType.Tongue -> KodamaVectorShape(
        viewBoxWidth = 30f,
        viewBoxHeight = 16f,
        paths = listOf(
            KodamaPathSpec(
                d = "M10 9C10 12.5 11 14.5 13 14.5C15 14.5 16 12.5 16 9",
                fill = Color(0xFFFF6B8A),
                alpha = 0.7f,
            ),
        ),
        strokes = listOf(KodamaStrokeSpec("M5 4C9 13 21 13 25 4", 3f, StrokeCap.Round)),
    )
    KodamaMouthType.Smirk -> KodamaVectorShape(
        viewBoxWidth = 30f,
        viewBoxHeight = 16f,
        strokes = listOf(KodamaStrokeSpec("M8 8C12 10 18 6 22 4", 3f, StrokeCap.Round)),
    )
    KodamaMouthType.Flat -> KodamaVectorShape(
        viewBoxWidth = 30f,
        viewBoxHeight = 16f,
        strokes = listOf(KodamaStrokeSpec("M8 8H22", 3f, StrokeCap.Round)),
    )
}

internal fun kodamaCheekShape(type: KodamaCheekType): KodamaVectorShape? = when (type) {
    KodamaCheekType.Blush -> KodamaVectorShape(
        viewBoxWidth = 70f,
        viewBoxHeight = 12f,
        circles = listOf(
            KodamaCircleSpec(cx = 16f, cy = 6f, r = 6f, fill = Color(0xFFFF9EBB), alpha = 0.3f),
            KodamaCircleSpec(cx = 54f, cy = 6f, r = 6f, fill = Color(0xFFFF9EBB), alpha = 0.3f),
        ),
    )
    KodamaCheekType.None -> null
}

internal fun kodamaAccessoryShape(type: KodamaAccessoryType): KodamaVectorShape? = when (type) {
    KodamaAccessoryType.None -> null
    KodamaAccessoryType.Glasses -> KodamaVectorShape(
        viewBoxWidth = 63f,
        viewBoxHeight = 20f,
        circles = listOf(
            KodamaCircleSpec(cx = 11f, cy = 11f, r = 8f, stroked = true, strokeWidth = 2.5f),
            KodamaCircleSpec(cx = 52f, cy = 11f, r = 8f, stroked = true, strokeWidth = 2.5f),
        ),
        strokes = listOf(KodamaStrokeSpec("M19 11C19 6 44 6 44 11", 2.5f, StrokeCap.Butt)),
    )
    KodamaAccessoryType.Sunglasses -> KodamaVectorShape(
        viewBoxWidth = 63f,
        viewBoxHeight = 20f,
        rects = listOf(
            KodamaRectSpec(x = 1f, y = 4f, width = 20f, height = 14f, rx = 4f, alpha = 0.75f),
            KodamaRectSpec(x = 42f, y = 4f, width = 20f, height = 14f, rx = 4f, alpha = 0.75f),
        ),
        strokes = listOf(KodamaStrokeSpec("M21 10C21 5 42 5 42 10", 3f, StrokeCap.Round)),
    )
}
