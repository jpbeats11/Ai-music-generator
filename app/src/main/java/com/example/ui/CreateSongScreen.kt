package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrightMagenta
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.components.AudioVisualizer
import com.example.viewmodel.GenerationState
import com.example.viewmodel.SongViewModel
import kotlinx.coroutines.launch

data class PresetPrompt(val label: String, val text: String)

data class MusicSuggestion(
    val title: String,
    val description: String,
    val prompt: String,
    val genre: String,
    val style: String,
    val mood: String,
    val lyrics: String,
    val emoji: String
)

@Composable
fun SuggestionBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

val musicSuggestions = listOf(
    MusicSuggestion(
        title = "Neon Hacker Journey",
        description = "Intense cyber action with sharp rhythms and digital lyrics",
        prompt = "Heavy cyberpunk dark synth track about a hacker infiltrating a virtual neon fortress.",
        genre = "Cyberpunk",
        style = "Cyberpunk",
        mood = "Futuristic",
        lyrics = """[Verse 1]
Glitch in the system, bypassing the wall,
The cyber guards rise but they're destined to fall.
Silicon wires are running through veins,
Breaking the firewall, severing chains.

[Chorus]
In the neon shadows of the fortress high,
Digital rebels under virtual sky.
Upload the virus, watch the servers glow,
Take back the power from the grid below.""",
        emoji = "⚡"
    ),
    MusicSuggestion(
        title = "Silent Supernova",
        description = "Slow ambient space synths focusing on cosmic discovery",
        prompt = "Cinematic atmospheric space synthesizer track about floating alone past beautiful supernovas.",
        genre = "Ambient Space",
        style = "Synthwave",
        mood = "Epic",
        lyrics = """[Intro]
(Gentle radar pings and soft stellar pads pulsing)

[Verse 1]
Lost connection at the edge of space,
Floating freely in this silent place.
Stars are drawing closer in the dark,
Only static left of my spark.

[Chorus]
Signal faded, but the soul is clear,
Now floating with no gravity or fear.
Through the starlight, drifting cold and far,
Walking rings around a giant star.""",
        emoji = "🌌"
    ),
    MusicSuggestion(
        title = "Rainy Day Compiler",
        description = "Soft, cozy study lo-fi beats with warm bedroom mood",
        prompt = "Chill relaxed bedroom lo-fi hip hop with warm vinyl crackles about writing codes on a rainy morning.",
        genre = "Lo-Fi",
        style = "Lo-Fi",
        mood = "Chill",
        lyrics = """[Intro]
(Soft sound of rain on glass, record needle drop)

[Verse]
Tapping keys under the amber gleam,
Warm green tea and a relaxing dream.
The terminal flashes, the lines are set,
Coffee stains on a desk I won't forget.

[Chorus]
Drip drop, let the soft beats flow,
Rain is hitting on the glass below.
Code compiling in the cozy shade,
This is how masterpieces get made.""",
        emoji = "☕"
    ),
    MusicSuggestion(
        title = "Tokyo Midnight Cruise",
        description = "Retro Japanese city-pop style with warm horn synth elements",
        prompt = "Funky nostalgic 80s Tokyo Midnight Cruise city pop with retro synthesizer lines.",
        genre = "City Pop",
        style = "City Pop",
        mood = "Uplifting",
        lyrics = """[Verse 1]
Neon flashing on the crowded street,
Engine roaring to a modern beat.
Midnight colors in your eyes shine bright,
Let the radio take us away tonight.

[Chorus]
City pop is singing from the dashboard light,
Dancing through the Tokyo neon night.
Bassline grooving and the horns play sweet,
Feel the summer breeze beneath our feet.""",
        emoji = "🌆"
    ),
    MusicSuggestion(
        title = "Zero-G Disco Dance",
        description = "Energetic cosmic dance beats with slapping futuristic bassline",
        prompt = "Slapping high-energy futuristic disco funk track about dancing in zero gravity orbit.",
        genre = "Disco Future",
        style = "Synthwave",
        mood = "Energetic",
        lyrics = """[Verse 1]
Floating upwards past the ceiling floor,
We don't need magnetic boots no more.
Sparks are flying from the DJ deck,
Zero-G maneuvers with no safety net.

[Chorus]
Shake it up in the zero gravity,
Dance together under custody.
Cosmic lights and a disco solar sphere,
We are spinning out of atmosphere!""",
        emoji = "🕺"
    ),
    MusicSuggestion(
        title = "Mountain High Country",
        description = "Acoustic indie folk guitar about climbing and achieving challenges",
        prompt = "Warm acoustic folk rock song with sweeping guitars about hiking past the timberline.",
        genre = "Folk Rock",
        style = "Folk Rock",
        mood = "Uplifting",
        lyrics = """[Verse 1]
Strap the boots and put the map away,
The sun is breaking on a brand new day.
Crisp fresh wind is pushing on my spine,
Walking upwards past the timberline.

[Chorus]
Take a breath at the highest peak,
For the silent wonders that we seek.
Hills are whispering a ancient song,
This is the high country where we belong.""",
        emoji = "⛰️"
    ),
    MusicSuggestion(
        title = "Retro Synthwave Drive",
        description = "Nostalgic 1980s retro synth drum beats with analog style",
        prompt = "Late-night retro synthwave drive through endless grids with sunset aesthetic.",
        genre = "Synthwave",
        style = "Synthwave",
        mood = "Futuristic",
        lyrics = """[Verse 1]
Gridlines stretching to the red sunset,
Memories I know I won't forget.
Drive forever in the digital breeze,
Washing over electronic trees.

[Chorus]
Retro engine howling in the wind,
Leaving all our worries far behind.
Keep on driving through the analog light,
Into the violet cybernetic night.""",
        emoji = "🚗"
    )
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateSongScreen(
    viewModel: SongViewModel,
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var promptText by remember { mutableStateOf("") }
    
    // Style and Mood modifier states
    var selectedStyles by remember { mutableStateOf(setOf<String>()) }
    var selectedMoods by remember { mutableStateOf(setOf<String>()) }
    
    val audioEngine = viewModel.audioEngine
    
    // Manual music settings states
    var showManualOptions by remember { mutableStateOf(false) }
    var manualGenreSelection by remember { mutableStateOf("Auto") }
    var manualDurationMinutesSelection by remember { mutableStateOf("Auto") }
    
    val manualGenresList = listOf(
        "Auto", "Synthwave", "Lo-Fi", "Techno", "Acoustic Pop", "Cyberpunk", "Metal",
        "Hip-Hop", "Classical Synth", "Reggae", "Jazz Hop", "Disco Future", "Drum & Bass",
        "House Deep", "Ambient Space", "Trance", "Dubstep", "Folk Rock", "City Pop",
        "Dark Synth", "Psytrance"
    )

    val manualDurationsList = listOf(
        "Auto", "1 Minute", "2 Minutes", "3 Minutes", "4 Minutes", "5 Minutes"
    )
    
    // Custom Lyrics editor states
    var useCustomLyrics by remember { mutableStateOf(false) }
    var customLyricsText by remember { mutableStateOf("") }
    val maxLyricsCharCount = 1000

    val generationState = viewModel.generationState
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    
    // Active suggestion index for Co-Writer Assistant
    var activeSuggestionIndex by remember { mutableStateOf(0) }

    val presetPrompts: List<PresetPrompt> = listOf(
        PresetPrompt("🎵 Midnight Code", "Late night 80s Synthwave synth track about compiling code at 3 AM"),
        PresetPrompt("☕ Focused Lo-Fi", "Soft relaxing lo-fi hip hop beat for coding, studying, and morning coffee energy"),
        PresetPrompt("⚡ Industrial Acid", "Deep peak-time driving Industrial Techno beat for neon clubs"),
        PresetPrompt("🎸 Bright Pop", "Uplifting Acoustic Pop guitar groove about friendship, sunny roads, and programming breakthroughs")
    )

    val styleModifiers = listOf("Synthwave", "Lo-Fi", "Techno", "Acoustic Pop", "Cyberpunk", "Metal", "Ambient Rap")
    val moodModifiers = listOf("Chill", "Energetic", "Dark", "Uplifting", "Melancholic", "Futuristic", "Epic")

    val finalizedPromptPreview = remember(promptText, selectedStyles, selectedMoods) {
        buildString {
            append(promptText.ifBlank { "Unprompted Theme" })
            if (selectedStyles.isNotEmpty()) {
                append(" [Styles: ${selectedStyles.joinToString(", ")}]")
            }
            if (selectedMoods.isNotEmpty()) {
                append(" [Moods: ${selectedMoods.joinToString(", ")}]")
            }
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val historySongs by viewModel.allSongs.collectAsState()

    // Auto-navigate to player when a new track is successfully prepared
    LaunchedEffect(generationState) {
        if (generationState is GenerationState.Success) {
            onNavigateToPlayer()
            viewModel.resetGenerationState()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.width(320.dp).fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Compositions Library",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(onClick = { scope.launch { drawerState.close() } }) {
                            Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (historySongs.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No sessions generated yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(historySongs) { song ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            promptText = song.prompt.substringBefore(" [Styles:").substringBefore(" [Moods:")
                                            manualGenreSelection = song.genre
                                            try {
                                                val moshi = com.squareup.moshi.Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
                                                val songAdapter = moshi.adapter(com.example.model.GeneratedSongJson::class.java)
                                                val songJson = songAdapter.fromJson(song.lyricsJson)
                                                if (songJson != null) {
                                                    val lLines = songJson.sections.flatMap { it.lines }.joinToString("\n")
                                                    customLyricsText = lLines
                                                    useCustomLyrics = true
                                                }
                                            } catch (e: Exception) {
                                                // fallback
                                            }
                                            scope.launch { drawerState.close() }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.6f)
                                    ),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = song.title,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = song.genre,
                                                fontSize = 11.sp,
                                                color = CyberCyan
                                            )
                                            Text(
                                                text = "${song.bpm} BPM",
                                                fontSize = 11.sp,
                                                color = BrightMagenta
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Tap to load parameters",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            IconButton(
                                                onClick = {
                                                    viewModel.audioEngine.play(song)
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.PlayArrow,
                                                    contentDescription = "Quick Play",
                                                    tint = ElectricViolet,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Gen-Studio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            },
                            modifier = Modifier.testTag("toggle_history_drawer")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "History Sidebar"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            modifier = modifier
        ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header decoration
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ElectricViolet.copy(alpha = 0.15f), BrightMagenta.copy(alpha = 0.05f))
                            )
                        )
                        .border(1.dp, ElectricViolet.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Describe your Masterpiece",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Our AI generator will compose custom lyrics, structure tempos, and synthesize instrument beats in real time.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Prompt Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "1. Tell the AI what to compose",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Robust Text Input Box
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    placeholder = {
                        Text(
                            "e.g., An energetic summer song about traveling the world",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("prompt_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricViolet,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = ElectricViolet,
                        cursorColor = ElectricViolet
                    ),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 💡 Interactive Music & Lyric Suggestion Assistant Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Brush.linearGradient(listOf(ElectricViolet.copy(alpha = 0.35f), CyberCyan.copy(alpha = 0.35f))))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "💡 Studio Music & Lyric Suggestions",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            
                            // Shuffle Button
                            IconButton(
                                onClick = {
                                    activeSuggestionIndex = (activeSuggestionIndex + 1) % musicSuggestions.size
                                },
                                modifier = Modifier.size(28.dp).testTag("suggestion_shuffle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Shuffle suggestions",
                                    tint = BrightMagenta,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val activeSuggestion = musicSuggestions[activeSuggestionIndex]
                        
                        // Navigation Row with suggestion title
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    activeSuggestionIndex = if (activeSuggestionIndex == 0) musicSuggestions.size - 1 else activeSuggestionIndex - 1
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronLeft,
                                    contentDescription = "Previous idea",
                                    tint = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeSuggestion.emoji,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = activeSuggestion.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    activeSuggestionIndex = (activeSuggestionIndex + 1) % musicSuggestions.size
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = "Next idea",
                                    tint = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = activeSuggestion.description,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Suggestion box details
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            // Section 1: Music Style / Prompt
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "🎼 SUGGESTED PLAYLIST CONCEPT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricViolet,
                                    letterSpacing = 1.sp
                                )
                                Button(
                                    onClick = {
                                        promptText = activeSuggestion.prompt
                                        selectedStyles = setOf(activeSuggestion.style)
                                        selectedMoods = setOf(activeSuggestion.mood)
                                        // Auto-apply manual genre
                                        manualGenreSelection = activeSuggestion.genre
                                    },
                                    modifier = Modifier.height(26.dp).testTag("apply_music_${activeSuggestion.title.lowercase().replace(" ", "_")}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet.copy(alpha = 0.15f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(13.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentPaste,
                                            contentDescription = null,
                                            tint = ElectricViolet,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Apply Music Concept", fontSize = 10.sp, color = ElectricViolet, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activeSuggestion.prompt,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            
                            // Suggested Style Tags Indicator Row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                SuggestionBadge(text = "Style: ${activeSuggestion.genre}", color = CyberCyan)
                                SuggestionBadge(text = "Mood: ${activeSuggestion.mood}", color = BrightMagenta)
                            }
                            
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Section 2: Lyrics suggestion
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "✍️ LYRICS DESIGN CONCEPT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan,
                                    letterSpacing = 1.sp
                                )
                                Button(
                                    onClick = {
                                        useCustomLyrics = true
                                        customLyricsText = activeSuggestion.lyrics
                                    },
                                    modifier = Modifier.height(26.dp).testTag("apply_lyrics_${activeSuggestion.title.lowercase().replace(" ", "_")}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.15f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(13.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.EditNote,
                                            contentDescription = null,
                                            tint = CyberCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Apply Lyrical Verses", fontSize = 10.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // Scrolling preview of rhyming lyrics
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 110.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = activeSuggestion.lyrics,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.75f),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Style Modifiers Category Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Cyclone,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Style Tags",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    styleModifiers.forEach { style ->
                        val isSelected = selectedStyles.contains(style)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedStyles = if (isSelected) {
                                    selectedStyles - style
                                } else {
                                    selectedStyles + style
                                }
                            },
                            label = { Text(style, fontSize = 11.sp) },
                            modifier = Modifier.testTag("style_chip_${style.lowercase()}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                selectedLabelColor = CyberCyan,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mood Modifiers Category Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEmotions,
                        contentDescription = null,
                        tint = BrightMagenta,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Mood Tags",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    moodModifiers.forEach { mood ->
                        val isSelected = selectedMoods.contains(mood)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedMoods = if (isSelected) {
                                    selectedMoods - mood
                                } else {
                                    selectedMoods + mood
                                }
                            },
                            label = { Text(mood, fontSize = 11.sp) },
                            modifier = Modifier.testTag("mood_chip_${mood.lowercase()}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrightMagenta.copy(alpha = 0.2f),
                                selectedLabelColor = BrightMagenta,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Live Preview box of how the finalized prompt looks
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SettingsVoice,
                                contentDescription = null,
                                tint = ElectricViolet,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "FINALIZED STUDIO PROMPT PREVIEW",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricViolet,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = finalizedPromptPreview,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Manual Settings Card
                var genreDropdownExpanded by remember { mutableStateOf(false) }
                var durationDropdownExpanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (showManualOptions) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (showManualOptions) CyberCyan.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.04f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Manual Music Control",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Customize exact genre and duration target",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = showManualOptions,
                                onCheckedChange = { showManualOptions = it },
                                modifier = Modifier.testTag("manual_options_toggle"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CyberCyan,
                                    checkedTrackColor = ElectricViolet
                                )
                            )
                        }

                        AnimatedVisibility(visible = showManualOptions) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                // Genre Dropdown
                                Text(
                                    text = "Select Specific Genre",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.background)
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .clickable { genreDropdownExpanded = true }
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = manualGenreSelection,
                                            fontSize = 13.sp,
                                            color = if (manualGenreSelection == "Auto") CyberCyan else Color.White
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Expand genre dropdown",
                                            tint = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = genreDropdownExpanded,
                                        onDismissRequest = { genreDropdownExpanded = false },
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    ) {
                                        manualGenresList.forEach { gen ->
                                            DropdownMenuItem(
                                                text = { 
                                                    Text(
                                                        text = gen, 
                                                        color = if (gen == "Auto") CyberCyan else Color.White,
                                                        fontSize = 13.sp
                                                    ) 
                                                },
                                                onClick = {
                                                    manualGenreSelection = gen
                                                    genreDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Duration Dropdown
                                Text(
                                    text = "Song Duration Selection",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.background)
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .clickable { durationDropdownExpanded = true }
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = manualDurationMinutesSelection,
                                            fontSize = 13.sp,
                                            color = if (manualDurationMinutesSelection == "Auto") CyberCyan else Color.White
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Expand duration dropdown",
                                            tint = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = durationDropdownExpanded,
                                        onDismissRequest = { durationDropdownExpanded = false },
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    ) {
                                        manualDurationsList.forEach { dur ->
                                            DropdownMenuItem(
                                                text = { 
                                                    Text(
                                                        text = dur, 
                                                        color = if (dur == "Auto") CyberCyan else Color.White,
                                                        fontSize = 13.sp
                                                    ) 
                                                },
                                                onClick = {
                                                    manualDurationMinutesSelection = dur
                                                    durationDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Custom Lyrics Section with Character Count validation
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (useCustomLyrics) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (useCustomLyrics) ElectricViolet.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.04f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.EditNote,
                                    contentDescription = null,
                                    tint = ElectricViolet,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Custom Song Lyrics",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Supply or edit the core verbal layout",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = useCustomLyrics,
                                onCheckedChange = { useCustomLyrics = it },
                                modifier = Modifier.testTag("custom_lyrics_toggle"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CyberCyan,
                                    checkedTrackColor = ElectricViolet
                                )
                            )
                        }

                        // Text Area for Custom Lyrics
                        AnimatedVisibility(visible = useCustomLyrics) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                val remainingCount = maxLyricsCharCount - customLyricsText.length
                                val counterColor = if (remainingCount <= 50) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant

                                OutlinedTextField(
                                    value = customLyricsText,
                                    onValueChange = { text ->
                                        // Restrict input to 1000 characters
                                        if (text.length <= maxLyricsCharCount) {
                                            customLyricsText = text
                                        }
                                    },
                                    placeholder = {
                                        Text(
                                            "Enter your custom verses and chorus here...\n\n[Verse 1]\nEarly morning coffee brewing\nWriting lines with no room for error...\n\n[Chorus]\nCode compiled, active synth beats rising!",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .testTag("lyrics_input"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricViolet,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        cursorColor = ElectricViolet
                                    )
                                )
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                // Text length metadata info
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Separate parts with block brackets (e.g., [Verse], [Chorus]) for sectioning",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${customLyricsText.length} / $maxLyricsCharCount Chars",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = counterColor,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Prompt Suggestions Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Need Inspiration?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Prompt Presets Cards Grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    presetPrompts.forEach { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                                .clickable {
                                    promptText = item.text
                                    // Prepopulate best tags matching presets
                                    if (item.label.contains("Synthwave")) {
                                        selectedStyles = setOf("Synthwave")
                                        selectedMoods = setOf("Futuristic")
                                    } else if (item.label.contains("Lo-Fi")) {
                                        selectedStyles = setOf("Lo-Fi")
                                        selectedMoods = setOf("Chill")
                                    } else if (item.label.contains("Techno")) {
                                        selectedStyles = setOf("Techno")
                                        selectedMoods = setOf("Dark", "Energetic")
                                    } else if (item.label.contains("Pop")) {
                                        selectedStyles = setOf("Acoustic Pop")
                                        selectedMoods = setOf("Uplifting")
                                    }
                                    keyboardController?.hide()
                                }
                                .padding(12.dp)
                                .testTag("preset_prompt_${item.label.lowercase().replace(" ", "_").trim()}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberCyan
                                    )
                                    Text(
                                        text = item.text,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Use prompt",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Creation trigger button
                Button(
                    onClick = {
                        keyboardController?.hide()
                        val selectedGenreArg = if (showManualOptions && manualGenreSelection != "Auto") manualGenreSelection else null
                        val selectedDurationSecsArg = if (showManualOptions) {
                            when (manualDurationMinutesSelection) {
                                "1 Minute" -> 60
                                "2 Minutes" -> 120
                                "3 Minutes" -> 180
                                "4 Minutes" -> 240
                                "5 Minutes" -> 300
                                else -> null
                            }
                        } else null

                        viewModel.generateSong(
                            prompt = finalizedPromptPreview,
                            customLyrics = if (useCustomLyrics) customLyricsText else null,
                            manualGenre = selectedGenreArg,
                            manualDurationSeconds = selectedDurationSecsArg
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("generate_button"),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(ElectricViolet, BrightMagenta)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Generate Studio Track",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Error Card visual message
                if (generationState is GenerationState.Error) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error notification",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = generationState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Real-time compiling studio screen overlay
            if (generationState is GenerationState.Generating) {
                // Spinning circle ring transition
                val infiniteTransition = rememberInfiniteTransition()
                val rotationAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .clickable(enabled = false) {}, // Lock scrolling
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .rotate(rotationAngle),
                                color = CyberCyan,
                                strokeWidth = 4.dp,
                                trackColor = ElectricViolet
                            )
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = BrightMagenta,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "AI GENERATION ACTIVE",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Current stage messaging
                        Text(
                            text = generationState.stepMessage,
                            color = CyberCyan,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Writing original verses and composing procedural synths...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Mini player / Now Playing bar at the bottom
            val playingSong = viewModel.audioEngine.currentPlayingSong
            if (playingSong != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .shadow(12.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0D0D14))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .clickable { onNavigateToPlayer() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = BrightMagenta,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playingSong.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (viewModel.audioEngine.isPlaying) "Playing procedural session..." else "Paused",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        
                        // Small visualizer in the mini-bar
                        AudioVisualizer(
                            amplitudes = viewModel.audioEngine.visualizerAmplitudes,
                            isPlaying = viewModel.audioEngine.isPlaying,
                            modifier = Modifier.size(60.dp, 30.dp)
                        )
                        
                        IconButton(onClick = { viewModel.audioEngine.togglePlayPause() }) {
                            Icon(
                                imageVector = if (viewModel.audioEngine.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
}

// Data classes and suggestions moved to top

