package com.example.data.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val path: String = "",
    val relativePath: String = "",
    val mimeType: String = "",
    val size: Long = 0L,
    val dateAdded: Long = 0L,
    val dateModified: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0L,
    val isVideo: Boolean = false,
    val bucketName: String = "",
    val isFavorite: Boolean = false
) {
    val formattedDuration: String
        get() {
            if (!isVideo || durationMs <= 0) return ""
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return if (minutes >= 60) {
                val hours = minutes / 60
                val remainingMinutes = minutes % 60
                String.format("%d:%02d:%02d", hours, remainingMinutes, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            if (size <= 0) return "0 B"
            val kb = size / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.1f KB", kb)
                else -> "$size B"
            }
        }
}
