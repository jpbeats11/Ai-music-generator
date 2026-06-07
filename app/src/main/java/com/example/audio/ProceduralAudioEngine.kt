package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.database.SongEntity
import com.example.model.GeneratedSongJson
import com.example.model.GeneratedSectionJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlin.math.sin
import kotlin.math.exp
import kotlin.random.Random

class ProceduralAudioEngine {
    private var audioTrack: AudioTrack? = null
    private var playJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // UI exposed states
    var isPlaying by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)

    var currentPositionSeconds by mutableStateOf(0f)
        private set

    var totalDurationSeconds by mutableStateOf(120f)
        private set

    var currentSectionName by mutableStateOf("Intro")
        private set

    var currentLyricsLine by mutableStateOf("")
        private set

    var currentPlayingSong: SongEntity? by mutableStateOf(null)
        private set

    // Simple visualizer buffer exposed to Compose Canvas
    private val visualizerInnerBuffer = FloatArray(32) { 0.1f }
    var visualizerAmplitudes by mutableStateOf(visualizerInnerBuffer.copyOf())
        private set

    private var parsedSong: GeneratedSongJson? = null

    companion object {
        private const val SAMPLE_RATE = 44100
        private val SCALE_A_MINOR_PENTATONIC = doubleArrayOf(220.0, 261.63, 293.66, 329.63, 392.00, 440.0, 523.25, 587.33, 659.25, 783.99)
    }

    fun getBaseModelGenre(g: String): String {
        return when (g.trim().lowercase()) {
            "synthwave", "cyberpunk", "disco future", "city pop", "dark synth" -> "Synthwave"
            "lo-fi", "jazz hop", "reggae", "ambient space" -> "Lo-fi"
            "techno", "psytrance", "trance", "drum & bass", "dubstep" -> "Techno"
            "acoustic pop", "metal", "hip-hop", "classical synth", "house deep", "folk rock" -> "Acoustic Pop"
            else -> {
                val lower = g.trim().lowercase()
                if (lower.contains("synth") || lower.contains("wave") || lower.contains("punk") || lower.contains("disco")) {
                    "Synthwave"
                } else if (lower.contains("lo-fi") || lower.contains("lofi") || lower.contains("reggae") || lower.contains("ambient") || lower.contains("space") || lower.contains("jazz")) {
                    "Lo-fi"
                } else if (lower.contains("techno") || lower.contains("house") || lower.contains("step") || lower.contains("trance") || lower.contains("bass") || lower.contains("psy")) {
                    "Techno"
                } else {
                    "Acoustic Pop"
                }
            }
        }
    }

    fun play(song: SongEntity) {
        errorMessage = null
        if (currentPlayingSong?.id == song.id && isPlaying) {
            // Already playing this song, do nothing
            return
        }

        // Stop current playback
        stop()

        currentPlayingSong = song
        
        // Parse the song json to extract sections
        try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(GeneratedSongJson::class.java)
            parsedSong = adapter.fromJson(song.lyricsJson)
        } catch (e: Exception) {
            parsedSong = null
        }

        val totalSecs = parsedSong?.sections?.sumOf { it.durationSeconds }?.toFloat() ?: 120f
        totalDurationSeconds = totalSecs
        isPlaying = true

        playJob = scope.launch {
            runAudioSynthLoop(song)
        }
    }

    fun togglePlayPause() {
        errorMessage = null
        val song = currentPlayingSong
        if (isPlaying) {
            pause()
        } else if (song != null) {
            isPlaying = true
            playJob = scope.launch {
                runAudioSynthLoop(song)
            }
        }
    }

    fun pause() {
        isPlaying = false
        playJob?.cancel()
        playJob = null
        audioTrack?.run {
            try {
                pause()
                flush()
            } catch (e: Exception) {}
        }
    }

    fun stop() {
        isPlaying = false
        playJob?.cancel()
        playJob = null
        audioTrack?.run {
            try {
                stop()
                release()
            } catch (e: Exception) {}
        }
        audioTrack = null
        currentPositionSeconds = 0f
        currentLyricsLine = ""
        currentSectionName = "Intro"
    }

    private suspend fun runAudioSynthLoop(song: SongEntity) {
        val resolvedGenre = getBaseModelGenre(song.genre)
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        // Ensure buffer size is reasonable for stereo stream
        val bufferSize = (minBufferSize * 2).coerceAtLeast(16384)
        
        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            
            if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                isPlaying = false
                errorMessage = "AudioTrack failed to initialize. Try restarting playback."
                return
            }
        } catch (e: Exception) {
            isPlaying = false
            errorMessage = "Failed to create AudioTrack: ${e.localizedMessage}"
            return
        }

        try {
            audioTrack?.play()
            // Ensure max volume for the track itself
            audioTrack?.setVolume(1.0f)
        } catch (e: Exception) {
            isPlaying = false
            errorMessage = "Failed to start AudioTrack: ${e.localizedMessage}"
            return
        }

        val bpm = if (song.bpm in 50..180) song.bpm else 120
        val secPerBeat = 60.0 / bpm
        val secPerStep = secPerBeat / 4.0 // 16th note step
        val samplesPerStep = (SAMPLE_RATE * secPerStep).toInt()

        // Generate matching melody loop of 16 steps using song seed
        val rand = Random(song.seed)
        val stepMelodyTrigger = BooleanArray(16) { rand.nextFloat() < 0.6f }
        val stepMelodyNotes = IntArray(16) { rand.nextInt(SCALE_A_MINOR_PENTATONIC.size) }

        var sampleIndex = (currentPositionSeconds * SAMPLE_RATE).toLong()
        val writeChunkSize = 2048 // stereo window chunk size
        val audioData = ShortArray(writeChunkSize * 2)

        // Track snare and kick hits
        var kickTriggerSample = -999999L
        var snareTriggerSample = -999999L
        var hatTriggerSample = -999999L
        var melodyTriggerSample = -999999L
        var currentMelodyFreq = 440.0

        val sections = parsedSong?.sections ?: emptyList()
        val sectionsWithSumDurations = mutableListOf<Pair<GeneratedSectionJson, Float>>()
        var accumulator = 0f
        for (sec in sections) {
            accumulator += sec.durationSeconds
            sectionsWithSumDurations.add(Pair(sec, accumulator))
        }

        var loopCounter = 0
        while (isPlaying) {
            val startSec = sampleIndex.toFloat() / SAMPLE_RATE
            if (startSec >= totalDurationSeconds) {
                // Done playing
                currentPositionSeconds = totalDurationSeconds
                isPlaying = false
                break
            }

            // Update current position for UI every 2 chunks to reduce recomposition
            if (loopCounter % 2 == 0) {
                currentPositionSeconds = startSec

                // Determine active section and lyric lines based on elapsed seconds
                var activeSection: GeneratedSectionJson? = null
                var priorDurationSum = 0f
                for (p in sectionsWithSumDurations) {
                    if (startSec < p.second) {
                        activeSection = p.first
                        break
                    }
                    priorDurationSum = p.second
                }

                if (activeSection != null) {
                    currentSectionName = activeSection.name
                    val sectionElapsed = startSec - priorDurationSum
                    val linesCount = activeSection.lines.size
                    if (linesCount > 0) {
                        val lineDur = activeSection.durationSeconds.toFloat() / linesCount
                        val activeLineIdx = (sectionElapsed / lineDur).toInt().coerceIn(0, linesCount - 1)
                        currentLyricsLine = activeSection.lines[activeLineIdx]
                    } else {
                        currentLyricsLine = ""
                    }
                } else {
                    currentLyricsLine = ""
                }
            }

            // Synthesize the PCM samples for this chunk
            for (index in 0 until writeChunkSize) {
                val currentSample = sampleIndex + index
                val t = currentSample.toDouble() / SAMPLE_RATE

                val step = (currentSample / samplesPerStep).toInt()
                val stepSampleIndex = currentSample % samplesPerStep
                val currentStepInBar = step % 16

                // Trigger instruments at start of step
                if (stepSampleIndex == 0L) {
                    // Kick trigger: Four on the floor (beats 1, 2, 3, 4 i.e. 0, 4, 8, 12)
                    if (currentStepInBar % 4 == 0) {
                        kickTriggerSample = currentSample
                    }

                    // Snare trigger: beats 2 and 4 (steps 4, 12)
                    if (currentStepInBar == 4 || currentStepInBar == 12) {
                        snareTriggerSample = currentSample
                    }

                    // Hi-hat trigger: Offbeat eighth notes (e.g., steps 2, 6, 10, 14) and sometimes others
                    if (resolvedGenre == "Techno" || resolvedGenre == "Synthwave") {
                        if (currentStepInBar % 2 == 1) {
                            hatTriggerSample = currentSample
                        }
                    } else if (resolvedGenre == "Lo-fi") {
                        if (currentStepInBar % 4 == 2) {
                            hatTriggerSample = currentSample
                        }
                    } else { // Acoustic Pop
                        if (currentStepInBar % 4 == 2) {
                            hatTriggerSample = currentSample
                        }
                    }

                    // Melody trigger
                    if (stepMelodyTrigger[currentStepInBar]) {
                        melodyTriggerSample = currentSample
                        currentMelodyFreq = SCALE_A_MINOR_PENTATONIC[stepMelodyNotes[currentStepInBar]]
                        if (resolvedGenre == "Lo-fi") {
                            // In lo-fi play soft octave higher or lower
                            currentMelodyFreq *= 1.0
                        } else if (resolvedGenre == "Techno") {
                            // Arpeggiator feel
                            val offset = (step % 3) * 1.5
                            currentMelodyFreq *= (1.0 + (offset * 0.1))
                        }
                    }
                }

                // 1. KICK SYNTH
                val kickTime = (currentSample - kickTriggerSample).toDouble() / SAMPLE_RATE
                val kickVal = if (kickTime in 0.0..0.3) {
                    val freqSweep = 50.0 + 130.0 * exp(-90.0 * kickTime)
                    val value = sin(2.0 * Math.PI * freqSweep * kickTime)
                    val envelope = exp(-18.0 * kickTime)
                    value * envelope
                } else 0.0

                // 2. SNARE SYNTH
                val snareTime = (currentSample - snareTriggerSample).toDouble() / SAMPLE_RATE
                val snareVal = if (snareTime in 0.0..0.4) {
                    val noiseBody = (Random.nextFloat() * 2.0 - 1.0) * exp(-28.0 * snareTime)
                    val toneBody = sin(2.0 * Math.PI * 160.0 * snareTime) * exp(-45.0 * snareTime) * 0.4
                    noiseBody + toneBody
                } else 0.0

                // 3. HI-HAT SYNTH
                val hatTime = (currentSample - hatTriggerSample).toDouble() / SAMPLE_RATE
                val hatVal = if (hatTime in 0.0..0.1) {
                    val rawNoise = (Random.nextFloat() * 2.0 - 1.0)
                    val envelope = exp(-140.0 * hatTime)
                    rawNoise * envelope
                } else 0.0

                // 4. BASS SYNTH
                // Primary progression chord index
                val barIndex = (step / 16) % 4
                val chordRootFreq = when (barIndex) {
                    0 -> 110.0 // Am (A2)
                    1 -> 87.31 // F (F2)
                    2 -> 130.81 // C (C3)
                    3 -> 98.0  // G (G2)
                    else -> 110.0
                }

                val bassVal = when (resolvedGenre) {
                    "Synthwave" -> {
                        // Driving saw-tooth pumping bassline on off-beats
                        val speedOfBass = if (currentStepInBar % 2 == 1) 1.0 else 0.6
                        val phase = (t * chordRootFreq) % 1.0
                        val rawSaw = 2.0 * phase - 1.0
                        val bassStepSecs = (stepSampleIndex.toDouble() / SAMPLE_RATE)
                        val env = exp(-12.0 * (bassStepSecs % 0.15))
                        rawSaw * env * speedOfBass
                    }
                    "Techno" -> {
                        // Steady rhythmic bass square/saw
                        val phase = (t * chordRootFreq) % 1.0
                        val rawSquare = if (phase < 0.5) 0.8 else -0.8
                        val bassStepSecs = (stepSampleIndex.toDouble() / SAMPLE_RATE)
                        val env = exp(-18.0 * bassStepSecs)
                        rawSquare * env
                    }
                    "Lo-fi" -> {
                        // Warm smooth sine bass wave
                        val bassStepSecs = (stepSampleIndex.toDouble() / SAMPLE_RATE)
                        val env = exp(-3.0 * bassStepSecs)
                        sin(2.0 * Math.PI * (chordRootFreq * 0.5) * t) * env
                    }
                    else -> { // Acoustic Pop
                        // Simple pluck on beat 1 and 3
                        if (currentStepInBar % 8 == 0 || currentStepInBar % 8 == 4) {
                            val bassStepSecs = (stepSampleIndex.toDouble() / SAMPLE_RATE)
                            val env = exp(-6.0 * bassStepSecs)
                            sin(2.0 * Math.PI * chordRootFreq * t) * env
                        } else 0.0
                    }
                }

                // 5. MELODY SYNTH
                val melodyTime = (currentSample - melodyTriggerSample).toDouble() / SAMPLE_RATE
                val melodyVal = if (melodyTime in 0.0..1.2) {
                    val envelope = exp(-1.5 * melodyTime)
                    when (resolvedGenre) {
                        "Synthwave" -> {
                            // Rich square/saw wave
                            val p = (t * currentMelodyFreq) % 1.0
                            val saw = 2.0 * p - 1.0
                            val sq = if (p < 0.5) 0.5 else -0.5
                            (saw * 0.5 + sq * 0.5) * envelope
                        }
                        "Lo-fi" -> {
                            // Warm ambient flute-like sine wave
                            val fineSine = sin(2.0 * Math.PI * currentMelodyFreq * t)
                            val harmonics = sin(2.0 * Math.PI * (currentMelodyFreq * 2.0) * t) * 0.15
                            (fineSine + harmonics) * envelope
                        }
                        "Techno" -> {
                            // Acid squelch/triangle pluck
                            val p = (t * currentMelodyFreq) % 1.0
                            val tri = if (p < 0.5) (4.0 * p - 1.0) else (3.0 - 4.0 * p)
                            tri * envelope
                        }
                        else -> { // Acoustic Pop
                            // Pluck piano feel
                            val p = (t * currentMelodyFreq) % 1.0
                            val tri = if (p < 0.5) (4.0 * p - 1.0) else (3.0 - 4.0 * p)
                            val sine = sin(2.0 * Math.PI * currentMelodyFreq * t)
                            (tri * 0.3 + sine * 0.7) * envelope
                        }
                    }
                } else 0.0

                // Mix them carefully to prevent clipping
                val masterVol = 1.3f
                val mix = (kickVal * 0.50f) + 
                          (snareVal * 0.30f) + 
                          (hatVal * 0.20f) + 
                          (bassVal * 0.35f) + 
                          (melodyVal * 0.35f)

                // Soft limiter
                val limited = if (mix > 1.0) 1.0 else if (mix < -1.0) -1.0 else mix
                val sampleValue = (limited * 32000.0 * masterVol).toInt().coerceIn(-32768, 32767).toShort()

                // Interleave Stereo sound
                audioData[index * 2] = sampleValue
                audioData[index * 2 + 1] = sampleValue
            }

            // Write PCM chunk to AudioTrack
            val written = audioTrack?.write(audioData, 0, writeChunkSize * 2) ?: 0
            if (written < 0) {
                errorMessage = "AudioTrack write error: $written"
            }
            sampleIndex += writeChunkSize
            loopCounter++

            // Update simple visualizer visual buffer for UI using pre-allocated array
            for (v in 0 until 32) {
                val p = (v * (writeChunkSize / 32)) * 2
                val amp = Math.abs(audioData[p].toFloat() / 32000f).coerceIn(0f, 1f)
                visualizerInnerBuffer[v] = amp
            }
            // Trigger state change only once per chunk by assigning a copy
            visualizerAmplitudes = visualizerInnerBuffer.copyOf()

            yield()
        }
    }
}
