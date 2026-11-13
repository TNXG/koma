package moe.tnxg.koma.platform.photo

/** Largest power-of-two subsample that keeps [width]x[height] within [maxDimension]. */
fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
    if (width <= 0 || height <= 0 || maxDimension <= 0) return 1
    var sampleSize = 1
    val longest = maxOf(width, height)
    while (longest / sampleSize > maxDimension) {
        sampleSize *= 2
    }
    return sampleSize
}
