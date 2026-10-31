package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DateGroup
import com.example.data.model.MediaItem
import com.example.ui.components.DraggableFastScroll
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MediaGridItem
import com.example.ui.components.pinchToZoomGrid
import com.example.viewmodel.GalleryViewModel

enum class ApplePhotosTimelineSegment(val label: String) {
    DAYS("Days"),
    MONTHS("Months"),
    YEARS("Years"),
    ALL("All Photos")
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    dateGroups: List<DateGroup>,
    allMedia: List<MediaItem>,
    viewModel: GalleryViewModel,
    isSelectionMode: Boolean,
    selectedUris: Set<android.net.Uri>,
    gridColumns: Int,
    onOpenMedia: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    if (allMedia.isEmpty()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshMedia() },
            modifier = modifier.fillMaxSize()
        ) {
            EmptyStateView(
                icon = Icons.Default.PhotoLibrary,
                title = "No Photos",
                description = "Photos and videos on your device will appear here. Pull down to refresh.",
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    // Default to DAYS for date-wise grouping
    var selectedSegment by remember { mutableStateOf(ApplePhotosTimelineSegment.DAYS) }
    val effectiveColumns = when (selectedSegment) {
        ApplePhotosTimelineSegment.YEARS -> 3
        ApplePhotosTimelineSegment.MONTHS -> 3
        ApplePhotosTimelineSegment.DAYS -> gridColumns.coerceIn(3, 6)
        ApplePhotosTimelineSegment.ALL -> gridColumns.coerceIn(3, 6)
    }

    val gridState = rememberLazyGridState()

    data class GridEntry(
        val isHeader: Boolean,
        val headerTitle: String = "",
        val item: MediaItem? = null
    )

    val gridEntries = remember(dateGroups) {
        val list = mutableListOf<GridEntry>()
        for (group in dateGroups) {
            list.add(GridEntry(isHeader = true, headerTitle = group.title))
            for (media in group.items) {
                list.add(GridEntry(isHeader = false, headerTitle = group.title, item = media))
            }
        }
        list
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshMedia() },
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pinchToZoomGrid(effectiveColumns, minColumns = 3, maxColumns = 6) { newCols ->
                    viewModel.gridColumns.value = newCols
                }
        ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(effectiveColumns),
            state = gridState,
            contentPadding = PaddingValues(start = 2.dp, end = 16.dp, top = 2.dp, bottom = 120.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("timeline_media_grid")
        ) {
            for (group in dateGroups) {
                // Section Header: Date-wise header
                item(
                    key = "header_${group.title}_${group.timestamp}",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = group.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = (-0.3).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                items(
                    items = group.items,
                    key = { it.id }
                ) { item ->
                    val isSelected = selectedUris.contains(item.uri)
                    MediaGridItem(
                        item = item,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                viewModel.toggleSelection(item)
                            } else {
                                onOpenMedia(item)
                            }
                        },
                        onLongClick = {
                            viewModel.startSelectionWith(item)
                        }
                    )
                }
            }
        }

        // Draggable fast scrollbar with top center date indicator
        DraggableFastScroll(
            gridState = gridState,
            totalItems = gridEntries.size,
            getDateLabelForIndex = { index ->
                if (index in gridEntries.indices) {
                    gridEntries[index].headerTitle
                } else ""
            }
        )

        // Floating Apple Photos Segmented Control Pill ("Days | Months | Years | All")
        if (!isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.clip(RoundedCornerShape(24.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ApplePhotosTimelineSegment.values().forEach { segment ->
                            val isSelected = selectedSegment == segment
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                    .clickable { selectedSegment = segment }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = segment.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

