package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val prompt: String,
    val genre: String,
    val bpm: Int,
    val seed: Long,
    val lyricsJson: String,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
