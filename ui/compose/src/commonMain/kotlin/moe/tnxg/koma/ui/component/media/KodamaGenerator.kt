package moe.tnxg.koma.ui.component.media

import kotlin.math.abs

internal val KODAMA_GRADIENTS: List<KodamaGradient> = listOf(
    KodamaGradient(0xFFE8D5F5, 0xFFC7A4E0),
    KodamaGradient(0xFFFFE0D0, 0xFFFFB899),
    KodamaGradient(0xFFD5F5E3, 0xFFA8E6CF),
    KodamaGradient(0xFFD6EEFF, 0xFF9DD1F5),
    KodamaGradient(0xFFFFD4D4, 0xFFFF9E9E),
    KodamaGradient(0xFFFFF5CC, 0xFFFFE580),
    KodamaGradient(0xFFE0EFD9, 0xFFB8D4A8),
    KodamaGradient(0xFFFFE4EE, 0xFFFFB6D0),
    KodamaGradient(0xFFF5E6D0, 0xFFE8C9A0),
    KodamaGradient(0xFFD0F0F5, 0xFFA0DDE8),
    KodamaGradient(0xFFF0D5E8, 0xFFE0A4CC),
    KodamaGradient(0xFFD5E8F5, 0xFFA4C7E0),
    KodamaGradient(0xFFF5F0D5, 0xFFE8DDA0),
    KodamaGradient(0xFFE0D5F0, 0xFFBCA4E0),
    KodamaGradient(0xFFF5D5D0, 0xFFE8A4A0),
    KodamaGradient(0xFFD0F5E0, 0xFFA0E8BF),
)

private val SPHERE_POSITIONS: List<KodamaRotation> = listOf(
    KodamaRotation(-1, 1),
    KodamaRotation(1, 1),
    KodamaRotation(1, 0),
    KodamaRotation(0, 1),
    KodamaRotation(-1, 0),
    KodamaRotation(0, 0),
    KodamaRotation(0, -1),
    KodamaRotation(-1, -1),
    KodamaRotation(1, -1),
)

private val SLOT_MIN_DETAIL: Map<KodamaSlotKind, KodamaDetailLevel> = mapOf(
    KodamaSlotKind.Eyes to KodamaDetailLevel.Minimal,
    KodamaSlotKind.Mouth to KodamaDetailLevel.Basic,
    KodamaSlotKind.Eyebrows to KodamaDetailLevel.Standard,
    KodamaSlotKind.Cheeks to KodamaDetailLevel.Full,
    KodamaSlotKind.Accessory to KodamaDetailLevel.Full,
)

internal fun kodamaHash(input: String): Long {
    var h = 0
    for (i in input.indices) {
        h = (h shl 5) - h + input[i].code
    }
    return abs(h.toLong())
}

internal fun canRenderKodamaSlot(slot: KodamaSlotKind, detailLevel: KodamaDetailLevel): Boolean {
    val minDetail = SLOT_MIN_DETAIL.getValue(slot)
    return detailLevel.ordinal >= minDetail.ordinal
}

/**
 * The whole look of a generated avatar comes from one persisted seed, so the same login keeps
 * showing the same face across restarts while every dimension still varies between seeds.
 */
internal fun kodamaConfigForSeed(seedName: String): KodamaConfig {
    val seed = kodamaHash(seedName)
    fun pick(salt: Long, size: Int): Int = (kodamaVariant(seed, salt) % size).toInt()
    val moods = listOf<KodamaMood?>(null) + KodamaMood.entries
    val animations = KodamaAnimation.entries
        .filterIndexed { index, _ -> kodamaVariant(seed, 131L + index) and 1L == 1L }
        .toSet()
        .ifEmpty { setOf(KodamaAnimation.Blink) }
    return KodamaConfig(
        seedName = seedName,
        background = KodamaBackground.entries[pick(17, KodamaBackground.entries.size)],
        detailLevel = KodamaDetailLevel.entries[pick(23, KodamaDetailLevel.entries.size)],
        depth = KodamaDepth.entries[pick(29, KodamaDepth.entries.size)],
        mood = moods[pick(37, moods.size)],
        animations = animations,
    )
}

/** Salted bit mixing so neighbouring seeds (…-0, …-1) differ in every dimension. */
private fun kodamaVariant(seed: Long, salt: Long): Long {
    val mixed = seed * salt + (seed ushr 13) + salt * 7919L
    return (mixed xor (mixed ushr 17)) and Long.MAX_VALUE
}

internal fun resolveKodama(config: KodamaConfig): KodamaResolved {
    val seed = kodamaHash(config.seedName)
    val eyes = KodamaEyeType.entries[(seed % KodamaEyeType.entries.size).toInt()]
    val eyebrows = KodamaEyebrowType.entries[((seed * 7L) % KodamaEyebrowType.entries.size).toInt()]
    val mouth = KodamaMouthType.entries[((seed * 13L) % KodamaMouthType.entries.size).toInt()]
    val cheeks = KodamaCheekType.entries[((seed * 19L) % KodamaCheekType.entries.size).toInt()]
    val accessory = KodamaAccessoryType.entries[((seed * 31L) % KodamaAccessoryType.entries.size).toInt()]
    val colorIndex = ((seed * 37L) % KODAMA_GRADIENTS.size).toInt()
    val positionIndex = ((seed * 43L) % SPHERE_POSITIONS.size).toInt()

    val baseSlots = KodamaSlots(
        eyes = eyes,
        eyebrows = eyebrows,
        mouth = mouth,
        cheeks = cheeks,
        accessory = accessory,
    )
    val moodSlots = applyKodamaMood(baseSlots, config.mood)

    return KodamaResolved(
        config = config,
        seed = seed,
        slots = moodSlots,
        gradient = KODAMA_GRADIENTS[colorIndex],
        rotation = SPHERE_POSITIONS[positionIndex],
    )
}

private fun applyKodamaMood(slots: KodamaSlots, mood: KodamaMood?): KodamaSlots = when (mood) {
    null -> slots
    KodamaMood.Happy -> slots.copy(
        eyes = KodamaEyeType.Curved,
        mouth = KodamaMouthType.Grin,
        eyebrows = KodamaEyebrowType.Arched,
    )
    KodamaMood.Surprised -> slots.copy(
        eyes = KodamaEyeType.Round,
        mouth = KodamaMouthType.O,
        eyebrows = KodamaEyebrowType.Raised,
    )
    KodamaMood.Sleepy -> slots.copy(
        eyes = KodamaEyeType.Line,
        mouth = KodamaMouthType.Flat,
        eyebrows = KodamaEyebrowType.Flat,
    )
    KodamaMood.Cool -> slots.copy(
        eyes = KodamaEyeType.Round,
        accessory = KodamaAccessoryType.Sunglasses,
        mouth = KodamaMouthType.Smirk,
    )
    KodamaMood.Cheeky -> slots.copy(
        eyes = KodamaEyeType.Wink,
        mouth = KodamaMouthType.Tongue,
        cheeks = KodamaCheekType.Blush,
    )
}

