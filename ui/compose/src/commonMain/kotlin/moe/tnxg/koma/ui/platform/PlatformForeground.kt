package moe.tnxg.koma.ui.platform

import androidx.compose.runtime.Composable

/** Fires once on attachment and whenever this app returns to the foreground. */
@Composable
expect fun PlatformForeground(onForeground: () -> Unit)
