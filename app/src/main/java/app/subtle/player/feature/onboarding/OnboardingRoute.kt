package app.subtle.player.feature.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun OnboardingRoute(
    viewModel: OnboardingViewModel = viewModel(),
) {
    val context = LocalContext.current
    val runtimePermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    OnboardingScreen(
        onGrantAccess = {
            val intent = viewModel.allFilesIntent()
            if (intent != null) {
                context.startActivity(intent)
            } else {
                viewModel.requiredRuntimePermission()?.let(runtimePermissionLauncher::launch)
            }
        },
    )
}
