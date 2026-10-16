package app.subtle.player

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.subtle.player.core.designsystem.SubtleTheme
import app.subtle.player.domain.model.ThemeMode
import app.subtle.player.feature.about.AboutRoute
import app.subtle.player.feature.main.MainRoute
import app.subtle.player.feature.onboarding.OnboardingRoute
import app.subtle.player.feature.player.PlayerRoute
import app.subtle.player.feature.settings.SettingsRoute
import app.subtle.player.feature.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val intentUri = intent.takeIf { it.action == Intent.ACTION_VIEW }?.data
        val fromIntent = intent.action == Intent.ACTION_VIEW
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val appSettings = settingsState.settings
            val darkTheme = when (appSettings.theme) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }

            SubtleTheme(darkTheme = darkTheme, dynamicColor = appSettings.dynamicColor) {
                val rootViewModel: RootViewModel = viewModel()
                val start by rootViewModel.start.collectAsStateWithLifecycle()
                LifecycleResumeEffect(Unit) {
                    rootViewModel.refresh()
                    onPauseOrDispose { }
                }
                var openVideo by rememberSaveable { mutableStateOf(intentUri?.toString()) }
                var showSettings by rememberSaveable { mutableStateOf(false) }
                var showAbout by rememberSaveable { mutableStateOf(false) }
                var tabIndex by rememberSaveable { mutableIntStateOf(0) }
                val current = openVideo

                BackHandler(enabled = current == null && start == AppStart.Main && (showSettings || showAbout)) {
                    if (showAbout) showAbout = false else showSettings = false
                }

                when {
                    current != null -> {
                        val close = { if (fromIntent) finish() else openVideo = null }
                        BackHandler(onBack = close)
                        PlayerRoute(initialUri = current.toUri(), onClose = close)
                    }

                    start == AppStart.Main -> when {
                        showAbout -> AboutRoute(onBack = { showAbout = false })
                        showSettings -> SettingsRoute(
                            onOpenAbout = { showAbout = true },
                            onBack = { showSettings = false },
                        )

                        else -> MainRoute(
                            tabIndex = tabIndex,
                            onTabChange = { tabIndex = it },
                            onVideoClick = { openVideo = it },
                            onOpenSettings = { showSettings = true },
                        )
                    }

                    else -> OnboardingRoute()
                }
            }
        }
    }
}
