package moe.tnxg.koma.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Reserves a centered band no taller than [limit] for the app's own drag: the system edge-back
 * gesture otherwise swallows horizontal swipes that start on an edge-docked control.
 */
@Composable
expect fun Modifier.gestureExclusionBand(limit: Dp): Modifier
