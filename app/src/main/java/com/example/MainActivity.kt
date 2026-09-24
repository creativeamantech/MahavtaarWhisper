package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screen.HomeScreen
import com.example.ui.screen.ImportAudioScreen
import com.example.ui.screen.RecordScreen
import com.example.ui.screen.SettingsScreen
import com.example.ui.screen.TranscriptionDetailScreen
import com.example.ui.theme.WhisperTheme
import com.example.ui.viewmodel.WhisperViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WhisperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WhisperAppNavigation()
                }
            }
        }
    }
}

@Composable
fun WhisperAppNavigation(
    viewModel: WhisperViewModel = viewModel()
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToRecord = { navController.navigate("record") },
                onNavigateToImport = { navController.navigate("import") },
                onNavigateToDetail = { id -> navController.navigate("detail/$id") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }

        composable("record") {
            RecordScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onTranscriptionReady = { id ->
                    navController.popBackStack()
                    navController.navigate("detail/$id")
                }
            )
        }

        composable("import") {
            ImportAudioScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onTranscriptionReady = { id ->
                    navController.popBackStack()
                    navController.navigate("detail/$id")
                }
            )
        }

        composable(
            route = "detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            TranscriptionDetailScreen(
                transcriptionId = id,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
