package moe.tnxg.koma.ui.platform

import androidx.compose.runtime.Composable

/** Opens a document and reads it as UTF-8. Cancellation does not call [onResult]. */
@Composable
expect fun rememberJsonFilePicker(onResult: (Result<String>) -> Unit): () -> Unit
