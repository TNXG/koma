package moe.tnxg.koma.ui.component.media

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.tnxg.koma.app.preferences.LocalAppPreferences

/** Generated avatar for one persisted [seed]; always circular. */
@Composable
fun KodamaAvatar(seed: String, modifier: Modifier = Modifier) {
    val resolved = remember(seed) { resolveKodama(kodamaConfigForSeed(seed)) }
    val reduceMotion = LocalAppPreferences.current.reduceMotion
    KodamaFace(resolved = resolved, reduceMotion = reduceMotion, modifier = modifier)
}

@Composable
private fun KodamaFace(
    resolved: KodamaResolved,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val config = resolved.config
    val animations = config.animations
    val depth = config.depth
    val detailLevel = config.detailLevel

    val showEyebrows = canRenderKodamaSlot(KodamaSlotKind.Eyebrows, detailLevel)
    val showMouth = canRenderKodamaSlot(KodamaSlotKind.Mouth, detailLevel)
    val showCheeks = canRenderKodamaSlot(KodamaSlotKind.Cheeks, detailLevel)
    val showAccessories = canRenderKodamaSlot(KodamaSlotKind.Accessory, detailLevel)
    val accessoryShape = kodamaAccessoryShape(resolved.slots.accessory)
    val glassesActive = showAccessories && accessoryShape != null
    val showEyes = !glassesActive

    val hasEntrance = !reduceMotion && KodamaAnimation.Entrance in animations
    val entranceProgress = remember(resolved, reduceMotion) { Animatable(if (hasEntrance) 0f else 1f) }
    val entranceEyeScale = remember(resolved, reduceMotion) {
        Animatable(if (hasEntrance && !glassesActive) 0.05f else 1f)
    }

    LaunchedEffect(resolved, reduceMotion) {
        if (!hasEntrance) {
            entranceProgress.snapTo(1f)
            entranceEyeScale.snapTo(1f)
            return@LaunchedEffect
        }
        coroutineScope {
            launch {
                entranceProgress.animateTo(1f, tween(durationMillis = 400, easing = FastOutSlowInEasing))
            }
            if (!glassesActive) {
                launch {
                    delay(200)
                    entranceEyeScale.animateTo(1f, tween(durationMillis = 300, easing = FastOutSlowInEasing))
                }
            }
        }
    }

    val infinite = rememberInfiniteTransition(label = "kodama")
    val blinkScaleY by rememberKodamaBlink(infinite, resolved.seed, !reduceMotion && KodamaAnimation.Blink in animations)
    val floatFrac by rememberKodamaFloat(infinite, !reduceMotion && KodamaAnimation.Float in animations)
    val swayFrac by rememberKodamaSway(infinite, !reduceMotion && KodamaAnimation.Sway in animations)
    val wanderX by rememberKodamaEyeWanderX(infinite, !reduceMotion && KodamaAnimation.EyeWander in animations)
    val wanderY by rememberKodamaEyeWanderY(infinite, !reduceMotion && KodamaAnimation.EyeWander in animations)
    val eyebrowBounceFrac by rememberKodamaEyebrowBounce(
        infinite,
        !reduceMotion && KodamaAnimation.EyebrowBounce in animations,
    )
    val glanceProgress by rememberKodamaGlance(
        infinite,
        resolved.seed,
        !reduceMotion && KodamaAnimation.Glance in animations && depth != KodamaDepth.None,
    )

    val containerScale = 0.8f + 0.2f * entranceProgress.value
    val restRotX = resolved.rotation.x * depth.rotateRange
    val restRotY = resolved.rotation.y * depth.rotateRange
    val activeRotX = restRotX * (1f - glanceProgress)
    val activeRotY = restRotY * (1f - glanceProgress)
    val effectiveFloat = if (KodamaAnimation.Glance in animations && depth != KodamaDepth.None) 0f else floatFrac

    val fromColor = Color(resolved.gradient.fromArgb)
    val toColor = Color(resolved.gradient.toArgb)

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .semantics {
                role = Role.Image
                contentDescription = "头像"
            }
            .graphicsLayer {
                scaleX = containerScale
                scaleY = containerScale
                alpha = entranceProgress.value
            }
            .clip(CircleShape)
            .background(fromColor),
    ) {
        val avatarSize = maxWidth
        val avatarPx = with(LocalDensity.current) { avatarSize.toPx() }
        if (config.background == KodamaBackground.Gradient) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(fromColor, toColor),
                        center = Offset(size.width * 0.4f, size.height * 0.4f),
                        radius = size.maxDimension * 0.85f,
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to Color.White.copy(alpha = 0.15f),
                            0.6f to Color.Transparent,
                            1f to Color.Transparent,
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                        radius = size.minDimension * 0.6f,
                    ),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = swayFrac * avatarPx },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = depth.contentScale
                        scaleY = depth.contentScale
                        rotationX = activeRotX
                        rotationY = activeRotY
                        cameraDistance = depth.cameraDistanceScale * density
                        translationY = effectiveFloat * avatarPx
                    },
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val eyebrowShape = if (showEyebrows) kodamaEyebrowShape(resolved.slots.eyebrows) else null
                    if (eyebrowShape != null) {
                        KodamaFeatureCanvas(
                            shape = eyebrowShape,
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .graphicsLayer { translationY = eyebrowBounceFrac * avatarPx },
                        )
                        Spacer(Modifier.height(avatarSize * if (glassesActive) 0.06f else 0.02f))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.60f)
                            .graphicsLayer {
                                translationX = wanderX * avatarPx
                                translationY = wanderY * avatarPx
                                scaleY = entranceEyeScale.value
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (showEyes) {
                            KodamaFeatureCanvas(
                                shape = kodamaEyeShape(resolved.slots.eyes),
                                modifier = Modifier
                                    .fillMaxWidth(0.90f)
                                    .graphicsLayer { scaleY = blinkScaleY },
                            )
                        }
                        if (glassesActive) {
                            KodamaFeatureCanvas(
                                shape = accessoryShape,
                                modifier = Modifier.fillMaxWidth(1.10f),
                            )
                        }
                    }

                    if (showMouth) {
                        Spacer(Modifier.height(avatarSize * if (glassesActive) 0.08f else 0.04f))
                        KodamaFeatureCanvas(
                            shape = kodamaMouthShape(resolved.slots.mouth),
                            modifier = Modifier.fillMaxWidth(0.35f),
                        )
                    }
                }

                val cheekShape = if (showCheeks) kodamaCheekShape(resolved.slots.cheeks) else null
                if (cheekShape != null) {
                    KodamaFeatureCanvas(
                        shape = cheekShape,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = avatarSize * 0.18f)
                            .fillMaxWidth(0.90f),
                    )
                }
            }
        }
    }
}

@Composable
private fun KodamaFeatureCanvas(shape: KodamaVectorShape, modifier: Modifier = Modifier) {
    val parsedPaths = remember(shape) {
        shape.paths.map { it to PathParser().parsePathString(it.d).toPath() }
    }
    val parsedStrokes = remember(shape) {
        shape.strokes.map { it to PathParser().parsePathString(it.d).toPath() }
    }
    Canvas(modifier.aspectRatio(shape.viewBoxWidth / shape.viewBoxHeight)) {
        val scale = size.width / shape.viewBoxWidth
        withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
            for ((spec, path) in parsedPaths) {
                val base = spec.fill ?: Color.Black
                drawPath(path = path, color = base.copy(alpha = spec.alpha))
            }
            for ((spec, path) in parsedStrokes) {
                drawPath(
                    path = path,
                    color = Color.Black,
                    style = Stroke(width = spec.strokeWidth, cap = spec.cap),
                )
            }
            for (circle in shape.circles) {
                val center = Offset(circle.cx, circle.cy)
                if (circle.stroked) {
                    drawCircle(
                        color = Color.Black.copy(alpha = circle.alpha),
                        radius = circle.r,
                        center = center,
                        style = Stroke(width = circle.strokeWidth),
                    )
                } else {
                    val base = circle.fill ?: Color.Black
                    drawCircle(
                        color = base.copy(alpha = circle.alpha),
                        radius = circle.r,
                        center = center,
                    )
                }
            }
            for (ellipse in shape.ellipses) {
                drawOval(
                    color = Color.Black,
                    topLeft = Offset(ellipse.cx - ellipse.rx, ellipse.cy - ellipse.ry),
                    size = Size(ellipse.rx * 2f, ellipse.ry * 2f),
                    style = Stroke(width = ellipse.strokeWidth),
                )
            }
            for (rect in shape.rects) {
                drawRoundRect(
                    color = Color.Black.copy(alpha = rect.alpha),
                    topLeft = Offset(rect.x, rect.y),
                    size = Size(rect.width, rect.height),
                    cornerRadius = CornerRadius(rect.rx, rect.rx),
                )
            }
        }
    }
}
