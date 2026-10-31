package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val mediaUri: String,
    val mediaId: Long,
    val addedAt: Long = System.currentTimeMillis()
)
