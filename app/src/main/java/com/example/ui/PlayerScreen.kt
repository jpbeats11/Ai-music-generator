package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.SongEntity
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.BrightMagenta
import com.example.ui.components.AudioVisualizer
import com.example.viewmodel.SongViewModel
import kotlinx.coroutines.launch
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: SongViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val audioEngine = viewModel.audioEngine
    val song = audioEngine.currentPlayingSong

    if (song == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No track playing",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onBackClick) {
                    Text("Browse Studio Library")
                }
            }
        }
        return
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showLyricEditor by remember { mutableStateOf(false) }
    var editedLyricText by remember { mutableStateOf("") }

    var showExportSettings by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableStateOf("WAV") }
    var selectedBitDepth by remember { mutableStateOf("24-bit") }
    var selectedSampleRate by remember { mutableStateOf("96 kHz") }
    var selectedBitrate by remember { mutableStateOf("320 kbps") }
    var selectedChannels by remember { mutableStateOf("Stereo") }

    val formatOptions = remember { listOf("WAV", "MP3", "FLAC", "AAC", "AIFF", "m4a", "OGG", "Opus", "WMA", "APE", "PCM", "DSD", "DFF", "CAF", "AMR", "AU", "RA", "AC3", "DTS", "MKA") }
    val bitDepthOptions = remember { listOf("1-bit", "2-bit", "4-bit", "8-bit", "12-bit", "16-bit", "20-bit", "24-bit", "32-bit Integer", "32-bit Float", "64-bit Float") }
    val sampleRateOptions = remember { listOf("8 kHz", "11.025 kHz", "16 kHz", "22.05 kHz", "32 kHz", "44.1 kHz", "48 kHz", "88.2 kHz", "96 kHz", "176.4 kHz", "192 kHz", "352.8 kHz", "384 kHz") }
    val bitrateOptions = remember { listOf("32 kbps", "64 kbps", "96 kbps", "128 kbps", "160 kbps", "192 kbps", "224 kbps", "256 kbps", "320 kbps", "500 kbps", "1000 kbps+") }
    val channelOptions = remember { listOf("Mono", "Stereo", "Surround (5.1)") }

    // Parse the lyrics structure to lists containing section details for visual browsing
    val sections = remember(song.lyricsJson) {
        try {
            val jsonAdapter = com.example.network.RetrofitClient.moshi.adapter(com.example.model.GeneratedSongJson::class.java)
            jsonAdapter.fromJson(song.lyricsJson)?.sections ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    val flatLyricsLinesWithSection = remember(sections) {
        val list = mutableListOf<Pair<String, String>>() // Key = Line Text, Value = Section Name
        for (sec in sections) {
            for (line in sec.lines) {
                list.add(Pair(line, sec.name))
            }
        }
        list
    }

    // Infinite rotating transition matching the playback status
    val infiniteTransition = rememberInfiniteTransition()
    val rotationAngle by if (audioEngine.isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Studio Player", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("player_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val rawLyricsFormatted = sections.joinToString("\n\n") { sec ->
                                "[${sec.name}]\n" + sec.lines.joinToString("\n")
                            }
                            editedLyricText = rawLyricsFormatted
                            showLyricEditor = true
                        },
                        modifier = Modifier.testTag("player_edit_lyrics")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Refine AI Lyrics",
                            tint = CyberCyan
                        )
                    }

                    IconButton(
                        onClick = { showExportSettings = true },
                        modifier = Modifier.testTag("player_export_audio")
                    ) {
                        if (viewModel.isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = BrightMagenta
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Export audio with studio quality configurations",
                                tint = BrightMagenta
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.toggleFavorite(song) },
                        modifier = Modifier.testTag("player_favorite_toggle")
                    ) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Toggle favorite",
                            tint = if (song.isFavorite) BrightMagenta else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            audioEngine.errorMessage?.let { errorMsg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Playback Error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMsg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Genre / BPM badges
            Row(
                modifier = Modifier.padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { },
                    label = { Text(song.genre) },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = CyberCyan,
                        leadingIconContentColor = CyberCyan
                    ),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                )
                AssistChip(
                    onClick = { },
                    label = { Text("${song.bpm} BPM") },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = BrightMagenta,
                        leadingIconContentColor = BrightMagenta
                    ),
                    border = BorderStroke(1.dp, BrightMagenta.copy(alpha = 0.5f))
                )
            }

            // Spinning Vinyl Disk component
            VinylDisk(isPlaying = audioEngine.isPlaying, rotationAngle = rotationAngle)

            Spacer(modifier = Modifier.height(16.dp))

            // Track details
            Text(
                text = song.title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = "Prompt: \"${song.prompt}\"",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Dynamic High-Fidelity Audio Visualization Component
            AudioVisualizer(
                amplitudes = audioEngine.visualizerAmplitudes,
                isPlaying = audioEngine.isPlaying,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(vertical = 12.dp)
            )

            // Karaoke sliding lyric showcase
            LyricsDisplay(
                audioEngine = audioEngine,
                flatLyricsLinesWithSection = flatLyricsLinesWithSection,
                listState = listState,
                scope = scope
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Slider
            PlaybackProgress(audioEngine = audioEngine)

            Spacer(modifier = Modifier.height(12.dp))

            // Playback Actions (Play/Pause, Stop)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { audioEngine.stop() },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .testTag("player_stop_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stop,
                        contentDescription = "Stop",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = { audioEngine.togglePlayPause() },
                    modifier = Modifier
                        .size(64.dp)
                        .padding(horizontal = 8.dp)
                        .shadow(4.dp, CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ElectricViolet, BrightMagenta)
                            ), CircleShape
                        )
                        .testTag("player_play_pause")
                ) {
                    Icon(
                        imageVector = if (audioEngine.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (audioEngine.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            if (showLyricEditor) {
                AlertDialog(
                    onDismissRequest = { showLyricEditor = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Refine Composed Lyrics",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Fine-tune lines or adjust bracket sections (e.g., [Verse 1], [Chorus]). Changes save directly to base track and update the live engine.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            OutlinedTextField(
                                value = editedLyricText,
                                onValueChange = { editedLyricText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .testTag("lyrics_editor_field"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                ),
                                placeholder = {
                                    Text(
                                        "Enter lyrics structured with sections...",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.updateSongLyricsText(song, editedLyricText)
                                showLyricEditor = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.testTag("save_lyrics_button")
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showLyricEditor = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showExportSettings) {
                AlertDialog(
                    onDismissRequest = { showExportSettings = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = BrightMagenta,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Hi-Res Export Studio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                        ) {
                            Text(
                                text = "Design and configure the physical encoding structure for your procedural production masters.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // 1. Selector for Format
                            FilterChipRow(
                                label = "1. TARGET AUDIO FORMAT",
                                options = formatOptions,
                                selectedOption = selectedFormat,
                                onOptionSelected = { selectedFormat = it }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 2. Selector for Bit Depth
                            FilterChipRow(
                                label = "2. BIT DEPTH RESOLUTION",
                                options = bitDepthOptions,
                                selectedOption = selectedBitDepth,
                                onOptionSelected = { selectedBitDepth = it }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 3. Selector for Sample Rate
                            FilterChipRow(
                                label = "3. SAMPLE RATE FREQUENCY",
                                options = sampleRateOptions,
                                selectedOption = selectedSampleRate,
                                onOptionSelected = { selectedSampleRate = it }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 4. Selector for Channels
                            FilterChipRow(
                                label = "4. CHANNEL MAPPING",
                                options = channelOptions,
                                selectedOption = selectedChannels,
                                onOptionSelected = { selectedChannels = it }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 5. Selector for Bitrate (compressed profiles)
                            FilterChipRow(
                                label = "5. TARGET BITRATE (Lossy Compression)",
                                options = bitrateOptions,
                                selectedOption = selectedBitrate,
                                onOptionSelected = { selectedBitrate = it }
                            )
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = BrightMagenta.copy(alpha = 0.08f)
                                ),
                                border = BorderStroke(1.dp, BrightMagenta.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = BrightMagenta,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val qualDesc = when {
                                        selectedBitDepth.contains("32-bit Float") && selectedSampleRate.contains("384") -> "Ultra High Resolution Audio Master"
                                        selectedBitDepth.contains("24") && (selectedSampleRate.contains("96") || selectedSampleRate.contains("192")) -> "Studio Standard Master Master (High Fidelity)"
                                        selectedBitDepth.contains("16") && selectedSampleRate.contains("44.1") -> "Red Book CD Standard Studio Mastering"
                                        selectedBitDepth.contains("8") || selectedBitDepth.contains("4") || selectedBitDepth.contains("2") || selectedBitDepth.contains("1") -> "Authentic Vintage 80s/90s Digital Crunch Emulation"
                                        else -> "Customized Hybrid Sound Master Profile"
                                    }
                                    Text(
                                        text = qualDesc,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val hz = when (selectedSampleRate) {
                                    "8 kHz" -> 8000
                                    "11.025 kHz" -> 11025
                                    "16 kHz" -> 16000
                                    "22.05 kHz" -> 22050
                                    "32 kHz" -> 32000
                                    "44.1 kHz" -> 44100
                                    "48 kHz" -> 48000
                                    "88.2 kHz" -> 88200
                                    "96 kHz" -> 96000
                                    "176.4 kHz" -> 176400
                                    "192 kHz" -> 192000
                                    "352.8 kHz" -> 352800
                                    "384 kHz" -> 384000
                                    else -> 44100
                                }
                                viewModel.exportSongToDownloads(
                                    context = context,
                                    song = song,
                                    format = selectedFormat,
                                    bitDepth = selectedBitDepth,
                                    sampleRate = hz,
                                    bitrate = selectedBitrate,
                                    channels = selectedChannels
                                )
                                showExportSettings = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrightMagenta,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("confirm_export_button")
                        ) {
                            Text("Export Master", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showExportSettings = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Text("Dismiss")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun VinylDisk(isPlaying: Boolean, rotationAngle: Float) {
    Box(
        modifier = Modifier
            .size(190.dp)
            .shadow(16.dp, CircleShape)
            .clip(CircleShape)
            .background(Color(0xFF03010A))
            .rotate(rotationAngle),
        contentAlignment = Alignment.Center
    ) {
        // Grooves
        for (d in listOf(170, 140, 110, 80)) {
            Box(
                modifier = Modifier
                    .size(d.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
            )
        }

        // Inner sticker
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        colors = listOf(ElectricViolet, BrightMagenta, CyberCyan, ElectricViolet)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
            )
        }
    }
}

@Composable
private fun LyricsDisplay(
    audioEngine: com.example.audio.ProceduralAudioEngine,
    flatLyricsLinesWithSection: List<Pair<String, String>>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val activeLineIdx = remember(audioEngine.currentLyricsLine, flatLyricsLinesWithSection) {
        flatLyricsLinesWithSection.indexOfFirst { it.first == audioEngine.currentLyricsLine }
    }

    var lastLyricsLineIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(activeLineIdx) {
        if (activeLineIdx != -1 && activeLineIdx != lastLyricsLineIndex) {
            lastLyricsLineIndex = activeLineIdx
            scope.launch {
                listState.animateScrollToItem(
                    index = (activeLineIdx - 1).coerceAtLeast(0),
                    scrollOffset = 0
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp) // Fixed height to prevent layout jumps
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(8.dp)
    ) {
        if (flatLyricsLinesWithSection.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Procedural beat sequence playing...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 40.dp)
            ) {
                itemsIndexed(flatLyricsLinesWithSection) { idx, item ->
                    val isCurrent = idx == activeLineIdx
                    val lineText = item.first
                    val sectionName = item.second

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Label section name on switch
                        val isFirstInSection = idx == 0 || flatLyricsLinesWithSection[idx - 1].second != sectionName
                        if (isFirstInSection) {
                            Text(
                                text = sectionName.uppercase(),
                                color = if (isCurrent) CyberCyan else Color.White.copy(alpha = 0.2f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        Text(
                            text = lineText,
                            fontSize = if (isCurrent) 17.sp else 14.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackProgress(audioEngine: com.example.audio.ProceduralAudioEngine) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val position = audioEngine.currentPositionSeconds
        val duration = audioEngine.totalDurationSeconds
        val progress = if (duration > 0f) position / duration else 0f

        Slider(
            value = progress,
            onValueChange = {},
            enabled = false,
            colors = SliderDefaults.colors(
                thumbColor = BrightMagenta,
                activeTrackColor = ElectricViolet,
                inactiveTrackColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(position),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatTime(duration),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FilterChipRow(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = CyberCyan,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(options) { item ->
                val isSelected = item == selectedOption
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) BrightMagenta.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                        .border(
                            1.dp,
                            if (isSelected) BrightMagenta else Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onOptionSelected(item) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = item,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatTime(seconds: Float): String {
    val m = (seconds / 60).toInt()
    val s = (seconds % 60).toInt()
    return String.format("%02d:%02d", m, s)
}
