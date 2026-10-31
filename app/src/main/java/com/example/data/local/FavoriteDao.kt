package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT mediaUri FROM favorites")
    fun getAllFavoriteUris(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaUri = :uri)")
    suspend fun isFavorite(uri: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaUri = :uri")
    suspend fun removeFavorite(uri: String)

    @Query("DELETE FROM favorites WHERE mediaUri IN (:uris)")
    suspend fun removeFavorites(uris: List<String>)
}
