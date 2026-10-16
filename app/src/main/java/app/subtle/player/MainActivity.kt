package app.subtle.player

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.subtle.player.core.designsystem.SubtleTheme
import app.subtle.player.feature.main.MainRoute
import app.subtle.player.feature.onboarding.OnboardingRoute
import app.subtle.player.feature.player.PlayerRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val intentUri = intent.takeIf { it.action == Intent.ACTION_VIEW }?.data
        val fromIntent = intent.action == Intent.ACTION_VIEW
        setContent {
            SubtleTheme {
                val rootViewModel: RootViewModel = viewModel()
                val start by rootViewModel.start.collectAsStateWithLifecycle()
                LifecycleResumeEffect(Unit) {
                    rootViewModel.refresh()
                    onPauseOrDispose { }
                }
                var openVideo by rememberSaveable { mutableStateOf(intentUri?.toString()) }
                val current = openVideo
                when {
                    current != null -> {
                        val close = { if (fromIntent) finish() else openVideo = null }
                        BackHandler(onBack = close)
                        PlayerRoute(initialUri = current.toUri(), onClose = close)
                    }

                    start == AppStart.Main -> MainRoute(onVideoClick = { openVideo = it })
                    else -> OnboardingRoute()
                }
            }
        }
    }
}
