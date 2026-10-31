package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
fun FavoritesScreen(
    favorites: List<MediaItem>,
    viewModel: GalleryViewModel,
    isSelectionMode: Boolean,
    selectedUris: Set<android.net.Uri>,
    gridColumns: Int,
    onOpenMedia: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    if (favorites.isEmpty()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshMedia() },
            modifier = modifier.fillMaxSize()
        ) {
            EmptyStateView(
                icon = Icons.Default.FavoriteBorder,
                title = "No Favorites Yet",
                description = "Tap the heart icon on any photo or video to add it to your favorites. Pull down to refresh.",
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    val gridState = rememberLazyGridState()
    val dayFormat = remember { SimpleDateFormat("d MMMM yyyy", Locale.getDefault()) }

    val indexedTitles = remember(favorites) {
        favorites.map { item ->
            val ts = if (item.dateModified > 0) item.dateModified * 1000L else item.dateAdded * 1000L
            dayFormat.format(Date(ts))
        }
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
                contentPadding = PaddingValues(start = 2.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("favorites_grid")
            ) {
                items(favorites, key = { it.id }) { item ->
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

            DraggableFastScroll(
                gridState = gridState,
                totalItems = favorites.size,
                getDateLabelForIndex = { index ->
                    if (index in indexedTitles.indices) indexedTitles[index] else ""
                }
            )
        }
    }
}
