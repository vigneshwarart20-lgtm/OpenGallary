package com.example.data.model

import android.net.Uri

enum class AlbumType {
    ALL_PHOTOS,
    ALL_VIDEOS,
    CAMERA,
    SCREENSHOTS,
    SCREEN_RECORDINGS,
    DOWNLOADS,
    WHATSAPP_IMAGES,
    WHATSAPP_VIDEOS,
    EDITED,
    FAVORITES,
    RECENTLY_ADDED,
    LARGE_FILES,
    CUSTOM_FOLDER
}

data class AlbumItem(
    val id: String,
    val name: String,
    val coverUri: Uri? = null,
    val itemCount: Int = 0,
    val videoCount: Int = 0,
    val type: AlbumType = AlbumType.CUSTOM_FOLDER,
    val bucketId: String = ""
)
