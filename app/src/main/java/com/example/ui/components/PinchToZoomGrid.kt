package com.example.ui.components

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Enables two-finger pinch-to-zoom to dynamically adjust grid column density
 * from 3 columns (zoomed in / detailed) up to 6 columns (zoomed out / compact).
 */
fun Modifier.pinchToZoomGrid(
    currentColumns: Int,
    minColumns: Int = 3,
    maxColumns: Int = 6,
    onColumnsChange: (Int) -> Unit
): Modifier = composed {
    var accumulatedScale by remember { mutableFloatStateOf(1f) }

    pointerInput(currentColumns, minColumns, maxColumns) {
        detectTransformGestures { _, _, zoom, _ ->
            if (zoom != 1f) {
                accumulatedScale *= zoom

                // Pinch in (two fingers spread apart -> zoom in -> fewer columns, min 3)
                if (accumulatedScale > 1.25f && currentColumns > minColumns) {
                    onColumnsChange((currentColumns - 1).coerceAtLeast(minColumns))
                    accumulatedScale = 1f
                }
                // Pinch out (two fingers move together -> zoom out -> more columns, max 6)
                else if (accumulatedScale < 0.80f && currentColumns < maxColumns) {
                    onColumnsChange((currentColumns + 1).coerceAtMost(maxColumns))
                    accumulatedScale = 1f
                }
            }
        }
    }
}
