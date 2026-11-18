/*
 * 补丁：基于 miuix-blur 0.9.4 sources（Apache-2.0，https://github.com/compose-miuix-ui/miuix）修改，
 * 适配 CMP 1.9.2 线：无 RenderEffect.skiaImageFilter()，改用旁路注册表串联 skia ImageFilter。
 * 构建期由 downloadMiuixSources 任务下载原始源码，本文件覆盖其中的同名 skiko 实现。
 */
// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
//
// OHOS (CMP 1.9.2) 适配版：CMP 1.11.0 才引入 RenderEffect.skiaImageFilter /
// Shader.asComposeShader() 等互操作 API，1.9.2 上不存在。
// 本库产生的全部 compose RenderEffect 都出自本文件的工厂函数，
// 因此用旁路注册表维护 compose RenderEffect → skia ImageFilter 映射，
// chain() 直接在 skia 侧串联，无需从 compose 侧反向提取。

package top.yukonga.miuix.kmp.blur.internal

import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import top.yukonga.miuix.kmp.blur.BackdropEffectScopeImpl
import top.yukonga.miuix.kmp.shader.RuntimeShader
import top.yukonga.miuix.kmp.shader.asSkikoRuntimeShader

private val skiaFilters = HashMap<RenderEffect, ImageFilter>()

private fun ImageFilter.asRegisteredComposeRenderEffect(): RenderEffect =
    asComposeRenderEffect().also { skiaFilters[it] = this }

internal actual fun RenderEffect?.chain(other: RenderEffect): RenderEffect = if (this != null) {
    ImageFilter.makeCompose(
        skiaFilters.getValue(other),
        skiaFilters.getValue(this),
    ).asRegisteredComposeRenderEffect()
} else {
    other
}

internal actual fun runtimeShaderEffect(
    runtimeShader: RuntimeShader,
    uniformShaderName: String,
): RenderEffect = ImageFilter.makeRuntimeShader(
    runtimeShader.asSkikoRuntimeShader(),
    uniformShaderName,
    null,
).asRegisteredComposeRenderEffect()

internal actual fun progressiveStackEffect(
    scope: BackdropEffectScopeImpl,
    sigma0X: Float,
    sigma0Y: Float,
    sigma1X: Float,
    sigma1Y: Float,
    sigma2X: Float,
    sigma2Y: Float,
    ax: Float,
    ay: Float,
    projFull: Float,
    projZero: Float,
    curve: Float,
    preEffect: RenderEffect?,
    postEffect: RenderEffect?,
    nativeSharpEnd: Boolean,
): RenderEffect? {
    val shader = scope.obtainRuntimeShader("ProgComposite", PROGRESSIVE_COMPOSITE_SHADER).apply {
        setFloatUniform("in_gradAxis", ax, ay)
        setFloatUniform("in_gradBand", projFull, projZero)
        setFloatUniform("in_curve", curve)
    }
    val pre = preEffect?.let { skiaFilters[it] }

    // Each level is pre → blur; null = the (pre-chained) source itself.
    fun level(sigmaX: Float, sigmaY: Float): ImageFilter? = if (sigmaX <= 0f && sigmaY <= 0f) {
        pre
    } else {
        ImageFilter.makeBlur(sigmaX, sigmaY, FilterTileMode.CLAMP, pre, null)
    }

    // clearChild := blur2 by default, degenerating the last mix segment (the sharp end is the
    // caller's full-res overlay); with nativeSharpEnd the (pre-chained) source binds instead —
    // a null child samples the source. Skia evaluates the shared blur2 instance once.
    val blur2 = level(sigma2X, sigma2Y)
    var filter = ImageFilter.makeRuntimeShader(
        shader.asSkikoRuntimeShader(),
        arrayOf("clearChild", "blur0", "blur1", "blur2"),
        arrayOf(
            if (nativeSharpEnd) pre else blur2,
            level(sigma0X, sigma0Y),
            level(sigma1X, sigma1Y),
            blur2,
        ),
    )
    if (postEffect != null) {
        val maskShader = scope.obtainRuntimeShader("ProgLevelMask", PROGRESSIVE_LEVEL_MASK_SHADER).apply {
            setFloatUniform("in_gradAxis", ax, ay)
            setFloatUniform("in_gradBand", projFull, projZero)
            setFloatUniform("in_curve", curve)
            setFloatUniform("in_level", 1f)
            setFloatUniform("in_slope", 1f)
        }
        val postBranch = ImageFilter.makeRuntimeShader(
            maskShader.asSkikoRuntimeShader(),
            "child",
            ImageFilter.makeCompose(skiaFilters.getValue(postEffect), filter),
        )
        filter = ImageFilter.makeBlend(BlendMode.SRC_OVER, filter, postBranch, null)
    }
    return filter.asRegisteredComposeRenderEffect()
}
