package moe.tnxg.koma.app.preferences

import kotlin.random.Random

/**
 * One persisted seed fully describes a generated avatar, so it only changes when the user asks
 * for another one or logs in again.
 */
fun newAvatarSeed(random: Random = Random.Default): String {
    val bytes = ByteArray(16).also(random::nextBytes)
    return buildString(32) {
        for (byte in bytes) {
            val value = byte.toInt() and 0xFF
            append(HEX_DIGITS[value ushr 4])
            append(HEX_DIGITS[value and 0x0F])
        }
    }
}

private const val HEX_DIGITS = "0123456789abcdef"
