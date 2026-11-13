package moe.tnxg.koma.ui.component.media

import androidx.compose.runtime.staticCompositionLocalOf
import moe.tnxg.koma.platform.ImageServices

/**
 * Photo picking and the local cover cache. Null on a platform without them, which keeps the
 * profile on generated artwork instead of failing.
 */
val LocalImageServices = staticCompositionLocalOf<ImageServices?> { null }
