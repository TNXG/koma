package com.example.cmp_hello

import androidx.compose.ui.window.ComposeArkUIViewController
import kotlinx.coroutines.initMainHandler
import moe.tnxg.koma.app.KomaApp
import moe.tnxg.koma.spike.OhosPlatformServices
import platform.ArkTS.ArkTS_Napi_NativeModule.napi_env
import platform.ArkTS.ArkTS_Napi_NativeModule.napi_value
import kotlin.experimental.ExperimentalNativeApi
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("MainArkUIViewController")
fun MainArkUIViewController(env: napi_env): napi_value {
    initMainHandler(env)
    return ComposeArkUIViewController(env) {
        KomaApp(OhosPlatformServices())
    }
}
