package com.example.audio

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.database.SongEntity
import com.example.model.GeneratedSongJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

object AudioExporter {
    private val SCALE_A_MINOR_PENTATONIC = doubleArrayOf(220.0, 261.63, 293.66, 329.63, 392.00, 440.0, 523.25, 587.33, 659.25, 783.99)

    suspend fun exportToWav(
        context: Context,
        song: SongEntity,
        format: String = "WAV",
        bitDepth: String = "16-bit",
        sampleRate: Int = 44100,
        bitrate: String = "320 kbps",
        channels: String = "Stereo"
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(GeneratedSongJson::class.java)
            val parsedSong = try {
                adapter.fromJson(song.lyricsJson)
            } catch (e: Exception) {
                null
            }

            val totalDurationSeconds = parsedSong?.sections?.sumOf { it.durationSeconds }?.toFloat() ?: 60f
            
            // Memory & CPU safety constraints for high resolutions
            val maxSafeSecs = when {
                sampleRate >= 384000 -> 10f
                sampleRate >= 192000 -> 15f
                sampleRate >= 96000 -> 30f
                else -> 45f
            }
            val exportDuration = totalDurationSeconds.coerceAtMost(maxSafeSecs)
            val totalSamples = (sampleRate * exportDuration).toInt()

            val resolvedGenre = when (song.genre.trim().lowercase()) {
                "synthwave", "cyberpunk", "disco future", "city pop", "dark synth" -> "Synthwave"
                "lo-fi", "jazz hop", "reggae", "ambient space" -> "Lo-fi"
                "techno", "psytrance", "trance", "drum & bass", "dubstep" -> "Techno"
                else -> "Acoustic Pop"
            }

            val bpm = if (song.bpm in 50..180) song.bpm else 120
            val secPerBeat = 60.0 / bpm
            val secPerStep = secPerBeat / 4.0
            val samplesPerStep = (sampleRate * secPerStep).toInt()

            val rand = Random(song.seed)
            val stepMelodyTrigger = BooleanArray(16) { rand.nextFloat() < 0.6f }
            val stepMelodyNotes = IntArray(16) { rand.nextInt(SCALE_A_MINOR_PENTATONIC.size) }

            var kickTriggerSample = -999999L
            var snareTriggerSample = -999999L
            var hatTriggerSample = -999999L
            var melodyTriggerSample = -999999L
            var currentMelodyFreq = 440.0

            val bitsPerSample = when (bitDepth) {
                "1-bit", "2-bit", "4-bit", "8-bit" -> 8
                "12-bit", "16-bit" -> 16
                "20-bit", "24-bit" -> 24
                "32-bit Integer", "32-bit Float" -> 32
                "64-bit Float" -> 64
                else -> 16
            }

            val numChannels = when (channels) {
                "Mono" -> 1
                "Surround (5.1)" -> 6
                else -> 2 // Stereo
            }

            val audioFormatCode = if (bitDepth == "32-bit Float" || bitDepth == "64-bit Float") 3 else 1
            val pcmDataLength = totalSamples * numChannels * (bitsPerSample / 8)

            val bos = ByteArrayOutputStream()
            writeWavHeader(bos, pcmDataLength, sampleRate.toLong(), numChannels, bitsPerSample, audioFormatCode)

            for (sampleIndex in 0 until totalSamples) {
                val t = sampleIndex.toDouble() / sampleRate
                val step = sampleIndex / samplesPerStep
                val stepSampleIndex = sampleIndex % samplesPerStep
                val currentStepInBar = (step % 16).toInt()

                if (stepSampleIndex == 0) {
                    if (currentStepInBar % 4 == 0) {
                        kickTriggerSample = sampleIndex.toLong()
                    }
                    if (currentStepInBar == 4 || currentStepInBar == 12) {
                        snareTriggerSample = sampleIndex.toLong()
                    }
                    if (resolvedGenre == "Techno" || resolvedGenre == "Synthwave") {
                        if (currentStepInBar % 2 == 1) {
                            hatTriggerSample = sampleIndex.toLong()
                        }
                    } else {
                        if (currentStepInBar % 4 == 2) {
                            hatTriggerSample = sampleIndex.toLong()
                        }
                    }
                    if (stepMelodyTrigger[currentStepInBar]) {
                        melodyTriggerSample = sampleIndex.toLong()
                        currentMelodyFreq = SCALE_A_MINOR_PENTATONIC[stepMelodyNotes[currentStepInBar]]
                        if (resolvedGenre == "Techno") {
                            val offset = (step % 3) * 1.5
                            currentMelodyFreq *= (1.0 + (offset * 0.1))
                        }
                    }
                }

                // Synth Instruments (matches ProceduralAudioEngine exactly)
                val kickTime = (sampleIndex - kickTriggerSample).toDouble() / sampleRate
                val kickVal = if (kickTime in 0.0..0.3) {
                    val freqSweep = 50.0 + 130.0 * exp(-90.0 * kickTime)
                    sin(2.0 * Math.PI * freqSweep * kickTime) * exp(-18.0 * kickTime)
                } else 0.0

                val snareTime = (sampleIndex - snareTriggerSample).toDouble() / sampleRate
                val snareVal = if (snareTime in 0.0..0.4) {
                    val noiseBody = (Random.nextFloat() * 2.0 - 1.0) * exp(-28.0 * snareTime)
                    val toneBody = sin(2.0 * Math.PI * 160.0 * snareTime) * exp(-45.0 * snareTime) * 0.4
                    noiseBody + toneBody
                } else 0.0

                val hatTime = (sampleIndex - hatTriggerSample).toDouble() / sampleRate
                val hatVal = if (hatTime in 0.0..0.1) {
                    (Random.nextFloat() * 2.0 - 1.0) * exp(-140.0 * hatTime)
                } else 0.0

                val barIndex = ((step / 16) % 4).toInt()
                val chordRootFreq = when (barIndex) {
                    0 -> 110.0
                    1 -> 87.31
                    2 -> 130.81
                    3 -> 98.0
                    else -> 110.0
                }

                val bassVal = when (resolvedGenre) {
                    "Synthwave" -> {
                        val speedOfBass = if (currentStepInBar % 2 == 1) 1.0 else 0.6
                        val phase = (t * chordRootFreq) % 1.0
                        (2.0 * phase - 1.0) * exp(-12.0 * (stepSampleIndex.toDouble() / sampleRate % 0.15)) * speedOfBass
                    }
                    "Techno" -> {
                        val phase = (t * chordRootFreq) % 1.0
                        val sq = if (phase < 0.5) 0.8 else -0.8
                        sq * exp(-18.0 * (stepSampleIndex.toDouble() / sampleRate))
                    }
                    "Lo-fi" -> {
                        sin(2.0 * Math.PI * (chordRootFreq * 0.5) * t) * exp(-3.0 * (stepSampleIndex.toDouble() / sampleRate))
                    }
                    else -> {
                        if (currentStepInBar % 8 == 0 || currentStepInBar % 8 == 4) {
                            sin(2.0 * Math.PI * chordRootFreq * t) * exp(-6.0 * (stepSampleIndex.toDouble() / sampleRate))
                        } else 0.0
                    }
                }

                val melodyTime = (sampleIndex - melodyTriggerSample).toDouble() / sampleRate
                val melodyVal = if (melodyTime in 0.0..1.2) {
                    val env = exp(-1.5 * melodyTime)
                    when (resolvedGenre) {
                        "Synthwave" -> {
                            val p = (t * currentMelodyFreq) % 1.0
                            val saw = 2.0 * p - 1.0
                            val sq = if (p < 0.5) 0.5 else -0.5
                            (saw * 0.5 + sq * 0.5) * env
                        }
                        "Lo-fi" -> {
                            (sin(2.0 * Math.PI * currentMelodyFreq * t) + sin(2.0 * Math.PI * (currentMelodyFreq * 2.0) * t) * 0.15) * env
                        }
                        "Techno" -> {
                            val p = (t * currentMelodyFreq) % 1.0
                            (if (p < 0.5) (4.0 * p - 1.0) else (3.0 - 4.0 * p)) * env
                        }
                        else -> {
                            val p = (t * currentMelodyFreq) % 1.0
                            val tri = if (p < 0.5) (4.0 * p - 1.0) else (3.0 - 4.0 * p)
                            (tri * 0.3 + sin(2.0 * Math.PI * currentMelodyFreq * t) * 0.7) * env
                        }
                    }
                } else 0.0

                // Premium spatial panned mixes
                // Kick, snare, and bass are centered. Hi-hat and melody have stereophonic width!
                val masterGain = 1.3
                val leftVal = ((kickVal * 0.50) + (snareVal * 0.30) + (hatVal * 0.10) + (bassVal * 0.35) + (melodyVal * 0.38)) * masterGain
                val rightVal = ((kickVal * 0.50) + (snareVal * 0.30) + (hatVal * 0.22) + (bassVal * 0.35) + (melodyVal * 0.22)) * masterGain

                when (numChannels) {
                    1 -> {
                        // Mono
                        val mixed = (leftVal + rightVal) * 0.5
                        writeSampleValue(bos, mixed, bitDepth)
                    }
                    2 -> {
                        // Stereo
                        writeSampleValue(bos, leftVal, bitDepth)
                        writeSampleValue(bos, rightVal, bitDepth)
                    }
                    6 -> {
                        // 5.1 Surround
                        val centerVal = (leftVal + rightVal) * 0.6
                        val lfeVal = (kickVal * 0.7 + bassVal * 0.5) // LFE channel for solid sub
                        val leftSurroundVal = leftVal * 0.7
                        val rightSurroundVal = rightVal * 0.7

                        writeSampleValue(bos, leftVal, bitDepth)          // Front L
                        writeSampleValue(bos, rightVal, bitDepth)         // Front R
                        writeSampleValue(bos, centerVal, bitDepth)        // Center
                        writeSampleValue(bos, lfeVal, bitDepth)           // LFE Subwoofer
                        writeSampleValue(bos, leftSurroundVal, bitDepth)  // Rear L
                        writeSampleValue(bos, rightSurroundVal, bitDepth) // Rear R
                    }
                }
            }

            val bytes = bos.toByteArray()
            val cleanTitle = song.title.replace("\\s+".toRegex(), "_")
            val extension = when (format.uppercase()) {
                "AIFF" -> "aiff"
                "FLAC" -> "flac"
                "MP3" -> "mp3"
                "AAC" -> "aac"
                else -> "wav"
            }
            val fileName = "Studio_${cleanTitle}.${extension}"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, when (extension) {
                    "aiff" -> "audio/x-aiff"
                    "flac" -> "audio/flac"
                    "mp3" -> "audio/mpeg"
                    "aac" -> "audio/aac"
                    else -> "audio/wav"
                })
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            
            uri?.let {
                resolver.openOutputStream(it)?.use { os ->
                    os.write(bytes)
                }
            }
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun writeSampleValue(bos: ByteArrayOutputStream, sampleValDouble: Double, bitDepth: String) {
        val limited = sampleValDouble.coerceIn(-1.0, 1.0)
        when (bitDepth) {
            "1-bit" -> {
                val outVal = if (limited >= 0.0) 255 else 0
                bos.write(outVal)
            }
            "2-bit" -> {
                val outVal = if (limited < -0.5) 0 else if (limited < 0.0) 85 else if (limited < 0.5) 170 else 255
                bos.write(outVal)
            }
            "4-bit" -> {
                val steps = Math.round((limited + 1.0) * 7.5).toInt().coerceIn(0, 15)
                val outVal = steps * 17
                bos.write(outVal)
            }
            "8-bit" -> {
                val outVal = ((limited + 1.0) * 127.5).toInt().coerceIn(0, 255)
                bos.write(outVal)
            }
            "12-bit" -> {
                val steps = Math.round(limited * 2047.0).toInt().coerceIn(-2048, 2047)
                val outVal = (steps shl 4).toShort()
                bos.write(outVal.toInt() and 0xFF)
                bos.write((outVal.toInt() shr 8) and 0xFF)
            }
            "16-bit" -> {
                val outVal = (limited * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
                bos.write(outVal.toInt() and 0xFF)
                bos.write((outVal.toInt() shr 8) and 0xFF)
            }
            "20-bit" -> {
                val steps = Math.round(limited * 524287.0).toInt().coerceIn(-524288, 524287)
                val outVal = steps shl 4
                bos.write(outVal and 0xFF)
                bos.write((outVal shr 8) and 0xFF)
                bos.write((outVal shr 16) and 0xFF)
            }
            "24-bit" -> {
                val outVal = (limited * 8388607.0).toInt().coerceIn(-8388608, 8388607)
                bos.write(outVal and 0xFF)
                bos.write((outVal shr 8) and 0xFF)
                bos.write((outVal shr 16) and 0xFF)
            }
            "32-bit Integer" -> {
                val outVal = (limited * 2147483647.0).toLong().coerceIn(-2147483648, 2147483647).toInt()
                bos.write(outVal and 0xFF)
                bos.write((outVal shr 8) and 0xFF)
                bos.write((outVal shr 16) and 0xFF)
                bos.write((outVal shr 24) and 0xFF)
            }
            "32-bit Float" -> {
                val intBits = java.lang.Float.floatToIntBits(limited.toFloat())
                bos.write(intBits and 0xFF)
                bos.write((intBits shr 8) and 0xFF)
                bos.write((intBits shr 16) and 0xFF)
                bos.write((intBits shr 24) and 0xFF)
            }
            "64-bit Float" -> {
                val longBits = java.lang.Double.doubleToLongBits(limited)
                bos.write((longBits and 0xFF).toInt())
                bos.write(((longBits shr 8) and 0xFF).toInt())
                bos.write(((longBits shr 16) and 0xFF).toInt())
                bos.write(((longBits shr 24) and 0xFF).toInt())
                bos.write(((longBits shr 32) and 0xFF).toInt())
                bos.write(((longBits shr 40) and 0xFF).toInt())
                bos.write(((longBits shr 48) and 0xFF).toInt())
                bos.write(((longBits shr 56) and 0xFF).toInt())
            }
            else -> {
                val outVal = (limited * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
                bos.write(outVal.toInt() and 0xFF)
                bos.write((outVal.toInt() shr 8) and 0xFF)
            }
        }
    }

    private fun writeWavHeader(
        out: OutputStream,
        pcmDataLength: Int,
        sampleRate: Long,
        channels: Int,
        bitsPerSample: Int,
        audioFormatCode: Int
    ) {
        val totalDataLen = pcmDataLength + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte() // RIFF
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte() // WAVE
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte() // 'fmt ' chunk
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = (audioFormatCode and 0xff).toByte()
        header[21] = ((audioFormatCode shr 8) and 0xff).toByte()
        header[22] = (channels and 0xff).toByte()
        header[23] = ((channels shr 8) and 0xff).toByte()
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte()
        header[33] = 0
        header[34] = (bitsPerSample and 0xff).toByte()
        header[35] = ((bitsPerSample shr 8) and 0xff).toByte()
        header[36] = 'd'.code.toByte() // data
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (pcmDataLength and 0xff).toByte()
        header[41] = ((pcmDataLength shr 8) and 0xff).toByte()
        header[42] = ((pcmDataLength shr 16) and 0xff).toByte()
        header[43] = ((pcmDataLength shr 24) and 0xff).toByte()

        out.write(header, 0, 44)
    }
}
