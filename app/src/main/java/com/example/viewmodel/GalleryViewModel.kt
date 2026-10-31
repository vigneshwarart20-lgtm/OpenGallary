package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PureGalleryDatabase
import com.example.data.model.AlbumItem
import com.example.data.model.AlbumType
import com.example.data.model.DateGroup
import com.example.data.model.MediaItem
import com.example.data.repository.MediaStoreRepository
import com.example.data.repository.SortOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}

enum class SearchFilterType {
    ALL,
    PHOTOS_ONLY,
    VIDEOS_ONLY,
    FAVORITES_ONLY
}

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = PureGalleryDatabase.getDatabase(application)
    val repository = MediaStoreRepository(application, database.favoriteDao())

    // Permission state
    val hasPermission = MutableStateFlow(false)

    // User preferences
    val sortOrder = MutableStateFlow(SortOrder.NEWEST_FIRST)
    val gridColumns = MutableStateFlow(3)
    val themePreference = MutableStateFlow(ThemePreference.SYSTEM)

    // Refresh and auto-update mechanism
    private val refreshTrigger = MutableStateFlow(0L)
    val isRefreshing = MutableStateFlow(false)

    private val contentObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            refreshTrigger.value = System.currentTimeMillis()
        }
    }

    init {
        try {
            application.contentResolver.registerContentObserver(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                contentObserver
            )
            application.contentResolver.registerContentObserver(
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                contentObserver
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().contentResolver.unregisterContentObserver(contentObserver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun refreshMedia() {
        viewModelScope.launch {
            isRefreshing.value = true
            refreshTrigger.value = System.currentTimeMillis()
            delay(400)
            isRefreshing.value = false
        }
    }

    // Master media stream (reacts to refreshTrigger and room favorites)
    private val rawMediaStream = refreshTrigger.flatMapLatest {
        repository.getAllMediaFlow()
    }

    val allMedia: StateFlow<List<MediaItem>> = combine(rawMediaStream, sortOrder) { items, sort ->
        repository.sortMedia(items, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dateGroups: StateFlow<List<DateGroup>> = allMedia.combine(sortOrder) { items, _ ->
        repository.groupMediaByDate(items)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<AlbumItem>> = allMedia.combine(sortOrder) { items, _ ->
        repository.buildAlbums(items)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videosList: StateFlow<List<MediaItem>> = allMedia.combine(sortOrder) { items, _ ->
        items.filter { it.isVideo }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoritesList: StateFlow<List<MediaItem>> = allMedia.combine(sortOrder) { items, _ ->
        items.filter { it.isFavorite }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected album for detail view
    val selectedAlbum = MutableStateFlow<AlbumItem?>(null)

    val selectedAlbumMedia: StateFlow<List<MediaItem>> = combine(allMedia, selectedAlbum) { all, album ->
        if (album == null) return@combine emptyList()
        when (album.type) {
            AlbumType.ALL_PHOTOS -> all.filter { !it.isVideo }
            AlbumType.ALL_VIDEOS -> all.filter { it.isVideo }
            AlbumType.FAVORITES -> all.filter { it.isFavorite }
            AlbumType.CAMERA -> all.filter {
                val b = it.bucketName.lowercase(Locale.ROOT)
                val p = it.relativePath.lowercase(Locale.ROOT)
                b.contains("camera") || p.contains("camera")
            }
            AlbumType.SCREENSHOTS -> all.filter {
                val b = it.bucketName.lowercase(Locale.ROOT)
                val p = it.relativePath.lowercase(Locale.ROOT)
                val n = it.name.lowercase(Locale.ROOT)
                b.contains("screenshot") || p.contains("screenshot") || n.startsWith("screenshot")
            }
            AlbumType.SCREEN_RECORDINGS -> all.filter {
                val b = it.bucketName.lowercase(Locale.ROOT)
                val p = it.relativePath.lowercase(Locale.ROOT)
                val n = it.name.lowercase(Locale.ROOT)
                b.contains("screen recording") || b.contains("screenrecorder") ||
                        p.contains("screen_recording") || p.contains("screenrecord") ||
                        n.contains("screen_recording") || n.contains("screenrecorder")
            }
            AlbumType.DOWNLOADS -> all.filter {
                val b = it.bucketName.lowercase(Locale.ROOT)
                val p = it.relativePath.lowercase(Locale.ROOT)
                b.contains("download") || p.contains("download")
            }
            AlbumType.WHATSAPP_IMAGES -> all.filter {
                val b = it.bucketName.lowercase(Locale.ROOT)
                val p = it.relativePath.lowercase(Locale.ROOT)
                (b.contains("whatsapp image") || p.contains("whatsapp images")) && !it.isVideo
            }
            AlbumType.WHATSAPP_VIDEOS -> all.filter {
                val b = it.bucketName.lowercase(Locale.ROOT)
                val p = it.relativePath.lowercase(Locale.ROOT)
                (b.contains("whatsapp video") || p.contains("whatsapp video")) && it.isVideo
            }
            AlbumType.EDITED -> all.filter {
                val b = it.bucketName.lowercase(Locale.ROOT)
                val p = it.relativePath.lowercase(Locale.ROOT)
                b.contains("puregallery") || b.contains("edited") || p.contains("puregallery")
            }
            AlbumType.LARGE_FILES -> all.filter { it.size > 50 * 1024 * 1024 }
            AlbumType.RECENTLY_ADDED -> all.take(100)
            AlbumType.CUSTOM_FOLDER -> all.filter { it.bucketName == album.bucketId }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedAlbumDateGroups: StateFlow<List<DateGroup>> = selectedAlbumMedia.combine(sortOrder) { items, _ ->
        repository.groupMediaByDate(items)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Multi-selection state
    val isSelectionMode = MutableStateFlow(false)
    val selectedUris = MutableStateFlow<Set<Uri>>(emptySet())

    fun toggleSelection(item: MediaItem) {
        val current = selectedUris.value.toMutableSet()
        if (current.contains(item.uri)) {
            current.remove(item.uri)
        } else {
            current.add(item.uri)
        }
        selectedUris.value = current
        if (current.isEmpty()) {
            isSelectionMode.value = false
        } else {
            isSelectionMode.value = true
        }
    }

    fun startSelectionWith(item: MediaItem) {
        isSelectionMode.value = true
        selectedUris.value = setOf(item.uri)
    }

    fun selectAll(items: List<MediaItem>) {
        selectedUris.value = items.map { it.uri }.toSet()
    }

    fun clearSelection() {
        selectedUris.value = emptySet()
        isSelectionMode.value = false
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item)
        }
    }

    fun favoriteSelected(favorite: Boolean) {
        val uris = selectedUris.value
        val items = allMedia.value.filter { uris.contains(it.uri) }
        viewModelScope.launch {
            repository.batchSetFavorite(items, favorite)
            clearSelection()
        }
    }

    // Search state
    val searchQuery = MutableStateFlow("")
    val searchFilterType = MutableStateFlow(SearchFilterType.ALL)

    val searchResults: StateFlow<List<MediaItem>> = combine(
        allMedia,
        searchQuery,
        searchFilterType
    ) { media, query, filter ->
        val trimmed = query.trim().lowercase(Locale.ROOT)
        media.filter { item ->
            // Filter type check
            val matchesType = when (filter) {
                SearchFilterType.ALL -> true
                SearchFilterType.PHOTOS_ONLY -> !item.isVideo
                SearchFilterType.VIDEOS_ONLY -> item.isVideo
                SearchFilterType.FAVORITES_ONLY -> item.isFavorite
            }
            if (!matchesType) return@filter false

            if (trimmed.isEmpty()) return@filter true

            val nameMatch = item.name.lowercase(Locale.ROOT).contains(trimmed)
            val bucketMatch = item.bucketName.lowercase(Locale.ROOT).contains(trimmed)
            val pathMatch = item.relativePath.lowercase(Locale.ROOT).contains(trimmed)
            val dateStr = SimpleDateFormat("yyyy MMMM dd", Locale.getDefault())
                .format(Date(if (item.dateModified > 0) item.dateModified * 1000L else item.dateAdded * 1000L))
                .lowercase(Locale.ROOT)
            val dateMatch = dateStr.contains(trimmed)

            nameMatch || bucketMatch || pathMatch || dateMatch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Full screen viewer state
    val viewerItem = MutableStateFlow<MediaItem?>(null)
    val viewerList = MutableStateFlow<List<MediaItem>>(emptyList())

    fun openViewer(item: MediaItem, list: List<MediaItem>) {
        viewerList.value = list
        viewerItem.value = item
    }

    fun closeViewer() {
        viewerItem.value = null
        viewerList.value = emptyList()
    }

    // Photo editor target
    val editingItem = MutableStateFlow<MediaItem?>(null)

    fun startEditing(item: MediaItem) {
        editingItem.value = item
    }

    fun finishEditing() {
        editingItem.value = null
    }

    fun setPermissionGranted(granted: Boolean) {
        hasPermission.value = granted
    }
}
