package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.ui.components.DraggableFastScroll
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MediaGridItem
import com.example.ui.components.pinchToZoomGrid
import com.example.viewmodel.GalleryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    videos: List<MediaItem>,
    viewModel: GalleryViewModel,
    isSelectionMode: Boolean,
    selectedUris: Set<android.net.Uri>,
    gridColumns: Int,
    onOpenVideo: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    if (videos.isEmpty()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshMedia() },
            modifier = modifier.fillMaxSize()
        ) {
            EmptyStateView(
                icon = Icons.Default.VideoLibrary,
                title = "No Videos",
                description = "Videos on your device will appear here. Pull down to refresh.",
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    val gridState = rememberLazyGridState()
    val dayFormat = remember { SimpleDateFormat("d MMMM yyyy", Locale.getDefault()) }

    // Group videos date-wise
    val videoGroups = remember(videos) {
        videos.groupBy { video ->
            val ts = if (video.dateModified > 0) video.dateModified * 1000L else video.dateAdded * 1000L
            dayFormat.format(Date(ts))
        }
    }

    val indexedTitles = remember(videoGroups) {
        val list = mutableListOf<String>()
        for ((day, items) in videoGroups) {
            list.add(day)
            for (i in items.indices) {
                list.add(day)
            }
        }
        list
    }

    val effectiveColumns = gridColumns.coerceIn(3, 6)

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
                contentPadding = PaddingValues(start = 2.dp, end = 16.dp, top = 2.dp, bottom = 100.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("videos_grid")
            ) {
                for ((day, groupVideos) in videoGroups) {
                    item(
                        key = "header_$day",
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    letterSpacing = (-0.3).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${groupVideos.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    items(groupVideos, key = { it.id }) { video ->
                        val isSelected = selectedUris.contains(video.uri)
                        MediaGridItem(
                            item = video,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleSelection(video)
                                } else {
                                    onOpenVideo(video)
                                }
                            },
                            onLongClick = {
                                viewModel.startSelectionWith(video)
                            }
                        )
                    }
                }
            }

            DraggableFastScroll(
                gridState = gridState,
                totalItems = indexedTitles.size,
                getDateLabelForIndex = { index ->
                    if (index in indexedTitles.indices) {
                        indexedTitles[index]
                    } else ""
                }
            )
        }
    }
}
