package moe.tnxg.koma.ui.component.media

import androidx.compose.runtime.Immutable

enum class KodamaBackground { Gradient, Solid }

enum class KodamaDetailLevel { Minimal, Basic, Standard, Full }

enum class KodamaDepth(
    val rotateRange: Float,
    val cameraDistanceScale: Float,
    val contentScale: Float,
) {
    None(rotateRange = 0f, cameraDistanceScale = 8f, contentScale = 1f),
    Subtle(rotateRange = 5f, cameraDistanceScale = 14f, contentScale = 0.97f),
    Medium(rotateRange = 10f, cameraDistanceScale = 10f, contentScale = 0.94f),
    Dramatic(rotateRange = 15f, cameraDistanceScale = 6f, contentScale = 0.90f),
}

enum class KodamaMood { Happy, Surprised, Sleepy, Cool, Cheeky }

enum class KodamaAnimation { Blink, Float, Sway, EyeWander, EyebrowBounce, Glance, Entrance }

enum class KodamaEyeType { Round, Cross, Line, Curved, Wink, Heart }

enum class KodamaEyebrowType { Arched, Flat, Raised, None }

enum class KodamaMouthType { Smile, Grin, O, Cat, Tongue, Smirk, Flat }

enum class KodamaCheekType { Blush, None }

enum class KodamaAccessoryType { None, Glasses, Sunglasses }

enum class KodamaSlotKind { Eyes, Mouth, Eyebrows, Cheeks, Accessory }

@Immutable
data class KodamaGradient(val fromArgb: Long, val toArgb: Long)

@Immutable
data class KodamaRotation(val x: Int, val y: Int)

@Immutable
data class KodamaSlots(
    val eyes: KodamaEyeType,
    val eyebrows: KodamaEyebrowType,
    val mouth: KodamaMouthType,
    val cheeks: KodamaCheekType,
    val accessory: KodamaAccessoryType,
)

@Immutable
data class KodamaConfig(
    val seedName: String,
    val background: KodamaBackground,
    val detailLevel: KodamaDetailLevel,
    val depth: KodamaDepth,
    val mood: KodamaMood?,
    val animations: Set<KodamaAnimation>,
)

@Immutable
data class KodamaResolved(
    val config: KodamaConfig,
    val seed: Long,
    val slots: KodamaSlots,
    val gradient: KodamaGradient,
    val rotation: KodamaRotation,
)
