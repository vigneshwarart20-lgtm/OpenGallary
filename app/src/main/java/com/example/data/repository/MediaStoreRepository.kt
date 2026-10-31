package com.example.data.repository

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.local.FavoriteDao
import com.example.data.local.FavoriteEntity
import com.example.data.model.AlbumItem
import com.example.data.model.AlbumType
import com.example.data.model.DateGroup
import com.example.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class SortOrder(val displayName: String) {
    NEWEST_FIRST("Newest first"),
    OLDEST_FIRST("Oldest first"),
    NAME_ASC("Name (A to Z)"),
    SIZE_DESC("Largest first")
}

class MediaStoreRepository(
    private val context: Context,
    private val favoriteDao: FavoriteDao
) {
    private val contentResolver: ContentResolver = context.contentResolver

    fun getAllMediaFlow(): Flow<List<MediaItem>> = flow {
        emit(queryAllMediaInternal())
    }.combine(favoriteDao.getAllFavoriteUris()) { mediaList, favoriteUris ->
        val favoriteSet = favoriteUris.toSet()
        mediaList.map { item ->
            item.copy(isFavorite = favoriteSet.contains(item.uri.toString()))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun queryAllMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = queryAllMediaInternal()
        val favoriteUris = favoriteDao.getAllFavorites()
        items
    }

    private fun queryAllMediaInternal(): List<MediaItem> {
        val mediaList = mutableListOf<MediaItem>()

        // 1. Query Images
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.RELATIVE_PATH
            } else {
                MediaStore.Images.Media.DATA
            }
        )

        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageProjection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                } else {
                    cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                }

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "IMG_$id"
                    val dateAdded = cursor.getLong(dateAddedCol)
                    val dateMod = cursor.getLong(dateModCol)
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: "image/*"
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)
                    val bucket = cursor.getString(bucketCol) ?: "Pictures"
                    val pathOrRelative = if (pathCol != -1) cursor.getString(pathCol) ?: "" else ""

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    mediaList.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            name = name,
                            path = pathOrRelative,
                            relativePath = pathOrRelative,
                            mimeType = mime,
                            size = size,
                            dateAdded = dateAdded,
                            dateModified = if (dateMod > 0) dateMod else dateAdded,
                            width = width,
                            height = height,
                            durationMs = 0L,
                            isVideo = false,
                            bucketName = bucket
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Query Videos
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.RELATIVE_PATH
            } else {
                MediaStore.Video.Media.DATA
            }
        )

        try {
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                } else {
                    cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                }

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "VID_$id"
                    val dateAdded = cursor.getLong(dateAddedCol)
                    val dateMod = cursor.getLong(dateModCol)
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: "video/*"
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)
                    val duration = cursor.getLong(durationCol)
                    val bucket = cursor.getString(bucketCol) ?: "Movies"
                    val pathOrRelative = if (pathCol != -1) cursor.getString(pathCol) ?: "" else ""

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    mediaList.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            name = name,
                            path = pathOrRelative,
                            relativePath = pathOrRelative,
                            mimeType = mime,
                            size = size,
                            dateAdded = dateAdded,
                            dateModified = if (dateMod > 0) dateMod else dateAdded,
                            width = width,
                            height = height,
                            durationMs = duration,
                            isVideo = true,
                            bucketName = bucket
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Sort descending by dateModified (timestamp in seconds or ms)
        mediaList.sortByDescending { it.effectiveTimestamp }
        return mediaList
    }

    private val MediaItem.effectiveTimestamp: Long
        get() {
            // If timestamp is in seconds (< 100_000_000_000L), convert to ms
            val ts = if (dateModified > 0) dateModified else dateAdded
            return if (ts in 1..99_999_999_999L) ts * 1000L else ts
        }

    fun sortMedia(items: List<MediaItem>, order: SortOrder): List<MediaItem> {
        return when (order) {
            SortOrder.NEWEST_FIRST -> items.sortedByDescending { it.effectiveTimestamp }
            SortOrder.OLDEST_FIRST -> items.sortedBy { it.effectiveTimestamp }
            SortOrder.NAME_ASC -> items.sortedBy { it.name.lowercase(Locale.ROOT) }
            SortOrder.SIZE_DESC -> items.sortedByDescending { it.size }
        }
    }

    fun groupMediaByDate(items: List<MediaItem>): List<DateGroup> {
        if (items.isEmpty()) return emptyList()

        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()
        calendar.timeInMillis = now

        val currentYear = calendar.get(Calendar.YEAR)
        val currentDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        val itemCal = Calendar.getInstance()
        val dayDateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())

        val groups = LinkedHashMap<String, MutableList<MediaItem>>()

        for (item in items) {
            val ts = item.effectiveTimestamp
            itemCal.timeInMillis = ts

            val itemYear = itemCal.get(Calendar.YEAR)
            val itemDayOfYear = itemCal.get(Calendar.DAY_OF_YEAR)

            val header = when {
                itemYear == currentYear && itemDayOfYear == currentDayOfYear -> "Today"
                itemYear == currentYear && itemDayOfYear == currentDayOfYear - 1 -> "Yesterday"
                else -> dayDateFormat.format(Date(ts))
            }

            groups.getOrPut(header) { mutableListOf() }.add(item)
        }

        return groups.map { (title, groupItems) ->
            DateGroup(
                title = title,
                items = groupItems,
                timestamp = groupItems.firstOrNull()?.effectiveTimestamp ?: 0L
            )
        }
    }

    fun buildAlbums(mediaList: List<MediaItem>): List<AlbumItem> {
        val albums = mutableListOf<AlbumItem>()

        val photosOnly = mediaList.filter { !it.isVideo }
        val videosOnly = mediaList.filter { it.isVideo }

        // 1. All Photos
        if (photosOnly.isNotEmpty()) {
            albums.add(
                AlbumItem(
                    id = "all_photos",
                    name = "All Photos",
                    coverUri = photosOnly.firstOrNull()?.uri,
                    itemCount = photosOnly.size,
                    videoCount = 0,
                    type = AlbumType.ALL_PHOTOS
                )
            )
        }

        // 2. All Videos
        if (videosOnly.isNotEmpty()) {
            albums.add(
                AlbumItem(
                    id = "all_videos",
                    name = "All Videos",
                    coverUri = videosOnly.firstOrNull()?.uri,
                    itemCount = videosOnly.size,
                    videoCount = videosOnly.size,
                    type = AlbumType.ALL_VIDEOS
                )
            )
        }

        // 3. Favorites
        val favorites = mediaList.filter { it.isFavorite }
        if (favorites.isNotEmpty()) {
            albums.add(
                AlbumItem(
                    id = "favorites",
                    name = "Favorites",
                    coverUri = favorites.firstOrNull()?.uri,
                    itemCount = favorites.size,
                    videoCount = favorites.count { it.isVideo },
                    type = AlbumType.FAVORITES
                )
            )
        }

        // 4. Camera
        val cameraItems = mediaList.filter {
            val b = it.bucketName.lowercase(Locale.ROOT)
            val p = it.relativePath.lowercase(Locale.ROOT)
            b.contains("camera") || p.contains("dcim/camera") || p.contains("camera")
        }
        if (cameraItems.isNotEmpty()) {
            albums.add(
                AlbumItem(
                    id = "camera",
                    name = "Camera",
                    coverUri = cameraItems.firstOrNull()?.uri,
                    itemCount = cameraItems.size,
                    videoCount = cameraItems.count { it.isVideo },
                    type = AlbumType.CAMERA
                )
            )
        }

        // 5. Screenshots
        val screenshotItems = mediaList.filter {
            val b = it.bucketName.lowercase(Locale.ROOT)
            val p = it.relativePath.lowercase(Locale.ROOT)
            val n = it.name.lowercase(Locale.ROOT)
            b.contains("screenshot") || p.contains("screenshot") || n.startsWith("screenshot")
        }
        if (screenshotItems.isNotEmpty()) {
            albums.add(
                AlbumItem(
                    id = "screenshots",
                    name = "Screenshots",
                    coverUri = screenshotItems.firstOrNull()?.uri,
                    itemCount = screenshotItems.size,
                    videoCount = screenshotItems.count { it.isVideo },
                    type = AlbumType.SCREENSHOTS
                )
            )
        }

        // 6. Screen Recordings
        val screenRecordings = mediaList.filter {
            val b = it.bucketName.lowercase(Locale.ROOT)
            val p = it.relativePath.lowercase(Locale.ROOT)
            val n = it.name.lowercase(Locale.ROOT)
            b.contains("screen recording") || b.contains("screenrecorder") ||
                    p.contains("screen_recording") || p.contains("screenrecord") ||
                    n.contains("screen_recording") || n.contains("screenrecorder")
        }
        if (screenRecordings.isNotEmpty()) {
            albums.add(
                AlbumItem(
                    id = "screen_recordings",
                    name = "Screen Recordings",
                    coverUri = screenRecordings.firstOrNull()?.uri,
                    itemCount = screenRecordings.size,
                    videoCount = screenRecordings.count { it.isVideo },
                    type = AlbumType.SCREEN_RECORDINGS
                )
            )
        }

        // 7. Downloads
        val downloadItems = mediaList.filter {
            val b = it.bucketName.lowercase(Locale.ROOT)
            val p = it.relativePath.lowercase(Locale.ROOT)
            b.contains("download") || p.contains("download")
        }
        if (downloadItems.isNotEmpty()) {
            albums.add(
                AlbumItem(
                    id = "downloads",
                    name = "Downloads",
                    coverUri = downloadItems.firstOrNull()?.uri,
                    itemCount = downloadItems.size,
                    videoCount = downloadItems.count { it.isVideo },
                    type = AlbumType.DOWNLOADS
                )
            )
        }

        // 8. WhatsApp Images
        val whatsappImages = mediaList.filter {
            val b = it.bucketName.lowercase(Locale.ROOT)
            val p = it.relativePath.lowercase(Locale.ROOT)
            (b.contains("whatsapp image") || p.contains("whatsapp images")) && !it.isVideo
        }
        if (whatsappImages.isNotEmpty()) {
            albums.add(
                AlbumItem(
                    id = "whatsapp_images",
                    name = "WhatsApp Images",
                    coverUri = whatsappImages.firstOrNull()?.uri,
                    itemCount = whatsappImages.size,
                    videoCount = 0,
                    type = AlbumType.WHATSAPP_IMAGES
                )
            )
        }

        // 9. WhatsApp Videos
        val whatsappVideos = mediaList.filter {
            val b = it.bucketName.lowercase(Locale.ROOT)
            val p = it.relativePath.lowercase(Locale.ROOT)
            (b.contains("whatsapp video") || p.contains("whatsapp video")) && it.isVideo
        }
        if (whatsappVideos.isNotEmpty()) {
            albums.add(
                AlbumItem(
                    id = "whatsapp_videos",
                    name = "WhatsApp Videos",
                    coverUri = whatsappVideos.firstOrNull()?.uri,
                    itemCount = whatsappVideos.size,
                    videoCount = whatsappVideos.size,
                    type = AlbumType.WHATSAPP_VIDEOS
                )
            )
        }

        // 10. Edited Photos
        val editedPhotos = mediaList.filter {
            val b = it.bucketName.lowercase(Locale.ROOT)
            val p = it.relativePath.lowercase(Locale.ROOT)
            b.contains("puregallery") || b.contains("edited") ||
                    p.contains("puregallery") || p.contains("edited")
        }
        if (editedPhotos.isNotEmpty()) {
            albums.add(
                AlbumItem(
                    id = "edited",
                    name = "Edited Photos",
                    coverUri = editedPhotos.firstOrNull()?.uri,
                    itemCount = editedPhotos.size,
                    videoCount = editedPhotos.count { it.isVideo },
                    type = AlbumType.EDITED
                )
            )
        }

        // 11. Large Files (> 50 MB)
        val largeFiles = mediaList.filter { it.size > 50 * 1024 * 1024 }
        if (largeFiles.isNotEmpty()) {
            albums.add(
                AlbumItem(
                    id = "large_files",
                    name = "Large Files (>50MB)",
                    coverUri = largeFiles.firstOrNull()?.uri,
                    itemCount = largeFiles.size,
                    videoCount = largeFiles.count { it.isVideo },
                    type = AlbumType.LARGE_FILES
                )
            )
        }

        // 12. Other Custom Folders
        val specialAlbumIds = setOf("camera", "screenshots", "screen_recordings", "downloads", "whatsapp_images", "whatsapp_videos", "edited")
        val bucketGroups = mediaList.groupBy { it.bucketName }
        for ((bucketName, itemsInBucket) in bucketGroups) {
            if (bucketName.isBlank()) continue
            val lower = bucketName.lowercase(Locale.ROOT)
            val isKnownSpecial = lower.contains("camera") || lower.contains("screenshot") ||
                    lower.contains("download") || lower.contains("whatsapp") ||
                    lower.contains("puregallery") || lower.contains("screen recording")

            if (!isKnownSpecial && itemsInBucket.isNotEmpty()) {
                albums.add(
                    AlbumItem(
                        id = "folder_$bucketName",
                        name = bucketName,
                        coverUri = itemsInBucket.firstOrNull()?.uri,
                        itemCount = itemsInBucket.size,
                        videoCount = itemsInBucket.count { it.isVideo },
                        type = AlbumType.CUSTOM_FOLDER,
                        bucketId = bucketName
                    )
                )
            }
        }

        return albums
    }

    suspend fun toggleFavorite(mediaItem: MediaItem) {
        val uriStr = mediaItem.uri.toString()
        if (mediaItem.isFavorite) {
            favoriteDao.removeFavorite(uriStr)
        } else {
            favoriteDao.insertFavorite(
                FavoriteEntity(
                    mediaUri = uriStr,
                    mediaId = mediaItem.id
                )
            )
        }
    }

    suspend fun batchSetFavorite(items: List<MediaItem>, favorite: Boolean) {
        if (favorite) {
            for (item in items) {
                favoriteDao.insertFavorite(
                    FavoriteEntity(
                        mediaUri = item.uri.toString(),
                        mediaId = item.id
                    )
                )
            }
        } else {
            favoriteDao.removeFavorites(items.map { it.uri.toString() })
        }
    }

    fun createDeletePendingIntent(uris: List<Uri>): PendingIntent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return MediaStore.createDeleteRequest(contentResolver, uris)
        }
        return null
    }

    suspend fun deleteMediaLegacy(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var count = 0
        for (uri in uris) {
            try {
                val deleted = contentResolver.delete(uri, null, null)
                if (deleted > 0) count++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        favoriteDao.removeFavorites(uris.map { it.toString() })
        count
    }
}
