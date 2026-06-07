package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioExporter
import com.example.audio.ProceduralAudioEngine
import com.example.database.SongDatabase
import com.example.database.SongEntity
import com.example.database.SongRepository
import com.example.model.GeneratedSongJson
import com.example.network.GeminiSongService
import com.example.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

sealed interface GenerationState {
    object Idle : GenerationState
    data class Generating(val stepMessage: String) : GenerationState
    data class Success(val song: SongEntity) : GenerationState
    data class Error(val message: String) : GenerationState
}

class SongViewModel(application: Application) : AndroidViewModel(application) {
    private val database = SongDatabase.getDatabase(application)
    private val repository = SongRepository(database.songDao())
    private val apiService = GeminiSongService()

    // Audio Engine instance
    val audioEngine = ProceduralAudioEngine()

    // Tracks song list
    val allSongs: StateFlow<List<SongEntity>> = repository.allSongs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteSongs: StateFlow<List<SongEntity>> = repository.favoriteSongs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    var generationState: GenerationState by mutableStateOf(GenerationState.Idle)
        private set

    // Session-based state management metric
    var sessionTracksCount by mutableStateOf(0)
        private set

    var isExporting by mutableStateOf(false)
        private set

    fun resetGenerationState() {
        generationState = GenerationState.Idle
    }

    fun generateSong(
        prompt: String,
        customLyrics: String? = null,
        manualGenre: String? = null,
        manualDurationSeconds: Int? = null
    ) {
        if (prompt.trim().isEmpty()) {
            generationState = GenerationState.Error("Please enter a song prompt.")
            return
        }

        viewModelScope.launch {
            try {
                generationState = GenerationState.Generating("Analyzing theme & mood...")
                delay(1200)
                
                generationState = GenerationState.Generating("Writing custom verses and chorus...")
                val result = apiService.generateSongFromPrompt(prompt, customLyrics, manualGenre, manualDurationSeconds)
                
                if (result == null) {
                    generationState = GenerationState.Error("Could not generate lyrics. Please check your Gemini API key configuration.")
                    return@launch
                }

                generationState = GenerationState.Generating("Composing dynamic backing track...")
                delay(1000)

                generationState = GenerationState.Generating("Arranging synthetic patterns...")
                delay(800)

                // Convert model to JSON string for DB
                val adapter = RetrofitClient.moshi.adapter(GeneratedSongJson::class.java)
                val jsonString = adapter.toJson(result)

                val newSong = SongEntity(
                    title = result.title,
                    prompt = prompt,
                    genre = result.genre,
                    bpm = result.bpm,
                    seed = Random.nextLong(),
                    lyricsJson = jsonString,
                    isFavorite = false
                )

                generationState = GenerationState.Generating("Saves in studio library...")
                val songId = repository.insertSong(newSong)
                
                // Retrieve full entity with id populated
                val savedSong = repository.getSongById(songId)
                if (savedSong != null) {
                    // Increment session counter
                    sessionTracksCount++
                    generationState = GenerationState.Success(savedSong)
                    // Auto-play the newly prepared music
                    audioEngine.play(savedSong)
                } else {
                    generationState = GenerationState.Error("Error saving song template.")
                }
            } catch (e: Exception) {
                generationState = GenerationState.Error(e.message ?: "An unexpected error occurred during music creation.")
            }
        }
    }

    fun toggleFavorite(song: SongEntity) {
        viewModelScope.launch {
            val updated = song.copy(isFavorite = !song.isFavorite)
            repository.updateSong(updated)
            
            // Sync with current playing if matches
            if (audioEngine.currentPlayingSong?.id == song.id) {
                // To force state refresh on player screens
                audioEngine.play(updated)
            }
        }
    }

    fun updateSongLyricsText(song: SongEntity, rawLyricsText: String) {
        viewModelScope.launch {
            try {
                val adapter = RetrofitClient.moshi.adapter(GeneratedSongJson::class.java)
                val existingJson = adapter.fromJson(song.lyricsJson)
                
                if (existingJson != null) {
                    val parsedSections = mutableListOf<com.example.model.GeneratedSectionJson>()
                    var currentSectionName = "Verse"
                    val currentLines = mutableListOf<String>()
                    
                    for (line in rawLyricsText.lines()) {
                        val trimmed = line.trim()
                        if (trimmed.isEmpty()) continue
                        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                            if (currentLines.isNotEmpty()) {
                                parsedSections.add(com.example.model.GeneratedSectionJson(currentSectionName, 30, currentLines.toList()))
                                currentLines.clear()
                            }
                            currentSectionName = trimmed.drop(1).dropLast(1)
                        } else {
                            currentLines.add(trimmed)
                        }
                    }
                    if (currentLines.isNotEmpty() || parsedSections.isEmpty()) {
                        parsedSections.add(com.example.model.GeneratedSectionJson(currentSectionName, 30, currentLines.toList()))
                    }
                    
                    val updatedJson = existingJson.copy(sections = parsedSections)
                    val updatedJsonString = adapter.toJson(updatedJson)
                    val updatedSong = song.copy(lyricsJson = updatedJsonString)
                    
                    repository.updateSong(updatedSong)
                    
                    if (audioEngine.currentPlayingSong?.id == song.id) {
                        // Refresh playing song reference in audio engine
                        audioEngine.play(updatedSong)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun exportSongToDownloads(
        context: Context,
        song: SongEntity,
        format: String = "WAV",
        bitDepth: String = "16-bit",
        sampleRate: Int = 44100,
        bitrate: String = "320 kbps",
        channels: String = "Stereo"
    ) {
        if (isExporting) return
        isExporting = true
        viewModelScope.launch {
            try {
                val uri = AudioExporter.exportToWav(
                    context = context,
                    song = song,
                    format = format,
                    bitDepth = bitDepth,
                    sampleRate = sampleRate,
                    bitrate = bitrate,
                    channels = channels
                )
                withContext(Dispatchers.Main) {
                    if (uri != null) {
                        Toast.makeText(context, "Track exported successfully as $format ($bitDepth, ${sampleRate / 1000f} kHz) to Downloads!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Failed to export track.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isExporting = false
            }
        }
    }

    fun deleteSong(song: SongEntity) {
        viewModelScope.launch {
            if (audioEngine.currentPlayingSong?.id == song.id) {
                audioEngine.stop()
            }
            repository.deleteSong(song)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
    }
}
