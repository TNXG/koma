package moe.tnxg.koma.ui.platform

import androidx.compose.runtime.Composable

/** OHOS 端文件选择器尚未接入（SAF 类能力需要 NAPI 桥接），M1 返回空启动器。 */
@Composable
actual fun rememberJsonFilePicker(onResult: (Result<String>) -> Unit): () -> Unit = {}
