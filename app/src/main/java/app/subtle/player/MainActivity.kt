package app.subtle.player

import android.Manifest
import android.app.LocaleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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

    /** Set by the player screen so leaving the app (home/recents) collapses into PiP. */
    var leaveToPip: (() -> Unit)? = null

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        leaveToPip?.invoke()
    }

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
                val context = LocalContext.current
                val notificationPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                val languageTag = appSettings.language.tag
                LaunchedEffect(languageTag) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val manager = context.getSystemService(LocaleManager::class.java)
                        val desired = if (languageTag == null) {
                            LocaleList.getEmptyLocaleList()
                        } else {
                            LocaleList.forLanguageTags(languageTag)
                        }
                        if (manager.applicationLocales != desired) {
                            manager.applicationLocales = desired
                        }
                    }
                }
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
