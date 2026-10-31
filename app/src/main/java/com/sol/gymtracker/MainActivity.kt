package com.sol.gymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sol.gymtracker.ui.GymViewModel
import com.sol.gymtracker.ui.GymTrackerApp
import com.sol.gymtracker.ui.theme.GymTrackerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: GymViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        setContent {
            GymTrackerTheme {
                val active by viewModel.activeWorkout.collectAsStateWithLifecycle()
                var screen by remember { mutableStateOf(AppScreen.HOME) }
                var activeId by remember { mutableStateOf<String?>(null) }
                GymTrackerApp(
                    viewModel = viewModel,
                    screen = screen,
                    activeSessionId = activeId ?: active?.session?.id,
                    onScreen = { screen = it },
                    onActive = { activeId = it; screen = AppScreen.ACTIVE },
                )
            }
        }
    }
}

enum class AppScreen { HOME, PLANS, BUILDER, PREVIEW, CATALOG, HISTORY, SETTINGS, ABOUT, ACTIVE }