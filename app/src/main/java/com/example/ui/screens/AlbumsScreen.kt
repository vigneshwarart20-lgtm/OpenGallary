package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AlbumItem
import com.example.data.model.AlbumType
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.iOSBlue
import com.example.viewmodel.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    albums: List<AlbumItem>,
    onSelectAlbum: (AlbumItem) -> Unit,
    viewModel: GalleryViewModel? = null,
    modifier: Modifier = Modifier
) {
    val isRefreshing = viewModel?.isRefreshing?.collectAsState()?.value ?: false

    if (albums.isEmpty()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel?.refreshMedia() },
            modifier = modifier.fillMaxSize()
        ) {
            EmptyStateView(
                icon = Icons.Default.FolderCopy,
                title = "No Albums",
                description = "Albums will appear here when you have photos or folders. Pull down to refresh.",
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    // Partition albums Apple Photos style: My Albums vs Media Types
    val mediaTypeTypes = setOf(
        AlbumType.ALL_VIDEOS,
        AlbumType.SCREENSHOTS,
        AlbumType.SCREEN_RECORDINGS,
        AlbumType.DOWNLOADS,
        AlbumType.WHATSAPP_VIDEOS
    )

    val myAlbums = albums.filter { it.type !in mediaTypeTypes }
    val mediaTypes = albums.filter { it.type in mediaTypeTypes }

    val utilityAlbums = albums.filter {
        it.type == AlbumType.FAVORITES || it.type == AlbumType.LARGE_FILES || it.type == AlbumType.EDITED
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel?.refreshMedia() },
        modifier = modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 110.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("albums_screen_list")
        ) {
            // Section 1: My Albums Header
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "My Albums",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                )
            }

            // 2-Column Vertical Grid items of My Albums (scrolling up and down)
            items(myAlbums, key = { it.id }) { album ->
                AppleAlbumTile(
                    album = album,
                    onClick = { onSelectAlbum(album) }
                )
            }

            // Section 2: Media Types (Apple Photos List Style spanning full width)
            if (mediaTypes.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Media Types",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                }

                items(mediaTypes, key = { it.id }, span = { GridItemSpan(maxLineSpan) }) { album ->
                    AppleMediaTypeRow(
                        album = album,
                        onClick = { onSelectAlbum(album) }
                    )
                }
            }

            // Section 3: Utilities (Favorites, Large Files, Edited spanning full width)
            if (utilityAlbums.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Utilities",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                }

                items(utilityAlbums, key = { "util_${it.id}" }, span = { GridItemSpan(maxLineSpan) }) { album ->
                    AppleMediaTypeRow(
                        album = album,
                        onClick = { onSelectAlbum(album) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppleAlbumTile(
    album: AlbumItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val tileCorner = RoundedCornerShape(12.dp)

    val imageRequest = remember(album.coverUri) {
        if (album.coverUri != null) {
            ImageRequest.Builder(context)
                .data(album.coverUri)
                .size(320, 320)
                .crossfade(true)
                .build()
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("album_tile_${album.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(tileCorner)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (imageRequest != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = album.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getAppleAlbumIcon(album.type),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                letterSpacing = (-0.2).sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "${album.itemCount}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AppleMediaTypeRow(
    album: AlbumItem,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getAppleAlbumIcon(album.type),
                contentDescription = null,
                tint = iOSBlue,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = album.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 17.sp,
                    letterSpacing = (-0.3).sp
                ),
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${album.itemCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(13.dp)
            )
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(start = 54.dp)
        )
    }
}

private fun getAppleAlbumIcon(type: AlbumType): ImageVector {
    return when (type) {
        AlbumType.ALL_PHOTOS -> Icons.Default.Image
        AlbumType.ALL_VIDEOS -> Icons.Default.VideoLibrary
        AlbumType.CAMERA -> Icons.Default.PhotoCamera
        AlbumType.SCREENSHOTS -> Icons.Default.Screenshot
        AlbumType.SCREEN_RECORDINGS -> Icons.Default.Videocam
        AlbumType.DOWNLOADS -> Icons.Default.Download
        AlbumType.WHATSAPP_IMAGES -> Icons.Default.Message
        AlbumType.WHATSAPP_VIDEOS -> Icons.Default.Videocam
        AlbumType.EDITED -> Icons.Default.AutoAwesome
        AlbumType.FAVORITES -> Icons.Default.Favorite
        AlbumType.LARGE_FILES -> Icons.Default.Storage
        AlbumType.RECENTLY_ADDED -> Icons.Default.PhotoAlbum
        AlbumType.CUSTOM_FOLDER -> Icons.Default.Folder
    }
}
