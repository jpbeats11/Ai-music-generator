package com.example.database

import kotlinx.coroutines.flow.Flow

class SongRepository(private val songDao: SongDao) {
    val allSongs: Flow<List<SongEntity>> = songDao.getAllSongs()
    val favoriteSongs: Flow<List<SongEntity>> = songDao.getFavoriteSongs()

    suspend fun getSongById(id: Long): SongEntity? {
        return songDao.getSongById(id)
    }

    suspend fun insertSong(song: SongEntity): Long {
        return songDao.insertSong(song)
    }

    suspend fun updateSong(song: SongEntity) {
        songDao.updateSong(song)
    }

    suspend fun deleteSong(song: SongEntity) {
        songDao.deleteSong(song)
    }
}
