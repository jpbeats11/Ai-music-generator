package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.CreateSongScreen
import com.example.ui.PlayerScreen
import com.example.ui.SongHistoryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SongViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val navController = rememberNavController()
        val viewModel: SongViewModel = viewModel()

        NavHost(
          navController = navController,
          startDestination = "history",
          modifier = Modifier.fillMaxSize()
        ) {
          composable("history") {
            SongHistoryScreen(
              viewModel = viewModel,
              onNavigateToCreate = { navController.navigate("create") },
              onNavigateToPlayer = { navController.navigate("player") }
            )
          }
          composable("create") {
            CreateSongScreen(
              viewModel = viewModel,
              onNavigateToPlayer = {
                navController.navigate("player") {
                  popUpTo("history")
                }
              }
            )
          }
          composable("player") {
            PlayerScreen(
              viewModel = viewModel,
              onBackClick = { navController.popBackStack() }
            )
          }
        }
      }
    }
  }
}
