package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.MediaItem
import com.example.viewmodel.GalleryViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    initialItem: MediaItem,
    mediaList: List<MediaItem>,
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
    onEditPhoto: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val initialIndex = remember(initialItem, mediaList) {
        val found = mediaList.indexOfFirst { it.id == initialItem.id }
        if (found >= 0) found else 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { mediaList.size }
    )

    val currentItem = mediaList.getOrNull(pagerState.currentPage) ?: initialItem

    LaunchedEffect(initialItem.id, mediaList) {
        val found = mediaList.indexOfFirst { it.id == initialItem.id }
        if (found >= 0 && found != pagerState.currentPage) {
            pagerState.scrollToPage(found)
        }
    }

    var isImmersive by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Launcher for Android 11+ scoped storage delete consent dialog
    val deleteIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("media_viewer_screen")
    ) {
        // Horizontal Pager for swiping between media
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { mediaList.getOrNull(it)?.id ?: it.toLong() }
        ) { page ->
            val item = mediaList.getOrNull(page)
            if (item != null) {
                if (item.isVideo) {
                    VideoPlayerView(
                        mediaItem = item,
                        onToggleControls = { isImmersive = !isImmersive }
                    )
                } else {
                    ZoomableImageView(
                        mediaItem = item,
                        onToggleControls = { isImmersive = !isImmersive }
                    )
                }
            }
        }

        // Top Overlay Bar
        AnimatedVisibility(
            visible = !isImmersive,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("viewer_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = currentItem.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    val dateFormatted = remember(currentItem) {
                        val ts = if (currentItem.dateModified > 0) currentItem.dateModified * 1000L else currentItem.dateAdded * 1000L
                        SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(ts))
                    }
                    Text(
                        text = "$dateFormatted • ${pagerState.currentPage + 1}/${mediaList.size}",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Favorite button
                IconButton(
                    onClick = { viewModel.toggleFavorite(currentItem) },
                    modifier = Modifier.testTag("viewer_favorite_button")
                ) {
                    Icon(
                        imageVector = if (currentItem.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (currentItem.isFavorite) Color(0xFFFF3B30) else Color.White
                    )
                }

                // Apple Photos "Edit" button in top right
                if (!currentItem.isVideo) {
                    TextButton(
                        onClick = { onEditPhoto(currentItem) },
                        modifier = Modifier.testTag("viewer_edit_button")
                    ) {
                        Text(
                            text = "Edit",
                            color = com.example.ui.theme.iOSBlue,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Bottom Overlay Bar (Apple Photos 4-icon layout: Share, Favorite, Info, Delete)
        AnimatedVisibility(
            visible = !isImmersive,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Share action
                IconButton(
                    onClick = { shareMedia(context, listOf(currentItem.uri), currentItem.mimeType) },
                    modifier = Modifier.testTag("viewer_share_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Info action
                IconButton(
                    onClick = { showInfoSheet = true },
                    modifier = Modifier.testTag("viewer_info_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Delete action
                IconButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.testTag("viewer_delete_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Details Modal Sheet
        if (showInfoSheet) {
            MediaDetailsBottomSheet(
                item = currentItem,
                onDismiss = { showInfoSheet = false }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Delete Media") },
                text = { Text("Are you sure you want to delete '${currentItem.name}' from your device storage?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = false
                            coroutineScope.launch {
                                val pendingIntent = viewModel.repository.createDeletePendingIntent(listOf(currentItem.uri))
                                if (pendingIntent != null) {
                                    val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                    deleteIntentLauncher.launch(request)
                                } else {
                                    viewModel.repository.deleteMediaLegacy(listOf(currentItem.uri))
                                    viewModel.refreshMedia()
                                    onBack()
                                }
                            }
                        },
                        modifier = Modifier.testTag("confirm_delete_button")
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun ZoomableImageView(
    mediaItem: MediaItem,
    onToggleControls: () -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val isZoomed = scale > 1.05f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(mediaItem.id) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = {
                        if (scale > 1.2f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .then(
                if (isZoomed) {
                    Modifier.pointerInput(mediaItem.id, isZoomed) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1.05f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(mediaItem.uri)
                .crossfade(true)
                .build(),
            contentDescription = mediaItem.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        )
    }
}

@Composable
private fun VideoPlayerView(
    mediaItem: MediaItem,
    onToggleControls: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(mediaItem.id) {
                detectTapGestures(
                    onTap = {
                        videoViewRef?.let { vv ->
                            if (vv.isPlaying) {
                                vv.pause()
                                isPlaying = false
                            } else {
                                vv.start()
                                isPlaying = true
                            }
                        }
                        onToggleControls()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                object : VideoView(ctx) {
                    override fun onTouchEvent(ev: android.view.MotionEvent?): Boolean {
                        // Return false so HorizontalPager can receive drag gestures to switch videos/photos
                        return false
                    }
                }.apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setVideoURI(mediaItem.uri)
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        mp.start()
                        isPlaying = true
                    }
                    videoViewRef = this
                }
            },
            update = { videoView ->
                videoViewRef = videoView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Center Play/Pause button when paused
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(Color.Black.copy(alpha = 0.55f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }
        }
    }
}

@Composable
private fun ViewerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .testTag(testTag)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaDetailsBottomSheet(
    item: MediaItem,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            DetailItemRow(label = "Filename", value = item.name)
            DetailItemRow(label = "Type", value = item.mimeType)
            DetailItemRow(label = "Size", value = item.formattedSize)

            if (item.width > 0 && item.height > 0) {
                DetailItemRow(label = "Resolution", value = "${item.width} × ${item.height} pixels")
            }

            if (item.isVideo && item.durationMs > 0) {
                DetailItemRow(label = "Duration", value = item.formattedDuration)
            }

            val dateStr = remember(item) {
                val ts = if (item.dateModified > 0) item.dateModified * 1000L else item.dateAdded * 1000L
                SimpleDateFormat("EEE, d MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date(ts))
            }
            DetailItemRow(label = "Date", value = dateStr)

            if (item.relativePath.isNotBlank()) {
                DetailItemRow(label = "Folder", value = item.relativePath)
            } else if (item.bucketName.isNotBlank()) {
                DetailItemRow(label = "Album", value = item.bucketName)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailItemRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

fun shareMedia(context: Context, uris: List<Uri>, mimeType: String = "*/*") {
    try {
        if (uris.size == 1) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
        } else if (uris.size > 1) {
            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Selected Media"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
