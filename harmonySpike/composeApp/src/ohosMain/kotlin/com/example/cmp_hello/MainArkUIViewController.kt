package com.example.cmp_hello

import androidx.compose.ui.window.ComposeArkUIViewController
import kotlinx.coroutines.initMainHandler
import moe.tnxg.koma.app.KomaApp
import moe.tnxg.koma.spike.OhosPlatformServices
import moe.tnxg.koma.spike.OhosPaths
import moe.tnxg.koma.spike.OhosCaptchaBridge
import moe.tnxg.koma.spike.OhosPhotoPickerBridge
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.toKString
import platform.ArkTS.ArkTS_Napi_NativeModule.napi_env
import platform.ArkTS.ArkTS_Napi_NativeModule.napi_value
import kotlin.experimental.ExperimentalNativeApi
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("MainArkUIViewController")
fun MainArkUIViewController(env: napi_env, filesDir: CPointer<ByteVar>, recognize: napi_value, pickPhoto: napi_value): napi_value {
    OhosPaths.initialize(filesDir.toKString())
    initMainHandler(env)
    OhosCaptchaBridge.initialize(env, recognize)
    OhosPhotoPickerBridge.initialize(env, pickPhoto)
    // KomaApp keys its session to this identity; configuration/resume recomposition must not replace it.
    val platform = OhosPlatformServices()
    return ComposeArkUIViewController(env) {
        KomaApp(platform)
    }
}
