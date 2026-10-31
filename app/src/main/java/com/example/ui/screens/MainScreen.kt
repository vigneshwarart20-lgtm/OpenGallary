package com.example.ui.screens

import android.app.Activity
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.ui.theme.iOSBlue
import com.example.ui.theme.iOSGray
import com.example.ui.theme.iOSRed
import com.example.viewmodel.GalleryViewModel
import kotlinx.coroutines.launch

enum class MainTab(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    LIBRARY("Library", Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    ALBUMS("Albums", Icons.Outlined.Folder, Icons.Outlined.Folder),
    VIDEOS("Videos", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary),
    FAVORITES("Favorites", Icons.Filled.Favorite, Icons.Default.FavoriteBorder)
}

enum class ActiveScreen {
    MAIN,
    SETTINGS,
    ALBUM_DETAIL,
    VIEWER,
    EDITOR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: GalleryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by remember { mutableStateOf(MainTab.LIBRARY) }
    var activeScreen by remember { mutableStateOf(ActiveScreen.MAIN) }

    // Observers from ViewModel
    val allMedia by viewModel.allMedia.collectAsState()
    val dateGroups by viewModel.dateGroups.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val videos by viewModel.videosList.collectAsState()
    val favorites by viewModel.favoritesList.collectAsState()
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()
    val selectedAlbumMedia by viewModel.selectedAlbumMedia.collectAsState()
    val selectedAlbumDateGroups by viewModel.selectedAlbumDateGroups.collectAsState()

    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedUris by viewModel.selectedUris.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchFilterType by viewModel.searchFilterType.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    val viewerItem by viewModel.viewerItem.collectAsState()
    val viewerList by viewModel.viewerList.collectAsState()
    val editingItem by viewModel.editingItem.collectAsState()

    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    val batchDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.clearSelection()
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Selected items deleted")
            }
        }
    }

    if (activeScreen == ActiveScreen.MAIN && isSelectionMode) {
        BackHandler { viewModel.clearSelection() }
    }

    when (activeScreen) {
        ActiveScreen.VIEWER -> {
            if (viewerItem != null) {
                MediaViewerScreen(
                    initialItem = viewerItem!!,
                    mediaList = viewerList.ifEmpty { allMedia },
                    viewModel = viewModel,
                    onBack = {
                        viewModel.closeViewer()
                        activeScreen = if (selectedAlbum != null) ActiveScreen.ALBUM_DETAIL else ActiveScreen.MAIN
                    },
                    onEditPhoto = { photo ->
                        viewModel.startEditing(photo)
                        activeScreen = ActiveScreen.EDITOR
                    }
                )
            }
        }

        ActiveScreen.EDITOR -> {
            if (editingItem != null) {
                PhotoEditorScreen(
                    mediaItem = editingItem!!,
                    onCancel = {
                        viewModel.finishEditing()
                        activeScreen = ActiveScreen.VIEWER
                    },
                    onSaved = {
                        viewModel.finishEditing()
                        activeScreen = ActiveScreen.MAIN
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Edited photo saved")
                        }
                    }
                )
            }
        }

        ActiveScreen.SETTINGS -> {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { activeScreen = ActiveScreen.MAIN }
            )
        }

        ActiveScreen.ALBUM_DETAIL -> {
            if (selectedAlbum != null) {
                AlbumDetailScreen(
                    album = selectedAlbum!!,
                    mediaList = selectedAlbumMedia,
                    dateGroups = selectedAlbumDateGroups,
                    viewModel = viewModel,
                    isSelectionMode = isSelectionMode,
                    selectedUris = selectedUris,
                    gridColumns = gridColumns,
                    onBack = {
                        viewModel.selectedAlbum.value = null
                        activeScreen = ActiveScreen.MAIN
                    },
                    onOpenMedia = { item ->
                        viewModel.openViewer(item, selectedAlbumMedia)
                        activeScreen = ActiveScreen.VIEWER
                    }
                )
            }
        }

        ActiveScreen.MAIN -> {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            if (isSelectionMode) {
                                Text(
                                    text = if (selectedUris.isEmpty()) "Select Items" else "${selectedUris.size} Selected",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    text = currentTab.label,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.5).sp
                                    )
                                )
                            }
                        },
                        actions = {
                            if (isSelectionMode) {
                                // Select All
                                TextButton(
                                    onClick = {
                                        val currentList = when (currentTab) {
                                            MainTab.LIBRARY -> allMedia
                                            MainTab.ALBUMS -> allMedia
                                            MainTab.VIDEOS -> videos
                                            MainTab.FAVORITES -> favorites
                                        }
                                        viewModel.selectAll(currentList)
                                    },
                                    modifier = Modifier.testTag("selection_select_all_button")
                                ) {
                                    Text("Select All", color = iOSBlue, fontWeight = FontWeight.Normal)
                                }

                                // Cancel / Done button in iOS Blue
                                TextButton(
                                    onClick = { viewModel.clearSelection() },
                                    modifier = Modifier.testTag("selection_close_button")
                                ) {
                                    Text("Done", color = iOSBlue, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                // Apple Photos "Select" text button
                                TextButton(
                                    onClick = { viewModel.isSelectionMode.value = true },
                                    modifier = Modifier.testTag("main_select_button")
                                ) {
                                    Text("Select", color = iOSBlue, fontSize = 17.sp, fontWeight = FontWeight.Normal)
                                }

                                IconButton(
                                    onClick = { activeScreen = ActiveScreen.SETTINGS },
                                    modifier = Modifier.testTag("main_settings_button")
                                ) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        tonalElevation = 0.dp
                    ) {
                        Column {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                thickness = 0.5.dp
                            )

                            if (isSelectionMode) {
                                // Apple Photos Bottom Selection Action Bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(horizontal = 24.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Share
                                    IconButton(
                                        onClick = { shareMedia(context, selectedUris.toList()) },
                                        enabled = selectedUris.isNotEmpty(),
                                        modifier = Modifier.testTag("selection_share_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share",
                                            tint = if (selectedUris.isNotEmpty()) iOSBlue else iOSGray
                                        )
                                    }

                                    // Favorite
                                    IconButton(
                                        onClick = { viewModel.favoriteSelected(true) },
                                        enabled = selectedUris.isNotEmpty(),
                                        modifier = Modifier.testTag("selection_favorite_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (selectedUris.isNotEmpty()) iOSBlue else iOSGray
                                        )
                                    }

                                    // Delete
                                    IconButton(
                                        onClick = { showBatchDeleteConfirm = true },
                                        enabled = selectedUris.isNotEmpty(),
                                        modifier = Modifier.testTag("selection_delete_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = if (selectedUris.isNotEmpty()) iOSRed else iOSGray
                                        )
                                    }
                                }
                            } else {
                                // Apple Photos Translucent Tab Bar with proper insets and non-cropped labels
                                NavigationBar(
                                    containerColor = Color.Transparent,
                                    tonalElevation = 0.dp,
                                    windowInsets = NavigationBarDefaults.windowInsets
                                ) {
                                    MainTab.values().forEach { tab ->
                                        val selected = currentTab == tab
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = {
                                                currentTab = tab
                                                viewModel.clearSelection()
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.label,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = tab.label,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                                    letterSpacing = (-0.2).sp
                                                )
                                            },
                                            alwaysShowLabel = true,
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = iOSBlue,
                                                selectedTextColor = iOSBlue,
                                                unselectedIconColor = iOSGray,
                                                unselectedTextColor = iOSGray,
                                                indicatorColor = Color.Transparent
                                            ),
                                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = modifier.fillMaxSize()
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        MainTab.LIBRARY -> {
                            TimelineScreen(
                                dateGroups = dateGroups,
                                allMedia = allMedia,
                                viewModel = viewModel,
                                isSelectionMode = isSelectionMode,
                                selectedUris = selectedUris,
                                gridColumns = gridColumns,
                                onOpenMedia = { item ->
                                    viewModel.openViewer(item, allMedia)
                                    activeScreen = ActiveScreen.VIEWER
                                }
                            )
                        }

                        MainTab.ALBUMS -> {
                            AlbumsScreen(
                                albums = albums,
                                viewModel = viewModel,
                                onSelectAlbum = { album ->
                                    viewModel.selectedAlbum.value = album
                                    activeScreen = ActiveScreen.ALBUM_DETAIL
                                }
                            )
                        }

                        MainTab.VIDEOS -> {
                            VideosScreen(
                                videos = videos,
                                viewModel = viewModel,
                                isSelectionMode = isSelectionMode,
                                selectedUris = selectedUris,
                                gridColumns = gridColumns,
                                onOpenVideo = { video ->
                                    viewModel.openViewer(video, videos)
                                    activeScreen = ActiveScreen.VIEWER
                                }
                            )
                        }

                        MainTab.FAVORITES -> {
                            FavoritesScreen(
                                favorites = favorites,
                                viewModel = viewModel,
                                isSelectionMode = isSelectionMode,
                                selectedUris = selectedUris,
                                gridColumns = gridColumns,
                                onOpenMedia = { item ->
                                    viewModel.openViewer(item, favorites)
                                    activeScreen = ActiveScreen.VIEWER
                                }
                            )
                        }
                    }
                }
            }

            // Batch Delete Confirmation Dialog
            if (showBatchDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showBatchDeleteConfirm = false },
                    title = { Text("Delete ${selectedUris.size} Items?") },
                    text = { Text("These items will be permanently deleted from your device storage.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showBatchDeleteConfirm = false
                                coroutineScope.launch {
                                    val uris = selectedUris.toList()
                                    val pendingIntent = viewModel.repository.createDeletePendingIntent(uris)
                                    if (pendingIntent != null) {
                                        val req = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                        batchDeleteLauncher.launch(req)
                                    } else {
                                        viewModel.repository.deleteMediaLegacy(uris)
                                        viewModel.clearSelection()
                                        snackbarHostState.showSnackbar("Deleted ${uris.size} items")
                                    }
                                }
                            },
                            modifier = Modifier.testTag("confirm_batch_delete_button")
                        ) {
                            Text("Delete", color = iOSRed, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBatchDeleteConfirm = false }) {
                            Text("Cancel", color = iOSBlue)
                        }
                    }
                )
            }
        }
    }
}
