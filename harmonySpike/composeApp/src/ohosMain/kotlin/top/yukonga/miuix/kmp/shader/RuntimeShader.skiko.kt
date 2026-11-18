/*
 * 补丁：基于 miuix-shader 0.9.4 sources（Apache-2.0，https://github.com/compose-miuix-ui/miuix）修改，
 * 适配 CMP 1.9.2 线：compose Shader 与 org.jetbrains.skia.Shader 为 typealias，移除 1.11+ 互操作 API。
 * 构建期由 downloadMiuixSources 任务下载原始源码，本文件覆盖其中的同名 skiko 实现。
 */
// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.shader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

// CMP 1.10 及更早：非 Android 平台的 compose Shader 是 org.jetbrains.skia.Shader 的 actual typealias，
// 因此无需 asComposeShader()/skiaShader() 互操作（那是 1.11.0 拆包后才引入的 API）。

actual fun isRuntimeShaderSupported(): Boolean = true

/**
 * Creates a platform-specific [RuntimeShader] from an AGSL/SkSL shader string.
 *
 * @param shaderString The SkSL shader source to compile.
 */
actual fun RuntimeShader(shaderString: String): RuntimeShader = SkikoRuntimeShader(RuntimeShaderBuilder(obtainRuntimeEffect(shaderString)))

// RuntimeEffect is immutable; cache to avoid recompiling SkSL per brush.
@OptIn(InternalCoroutinesApi::class)
private val effectCacheLock = SynchronizedObject()
private val effectCache = mutableMapOf<String, RuntimeEffect>()

@OptIn(InternalCoroutinesApi::class)
private fun obtainRuntimeEffect(shaderString: String): RuntimeEffect = synchronized(effectCacheLock) {
    effectCache.getOrPut(shaderString) { RuntimeEffect.makeForShader(shaderString) }
}

actual fun RuntimeShader.asComposeShader(): Shader = asSkikoRuntimeShader().makeShader()

actual fun RuntimeShader.asBrush(): ShaderBrush = ShaderBrush(asComposeShader())

/** Returns the underlying Skia [RuntimeShaderBuilder] for interop with native shader APIs. */
fun RuntimeShader.asSkikoRuntimeShader(): RuntimeShaderBuilder = (this as SkikoRuntimeShader).shader

private class SkikoRuntimeShader(val shader: RuntimeShaderBuilder) : RuntimeShader {

    override fun setFloatUniform(name: String, value: Float) {
        shader.uniform(name, value)
    }

    override fun setFloatUniform(name: String, value1: Float, value2: Float) {
        shader.uniform(name, value1, value2)
    }

    override fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float) {
        shader.uniform(name, value1, value2, value3)
    }

    override fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float, value4: Float) {
        shader.uniform(name, value1, value2, value3, value4)
    }

    override fun setFloatUniform(name: String, values: FloatArray) {
        shader.uniform(name, values)
    }

    override fun setIntUniform(name: String, value: Int) {
        shader.uniform(name, value)
    }

    override fun setIntUniform(name: String, value1: Int, value2: Int) {
        shader.uniform(name, value1, value2)
    }

    override fun setIntUniform(name: String, value1: Int, value2: Int, value3: Int) {
        shader.uniform(name, value1, value2, value3)
    }

    override fun setIntUniform(name: String, value1: Int, value2: Int, value3: Int, value4: Int) {
        shader.uniform(name, value1, value2, value3, value4)
    }

    override fun setIntUniform(name: String, values: IntArray) {
        val floats = FloatArray(values.size) { values[it].toFloat() }
        shader.uniform(name, floats)
    }

    override fun setColorUniform(name: String, color: Color) {
        val srgb = color.convert(ColorSpaces.Srgb)
        val a = srgb.alpha
        shader.uniform(name, srgb.red * a, srgb.green * a, srgb.blue * a, a)
    }

    override fun setInputShader(name: String, shader: Shader) {
        // CMP 1.10-：compose Shader 与 skia Shader 是同一类型（typealias），直接传给 child
        this.shader.child(name, shader)
    }
}
