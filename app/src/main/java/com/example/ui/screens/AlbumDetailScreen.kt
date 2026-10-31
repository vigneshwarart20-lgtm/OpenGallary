package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.data.model.AlbumItem
import com.example.data.model.DateGroup
import com.example.data.model.MediaItem
import com.example.ui.components.DraggableFastScroll
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MediaGridItem
import com.example.ui.components.pinchToZoomGrid
import com.example.viewmodel.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    album: AlbumItem,
    mediaList: List<MediaItem>,
    dateGroups: List<DateGroup>,
    viewModel: GalleryViewModel,
    isSelectionMode: Boolean,
    selectedUris: Set<android.net.Uri>,
    gridColumns: Int,
    onBack: () -> Unit,
    onOpenMedia: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = album.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${mediaList.size} items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("album_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            if (isSelectionMode) {
                                viewModel.clearSelection()
                            } else {
                                viewModel.isSelectionMode.value = true
                            }
                        },
                        modifier = Modifier.testTag("album_select_button")
                    ) {
                        Text(
                            text = if (isSelectionMode) "Done" else "Select",
                            color = com.example.ui.theme.iOSBlue,
                            fontSize = 17.sp,
                            fontWeight = if (isSelectionMode) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        val isRefreshing by viewModel.isRefreshing.collectAsState()

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshMedia() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (mediaList.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.PhotoLibrary,
                    title = "Album is Empty",
                    description = "There are no photos or videos in this album. Pull down to refresh.",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val effectiveColumns = gridColumns.coerceIn(3, 6)
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
                        contentPadding = PaddingValues(start = 2.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("album_media_grid")
                    ) {
                        for (group in dateGroups) {
                            item(
                                key = "header_${group.title}_${group.timestamp}",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = group.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            items(group.items, key = { it.id }) { item ->
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

                    DraggableFastScroll(
                        gridState = gridState,
                        totalItems = gridEntries.size,
                        getDateLabelForIndex = { index ->
                            if (index in gridEntries.indices) {
                                gridEntries[index].headerTitle
                            } else ""
                        }
                    )
                }
            }
        }
    }
}
