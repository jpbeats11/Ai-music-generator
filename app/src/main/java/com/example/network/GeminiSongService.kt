package com.example.network

import com.example.BuildConfig
import com.example.model.GeneratedSongJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Model representation of Retrofit queries
data class Part(val text: String)
data class Content(val parts: List<Part>)
data class GenerationConfig(val responseMimeType: String? = null, val temperature: Float? = null)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

data class ResponsePart(val text: String?)
data class ResponseContent(val parts: List<ResponsePart>?)
data class Candidate(val content: ResponseContent?)
data class GenerateContentResponse(val candidates: List<Candidate>?)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

class GeminiSongService {
    suspend fun generateSongFromPrompt(
        prompt: String,
        customLyrics: String? = null,
        manualGenre: String? = null,
        manualDurationSeconds: Int? = null
    ): GeneratedSongJson? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API key is unconfigured. Please add it via the AI Studio Secrets panel.")
        }

        val lyricsInstruction = if (!customLyrics.isNullOrBlank()) {
            """
            CRITICAL: The user has explicitly provided these custom lyrics. You MUST use these exact lyrics! Do NOT write new lyrics.
            Structure these custom lyrics precisely into appropriate section parts (like Intro, Verse 1, Chorus, Verse 2, Chorus, Outro as needed) of the JSON output:
            
            CUSTOM LYRICS:
            $customLyrics
            """
        } else {
            "Be extremely creative with the lyrics! Make them rhyme well and flow rhythmically. Generate between 4 and 6 structured song sections (e.g. Intro, Verse 1, Chorus, Verse 2, Chorus, Outro)."
        }

        val genreRestriction = if (!manualGenre.isNullOrBlank()) {
            "CRITICAL: You MUST use the genre \"$manualGenre\" for the song in the \"genre\" JSON field."
        } else {
            "Choose one of the 20 supported genres listed below that best matches the prompt's emotion or style."
        }

        val durationRestriction = if (manualDurationSeconds != null && manualDurationSeconds > 0) {
            "CRITICAL: The sum of `durationSeconds` for all sections in the generated JSON MUST exactly sum to $manualDurationSeconds seconds! (e.g. if $manualDurationSeconds, divide it into sections like 15s Intro, 45s Verse 1, 45s Chorus, 45s Verse 2, 30s Outro or similar)."
        } else {
            "Make the total song duration around 90-180 seconds, with sections having reasonable individual durations (usually 10 to 45 seconds each)."
        }

        val systemPrompt = """
            You are an expert AI Music Composer. Your task is to generate complete song lyrics and track metadata based on the user's prompt.
            You MUST output your response strictly inside a JSON block of the following format:
            {
              "title": "A short, beautiful, catchy song title",
              "genre": "one of the 20 supported genres listed below",
              "bpm": 120, 
              "sections": [
                {
                  "name": "Intro",
                  "durationSeconds": 15,
                  "lines": ["lyric line 1", "lyric line 2"]
                },
                {
                  "name": "Verse 1",
                  "durationSeconds": 30,
                  "lines": ["verse lyric line 1", "verse lyric line 2"]
                },
                {
                  "name": "Chorus",
                  "durationSeconds": 30,
                  "lines": ["chorus lyric line 1", "chorus lyric line 2"]
                }
              ]
            }

            SUPPORTED GENRES AND CHARACTERISTICS:
            1. "Synthwave": BPM 110-125, nostalgic driving electronic synthesizer theme.
            2. "Lo-Fi": BPM 70-85, cozy, dusty record sound, relaxed.
            3. "Techno": BPM 130-145, rapid hypnotic peak-time dance rhythms.
            4. "Acoustic Pop": BPM 90-105, warm melody, bright upbeat chords.
            5. "Cyberpunk": BPM 120-140, heavy dark synthesizer with industrial tech elements.
            6. "Metal": BPM 140-170, fast-tempo, intense aggressive sound.
            7. "Hip-Hop": BPM 85-100, bouncy flow beats, urban aesthetic.
            8. "Classical Synth": BPM 100-120, cinematic, symphonic synthesizer.
            9. "Reggae": BPM 75-90, relaxed offbeat groove and warm bass.
            10. "Jazz Hop": BPM 80-95, smooth jazzy piano chords combined with cozy beats.
            11. "Disco Future": BPM 115-130, energetic funk bassline and futuristic pop colors.
            12. "Drum & Bass": BPM 160-180, high-speed breaks and heavy sub bass waves.
            13. "House Deep": BPM 120-126, deep warm backing chords and steady rhythm.
            14. "Ambient Space": BPM 60-80, slow stellar pads, atmospheric, minimal drums.
            15. "Trance": BPM 135-144, euphoric high-energy arpeggiator builds.
            16. "Dubstep": BPM 140, intense wobbling low frequencies and heavy stomps.
            17. "Folk Rock": BPM 95-115, acoustic plucks with positive indie vibes.
            18. "City Pop": BPM 110-120, retro Japanese funk-pop, warm synth brass lines.
            19. "Dark Synth": BPM 100-115, gothic or mysterious mood, heavy vintage leads.
            20. "Psytrance": BPM 138-145, rapid hypnotic rolling bass grids.

            $genreRestriction
            $durationRestriction
            $lyricsInstruction

            Output ONLY valid JSON inside the response. Do not surround it with markdown blocks or anything else.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "User prompt for song content/theme/style: $prompt")))),
            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 1.0f),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw IllegalStateException("Empty response from AI engine")
            
            val cleanedJson = cleanJsonResponse(responseText)
            val adapter = RetrofitClient.moshi.adapter(GeneratedSongJson::class.java)
            adapter.fromJson(cleanedJson)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun cleanJsonResponse(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json")
        } else if (text.startsWith("```")) {
            text = text.removePrefix("```")
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```")
        }
        return text.trim()
    }
}
