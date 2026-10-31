package com.example.data.model

data class DateGroup(
    val title: String,
    val items: List<MediaItem>,
    val timestamp: Long = 0L
)
