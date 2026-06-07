package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.SongEntity
import com.example.ui.theme.BrightMagenta
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepStudioSlate
import com.example.ui.theme.ElectricViolet
import com.example.viewmodel.SongViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongHistoryScreen(
    viewModel: SongViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allSongs by viewModel.allSongs.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0 = All, 1 = Favorites

    val activeSongsList = if (selectedTab == 0) allSongs else favoriteSongs
    
    val filteredSongs = remember(activeSongsList, searchQuery) {
        if (searchQuery.trim().isEmpty()) {
            activeSongsList
        } else {
            activeSongsList.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.prompt.contains(searchQuery, ignoreCase = true) ||
                it.genre.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Studio Library", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    // Floating button to navigate to song creation
                    IconButton(
                        onClick = onNavigateToCreate,
                        modifier = Modifier.testTag("library_add_button")
                    ) {
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Create track", tint = CyberCyan, modifier = Modifier.size(28.dp))
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
        ) {
            // Session Statistics Panel
            AnimatedVisibility(
                visible = viewModel.sessionTracksCount > 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = DeepStudioSlate),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.WorkspacePremium,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Session Creator Active",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Tracks composed in current session",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(CyberCyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${viewModel.sessionTracksCount} Generated",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search title, style, prompt...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricViolet,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Tabs Block
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = ElectricViolet,
                divider = {},
                indicator = { tabPositions ->
                    if (tabPositions.isNotEmpty()) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = ElectricViolet,
                            height = 3.dp
                        )
                    }
                },
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("All Songs", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("all_songs_tab")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Favorites", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("favorites_tab")
                )
            }

            // Playlist history list
            if (filteredSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Default.FavoriteBorder else Icons.Rounded.LibraryMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTab == 1) "No favorite tracks found" else "Your studio is quiet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedTab == 1) "Mark songs with a heart in your library to access them here." else "Generate your very first creative track outline inside Gen-Studio!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        if (selectedTab == 0) {
                            Button(
                                onClick = onNavigateToCreate,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                modifier = Modifier.testTag("create_first_song_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate Custom Song")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredSongs, key = { it.id }) { song ->
                        val isPlayingThis = playerMatchesSong(viewModel.audioEngine.currentPlayingSong, song)
                        val isCurrentlyPlaying = isPlayingThis && viewModel.audioEngine.isPlaying

                        // Construct a high-contrast simulated sharing link
                        val simulatedPlaybackUrl = "https://gen-studio.io/play/${song.id}?seed=${song.seed}"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = if (isPlayingThis) ElectricViolet.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.04f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    // Set currently playing song if not set and trigger transition
                                    if (!isPlayingThis) {
                                        viewModel.audioEngine.play(song)
                                    }
                                    onNavigateToPlayer()
                                }
                                .testTag("song_item_${song.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPlayingThis) DeepStudioSlate else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Play icon with subtle indicator background
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isPlayingThis) {
                                                Brush.linearGradient(colors = listOf(ElectricViolet, BrightMagenta))
                                            } else {
                                                Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.02f)))
                                            }
                                        )
                                        .clickable {
                                            if (isPlayingThis) {
                                                viewModel.audioEngine.togglePlayPause()
                                            } else {
                                                viewModel.audioEngine.play(song)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentlyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isCurrentlyPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Song metadata
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPlayingThis) CyberCyan else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.prompt,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Row(
                                        modifier = Modifier.padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = song.genre,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberCyan,
                                            modifier = Modifier
                                                .background(CyberCyan.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                        Text(
                                            text = "${song.bpm} BPM",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrightMagenta,
                                            modifier = Modifier
                                                .background(BrightMagenta.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                        Text(
                                            text = formatDate(song.createdAt),
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Quick Actions: Share Playback Link, Favorite & Delete
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Copy Playback Link Action Button
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(simulatedPlaybackUrl))
                                            Toast.makeText(context, "Playback link copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.testTag("copy_link_${song.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share",
                                            tint = CyberCyan.copy(alpha = 0.85f),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    
                                    IconButton(
                                        onClick = { viewModel.toggleFavorite(song) },
                                        modifier = Modifier.testTag("favorite_button_${song.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (song.isFavorite) BrightMagenta else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    
                                    IconButton(
                                        onClick = { viewModel.deleteSong(song) },
                                        modifier = Modifier.testTag("delete_button_${song.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Floating mini player if there is a song loaded but we're on the history list
            val currentLoadedSong = viewModel.audioEngine.currentPlayingSong
            AnimatedVisibility(visible = currentLoadedSong != null) {
                if (currentLoadedSong != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .border(1.dp, ElectricViolet.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable { onNavigateToPlayer() }
                            .testTag("mini_player_card"),
                        colors = CardDefaults.cardColors(containerColor = DeepStudioSlate),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "NOW PLAYING",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricViolet,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = currentLoadedSong.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (viewModel.audioEngine.isPlaying) "Active in sequencer..." else "Paused",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.audioEngine.togglePlayPause() }) {
                                Icon(
                                    imageVector = if (viewModel.audioEngine.isPlaying) Icons.Rounded.PauseCircle else Icons.Rounded.PlayCircle,
                                    contentDescription = "Play/Pause",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun playerMatchesSong(playingSong: SongEntity?, targetSong: SongEntity): Boolean {
    return playingSong != null && playingSong.id == targetSong.id
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
