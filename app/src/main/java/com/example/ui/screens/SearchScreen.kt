package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.MediaItem
import com.example.ui.components.DraggableFastScroll
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MediaGridItem
import com.example.viewmodel.GalleryViewModel
import com.example.viewmodel.SearchFilterType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchQuery: String,
    filterType: SearchFilterType,
    searchResults: List<MediaItem>,
    viewModel: GalleryViewModel,
    gridColumns: Int,
    onBack: () -> Unit,
    onOpenMedia: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val gridState = rememberLazyGridState()

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchQuery.value = it },
                            placeholder = { Text("Photos, Albums, Dates...") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = com.example.ui.theme.iOSGray)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.searchQuery.value = "" },
                                        modifier = Modifier.testTag("clear_search_button")
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = com.example.ui.theme.iOSGray)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_text_input")
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("search_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )

                // Category Filter Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = filterType == SearchFilterType.ALL,
                            onClick = { viewModel.searchFilterType.value = SearchFilterType.ALL },
                            label = { Text("All Media") },
                            modifier = Modifier.testTag("filter_chip_all")
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterType == SearchFilterType.PHOTOS_ONLY,
                            onClick = { viewModel.searchFilterType.value = SearchFilterType.PHOTOS_ONLY },
                            label = { Text("Photos") },
                            modifier = Modifier.testTag("filter_chip_photos")
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterType == SearchFilterType.VIDEOS_ONLY,
                            onClick = { viewModel.searchFilterType.value = SearchFilterType.VIDEOS_ONLY },
                            label = { Text("Videos") },
                            modifier = Modifier.testTag("filter_chip_videos")
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterType == SearchFilterType.FAVORITES_ONLY,
                            onClick = { viewModel.searchFilterType.value = SearchFilterType.FAVORITES_ONLY },
                            label = { Text("Favorites") },
                            modifier = Modifier.testTag("filter_chip_favorites")
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (searchResults.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.SearchOff,
                title = "No Matches Found",
                description = "Try searching for another filename, folder name (e.g. Camera, WhatsApp), or month.",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    state = gridState,
                    contentPadding = PaddingValues(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("search_results_grid")
                ) {
                    items(searchResults, key = { it.id }) { item ->
                        MediaGridItem(
                            item = item,
                            isSelected = false,
                            isSelectionMode = false,
                            onClick = { onOpenMedia(item) },
                            onLongClick = {}
                        )
                    }
                }

                DraggableFastScroll(
                    gridState = gridState,
                    totalItems = searchResults.size,
                    getDateLabelForIndex = { index ->
                        if (index in searchResults.indices) searchResults[index].bucketName else ""
                    },
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}
