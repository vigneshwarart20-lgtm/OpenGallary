package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.iOSBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun DraggableFastScroll(
    gridState: LazyGridState,
    totalItems: Int,
    getDateLabelForIndex: (Int) -> String,
    modifier: Modifier = Modifier,
    thumbHeightDp: Float = 50f
) {
    if (totalItems <= 10) return

    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var dragYOffset by remember { mutableFloatStateOf(0f) }
    var showThumbIdle by remember { mutableStateOf(false) }

    // Synchronize thumb position from grid state when not dragging
    val scrollProgress by remember(totalItems) {
        derivedStateOf {
            val firstVisible = gridState.firstVisibleItemIndex
            if (totalItems > 1) {
                (firstVisible.toFloat() / (totalItems - 1)).coerceIn(0f, 1f)
            } else 0f
        }
    }

    // Current date label to show in top center pill
    val currentDateLabel by remember(totalItems, scrollProgress, isDragging, dragYOffset) {
        derivedStateOf {
            val targetIndex = if (isDragging) {
                (dragYOffset * (totalItems - 1)).roundToInt().coerceIn(0, totalItems - 1)
            } else {
                gridState.firstVisibleItemIndex.coerceIn(0, totalItems - 1)
            }
            getDateLabelForIndex(targetIndex)
        }
    }

    // Auto fade thumb after scrolling stops
    LaunchedEffect(gridState.isScrollInProgress) {
        if (gridState.isScrollInProgress) {
            showThumbIdle = true
        } else if (!isDragging) {
            delay(1600)
            showThumbIdle = false
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val availableHeightPx = with(density) { maxHeight.toPx() }
        val thumbHeightPx = with(density) { thumbHeightDp.dp.toPx() }
        val maxTravelPx = (availableHeightPx - thumbHeightPx).coerceAtLeast(1f)

        // Compute current thumb Y offset in pixels
        val currentThumbYPx = if (isDragging) {
            (dragYOffset * maxTravelPx).coerceIn(0f, maxTravelPx)
        } else {
            (scrollProgress * maxTravelPx).coerceIn(0f, maxTravelPx)
        }

        val isVisible = isDragging || gridState.isScrollInProgress || showThumbIdle

        // 1. Floating Date Pill at Top Center of UI (Clean, unconstrained, elegant)
        AnimatedVisibility(
            visible = isVisible && currentDateLabel.isNotBlank(),
            enter = fadeIn() + slideInVertically { -it / 2 } + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + slideOutVertically { -it / 2 } + scaleOut(targetScale = 0.9f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shadowElevation = 8.dp,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .wrapContentWidth()
                    .testTag("fast_scroll_top_date_pill")
            ) {
                Text(
                    text = currentDateLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        letterSpacing = (-0.2).sp
                    ),
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp)
                )
            }
        }

        // 2. Right Track and Draggable Thumb
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(56.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(56.dp)
                    .pointerInput(totalItems, maxTravelPx) {
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                val clampedY = (offset.y - thumbHeightPx / 2).coerceIn(0f, maxTravelPx)
                                dragYOffset = clampedY / maxTravelPx
                                val targetIndex = (dragYOffset * (totalItems - 1))
                                    .roundToInt()
                                    .coerceIn(0, totalItems - 1)
                                coroutineScope.launch {
                                    gridState.scrollToItem(targetIndex)
                                }
                            },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onVerticalDrag = { change, _ ->
                                change.consume()
                                val clampedY = (change.position.y - thumbHeightPx / 2).coerceIn(0f, maxTravelPx)
                                dragYOffset = clampedY / maxTravelPx
                                val targetIndex = (dragYOffset * (totalItems - 1))
                                    .roundToInt()
                                    .coerceIn(0, totalItems - 1)
                                coroutineScope.launch {
                                    gridState.scrollToItem(targetIndex)
                                }
                            }
                        )
                    }
            ) {
                // Subtle right guide line
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp)
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            shape = CircleShape
                        )
                )

                // Draggable thumb indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset { IntOffset(0, currentThumbYPx.roundToInt()) }
                        .padding(end = 2.dp)
                        .size(width = 40.dp, height = thumbHeightDp.dp)
                        .testTag("fast_scroll_thumb"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .size(width = 28.dp, height = 44.dp)
                            .shadow(if (isDragging) 8.dp else 3.dp, shape = RoundedCornerShape(14.dp))
                            .background(
                                color = if (isDragging) iOSBlue else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.UnfoldMore,
                            contentDescription = "Scroll grip",
                            tint = if (isDragging) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(22.dp)
                                .align(Alignment.CenterVertically)
                                .padding(horizontal = 3.dp)
                        )
                    }
                }
            }
        }
    }
}
