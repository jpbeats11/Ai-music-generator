package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeneratedSongJson(
    val title: String,
    val genre: String, // "Synthwave", "Lo-fi", "Techno", "Acoustic Pop"
    val bpm: Int,
    val sections: List<GeneratedSectionJson>
)

@JsonClass(generateAdapter = true)
data class GeneratedSectionJson(
    val name: String, // e.g. "Intro", "Verse 1", "Chorus"
    val durationSeconds: Int,
    val lines: List<String>
)
